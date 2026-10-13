package dev.hoyin1600p.arcanebeam.mixin;


import dev.hoyin1600p.arcanebeam.hook.MapHooks;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms.Surface;
import iskallia.vault.client.map.VaultMapData;
import iskallia.vault.client.render.hud.module.context.ModuleRenderContext;
import iskallia.vault.client.render.hud.module.vault.VaultMapOverlayModule;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.player.Listener;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VaultMapOverlayModule.class, remap = false)
public abstract class DungeonGlobeMapOpacityMixin {
    @Inject(method = "renderVault", at = @At("HEAD"), require = 0)
    private void arcanebeam$beginGlobeMapFrame(ModuleRenderContext context, Vault vault, Listener listener, CallbackInfo ci) {
        MapHooks.beginFrame(Surface.OVERLAY, vault);
    }

    @Inject(
            method = "renderVault",
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getRooms()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$beginGlobeMapRooms(ModuleRenderContext context, Vault vault, Listener listener, CallbackInfo ci) {
        MapHooks.beginRooms();
    }

    // Room.getRoomId() is read once per room, immediately before that room's icon is drawn.
    @Redirect(
            method = "renderVault",
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData$Room;getRoomId()Lnet/minecraft/resources/ResourceLocation;"),
            require = 0
    )
    private ResourceLocation arcanebeam$tintGlobeRevealedRoom(VaultMapData.Room room) {
        MapHooks.applyRoom(room);
        return room.getRoomId();
    }

    @Inject(
            method = "renderVault",
            at = @At(value = "INVOKE", target = "Liskallia/vault/client/map/VaultMapData;getTunnels()Ljava/util/List;"),
            require = 0
    )
    private void arcanebeam$endGlobeMapRooms(ModuleRenderContext context, Vault vault, Listener listener, CallbackInfo ci) {
        MapHooks.endRooms();
    }

    @Inject(method = "renderVault", at = @At("RETURN"), require = 0)
    private void arcanebeam$endGlobeMapFrame(ModuleRenderContext context, Vault vault, Listener listener, CallbackInfo ci) {
        MapHooks.endFrame();
    }
}
