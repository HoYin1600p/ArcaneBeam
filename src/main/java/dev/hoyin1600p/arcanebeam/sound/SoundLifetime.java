package dev.hoyin1600p.arcanebeam.sound;

/** How long a one-shot replacement sound may live before the controller stops it as a safety net. */
public final class SoundLifetime {
    /** The engine ends a sound by itself when the file ends; this is slack on top of the real length. */
    static final int GRACE_TICKS = 40;
    /** Used when the file length cannot be read: long enough for any sensible ability sound. */
    static final int UNKNOWN_LENGTH_TICKS = 600;
    static final int MAX_TICKS = 2400;

    private SoundLifetime() {
    }

    public static int ticksFor(double durationSeconds) {
        if (!(durationSeconds > 0.0D) || Double.isInfinite(durationSeconds)) {
            return UNKNOWN_LENGTH_TICKS;
        }
        long ticks = (long) Math.ceil(durationSeconds * 20.0D) + GRACE_TICKS;
        return (int) Math.min(MAX_TICKS, ticks);
    }
}
