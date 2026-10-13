package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/**
 * Moves the end of the route from the top of the exit passage to its mouth.
 * <p>
 * The way out of a village labyrinth is a one wide passage (a stair shaft, a spiral stair or a ladder) that climbs
 * from a floor of the labyrinth to the surface. The route and the lines should end where that passage starts, on the
 * floor, and not run up into it: the player finds their own way up. The passage is made of connector cells that
 * belong to no floor ({@link FloorLayers}); the mouth is the first floor cells met when walking from the top of the
 * passage down into the labyrinth.
 */
final class ExitMouth {
    private ExitMouth() {
    }

    /**
     * The cells at the mouth of the passage that starts at {@code top}: the floor cells nearest to the top by walking
     * through connector cells. Returns {@code top} itself when it already lies on a floor or no floor is reached.
     */
    static int[] mouth(WalkGraph g, FloorLayers layers, int[] top) {
        if (top.length == 0) {
            return top;
        }
        for (int cell : top) {
            if (layers.layerOf[cell] >= 0) {
                return top;
            }
        }
        int[] dist = new int[g.count];
        Arrays.fill(dist, -1);
        int[] queue = new int[g.count];
        int head = 0;
        int tail = 0;
        for (int cell : top) {
            dist[cell] = 0;
            queue[tail++] = cell;
        }
        int found = Integer.MAX_VALUE;
        int[] mouth = new int[g.count];
        int count = 0;
        while (head < tail) {
            int c = queue[head++];
            if (dist[c] + 1 > found) {
                break;
            }
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (dist[d] >= 0) {
                    continue;
                }
                dist[d] = dist[c] + 1;
                if (layers.layerOf[d] >= 0) {
                    found = dist[d];
                    mouth[count++] = d;
                } else {
                    queue[tail++] = d;
                }
            }
        }
        return count == 0 ? top : Arrays.copyOf(mouth, count);
    }

    /**
     * The cells of the exit passage itself: the connector cells reached from {@code top} without stepping on a floor,
     * no lower than the mouth. No route is drawn from them (the player is on the way in or out). Empty when the top
     * already lies on a floor.
     */
    static boolean[] passage(WalkGraph g, FloorLayers layers, int[] top, int[] mouth) {
        boolean[] passage = new boolean[g.count];
        if (top.length == 0 || Arrays.equals(top, mouth)) {
            return passage;
        }
        int lowest = Integer.MAX_VALUE;
        for (int m : mouth) {
            lowest = Math.min(lowest, g.feetY(m));
        }
        int[] queue = new int[g.count];
        int tail = 0;
        for (int cell : top) {
            if (layers.layerOf[cell] < 0 && !passage[cell]) {
                passage[cell] = true;
                queue[tail++] = cell;
            }
        }
        for (int head = 0; head < tail; head++) {
            int c = queue[head];
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (!passage[d] && layers.layerOf[d] < 0 && g.feetY(d) >= lowest) {
                    passage[d] = true;
                    queue[tail++] = d;
                }
            }
        }
        return passage;
    }
}
