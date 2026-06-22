package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hoyin1600p.arcanebeam.client.DarksaberItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public class DarksaberItemRendererMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemTransforms$TransformType;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V")
    )
    private void arcanebeam$renderDarksaber(ItemRenderer itemRenderer, BakedModel model, ItemStack stack, int packedLight, int packedOverlay, PoseStack poseStack, VertexConsumer originalConsumer, ItemStack originalStack, ItemTransforms.TransformType transformType, boolean leftHand, PoseStack originalPoseStack, MultiBufferSource buffer, int originalPackedLight, int originalPackedOverlay, BakedModel originalModel) {
        if (DarksaberItemRenderer.renderItem(stack, poseStack, buffer, packedLight, packedOverlay)) {
            return;
        }
        itemRenderer.renderModelLists(model, stack, packedLight, packedOverlay, poseStack, originalConsumer);
    }
}
