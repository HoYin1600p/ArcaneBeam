package dev.hoyin1600p.arcanebeam.config.model;

public enum VaultAltarSoundMode {
    DEFAULT("default", "Default"),
    ALTAR_1("altar_1", "Altar 1"),
    RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
    RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

    public final String id;
    public final String label;

    VaultAltarSoundMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static VaultAltarSoundMode fromId(String id) {
        for (VaultAltarSoundMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
