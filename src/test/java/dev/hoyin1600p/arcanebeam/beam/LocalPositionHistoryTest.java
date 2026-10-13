package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.ability.OwnershipMath;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalPositionHistoryTest {
    private static final double OWNER_DISTANCE_SQR = 1.0D;
    private static final double AMBIGUITY_MARGIN = 0.25D;
    private final LocalPositionHistory history = new LocalPositionHistory();

    private boolean ownsSound(Vec3 current, Vec3 sound, long gameTime, double... remoteDistancesSqr) {
        double[] distances = new double[remoteDistancesSqr.length + 1];
        distances[0] = history.nearestDistanceSqr(current, sound, gameTime);
        System.arraycopy(remoteDistancesSqr, 0, distances, 1, remoteDistancesSqr.length);
        return OwnershipMath.isUniqueNearest(distances, 0, OWNER_DISTANCE_SQR, AMBIGUITY_MARGIN);
    }

    @Test
    void sprintingCasterStillOwnsTheSoundVaultPlaysAtItsLaggingServerPosition() {
        // Running at 0.6 blocks a tick: the server played the sound where the client stood three ticks ago.
        for (long tick = 100; tick <= 103; tick++) {
            history.record(tick, new Vec3(0.6 * (tick - 100), 64, 0));
        }
        Vec3 current = new Vec3(1.8, 64, 0);
        Vec3 sound = new Vec3(0.0, 64, 0);
        assertTrue(current.distanceToSqr(sound) > OWNER_DISTANCE_SQR, "the old current-position check missed it");
        assertTrue(ownsSound(current, sound, 103));
    }

    @Test
    void anotherPlayerStandingOnTheSoundKeepsIt() {
        history.record(100, new Vec3(0, 64, 0));
        history.record(101, new Vec3(3, 64, 0));
        // The sound is where we were a tick ago, but a remote player is right there now.
        assertFalse(ownsSound(new Vec3(3, 64, 0), new Vec3(0, 64, 0), 101, 0.0D));
    }

    @Test
    void oldPositionsExpire() {
        history.record(100, new Vec3(0, 64, 0));
        history.record(100 + LocalPositionHistory.HISTORY_TICKS + 1, new Vec3(20, 64, 0));
        assertFalse(ownsSound(new Vec3(20, 64, 0), new Vec3(0, 64, 0), 100 + LocalPositionHistory.HISTORY_TICKS + 1));
    }

    @Test
    void currentPositionCountsWithoutHistory() {
        assertEquals(0.0D, history.nearestDistanceSqr(new Vec3(1, 2, 3), new Vec3(1, 2, 3), 50));
        assertTrue(ownsSound(new Vec3(1, 2, 3), new Vec3(1.25, 2, 3), 50));
    }

    @Test
    void clockRollbackAndClearDropHistory() {
        history.record(100, new Vec3(0, 64, 0));
        assertFalse(ownsSound(new Vec3(10, 64, 0), new Vec3(0, 64, 0), 50));
        history.record(60, new Vec3(0, 64, 0));
        history.clear();
        assertFalse(ownsSound(new Vec3(10, 64, 0), new Vec3(0, 64, 0), 60));
    }

    @Test
    void nonFinitePositionsAreIgnored() {
        history.record(100, new Vec3(Double.NaN, 64, 0));
        assertFalse(ownsSound(new Vec3(10, 64, 0), new Vec3(0, 64, 0), 100));
    }
}
