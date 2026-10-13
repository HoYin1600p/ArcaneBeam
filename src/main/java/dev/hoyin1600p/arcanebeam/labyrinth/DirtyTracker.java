package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * Decides when a changed room is scanned again. Changes are batched: the update waits until they have been quiet for
 * {@link #DEBOUNCE_TICKS}, but never longer than {@link #MAX_WAIT_TICKS} after the first one, so something that keeps
 * changing (a villager and a door) cannot starve the update. Two updates are at least {@link #MIN_GAP_TICKS} apart.
 */
final class DirtyTracker {
    static final int DEBOUNCE_TICKS = 10;
    static final int MAX_WAIT_TICKS = 60;
    static final int MIN_GAP_TICKS = 20;

    private boolean dirty;
    private int firstTick;
    private int lastTick;
    private int lastUpdate = Integer.MIN_VALUE / 2;

    /** A change happened at {@code tick}. */
    void mark(int tick) {
        if (!dirty) {
            dirty = true;
            firstTick = tick;
        }
        lastTick = tick;
    }

    /** True when a change should be applied now; call {@link #started(int)} when it is. */
    boolean due(int tick) {
        return dirty && tick - lastUpdate >= MIN_GAP_TICKS
                && (tick - lastTick >= DEBOUNCE_TICKS || tick - firstTick >= MAX_WAIT_TICKS);
    }

    void started(int tick) {
        dirty = false;
        lastUpdate = tick;
    }

    boolean dirty() {
        return dirty;
    }
}
