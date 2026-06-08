package dev.hoyin1600p.arcanebeam.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPlayerGameMode.class)
public abstract class SophisticatedStorageLimitedBarrelClientInteractionMixin {
    @ModifyVariable(
            method = "useItemOn",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BlockHitResult arcanebeam$sneakOpenLimitedBarrelFrontFace(
            BlockHitResult hitResult,
            LocalPlayer player,
            ClientLevel level,
            InteractionHand hand
    ) {
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        if (!(state.getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock)) {
            return hitResult;
        }

        Direction facing = limitedBarrelBlock.getFacing(state);
        if (!player.isSecondaryUseActive() || hitResult.getDirection() != facing || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return hitResult;
        }

        return hitResult.withDirection(getMenuOpenDirection(facing));
    }

    private static Direction getMenuOpenDirection(Direction facing) {
        return facing.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
    }
}
