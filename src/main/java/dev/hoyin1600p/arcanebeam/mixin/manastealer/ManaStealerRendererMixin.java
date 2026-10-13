package dev.hoyin1600p.arcanebeam.mixin.manastealer;


import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.hook.ManaStealerHooks;
import iskallia.vault.entity.entity.ManaStealerEntity;
import iskallia.vault.entity.renderer.ManaStealerRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManaStealerRenderer.class, remap = false)
public abstract class ManaStealerRendererMixin {
    @Inject(
            method = "render(Liskallia/vault/entity/entity/ManaStealerEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void arcanebeam$replaceManaStealerSigil(
            ManaStealerEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo ci
    ) {
        if (ManaStealerHooks.canReplace() && ManaStealerHooks.replaceGroundSigil()) {
            ci.cancel();
        }
    }
}
