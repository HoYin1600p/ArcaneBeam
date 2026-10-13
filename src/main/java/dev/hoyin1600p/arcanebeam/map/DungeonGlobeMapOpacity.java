package dev.hoyin1600p.arcanebeam.map;

import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.DungeonGlobeMapSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Decides how strongly rooms that the local player did not discover by walking fade on Vault's maps.
 *
 * <p>Vault tags every discovered room with a {@code DiscoverVaultRoomEvent.Context}:
 * <ul>
 *   <li>PLAYER_DISCOVERED: this player walked in. Never faded.</li>
 *   <li>INSCRIPTION: revealed by an inscription. Never faded.</li>
 *   <li>SYSTEM: revealed by a globe, an objective (Cake), Pandora, a personal vault or an operator command.
 *       Faded to the revealed-room opacity until the local player stands in the room.</li>
 *   <li>SHARED: a teammate discovered the room and Vault copied it into this player's stats
 *       ({@code StatCollector.shareDiscoveredRoom}). Faded to the teammate-room opacity until the local
 *       player stands in the room.</li>
 * </ul>
 * Unknown (future) contexts are not faded. Because Vault records the first reveal only, a room revealed before
 * the player entered it keeps its SYSTEM or SHARED tag forever, which is why visits are tracked on the client.
 */
public final class DungeonGlobeMapOpacity {
    private static final Logger LOGGER = LogManager.getLogger("arcanebeam");
    private static final Set<String> LOGGED_UNKNOWN = new HashSet<>();

    private DungeonGlobeMapOpacity() {
    }

    /** How a room was revealed, reduced to what the fade rule needs. */
    public enum RevealKind {
        /** Walked in, inscription, unknown or missing: drawn at Vault's normal opacity. */
        NONE,
        SYSTEM,
        SHARED
    }

    /** Fade multipliers per reveal kind; 1.0 means untouched. */
    public record Factors(float revealed, float shared) {
        public static final Factors NONE = new Factors(1.0F, 1.0F);

        public boolean anyFade() {
            return revealed < 1.0F || shared < 1.0F;
        }

        public float forKind(RevealKind kind) {
            return switch (kind) {
                case SYSTEM -> revealed;
                case SHARED -> shared;
                case NONE -> 1.0F;
            };
        }
    }

    public static Factors configuredFactors() {
        DungeonGlobeMapSettings settings = ArcaneBeamConfig.INSTANCE.dungeonGlobeMap;
        if (settings == null) {
            return Factors.NONE;
        }
        return resolveFactors(settings.enabled, settings.opacityPercent, settings.sharedOpacityPercent);
    }

    static Factors resolveFactors(boolean enabled, int revealedPercent, int sharedPercent) {
        return new Factors(resolveFactor(enabled, revealedPercent), resolveFactor(enabled, sharedPercent));
    }

    static float resolveFactor(boolean enabled, int opacityPercent) {
        if (!enabled) {
            return 1.0F;
        }
        int clampedPercent = Math.max(0, Math.min(100, opacityPercent));
        return clampedPercent / 100.0F;
    }

    /** Maps a Vault discovery context name (the enum constant name) to its fade category. */
    static RevealKind classify(String contextName) {
        if (contextName == null) {
            return RevealKind.NONE;
        }
        switch (contextName) {
            case "SYSTEM":
                return RevealKind.SYSTEM;
            case "SHARED":
                return RevealKind.SHARED;
            case "PLAYER_DISCOVERED":
            case "INSCRIPTION":
                return RevealKind.NONE;
            default:
                if (LOGGED_UNKNOWN.add(contextName)) {
                    LOGGER.debug("Dungeon globe map: unknown room discovery context {}; not fading it", contextName);
                }
                return RevealKind.NONE;
        }
    }

    static float roomFactor(RevealKind kind, boolean visited, Factors factors) {
        return kind == RevealKind.NONE || visited ? 1.0F : factors.forKind(kind);
    }

    /** True when this room will be drawn below its normal opacity. */
    static boolean fades(RevealKind kind, boolean visited, Factors factors) {
        return roomFactor(kind, visited, factors) < 1.0F;
    }
}
