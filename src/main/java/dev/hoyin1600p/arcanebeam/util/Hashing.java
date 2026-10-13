package dev.hoyin1600p.arcanebeam.util;

import net.minecraft.util.Mth;

/** Small deterministic hash helpers used to vary visuals without a random source. */
public final class Hashing {
    private static final double UNIT_DOUBLE = 0x1.0p-53;

    private Hashing() {
    }

    public static float hash01(float value) {
        return Mth.frac(Mth.sin(value * 12.9898F) * 43758.547F);
    }

    public static double unit(long value) {
        return (value >>> 11) * UNIT_DOUBLE;
    }

    public static long mix64(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
