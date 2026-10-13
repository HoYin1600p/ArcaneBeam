package dev.hoyin1600p.arcanebeam.labyrinth;

import net.minecraft.world.phys.AABB;

/**
 * One drawable set of {@link LinePieces} (the centre lines, or the route) with its line-of-sight cache, the frustum
 * bounds of each piece and the cached light value of each piece. Built on the client thread.
 */
final class PieceLayer {
    /** Packed light of full brightness (block 15, sky 15). */
    static final int FULL_LIGHT = 15728880;

    static final PieceLayer EMPTY = new PieceLayer(LinePieces.EMPTY);
    private static final double ADOPT_RADIUS = 0.33;

    final LinePieces pieces;
    final PieceCuller culler;
    final int[] light;
    private AABB[] bounds;
    private float boundsElevation = Float.NaN;
    private float boundsMargin = Float.NaN;

    PieceLayer(LinePieces pieces) {
        this.pieces = pieces;
        this.culler = new PieceCuller(pieces.count);
        this.light = new int[pieces.count];
        java.util.Arrays.fill(light, FULL_LIGHT);
        this.bounds = new AABB[pieces.count];
    }

    /**
     * Copies the cached visibility and light of the pieces of {@code old} that lie in the same place as pieces of
     * this layer (same midpoint within a third of a block), so a rebuilt route or network does not blink.
     */
    void adoptFrom(PieceLayer old) {
        if (old.pieces.count == 0 || pieces.count == 0) {
            return;
        }
        java.util.HashMap<Long, java.util.ArrayList<Integer>> cells = new java.util.HashMap<>();
        for (int j = 0; j < old.pieces.count; j++) {
            cells.computeIfAbsent(cellKey(old.pieces.midX(j), old.pieces.midY(j), old.pieces.midZ(j)), k -> new java.util.ArrayList<>(2)).add(j);
        }
        for (int i = 0; i < pieces.count; i++) {
            double x = pieces.midX(i);
            double y = pieces.midY(i);
            double z = pieces.midZ(i);
            int found = -1;
            for (int dx = -1; dx <= 1 && found < 0; dx++) {
                for (int dy = -1; dy <= 1 && found < 0; dy++) {
                    for (int dz = -1; dz <= 1 && found < 0; dz++) {
                        java.util.ArrayList<Integer> bucket = cells.get(cellKey(x + dx * 0.5, y + dy * 0.5, z + dz * 0.5));
                        if (bucket == null) {
                            continue;
                        }
                        for (int j : bucket) {
                            double ex = old.pieces.midX(j) - x;
                            double ey = old.pieces.midY(j) - y;
                            double ez = old.pieces.midZ(j) - z;
                            if (ex * ex + ey * ey + ez * ez < ADOPT_RADIUS * ADOPT_RADIUS) {
                                found = j;
                                break;
                            }
                        }
                    }
                }
            }
            if (found >= 0) {
                culler.adopt(i, old.culler, found);
                light[i] = old.light[found];
            }
        }
    }

    private static long cellKey(double x, double y, double z) {
        long qx = (long) Math.floor(x * 2.0);
        long qy = (long) Math.floor(y * 2.0);
        long qz = (long) Math.floor(z * 2.0);
        return (qx & 0x1FFFFF) | ((qy & 0x1FFFFF) << 21) | ((qz & 0x1FFFFF) << 42);
    }

    AABB bounds(int piece) {
        return bounds[piece];
    }

    /** Rebuilds the frustum bounds when the elevation or the bar size changed. */
    void ensureBounds(float elevation, float margin) {
        if (elevation == boundsElevation && margin == boundsMargin) {
            return;
        }
        boundsElevation = elevation;
        boundsMargin = margin;
        for (int i = 0; i < pieces.count; i++) {
            bounds[i] = new AABB(
                    Math.min(pieces.startX(i), pieces.endX(i)) - margin,
                    Math.min(pieces.startY(i), pieces.endY(i)) + elevation - margin,
                    Math.min(pieces.startZ(i), pieces.endZ(i)) - margin,
                    Math.max(pieces.startX(i), pieces.endX(i)) + margin,
                    Math.max(pieces.startY(i), pieces.endY(i)) + elevation + margin,
                    Math.max(pieces.startZ(i), pieces.endZ(i)) + margin);
        }
    }
}
