package dev.hoyin1600p.arcanebeam.beam;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneParticleSuppressionTest {
    private static final UUID LOCAL = new UUID(0L, 1L);
    private static final UUID REMOTE = new UUID(0L, 2L);
    private final LocalArcaneParticleTracker tracker = new LocalArcaneParticleTracker();
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    void ownerlessParticleOnRecentFiringPathIsSuppressedDuringLocalReplacement() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertTrue(tracker.shouldSuppress(true, LOCAL, null, 101, new Vec3(0.1, 0, 8)));
    }

    @Test
    void ownerlessParticleFarOffFiringPathIsNotSuppressed() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(true, LOCAL, null, 101, new Vec3(-4, 0, 8)));
    }

    @Test
    void ownerlessParticleWithoutAnyRecordedPathIsNotSuppressed() {
        assertFalse(tracker.shouldSuppress(true, LOCAL, null, 100, new Vec3(-2, 0, 8)));
    }

    @Test
    void identifiedLocalParticlesAreSuppressedDuringLocalReplacement() {
        assertTrue(tracker.shouldSuppress(true, LOCAL, LOCAL, 100, new Vec3(0, 0, 8)));
    }

    @Test
    void identifiedRemoteParticlesRemainAvailableToRemoteProfiles() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 100, new Vec3(4, 0, 8)));
        assertFalse(tracker.shouldSuppress(false, LOCAL, REMOTE, 100, new Vec3(4, 0, 8)));
    }

    @Test
    void unknownParticlesRemainVanillaOutsideLocalReplacementWindow() {
        assertFalse(tracker.shouldSuppress(false, LOCAL, null, 100, Vec3.ZERO));
    }

    @Test
    void disablingReplacementOrEndingItsTailPreservesLocalParticles() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(false, LOCAL, LOCAL, 101, new Vec3(0, 0, 8)));
        assertFalse(tracker.shouldSuppress(false, LOCAL, REMOTE, 101, new Vec3(0, 0, 8)));
    }

    @Test
    void strafingKeepsOldLocalPathEvenWhenCurrentRayHeuristicAssignsItToAnotherPlayer() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        tracker.record(101, new Vec3(1, 0, 0), FORWARD);
        tracker.record(102, new Vec3(2, 0, 0), FORWARD);
        // Original particles arrive on x=0 while our replacement has moved to x=2.
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 103, new Vec3(0.09, 0, 16.07)));
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 103, new Vec3(1.05, 0, 8)));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 103, new Vec3(-3, 0, 8)));
    }

    @Test
    void fastTurnKeepsPreviousAimDirectionAndItsImpactParticles() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        tracker.record(101, Vec3.ZERO, new Vec3(1, 0, 0));
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 102, new Vec3(0, 0, 32)));
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 102, new Vec3(32, 0, 0)));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 102, new Vec3(16, 0, 16)));
    }

    @Test
    void preactivationPoseIsUsedOnlyAfterLocalCastIsConfirmed() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(false, LOCAL, REMOTE, 101, new Vec3(0, 0, 8)));
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 103, new Vec3(0, 0, 8)));
    }

    @Test
    void recentPathSurvivesReleaseTailButExpiresDuringLaterCasting() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertTrue(tracker.shouldSuppress(true, LOCAL, REMOTE, 108, new Vec3(0, 0, 8)));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 121, new Vec3(0, 0, 8)));
    }

    @Test
    void historyDoesNotBecomeAnInfiniteRayOrAWholeWorldParticleFilter() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 101, new Vec3(0, 0, -2)));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 101, new Vec3(0, 0, 80)));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 101, new Vec3(0, 2, 8)));
    }

    @Test
    void worldChangeOrDisabledReplacementClearsHistoryEvenWithTheSameGameTime() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        tracker.clear();
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 100, new Vec3(0, 0, 8)));
    }

    @Test
    void clockRollbackAndInvalidDirectionsCannotRetainFalseOwnership() {
        tracker.record(100, Vec3.ZERO, FORWARD);
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 10, new Vec3(0, 0, 8)));
        tracker.record(10, Vec3.ZERO, Vec3.ZERO);
        tracker.record(10, Vec3.ZERO, new Vec3(Double.NaN, 0, 1));
        assertFalse(tracker.shouldSuppress(true, LOCAL, REMOTE, 11, new Vec3(0, 0, 8)));
    }

    @Test
    void keyPressWindowCoversTheFirstTickSentBeforeVaultConfirmsTheCast() {
        // Key pressed at 100 (armed until 120); Vault's first fireRay particles arrive at 101, before the
        // AbilityActivityMessage that confirms the cast.
        tracker.record(100, Vec3.ZERO, FORWARD);
        boolean window = LocalArcaneParticleTracker.suppressionWindowOpen(false, 101, 120);
        assertTrue(window);
        assertTrue(tracker.shouldSuppress(window, LOCAL, LOCAL, 101, new Vec3(0, 0, 8)));
        assertTrue(tracker.shouldSuppress(window, LOCAL, null, 101, new Vec3(0.1, 0, 12)));
        // Particles off our path still belong to whoever cast them.
        assertFalse(tracker.shouldSuppress(window, LOCAL, REMOTE, 101, new Vec3(4, 0, 8)));
    }

    @Test
    void windowClosesWhenNeitherConfirmedNorRecentlyArmed() {
        assertTrue(LocalArcaneParticleTracker.suppressionWindowOpen(true, 500, Long.MIN_VALUE));
        assertTrue(LocalArcaneParticleTracker.suppressionWindowOpen(false, 120, 120));
        assertFalse(LocalArcaneParticleTracker.suppressionWindowOpen(false, 121, 120));
        assertFalse(LocalArcaneParticleTracker.suppressionWindowOpen(false, 0, Long.MIN_VALUE));
    }
}
