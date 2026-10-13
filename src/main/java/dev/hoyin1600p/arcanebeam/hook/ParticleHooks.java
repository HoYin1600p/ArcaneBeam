package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.altar.VaultAltarBeamManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.beam.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.beam.ResourcePackParticleStyle;
import dev.hoyin1600p.arcanebeam.util.Features;
import iskallia.vault.entity.entity.VaultStormArrow;
import iskallia.vault.entity.entity.VaultStormEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

/**
 * Entry points the mixins call to replace or suppress particles. A failure switches that feature off, which
 * leaves the particle to Vault or vanilla.
 */
public final class ParticleHooks {
    private ParticleHooks() {
    }

    public static Vec3 resourcePackParticlePosition(boolean rail, double x, double y, double z) {
        if (Features.PARTICLES.isDisabled()) {
            return null;
        }
        try {
            return ArcaneBeamManager.resourcePackParticlePosition(rail, x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.PARTICLES.fail(failure);
            return null;
        }
    }

    public static void applyResourcePackStyle(Particle particle) {
        if (Features.PARTICLES.isDisabled()) {
            return;
        }
        try {
            ResourcePackParticleStyle.apply(particle);
        } catch (RuntimeException | LinkageError failure) {
            Features.PARTICLES.fail(failure);
        }
    }

    /** Drops a style prepared for a particle that was never created, so it cannot reach the next one. */
    public static void discardPendingResourcePackStyle() {
        try {
            ResourcePackParticleStyle.clearPending();
        } catch (RuntimeException | LinkageError failure) {
            Features.PARTICLES.fail(failure);
        }
    }

    public static void tickResourcePackStyle(Particle particle) {
        if (Features.PARTICLES.isDisabled()) {
            return;
        }
        try {
            ResourcePackParticleStyle.tick(particle);
        } catch (RuntimeException | LinkageError failure) {
            Features.PARTICLES.fail(failure);
        }
    }

    public static boolean handleArcaneParticle(boolean rail, double x, double y, double z,
                                               double xSpeed, double ySpeed, double zSpeed) {
        if (Features.PARTICLES.isDisabled()) {
            return false;
        }
        try {
            return ArcaneBeamManager.handleArcaneParticle(rail, x, y, z, xSpeed, ySpeed, zSpeed);
        } catch (RuntimeException | LinkageError failure) {
            Features.PARTICLES.fail(failure);
            return false;
        }
    }

    public static boolean captureAltarParticle(ParticleOptions particle, double x, double y, double z) {
        if (Features.ALTAR.isDisabled()) {
            return false;
        }
        try {
            return VaultAltarBeamManager.captureParticle(particle, x, y, z);
        } catch (RuntimeException | LinkageError failure) {
            Features.ALTAR.fail(failure);
            return false;
        }
    }

    public static boolean shouldSuppressStormCloudParticle(VaultStormEntity stormEntity) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.shouldSuppressCloudParticle(stormEntity);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }

    public static boolean shouldSuppressStormProjectileTrail(VaultStormArrow arrow) {
        if (Features.STORM_ARROW.isDisabled()) {
            return false;
        }
        try {
            return StormArrowVisualManager.shouldSuppressProjectileTrail(arrow);
        } catch (RuntimeException | LinkageError failure) {
            Features.STORM_ARROW.fail(failure);
            return false;
        }
    }
}
