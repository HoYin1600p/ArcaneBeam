package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntranceTransformTest {
    private static final int N = RoomTransform.ROOM;

    @Test
    void thereAreEightDistinctOrientationsAndIdentityIsFirst() {
        List<RoomTransform> all = RoomTransform.all();
        assertEquals(8, all.size());
        assertEquals(new RoomTransform(false, 0), all.get(0));
        Set<Long> images = new HashSet<>();
        for (RoomTransform t : all) {
            int[] p = t.apply(39, 6);
            images.add(p[0] * 100L + p[1]);
        }
        assertEquals(8, images.size(), "a generic column has 8 different images");
    }

    @Test
    void everyOrientationMapsTheRoomOntoItself() {
        for (RoomTransform t : RoomTransform.all()) {
            boolean[] hit = new boolean[N * N];
            for (int x = 0; x < N; x++) {
                for (int z = 0; z < N; z++) {
                    int[] p = t.apply(x, z);
                    assertTrue(p[0] >= 0 && p[0] < N && p[1] >= 0 && p[1] < N, t + " stays inside the room");
                    assertFalse(hit[p[0] * N + p[1]], t + " is a bijection");
                    hit[p[0] * N + p[1]] = true;
                }
            }
            int[] centre = t.apply(23, 23);
            assertArrayEquals(new int[]{23, 23}, centre, t + " keeps the room centre");
        }
    }

    @Test
    void knownImagesOfACorner() {
        assertArrayEquals(new int[]{0, 0}, new RoomTransform(false, 0).apply(0, 0));
        assertArrayEquals(new int[]{46, 0}, new RoomTransform(true, 0).apply(0, 0));
        assertArrayEquals(new int[]{46, 0}, new RoomTransform(false, 1).apply(0, 0));
        assertArrayEquals(new int[]{46, 46}, new RoomTransform(false, 2).apply(0, 0));
        assertArrayEquals(new int[]{0, 46}, new RoomTransform(false, 3).apply(0, 0));
    }

    @Test
    void fourQuarterTurnsAreTheIdentity() {
        for (int x : new int[]{0, 5, 39}) {
            for (int z : new int[]{0, 6, 40}) {
                int[] p = {x, z};
                for (int i = 0; i < 4; i++) {
                    p = new RoomTransform(false, 1).apply(p[0], p[1]);
                }
                assertArrayEquals(new int[]{x, z}, p);
            }
        }
    }

    @Test
    void eachOrientationOfTheLivingExitIsFoundInTheScan() {
        for (RoomTransform t : RoomTransform.all()) {
            int[] column = t.apply(39, 6);
            WalkGraph graph = roomWithExitAt(LabyrinthVariant.LIVING, column[0], column[1], 16, false);
            EntranceLocator.Result result = EntranceLocator.locate(LabyrinthVariant.LIVING, graph);
            assertEquals(EntranceLocator.Method.TRANSFORM, result.method(), t.toString());
            assertEquals(1, result.cells().length);
            assertEquals(column[0], graph.x(result.cells()[0]));
            assertEquals(column[1], graph.z(result.cells()[0]));
            assertEquals(t.apply(39, 6)[0], graph.x(result.cells()[0]));
        }
    }

    @Test
    void theGildedAndOrnateExitsUseTheirOwnPositions() {
        for (RoomTransform t : RoomTransform.all()) {
            int[] gilded = t.apply(28, 23);
            WalkGraph g = roomWithExitAt(LabyrinthVariant.GILDED, gilded[0], gilded[1], 14, false);
            EntranceLocator.Result r = EntranceLocator.locate(LabyrinthVariant.GILDED, g);
            assertEquals(EntranceLocator.Method.TRANSFORM, r.method());
            assertEquals(gilded[0], g.x(r.cells()[0]));
            assertEquals(gilded[1], g.z(r.cells()[0]));

            int[] ornate = t.apply(29, 24);
            WalkGraph o = roomWithExitAt(LabyrinthVariant.ORNATE, ornate[0], ornate[1], 16, true);
            EntranceLocator.Result ro = EntranceLocator.locate(LabyrinthVariant.ORNATE, o);
            assertEquals(EntranceLocator.Method.TRANSFORM, ro.method());
            assertEquals(ornate[0], o.x(ro.cells()[0]));
            assertEquals(ornate[1], o.z(ro.cells()[0]));
        }
    }

    @Test
    void anUnexpectedExitPositionFallsBackToTheTopCells() {
        WalkGraph graph = roomWithExitAt(LabyrinthVariant.LIVING, 10, 10, 16, false);
        EntranceLocator.Result result = EntranceLocator.locate(LabyrinthVariant.LIVING, graph);
        assertEquals(EntranceLocator.Method.TOP_CELLS, result.method());
        assertEquals(1, result.cells().length);
        assertEquals(10, graph.x(result.cells()[0]));
        assertEquals(10, graph.z(result.cells()[0]));
    }

    @Test
    void noExitMeansNoCells() {
        WalkGraph graph = roomWithExitAt(LabyrinthVariant.LIVING, -1, -1, 16, false);
        assertFalse(EntranceLocator.locate(LabyrinthVariant.LIVING, graph).found());
    }

    @Test
    void theWoldsGatesAreTheSameInEveryOrientation() {
        Set<Long> expected = new HashSet<>();
        for (int[] gate : LabyrinthVariant.WOLDS_LABYRINTH.entranceCells()) {
            expected.add(gate[0] * 100L + gate[2]);
            for (RoomTransform t : RoomTransform.all()) {
                int[] p = t.apply(gate[0], gate[2]);
                assertTrue(containsGate(p[0], p[1]), "the image of a gate is a gate in orientation " + t);
            }
        }
        assertEquals(12, expected.size());
    }

    @Test
    void woldsGateCellsAreReportedWhenWalkable() {
        TestVoxels v = new TestVoxels(N, N);
        v.fill(0, 21, 0, N - 1, 21, N - 1, BlockCode.fullBlock());
        v.fill(0, 27, 0, N - 1, 27, N - 1, BlockCode.fullBlock());
        // Walls around the rim with the four 3-wide tunnel mouths open, the rest of the rim solid at feet level.
        for (int i = 0; i < N; i++) {
            boolean mouth = i >= 22 && i <= 24;
            if (!mouth) {
                v.fill(i, 22, 0, i, 26, 0, BlockCode.fullBlock());
                v.fill(i, 22, N - 1, i, 26, N - 1, BlockCode.fullBlock());
                v.fill(0, 22, i, 0, 26, i, BlockCode.fullBlock());
                v.fill(N - 1, 22, i, N - 1, 26, i, BlockCode.fullBlock());
            }
        }
        v.maxFeet = 45;
        WalkGraph graph = v.scan();
        EntranceLocator.Result result = EntranceLocator.locate(LabyrinthVariant.WOLDS_LABYRINTH, graph);
        assertEquals(EntranceLocator.Method.GATES, result.method());
        assertEquals(12, result.cells().length);
    }

    private static boolean containsGate(int x, int z) {
        for (int[] gate : LabyrinthVariant.WOLDS_LABYRINTH.entranceCells()) {
            if (gate[0] == x && gate[2] == z) {
                return true;
            }
        }
        return false;
    }

    /**
     * A room with a floor, a solid ceiling at the top of the labyrinth and one pillar that reaches the opening in the
     * ceiling at (ex, ez): the only cell at the top level. {@code ladder} puts a climbable block on the pillar top.
     */
    private static WalkGraph roomWithExitAt(LabyrinthVariant variant, int ex, int ez, int topFeet, boolean ladder) {
        TestVoxels v = new TestVoxels(N, variant.scanHeight());
        v.fill(0, 0, 0, N - 1, 0, N - 1, BlockCode.fullBlock());
        v.fill(0, topFeet, 0, N - 1, topFeet, N - 1, BlockCode.fullBlock());
        if (ex >= 0) {
            v.set(ex, topFeet, ez, BlockCode.AIR);
            v.fill(ex, 1, ez, ex, topFeet - 1, ez, BlockCode.fullBlock());
            if (ladder) {
                v.set(ex, topFeet, ez, BlockCode.ladder());
            }
        }
        v.maxFeet = variant.topFeetY;
        return v.scan();
    }
}
