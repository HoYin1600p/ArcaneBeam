package dev.hoyin1600p.arcanebeam.config.model;

/** Labyrinth Breadcrumbs: a route to the exit inside the Vault village and Wolds labyrinths. */
public final class LabyrinthSettings {
    public static final float DEFAULT_OPACITY = 0.85F;
    public static final float DEFAULT_WIDTH = 0.08F;
    public static final float DEFAULT_THICKNESS = 0.04F;
    public static final float DEFAULT_ELEVATION = 1.10F;
    public static final int DEFAULT_MAX_DISTANCE = 48;
    public static final int DEFAULT_ROUTE_COLOR = 0x33FF66;

    /** Bounds applied on load; the settings screen fields clamp to the same values. */
    public static final float MIN_OPACITY = 0.05F;
    public static final float MAX_OPACITY = 1.0F;
    public static final float MIN_WIDTH = 0.02F;
    public static final float MAX_WIDTH = 0.5F;
    public static final float MIN_THICKNESS = 0.01F;
    public static final float MAX_THICKNESS = 0.3F;
    public static final float MIN_ELEVATION = 0.05F;
    public static final float MAX_ELEVATION = 3.0F;
    public static final int MIN_DISTANCE = 8;
    public static final int MAX_DISTANCE = 128;

    /** Opt-in: the labyrinth is scanned and the route drawn only when this is on. */
    public boolean enabled = false;
    /** {@link LabyrinthVisibility} id. */
    public String visibility = LabyrinthVisibility.ALWAYS.id;
    public int routeColor = DEFAULT_ROUTE_COLOR;
    public float opacity = DEFAULT_OPACITY;
    /** Horizontal width of the route bar, in blocks. */
    public float width = DEFAULT_WIDTH;
    /** Vertical thickness of the route bar, in blocks. */
    public float thickness = DEFAULT_THICKNESS;
    /** Height of the route above the walking surface, in blocks. */
    public float elevation = DEFAULT_ELEVATION;
    /** The route ignores world lighting (Fullbright route). */
    public boolean fullbright = true;
    public boolean chevrons = false;
    /** Route pieces farther than this from the camera are not drawn, in blocks. */
    public int maxDistance = DEFAULT_MAX_DISTANCE;

    public LabyrinthVisibility visibilityMode() {
        return LabyrinthVisibility.fromId(visibility, LabyrinthVisibility.ALWAYS);
    }
}
