package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ability.altar.VaultAltarBeamManager;
import dev.hoyin1600p.arcanebeam.ability.blastwave.BlastWaveVisualManager;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.smite.SmiteVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.beam.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.beam.RemotePlayerParticleManager;
import dev.hoyin1600p.arcanebeam.labyrinth.LabyrinthManager;
import dev.hoyin1600p.arcanebeam.util.FeatureGuard;
import dev.hoyin1600p.arcanebeam.util.Features;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.Consumer;

/**
 * Registers the feature managers' tick, render and entity-join handlers on the Forge event bus, each behind the
 * kill switch of its feature. An exception thrown inside any of them is logged once, switches that feature off for
 * the session and never reaches the game loop (where it would become a crash report). The registration order is
 * the order the managers used to be registered in by annotation, so handlers run in the same order as before.
 */
final class GuardedEvents {
    private GuardedEvents() {
    }

    static void register() {
        listen(TickEvent.ClientTickEvent.class, Features.ALTAR, VaultAltarBeamManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.ALTAR, VaultAltarBeamManager::onRenderLevelStage);

        listen(TickEvent.ClientTickEvent.class, Features.BLAST_WAVE, BlastWaveVisualManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.BLAST_WAVE, BlastWaveVisualManager::onRenderLevelStage);

        listen(EntityJoinWorldEvent.class, Features.LIGHTNING, LightningStrikeShockwaveManager::onEntityJoinWorld);
        listen(TickEvent.ClientTickEvent.class, Features.LIGHTNING, LightningStrikeShockwaveManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.LIGHTNING, LightningStrikeShockwaveManager::onRenderLevelStage);

        listen(TickEvent.ClientTickEvent.class, Features.SMITE, SmiteVisualManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.SMITE, SmiteVisualManager::onRenderLevelStage);

        listen(EntityJoinWorldEvent.class, Features.STORM_ARROW, StormArrowVisualManager::onEntityJoinWorld);
        listen(TickEvent.ClientTickEvent.class, Features.STORM_ARROW, StormArrowVisualManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.STORM_ARROW, StormArrowVisualManager::onRenderLevelStage);

        listen(TickEvent.ClientTickEvent.class, Features.LABYRINTH, LabyrinthManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.LABYRINTH, LabyrinthManager::onRenderLevelStage);

        listen(TickEvent.ClientTickEvent.class, Features.ARCANE_BEAM, ArcaneBeamManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.ARCANE_BEAM, ArcaneBeamManager::onRenderLevelStage);

        listen(TickEvent.ClientTickEvent.class, Features.PARTICLES, RemotePlayerParticleManager::onClientTick);
        listen(RenderLevelStageEvent.class, Features.PARTICLES, RemotePlayerParticleManager::onRenderLevelStage);
    }

    /** Subscribes {@code handler} to {@code type} on the Forge bus, behind {@code guard}. */
    static <E extends Event> void listen(Class<E> type, FeatureGuard guard, Consumer<E> handler) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, guard.wrap(handler));
    }
}
