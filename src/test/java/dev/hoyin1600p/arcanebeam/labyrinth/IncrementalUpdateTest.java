package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The incremental update of the block snapshot must give the model a full rescan would give. */
class IncrementalUpdateTest {
    private static final int N = RoomTransform.ROOM;
    private static final LabyrinthVariant V = LabyrinthVariant.LIVING;
    /** World origin of the test box; chosen so chunk borders cut through it (x 8..54 spans chunks 0..3). */
    private static final int OX = 8;
    private static final int OZ = -20;

    /** A little world: the box codes plus a switch for which chunks are loaded. */
    private static final class World implements VoxelSampler {
        final int[] codes = new int[N * N * V.scanHeight()];
        final java.util.Set<Long> unloaded = new java.util.HashSet<>();

        int index(int x, int y, int z) {
            return (y * N + z) * N + x;
        }

        void set(int x, int y, int z, int code) {
            codes[index(x, y, z)] = code;
        }

        @Override
        public int sample(int x, int y, int z) {
            int cx = (OX + x) >> 4;
            int cz = (OZ + z) >> 4;
            if (unloaded.contains(((long) cx << 32) | (cz & 0xFFFFFFFFL))) {
                return BlockCode.unknown();
            }
            return codes[index(x, y, z)];
        }
    }

