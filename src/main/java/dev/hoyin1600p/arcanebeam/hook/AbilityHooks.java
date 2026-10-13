package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.blastwave.BlastWaveVisualManager;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.smite.SmiteVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.beam.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.util.Features;
import iskallia.vault.entity.entity.VaultStormEntity;
import iskallia.vault.network.message.AbilityActivityMessage;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import iskallia.vault.network.message.AbilityQuickselectMessage;
import iskallia.vault.network.message.ServerboundAbilityKeyMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Entry points the mixins call for ability state and gameplay events. Each method forwards to the feature class
 * that owns the behaviour and isolates it: a failure is logged once, switches that feature off for the session
 * and falls back to Vault's behaviour (see {@link dev.hoyin1600p.arcanebeam.util.FeatureGuard}).
 */
public final class AbilityHooks {
    private AbilityHooks() {
    }

    /** Schedules the Blast Wave and Arcane activity sync on the client thread. */
    public static void observeAbilityActivity(AbilityActivityMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        if (Features.ARCANE_BEAM.isDisabled() && Features.BLAST_WAVE.isDisabled()) {
            return;
        }
        try {
            if (message != null && message.getActiveFlag() != null) {
                contextSupplier.get().enqueueWork(() -> {
                    observeBlastWaveActivity(message);
                    syncLocalAbilityActivity(message);
                });
            }
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
            Features.BLAST_WAVE.fail(failure);
        }
    }

    /** Schedules the known-abilities sync on the client thread; Arcane and Smite each guard their own part. */
    public static void observeKnownAbilities(AbilityKnownOnesMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        if (Features.ARCANE_BEAM.isDisabled() && Features.SMITE.isDisabled()) {
            return;
        }
        try {
            contextSupplier.get().enqueueWork(() -> {
                syncLocalAbilityState(message);
                observeSmiteAbilityTree(message);
            });
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
        }
    }

    private static void observeBlastWaveActivity(AbilityActivityMessage message) {
        if (Features.BLAST_WAVE.isDisabled()) {
            return;
        }
        try {
            BlastWaveVisualManager.observeAbilityActivity(message.getAbility(), message.getActiveFlag().name());
        } catch (RuntimeException | LinkageError failure) {
            Features.BLAST_WAVE.fail(failure);
        }
    }

    private static void syncLocalAbilityActivity(AbilityActivityMessage message) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return;
        }
        try {
            ArcaneBeamManager.syncLocalAbilityActivity(message);
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
        }
    }

    private static void syncLocalAbilityState(AbilityKnownOnesMessage message) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return;
        }
        try {
            ArcaneBeamManager.syncLocalAbilityState(message);
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
        }
    }

    /** Smite reads the synced ability colours from the same message, under its own guard so Arcane failures stay separate. */
    private static void observeSmiteAbilityTree(AbilityKnownOnesMessage message) {
        if (Features.SMITE.isDisabled()) {
            return;
        }
        try {
            SmiteVisualManager.observeAbilityTree(message == null ? null : message.getTree());
        } catch (RuntimeException | LinkageError failure) {
            Features.SMITE.fail(failure);
        }
    }
    /** A quickselect message was built; only a key press on the client thread counts as a local cast. */
    public static void observeQuickselect(String abilityId, int action) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return;
        }
        try {
            if (action == AbilityQuickselectMessage.PRESSED && Minecraft.getInstance().isSameThread()) {
                ArcaneBeamManager.observeLocalQuickselect(abilityId);
            }
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
        }
    }

    public static void observeLocalAbilityKey(ServerboundAbilityKeyMessage.Opcode opcode) {
        if (Features.ARCANE_BEAM.isDisabled()) {
            return;
        }
        try {
            ArcaneBeamManager.observeLocalAbilityKey(opcode);
        } catch (RuntimeException | LinkageError failure) {
            Features.ARCANE_BEAM.fail(failure);
        }
    }

    public static boolean handleBlastWavePulse(Vec3 packetOrigin, float radius, int[] targetIds) {
        if (Features.BLAST_WAVE.isDisabled()) {
            return false;
        }
        try {
            return BlastWaveVisualManager.handlePulse(packetOrigin, radius, targetIds);
        } catch (RuntimeException | LinkageError failure) {
            Features.BLAST_WAVE.fail(failure);
            return false;
        }
    }

    /** A Chain Lightning projectile hit something: start a Lightning Strike shockwave for a local hit on a living target. */
    public static void onLightningProjectileHit(Entity projectile, HitResult hitResult) {
        if (Features.LIGHTNING.isDisabled()) {
            return;
        }
        try {
            if (projectile.level.isClientSide && LightningStrikeShockwaveManager.isLocallyOwned(projectile)
                    && hitResult instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity() instanceof LivingEntity) {
                LightningStrikeShockwaveManager.spawn(hitResult.getLocation());
            }
        } catch (RuntimeException | LinkageError failure) {
            Features.LIGHTNING.fail(failure);
        }
    }

    public static void observeStormEntity(VaultStormEntity stormEntity) {
        if (Features.STORM_ARROW.isDisabled()) {
            return;
        }
        try {
            StormArrowVisualManager.observeStormEntity(stormEntity);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
        }
    }
}
