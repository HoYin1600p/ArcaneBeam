package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.ParticleHooks;
import dev.hoyin1600p.arcanebeam.vault.VaultFields;
import iskallia.vault.client.particles.ArcaneParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ArcaneParticle.Factory.class, remap = false)
public abstract class ArcaneParticleFactoryMixin {
    @Unique
    private static boolean arcanebeam$creatingMovedParticle;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void arcanebeam$replaceOwnedParticle(SimpleParticleType type, ClientLevel level,
                                                  double x, double y, double z,
                                                  double xSpeed, double ySpeed, double zSpeed,
                                                  CallbackInfoReturnable<Particle> cir) {
        if (arcanebeam$creatingMovedParticle) {
            return;
        }
        // Read through reflection, not @Shadow: a renamed Vault field must disable this feature, not fail the mixin.
        int railFlag = VaultFields.arcaneFactoryIsRail(this);
        if (railFlag < 0) {
            return;
        }
        boolean isRail = railFlag == 1;
        // Resource pack mode: keep Vault's particle but start its stream at the configured beam origin.
        Vec3 moved = ParticleHooks.resourcePackParticlePosition(isRail, x, y, z);
        if (moved != null) {
            arcanebeam$creatingMovedParticle = true;
            try {
                Particle particle = ((ParticleProvider<SimpleParticleType>) (Object) this)
                        .createParticle(type, level, moved.x, moved.y, moved.z, xSpeed, ySpeed, zSpeed);
                ParticleHooks.applyResourcePackStyle(particle);
                cir.setReturnValue(particle);
            } finally {
                arcanebeam$creatingMovedParticle = false;
                // A style prepared for this call must not outlive it, even when createParticle threw.
                ParticleHooks.discardPendingResourcePackStyle();
            }
            return;
        }
        if (ParticleHooks.handleArcaneParticle(isRail, x, y, z, xSpeed, ySpeed, zSpeed)) {
            cir.setReturnValue(null);
        }
    }
}
