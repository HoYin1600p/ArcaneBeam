package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import iskallia.vault.network.message.ServerboundAbilityKeyMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundAbilityKeyMessage.class)
public abstract class ServerboundAbilityKeyMessageMixin {
    @Inject(method = "send", at = @At("HEAD"), remap = false)
    private static void arcanebeam$observeLocalAbilityKey(ServerboundAbilityKeyMessage.Opcode opcode, CallbackInfo ci) {
        ArcaneBeamManager.observeLocalAbilityKey(opcode);
    }
}
