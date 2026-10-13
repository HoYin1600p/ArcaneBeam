package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * The block codes of a labyrinth box, kept current block by block. Pure bookkeeping: blocks are read through a
 * {@link VoxelSampler} in box-local coordinates, so the incremental update can be checked against a full scan without
 * a game. The box starts at world column ({@code originX}, {@code originZ}) and is {@code size} wide.
 */
final class BoxSnapshot {
    final int size;
    final int height;
    final int originX;
    final int originZ;
    private final int[] codes;
    private int sampledLayers;

    BoxSnapshot(int size, int height, int originX, int originZ) {
        this.size = size;
        this.height = height;
        this.originX = originX;
        this.originZ = originZ;
        this.codes = new int[size * size * height];
    }

    /** Samples up to {@code layers} more layers of the box; true once the whole box has been read. */
    boolean sampleLayers(VoxelSampler sampler, int layers) {
        int end = Math.min(height, sampledLayers + layers);
        for (int y = sampledLayers; y < end; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    codes[(y * size + z) * size + x] = sampler.sample(x, y, z);
                }
            }
        }
        sampledLayers = end;
        return sampledLayers >= height;
    }

    boolean complete() {
        return sampledLayers >= height;
    }

    /**
     * Reads the block at the world position again; true when its code changed. {@code worldY} is box-local
     * ({@code y - originY}) because the box origin's height is the caller's business.
     */
    boolean reread(VoxelSampler sampler, int worldX, int boxY, int worldZ) {
        int x = worldX - originX;
        int z = worldZ - originZ;
        if (x < 0 || z < 0 || x >= size || z >= size || boxY < 0 || boxY >= height) {
            return false;
        }
        int code = sampler.sample(x, boxY, z);
        int index = (boxY * size + z) * size + x;
        if (codes[index] == code) {
            return false;
        }
        codes[index] = code;
        return true;
    }

    /** Reads again every block of the box that lies in the chunk; always counts as a change. */
    void rereadChunk(VoxelSampler sampler, int chunkX, int chunkZ) {
        int minX = Math.max(0, chunkX * 16 - originX);
        int maxX = Math.min(size - 1, chunkX * 16 + 15 - originX);
        int minZ = Math.max(0, chunkZ * 16 - originZ);
        int maxZ = Math.min(size - 1, chunkZ * 16 + 15 - originZ);
        for (int y = 0; y < height; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    codes[(y * size + z) * size + x] = sampler.sample(x, y, z);
                }
            }
        }
    }

    int code(int x, int y, int z) {
        return codes[(y * size + z) * size + x];
    }

    int unknownBlocks() {
        int unknown = 0;
        for (int code : codes) {
            if (BlockCode.unknownBlock(code)) {
                unknown++;
            }
        }
        return unknown;
    }

    /** A copy of the codes, safe to hand to the build thread. */
    int[] copy() {
        return codes.clone();
    }
}
