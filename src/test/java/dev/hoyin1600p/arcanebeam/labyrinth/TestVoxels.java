package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/** A small mutable block box for the scan tests: everything is air until set. */
final class TestVoxels implements VoxelSampler {
    final int size;
    final int height;
    private final int[] codes;
    /** Cells with feet above this are dropped, like the roof over a real labyrinth. */
    int maxFeet;

    TestVoxels(int size, int height) {
        this.size = size;
        this.height = height;
        this.codes = new int[size * size * height];
        this.maxFeet = height;
    }

    TestVoxels set(int x, int y, int z, int code) {
        codes[(y * size + z) * size + x] = code;
        return this;
    }

    /** Fills the inclusive box with the code. */
    TestVoxels fill(int x0, int y0, int z0, int x1, int y1, int z1, int code) {
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    set(x, y, z, code);
                }
            }
        }
        return this;
    }

    @Override
    public int sample(int x, int y, int z) {
        return codes[(y * size + z) * size + x];
    }

    WalkGraph scan() {
        return WalkScanner.scan(this, size, height, maxFeet);
    }

    /**
     * Builds a one-floor box from an ASCII plan (rows are z, columns are x): '#' is a wall column from the floor
     * up to the ceiling, '.' is open floor; cells outside the plan are wall.
     * The floor block is at y = 0 (feet y 1) and the ceiling at y = 4.
     */
    static TestVoxels plan(String... rows) {
        int size = Math.max(rows.length, rows[0].length());
        TestVoxels v = new TestVoxels(size, 7);
        v.maxFeet = 3;
        v.fill(0, 0, 0, size - 1, 0, size - 1, BlockCode.fullBlock());
        v.fill(0, 4, 0, size - 1, 4, size - 1, BlockCode.fullBlock());
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                boolean open = z < rows.length && x < rows[z].length() && rows[z].charAt(x) != '#';
                if (!open) {
                    v.fill(x, 1, z, x, 3, z, BlockCode.fullBlock());
                }
            }
        }
        return v;
    }

    int[] copyCodes() {
        return Arrays.copyOf(codes, codes.length);
    }
}
