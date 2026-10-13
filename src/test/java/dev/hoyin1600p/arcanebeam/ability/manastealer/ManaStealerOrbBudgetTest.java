package dev.hoyin1600p.arcanebeam.ability.manastealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManaStealerOrbBudgetTest {
    @Test
    void singleSourceKeepsRequestedTarget() {
        assertEquals(80, ManaStealerPopulationPolicy.fairTarget(80, 1, 1024));
    }

    @Test
    void manySourcesShareTheGlobalBudget() {
        int share = ManaStealerPopulationPolicy.fairTarget(512, 64, 1024);
        assertEquals(16, share);
        assertTrue(share * 64 <= 1024);
    }

    @Test
    void zeroTargetStaysZeroAndEverySourceGetsAtLeastOne() {
        assertEquals(0, ManaStealerPopulationPolicy.fairTarget(0, 4, 1024));
        assertEquals(1, ManaStealerPopulationPolicy.fairTarget(80, 5000, 1024));
    }

    @Test
    void remainingBudgetNeverNegative() {
        assertEquals(24, ManaStealerPopulationPolicy.remainingBudget(1000, 1024));
        assertEquals(0, ManaStealerPopulationPolicy.remainingBudget(2000, 1024));
    }

    @Test
    void streamCountKeepsOrbsWithinBudget() {
        assertEquals(21, ManaStealerDrainStreamPolicy.maxStreams(1024, 48));
        assertEquals(1, ManaStealerDrainStreamPolicy.maxStreams(1024, 5000));
        assertEquals(4, ManaStealerDrainStreamPolicy.maxStreams(1024, 256));
    }
}
