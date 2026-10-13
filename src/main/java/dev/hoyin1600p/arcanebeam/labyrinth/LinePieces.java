package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * Lines cut into short pieces (at most {@link #MAX_LENGTH} blocks) in world coordinates, as flat arrays so the
 * renderer and the visibility checks never allocate. Heights are the walking surface; the configured elevation is
 * added when drawing. Immutable apart from the cached light values.
 */
public final class LinePieces {
    /** Longest piece, in blocks. Short pieces make the frustum and sight culling fine grained. */
    public static final double MAX_LENGTH = 4.0;

    public static final LinePieces EMPTY = new LinePieces(new double[0], new double[0], new double[0], new double[0], new double[0],
            new double[0], new float[0]);

    public final int count;
    final double[] ax;
    final double[] ay;
    final double[] az;
    final double[] bx;
    final double[] by;
    final double[] bz;
    /** Distance along the whole line from its first vertex to the start of each piece (route pieces: chevron phase). */
    final float[] startLength;

    private LinePieces(double[] ax, double[] ay, double[] az, double[] bx, double[] by, double[] bz, float[] startLength) {
        this.count = ax.length;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.bx = bx;
        this.by = by;
        this.bz = bz;
        this.startLength = startLength;
    }

    /**
     * One polyline of arbitrary points, for example a route, cut into pieces and moved to world coordinates; vertices are
     * box-local and {@code ox, oy, oz} is the world position of box-local (0, 0, 0).
     */
    public static LinePieces fromPoints(float[] x, float[] y, float[] z, double ox, double oy, double oz) {
        if (x.length < 2) {
            return EMPTY;
        }
        Builder b = new Builder(pieceCount(x, y, z));
        b.add(x, y, z, ox, oy, oz, 0.0);
        return b.build();
    }

    /** A route; vertices are box-local. */
    public static LinePieces fromRoute(Route route, double ox, double oy, double oz) {
        return fromPoints(route.xs(), route.ys(), route.zs(), ox, oy, oz);
    }

    private static int pieceCount(float[] x, float[] y, float[] z) {
        int count = 0;
        for (int i = 1; i < x.length; i++) {
            count += Math.max(1, (int) Math.ceil(length(x, y, z, i) / MAX_LENGTH - 1e-9));
        }
        return count;
    }

    private static double length(float[] x, float[] y, float[] z, int i) {
        double dx = x[i] - x[i - 1];
        double dy = y[i] - y[i - 1];
        double dz = z[i] - z[i - 1];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double startX(int i) {
        return ax[i];
    }

    public double startY(int i) {
        return ay[i];
    }

    public double startZ(int i) {
        return az[i];
    }

    public double endX(int i) {
        return bx[i];
    }

    public double endY(int i) {
        return by[i];
    }

    public double endZ(int i) {
        return bz[i];
    }

    public double midX(int i) {
        return (ax[i] + bx[i]) * 0.5;
    }

    public double midY(int i) {
        return (ay[i] + by[i]) * 0.5;
    }

    public double midZ(int i) {
        return (az[i] + bz[i]) * 0.5;
    }

    public double length(int i) {
        double dx = bx[i] - ax[i];
        double dy = by[i] - ay[i];
        double dz = bz[i] - az[i];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float startLength(int i) {
        return startLength[i];
    }

    /** Total length of all pieces. */
    public double totalLength() {
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += length(i);
        }
        return sum;
    }

    private static final class Builder {
        private final double[] ax;
        private final double[] ay;
        private final double[] az;
        private final double[] bx;
        private final double[] by;
        private final double[] bz;
        private final float[] start;
        private int n;

        Builder(int capacity) {
            ax = new double[capacity];
            ay = new double[capacity];
            az = new double[capacity];
            bx = new double[capacity];
            by = new double[capacity];
            bz = new double[capacity];
            start = new float[capacity];
        }

        void add(float[] x, float[] y, float[] z, double ox, double oy, double oz, double startOffset) {
            double along = startOffset;
            for (int i = 1; i < x.length; i++) {
                double len = length(x, y, z, i);
                int pieces = Math.max(1, (int) Math.ceil(len / MAX_LENGTH - 1e-9));
                for (int k = 0; k < pieces; k++) {
                    double t0 = (double) k / pieces;
                    double t1 = (double) (k + 1) / pieces;
                    ax[n] = ox + x[i - 1] + (x[i] - x[i - 1]) * t0;
                    ay[n] = oy + y[i - 1] + (y[i] - y[i - 1]) * t0;
                    az[n] = oz + z[i - 1] + (z[i] - z[i - 1]) * t0;
                    bx[n] = ox + x[i - 1] + (x[i] - x[i - 1]) * t1;
                    by[n] = oy + y[i - 1] + (y[i] - y[i - 1]) * t1;
                    bz[n] = oz + z[i - 1] + (z[i] - z[i - 1]) * t1;
                    start[n] = (float) (along + len * t0);
                    n++;
                }
                along += len;
            }
        }

        LinePieces build() {
            return new LinePieces(ax, ay, az, bx, by, bz, start);
        }
    }
}
