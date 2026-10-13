package dev.hoyin1600p.arcanebeam.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateRetryPolicyTest {
    @Test
    void schedulesRetriesWithBackoffAndStopsAfterThree() {
        UpdateRetryPolicy policy = new UpdateRetryPolicy();
        int[] expectedTicks = {600, 2_400, 12_000};

        for (int expected : expectedTicks) {
            assertTrue(policy.onFailure());
            assertTrue(policy.isPending());
            int ticks = 0;
            while (!policy.tick()) {
                ticks++;
            }
            assertEquals(expected - 1, ticks);
            assertFalse(policy.isPending());
        }

        assertFalse(policy.onFailure());
        assertFalse(policy.isPending());
        assertFalse(policy.tick());
    }

    @Test
    void doesNotFireWithoutAFailure() {
        UpdateRetryPolicy policy = new UpdateRetryPolicy();

        for (int i = 0; i < 1_000; i++) {
            assertFalse(policy.tick());
        }
    }

    @Test
    void resetRestoresTheFullBudget() {
        UpdateRetryPolicy policy = new UpdateRetryPolicy();
        policy.onFailure();
        policy.onFailure();
        policy.onFailure();
        policy.reset();

        assertFalse(policy.isPending());
        assertTrue(policy.onFailure());
    }
}
