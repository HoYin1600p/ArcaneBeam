package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.AbilityHooks;
import iskallia.vault.network.message.AbilityQuickselectMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbilityQuickselectMessage.class, remap = false)
public abstract class AbilityQuickselectMessageMixin {
    // Quickselect keys cast through this message instead of ServerboundAbilityKeyMessage.
    // The client builds it only to send; the integrated server's decode runs off the client thread.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void arcanebeam$observeLocalQuickselect(String abilityName, int action, CallbackInfo ci) {
        AbilityHooks.observeQuickselect(abilityName, action);
    }
}
