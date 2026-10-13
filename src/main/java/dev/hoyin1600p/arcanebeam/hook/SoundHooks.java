package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.altar.VaultAltarBeamManager;
import dev.hoyin1600p.arcanebeam.ability.blastwave.BlastWaveVisualManager;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.smite.SmiteVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.beam.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.util.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Entry points the sound-manager mixin calls to stop, suppress or replace sounds. A failure switches the
 * feature that owns the sound off, so the original sound plays from then on.
 */
public final class SoundHooks {
    private SoundHooks() {
    }

    public static void stopAll(Minecraft minecraft) {
        if (Features.SOUNDS.isDisabled()) {
            return;
        }
        try {
            ArcaneBeamSoundController.stopAll(minecraft);
        } catch (RuntimeException | LinkageError failure) {
            Features.SOUNDS.fail(failure);
        }
    }

    public static boolean shouldSuppressAbilityCooldownSound() {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return false;
        }
        try {
            return ArcaneBeamManager.shouldSuppressAbilityCooldownSound();
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
            return false;
        }
    }

    public static boolean handleArcaneCastSound(SoundInstance sound) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return false;
        }
        try {
            return ArcaneBeamManager.handleArcaneCastSound(sound);
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
            return false;
        }
    }

    public static boolean handleRailCastSound(SoundInstance sound) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return false;
        }
        try {
            return ArcaneBeamManager.handleRailCastSound(sound);
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
            return false;
        }
    }

    public static boolean handleVaultAltarStartSound(double x, double y, double z) {
        if (Features.ALTAR.isDisabled()) {
            return false;
        }
        try {
            return VaultAltarBeamManager.handleVaultAltarStartSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.ALTAR.fail(failure);
            return false;
        }
    }

    public static boolean handleVaultAltarCompletionSound(double x, double y, double z) {
        if (Features.ALTAR.isDisabled()) {
            return false;
        }
        try {
            return VaultAltarBeamManager.handleVaultAltarCompletionSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.ALTAR.fail(failure);
            return false;
        }
    }

    public static boolean shouldSuppressLightningCastSound(double x, double y, double z) {
        if (Features.LIGHTNING.isDisabled()) {
            return false;
        }
        try {
            return LightningStrikeShockwaveManager.shouldSuppressLightningCastSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.LIGHTNING.fail(failure);
            return false;
        }
    }

    public static boolean handleStormArrowProjectileSound(double x, double y, double z) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.handleStormArrowProjectileSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }

    public static boolean handleBlastWaveActivationSound(double x, double y, double z) {
        if (Features.BLAST_WAVE.isDisabled()) {
            return false;
        }
        try {
            return BlastWaveVisualManager.handleActivationSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.BLAST_WAVE.fail(failure);
            return false;
        }
    }

    public static boolean handleSmiteActivationSound(double x, double y, double z) {
        if (Features.SMITE.isDisabled()) {
            return false;
        }
        try {
            return SmiteVisualManager.handleSmiteActivationSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.SMITE.fail(failure);
            return false;
        }
    }

    public static boolean handleLightningImpactSound(double x, double y, double z) {
        if (Features.LIGHTNING.isDisabled()) {
            return false;
        }
        try {
            return LightningStrikeShockwaveManager.handleLightningImpactSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.LIGHTNING.fail(failure);
            return false;
        }
    }

    public static boolean handleBlastWaveStrikeSound(double x, double y, double z) {
        if (Features.BLAST_WAVE.isDisabled()) {
            return false;
        }
        try {
            return BlastWaveVisualManager.handleStrikeSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.BLAST_WAVE.fail(failure);
            return false;
        }
    }

    public static boolean handleSmiteStrikeSound(double x, double y, double z) {
        if (Features.SMITE.isDisabled()) {
            return false;
        }
        try {
            return SmiteVisualManager.handleSmiteStrikeSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.SMITE.fail(failure);
            return false;
        }
    }

    public static boolean handleStormArrowStrikeSound(double x, double y, double z) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.handleStormArrowStrikeSound(x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }
}
