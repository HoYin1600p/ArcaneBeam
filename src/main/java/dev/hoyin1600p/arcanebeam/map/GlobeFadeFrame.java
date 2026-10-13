package dev.hoyin1600p.arcanebeam.map;

/**
 * Owns the render state changes of one faded rooms pass: capture on {@link #begin()}, per-room alpha via
 * {@link #setFactor(float)}, and a guaranteed restore in {@link #end()}. The GL access sits behind {@link Gl}
 * so the begin/end contract is unit tested. Nothing is queried or changed until {@link #begin()} is called,
 * and {@link #end()} is idempotent so every exit path of a frame can call it.
 */
final class GlobeFadeFrame {
    /** Minimal render-state surface the fade needs. */
    interface Gl {
        /** Copies the current shader colour into {@code out} (length 4) and remembers the blend state. */
        void captureAndEnableAlphaBlend(float[] out);

        void setColor(float r, float g, float b, float a);

        /** Restores the blend state remembered by the last capture. */
        void restoreBlend();
    }

    private final Gl gl;
    private final float[] base = new float[4];
    private boolean active;

    GlobeFadeFrame(Gl gl) {
        this.gl = gl;
    }

    boolean isActive() {
        return active;
    }

    float baseAlpha() {
        return base[3];
    }

    void begin() {
        if (active) {
            end();
        }
        gl.captureAndEnableAlphaBlend(base);
        active = true;
    }

    void setFactor(float factor) {
        if (active) {
            gl.setColor(base[0], base[1], base[2], base[3] * factor);
        }
    }

    /** Restores colour and blend if a pass was begun; safe to call any number of times. */
    void end() {
        if (!active) {
            return;
        }
        active = false;
        try {
            gl.setColor(base[0], base[1], base[2], base[3]);
        } finally {
            gl.restoreBlend();
        }
    }
}
