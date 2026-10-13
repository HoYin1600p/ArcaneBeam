package dev.hoyin1600p.arcanebeam.hook;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.smite.SmiteVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.util.Features;
import iskallia.vault.entity.entity.VaultStormArrow;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Entry points the mixins call to take over entity rendering. A failure switches that feature off, so Vault
 * draws the entity itself from then on.
 */
public final class RenderHooks {
    private RenderHooks() {
    }

    public static boolean shouldReplaceLightningProjectileRender(Entity projectile) {
        if (Features.LIGHTNING.isDisabled()) {
            return false;
        }
        try {
            return LightningStrikeShockwaveManager.shouldReplaceProjectileRender(projectile);
        } catch (RuntimeException | LinkageError failure) {
            Features.LIGHTNING.fail(failure);
            return false;
        }
    }

    public static boolean handleSmiteBoltRender(Entity smiteBolt) {
        if (Features.SMITE.isDisabled()) {
            return false;
        }
        try {
            return SmiteVisualManager.handleSmiteBoltRender(smiteBolt);
        } catch (RuntimeException | LinkageError failure) {
            Features.SMITE.fail(failure);
            return false;
        }
    }

    public static boolean handleStormSmiteBoltRender(Entity smiteBolt) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.handleSmiteBoltRender(smiteBolt);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }

    public static boolean handleStormArrowProjectileRender(VaultStormArrow arrow, PoseStack poseStack,
                                                           MultiBufferSource buffer, float partialTick) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.handleProjectileRender(arrow, poseStack, buffer, partialTick);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }
}
