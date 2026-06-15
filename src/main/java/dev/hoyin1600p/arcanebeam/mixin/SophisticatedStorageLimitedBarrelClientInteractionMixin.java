package dev.hoyin1600p.arcanebeam.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class SophisticatedStorageLimitedBarrelClientInteractionMixin {
    @Unique
    private boolean arcanebeam$spoofedLimitedBarrelSneakRelease = false;

    @Unique
    private InteractionHand arcanebeam$limitedBarrelInteractionHand = null;

    @ModifyVariable(
            method = "useItemOn",
            at = @At("HEAD"),
            argsOnly = true,
            index = 4
    )
    private BlockHitResult arcanebeam$trackLimitedBarrelInteractionHand(
            BlockHitResult hitResult,
            LocalPlayer player,
            ClientLevel level,
            InteractionHand hand
    ) {
        this.arcanebeam$limitedBarrelInteractionHand = hand;
        return hitResult;
    }

    @ModifyArg(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundUseItemOnPacket;<init>(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)V"
            ),
            index = 1
    )
    private BlockHitResult arcanebeam$sneakOpenLimitedBarrelFrontFacePacket(BlockHitResult hitResult) {
        if (this.arcanebeam$limitedBarrelInteractionHand != InteractionHand.MAIN_HAND) {
            return hitResult;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.getConnection() == null) {
            return hitResult;
        }

        BlockHitResult rewrittenHitResult = arcanebeam$rewriteLimitedBarrelFrontFace(hitResult, player, level);
        if (rewrittenHitResult == hitResult) {
            return hitResult;
        }

        // Multiplayer also checks both hands before block.use while sneaking. Release sneak only for this packet
        // so a main-hand-empty front-face click reaches Sophisticated Storage's normal non-front-face open path.
        minecraft.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        this.arcanebeam$spoofedLimitedBarrelSneakRelease = true;
        return rewrittenHitResult;
    }

    @Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
    private void arcanebeam$restoreLimitedBarrelSneak(
            LocalPlayer player,
            ClientLevel level,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!this.arcanebeam$spoofedLimitedBarrelSneakRelease) {
            return;
        }

        this.arcanebeam$spoofedLimitedBarrelSneakRelease = false;
        this.arcanebeam$limitedBarrelInteractionHand = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && player.isSecondaryUseActive()) {
            minecraft.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void arcanebeam$clearLimitedBarrelInteractionHand(
            LocalPlayer player,
            ClientLevel level,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        this.arcanebeam$limitedBarrelInteractionHand = null;
    }

    private static BlockHitResult arcanebeam$rewriteLimitedBarrelFrontFace(BlockHitResult hitResult, LocalPlayer player, ClientLevel level) {
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        if (!(state.getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock)) {
            return hitResult;
        }

        Direction facing = limitedBarrelBlock.getFacing(state);
        if (!player.isSecondaryUseActive() || hitResult.getDirection() != facing || !player.getMainHandItem().isEmpty()) {
            return hitResult;
        }

        return hitResult.withDirection(getMenuOpenDirection(facing));
    }

    private static Direction getMenuOpenDirection(Direction facing) {
        return facing.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
    }
}
