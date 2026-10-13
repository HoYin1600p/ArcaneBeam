package dev.hoyin1600p.arcanebeam.ability.manastealer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class ManaStealerCommand {
    /** Longest preview the command accepts: 12000 ticks, 10 minutes. */
    static final int MAX_PREVIEW_TICKS = 12_000;

    private ManaStealerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mana_stealer")
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> report(context.getSource())))
                .then(Commands.literal("on")
                        .executes(context -> setEnabled(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setEnabled(context.getSource(), false)))
                .then(Commands.literal("preview")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, MAX_PREVIEW_TICKS))
                                .executes(context -> startPreview(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "ticks")))
                                .then(Commands.argument("center", Vec3Argument.vec3(false))
                                        .executes(context -> startPreview(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ticks"),
                                                Vec3Argument.getVec3(context, "center")))))
                        .then(Commands.literal("stop")
                                .executes(context -> stopPreview(context.getSource()))))
                .then(Commands.literal("legacy_sigil")
                        .then(Commands.literal("show")
                                .executes(context -> setSigil(context.getSource(), false)))
                        .then(Commands.literal("hide")
                                .executes(context -> setSigil(context.getSource(), true))))
                .then(Commands.literal("sigil")
                        .then(Commands.literal("on")
                                .executes(context -> setSigil(context.getSource(), false)))
                        .then(Commands.literal("off")
                                .executes(context -> setSigil(context.getSource(), true))))
                .then(Commands.literal("stream")
                        .executes(context -> report(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setStream(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setStream(context.getSource(), false))));
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        ManaStealerVisualConfig.setEnabled(enabled);
        return report(source);
    }

    private static int setSigil(CommandSourceStack source, boolean replace) {
        ManaStealerVisualConfig.setReplaceGroundSigil(replace);
        return report(source);
    }

    private static int setStream(CommandSourceStack source, boolean enabled) {
        ManaStealerVisualConfig.setDrainStreamEnabled(enabled);
        return report(source);
    }

    private static int startPreview(CommandSourceStack source, int durationTicks) {
        ManaStealerPreviewController.StartResult result = ManaStealerPreviewController.start(durationTicks);
        return reportPreviewStart(source, result);
    }

    private static int startPreview(
            CommandSourceStack source,
            int durationTicks,
            Vec3 center
    ) {
        ManaStealerPreviewController.StartResult result = ManaStealerPreviewController.start(
                durationTicks,
                center
        );
        return reportPreviewStart(source, result);
    }

    private static int reportPreviewStart(
            CommandSourceStack source,
            ManaStealerPreviewController.StartResult result
    ) {
        if (!result.started()) {
            source.sendFailure(new TextComponent("[Arcane Beam] Mana Stealer preview could not start: " + result.error()));
            return 0;
        }
        source.sendSuccess(
                new TextComponent(
                        String.format(Locale.ROOT,
                                "[Arcane Beam] Started client-only Mana Stealer preview for %d ticks at %.1f, %.1f, %.1f "
                                        + "(radius 6.0; visual/audio only, no mana drain).",
                                result.durationTicks(),
                                result.center().x,
                                result.center().y,
                                result.center().z
                        )
                ),
                false
        );
        return 1;
    }

    private static int stopPreview(CommandSourceStack source) {
        boolean stopped = ManaStealerPreviewController.stop();
        source.sendSuccess(
                new TextComponent(stopped
                        ? "[Arcane Beam] Stopped replenishing the Mana Stealer preview; existing orbs will finish naturally."
                        : "[Arcane Beam] No Mana Stealer preview is running."),
                false
        );
        return stopped ? 1 : 0;
    }

    private static int report(CommandSourceStack source) {
        source.sendSuccess(
                new TextComponent(
                        "[Arcane Beam] Mana Stealer visuals: configured " + state(ManaStealerVisualConfig.configuredEnabled())
                                + ", active " + activeState()
                                + ", Legacy sigil: "
                                + (ManaStealerVisualConfig.replaceGroundSigil() ? "hidden" : "shown")
                                + ", player drain stream " + state(ManaStealerVisualConfig.drainStreamEnabled())
                                + ", stream targets at 6 blocks All/Decreased/Minimal "
                                + streamTarget(ManaStealerDrainStreamPolicy.ALL) + "/"
                                + streamTarget(ManaStealerDrainStreamPolicy.DECREASED) + "/"
                                + streamTarget(ManaStealerDrainStreamPolicy.MINIMAL)
                                + ", live targets All/Decreased/Minimal "
                                + ManaStealerVisualConfig.allPopulation() + "/"
                                + ManaStealerVisualConfig.decreasedPopulation() + "/"
                                + ManaStealerVisualConfig.minimalPopulation()
                                + ". Changes apply immediately."
                ),
                false
        );
        return 1;
    }

    private static String activeState() {
        if (!ManaStealerOwnership.owns()) {
            return "OFF (legacy VRO module owns the visuals)";
        }
        if (!ManaStealerOwnership.mixinsAvailable()) {
            return "OFF (mixins were not applied; see the log)";
        }
        return state(ManaStealerVisualController.canReplace());
    }

    private static String state(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static int streamTarget(int quality) {
        return ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D,
                ManaStealerVisualConfig.drainStreamDensity(),
                ManaStealerVisualConfig.drainStreamMinimumOrbs(),
                ManaStealerVisualConfig.drainStreamMaximumOrbs(),
                quality
        );
    }
}
