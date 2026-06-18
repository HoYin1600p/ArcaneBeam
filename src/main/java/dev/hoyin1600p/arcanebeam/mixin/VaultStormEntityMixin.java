package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.StormArrowVisualManager;
import iskallia.vault.entity.entity.VaultStormEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VaultStormEntity.class)
public abstract class VaultStormEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void arcanebeam$observeStormArrow(CallbackInfo ci) {
        StormArrowVisualManager.observeStormEntity((VaultStormEntity) (Object) this);
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"
            )
    )
    private Particle arcanebeam$suppressStormCloud(ParticleEngine particleEngine, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        VaultStormEntity stormEntity = (VaultStormEntity) (Object) this;
        if (StormArrowVisualManager.shouldSuppressCloudParticle(stormEntity)) {
            return null;
        }
        return particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
