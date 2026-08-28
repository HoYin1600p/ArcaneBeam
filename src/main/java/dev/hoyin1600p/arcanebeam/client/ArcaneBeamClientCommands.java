package dev.hoyin1600p.arcanebeam.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

final class ArcaneBeamClientCommands {
    private ArcaneBeamClientCommands() {
    }

    static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("arcanebeam")
                .then(Commands.literal("updates")
                        .executes(ArcaneBeamClientCommands::showUpdateStatus)
                        .then(Commands.literal("status")
                                .executes(ArcaneBeamClientCommands::showUpdateStatus))
                        .then(Commands.literal("on")
                                .executes(context -> setUpdatesEnabled(context, true)))
                        .then(Commands.literal("off")
                                .executes(context -> setUpdatesEnabled(context, false)))));
    }

    private static int showUpdateStatus(CommandContext<CommandSourceStack> context) {
        boolean enabled = ArcaneBeamConfig.updateChecksEnabled();
        context.getSource().sendSuccess(new TextComponent(
                "Arcane Beam update checks are " + (enabled ? "enabled" : "disabled") + "."
        ), false);
        return 1;
    }

    private static int setUpdatesEnabled(
            CommandContext<CommandSourceStack> context,
            boolean enabled
    ) {
        ArcaneBeamConfig.setUpdateChecksEnabled(enabled);
        context.getSource().sendSuccess(new TextComponent(
                "Arcane Beam update checks are now "
                        + (enabled ? "enabled" : "disabled")
                        + ". The change is immediate."
        ), false);
        return 1;
    }
}
