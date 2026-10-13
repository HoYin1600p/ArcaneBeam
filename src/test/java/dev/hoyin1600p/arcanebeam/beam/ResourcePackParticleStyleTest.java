package dev.hoyin1600p.arcanebeam.beam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePackParticleStyleTest {
    private static final double HALF_PI = Math.PI / 2.0D;

    @Test
    void beamPointingUpOnScreenNeedsNoRoll() {
        assertEquals(0.0F, ResourcePackParticleStyle.roll(0.0D, 1.0D, 0.0F), 1.0E-6F);
    }

    @Test
    void beamPointingRightRollsAQuarterTurnClockwise() {
        assertEquals((float) HALF_PI, ResourcePackParticleStyle.roll(1.0D, 0.0D, 0.0F), 1.0E-6F);
    }

    @Test
    void beamPointingLeftRollsAQuarterTurnCounterclockwise() {
        assertEquals((float) -HALF_PI, ResourcePackParticleStyle.roll(-1.0D, 0.0D, 0.0F), 1.0E-6F);
    }

    @Test
    void beamUpAndRightRollsClockwiseByItsAngle() {
        assertEquals((float) (Math.PI / 4.0D), ResourcePackParticleStyle.roll(1.0D, 1.0D, 0.0F), 1.0E-6F);
    }

    @Test
    void rotationOffsetIsAppliedClockwise() {
        assertEquals((float) HALF_PI, ResourcePackParticleStyle.roll(0.0D, 1.0D, (float) HALF_PI), 1.0E-6F);
    }

    @Test
    void beamStraightIntoTheScreenKeepsOnlyTheOffset() {
        assertEquals(0.5F, ResourcePackParticleStyle.roll(0.0D, 0.0D, 0.5F), 1.0E-6F);
    }

    @Test
    void scaleIsClampedAndNaNFallsBackToOne() {
        assertEquals(ResourcePackParticleStyle.MIN_SCALE, ResourcePackParticleStyle.clampScale(0.0F));
        assertEquals(ResourcePackParticleStyle.MAX_SCALE, ResourcePackParticleStyle.clampScale(100.0F));
        assertEquals(1.0F, ResourcePackParticleStyle.clampScale(Float.NaN));
    }

    @Test
    void aPreparedStyleDoesNotSurviveClearPendingOrALevelChange() {
        ResourcePackParticleStyle.prepare(dev.hoyin1600p.arcanebeam.config.model.BeamSettings.defaultArcane(), net.minecraft.world.phys.Vec3.ZERO);
        org.junit.jupiter.api.Assertions.assertTrue(ResourcePackParticleStyle.hasPending());
        ResourcePackParticleStyle.clearPending();
        org.junit.jupiter.api.Assertions.assertFalse(ResourcePackParticleStyle.hasPending());

        ResourcePackParticleStyle.prepare(dev.hoyin1600p.arcanebeam.config.model.BeamSettings.defaultArcane(), net.minecraft.world.phys.Vec3.ZERO);
        ResourcePackParticleStyle.clear();
        org.junit.jupiter.api.Assertions.assertFalse(ResourcePackParticleStyle.hasPending());
    }
}
