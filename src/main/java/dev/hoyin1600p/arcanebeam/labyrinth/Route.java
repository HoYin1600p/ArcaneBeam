package dev.hoyin1600p.arcanebeam.labyrinth;

/** A path from a cell to the exit: box-local vertices, y being the walking surface. */
public final class Route {
    private final float[] x;
    private final float[] y;
    private final float[] z;

    Route(float[] x, float[] y, float[] z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** A route through the points with every point dropped that lies within {@code tolerance} of the line through its neighbours. */
    static Route simplified(float[] x, float[] y, float[] z, double tolerance) {
        int n = x.length;
        if (n < 3) {
            return new Route(x, y, z);
        }
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        int[] stack = new int[n * 2 + 2];
        int top = 0;
        stack[top++] = 0;
        stack[top++] = n - 1;
        while (top > 0) {
            int hi = stack[--top];
            int lo = stack[--top];
            int far = -1;
            double farDistance = tolerance;
            for (int i = lo + 1; i < hi; i++) {
                double d = distanceToSegment(i, lo, hi, x, y, z);
                if (d > farDistance) {
                    farDistance = d;
                    far = i;
                }
            }
            if (far >= 0) {
                keep[far] = true;
                stack[top++] = lo;
                stack[top++] = far;
                stack[top++] = far;
                stack[top++] = hi;
            }
        }
        int count = 0;
        for (boolean k : keep) {
            if (k) {
                count++;
            }
        }
        float[] ox = new float[count];
        float[] oy = new float[count];
        float[] oz = new float[count];
        int o = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                ox[o] = x[i];
                oy[o] = y[i];
                oz[o] = z[i];
                o++;
            }
        }
        return new Route(ox, oy, oz);
    }

    private static double distanceToSegment(int p, int a, int b, float[] px, float[] py, float[] pz) {
        double abx = px[b] - px[a];
        double aby = py[b] - py[a];
        double abz = pz[b] - pz[a];
        double apx = px[p] - px[a];
        double apy = py[p] - py[a];
        double apz = pz[p] - pz[a];
        double len2 = abx * abx + aby * aby + abz * abz;
        double t = len2 == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, (apx * abx + apy * aby + apz * abz) / len2));
        double dx = apx - abx * t;
        double dy = apy - aby * t;
        double dz = apz - abz * t;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    float[] xs() {
        return x;
    }

    float[] ys() {
        return y;
    }

    float[] zs() {
        return z;
    }

    public int length() {
        return x.length;
    }

    public float x(int i) {
        return x[i];
    }

    public float y(int i) {
        return y[i];
    }

    public float z(int i) {
        return z[i];
    }

    /** Length of the path in blocks. */
    public double blocks() {
        double sum = 0.0;
        for (int i = 1; i < x.length; i++) {
            double dx = x[i] - x[i - 1];
            double dy = y[i] - y[i - 1];
            double dz = z[i] - z[i - 1];
            sum += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return sum;
    }
}
