package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/**
 * Line-of-sight cache for {@link LinePieces}. A piece is drawn only while it is known to be visible: each piece in
 * range is checked with a ray from the eye (by the {@link Sight} the game supplies), the answer is cached, and the
 * pieces are re-checked round robin with a hard per-tick budget so the cost stays flat however long the lines are.
 * Pieces out of range cost nothing and are forgotten, so they are checked again when they come back into range.
 * Not thread safe: used from the client thread only.
 */
public final class PieceCuller {
    /** A piece is checked again after this many ticks. */
    public static final int RECHECK_TICKS = 10;
    /** Pieces checked per tick (up to three rays each). */
    public static final int DEFAULT_BUDGET = 48;
    /** Pieces looked at per tick, checked or not, so the distance filter is cheap on long networks. */
    public static final int DEFAULT_SCAN_LIMIT = 512;

    /** The game's line-of-sight test for one piece. */
    @FunctionalInterface
    public interface Sight {
        boolean visible(int piece);
    }

    private static final byte UNKNOWN = 0;
    private static final byte VISIBLE = 1;
    private static final byte HIDDEN = 2;

    private final int count;
    private final byte[] state;
    private final int[] checkedAt;
    private int cursor;

    public PieceCuller(int count) {
        this.count = count;
        this.state = new byte[count];
        this.checkedAt = new int[count];
    }

    /** True when the piece was last seen from the eye and is still within range. */
    public boolean visible(int piece) {
        return state[piece] == VISIBLE;
    }

    /** Pieces currently known visible, for statistics. */
    public int visibleCount() {
        int n = 0;
        for (byte s : state) {
            if (s == VISIBLE) {
                n++;
            }
        }
        return n;
    }

    /** Pieces that have never been checked or were forgotten. */
    public int unknownCount() {
        int n = 0;
        for (byte s : state) {
            if (s == UNKNOWN) {
                n++;
            }
        }
        return n;
    }

    /**
     * Takes over the cached answer of {@code oldPiece} of another culler for {@code piece}, so a rebuilt set of
     * pieces that lies in the same place does not flicker while it is checked again.
     */
    public void adopt(int piece, PieceCuller old, int oldPiece) {
        state[piece] = old.state[oldPiece];
        checkedAt[piece] = old.checkedAt[oldPiece];
    }

    /** Forgets every answer, for example after a rescan. */
    public void reset() {
        Arrays.fill(state, UNKNOWN);
        cursor = 0;
    }

    /**
     * Checks up to {@code budget} pieces that are in range and unknown or stale, starting where the last call
     * stopped, and looks at no more than {@code scanLimit} pieces. Returns the number of sight checks made.
     */
    public int refresh(int tick, LinePieces pieces, double eyeX, double eyeY, double eyeZ, double maxDistance, int budget, int scanLimit,
            Sight sight) {
        if (count == 0 || pieces.count != count) {
            return 0;
        }
        double range = maxDistance + LinePieces.MAX_LENGTH;
        double rangeSq = range * range;
        int checks = 0;
        int looked = Math.min(count, scanLimit);
        for (int n = 0; n < looked && checks < budget; n++) {
            int i = cursor;
            cursor = cursor + 1 == count ? 0 : cursor + 1;
            double dx = pieces.midX(i) - eyeX;
            double dy = pieces.midY(i) - eyeY;
            double dz = pieces.midZ(i) - eyeZ;
            if (dx * dx + dy * dy + dz * dz > rangeSq) {
                state[i] = UNKNOWN;
                continue;
            }
            if (state[i] == UNKNOWN || tick - checkedAt[i] >= RECHECK_TICKS) {
                state[i] = sight.visible(i) ? VISIBLE : HIDDEN;
                checkedAt[i] = tick;
                checks++;
            }
        }
        return checks;
    }
}
