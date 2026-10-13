package dev.hoyin1600p.arcanebeam.config.model;

public enum FadeInStyle {
    FADE("fade", "Fade In"),
    GROW("grow", "Grow In");

    public final String id;
    public final String label;

    FadeInStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static FadeInStyle fromId(String id) {
        for (FadeInStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }
}
