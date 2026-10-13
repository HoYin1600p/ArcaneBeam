package dev.hoyin1600p.arcanebeam.config.model;

public enum StartHand {
    MAIN_HAND("main_hand", "Main Hand"),
    OFFHAND("offhand", "Offhand");

    public final String id;
    public final String label;

    StartHand(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static StartHand fromId(String id) {
        for (StartHand hand : values()) {
            if (hand.id.equals(id)) {
                return hand;
            }
        }
        return null;
    }
}
