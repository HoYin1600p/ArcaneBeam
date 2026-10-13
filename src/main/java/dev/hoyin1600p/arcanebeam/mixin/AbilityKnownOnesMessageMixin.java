package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.AbilityHooks;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AbilityKnownOnesMessage.class)
public abstract class AbilityKnownOnesMessageMixin {
    @Inject(method = "handle", at = @At("TAIL"), remap = false)
    private static void arcanebeam$syncKnownAbilities(AbilityKnownOnesMessage message,
                                                       Supplier<NetworkEvent.Context> contextSupplier,
                                                       CallbackInfo ci) {
        AbilityHooks.observeKnownAbilities(message, contextSupplier);
    }
}
