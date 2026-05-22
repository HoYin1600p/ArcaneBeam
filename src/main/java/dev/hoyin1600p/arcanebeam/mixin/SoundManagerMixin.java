package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    private static final ResourceLocation ABILITY_ON_COOLDOWN = new ResourceLocation("the_vault", "ability_on_cooldown");
    private static final ResourceLocation ARCANE_CAST = new ResourceLocation("minecraft", "block.fire.extinguish");
    private static final ResourceLocation RAIL_CAST = new ResourceLocation("minecraft", "block.beacon.deactivate");

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void arcanebeam$suppressAbilitySounds(SoundInstance sound, CallbackInfo ci) {
        if (ABILITY_ON_COOLDOWN.equals(sound.getLocation()) && ArcaneBeamManager.shouldSuppressAbilityCooldownSound()) {
            ci.cancel();
            return;
        }
        if (ARCANE_CAST.equals(sound.getLocation()) && ArcaneBeamManager.shouldSuppressArcaneCastSound()) {
            ci.cancel();
            return;
        }
        if (RAIL_CAST.equals(sound.getLocation()) && ArcaneBeamManager.shouldSuppressRailCastSound()) {
            ci.cancel();
        }
    }
}
