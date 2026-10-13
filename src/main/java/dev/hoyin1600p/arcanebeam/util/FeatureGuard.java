package dev.hoyin1600p.arcanebeam.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * Kill switch for one visual feature. Hooks and event handlers check {@link #isDisabled()} first and report
 * any failure through {@link #fail(Throwable)}: the first failure is logged once with its stack and the
 * feature then stays off for the rest of the session, so a broken integration degrades to Vault's own visuals
 * instead of crashing the client or flooding the log. The check is one volatile read and never allocates.
 */
public final class FeatureGuard {
    private static final Logger LOGGER = LogManager.getLogger("arcanebeam");

    private final String feature;
    private volatile boolean disabled;

    public FeatureGuard(String feature) {
        this.feature = feature;
    }

    public String feature() {
        return feature;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /** Disables the feature and logs the cause the first time it is called; later calls are silent. */
    public void fail(Throwable cause) {
        if (disabled) {
            return;
        }
        synchronized (this) {
            if (disabled) {
                return;
            }
            disabled = true;
        }
        try {
            LOGGER.warn("Arcane Beam feature '{}' hit an error and is disabled for this session; "
                    + "Vault's own visuals are used instead", feature, cause);
        } catch (Throwable ignored) {
            // Failure reporting must never throw back into the game.
        }
    }

    /** Wraps an event handler so it is skipped once this feature is disabled and disables it when the handler throws. */
    public <E> Consumer<E> wrap(Consumer<E> handler) {
        return event -> {
            if (disabled) {
                return;
            }
            try {
                handler.accept(event);
            } catch (RuntimeException | LinkageError failure) {
                fail(failure);
            }
        };
    }

    /** Re-enables the feature; used by tests and when the player changes the setting that owns it. */
    public void reset() {
        disabled = false;
    }
}
