package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the problems found by the offline deep test of all room layouts. */
class DeepRegressionTest {
    private static final int N = RoomTransform.ROOM;

    // ---- B1: a route never starts by walking away from the exit ----------------------------------------------------

    @Test
    void aRouteFromAnywhereInARoomIsNotMuchLongerThanTheWalkOut() {
        TestVoxels v = TestVoxels.plan(
                "#######################",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#.....................#",
                "#######.###############",
                "#######.###############",
                "#######.###############",
                "#######.###############",
                "#######.###############",
                "#######################");
        WalkGraph g = v.scan();
        int exit = g.cellAt(7, 0, 13);
        RouteField field = RouteField.build(g, new int[]{exit});
        int[] steps = bfs(g, exit);
        for (int c = 0; c < g.count; c++) {
            if (c == exit) {
                continue;
            }
            Route route = field.route(c);
            assertNotNull(route, "cell " + g.x(c) + "," + g.z(c));
            assertTrue(route.blocks() <= steps[c] + 3.0,
                    "from " + g.x(c) + "," + g.z(c) + " route " + route.blocks() + " vs " + steps[c] + " steps");
            // the first leg gets closer to the exit (or at least not much further from it)
            double first = Math.hypot(route.x(Math.min(1, route.length() - 1)) - 7.5, route.z(Math.min(1, route.length() - 1)) - 13.5);
            double start = Math.hypot(route.x(0) - 7.5, route.z(0) - 13.5);
            assertTrue(first <= start + 1.0, "first leg from " + g.x(c) + "," + g.z(c) + " goes away from the exit");
        }
    }

