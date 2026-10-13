package dev.hoyin1600p.arcanebeam.config.model;

public enum LightningSoundMode {
    DEFAULT("default", "Default"),
    SEISMIC_CHARGE("seismic_charge", "Seismic Charge"),
    RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
    RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

    public final String id;
    public final String label;

    LightningSoundMode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static LightningSoundMode fromId(String id) {
        for (LightningSoundMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
