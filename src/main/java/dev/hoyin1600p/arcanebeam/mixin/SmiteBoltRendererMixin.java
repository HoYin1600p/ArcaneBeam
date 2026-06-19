package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.client.SmiteVisualManager;
import iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility$SmiteBoltRenderer", remap = false)
public abstract class SmiteBoltRendererMixin {
    @Inject(method = "render(Liskallia/vault/skill/ability/effect/spi/AbstractSmiteAbility$SmiteBolt;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void arcanebeam$replaceSmiteBolt(AbstractSmiteAbility.SmiteBolt smiteBolt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (SmiteVisualManager.handleSmiteBoltRender(smiteBolt)) {
            ci.cancel();
        }
    }
}
