package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import net.minecraft.world.phys.Vec3;

public record BeamTrace(BeamKind kind, BeamSettings settings,
                        Vec3 start, Vec3 end, Vec3 direction,
                        float alphaMultiplier, float beamRadiusMultiplier, float glowRadiusMultiplier) {
}
