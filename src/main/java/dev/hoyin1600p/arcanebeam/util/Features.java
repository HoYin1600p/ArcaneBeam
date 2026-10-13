package dev.hoyin1600p.arcanebeam.util;

/** The kill switches shared by the hook facades, the event listeners and the client tick. */
public final class Features {
    public static final FeatureGuard ARCANE_BEAM = new FeatureGuard("Arcane and Rail beams");
    public static final FeatureGuard PARTICLES = new FeatureGuard("Arcane particle replacement");
    public static final FeatureGuard SOUNDS = new FeatureGuard("Ability sound replacement");
    public static final FeatureGuard ALTAR = new FeatureGuard("Vault Altar beam");
    public static final FeatureGuard BLAST_WAVE = new FeatureGuard("Blast Wave visuals");
    public static final FeatureGuard LIGHTNING = new FeatureGuard("Lightning Strike visuals");
    public static final FeatureGuard SMITE = new FeatureGuard("Smite and Archon visuals");
    public static final FeatureGuard STORM_ARROW = new FeatureGuard("Storm Arrow visuals");
    public static final FeatureGuard LABYRINTH = new FeatureGuard("Labyrinth breadcrumbs");
    public static final FeatureGuard GLOBE_MAP = new FeatureGuard("Dungeon globe map");
    public static final FeatureGuard MANA_STEALER = new FeatureGuard("Mana Stealer visuals");
    public static final FeatureGuard GUI = new FeatureGuard("Config screen HUD hiding");
    public static final FeatureGuard UPDATE_NOTICE = new FeatureGuard("Update notice");
    public static final FeatureGuard CLIENT = new FeatureGuard("Client tick services");

    private Features() {
    }
}
