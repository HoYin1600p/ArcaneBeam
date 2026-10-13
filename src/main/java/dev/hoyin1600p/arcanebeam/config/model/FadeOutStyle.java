package dev.hoyin1600p.arcanebeam.config.model;

public enum FadeOutStyle {
    FADE("fade", "Fade Out"),
    SHRINK("shrink", "Shrink Out");

    public final String id;
    public final String label;

    FadeOutStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static FadeOutStyle fromId(String id) {
        for (FadeOutStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }
}
