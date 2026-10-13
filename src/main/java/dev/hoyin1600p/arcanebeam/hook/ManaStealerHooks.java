package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerVisualConfig;
import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerVisualController;
import dev.hoyin1600p.arcanebeam.util.Features;
import iskallia.vault.entity.entity.ManaStealerEntity;

/** Entry points the Mana Stealer mixins call at runtime. A failure switches the Mana Stealer visuals off. */
public final class ManaStealerHooks {
    private ManaStealerHooks() {
    }

    public static boolean canReplace() {
        if (Features.MANA_STEALER.isDisabled()) {
            return false;
        }
        try {
            return ManaStealerVisualController.canReplace();
        } catch (RuntimeException | LinkageError failure) {
            Features.MANA_STEALER.fail(failure);
            return false;
        }
    }

    /** Replaces the entity's own client particles; returns true when the caller must skip Vault's version. */
    public static boolean replaceParticles(ManaStealerEntity entity) {
        if (Features.MANA_STEALER.isDisabled()) {
            return false;
        }
        try {
            if (!ManaStealerVisualController.canReplace()) {
                return false;
            }
            ManaStealerVisualController.maintain(entity, entity.getRadius());
            return true;
        } catch (RuntimeException | LinkageError failure) {
            Features.MANA_STEALER.fail(failure);
            return false;
        }
    }

    public static boolean replaceGroundSigil() {
        if (Features.MANA_STEALER.isDisabled()) {
            return false;
        }
        try {
            return ManaStealerVisualConfig.replaceGroundSigil();
        } catch (RuntimeException | LinkageError failure) {
            Features.MANA_STEALER.fail(failure);
            return false;
        }
    }
}
