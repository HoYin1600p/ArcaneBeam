package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.client.LightningStrikeShockwaveManager;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChainLightningAbility.LightningArrowRenderer.class)
public abstract class LightningArrowRendererMixin {
    @Inject(method = "render(Liskallia/vault/skill/ability/effect/ChainLightningAbility$ChainLightningProjectile;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcanebeam$hideLightningArrow(ChainLightningAbility.ChainLightningProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (LightningStrikeShockwaveManager.handleProjectileRender(projectile, poseStack, buffer, partialTick)) {
            ci.cancel();
        }
    }
}