    private static int[] bfs(WalkGraph g, int from) {
        int[] d = new int[g.count];
        Arrays.fill(d, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        d[from] = 0;
        q.add(from);
        while (!q.isEmpty()) {
            int c = q.poll();
            for (int i = 0; i < g.degree(c); i++) {
                int o = g.neighbour(c, i);
                if (d[o] < 0) {
                    d[o] = d[c] + 1;
                    q.add(o);
                }
            }
        }
        return d;
    }

    // ---- B2: hazards are never walked through ----------------------------------------------------------------------

    @Test
    void aPoolOfLavaOrVoidLiquidSplitsACorridor() {
        TestVoxels v = TestVoxels.plan(
                "#########",
                "#.......#",
                "#########");
        // a one deep pool: the hazard fills the feet block of three columns
        for (int x = 3; x <= 5; x++) {
            v.set(x, 1, 1, BlockCode.hazard());
        }
        WalkGraph g = v.scan();
        assertTrue(g.cellAt(1, 0, 1) >= 0);
        assertTrue(g.cellAt(2, 0, 1) >= 0);
        assertEquals(-1, g.cellAt(4, 0, 1), "no cell under a hazard");
        assertFalse(g.linked(g.cellAt(2, 0, 1), g.cellAt(6, 0, 1)));
        int[] steps = bfs(g, g.cellAt(1, 0, 1));
        assertEquals(-1, steps[g.cellAt(6, 0, 1)], "the far side is cut off");
    }

    @Test
    void aHazardBlockIsNeverASurfaceToStandOn() {
        TestVoxels v = TestVoxels.plan(
                "#####",
                "#...#",
                "#####");
        v.set(2, 0, 1, BlockCode.fullBlock() | BlockCode.HAZARD);
        WalkGraph g = v.scan();
        assertEquals(-1, g.cellAt(2, 0, 1));
    }

    // ---- R1 / R7: half steps only on stairs and slabs, headroom when stepping between heights --------------------

    @Test
    void aCarpetNextToAHigherBlockIsAJumpNeedingHeadroom() {
        // floor at y 0; a full block at x 3 (its top is one block up). Carpet at x 2 sits 1/16 high: the step is 15/16.
        TestVoxels v = new TestVoxels(9, 8);
        v.fill(0, 0, 0, 8, 0, 8, BlockCode.fullBlock());
        v.fill(3, 1, 4, 3, 1, 4, BlockCode.fullBlock());
        v.set(2, 1, 4, BlockCode.solid(0, 1));
        // the underside of a block hangs just above the carpet cell's body (16+1+29 = 46) but inside a jump's
        v.set(2, 2, 4, BlockCode.solid(14, 16));
        v.maxFeet = 6;
        WalkGraph g = v.scan();
        int carpet = g.cellAt(2, 1, 4);
        int block = g.cellAt(3, 1, 4);
        assertTrue(carpet >= 0 && block >= 0);
        assertFalse(g.linked(carpet, block), "a 15/16 step under a two block ceiling needs a jump that cannot be made");
    }

    @Test
    void aSlabIsStillAHalfStepEvenUnderALowCeilingElsewhere() {
        TestVoxels v = new TestVoxels(9, 8);
        v.fill(0, 0, 0, 8, 0, 8, BlockCode.fullBlock());
        v.set(3, 1, 4, BlockCode.solid(0, 8));
        v.maxFeet = 6;
        WalkGraph g = v.scan();
        assertTrue(g.linked(g.cellAt(2, 0, 4), g.cellAt(3, 1, 4)), "floor to slab and back is a half step");
    }

    @Test
    void steppingOffASlabIntoATwoHighCorridorNeedsHeadroom() {
        // slab at x 2 (surface +8/16); corridor at x 3 has a ceiling at 2 blocks above the floor.
        TestVoxels v = new TestVoxels(9, 8);
        v.fill(0, 0, 0, 8, 0, 8, BlockCode.fullBlock());
        v.set(2, 1, 4, BlockCode.solid(0, 8));
        v.set(3, 3, 4, BlockCode.fullBlock());
        v.maxFeet = 6;
        WalkGraph g = v.scan();
        int slab = g.cellAt(2, 1, 4);
        int corridor = g.cellAt(3, 0, 4);
        assertTrue(slab >= 0 && corridor >= 0);
        assertFalse(g.linked(slab, corridor), "1.8 blocks do not fit above the slab in a corridor 2 blocks high");
    }

    // ---- exit: the route ends at the mouth of the one wide passage ------------------------------------------------

    /**
     * A hall 3 wide with floor y 0 (feet 1) from x 5 to 20 and a one wide stair shaft going up from its east end to the
     * top level. Placed through {@code t}.
     */
    private static int[] shaftRoom(RoomTransform t) {
        LabyrinthVariant variant = LabyrinthVariant.LIVING;
        TestVoxels v = new TestVoxels(N, variant.scanHeight());
        v.fill(0, 0, 0, N - 1, v.height - 1, N - 1, BlockCode.fullBlock());
        // hall z 20..22, floor feet 1, ceiling above feet 5
        for (int x = 5; x <= 20; x++) {
            for (int z = 20; z <= 22; z++) {
                carve(v, t, x, z, 1, 5);
            }
        }
        // a one wide stair at z 21 climbing east from x 21 (feet 2) to x 35 (feet 16): one block per column
        for (int i = 0; i < 15; i++) {
            int x = 21 + i;
            int feet = 2 + i;
            int[] p = t.apply(x, 21);
            for (int y = feet; y <= feet + 3 && y < v.height; y++) {
                v.set(p[0], y, p[1], BlockCode.AIR);
            }
            // the stair block itself under the feet
            v.set(p[0], feet - 1, p[1], BlockCode.stair());
        }
        return v.copyCodes();
    }

    private static void carve(TestVoxels v, RoomTransform t, int x, int z, int feetFrom, int feetTo) {
        int[] p = t.apply(x, z);
        for (int y = feetFrom; y <= feetTo; y++) {
            v.set(p[0], y, p[1], BlockCode.AIR);
        }
    }

    @Test
    void theRouteEndsAtTheMouthOfThePassageAndNeverClimbsIt() {
        for (RoomTransform t : RoomTransform.all()) {
            WalkGraph graph = WalkScanner.build(shaftRoom(t), N, LabyrinthVariant.LIVING.scanHeight(), 0, LabyrinthVariant.LIVING.topFeetY);
            FloorLayers layers = FloorLayers.compute(graph);
            int topColumn = t.apply(35, 21)[0];
            int topColumnZ = t.apply(35, 21)[1];
            int top = graph.cellAt(topColumn, 15, topColumnZ);
            assertTrue(top >= 0, "the top step is a cell in " + t);
            int[] mouth = ExitMouth.mouth(graph, layers, new int[]{top});
            assertEquals(1, mouth.length, t.toString());
            int[] foot = t.apply(20, 21);
            assertEquals(foot[0], graph.x(mouth[0]), t.toString());
            assertEquals(foot[1], graph.z(mouth[0]), t.toString());
            assertEquals(1, graph.feetY(mouth[0]), "the mouth is on the hall floor");
            // the whole pipeline puts the route end there, never climbs the passage and shows nothing from inside it
            boolean[] passage = ExitMouth.passage(graph, layers, new int[]{top}, mouth);
            assertTrue(passage[top], "the top of the passage is part of it");
            RouteField field = RouteField.build(graph, mouth, passage);
            for (int c = 0; c < graph.count; c++) {
                if (graph.feetY(c) >= 3) {
                    assertTrue(passage[c], "a stair cell above the hall is passage in " + t);
                    assertFalse(field.reachable(c), "no route from the passage in " + t);
                }
            }
            int[] start = t.apply(8, 21);
            Route route = field.route(graph.cellAt(start[0], 0, start[1]));
            assertNotNull(route);
            assertEquals(foot[0] + 0.5F, route.x(route.length() - 1), 0.51F, t.toString());
            for (int i = 0; i < route.length(); i++) {
                assertTrue(route.y(i) <= 1.1F, "stays on the hall floor in " + t);
            }
        }
    }

    @Test
    void anExitThatAlreadyLiesOnAFloorStaysWhereItIs() {
        TestVoxels v = TestVoxels.plan(
                "#########",
                "#.......#",
                "#.......#",
                "#.......#",
                "#########");
        WalkGraph g = v.scan();
        FloorLayers layers = FloorLayers.compute(g);
        int cell = g.cellAt(4, 0, 2);
        assertEquals(cell, ExitMouth.mouth(g, layers, new int[]{cell})[0]);
    }

    // ---- stairways between floors that are already joined -------------------------------------------------------------

    @Test
    void theRouteTakesTheNearerOfTwoShaftsBetweenTheFloors() {
        // Two 3-wide halls, one over the other (floor feet 1 and feet 9), joined by two one wide stairs at the two ends.
        TestVoxels v = new TestVoxels(N, 16);
        v.fill(0, 0, 0, N - 1, 15, N - 1, BlockCode.fullBlock());
        for (int x = 4; x <= 30; x++) {
            for (int z = 4; z <= 6; z++) {
                clear(v, x, z, 1, 5);
                clear(v, x, z + 20, 9, 13);
            }
        }
        // stairs along x at z 5 -> from the lower hall at x 3 going west is not needed: use columns 31..38 climbing to the upper hall
        // west stair: columns x 3 .. -> keep the geometry simple: vertical ladders at both ends instead
        for (int y = 1; y <= 13; y++) {
            v.set(3, y, 5, BlockCode.ladder());
            v.set(31, y, 5, BlockCode.ladder());
        }
        for (int y = 1; y <= 13; y++) {
            v.set(3, y, 25, BlockCode.ladder());
            v.set(31, y, 25, BlockCode.ladder());
        }
        // join the ladder shafts: the ladders run from the lower hall (z 5) ... a shaft is only useful if it reaches the
        // upper hall, so connect each end of the lower hall to the same end of the upper hall through a corridor in z
        for (int z = 6; z <= 25; z++) {
            for (int y = 1; y <= 13; y++) {
                if (y <= 5 || y >= 9) {
                    // keep x = 3 and x = 31 columns open as a ladder shaft all the way
                    v.set(3, y, z, BlockCode.ladder());
                    v.set(31, y, z, BlockCode.ladder());
                }
            }
        }
        v.maxFeet = 14;
        WalkGraph g = v.scan();
        int exit = g.cellAt(17, 8, 25);
        assertTrue(exit >= 0, "the upper hall exists");
        RouteField field = RouteField.build(g, new int[]{exit});
        // From the east end of the lower hall the way up is the east shaft; from the west end, the west one.
        assertTrue(climbsAt(field.route(g.cellAt(29, 0, 5)), 31), "east end uses the east shaft");
        assertTrue(climbsAt(field.route(g.cellAt(5, 0, 5)), 3), "west end uses the west shaft");
    }

    /** True when the route climbs past the middle between the two floors (height 5) inside column x. */
    private static boolean climbsAt(Route route, int x) {
        assertNotNull(route);
        for (int i = 1; i < route.length(); i++) {
            boolean inColumn = (int) Math.floor(route.x(i)) == x && (int) Math.floor(route.x(i - 1)) == x;
            if (inColumn && Math.min(route.y(i), route.y(i - 1)) <= 5.0F && Math.max(route.y(i), route.y(i - 1)) >= 5.0F) {
                return true;
            }
        }
        return false;
    }

    private static void clear(TestVoxels v, int x, int z, int feetFrom, int feetTo) {
        for (int y = feetFrom; y <= feetTo; y++) {
            v.set(x, y, z, BlockCode.AIR);
        }
    }
    // ---- centre lines stay over their own floor, routes start at the player -------------------------------------------

    @Test
    void aRouteStartsInThePlayersCell() {
        TestVoxels v = TestVoxels.plan(
                "##########",
                "#........#",
                "#........#",
                "##########");
        WalkGraph g = v.scan();
        int exit = g.cellAt(8, 0, 1);
        RouteField field = RouteField.build(g, new int[]{exit});
        for (int c = 0; c < g.count; c++) {
            if (c == exit) {
                continue;
            }
            Route route = field.route(c);
            assertNotNull(route);
            assertEquals(g.x(c) + 0.5F, route.x(0), 0.01F, "start x of " + g.x(c) + "," + g.z(c));
            assertEquals(g.z(c) + 0.5F, route.z(0), 0.01F, "start z of " + g.x(c) + "," + g.z(c));
        }
    }

    @Test
    void noRoutePointLiesOverABlockedColumnNextToAPillar() {
        TestVoxels v = TestVoxels.plan(
                "###########",
                "#.........#",
                "#.##......#",
                "#.##......#",
                "#.........#",
                "#.........#",
                "###########");
        WalkGraph g = v.scan();
        int exit = g.cellAt(9, 0, 5);
        RouteField field = RouteField.build(g, new int[]{exit});
        for (int c = 0; c < g.count; c++) {
            Route route = field.route(c);
            if (c == exit) {
                continue;
            }
            for (int i = 0; i < route.length(); i++) {
                int x = (int) Math.floor(route.x(i));
                int z = (int) Math.floor(route.z(i));
                boolean seamX = route.x(i) == x;
                boolean seamZ = route.z(i) == z;
                for (int dx = 0; dx <= (seamX ? 1 : 0); dx++) {
                    for (int dz = 0; dz <= (seamZ ? 1 : 0); dz++) {
                        assertTrue(g.cellAt(x - dx, 0, z - dz) >= 0, "route vertex " + route.x(i) + "," + route.z(i) + " over a wall");
                    }
                }
            }
            RouteShapes.assertKeepsClear(g, field, route, 0.35);
        }
    }
}
