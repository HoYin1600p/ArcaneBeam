package dev.hoyin1600p.arcanebeam.ability.manastealer;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;

/**
 * Temporary overlap arbitration for releases of VRO that still contain the
 * original Mana Stealer module. Arcane Beam takes ownership automatically
 * as soon as that legacy class is absent.
 */
public final class ManaStealerOwnership {
    static final String LEGACY_VRO_MODULE_RESOURCE =
            "dev/hoyin1600p/vault_render_optimization/client/compat/manastealer/ManaStealerVisualModule.class";

    private static final boolean LEGACY_VRO_MODULE_PRESENT =
            detectLegacyVroModule(ManaStealerOwnership.class.getClassLoader());

    private static volatile boolean mixinsDeclined;
    private static volatile boolean mixinsApplied;

    private ManaStealerOwnership() {
    }

    public static boolean owns() {
        return owns(LEGACY_VRO_MODULE_PRESENT);
    }

    /** Called by the mixin plugin when it declines to apply the Mana Stealer mixins. */
    public static void markMixinsDeclined() {
        mixinsDeclined = true;
    }

    /** Called by the mixin plugin once a Mana Stealer mixin has really been applied. */
    public static void markMixinsApplied() {
        mixinsApplied = true;
    }

    /** False when the mixin plugin declined, so no replacement can ever run. */
    public static boolean mixinsAvailable() {
        return !mixinsDeclined;
    }

    public static boolean mixinsApplied() {
        return mixinsApplied;
    }

    static boolean owns(boolean legacyVroModulePresent) {
        return !legacyVroModulePresent;
    }

    public static void logDecision() {
        if (owns()) {
            ArcaneBeam.LOGGER.info("Mana Stealer visuals are applied by Arcane Beam.");
        } else {
            ArcaneBeam.LOGGER.info("Mana Stealer visuals are yielded to the legacy VRO module.");
        }
    }

    static boolean detectLegacyVroModule(ClassLoader loader) {
        return loader != null && loader.getResource(LEGACY_VRO_MODULE_RESOURCE) != null;
    }
}
