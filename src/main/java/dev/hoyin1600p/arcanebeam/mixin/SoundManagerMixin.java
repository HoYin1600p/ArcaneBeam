package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.SoundHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.MINECRAFT_NAMESPACE;
import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.VAULT_NAMESPACE;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    private static final ResourceLocation ABILITY_ON_COOLDOWN = new ResourceLocation(VAULT_NAMESPACE, "ability_on_cooldown");
    private static final ResourceLocation ARCANE_CAST = new ResourceLocation(MINECRAFT_NAMESPACE, "block.fire.extinguish");
    private static final ResourceLocation RAIL_CAST = new ResourceLocation(MINECRAFT_NAMESPACE, "block.beacon.deactivate");
    private static final ResourceLocation VAULT_ALTAR_START = new ResourceLocation(MINECRAFT_NAMESPACE, "block.beacon.activate");
    private static final ResourceLocation VAULT_ALTAR_COMPLETION = new ResourceLocation(MINECRAFT_NAMESPACE, "entity.player.levelup");
    private static final ResourceLocation LIGHTNING_CAST = new ResourceLocation(MINECRAFT_NAMESPACE, "item.trident.throw");
    private static final ResourceLocation LIGHTNING_CAST_ARROW_FALLBACK = new ResourceLocation(MINECRAFT_NAMESPACE, "entity.arrow.shoot");
    private static final ResourceLocation LIGHTNING_IMPACT = new ResourceLocation(VAULT_NAMESPACE, "lightning_bolt");
    private static final ResourceLocation STORM_ARROW_PROJECTILE = new ResourceLocation(MINECRAFT_NAMESPACE, "item.crossbow.shoot");
    private static final ResourceLocation STORM_ARROW_PROJECTILE_ARROW_FALLBACK = new ResourceLocation(MINECRAFT_NAMESPACE, "entity.arrow.shoot");
    private static final ResourceLocation STORM_ARROW_STRIKE = new ResourceLocation(VAULT_NAMESPACE, "smite_bolt");
    private static final ResourceLocation SMITE_ACTIVATION = new ResourceLocation(VAULT_NAMESPACE, "smite");
    private static final ResourceLocation SMITE_STRIKE = new ResourceLocation(VAULT_NAMESPACE, "smite_bolt");

    @Inject(method = "stop()V", at = @At("HEAD"), require = 0)
    private void arcanebeam$stopTrackedCustomSounds(CallbackInfo ci) {
        SoundHooks.stopAll(Minecraft.getInstance());
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void arcanebeam$suppressAbilitySounds(SoundInstance sound, CallbackInfo ci) {
        if (ABILITY_ON_COOLDOWN.equals(sound.getLocation()) && SoundHooks.shouldSuppressAbilityCooldownSound()) {
            ci.cancel();
            return;
        }
        if (ARCANE_CAST.equals(sound.getLocation()) && SoundHooks.handleArcaneCastSound(sound)) {
            ci.cancel();
            return;
        }
        if (RAIL_CAST.equals(sound.getLocation()) && SoundHooks.handleRailCastSound(sound)) {
            ci.cancel();
            return;
        }
        if (VAULT_ALTAR_START.equals(sound.getLocation())
                && SoundHooks.handleVaultAltarStartSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (VAULT_ALTAR_COMPLETION.equals(sound.getLocation())
                && SoundHooks.handleVaultAltarCompletionSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if ((LIGHTNING_CAST.equals(sound.getLocation()) || LIGHTNING_CAST_ARROW_FALLBACK.equals(sound.getLocation()))
                && SoundHooks.shouldSuppressLightningCastSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if ((STORM_ARROW_PROJECTILE.equals(sound.getLocation()) || STORM_ARROW_PROJECTILE_ARROW_FALLBACK.equals(sound.getLocation()))
                && SoundHooks.handleStormArrowProjectileSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (SMITE_ACTIVATION.equals(sound.getLocation())
                && SoundHooks.handleBlastWaveActivationSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (SMITE_ACTIVATION.equals(sound.getLocation())
                && SoundHooks.handleSmiteActivationSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (LIGHTNING_IMPACT.equals(sound.getLocation())
                && SoundHooks.handleLightningImpactSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (SMITE_STRIKE.equals(sound.getLocation())
                && SoundHooks.handleBlastWaveStrikeSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (SMITE_STRIKE.equals(sound.getLocation())
                && SoundHooks.handleSmiteStrikeSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
            return;
        }
        if (STORM_ARROW_STRIKE.equals(sound.getLocation())
                && SoundHooks.handleStormArrowStrikeSound(sound.getX(), sound.getY(), sound.getZ())) {
            ci.cancel();
        }
    }
}