    /** Three 3-wide halls (west x 5..7, south z 40..42, east x 37..39) in rock, with a ladder exit at (39, 6). */
    private static World room() {
        World w = new World();
        int h = V.scanHeight();
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < N; z++) {
                for (int x = 0; x < N; x++) {
                    boolean hall = (x >= 5 && x <= 7 && z >= 5 && z <= 42) || (z >= 40 && z <= 42 && x >= 5 && x <= 39)
                            || (x >= 37 && x <= 39 && z >= 5 && z <= 42);
                    boolean solid = y == 0 || y >= 16 || !hall;
                    if (solid) {
                        w.set(x, y, z, BlockCode.fullBlock());
                    }
                }
            }
        }
        for (int y = 1; y <= 16; y++) {
            w.set(39, y, 6, BlockCode.ladder());
        }
        // a wall segment across the south hall with a one block gap that a player can mine shut or open
        for (int z = 40; z <= 42; z++) {
            for (int y = 1; y <= 15; y++) {
                w.set(20, y, z, BlockCode.fullBlock());
            }
        }
        for (int y = 1; y <= 2; y++) {
            w.set(20, y, 41, BlockCode.AIR);
        }
        return w;
    }

    private static BoxSnapshot fullScan(World w) {
        BoxSnapshot s = new BoxSnapshot(N, V.scanHeight(), OX, OZ);
        while (!s.sampleLayers(w, 6)) {
            // keep sampling a few layers at a time like the session does
        }
        return s;
    }

    private static void assertSameModel(LabyrinthModel a, LabyrinthModel b) {
        assertEquals(a.graph.count, b.graph.count);
        assertEquals(a.exit.cells().length, b.exit.cells().length);
        for (int c = 0; c < a.graph.count; c++) {
            assertEquals(a.field.distance(c), b.field.distance(c), 1e-6, "distance of cell " + c);
            assertEquals(a.field.reachable(c), b.field.reachable(c), "cell " + c);
        }
    }

    private static LabyrinthModel model(BoxSnapshot s) {
        return LabyrinthModel.build(s.copy(), V, s.unknownBlocks());
    }

    private static void assertSnapshotsEqual(BoxSnapshot a, BoxSnapshot b) {
        for (int y = 0; y < a.height; y++) {
            for (int z = 0; z < N; z++) {
                for (int x = 0; x < N; x++) {
                    assertEquals(a.code(x, y, z), b.code(x, y, z), "block " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void breakingAWallBlockAndPlacingOneGiveWhatAFullRescanGives() {
        World w = room();
        BoxSnapshot live = fullScan(w);
        LabyrinthModel before = model(live);
        int start = before.graph.cellAt(6, 0, 20);
        assertTrue(before.field.reachable(start));
        // the player seals the gap in the south hall wall: place two blocks
        w.set(20, 1, 41, BlockCode.fullBlock());
        w.set(20, 2, 41, BlockCode.fullBlock());
        assertTrue(live.reread(w, OX + 20, 1, OZ + 41));
        assertTrue(live.reread(w, OX + 20, 2, OZ + 41));
        BoxSnapshot fresh = fullScan(w);
        assertSnapshotsEqual(fresh, live);
        LabyrinthModel sealed = model(live);
        assertSameModel(model(fresh), sealed);
        assertFalse(sealed.field.reachable(sealed.graph.cellAt(6, 0, 20)), "the east side is cut off now");
        assertNull(sealed.field.route(sealed.graph.cellAt(6, 0, 20)));
        // and mining it open again restores the old model
        w.set(20, 1, 41, BlockCode.AIR);
        w.set(20, 2, 41, BlockCode.AIR);
        live.reread(w, OX + 20, 1, OZ + 41);
        live.reread(w, OX + 20, 2, OZ + 41);
        assertSameModel(before, model(live));
    }

    @Test
    void aChangeThatDoesNotChangeTheCodeIsNotAChange() {
        World w = room();
        BoxSnapshot live = fullScan(w);
        assertFalse(live.reread(w, OX + 6, 1, OZ + 20), "air stays air");
        // a block outside the box height or footprint is ignored
        assertFalse(live.reread(w, OX + 100, 1, OZ + 20));
        assertFalse(live.reread(w, OX + 6, 99, OZ + 20));
    }

    @Test
    void aTrapdoorOrDoorToggleLeavesTheModelAlone() {
        // doors and trapdoors are air to the scan in both states: the code does not change, so nothing is rebuilt
        World w = room();
        BoxSnapshot live = fullScan(w);
        w.set(6, 1, 20, BlockCode.AIR);
        assertFalse(live.reread(w, OX + 6, 1, OZ + 20));
    }

    @Test
    void aChunkThatLoadsLateIsFilledInAndTheModelEqualsAFullScan() {
        World w = room();
        // the chunk column holding world x 48..63 (box x 40..46) and the middle chunk are not loaded at first
        long far = ((long) ((OX + 40) >> 4) << 32) | ((OZ + 20) >> 4 & 0xFFFFFFFFL);
        w.unloaded.add(far);
        long middle = ((long) ((OX + 20) >> 4) << 32) | ((OZ + 41) >> 4 & 0xFFFFFFFFL);
        w.unloaded.add(middle);
        BoxSnapshot live = fullScan(w);
        assertTrue(live.unknownBlocks() > 0);
        LabyrinthModel partial = model(live);
        assertTrue(partial.graph.unknownBlocks > 0);
        // both chunks arrive
        w.unloaded.clear();
        for (long key : new long[]{far, middle}) {
            live.rereadChunk(w, (int) (key >> 32), (int) key);
        }
        BoxSnapshot fresh = fullScan(w);
        assertEquals(0, live.unknownBlocks());
        assertSnapshotsEqual(fresh, live);
        assertSameModel(model(fresh), model(live));
        assertNotNull(model(live).field.route(model(live).graph.cellAt(6, 0, 20)));
    }

    @Test
    void aReSentChunkIsReadAgain() {
        World w = room();
        BoxSnapshot live = fullScan(w);
        // the server re-sends the chunk with the gap in the wall closed, without any block update
        w.set(20, 1, 41, BlockCode.fullBlock());
        w.set(20, 2, 41, BlockCode.fullBlock());
        live.rereadChunk(w, (OX + 20) >> 4, (OZ + 41) >> 4);
        assertSnapshotsEqual(fullScan(w), live);
    }

    // ---- the update scheduler -------------------------------------------------------------------------------------

    @Test
    void anUpdateWaitsForQuietButNeverBeyondSixtyTicks() {
        DirtyTracker t = new DirtyTracker();
        t.mark(100);
        assertFalse(t.due(105), "still settling");
        assertTrue(t.due(110), "quiet for the debounce time");
        // a block that changes every 4 ticks forever (a villager and a door) cannot starve the update
        DirtyTracker busy = new DirtyTracker();
        int firstDue = -1;
        for (int tick = 100; tick < 400; tick += 4) {
            busy.mark(tick);
            if (busy.due(tick)) {
                firstDue = tick;
                break;
            }
        }
        assertTrue(firstDue > 0 && firstDue - 100 <= DirtyTracker.MAX_WAIT_TICKS + 4, "due after " + (firstDue - 100) + " ticks");
    }

    @Test
    void twoUpdatesAreAtLeastTwentyTicksApart() {
        DirtyTracker t = new DirtyTracker();
        t.mark(0);
        assertTrue(t.due(30));
        t.started(30);
        t.mark(31);
        assertFalse(t.due(45), "too soon after the last update");
        assertTrue(t.due(50));
    }
}
