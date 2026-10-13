package dev.hoyin1600p.arcanebeam.ability.smite;

import java.util.Set;

/**
 * Decides which ability a Vault smite bolt belongs to.
 *
 * <p>Bolts carry only a colour. The colour is looked up in the configured {@link SmiteColorTable} and trusted when
 * it names exactly one variant. When the colour is shared (Archon and Thunderstorm in both test packs, or every
 * variant on Vault's own defaults) or unknown, the local player's active Smite effect decides, but only for a bolt
 * landing within casting reach of the local player. Without either piece of evidence the bolt is left to Vault,
 * so a bolt is never drawn as an ability it may not belong to.
 *
 * <p>Why Smite matched in the 2026-09-30 test although the pack colour (16776959) equals neither constant: the old
 * classifier tested "default colour" and "Archon colour" and sent everything else to Smite, so any recoloured
 * Smite fell into that last branch. That fallback is kept for the case where no ability colours have been synced.
 */
public final class SmiteBoltClassifier {
    /** Colour of every bolt Vault creates without recolouring it (Chain Lightning's Lightning Strike, orbs, bosses). */
    public static final int VAULT_DEFAULT_COLOR = -1864448;
    /** The Archon colour both test packs configure; used only when no colours were synced. */
    public static final int LEGACY_ARCHON_COLOR = 14282239;

    private SmiteBoltClassifier() {
    }

    public enum BoltClass {
        SMITE,
        ARCHON,
        THUNDERSTORM,
        /** A default-coloured bolt no Smite variant claims: Lightning Strike or another Vault effect. */
        LIGHTNING_DEFAULT,
        /** Ambiguous or foreign: leave it to Vault. */
        UNKNOWN
    }

    /**
     * @param color            the bolt's synced colour
     * @param table            the colours synced from the ability tree
     * @param localVariant     the Smite variant the local player has active, or null
     * @param nearLocalCaster  whether the bolt landed within casting reach of the local player
     */
    public static BoltClass classify(int color, SmiteColorTable table, SmiteVariant localVariant, boolean nearLocalCaster) {
        Set<SmiteVariant> byColor = table.variantsWith(color);
        if (byColor.size() == 1) {
            return of(byColor.iterator().next());
        }
        boolean localOwns = nearLocalCaster && localVariant != null;
        if (byColor.size() > 1) {
            if (localOwns && byColor.contains(localVariant)) {
                return of(localVariant);
            }
            return color == VAULT_DEFAULT_COLOR ? BoltClass.LIGHTNING_DEFAULT : BoltClass.UNKNOWN;
        }
        // The colour is not configured for any variant (or nothing was synced).
        if (localOwns && table.isEmpty() && color != VAULT_DEFAULT_COLOR) {
            return of(localVariant);
        }
        if (color == VAULT_DEFAULT_COLOR) {
            return BoltClass.LIGHTNING_DEFAULT;
        }
        if (!table.isEmpty()) {
            return BoltClass.UNKNOWN;
        }
        // No synced colours at all: the original colour-only behaviour.
        return color == LEGACY_ARCHON_COLOR ? BoltClass.ARCHON : BoltClass.SMITE;
    }

    private static BoltClass of(SmiteVariant variant) {
        return switch (variant) {
            case SMITE -> BoltClass.SMITE;
            case ARCHON -> BoltClass.ARCHON;
            case THUNDERSTORM -> BoltClass.THUNDERSTORM;
        };
    }
}
