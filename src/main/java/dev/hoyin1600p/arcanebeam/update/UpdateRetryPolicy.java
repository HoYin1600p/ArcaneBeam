package dev.hoyin1600p.arcanebeam.update;

/**
 * Bounded retry schedule for failed manifest fetches, counted in client ticks
 * so it can be driven from the existing tick handler.
 */
final class UpdateRetryPolicy {
    /** 30 seconds, 2 minutes, then 10 minutes at 20 ticks per second. */
    private static final int[] DELAY_TICKS = {600, 2_400, 12_000};

    private int retriesScheduled;
    private int ticksRemaining = -1;

    /** Records a failure; returns true when a retry has been scheduled. */
    boolean onFailure() {
        if (retriesScheduled >= DELAY_TICKS.length) {
            ticksRemaining = -1;
            return false;
        }
        ticksRemaining = DELAY_TICKS[retriesScheduled++];
        return true;
    }

    /** Advances one tick; returns true when a retry is due now. */
    boolean tick() {
        if (ticksRemaining < 0) {
            return false;
        }
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            ticksRemaining = -1;
            return true;
        }
        return false;
    }

    boolean isPending() {
        return ticksRemaining >= 0;
    }

    void reset() {
        retriesScheduled = 0;
        ticksRemaining = -1;
    }
}
