package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The local player's recent feet positions, one per client tick.
 *
 * <p>Vault plays Arcane's per-tick cast sound at the caster's <em>server</em> position, which trails the client's
 * own position by the round trip. While the player runs, jumps or dashes that gap easily passes one block, so a
 * sound compared only with the current client position stops being recognised as the player's own and Vault's
 * sound leaks through. Comparing with the positions of the last few ticks covers that lag.
 */
final class LocalPositionHistory {
    /** Long enough for a 500 ms round trip; short enough that a spot left long ago is not still "ours". */
    static final long HISTORY_TICKS = 10L;
    private static final int MAX_ENTRIES = 32;

    private final Deque<Entry> entries = new ArrayDeque<>();

    void record(long gameTime, Vec3 position) {
        expire(gameTime);
        if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            return;
        }
        Entry latest = entries.peekLast();
        if (latest != null && latest.gameTime == gameTime) {
            entries.removeLast();
        }
        entries.addLast(new Entry(gameTime, position));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    /**
     * Smallest squared distance from {@code point} to the current position or any position of the last
     * {@link #HISTORY_TICKS} ticks.
     */
    double nearestDistanceSqr(Vec3 current, Vec3 point, long gameTime) {
        expire(gameTime);
        double best = current == null ? Double.MAX_VALUE : current.distanceToSqr(point);
        for (Entry entry : entries) {
            best = Math.min(best, entry.position.distanceToSqr(point));
        }
        return best;
    }

    void clear() {
        entries.clear();
    }

    private void expire(long gameTime) {
        if (!entries.isEmpty() && gameTime < entries.peekLast().gameTime) {
            entries.clear();
        }
        while (!entries.isEmpty() && gameTime - entries.peekFirst().gameTime > HISTORY_TICKS) {
            entries.removeFirst();
        }
    }

    private record Entry(long gameTime, Vec3 position) {
    }
}
