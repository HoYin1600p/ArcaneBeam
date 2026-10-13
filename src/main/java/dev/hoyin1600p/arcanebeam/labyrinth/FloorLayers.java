package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Splits the walkable cells into floors so each floor can be flattened to a 2D mask and thinned.
 * <p>
 * A flat patch of cells at one surface height is a plateau. Large plateaus are floors; large plateaus that touch
 * with a step of at most one block join into one floor as long as no column would hold two cells of it. Small
 * leftovers within half a block of a floor (slabs, carpet) join it too. Everything else (the steps of a stair,
 * a ladder shaft) belongs to no floor: those cells are connectors, and the network crosses them with a path.
 */
final class FloorLayers {
    /** Plateaus with fewer cells than this are not floors on their own. */
    static final int MAJOR_PLATEAU = 6;
    private static final int HALF_STEP16 = 8;
    private static final int STEP16 = 16;

    /** Floor id of every cell, or -1 for a connector cell. */
    final int[] layerOf;
    final int layerCount;

    private FloorLayers(int[] layerOf, int layerCount) {
        this.layerOf = layerOf;
        this.layerCount = layerCount;
    }

    static FloorLayers compute(WalkGraph g) {
        int n = g.count;
        int[] plateau = new int[n];
        Arrays.fill(plateau, -1);
        IntList sizes = new IntList();
        int[] queue = new int[n];
        for (int start = 0; start < n; start++) {
            if (plateau[start] >= 0) {
                continue;
            }
            int id = sizes.size();
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            plateau[start] = id;
            while (head < tail) {
                int c = queue[head++];
                for (int i = 0; i < g.degree(c); i++) {
                    int d = g.neighbour(c, i);
                    if (plateau[d] < 0 && g.surface16(d) == g.surface16(c)) {
                        plateau[d] = id;
                        queue[tail++] = d;
                    }
                }
            }
            sizes.add(tail);
        }

        int plateaus = sizes.size();
        int[] parent = new int[plateaus];
        BitSet[] columns = new BitSet[plateaus];
        for (int p = 0; p < plateaus; p++) {
            parent[p] = p;
            if (sizes.get(p) >= MAJOR_PLATEAU) {
                columns[p] = new BitSet(g.size * g.size);
            }
        }
        for (int c = 0; c < n; c++) {
            if (columns[plateau[c]] != null) {
                columns[plateau[c]].set(g.z(c) * g.size + g.x(c));
            }
        }
        for (int c = 0; c < n; c++) {
            if (columns[plateau[c]] == null) {
                continue;
            }
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (d < c || columns[plateau[d]] == null || plateau[d] == plateau[c]) {
                    continue;
                }
                if (Math.abs(g.surface16(d) - g.surface16(c)) > STEP16 || sameColumn(g, c, d)) {
                    continue;
                }
                int ra = find(parent, plateau[c]);
                int rb = find(parent, plateau[d]);
                if (ra == rb || columns[ra].intersects(columns[rb])) {
                    continue;
                }
                parent[rb] = ra;
                columns[ra].or(columns[rb]);
            }
        }

        int[] layerOf = new int[n];
        Arrays.fill(layerOf, -1);
        int[] rootToLayer = new int[plateaus];
        Arrays.fill(rootToLayer, -1);
        int layers = 0;
        int tail = 0;
        for (int c = 0; c < n; c++) {
            if (columns[plateau[c]] == null) {
                continue;
            }
            int root = find(parent, plateau[c]);
            if (rootToLayer[root] < 0) {
                rootToLayer[root] = layers++;
            }
            layerOf[c] = rootToLayer[root];
            queue[tail++] = c;
        }
        BitSet[] layerColumns = new BitSet[layers];
        for (int p = 0; p < plateaus; p++) {
            if (columns[p] != null && find(parent, p) == p) {
                layerColumns[rootToLayer[p]] = columns[p];
            }
        }

        // Absorb half-step leftovers (slabs, carpet) into the floor they border.
        int head = 0;
        while (head < tail) {
            int c = queue[head++];
            int layer = layerOf[c];
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (layerOf[d] >= 0 || sameColumn(g, c, d)) {
                    continue;
                }
                if (Math.abs(g.surface16(d) - g.surface16(c)) > HALF_STEP16) {
                    continue;
                }
                int column = g.z(d) * g.size + g.x(d);
                if (layerColumns[layer].get(column)) {
                    continue;
                }
                layerColumns[layer].set(column);
                layerOf[d] = layer;
                queue[tail++] = d;
            }
        }
        return new FloorLayers(layerOf, layers);
    }

    private static boolean sameColumn(WalkGraph g, int a, int b) {
        return g.x(a) == g.x(b) && g.z(a) == g.z(b);
    }

    private static int find(int[] parent, int p) {
        while (parent[p] != p) {
            parent[p] = parent[parent[p]];
            p = parent[p];
        }
        return p;
    }
}
