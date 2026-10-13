package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerOwnership;

/**
 * Entry points the Mana Stealer mixin plugin calls while Mixin is still preparing its configs.
 *
 * <p>Kept apart from {@link ManaStealerHooks} on purpose: loading a class at that stage must not make the JVM load
 * any Minecraft or Vault class (verifying a method that passes a Vault entity as an {@code Entity} loads
 * {@code Entity}), or other mods' mixins into those classes fail with "target was loaded too early".
 */
public final class ManaStealerMixinState {
    private ManaStealerMixinState() {
    }

    public static boolean owns() {
        return ManaStealerOwnership.owns();
    }

    public static void markMixinsDeclined() {
        ManaStealerOwnership.markMixinsDeclined();
    }

    public static void markMixinsApplied() {
        ManaStealerOwnership.markMixinsApplied();
    }
}
