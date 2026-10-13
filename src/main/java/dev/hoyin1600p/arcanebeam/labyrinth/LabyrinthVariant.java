package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * The rooms that hold a labyrinth: the three Vault village challenge rooms with an underground maze and the
 * Wolds-only static labyrinth room. All are 47 by 47 blocks and sit with template y 0 at world Y 9.
 * <p>
 * Coordinates are template-local (before the unknown mirror and rotation).
 */
public enum LabyrinthVariant {
    /** village1: stair shaft from the surface down to the upper floor. */
    LIVING("the_vault:vault/rooms/challenge/village1", "Living village", 16, EntranceKind.SHAFT, new int[][]{{39, 16, 6}}),
    /** village2: spiral stair from the surface down to the lower floor; its ceiling is two blocks lower. */
    GILDED("the_vault:vault/rooms/challenge/village2", "Gilded village", 14, EntranceKind.SHAFT, new int[][]{{28, 14, 23}}),
    /** village3: a one-wide ladder from the surface down to the upper floor. */
    ORNATE("the_vault:vault/rooms/challenge/village3", "Ornate village", 16, EntranceKind.LADDER, new int[][]{{29, 16, 24}}),
    /** The Wolds challenge/labyrinth room: five maze levels; the room tunnels on all four sides are its exits. */
    WOLDS_LABYRINTH("the_vault:vault/rooms/challenge/labyrinth", "Wolds labyrinth", 45, EntranceKind.GATES, wolds());

    /** Template y 0 sits at this world Y, for every 47 wide room (the generator adds 9 when the room is at most 47 wide). */
    public static final int ROOM_Y_OFFSET = 9;

    /** How the exit of a labyrinth looks, for matching it in the scan. */
    public enum EntranceKind {
        /** A stair shaft open to the surface above; the top cell of the shaft is the exit. */
        SHAFT,
        /** A ladder shaft; the top ladder cell is the exit. */
        LADDER,
        /** Open room tunnels on the four sides at the same height; every tunnel mouth is an exit. */
        GATES
    }

    public final String templateId;
    public final String label;
    /** The highest feet level of the labyrinth; cells above it belong to the surface and are dropped. */
    public final int topFeetY;
    public final EntranceKind entranceKind;
    private final int[][] entrance;

    LabyrinthVariant(String templateId, String label, int topFeetY, EntranceKind entranceKind, int[][] entrance) {
        this.templateId = templateId;
        this.label = label;
        this.topFeetY = topFeetY;
        this.entranceKind = entranceKind;
        this.entrance = entrance;
    }

    /** Blocks scanned vertically: the labyrinth plus headroom above its top so the shaft mouth is seen as open. */
    public int scanHeight() {
        return this == WOLDS_LABYRINTH ? RoomTransform.ROOM : topFeetY + 3;
    }

    /** The template-local entrance cells as {@code {x, feetY, z}}; a copy. */
    public int[][] entranceCells() {
        int[][] copy = new int[entrance.length][];
        for (int i = 0; i < entrance.length; i++) {
            copy[i] = entrance[i].clone();
        }
        return copy;
    }

    /** The variant of a room template id such as {@code the_vault:vault/rooms/challenge/village2}, or null. */
    public static LabyrinthVariant fromTemplateId(String id) {
        if (id == null) {
            return null;
        }
        for (LabyrinthVariant variant : values()) {
            if (variant.templateId.equals(id)) {
                return variant;
            }
        }
        return null;
    }

    private static int[][] wolds() {
        int[][] gates = new int[12][];
        int n = 0;
        for (int i = 22; i <= 24; i++) {
            gates[n++] = new int[]{i, 22, 0};
            gates[n++] = new int[]{i, 22, RoomTransform.ROOM - 1};
            gates[n++] = new int[]{0, 22, i};
            gates[n++] = new int[]{RoomTransform.ROOM - 1, 22, i};
        }
        return gates;
    }
}
