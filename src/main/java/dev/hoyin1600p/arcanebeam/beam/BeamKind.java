package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;

public enum BeamKind {
    ARCANE,
    RAIL;

    public BeamSettings settings() {
        return this == RAIL ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
    }
}
