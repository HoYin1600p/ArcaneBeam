package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import iskallia.vault.network.message.AbilityActivityMessage;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AbilityActivityMessage.class)
public abstract class AbilityActivityMessageMixin {
    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void arcanebeam$observeAbilityActivity(AbilityActivityMessage message, Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        if (message != null && message.getActiveFlag() != null) {
            ArcaneBeamManager.observeAbilityActivity(message.getAbility(), message.getActiveFlag().name());
        }
    }
}
