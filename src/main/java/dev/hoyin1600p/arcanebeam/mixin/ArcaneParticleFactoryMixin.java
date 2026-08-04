package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import iskallia.vault.client.particles.ArcaneParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArcaneParticle.Factory.class, remap = false)
public abstract class ArcaneParticleFactoryMixin {
    @Shadow
    private boolean isRail;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void arcanebeam$replaceOwnedParticle(SimpleParticleType type, ClientLevel level,
                                                  double x, double y, double z,
                                                  double xSpeed, double ySpeed, double zSpeed,
                                                  CallbackInfoReturnable<Particle> cir) {
        if (ArcaneBeamManager.handleArcaneParticle(this.isRail, x, y, z)) {
            cir.setReturnValue(null);
        }
    }
}
