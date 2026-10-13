package dev.hoyin1600p.arcanebeam.ability.smite;

import java.util.Locale;

/**
 * The Smite abilities that spawn lightning bolts. Blast Wave is not listed: it casts through its own packet and
 * is handled by the Blast Wave manager.
 */
public enum SmiteVariant {
    SMITE,
    ARCHON,
    THUNDERSTORM;

    /**
     * Maps the path of a Vault mob effect in the {@code the_vault} namespace to a variant. Both the Remastered
     * (6932) and Wolds (6884) jars register {@code smite}, {@code smite_archon} and {@code smite_thunderstorm}
     * as the toggle effects of the three abilities; anything else (including {@code smite_blast_wave}) is not a
     * bolt caster.
     */
    public static SmiteVariant fromEffectPath(String path) {
        if (path == null) {
            return null;
        }
        return switch (path.toLowerCase(Locale.ROOT)) {
            case "smite" -> SMITE;
            case "smite_archon" -> ARCHON;
            case "smite_thunderstorm" -> THUNDERSTORM;
            default -> null;
        };
    }
}
