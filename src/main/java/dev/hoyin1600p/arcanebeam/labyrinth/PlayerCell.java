package dev.hoyin1600p.arcanebeam.labyrinth;

/** Places the player on the cell graph. */
final class PlayerCell {
    /** Height tolerance, in sixteenths (1.25 blocks): covers the top of a jump and the depth of a shallow channel. */
    static final int TOLERANCE16 = 20;

    private PlayerCell() {
    }

    /**
     * The cell under the player: the one in the column of the player's block position whose surface is closest to the
     * feet, or -1 (mid-air, deep water, on a block that has no cell). World coordinates; the box starts at
     * ({@code originX}, {@code originY}, {@code originZ}).
     */
    static int find(WalkGraph graph, double x, double y, double z, int originX, int originY, int originZ) {
        int lx = (int) Math.floor(x) - originX;
        int lz = (int) Math.floor(z) - originZ;
        int feet16 = (int) Math.round((y - originY) * 16.0);
        return graph.findCell(lx, lz, feet16, TOLERANCE16);
    }
}
