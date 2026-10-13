package dev.hoyin1600p.arcanebeam.mixin.manastealer;


import dev.hoyin1600p.arcanebeam.hook.ManaStealerHooks;
import iskallia.vault.entity.entity.ManaStealerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManaStealerEntity.class, remap = false)
public abstract class ManaStealerEntityParticlesMixin {
    @Inject(method = "spawnClientParticles()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcanebeam$replaceManaStealerParticles(CallbackInfo ci) {
        if (ManaStealerHooks.replaceParticles((ManaStealerEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
