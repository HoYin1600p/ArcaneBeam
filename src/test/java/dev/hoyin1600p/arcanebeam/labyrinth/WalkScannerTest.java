package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkScannerTest {
    private static final int STAIR = BlockCode.stair();
    private static final int SLAB_BOTTOM = BlockCode.solid(0, 8);
    private static final int SLAB_TOP = BlockCode.solid(8, 16);
    private static final int FENCE = BlockCode.solid(0, 24);

    private static int cell(WalkGraph g, int x, int feetY, int z) {
        // The support is the block under the feet, so full-top supports sit one block lower than the feet.
        return g.cellAt(x, feetY - 1, z);
    }

    @Test
    void openFloorIsWalkableAndFourConnected() {
        WalkGraph g = TestVoxels.plan(
                "...",
                "...",
                "...").scan();
        assertEquals(9, g.count);
        int centre = cell(g, 1, 1, 1);
        assertTrue(centre >= 0);
        assertEquals(4, g.degree(centre));
        assertEquals(2, g.degree(cell(g, 0, 1, 0)));
        assertEquals(1.0, g.surface(centre) , 1e-9);
        assertEquals(1, g.feetY(centre));
    }

    @Test
    void wallsAreNeverWalkedThrough() {
        WalkGraph g = TestVoxels.plan(
                "..#..",
                "..#..",
                "..#..",
                "..#..",
                "..#..").scan();
        int left = cell(g, 1, 1, 1);
        int right = cell(g, 3, 1, 1);
        assertTrue(left >= 0 && right >= 0);
        assertEquals(-1, cell(g, 2, 1, 1));
        assertFalse(connected(g, left, right));
    }

    @Test
    void anOpenGapInAWallLinksBothSides() {
        TestVoxels v = TestVoxels.plan(
                "..#..",
                "..#..",
                "..#..",
                "..#..",
                "..#..");
        v.fill(2, 1, 1, 2, 3, 1, BlockCode.AIR);
        WalkGraph g = v.scan();
        assertTrue(connected(g, cell(g, 1, 1, 1), cell(g, 3, 1, 1)));
    }

    @Test
    void trapdoorsAndDoorsHaveNoCollisionAndAreWalkedThrough() {
        // The game adapter reports a trapdoor or door as a block without a collision shape, so the scan sees air.
        TestVoxels v = TestVoxels.plan(
                "..#..",
                "..#..",
                "..#..",
                "..#..",
                "..#..");
        v.fill(2, 1, 1, 2, 2, 1, BlockCode.AIR);
        WalkGraph g = v.scan();
        assertTrue(connected(g, cell(g, 0, 1, 0), cell(g, 4, 1, 2)));
    }

    @Test
    void headroomOfLessThanAPlayerBlocksTheCell() {
        TestVoxels v = TestVoxels.plan("...", "...", "...");
        v.set(1, 2, 1, BlockCode.fullBlock()); // a block at head height leaves one block of space
        v.set(1, 3, 1, BlockCode.fullBlock());
        v.set(1, 1, 1, BlockCode.AIR);
        WalkGraph g = v.scan();
        // Feet block free, head block (y 2) solid: not walkable.
        assertEquals(-1, cell(g, 1, 1, 1));
        // A top-half slab at y 2 leaves 1.5 blocks: still too low.
        TestVoxels w = TestVoxels.plan("...", "...", "...");
        w.set(1, 2, 1, SLAB_TOP);
        assertEquals(-1, cell(w.scan(), 1, 1, 1));
    }

    @Test
    void stairsAndSlabsAreHalfStepsUpAndDown() {
        // Floor at feet 1; a slab (feet 1.5) then a full block step (feet 2) in a straight lane.
        TestVoxels v = new TestVoxels(6, 8);
        v.fill(0, 0, 0, 5, 0, 2, BlockCode.fullBlock());
        v.fill(0, 7, 0, 5, 7, 2, BlockCode.fullBlock());
        v.fill(0, 1, 0, 5, 6, 0, BlockCode.fullBlock());
        v.fill(0, 1, 2, 5, 6, 2, BlockCode.fullBlock());
        v.set(2, 1, 1, SLAB_BOTTOM);
        v.set(3, 1, 1, STAIR);
        v.set(4, 1, 1, STAIR);
        v.set(4, 2, 1, STAIR);
        v.set(5, 1, 1, STAIR);
        v.set(5, 2, 1, STAIR);
        v.set(5, 3, 1, STAIR);
        WalkGraph g = v.scan();
        int floor = g.cellAt(1, 0, 1);
        int slab = g.cellAt(2, 1, 1);
        int step1 = g.cellAt(3, 1, 1);
        int step2 = g.cellAt(4, 2, 1);
        int step3 = g.cellAt(5, 3, 1);
        assertTrue(floor >= 0 && slab >= 0 && step1 >= 0 && step2 >= 0 && step3 >= 0);
        assertEquals(1.5, g.surface(slab), 1e-9);
        assertTrue(g.linked(floor, slab));
        assertTrue(g.linked(slab, step1));
        assertTrue(g.linked(step1, step2));
        assertTrue(g.linked(step2, step3));
        assertEquals(4.0, g.surface(step3), 1e-9);
        assertTrue(connected(g, floor, step3));
    }

    @Test
    void aTwoBlockWallIsNotAStep() {
        TestVoxels v = TestVoxels.plan("...", "...", "...");
        v.fill(1, 1, 0, 1, 2, 2, BlockCode.fullBlock());
        WalkGraph g = v.scan();
        // The column of full blocks is two high: nothing to stand on at feet 1, and the top is under the ceiling.
        assertFalse(connected(g, cell(g, 0, 1, 1), cell(g, 2, 1, 1)));
    }

    @Test
    void fencesCannotBeSteppedOntoOrThrough() {
        TestVoxels v = TestVoxels.plan("...", "...", "...");
        v.set(1, 1, 1, FENCE);
        WalkGraph g = v.scan();
        assertEquals(-1, cell(g, 1, 1, 1));
        assertEquals(-1, g.cellAt(1, 1, 1));
        assertTrue(connected(g, cell(g, 0, 1, 1), cell(g, 2, 1, 1))); // around it
    }

    @Test
    void ladderClimbsBetweenTwoFloors() {
        // Lower floor at y 0, upper floor at y 6 with a hole at (1, *, 1) where the ladder climbs.
        TestVoxels v = new TestVoxels(5, 12);
        v.fill(0, 0, 0, 4, 0, 4, BlockCode.fullBlock());
        v.fill(0, 6, 0, 4, 6, 4, BlockCode.fullBlock());
        v.fill(0, 11, 0, 4, 11, 4, BlockCode.fullBlock());
        v.set(1, 6, 1, BlockCode.AIR);
        // Walls so the only way up is the ladder at (1, 1..6, 1) attached to a wall block at (1, 1..6, 0).
        v.fill(1, 1, 0, 1, 6, 0, BlockCode.fullBlock());
        for (int y = 1; y <= 6; y++) {
            v.set(1, y, 1, BlockCode.ladder());
        }
        // Box off the lower-floor lane next to the ladder so the floors are only joined by the ladder.
        v.fill(0, 1, 0, 0, 5, 4, BlockCode.fullBlock());
        v.fill(2, 1, 0, 2, 5, 4, BlockCode.fullBlock());
        v.fill(1, 1, 2, 1, 5, 4, BlockCode.fullBlock());
        WalkGraph g = v.scan();
        int bottom = g.cellAt(1, 0, 1);
        assertTrue(bottom >= 0);
        assertTrue(g.climbable(bottom));
        int top = -1;
        for (int c = 0; c < g.count; c++) {
            if (g.x(c) == 1 && g.z(c) == 1 && g.feetY(c) == 6) {
                top = c;
            }
        }
        assertTrue(top >= 0, "a ladder cell at the upper floor level");
        assertTrue(connected(g, bottom, top));
        int upperFloor = g.cellAt(1, 6, 2); // the cell standing on the upper floor next to the hole
        assertTrue(upperFloor >= 0);
        assertTrue(connected(g, bottom, upperFloor));
    }

    @Test
    void twoFloorsAreOnlyJoinedThroughTheirStairs() {
        WalkGraph joined = twoFloorBox(true).scan();
        int lower = joined.cellAt(0, 0, 1);
        int upper = joined.cellAt(11, 4, 1);
        assertTrue(lower >= 0 && upper >= 0);
        assertEquals(1.0, joined.surface(lower), 1e-9);
        assertEquals(5.0, joined.surface(upper), 1e-9);
        assertTrue(connected(joined, lower, upper));

        WalkGraph broken = twoFloorBox(false).scan();
        assertFalse(connected(broken, broken.cellAt(0, 0, 1), broken.cellAt(11, 4, 1)));
    }

    /** A 12 x 3 lane: lower floor (feet 1) at x 0..3, four stair towers at x 4..7, upper floor (feet 5) at x 8..11. */
    private static TestVoxels twoFloorBox(boolean middleStep) {
        TestVoxels v = new TestVoxels(12, 10);
        v.fill(0, 0, 0, 11, 0, 2, BlockCode.fullBlock());
        v.fill(0, 9, 0, 11, 9, 2, BlockCode.fullBlock());
        v.fill(0, 1, 0, 11, 8, 0, BlockCode.fullBlock());
        v.fill(0, 1, 2, 11, 8, 2, BlockCode.fullBlock());
        v.fill(8, 1, 1, 11, 4, 1, BlockCode.fullBlock());
        for (int s = 0; s < 4; s++) {
            if (s == 1 && !middleStep) {
                continue;
            }
            v.fill(4 + s, 1, 1, 4 + s, 1 + s, 1, BlockCode.fullBlock());
        }
        v.maxFeet = 8;
        return v;
    }

    @Test
    void stairsClimbUnderASlopedTwoHighRoofButFullBlockStepsDoNot() {
        for (boolean stairs : new boolean[]{true, false}) {
            int step = stairs ? BlockCode.stair() : BlockCode.fullBlock();
            TestVoxels v = new TestVoxels(8, 10);
            v.fill(0, 0, 0, 7, 0, 2, BlockCode.fullBlock());
            v.fill(0, 9, 0, 7, 9, 2, BlockCode.fullBlock());
            v.fill(0, 1, 0, 7, 8, 0, BlockCode.fullBlock());
            v.fill(0, 1, 2, 7, 8, 2, BlockCode.fullBlock());
            // Roof two blocks above each tread: block y = support + 3, rising one block per column.
            for (int x = 0; x < 3; x++) {
                v.fill(x, 3, 1, x, 8, 1, BlockCode.fullBlock());
            }
            for (int s = 0; s < 3; s++) {
                int x = 3 + s;
                v.fill(x, 1, 1, x, s, 1, BlockCode.fullBlock());
                v.set(x, 1 + s, 1, step);
                v.fill(x, 4 + s, 1, x, 8, 1, BlockCode.fullBlock());
            }
            v.fill(6, 1, 1, 7, 8, 1, BlockCode.fullBlock());
            WalkGraph g = v.scan();
            int floor = g.cellAt(2, 0, 1);
            int first = g.cellAt(3, 1, 1);
            int last = g.cellAt(5, 3, 1);
            assertTrue(floor >= 0 && first >= 0 && last >= 0);
            assertEquals(stairs, connected(g, floor, last), "stairs=" + stairs);
        }
    }

    @Test
    void unloadedBlocksCountAsWallsAndAreCounted() {
        TestVoxels v = TestVoxels.plan("...", "...", "...");
        v.set(1, 1, 1, BlockCode.unknown());
        WalkGraph g = v.scan();
        assertEquals(1, g.unknownBlocks);
        assertEquals(-1, cell(g, 1, 1, 1));
    }

    static boolean connected(WalkGraph g, int from, int to) {
        if (from < 0 || to < 0) {
            return false;
        }
        boolean[] seen = new boolean[g.count];
        int[] queue = new int[g.count];
        int head = 0;
        int tail = 0;
        queue[tail++] = from;
        seen[from] = true;
        while (head < tail) {
            int c = queue[head++];
            if (c == to) {
                return true;
            }
            for (int i = 0; i < g.degree(c); i++) {
                int n = g.neighbour(c, i);
                if (!seen[n]) {
                    seen[n] = true;
                    queue[tail++] = n;
                }
            }
        }
        return false;
    }
}
