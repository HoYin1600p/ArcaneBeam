package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.client.LightningStrikeShockwaveManager;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChainLightningAbility.ChainLightningProjectile.class)
public abstract class ChainLightningProjectileMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void arcanebeam$spawnLightningStrikeShockwave(HitResult hitResult, CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        if (projectile.level.isClientSide && hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof LivingEntity) {
            LightningStrikeShockwaveManager.spawn(hitResult.getLocation());
        }
    }
}
