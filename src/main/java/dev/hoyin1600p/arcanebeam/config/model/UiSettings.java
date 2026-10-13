package dev.hoyin1600p.arcanebeam.config.model;

/** Settings screen preferences that are not about any beam or ability. */
public final class UiSettings {
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";

    /** The screen edge the settings panel is docked to: {@code LEFT} or {@code RIGHT}. */
    public String panelSide = LEFT;

    public boolean panelOnRight() {
        return RIGHT.equals(panelSide);
    }
}
