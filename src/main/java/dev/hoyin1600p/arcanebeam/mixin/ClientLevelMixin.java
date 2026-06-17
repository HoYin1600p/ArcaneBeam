package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.client.VaultAltarBeamManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcanebeam$captureParticle(ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (ArcaneBeamManager.captureParticle(particle, x, y, z) || VaultAltarBeamManager.captureParticle(particle, x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arcanebeam$captureForcedParticle(ParticleOptions particle, boolean force, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (ArcaneBeamManager.captureParticle(particle, x, y, z) || VaultAltarBeamManager.captureParticle(particle, x, y, z)) {
            ci.cancel();
        }
    }
}
