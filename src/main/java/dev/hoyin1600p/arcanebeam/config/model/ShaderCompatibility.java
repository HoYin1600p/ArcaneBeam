package dev.hoyin1600p.arcanebeam.config.model;

public enum ShaderCompatibility {
    OFF("off", "Off"),
    ON("on", "On");

    public final String id;
    public final String label;

    ShaderCompatibility(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static ShaderCompatibility fromId(String id) {
        for (ShaderCompatibility compatibility : values()) {
            if (compatibility.id.equals(id)) {
                return compatibility;
            }
        }
        return null;
    }
}
