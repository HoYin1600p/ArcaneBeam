package dev.hoyin1600p.arcanebeam.util;

public final class IntMath {
    private IntMath() {
    }

    /** Clamps to [min, max]; when min exceeds max the result is min. */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
