package dev.hoyin1600p.arcanebeam.config.model;

public enum SoundChoice {
    DEFAULT("default", "Default"),
    OPTION_1("option_1", "Option 1"),
    OPTION_2("option_2", "Option 2"),
    RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
    RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

    public final String id;
    public final String label;

    SoundChoice(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static SoundChoice fromId(String id) {
        for (SoundChoice choice : values()) {
            if (choice.id.equals(id)) {
                return choice;
            }
        }
        return null;
    }
}
