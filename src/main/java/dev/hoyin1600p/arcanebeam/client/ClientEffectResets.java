package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.ability.altar.VaultAltarBeamManager;
import dev.hoyin1600p.arcanebeam.ability.blastwave.BlastWaveVisualManager;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.smite.SmiteVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.beam.RemotePlayerParticleManager;
import dev.hoyin1600p.arcanebeam.beam.ResourcePackParticleStyle;
import dev.hoyin1600p.arcanebeam.labyrinth.LabyrinthManager;

import java.util.List;

/** The per-level effect state that is cleared whenever the client level changes, in the order it is cleared. */
final class ClientEffectResets {
    private static final List<Runnable> RESETS = List.of(
            StormArrowVisualManager::clear,
            LightningStrikeShockwaveManager::clear,
            BlastWaveVisualManager::clear,
            SmiteVisualManager::clear,
            VaultAltarBeamManager::clear,
            RemotePlayerParticleManager::clear,
            LabyrinthManager::clear,
            ResourcePackParticleStyle::clear
    );

    private ClientEffectResets() {
    }

    static void runAll() {
        for (Runnable reset : RESETS) {
            try {
                reset.run();
            } catch (RuntimeException | LinkageError failure) {
                // One broken reset must not leave the others (and the level change) half done.
                ArcaneBeam.LOGGER.warn("Arcane Beam could not reset per-level effect state", failure);
            }
        }
    }
}
