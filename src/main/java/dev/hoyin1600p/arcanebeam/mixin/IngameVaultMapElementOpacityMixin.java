package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.hook.MapHooks;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms.Surface;
import iskallia.vault.client.gui.framework.render.spi.IElementRenderer;
import iskallia.vault.client.gui.screen.map.IngameVaultMapElement;
import iskallia.vault.client.map.VaultMapData;
import iskallia.vault.core.vault.Vault;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the dungeon globe map fade to Vault's full map screen. Only {@code render} is hooked: the hover tooltip
 * method also calls getRooms/getRoomId but draws nothing. The end-of-vault summary map is not hooked because its
 * screens draw through a buffered renderer that ignores the shader colour at flush time.
 */
@Mixin(value = IngameVaultMapElement.class, remap = false)
public abstract class IngameVaultMapElementOpacityMixin {
    private static final String RENDER = "render(Liskallia/vault/client/gui/framework/render/spi/IElementRenderer;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V";

    @Shadow
    @Final
    private Vault vault;

    @Inject(method = RENDER, at = @At("HEAD"), require = 0)
    private void arcanebeam$beginMapScreenFrame(IElementRenderer renderer, PoseStack poseStack, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        MapHooks.beginFrame(Surface.MAP_SCREEN, this.vault);
    }

    @Inject(
            method = RENDER,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getRooms()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$beginMapScreenRooms(IElementRenderer renderer, PoseStack poseStack, int mouseX, int mouseY,
                                                float partialTick, CallbackInfo ci) {
        MapHooks.beginRooms();
    }

    @Redirect(
            method = RENDER,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData$Room;getRoomId()Lnet/minecraft/resources/ResourceLocation;"),
            require = 0
    )
    private ResourceLocation arcanebeam$tintMapScreenRoom(VaultMapData.Room room) {
        MapHooks.applyRoom(room);
        return room.getRoomId();
    }

    @Inject(
            method = RENDER,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getTunnels()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$endMapScreenRooms(IElementRenderer renderer, PoseStack poseStack, int mouseX, int mouseY,
                                              float partialTick, CallbackInfo ci) {
        MapHooks.endRooms();
    }

    @Inject(method = RENDER, at = @At("RETURN"), require = 0)
    private void arcanebeam$endMapScreenFrame(IElementRenderer renderer, PoseStack poseStack, int mouseX, int mouseY,
                                              float partialTick, CallbackInfo ci) {
        MapHooks.endFrame();
    }
}
