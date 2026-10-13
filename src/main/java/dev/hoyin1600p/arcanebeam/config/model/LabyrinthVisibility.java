package dev.hoyin1600p.arcanebeam.config.model;

/** When the labyrinth lines are drawn while the player is inside a supported labyrinth. */
public enum LabyrinthVisibility {
    ALWAYS("always", "Always"),
    HOLD_KEY("hold_key", "Hold key");

    public final String id;
    public final String label;

    LabyrinthVisibility(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** The mode with this id, or {@code fallback} when the id is unknown or null. */
    public static LabyrinthVisibility fromId(String id, LabyrinthVisibility fallback) {
        for (LabyrinthVisibility mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return fallback;
    }

    public LabyrinthVisibility next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
