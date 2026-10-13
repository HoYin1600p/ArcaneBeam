package dev.hoyin1600p.arcanebeam.mixin;

import dev.hoyin1600p.arcanebeam.hook.LabyrinthHooks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the labyrinth scan when a block of the client level changes (a door or trapdoor toggled, a wall mined),
 * so the scanned room is refreshed. Both server updates and chunk section updates reach the level through
 * {@code sendBlockUpdated}.
 */
@Mixin(ClientLevel.class)
public abstract class LabyrinthBlockChangeMixin {
    @Inject(
            method = "sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At("HEAD")
    )
    private void arcanebeam$labyrinthBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        LabyrinthHooks.blockChanged(pos, oldState, newState);
    }
}
