package dev.hoyin1600p.arcanebeam.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnershipMathTest {
    @Test
    void theOnlyNearbyPlayerOwnsTheSound() {
        assertTrue(OwnershipMath.isUniqueNearest(new double[]{0.25D, 100.0D}, 0, 6.25D, 0.25D));
    }

    @Test
    void aTeammateStandingAsCloseMakesItAmbiguous() {
        assertFalse(OwnershipMath.isUniqueNearest(new double[]{0.25D, 0.3D}, 0, 6.25D, 0.25D));
    }

    @Test
    void aSoundFarFromThePlayerIsNotTheirs() {
        assertFalse(OwnershipMath.isUniqueNearest(new double[]{50.0D}, 0, 6.25D, 0.25D));
    }

    @Test
    void aCloserOtherPlayerWins() {
        assertFalse(OwnershipMath.isUniqueNearest(new double[]{4.0D, 0.5D}, 0, 6.25D, 0.25D));
    }

    @Test
    void reachIsABoxNotASphere() {
        assertTrue(OwnershipMath.withinReach(20.0D, 5.0D, 10.0D, 24.0D, 16.0D));
        assertFalse(OwnershipMath.withinReach(20.0D, 5.0D, 20.0D, 24.0D, 16.0D));
        assertFalse(OwnershipMath.withinReach(1.0D, 17.0D, 1.0D, 24.0D, 16.0D));
    }
}
