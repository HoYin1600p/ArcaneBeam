package dev.hoyin1600p.arcanebeam.config.model;

public enum StormArrowSoundMode {
    DEFAULT("default", "Default"),
    BLASTER("blaster", "Blaster"),
    RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
    RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

    public final String id;
    public final String label;

    StormArrowSoundMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static StormArrowSoundMode fromId(String id) {
        for (StormArrowSoundMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
