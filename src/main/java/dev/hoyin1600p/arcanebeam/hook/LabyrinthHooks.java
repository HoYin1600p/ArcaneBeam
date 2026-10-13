package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.labyrinth.LabyrinthManager;
import dev.hoyin1600p.arcanebeam.util.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Entry point for the client level mixin that reports block changes to the labyrinth scan. Never throws. */
public final class LabyrinthHooks {
    private LabyrinthHooks() {
    }

    public static void blockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        if (Features.LABYRINTH.isDisabled() || !Minecraft.getInstance().isSameThread()) {
            return;
        }
        try {
            LabyrinthManager.onBlockChanged(pos, oldState, newState);
        } catch (RuntimeException | LinkageError failure) {
            Features.LABYRINTH.fail(failure);
        }
    }
}
