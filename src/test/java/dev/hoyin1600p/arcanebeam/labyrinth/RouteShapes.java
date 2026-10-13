package dev.hoyin1600p.arcanebeam.labyrinth;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks on the shape of a route, measured against the blocked columns of a one-floor test plan. */
final class RouteShapes {
    private RouteShapes() {
    }

    /**
     * Every point of the route (sampled every 0.05 block; the first half block and the last block excepted) keeps at least
     * {@code min} from every column of the floor of {@code start} that holds no cell.
     */
    static void assertKeepsClear(WalkGraph g, RouteField field, Route route, double min) {
        double total = horizontalLength(route);
        double walked = 0.0;
        for (int i = 1; i < route.length(); i++) {
            double x0 = route.x(i - 1);
            double z0 = route.z(i - 1);
            double x1 = route.x(i);
            double z1 = route.z(i);
            double len = Math.hypot(x1 - x0, z1 - z0);
            int steps = Math.max(1, (int) Math.ceil(len / 0.05));
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                double x = x0 + (x1 - x0) * t;
                double z = z0 + (z1 - z0) * t;
                if (walked + len * t < 0.5 || walked + len * t > total - 1.0) {
                    continue;
                }
                double clear = clearance(g, x, z);
                assertTrue(clear >= min - 1.0E-6, String.format("the route passes %.3f from a wall at (%.2f, %.2f)", clear, x, z));
            }
            walked += len;
        }
    }

    /** Distance from (x, z) to the nearest column (within 4 blocks) that has no cell at any height. */
    static double clearance(WalkGraph g, double x, double z) {
        double best = 9.0;
        int cx = (int) Math.floor(x);
        int cz = (int) Math.floor(z);
        for (int bz = cz - 4; bz <= cz + 4; bz++) {
            for (int bx = cx - 4; bx <= cx + 4; bx++) {
                if (hasCell(g, bx, bz)) {
                    continue;
                }
                double dx = Math.max(Math.max(bx - x, 0.0), x - (bx + 1));
                double dz = Math.max(Math.max(bz - z, 0.0), z - (bz + 1));
                best = Math.min(best, Math.hypot(dx, dz));
            }
        }
        return best;
    }

    static boolean hasCell(WalkGraph g, int x, int z) {
        for (int y = 0; y < g.height; y++) {
            if (g.cellAt(x, y, z) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Number of vertices where the route turns by more than {@code degrees} (horizontally). */
    static int turns(Route route, double degrees) {
        int turns = 0;
        for (int i = 1; i + 1 < route.length(); i++) {
            double ax = route.x(i) - route.x(i - 1);
            double az = route.z(i) - route.z(i - 1);
            double bx = route.x(i + 1) - route.x(i);
            double bz = route.z(i + 1) - route.z(i);
            if (Math.hypot(ax, az) < 1.0E-4 || Math.hypot(bx, bz) < 1.0E-4) {
                continue;
            }
            double angle = Math.toDegrees(Math.abs(Math.atan2(ax * bz - az * bx, ax * bx + az * bz)));
            if (angle > degrees) {
                turns++;
            }
        }
        return turns;
    }

    /** The route's points, for failure messages. */
    static String describe(Route route) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < route.length(); i++) {
            b.append(String.format(java.util.Locale.ROOT, "(%.2f, %.2f, %.2f)", route.x(i), route.y(i), route.z(i)));
        }
        return b.append(']').toString();
    }

    /** The horizontal length of the route. */
    static double horizontalLength(Route route) {
        double sum = 0.0;
        for (int i = 1; i < route.length(); i++) {
            sum += Math.hypot(route.x(i) - route.x(i - 1), route.z(i) - route.z(i - 1));
        }
        return sum;
    }

    /** Calls {@code check} with (x, y, z) every 0.05 block along the route. */
    static void forEachPoint(Route route, PointCheck check) {
        for (int i = 1; i < route.length(); i++) {
            double len = Math.hypot(route.x(i) - route.x(i - 1), route.z(i) - route.z(i - 1));
            int steps = Math.max(1, (int) Math.ceil(len / 0.05));
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                check.at(route.x(i - 1) + (route.x(i) - route.x(i - 1)) * t, route.y(i - 1) + (route.y(i) - route.y(i - 1)) * t,
                        route.z(i - 1) + (route.z(i) - route.z(i - 1)) * t);
            }
        }
    }

    @FunctionalInterface
    interface PointCheck {
        void at(double x, double y, double z);
    }
}
