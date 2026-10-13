package dev.hoyin1600p.arcanebeam.config.model;

public enum StormArrowProjectileSoundMode {
    DEFAULT("default", "Default"),
    OPTION_1("option_1", "Option 1"),
    RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
    RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

    public final String id;
    public final String label;

    StormArrowProjectileSoundMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static StormArrowProjectileSoundMode fromId(String id) {
        for (StormArrowProjectileSoundMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
