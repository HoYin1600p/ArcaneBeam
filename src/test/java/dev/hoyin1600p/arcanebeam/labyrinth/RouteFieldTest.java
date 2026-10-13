package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteFieldTest {
    private record Built(WalkGraph graph, RouteField field) {
    }

    private static Built build(TestVoxels voxels, int exitX, int exitSupportY, int exitZ) {
        WalkGraph g = voxels.scan();
        int exit = g.cellAt(exitX, exitSupportY, exitZ);
        assertTrue(exit >= 0, "the exit is a walkable cell");
        return new Built(g, RouteField.build(g, new int[]{exit}));
    }

    /** Builds with every cell of column x = exitX (any z) of the plan as the exit: a whole hall end. */
    private static Built buildHallEnd(TestVoxels voxels, int exitX) {
        WalkGraph g = voxels.scan();
        IntList exits = new IntList();
        for (int c = 0; c < g.count; c++) {
            if (g.x(c) == exitX) {
                exits.add(c);
            }
        }
        return new Built(g, RouteField.build(g, exits.toArray()));
    }

    private static final String[] MAZE = {
            "###############",
            "#.............#",
            "#.###########.#",
            "#.#.........#.#",
            "#.#.#######.#.#",
            "#.#.#.....#.#.#",
            "#.#.#.###.#.#.#",
            "#...#.#.....#.#",
            "#####.#########",
            "###############"};

    @Test
    void theRouteStartsAtThePlayerAndEndsAtTheExit() {
        TestVoxels v = TestVoxels.plan(MAZE);
        Built b = build(v, 13, 0, 1);
        int player = b.graph().cellAt(1, 0, 7);
        assertTrue(b.field().reachable(player));
        Route route = b.field().route(player);
        assertNotNull(route);
        assertEquals(1.5F, route.x(0), 0.01F, "the route starts in the player's cell");
        assertEquals(7.5F, route.z(0), 0.01F);
        int last = route.length() - 1;
        assertEquals(13.5F, route.x(last), 0.8F);
        assertEquals(1.5F, route.z(last), 0.8F);
    }

    @Test
    void theRouteOnlyTurnsAtRealBends() {
        TestVoxels v = TestVoxels.plan(MAZE);
        Built b = build(v, 13, 0, 1);
        Route route = b.field().route(b.graph().cellAt(1, 0, 7));
        // Up the left lane, along the top lane and down the right lane: a handful of vertices, not one per cell.
        assertTrue(route.length() <= 8, "got " + route.length() + " vertices");
        assertTrue(route.blocks() > 15.0);
        RouteShapes.assertKeepsClear(b.graph(), b.field(), route, 0.45);
    }

    @Test
    void theDistanceToTheExitFallsAlongTheWayOut() {
        TestVoxels v = TestVoxels.plan(MAZE);
        Built b = build(v, 13, 0, 1);
        double previous = Double.POSITIVE_INFINITY;
        int[][] lane = {{1, 7}, {1, 6}, {1, 5}, {1, 4}, {1, 3}, {1, 2}, {1, 1}, {2, 1}, {3, 1}, {4, 1}};
        for (int[] p : lane) {
            double d = b.field().distance(b.graph().cellAt(p[0], 0, p[1]));
            assertTrue(d < previous, "distance falls along the lane: " + d + " after " + previous);
            previous = d;
        }
    }

    @Test
    void anUnreachablePocketHasNoRoute() {
        TestVoxels v = TestVoxels.plan(
                "###########",
                "#....#....#",
                "#....#....#",
                "#....#....#",
                "###########");
        Built b = build(v, 1, 0, 1);
        int outside = b.graph().cellAt(8, 0, 2);
        assertTrue(outside >= 0);
        assertFalse(b.field().reachable(outside));
        assertNull(b.field().route(outside));
        assertTrue(b.field().reachable(b.graph().cellAt(3, 0, 2)));
        assertNull(b.field().route(b.graph().cellAt(1, 0, 1)), "no route at the exit itself");
    }

    @Test
    void theRouteCrossesFloorsThroughTheStairs() {
        TestVoxels v = twoFloors(true);
        Built b = build(v, 19, 4, 1);
        int player = b.graph().cellAt(0, 0, 1);
        assertTrue(b.field().reachable(player));
        Route route = b.field().route(player);
        assertEquals(1.0F, route.y(0), 0.01F);
        assertEquals(5.0F, route.y(route.length() - 1), 0.01F);
        for (int i = 1; i < route.length(); i++) {
            assertTrue(route.x(i) >= route.x(i - 1) - 0.01F, "the lane runs one way");
            assertTrue(route.y(i) >= route.y(i - 1) - 0.01F, "it only climbs");
        }
    }

    @Test
    void withoutTheMiddleStepTheUpperFloorIsUnreachable() {
        TestVoxels v = twoFloors(false);
        WalkGraph g = v.scan();
        int exit = g.cellAt(19, 4, 1);
        RouteField field = RouteField.build(g, new int[]{exit});
        assertFalse(field.reachable(g.cellAt(0, 0, 1)));
        assertNull(field.route(g.cellAt(0, 0, 1)));
        assertTrue(field.reachable(g.cellAt(15, 4, 1)));
    }

    // ---- shape: straight across rooms, centred in halls, corners kept -------------------------------------------------

    private static final String[] ROOM = {
            "###################",
            "#.................#",
            "#.................#",
            "#.................#",
            "#.................#",
            "#..................",
            "#.................#",
            "#.................#",
            "#.................#",
            "#.................#",
            "###################"};

    @Test
    void theRouteGoesStraightAcrossAnOpenRoom() {
        TestVoxels v = TestVoxels.plan(ROOM);
        Built b = build(v, 18, 0, 5);
        Route across = b.field().route(b.graph().cellAt(1, 0, 5));
        assertEquals(2, across.length(), "one straight piece from the far wall to the door");
        assertEquals(5.5F, across.z(0), 1.0E-4F);
        assertEquals(5.5F, across.z(1), 1.0E-4F);
        // From a far corner: straight towards the door, a short turn in front of it at most.
        Route corner = b.field().route(b.graph().cellAt(1, 0, 1));
        assertTrue(corner.length() <= 3, "at most one bend, got " + corner.length() + " points");
        double straight = Math.hypot(18.5 - 1.5, 5.5 - 1.5);
        assertTrue(RouteShapes.horizontalLength(corner) <= straight * 1.02, "barely longer than the straight line: "
                + RouteShapes.horizontalLength(corner) + " vs " + straight);
        RouteShapes.assertKeepsClear(b.graph(), b.field(), corner, 0.45);
    }

    @Test
    void aTwoWideHallIsFollowedOnItsSeam() {
        TestVoxels v = TestVoxels.plan(
                "########################",
                "#.......................",
                "#.......................",
                "########################");
        Built b = buildHallEnd(v, 23);
        for (int z = 1; z <= 2; z++) {
            Route route = b.field().route(b.graph().cellAt(1, 0, z));
            RouteShapes.forEachPoint(route, (x, y, pz) -> {
                if (x >= 4.0 && x <= 21.0) {
                    assertEquals(2.0, pz, 0.02, "on the seam at x " + x);
                }
            });
        }
    }

    @Test
    void aThreeWideHallIsFollowedThroughItsMiddle() {
        TestVoxels v = TestVoxels.plan(
                "########################",
                "#.......................",
                "#.......................",
                "#.......................",
                "########################");
        Built b = buildHallEnd(v, 23);
        for (int z = 1; z <= 3; z++) {
            Route route = b.field().route(b.graph().cellAt(1, 0, z));
            RouteShapes.forEachPoint(route, (x, y, pz) -> {
                if (x >= 5.0 && x <= 21.0) {
                    assertEquals(2.5, pz, 0.02, "through the middle at x " + x);
                }
            });
            assertTrue(route.length() <= 3, "a short move to the middle, then straight: " + route.length());
        }
    }

    @Test
    void anLCornerIsNotCut() {
        TestVoxels narrow = TestVoxels.plan(
                "############",
                "#..........#",
                "##########.#",
                "##########.#",
                "##########.#",
                "##########.#",
                "##########.#",
                "############");
        Built b = build(narrow, 10, 0, 6);
        Route route = b.field().route(b.graph().cellAt(1, 0, 1));
        RouteShapes.assertKeepsClear(b.graph(), b.field(), route, 0.45);
        assertTrue(route.length() <= 4, "along, round the corner, down: " + route.length());

        TestVoxels wide = TestVoxels.plan(
                "##############",
                "#............#",
                "#............#",
                "#............#",
                "##########...#",
                "##########...#",
                "##########...#",
                "##########...#",
                "##########...#",
                "##############");
        b = build(wide, 11, 0, 8);
        route = b.field().route(b.graph().cellAt(4, 0, 2));
        // The inner corner is the wall corner at (10, 4): the route keeps the clearance of the halls' middle around it.
        RouteShapes.assertKeepsClear(b.graph(), b.field(), route, 1.4);
    }

    // ---- stairways ------------------------------------------------------------------------------------------------------

    /**
     * A straight stairway {@code width} wide along +x: lower floor (feet 1) at x 1..5, steps of one block at x 6..9,
     * upper floor (feet 5) at x 10..20. The columns across the stair are different blocks: stairs at z 1, full blocks at
     * z 2 and, in a three wide stair, slabs (half a block lower) at z 3.
     */
    static TestVoxels stairway(int width) {
        TestVoxels v = new TestVoxels(24, 14);
        v.fill(0, 0, 0, 23, 13, 0, BlockCode.fullBlock());
        v.fill(0, 0, width + 1, 23, 13, width + 1, BlockCode.fullBlock());
        v.fill(0, 0, 0, 0, 13, width + 1, BlockCode.fullBlock());
        v.fill(21, 0, 0, 21, 13, width + 1, BlockCode.fullBlock());
        v.fill(0, 13, 0, 23, 13, width + 1, BlockCode.fullBlock());
        for (int x = 1; x <= 20; x++) {
            int h = x <= 5 ? 0 : x <= 9 ? x - 5 : 4;
            boolean run = x >= 6 && x <= 9;
            for (int z = 1; z <= width; z++) {
                v.fill(x, 0, z, x, h, z, BlockCode.fullBlock());
                if (run && z == 1) {
                    v.set(x, h, z, BlockCode.stair());
                } else if (run && z == 3) {
                    v.set(x, h, z, BlockCode.solid(0, 8));
                }
            }
        }
        v.maxFeet = 12;
        return v;
    }

    private static void checkStairway(int width) {
        TestVoxels v = stairway(width);
        WalkGraph g = v.scan();
        IntList exits = new IntList();
        for (int z = 1; z <= width; z++) {
            exits.add(g.cellAt(20, 4, z));
        }
        RouteField field = RouteField.build(g, exits.toArray());
        double centre = 1.0 + width / 2.0;
        for (int z = 1; z <= width; z++) {
            Route route = field.route(g.cellAt(1, 0, z));
            assertNotNull(route, "width " + width + " from z " + z);
            RouteShapes.forEachPoint(route, (x, y, pz) -> {
                if (x >= 6.0 && x <= 18.0) {
                    assertEquals(centre, pz, 0.02, "width " + width + ": in the middle of the stairway at x " + x + " " + RouteShapes.describe(route));
                }
            });
            int inside = 0;
            for (int i = 0; i < route.length(); i++) {
                if (route.x(i) > 6.0F && route.x(i) < 10.0F) {
                    inside++;
                }
                if (i > 0) {
                    assertTrue(route.y(i) >= route.y(i - 1) - 1.0E-4F, "it only climbs");
                }
            }
            assertTrue(inside <= 2, "width " + width + ": the run is one or two straight pieces, not a zig-zag (" + inside + " points)");
            assertTrue(RouteShapes.turns(route, 10.0) <= 1, "width " + width + " from z " + z + ": onto the middle line once, then "
                    + "straight; turns " + RouteShapes.turns(route, 10.0));
        }
    }

    @Test
    void aTwoWideStairwayOfMixedBlocksIsClimbedOnItsSeam() {
        checkStairway(2);
    }

    @Test
    void aThreeWideStairwayOfMixedBlocksIsClimbedThroughItsMiddle() {
        checkStairway(3);
    }

    /**
     * Three quarters of a two wide square spiral around a 2 x 2 pillar, rising half a block per block of its middle line
     * (slabs and full blocks), like the Gilded stair. The middle line runs (3, 7) -> (3, 3) -> (7, 3) -> (7, 7).
     */
    static TestVoxels spiral() {
        TestVoxels v = new TestVoxels(10, 12);
        v.fill(0, 0, 0, 9, 11, 9, BlockCode.fullBlock());
        for (int x = 2; x <= 7; x++) {
            for (int z = 2; z <= 7; z++) {
                boolean pillar = x >= 4 && x <= 5 && z >= 4 && z <= 5;
                boolean blocked = x >= 4 && x <= 5 && z >= 6;
                if (pillar || blocked) {
                    continue;
                }
                int s16 = 16 + 8 * (int) Math.floor(spiralS(x + 0.5, z + 0.5));
                for (int y = 0; y <= 10; y++) {
                    v.set(x, y, z, BlockCode.AIR);
                }
                for (int y = 0; y < s16 / 16; y++) {
                    v.set(x, y, z, BlockCode.fullBlock());
                }
                if (s16 % 16 == 8) {
                    v.set(x, s16 / 16, z, BlockCode.solid(0, 8));
                }
            }
        }
        v.maxFeet = 10;
        return v;
    }

    /** Position along the spiral's middle line of the point nearest to (x, z). */
    static double spiralS(double x, double z) {
        double[][] p = {{3, 7}, {3, 3}, {7, 3}, {7, 7}};
        double best = Double.POSITIVE_INFINITY;
        double bestS = 0;
        double s = 0;
        for (int i = 1; i < p.length; i++) {
            double dx = p[i][0] - p[i - 1][0];
            double dz = p[i][1] - p[i - 1][1];
            double len = Math.hypot(dx, dz);
            double t = Math.max(0, Math.min(1, ((x - p[i - 1][0]) * dx + (z - p[i - 1][1]) * dz) / (len * len)));
            double d = Math.hypot(x - (p[i - 1][0] + dx * t), z - (p[i - 1][1] + dz * t));
            if (d < best - 1.0E-9) {
                best = d;
                bestS = s + t * len;
            }
            s += len;
        }
        return bestS;
    }

    static double spiralOffset(double x, double z) {
        double[][] p = {{3, 7}, {3, 3}, {7, 3}, {7, 7}};
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < p.length; i++) {
            double dx = p[i][0] - p[i - 1][0];
            double dz = p[i][1] - p[i - 1][1];
            double len2 = dx * dx + dz * dz;
            double t = Math.max(0, Math.min(1, ((x - p[i - 1][0]) * dx + (z - p[i - 1][1]) * dz) / len2));
            best = Math.min(best, Math.hypot(x - (p[i - 1][0] + dx * t), z - (p[i - 1][1] + dz * t)));
        }
        return best;
    }

    @Test
    void aSpiralStairIsFollowedAlongItsMiddle() {
        TestVoxels v = spiral();
        WalkGraph g = v.scan();
        IntList exits = new IntList();
        for (int x = 6; x <= 7; x++) {
            int c = g.findCell(x, 7, 16 + 8 * (int) Math.floor(spiralS(x + 0.5, 7.5)), 4);
            assertTrue(c >= 0, "the top of the spiral is walkable");
            exits.add(c);
        }
        RouteField field = RouteField.build(g, exits.toArray());
        int start = g.findCell(2, 7, 16, 4);
        assertTrue(start >= 0);
        Route route = field.route(start);
        assertNotNull(route, "the spiral is climbable");
        RouteShapes.forEachPoint(route, (x, y, z) -> {
            double s = spiralS(x, z);
            double fromCorner = Math.min(Math.abs(s - 4.0), Math.abs(s - 8.0));
            if (s > 1.5 && s < 10.5 && fromCorner > 1.5) {
                assertTrue(spiralOffset(x, z) <= 0.02, String.format("on the middle line at (%.2f, %.2f), off by %.3f", x, z, spiralOffset(x, z)));
            }
            assertTrue(spiralOffset(x, z) <= 0.75, String.format("near the middle line at (%.2f, %.2f)", x, z));
        });
        assertTrue(RouteShapes.turns(route, 10.0) <= 6, "two corners, no zig-zag: " + RouteShapes.turns(route, 10.0) + " turns");
        for (int i = 1; i < route.length(); i++) {
            assertTrue(route.y(i) >= route.y(i - 1) - 1.0E-4F, "it only climbs");
        }
        RouteShapes.assertKeepsClear(g, field, route, 0.6);
    }

    // ---- steadiness -------------------------------------------------------------------------------------------------------

    @Test
    void aShownRouteIsKeptWhileThePlayerWalksAlongIt() {
        TestVoxels v = TestVoxels.plan(
                "########################",
                "#.......................",
                "#.......................",
                "########################");
        Built b = buildHallEnd(v, 23);
        Route first = b.field().route(b.graph().cellAt(2, 0, 1));
        Route shown = first;
        for (int x = 5; x <= 20; x++) {
            int z = 1 + (x % 2);
            Route kept = RouteField.follow(shown, x + 0.5, 1.0, z + 0.5);
            assertNotNull(kept, "kept at " + x + "," + z);
            assertEquals(first.x(first.length() - 1), kept.x(kept.length() - 1), 1.0E-4F, "same end");
            assertEquals(2.0F, kept.z(0), 1.0E-4F, "starts on the shown line");
            assertEquals(x + 0.5F, kept.x(0), 1.0E-4F, "starts beside the player");
            shown = kept;
        }
        // A step off the line (more than 0.6 blocks) asks for a new route.
        assertNull(RouteField.follow(first, 6.5, 1.0, 3.2));
    }

    /** A 20 x 3 lane: lower floor (feet 1) at x 0..7, four stair towers at x 8..11, upper floor (feet 5) at x 12..19. */
    static TestVoxels twoFloors(boolean middleStep) {
        TestVoxels v = new TestVoxels(20, 10);
        v.fill(0, 0, 0, 19, 0, 2, BlockCode.fullBlock());
        v.fill(0, 9, 0, 19, 9, 2, BlockCode.fullBlock());
        v.fill(0, 1, 0, 19, 8, 0, BlockCode.fullBlock());
        v.fill(0, 1, 2, 19, 8, 2, BlockCode.fullBlock());
        v.fill(12, 1, 1, 19, 4, 1, BlockCode.fullBlock());
        for (int s = 0; s < 4; s++) {
            if (s == 1 && !middleStep) {
                continue;
            }
            v.fill(8 + s, 1, 1, 8 + s, 1 + s, 1, BlockCode.fullBlock());
        }
        v.maxFeet = 8;
        return v;
    }
}
