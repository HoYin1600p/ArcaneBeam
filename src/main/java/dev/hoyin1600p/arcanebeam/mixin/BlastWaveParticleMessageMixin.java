package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.AbilityHooks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "iskallia.vault.network.message.ClientboundBlastWaveParticleMessage", remap = false)
public abstract class BlastWaveParticleMessageMixin {
    @Inject(method = "spawnParticles", at = @At("HEAD"), cancellable = true)
    private static void arcanebeam$replaceBlastWaveParticles(Vec3 origin, float radius, int[] targetIds, CallbackInfo ci) {
        if (AbilityHooks.handleBlastWavePulse(origin, radius, targetIds)) {
            ci.cancel();
        }
    }
}
