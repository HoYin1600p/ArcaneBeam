package dev.hoyin1600p.arcanebeam.sound;

/**
 * Maps the name of a replacement sound (a built-in file or a resource pack slot) to its subtitle translation key,
 * so a replaced Vault sound keeps a subtitle. Built-in options and resource pack slots of one sound share a key.
 */
public final class SoundSubtitles {
    private static final String PREFIX = "subtitles.arcanebeam.";

    private SoundSubtitles() {
    }

    /** The translation key for a sound name such as {@code abilities/smite_resourcepack_1} or {@code smite_1}. */
    public static String keyFor(String soundName) {
        String name = soundName.startsWith("abilities/") ? soundName.substring("abilities/".length()) : soundName;
        String key;
        if (name.startsWith("arcane")) {
            key = "arcane";
        } else if (name.startsWith("rail")) {
            key = "rail";
        } else if (name.startsWith("lightning")) {
            key = name.endsWith("impact") ? "lightning_impact" : "lightning_cast";
        } else if (name.startsWith("vault_altar")) {
            key = "altar";
        } else if (name.startsWith("storm_arrow_projectile")) {
            key = "storm_arrow_projectile";
        } else if (name.startsWith("storm_arrow")) {
            key = "storm_arrow_strike";
        } else if (name.startsWith("smite_activation")) {
            key = "smite_activation";
        } else if (name.startsWith("smite")) {
            key = "smite_strike";
        } else if (name.startsWith("archon_activation")) {
            key = "archon_activation";
        } else if (name.startsWith("archon")) {
            key = "archon_strike";
        } else {
            return null;
        }
        return PREFIX + key;
    }
}
