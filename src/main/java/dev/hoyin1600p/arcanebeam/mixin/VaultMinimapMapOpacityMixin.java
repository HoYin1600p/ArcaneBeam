package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.hook.MapHooks;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms.Surface;
import iskallia.vault.client.map.VaultMapData;
import iskallia.vault.client.render.hud.module.vault.VaultMinimapModule;
import iskallia.vault.core.vault.Vault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the dungeon globe map fade to Vault's minimap. The minimap draws its rooms inside the clipped lambda
 * {@code renderVault$2} (run synchronously by {@code UIHelper.renderOverflowHidden}), so that lambda is the target.
 * Same hook pattern as {@link DungeonGlobeMapOpacityMixin}.
 */
@Mixin(value = VaultMinimapModule.class, remap = false)
public abstract class VaultMinimapMapOpacityMixin {
    private static final String ROOM_PASS = "lambda$renderVault$2(Lnet/minecraft/core/Direction;Lnet/minecraft/client/player/LocalPlayer;"
            + "Liskallia/vault/core/vault/Vault;Liskallia/vault/client/map/VaultMapData;Lcom/mojang/blaze3d/vertex/PoseStack;)V";

    @Inject(method = ROOM_PASS, at = @At("HEAD"), require = 0)
    private void arcanebeam$beginMinimapFrame(Direction direction, LocalPlayer player, Vault vault, VaultMapData mapData,
                                              PoseStack poseStack, CallbackInfo ci) {
        MapHooks.beginFrame(Surface.MINIMAP, vault);
    }

    @Inject(
            method = ROOM_PASS,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getRooms()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$beginMinimapRooms(Direction direction, LocalPlayer player, Vault vault, VaultMapData mapData,
                                              PoseStack poseStack, CallbackInfo ci) {
        MapHooks.beginRooms();
    }

    @Redirect(
            method = ROOM_PASS,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData$Room;getRoomId()Lnet/minecraft/resources/ResourceLocation;"),
            require = 0
    )
    private ResourceLocation arcanebeam$tintMinimapRoom(VaultMapData.Room room) {
        MapHooks.applyRoom(room);
        return room.getRoomId();
    }

    @Inject(
            method = ROOM_PASS,
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getTunnels()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$endMinimapRooms(Direction direction, LocalPlayer player, Vault vault, VaultMapData mapData,
                                            PoseStack poseStack, CallbackInfo ci) {
        MapHooks.endRooms();
    }

    @Inject(method = ROOM_PASS, at = @At("RETURN"), require = 0)
    private void arcanebeam$endMinimapFrame(Direction direction, LocalPlayer player, Vault vault, VaultMapData mapData,
                                            PoseStack poseStack, CallbackInfo ci) {
        MapHooks.endFrame();
    }
}
