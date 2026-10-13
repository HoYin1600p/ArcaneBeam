package dev.hoyin1600p.arcanebeam.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.hook.RenderHooks;
import iskallia.vault.entity.entity.VaultStormArrow;
import iskallia.vault.entity.renderer.StormArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StormArrowRenderer.class, remap = false)
public abstract class StormArrowRendererMixin {
    @Inject(method = "render(Liskallia/vault/entity/entity/VaultStormArrow;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void arcanebeam$replaceStormArrowProjectile(VaultStormArrow arrow, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (RenderHooks.handleStormArrowProjectileRender(arrow, poseStack, buffer, partialTick)) {
            ci.cancel();
        }
    }
}
