package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole pure pipeline on a synthetic 47 by 47 room: scan, exit, lines, route. */
class LabyrinthModelTest {
    private static final int N = RoomTransform.ROOM;

    /**
     * A Living-like room made of three 3-wide halls cut out of solid rock (floor at y 0, ceiling at y 16): the west
     * hall at x 5..7, the south hall at z 40..42 and the east hall at x 37..39, all joined. The exit is a ladder at
     * the template position (39, 6) that climbs through a hole in the ceiling. Everything is placed through
     * {@code transform}.
     */
    private static int[] room(RoomTransform transform, boolean withExit) {
        TestVoxels v = new TestVoxels(N, LabyrinthVariant.LIVING.scanHeight());
        v.fill(0, 0, 0, N - 1, 0, N - 1, BlockCode.fullBlock());
        v.fill(0, 16, 0, N - 1, 16, N - 1, BlockCode.fullBlock());
        for (int z = 0; z < N; z++) {
            for (int x = 0; x < N; x++) {
                boolean hall = (x >= 5 && x <= 7 && z >= 5 && z <= 42) || (z >= 40 && z <= 42 && x >= 5 && x <= 39)
                        || (x >= 37 && x <= 39 && z >= 5 && z <= 42);
                if (!hall) {
                    int[] p = transform.apply(x, z);
                    v.fill(p[0], 1, p[1], p[0], 15, p[1], BlockCode.fullBlock());
                }
            }
        }
        if (withExit) {
            int[] exit = transform.apply(39, 6);
            v.fill(exit[0], 1, exit[1], exit[0], 16, exit[1], BlockCode.ladder());
        }
        return v.copyCodes();
    }

    @Test
    void theRouteFollowsTheHallsToTheExitInEveryOrientation() {
        for (RoomTransform t : RoomTransform.all()) {
            LabyrinthModel model = LabyrinthModel.build(room(t, true), LabyrinthVariant.LIVING, 0);
            assertEquals(EntranceLocator.Method.TRANSFORM, model.exit.method(), t.toString());
            int[] start = t.apply(6, 10);
            int cell = model.graph.cellAt(start[0], 0, start[1]);
            assertTrue(cell >= 0, "the player's cell exists");
            assertTrue(model.field.reachable(cell), t.toString());
            Route route = model.field.route(cell);
            assertNotNull(route);
            // Down the west hall, along the south hall, up the east hall: the route has vertices in the south hall.
            boolean throughSouthHall = false;
            for (int i = 0; i < route.length(); i++) {
                int[] back = inverse(t, route.x(i), route.z(i));
                throughSouthHall |= back[1] >= 40;
            }
            assertTrue(throughSouthHall, "route reaches the south hall in " + t);
            int[] exitColumn = t.apply(39, 6);
            int last = route.length() - 1;
            assertEquals(exitColumn[0] + 0.5F, route.x(last), 1.1F);
            assertEquals(exitColumn[1] + 0.5F, route.z(last), 1.1F);
            assertEquals(start[0] + 0.5F, route.x(0), 0.01F);
            assertEquals(start[1] + 0.5F, route.z(0), 0.01F);
            assertTrue(route.blocks() > 60.0, "a long way round: " + route.blocks());
        }
    }

    @Test
    void theRouteRunsDownTheMiddleOfEveryHall() {
        LabyrinthModel model = LabyrinthModel.build(room(new RoomTransform(false, 0), true), LabyrinthVariant.LIVING, 0);
        Route route = model.field.route(model.graph.cellAt(6, 0, 8));
        assertNotNull(route);
        assertTrue(hasSegment(route, true, 6.5F, 20.0F), "down the middle of the west hall");
        assertTrue(hasSegment(route, false, 41.5F, 20.0F), "along the middle of the south hall");
        assertTrue(hasSegment(route, true, 38.5F, 20.0F), "up the middle of the east hall");
    }

    @Test
    void nothingIsShownFromTheExitLadder() {
        LabyrinthModel model = LabyrinthModel.build(room(new RoomTransform(false, 0), true), LabyrinthVariant.LIVING, 0);
        int shown = 0;
        for (int c = 0; c < model.graph.count; c++) {
            if (model.graph.x(c) == 39 && model.graph.z(c) == 6 && model.graph.feetY(c) >= 2) {
                assertTrue(model.inPassage(c), "ladder rung at feet " + model.graph.feetY(c) + " is passage");
                assertNull(model.field.route(c));
                shown++;
            }
        }
        assertTrue(shown > 5, "the ladder has rungs");
    }

    /** True when one route piece runs along the given x (or z) for at least {@code span} blocks. */
    private static boolean hasSegment(Route route, boolean alongZ, float fixed, float span) {
        for (int i = 1; i < route.length(); i++) {
            float a = alongZ ? route.x(i - 1) : route.z(i - 1);
            float b = alongZ ? route.x(i) : route.z(i);
            float from = alongZ ? route.z(i - 1) : route.x(i - 1);
            float to = alongZ ? route.z(i) : route.x(i);
            if (Math.abs(a - fixed) < 0.01F && Math.abs(b - fixed) < 0.01F && Math.abs(to - from) >= span) {
                return true;
            }
        }
        return false;
    }

    /** The template column of a placed world column, by searching the 8 images. */
    private static int[] inverse(RoomTransform t, float x, float z) {
        int px = (int) Math.floor(x);
        int pz = (int) Math.floor(z);
        for (int tx = 0; tx < N; tx++) {
            for (int tz = 0; tz < N; tz++) {
                int[] p = t.apply(tx, tz);
                if (p[0] == px && p[1] == pz) {
                    return new int[]{tx, tz};
                }
            }
        }
        throw new IllegalArgumentException();
    }

    @Test
    void nothingAboveTheTopLevelIsPartOfTheModel() {
        LabyrinthModel model = LabyrinthModel.build(room(new RoomTransform(false, 0), true), LabyrinthVariant.LIVING, 0);
        for (int c = 0; c < model.graph.count; c++) {
            assertTrue(model.graph.feetY(c) <= LabyrinthVariant.LIVING.topFeetY);
        }
    }

    @Test
    void withoutAnExitThereIsNoRoute() {
        LabyrinthModel model = LabyrinthModel.build(room(new RoomTransform(false, 0), false), LabyrinthVariant.LIVING, 0);
        assertEquals(EntranceLocator.Method.NONE, model.exit.method());
        assertTrue(model.graph.count > 0);
        assertNull(model.field.route(model.graph.cellAt(6, 0, 10)));
    }
}
