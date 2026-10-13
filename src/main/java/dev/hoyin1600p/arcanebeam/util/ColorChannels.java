package dev.hoyin1600p.arcanebeam.util;

/** Splits a packed 0xRRGGBB colour into unit-range float channels. */
public final class ColorChannels {
    private ColorChannels() {
    }

    public static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    public static float[] rgb(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        };
    }
}
