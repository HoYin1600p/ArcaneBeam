package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.client.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.client.VaultAltarBeamManager;
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
    private static final ResourceLocation VAULT_ALTAR_START = new ResourceLocation("minecraft", "block.beacon.activate");
    private static final ResourceLocation VAULT_ALTAR_COMPLETION = new ResourceLocation("minecraft", "entity.player.levelup");
    private static final ResourceLocation LIGHTNING_CAST = new ResourceLocation("minecraft", "item.trident.throw");
    private static final ResourceLocation LIGHTNING_CAST_ARROW_FALLBACK = new ResourceLocation("minecraft", "entity.arrow.shoot");
    private static final ResourceLocation LIGHTNING_IMPACT = new ResourceLocation("the_vault", "lightning_bolt");

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
            return;
        }
        if (VAULT_ALTAR_START.equals(sound.getLocation())
                && VaultAltarBeamManager.handleVaultAltarStartSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (VAULT_ALTAR_COMPLETION.equals(sound.getLocation())
                && VaultAltarBeamManager.handleVaultAltarCompletionSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if ((LIGHTNING_CAST.equals(sound.getLocation()) || LIGHTNING_CAST_ARROW_FALLBACK.equals(sound.getLocation()))
                && LightningStrikeShockwaveManager.shouldSuppressLightningCastSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (LIGHTNING_IMPACT.equals(sound.getLocation())
                && LightningStrikeShockwaveManager.handleLightningImpactSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
        }
    }
}
