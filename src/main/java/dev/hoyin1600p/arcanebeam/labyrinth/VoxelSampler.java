package dev.hoyin1600p.arcanebeam.labyrinth;

/** Reads the blocks of the scan box in box-local coordinates; the game implementation lives in {@code LevelSampler}. */
@FunctionalInterface
public interface VoxelSampler {
    /** The {@link BlockCode} of the block at the box-local position. Positions are always inside the box. */
    int sample(int x, int y, int z);
}
