package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the 8 ways Vault can place a room template: a mirror (none or front-back) followed by 0 to 3 quarter turns
 * around the room centre. A 47 by 47 room maps onto itself under every one of them, so a template column (x, z)
 * lands on another column of the same room. The client does not know which one was used.
 */
public record RoomTransform(boolean mirror, int quarterTurns) {
    /** Side length of a Vault grid room, in blocks. */
    public static final int ROOM = 47;

    private static final List<RoomTransform> ALL = build();

    private static List<RoomTransform> build() {
        List<RoomTransform> all = new ArrayList<>(8);
        for (int mirrorFlag = 0; mirrorFlag < 2; mirrorFlag++) {
            for (int turns = 0; turns < 4; turns++) {
                all.add(new RoomTransform(mirrorFlag == 1, turns));
            }
        }
        return List.copyOf(all);
    }

    /** The 8 orientations, identity first. */
    public static List<RoomTransform> all() {
        return ALL;
    }

    /** The image of template column (x, z) in a room {@code size} blocks wide, as {@code {x, z}}. */
    public int[] apply(int x, int z, int size) {
        int last = size - 1;
        int px = mirror ? last - x : x;
        int pz = z;
        for (int i = 0; i < quarterTurns; i++) {
            int nx = last - pz;
            int nz = px;
            px = nx;
            pz = nz;
        }
        return new int[]{px, pz};
    }

    /** The image in a standard {@value #ROOM} block room. */
    public int[] apply(int x, int z) {
        return apply(x, z, ROOM);
    }
}
