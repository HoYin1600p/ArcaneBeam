package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * The walkable cells of a scanned box and how they connect. A cell is a place a player can stand: it is identified
 * by its column (x, z) and the block {@code sy} that supports it (for a ladder with nothing under it, the block
 * below the climbed block). {@code s16} is the standing surface in sixteenths of a block, box-local.
 * <p>
 * Immutable once built by {@link WalkScanner}; safe to share between threads.
 */
public final class WalkGraph {
    public final int size;
    public final int height;
    public final int count;
    final short[] x;
    final short[] sy;
    final short[] z;
    final short[] s16;
    final boolean[] climb;
    /** CSR adjacency: neighbours of cell c are {@code adj[adjStart[c] .. adjStart[c + 1])}. */
    final int[] adjStart;
    final int[] adj;
    /** Dense lookup {@code (sy * size + z) * size + x -> cell id or -1}. */
    private final int[] index;
    /** Number of scanned blocks that were not loaded; their cells are missing from the graph. */
    public final int unknownBlocks;

    WalkGraph(int size, int height, int count, short[] x, short[] sy, short[] z, short[] s16, boolean[] climb,
            int[] adjStart, int[] adj, int[] index, int unknownBlocks) {
        this.size = size;
        this.height = height;
        this.count = count;
        this.x = x;
        this.sy = sy;
        this.z = z;
        this.s16 = s16;
        this.climb = climb;
        this.adjStart = adjStart;
        this.adj = adj;
        this.index = index;
        this.unknownBlocks = unknownBlocks;
    }

    public int x(int cell) {
        return x[cell];
    }

    public int z(int cell) {
        return z[cell];
    }

    /** The block that supports the cell. */
    public int supportY(int cell) {
        return sy[cell];
    }

    public int surface16(int cell) {
        return s16[cell];
    }

    /** The standing surface height, box-local, in blocks. */
    public double surface(int cell) {
        return s16[cell] / 16.0;
    }

    /** The block the player's feet are in: the integer part of the surface height. */
    public int feetY(int cell) {
        return s16[cell] >> 4;
    }

    public boolean climbable(int cell) {
        return climb[cell];
    }

    public int degree(int cell) {
        return adjStart[cell + 1] - adjStart[cell];
    }

    public int neighbour(int cell, int i) {
        return adj[adjStart[cell] + i];
    }

    /** The cell supported by block {@code csy} in column (x, z), or -1. */
    public int cellAt(int cx, int csy, int cz) {
        if (cx < 0 || cz < 0 || cx >= size || csy < 0 || csy >= height || cz >= size) {
            return -1;
        }
        return index[(csy * size + cz) * size + cx];
    }

    /**
     * The cell of column (x, z) whose surface is closest to {@code surface16}, or -1 when none is within
     * {@code tolerance16}. Used to place the player (feet height times 16).
     */
    public int findCell(int cx, int cz, int surface16, int tolerance16) {
        if (cx < 0 || cz < 0 || cx >= size || cz >= size) {
            return -1;
        }
        int best = -1;
        int bestDelta = Integer.MAX_VALUE;
        for (int level = 0; level < height; level++) {
            int cell = index[(level * size + cz) * size + cx];
            if (cell < 0) {
                continue;
            }
            int delta = Math.abs(s16[cell] - surface16);
            if (delta <= tolerance16 && delta < bestDelta) {
                best = cell;
                bestDelta = delta;
            }
        }
        return best;
    }

    /** True when {@code a} and {@code b} are linked by one move. */
    public boolean linked(int a, int b) {
        for (int i = adjStart[a]; i < adjStart[a + 1]; i++) {
            if (adj[i] == b) {
                return true;
            }
        }
        return false;
    }
}
