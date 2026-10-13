package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.ParticleHooks;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Embeddium replaces SingleQuadParticle.render, so facing is updated each tick; its renderer still honours roll.
@Mixin(Particle.class)
public abstract class ParticleFacingMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void arcanebeam$faceResourcePackParticle(CallbackInfo ci) {
        ParticleHooks.tickResourcePackStyle((Particle) (Object) this);
    }
}
