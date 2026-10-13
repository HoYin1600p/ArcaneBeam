package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.AbilityHooks;
import dev.hoyin1600p.arcanebeam.hook.ParticleHooks;
import iskallia.vault.entity.entity.VaultStormEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VaultStormEntity.class)
public abstract class VaultStormEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void arcanebeam$observeStormArrow(CallbackInfo ci) {
        AbilityHooks.observeStormEntity((VaultStormEntity) (Object) this);
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"
            )
    )
    private Particle arcanebeam$suppressStormCloud(ParticleEngine particleEngine, ParticleOptions particle, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        VaultStormEntity stormEntity = (VaultStormEntity) (Object) this;
        if (ParticleHooks.shouldSuppressStormCloudParticle(stormEntity)) {
            return null;
        }
        return particleEngine.createParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    // The base storm also drops vanilla rain through Level.addParticle; Blizzard's snowflakes are kept.
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void arcanebeam$suppressStormRain(Level level, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed) {
        VaultStormEntity stormEntity = (VaultStormEntity) (Object) this;
        if (particle != ParticleTypes.SNOWFLAKE && ParticleHooks.shouldSuppressStormCloudParticle(stormEntity)) {
            return;
        }
        level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
