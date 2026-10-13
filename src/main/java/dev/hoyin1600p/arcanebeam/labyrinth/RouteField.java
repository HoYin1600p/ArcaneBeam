package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/**
 * The way out from anywhere inside a labyrinth, built once per scan.
 * <p>
 * Paths run over a half-block lattice tied to the walkable cells: the centre of every cell, the middle of every seam
 * between two linked cells and the corner where four linked cells meet. Every lattice point knows its clearance, the
 * distance to the nearest wall face of its walkway (a cell side without a link: a wall, a drop or a step too high), so
 * linked cells count as one walkway whatever blocks fill it (stairs, slabs and full blocks side by side). A step costs
 * more the closer it is to a wall, so the cheapest way runs on the ridge of greatest clearance: the exact middle of
 * every hall (on the seam of an even-width hall, through the middle block of an odd one) and of every stairway.
 * <p>
 * One Dijkstra run from the exit mouth fills the field. Asking for a route follows the field from the player's cell
 * and pulls the lattice path straight: a straight piece replaces a stretch of the path only while it stays on linked
 * cells, keeps the clearance the path had (so it never cuts a corner) and keeps near the walking surface (so it rises
 * evenly over a stair run but never floats over a step).
 * <p>
 * Only cells inside the labyrinth take part: cells of the exit passage (the shaft or stair up to the surface) and cells
 * not connected to the mouth have no route, so nothing is drawn while the player is in the passage or outside.
 */
public final class RouteField {
    /** Clearance from which on a lattice point costs no more than open floor, in blocks. */
    static final double CLEAR_CAP = 3.0;
    /** How much more a step right at a wall costs than one in the open. */
    static final double WALL_WEIGHT = 6.0;
    /** Extra cost per block climbed or descended, so the way keeps to the floor rather than crossing furniture. */
    static final double CLIMB_WEIGHT = 5.0;
    /** A straight piece never has to keep more clearance than this, in blocks. */
    static final double REQUIRED_CLEAR_CAP = 2.0;
    /** Path clearance up to this is a hall (four wide or less), more is open floor. */
    static final double HALL_CLEAR = 2.0;
    /** Clearance a straight piece keeps across open floor, in blocks. */
    static final double OPEN_CLEAR = 1.0;
    /** Near either end a straight piece may be closer to a wall, by this much less per block towards the end. */
    static final double END_SLOPE = 0.35;
    /** Sideways jogs up to this size (in blocks) are straightened. */
    static final double JOG = 0.5;
    /** Longest stretch of route a straightened bulge may span, in blocks. */
    static final double JOG_LENGTH = 5.0;
    /** A straightened bulge still keeps this clearance (the middle of a one wide hall, less a little). */
    static final double JOG_FLOOR = 0.45;
    /** Sample spacing along a straight piece, in blocks. */
    static final double SAMPLE = 0.2;
    /** A straight piece may pass at most this far above or below the walking surface, in blocks... */
    static final double HEIGHT_TOLERANCE = 0.6;
    /** ...and must come at least this close to the surface of every cell it really crosses. */
    static final double HEIGHT_TOUCH = 0.3;
    /** Lattice points tried past the first one that cannot be reached straight. */
    static final int LOOKAHEAD = 8;
    /** Window (in cells either way) searched for the wall faces near a cell. */
    static final int FACE_RADIUS = 3;
    /** A kept route is followed while the player is within this distance of it, in blocks. */
    static final double FOLLOW_OFFSET = 0.6;
    /** Only this much of a kept route, from its start, is searched for the player, in blocks. */
    static final double FOLLOW_SEARCH = 8.0;

    private static final float INF = Float.POSITIVE_INFINITY;

    private final WalkGraph g;
    /** Cells that may hold a route: connected to the mouth without passing through the exit passage. */
    private final boolean[] inside;
    private final boolean[] mouth;
    /** Linked horizontal neighbour per cell and direction (+x, -x, +z, -z), or -1; inside cells only. */
    private final int[] nb;
    /** Wall faces near each cell, CSR: packed (x * 64 + z) * 2 + orientation. */
    private final int[] faceStart;
    private final int[] faces;

    // Lattice: node ids 0 .. g.count - 1 are the cell centres.
    private final int nodeCount;
    private final float[] px;
    private final float[] py;
    private final float[] pz;
    private final int[] owner;
    private final float[] clear;
    private final float[] dist;
    private final int[] next;

    private RouteField(WalkGraph g, boolean[] inside, boolean[] mouth, int[] nb, int[] faceStart, int[] faces, Lattice lattice,
            float[] dist, int[] next) {
        this.g = g;
        this.inside = inside;
        this.mouth = mouth;
        this.nb = nb;
        this.faceStart = faceStart;
        this.faces = faces;
        this.nodeCount = lattice.count;
        this.px = lattice.px.toArray();
        this.py = lattice.py.toArray();
        this.pz = lattice.pz.toArray();
        this.owner = lattice.owner.toArray();
        this.clear = lattice.clear;
        this.dist = dist;
        this.next = next;
    }

    /** Builds the field for routes that end at the {@code exits} cells (the mouth), with no exit passage. */
    public static RouteField build(WalkGraph g, int[] exits) {
        return build(g, exits, new boolean[g.count]);
    }

    /**
     * Builds the field for routes that end at the {@code exits} cells. Cells marked in {@code passage} (the exit shaft or
     * stair) are left out, together with everything only reachable through them.
     */
    public static RouteField build(WalkGraph g, int[] exits, boolean[] passage) {
        int n = g.count;
        boolean[] mouth = new boolean[n];
        boolean[] inside = new boolean[n];
        int[] queue = new int[n];
        int tail = 0;
        for (int e : exits) {
            if (e >= 0 && e < n && !passage[e] && !inside[e]) {
                mouth[e] = true;
                inside[e] = true;
                queue[tail++] = e;
            }
        }
        for (int head = 0; head < tail; head++) {
            int c = queue[head];
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (!inside[d] && !passage[d]) {
                    inside[d] = true;
                    queue[tail++] = d;
                }
            }
        }
        int[] nb = neighbours(g, inside);
        int[][] faceTable = faces(g, inside, nb);
        Lattice lattice = new Lattice(g, inside, nb, faceTable[0], faceTable[1]);
        float[] dist = new float[lattice.count];
        int[] next = new int[lattice.count];
        lattice.solve(mouth, dist, next);
        return new RouteField(g, inside, mouth, nb, faceTable[0], faceTable[1], lattice, dist, next);
    }

    // ---- queries ------------------------------------------------------------------------------------------------

    /** True when a route from this cell to an exit exists (the cell is inside and connected to the mouth). */
    public boolean reachable(int cell) {
        return cell >= 0 && cell < g.count && inside[cell] && dist[cell] != INF;
    }

    /** True for the cells of the exit mouth: there the player is at the exit and no route is drawn. */
    public boolean atMouth(int cell) {
        return cell >= 0 && cell < g.count && mouth[cell];
    }

    /** True when the cell takes part in routing (not part of the exit passage, not cut off from the mouth). */
    public boolean inside(int cell) {
        return cell >= 0 && cell < g.count && inside[cell];
    }

    /** The weighted distance from the cell to the exit (one per block in the open, more near walls); infinity if none. */
    public double distance(int cell) {
        return reachable(cell) ? dist[cell] : Double.POSITIVE_INFINITY;
    }

    /**
     * The clearance at a box-local point of the walkway of {@code cell} (the point should lie in or next to the cell's
     * column): the distance to the nearest wall face, capped at {@link #CLEAR_CAP}. Zero for cells that are not inside.
     */
    public double clearanceAt(int cell, double x, double z) {
        return inside(cell) ? clearanceAt(faceStart, faces, cell, x, z) : 0.0;
    }

    /**
     * The route from the cell to the exit mouth, or null when the cell has none (unreachable, in the exit passage, or
     * at the mouth already). Vertices are box-local, y being the walking surface; the first is the cell's centre.
     */
    public Route route(int cell) {
        if (!reachable(cell) || mouth[cell]) {
            return null;
        }
        IntList path = new IntList();
        int node = cell;
        int guard = 0;
        while (node >= 0) {
            path.add(node);
            if (dist[node] == 0.0F || ++guard > nodeCount) {
                break;
            }
            node = next[node];
        }
        if (path.size() < 2) {
            return null;
        }
        int[] nodes = path.toArray();
        int n = nodes.length;
        // nextClimb[m]: the first m' >= m whose step from m' - 1 is a climb (n when none). A straight piece never spans one.
        int[] nextClimb = new int[n + 1];
        nextClimb[n] = n;
        for (int m = n - 1; m >= 1; m--) {
            nextClimb[m] = isClimb(nodes[m - 1], nodes[m]) ? m : nextClimb[m + 1];
        }
        // Indices into the lattice path of the points kept.
        IntList kept = new IntList();
        kept.add(0);
        int i = 0;
        while (i < n - 1) {
            int limit = nextClimb[i + 1] == i + 1 ? i + 1 : Math.min(n, nextClimb[i + 1]) - 1;
            i = farthest(nodes, i, limit, 0.0);
            kept.add(i);
        }
        int[] at = smoothJogs(nodes, kept.toArray(), nextClimb);
        int count = at.length;
        float[] x = new float[count];
        float[] y = new float[count];
        float[] z = new float[count];
        for (int k = 0; k < count; k++) {
            int v = nodes[at[k]];
            x[k] = px[v];
            y[k] = py[v];
            z[k] = pz[v];
        }
        return Route.simplified(x, y, z, 0.02);
    }

    /**
     * Straightens small bulges: where a hall's walls step out for a few blocks (a niche, a doorway) the middle line moves
     * sideways and back, and following it exactly would make the route jog. One or two points between two points that lie
     * on one straight line with the pieces before and after them are dropped when they are within {@link #JOG} of that
     * line, the bulge is at most {@link #JOG_LENGTH} long and the straight piece passes the usual checks with its clearance
     * relaxed by the size of the jog (but never below the middle of a one wide hall). A uniform hall has no bulges, and a
     * lasting shift of the middle line is not a bulge, so the route stays on the middle line there.
     */
    private int[] smoothJogs(int[] nodes, int[] at, int[] nextClimb) {
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>(at.length);
        for (int v : at) {
            out.add(v);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int v = 1; v + 3 < out.size() + 1 && !changed; v++) {
                for (int w = Math.min(v + 3, out.size() - 2); w >= v + 2; w--) {
                    int i = out.get(v);
                    int j = out.get(w);
                    if (nextClimb[i + 1] <= j || Math.hypot(px[nodes[j]] - px[nodes[i]], pz[nodes[j]] - pz[nodes[i]]) > JOG_LENGTH
                            || !inLine(nodes[out.get(v - 1)], nodes[i], nodes[j]) || !inLine(nodes[i], nodes[j], nodes[out.get(w + 1)])) {
                        continue;
                    }
                    double jog = 0.0;
                    for (int k = v + 1; k < w; k++) {
                        jog = Math.max(jog, offFrom(nodes[out.get(k)], nodes[i], nodes[j]));
                    }
                    if (jog <= JOG && straight(nodes, i, j, jog)) {
                        for (int k = w - 1; k > v; k--) {
                            out.remove((int) k);
                        }
                        changed = true;
                        break;
                    }
                }
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    /** True when the pieces a-b and b-c run in the same horizontal direction (within 5 degrees). */
    private boolean inLine(int a, int b, int c) {
        double ax = px[b] - px[a];
        double az = pz[b] - pz[a];
        double bx = px[c] - px[b];
        double bz = pz[c] - pz[b];
        double la = Math.hypot(ax, az);
        double lb = Math.hypot(bx, bz);
        if (la < 1.0E-6 || lb < 1.0E-6) {
            return false;
        }
        return (ax * bx + az * bz) / (la * lb) >= Math.cos(Math.toRadians(5.0));
    }

    /** Horizontal distance of lattice point p from the segment a-b. */
    private double offFrom(int p, int a, int b) {
        double abx = px[b] - px[a];
        double abz = pz[b] - pz[a];
        double len2 = abx * abx + abz * abz;
        double t = len2 < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, ((px[p] - px[a]) * abx + (pz[p] - pz[a]) * abz) / len2));
        return Math.hypot(px[p] - (px[a] + abx * t), pz[p] - (pz[a] + abz * t));
    }

    /**
     * The farthest lattice point after {@code i} (up to {@code limit}) that a straight piece from {@code i} reaches: grows
     * the reach by doubling, narrows it by bisection, then tries a few points past it, since a point that cannot be
     * reached straight may be followed by one that can.
     */
    private int farthest(int[] nodes, int i, int limit, double slack) {
        if (limit <= i + 1) {
            return i + 1;
        }
        int good = i + 1;
        int bad = -1;
        int gap = 1;
        while (true) {
            int j = Math.min(limit, good + gap);
            if (straight(nodes, i, j, slack)) {
                good = j;
                if (j == limit) {
                    break;
                }
                gap *= 2;
            } else {
                bad = j;
                break;
            }
        }
        if (bad > 0) {
            while (bad - good > 1) {
                int mid = (good + bad) >>> 1;
                if (straight(nodes, i, mid, slack)) {
                    good = mid;
                } else {
                    bad = mid;
                }
            }
            for (int j = bad + 1; j <= Math.min(limit, bad + LOOKAHEAD); j++) {
                if (straight(nodes, i, j, slack)) {
                    good = j;
                }
            }
        }
        return good;
    }

    /** A climb: a step between two points stacked in one column (a ladder). Never pulled straight. */
    private boolean isClimb(int a, int b) {
        return px[a] == px[b] && pz[a] == pz[b] && py[a] != py[b];
    }

    /**
     * True when the straight piece from lattice point {@code nodes[i]} to {@code nodes[j]} may replace the lattice path
     * between them: every sample lies on a cell linked to the previous one, keeps the clearance the path kept (relaxed
     * linearly towards the two ends, which may lie close to a wall) and stays near the walking surface.
     */
    private boolean straight(int[] nodes, int i, int j, double slack) {
        int a = nodes[i];
        int b = nodes[j];
        if (isClimb(nodes[i], nodes[i + 1])) {
            return false;
        }
        // Path length at each lattice point between i and j, to match points of the piece with the stretch of path they
        // replace.
        double[] at = PATH_AT.get();
        if (at.length < j - i + 1) {
            at = new double[Math.max(at.length * 2, j - i + 1)];
            PATH_AT.set(at);
        }
        at[0] = 0.0;
        for (int k = i + 1; k <= j; k++) {
            at[k - i] = at[k - i - 1] + Math.hypot(px[nodes[k]] - px[nodes[k - 1]], pz[nodes[k]] - pz[nodes[k - 1]]);
        }
        double pathLength = at[j - i];
        double ax = px[a];
        double ay = py[a];
        double az = pz[a];
        double bx = px[b];
        double by = py[b];
        double bz = pz[b];
        double len = Math.hypot(bx - ax, bz - az);
        if (len < 1.0E-6) {
            return false;
        }
        double dx = bx - ax;
        double dz = bz - az;
        // A piece running exactly along a seam lies on two columns at once: either may carry it.
        boolean seamX = dx == 0.0 && ax == Math.floor(ax);
        boolean seamZ = dz == 0.0 && az == Math.floor(az);
        int cur = owner[a];
        int cx = g.x(cur);
        int cz = g.z(cur);
        int startX = (int) Math.floor(ax + Math.signum(dx) * 1.0E-6);
        int startZ = (int) Math.floor(az + Math.signum(dz) * 1.0E-6);
        if (startX != cx || startZ != cz) {
            cur = stepTo(cur, cx, cz, startX, startZ, ay);
            if (cur < 0) {
                return false;
            }
            cx = startX;
            cz = startZ;
        }
        // Every column the piece crosses, in order (exact grid traversal), with the parameter where it leaves it.
        int stepX = dx > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0.0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double tDeltaZ = dz == 0.0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);
        double tMaxX = dx == 0.0 ? Double.POSITIVE_INFINITY : ((dx > 0 ? cx + 1 : cx) - ax) / dx;
        double tMaxZ = dz == 0.0 ? Double.POSITIVE_INFINITY : ((dz > 0 ? cz + 1 : cz) - az) / dz;
        IntList cells = TRAVERSED.get();
        cells.clear();
        double[] exits = EXITS.get();
        double tEnter = 0.0;
        while (true) {
            double tExit = Math.min(1.0, Math.min(tMaxX, tMaxZ));
            if (!heightFits(cur, seamX, seamZ, ay + (by - ay) * tEnter, ay + (by - ay) * tExit, (tExit - tEnter) * len)) {
                return false;
            }
            if (cells.size() == exits.length) {
                exits = Arrays.copyOf(exits, exits.length * 2);
                EXITS.set(exits);
            }
            exits[cells.size()] = tExit;
            cells.add(cur);
            if (tExit >= 1.0 - 1.0E-9) {
                break;
            }
            int tx = cx;
            int tz = cz;
            if (Math.abs(tMaxX - tMaxZ) < 1.0E-9) {
                tx += stepX;
                tz += stepZ;
                tMaxX += tDeltaX;
                tMaxZ += tDeltaZ;
            } else if (tMaxX < tMaxZ) {
                tx += stepX;
                tMaxX += tDeltaX;
            } else {
                tz += stepZ;
                tMaxZ += tDeltaZ;
            }
            cur = stepTo(cur, cx, cz, tx, tz, ay + (by - ay) * tExit);
            if (cur < 0) {
                return false;
            }
            cx = tx;
            cz = tz;
            tEnter = tExit;
        }
        // Clearance, sampled along the piece: each point must keep the clearance the path kept on the stretch it replaces
        // (within a block either way of the matching path length), relaxed linearly towards the two ends.
        int steps = Math.max(2, (int) Math.ceil(len / SAMPLE));
        int k = 0;
        int lo = 0;
        for (int s = 1; s < steps; s++) {
            double t = s / (double) steps;
            while (k < cells.size() - 1 && exits[k] < t) {
                k++;
            }
            double pos = t * pathLength;
            while (lo < j - i && at[lo] < pos - 1.0) {
                lo++;
            }
            double kept = REQUIRED_CLEAR_CAP;
            for (int q = lo; q <= j - i && at[q] <= pos + 1.0; q++) {
                kept = Math.min(kept, keptClearance(clear[nodes[i + q]]));
            }
            double along = t * len;
            double ends = Math.min(clear[a] + along * END_SLOPE, clear[b] + (len - along) * END_SLOPE);
            double required = slack > 0.0 ? Math.max(JOG_FLOOR, Math.min(kept, ends) - 0.01 - slack) : Math.min(kept, ends) - 0.01;
            if (clearanceAt(faceStart, faces, cells.get(k), ax + dx * t, az + dz * t) < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * The clearance a straight piece must keep where the path had {@code clearance}: all of it in a hall (up to four wide,
     * so the piece stays on the middle line and off the inner side of corners), a plain block in the open, where any
     * straight way across that keeps off the walls is fine.
     */
    static double keptClearance(double clearance) {
        return clearance <= HALL_CLEAR ? clearance : OPEN_CLEAR;
    }

    private static final ThreadLocal<double[]> PATH_AT = ThreadLocal.withInitial(() -> new double[256]);
    private static final ThreadLocal<IntList> TRAVERSED = ThreadLocal.withInitial(IntList::new);
    private static final ThreadLocal<double[]> EXITS = ThreadLocal.withInitial(() -> new double[64]);

    /**
     * True when a piece crossing the column of {@code cell} from height {@code yIn} to {@code yOut} over {@code length}
     * blocks keeps near its walking surface: never more than {@link #HEIGHT_TOLERANCE} away, and on a real crossing
     * (0.3 blocks or more) coming within {@link #HEIGHT_TOUCH} of it. A piece on a seam is measured against the closer
     * of the two columns.
     */
    private boolean heightFits(int cell, boolean seamX, boolean seamZ, double yIn, double yOut, double length) {
        if (length < 0.02) {
            return true;
        }
        boolean fits = fits(cell, yIn, yOut, length);
        if (!fits && seamX && nb[cell * 4 + 1] >= 0) {
            fits = fits(nb[cell * 4 + 1], yIn, yOut, length);
        }
        if (!fits && seamZ && nb[cell * 4 + 3] >= 0) {
            fits = fits(nb[cell * 4 + 3], yIn, yOut, length);
        }
        return fits;
    }

    private boolean fits(int cell, double yIn, double yOut, double length) {
        double s = g.surface(cell);
        double a = yIn - s;
        double b = yOut - s;
        if (Math.max(Math.abs(a), Math.abs(b)) > HEIGHT_TOLERANCE) {
            return false;
        }
        double closest = a * b <= 0.0 ? 0.0 : Math.min(Math.abs(a), Math.abs(b));
        return length < 0.3 || closest <= HEIGHT_TOUCH;
    }

    /** Moves from cell {@code cur} in column (cx, cz) to the linked cell in column (tx, tz), or -1. */
    private int stepTo(int cur, int cx, int cz, int tx, int tz, double y) {
        int ddx = tx - cx;
        int ddz = tz - cz;
        if (Math.abs(ddx) > 1 || Math.abs(ddz) > 1) {
            return -1;
        }
        int dirX = ddx > 0 ? 0 : 1;
        int dirZ = ddz > 0 ? 2 : 3;
        if (ddz == 0) {
            return nb[cur * 4 + dirX];
        }
        if (ddx == 0) {
            return nb[cur * 4 + dirZ];
        }
        // Diagonal (the piece passes a corner): through either side column.
        int viaX = nb[cur * 4 + dirX];
        int first = viaX >= 0 ? nb[viaX * 4 + dirZ] : -1;
        int viaZ = nb[cur * 4 + dirZ];
        int second = viaZ >= 0 ? nb[viaZ * 4 + dirX] : -1;
        if (first < 0) {
            return second;
        }
        if (second < 0 || first == second) {
            return first;
        }
        return Math.abs(g.surface(first) - y) <= Math.abs(g.surface(second) - y) ? first : second;
    }

    /**
     * The rest of {@code route} from the point nearest to the box-local position (x, y, z), when that point lies on the
     * first stretch of the route within {@link #FOLLOW_OFFSET} horizontally and one block vertically; null otherwise.
     * Keeps the drawn route steady while the player walks along it.
     */
    public static Route follow(Route route, double x, double y, double z) {
        if (route == null || route.length() < 2) {
            return null;
        }
        int bestSeg = -1;
        double bestT = 0.0;
        double bestD = FOLLOW_OFFSET;
        double walked = 0.0;
        for (int i = 0; i + 1 < route.length() && walked <= FOLLOW_SEARCH; i++) {
            double ax = route.x(i);
            double az = route.z(i);
            double dx = route.x(i + 1) - ax;
            double dz = route.z(i + 1) - az;
            double len2 = dx * dx + dz * dz;
            double t = len2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((x - ax) * dx + (z - az) * dz) / len2));
            double qx = ax + dx * t;
            double qz = az + dz * t;
            double qy = route.y(i) + (route.y(i + 1) - route.y(i)) * t;
            double d = Math.hypot(x - qx, z - qz);
            if (d <= bestD && Math.abs(qy - y) <= 1.0) {
                bestD = d;
                bestSeg = i;
                bestT = t;
            }
            walked += Math.sqrt(len2);
        }
        if (bestSeg < 0) {
            return null;
        }
        boolean atVertex = bestT > 0.999;
        int first = atVertex ? bestSeg + 1 : bestSeg;
        int rest = route.length() - first - 1;
        if (rest < 1) {
            return null;
        }
        float[] ox = new float[rest + 1];
        float[] oy = new float[rest + 1];
        float[] oz = new float[rest + 1];
        double t = atVertex ? 0.0 : bestT;
        ox[0] = (float) (route.x(first) + (route.x(first + 1) - route.x(first)) * t);
        oy[0] = (float) (route.y(first) + (route.y(first + 1) - route.y(first)) * t);
        oz[0] = (float) (route.z(first) + (route.z(first + 1) - route.z(first)) * t);
        for (int k = 1; k <= rest; k++) {
            ox[k] = route.x(first + k);
            oy[k] = route.y(first + k);
            oz[k] = route.z(first + k);
        }
        Route trimmed = new Route(ox, oy, oz);
        return trimmed.blocks() < 0.75 ? null : trimmed;
    }

    // ---- construction -----------------------------------------------------------------------------------------------

    /** The linked horizontal neighbour of every inside cell in each direction (closest surface when several), or -1. */
    private static int[] neighbours(WalkGraph g, boolean[] inside) {
        int[] nb = new int[g.count * 4];
        Arrays.fill(nb, -1);
        for (int c = 0; c < g.count; c++) {
            if (!inside[c]) {
                continue;
            }
            for (int i = 0; i < g.degree(c); i++) {
                int d = g.neighbour(c, i);
                if (!inside[d]) {
                    continue;
                }
                int ddx = g.x(d) - g.x(c);
                int ddz = g.z(d) - g.z(c);
                int dir = ddx == 1 ? 0 : ddx == -1 ? 1 : ddz == 1 ? 2 : ddz == -1 ? 3 : -1;
                if (dir < 0) {
                    continue;
                }
                int old = nb[c * 4 + dir];
                if (old < 0 || Math.abs(g.surface16(d) - g.surface16(c)) < Math.abs(g.surface16(old) - g.surface16(c))) {
                    nb[c * 4 + dir] = d;
                }
            }
        }
        return nb;
    }

    /** For every inside cell, the wall faces of the cells within {@link #FACE_RADIUS} that its walkway reaches. */
    private static int[][] faces(WalkGraph g, boolean[] inside, int[] nb) {
        int n = g.count;
        int[] start = new int[n + 1];
        IntList out = new IntList(n * 16 + 16);
        int[] seen = new int[n];
        Arrays.fill(seen, -1);
        int[] queue = new int[n];
        for (int c = 0; c < n; c++) {
            start[c] = out.size();
            if (!inside[c]) {
                continue;
            }
            int ox = g.x(c);
            int oz = g.z(c);
            int head = 0;
            int tail = 0;
            queue[tail++] = c;
            seen[c] = c;
            while (head < tail) {
                int v = queue[head++];
                int vx = g.x(v);
                int vz = g.z(v);
                for (int dir = 0; dir < 4; dir++) {
                    int w = nb[v * 4 + dir];
                    if (w < 0) {
                        out.add(face(vx, vz, dir));
                    } else if (seen[w] != c && Math.abs(g.x(w) - ox) <= FACE_RADIUS && Math.abs(g.z(w) - oz) <= FACE_RADIUS) {
                        seen[w] = c;
                        queue[tail++] = w;
                    }
                }
            }
        }
        start[n] = out.size();
        return new int[][]{start, out.toArray()};
    }

    /** The closed side {@code dir} of the cell in column (x, z) as a unit segment on the block grid. */
    private static int face(int x, int z, int dir) {
        return switch (dir) {
            case 0 -> ((x + 1) * 64 + z) * 2 + 1;
            case 1 -> (x * 64 + z) * 2 + 1;
            case 2 -> (x * 64 + z + 1) * 2;
            default -> (x * 64 + z) * 2;
        };
    }

    private static double clearanceAt(int[] faceStart, int[] faces, int cell, double x, double z) {
        double best = CLEAR_CAP;
        for (int k = faceStart[cell]; k < faceStart[cell + 1]; k++) {
            int f = faces[k];
            int o = f & 1;
            int fz = (f >> 1) & 63;
            int fx = (f >> 1) >> 6;
            double d;
            if (o == 0) {
                double cx = Math.max(fx, Math.min(fx + 1.0, x));
                d = Math.hypot(x - cx, z - fz);
            } else {
                double cz = Math.max(fz, Math.min(fz + 1.0, z));
                d = Math.hypot(x - fx, z - cz);
            }
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    /** Lattice points and their links; solved with one Dijkstra run from the mouth. */
    private static final class Lattice {
        final WalkGraph g;
        final FloatList px = new FloatList();
        final FloatList py = new FloatList();
        final FloatList pz = new FloatList();
        final IntList owner = new IntList();
        /** The cells around each node (1 for a centre, 2 for a seam, 4 for a corner), CSR. */
        final IntList cellStart = new IntList();
        final IntList cells = new IntList();
        IntList edgeA = new IntList();
        IntList edgeB = new IntList();
        final float[] clear;
        final int count;

        Lattice(WalkGraph g, boolean[] inside, int[] nb, int[] faceStart, int[] faces) {
            this.g = g;
            int n = g.count;
            // Seam node on the +x and +z side of each cell.
            int[] seamX = new int[n];
            int[] seamZ = new int[n];
            Arrays.fill(seamX, -1);
            Arrays.fill(seamZ, -1);
            for (int c = 0; c < n; c++) {
                add(g.x(c) + 0.5F, (float) g.surface(c), g.z(c) + 0.5F, c);
            }
            for (int c = 0; c < n; c++) {
                if (!inside[c]) {
                    continue;
                }
                int r = nb[c * 4];
                if (r >= 0) {
                    seamX[c] = add(g.x(c) + 1.0F, (float) ((g.surface(c) + g.surface(r)) * 0.5), g.z(c) + 0.5F, c, r);
                    link(c, seamX[c]);
                    link(r, seamX[c]);
                }
                int u = nb[c * 4 + 2];
                if (u >= 0) {
                    seamZ[c] = add(g.x(c) + 0.5F, (float) ((g.surface(c) + g.surface(u)) * 0.5), g.z(c) + 1.0F, c, u);
                    link(c, seamZ[c]);
                    link(u, seamZ[c]);
                }
            }
            // Corner nodes where four cells are linked all round.
            for (int c = 0; c < n; c++) {
                int r = inside[c] ? nb[c * 4] : -1;
                int u = inside[c] ? nb[c * 4 + 2] : -1;
                if (r < 0 || u < 0) {
                    continue;
                }
                int q = nb[r * 4 + 2];
                if (q < 0 || nb[u * 4] != q || seamZ[r] < 0 || seamX[u] < 0) {
                    continue;
                }
                float h = (float) ((g.surface(c) + g.surface(r) + g.surface(u) + g.surface(q)) * 0.25);
                int k = add(g.x(c) + 1.0F, h, g.z(c) + 1.0F, c, r, u, q);
                link(k, seamX[c]);
                link(k, seamZ[c]);
                link(k, seamZ[r]);
                link(k, seamX[u]);
                link(k, c);
                link(k, r);
                link(k, u);
                link(k, q);
            }
            // Diagonals between the seams of one cell (they cross a quarter of that cell only).
            for (int c = 0; c < n; c++) {
                if (!inside[c]) {
                    continue;
                }
                int east = seamX[c];
                int west = nb[c * 4 + 1] >= 0 ? seamX[nb[c * 4 + 1]] : -1;
                int south = seamZ[c];
                int north = nb[c * 4 + 3] >= 0 ? seamZ[nb[c * 4 + 3]] : -1;
                pair(east, south);
                pair(south, west);
                pair(west, north);
                pair(north, east);
            }
            // Climbs: two cells stacked in one column.
            for (int c = 0; c < n; c++) {
                if (!inside[c]) {
                    continue;
                }
                for (int i = 0; i < g.degree(c); i++) {
                    int d = g.neighbour(c, i);
                    if (d > c && inside[d] && g.x(d) == g.x(c) && g.z(d) == g.z(c)) {
                        link(c, d);
                    }
                }
            }
            cellStart.add(cells.size());
            count = px.size();
            clear = new float[count];
            for (int v = 0; v < count; v++) {
                int cell = owner.get(v);
                clear[v] = inside[cell] ? (float) clearanceAt(faceStart, faces, cell, px.get(v), pz.get(v)) : 0.0F;
            }
            // A diagonal link that passes closer to a wall than both its ends (round the inside of a corner) is dropped:
            // the way goes through the cell centre there instead of grazing the corner.
            IntList keepA = new IntList(edgeA.size());
            IntList keepB = new IntList(edgeB.size());
            for (int e = 0; e < edgeA.size(); e++) {
                int a = edgeA.get(e);
                int b = edgeB.get(e);
                float mx = (px.get(a) + px.get(b)) * 0.5F;
                float mz = (pz.get(a) + pz.get(b)) * 0.5F;
                boolean diagonal = px.get(a) != px.get(b) && pz.get(a) != pz.get(b);
                if (diagonal) {
                    int cell = cellAround(a, b, mx, mz);
                    double mid = clearanceAt(faceStart, faces, cell, mx, mz);
                    if (mid < Math.min(clear[a], clear[b]) - 0.05) {
                        continue;
                    }
                }
                keepA.add(a);
                keepB.add(b);
            }
            edgeA = keepA;
            edgeB = keepB;
        }

        /** A cell around node a or b whose column holds the point. */
        private int cellAround(int a, int b, float x, float z) {
            int cx = (int) Math.floor(x);
            int cz = (int) Math.floor(z);
            for (int v : new int[]{a, b}) {
                for (int k = cellStart.get(v); k < cellStart.get(v + 1); k++) {
                    int c = cells.get(k);
                    if (g.x(c) == cx && g.z(c) == cz) {
                        return c;
                    }
                }
            }
            return owner.get(a);
        }

        private int add(float x, float y, float z, int... around) {
            px.add(x);
            py.add(y);
            pz.add(z);
            owner.add(around[0]);
            cellStart.add(cells.size());
            for (int cell : around) {
                cells.add(cell);
            }
            return px.size() - 1;
        }

        private void link(int a, int b) {
            edgeA.add(a);
            edgeB.add(b);
        }

        private void pair(int a, int b) {
            if (a >= 0 && b >= 0) {
                link(a, b);
            }
        }

        /** Dijkstra from every node made only of mouth cells; fills the distance and the next node towards the exit. */
        void solve(boolean[] mouth, float[] dist, int[] next) {
            int m = edgeA.size();
            int[] adjStart = new int[count + 1];
            for (int e = 0; e < m; e++) {
                adjStart[edgeA.get(e) + 1]++;
                adjStart[edgeB.get(e) + 1]++;
            }
            for (int v = 0; v < count; v++) {
                adjStart[v + 1] += adjStart[v];
            }
            int[] fill = Arrays.copyOf(adjStart, count);
            int[] adj = new int[2 * m];
            float[] cost = new float[2 * m];
            float[] weight = new float[count];
            for (int v = 0; v < count; v++) {
                double c = Math.min(clear[v], CLEAR_CAP) / CLEAR_CAP;
                weight[v] = (float) (1.0 + WALL_WEIGHT * (1.0 - c) * (1.0 - c));
            }
            for (int e = 0; e < m; e++) {
                int a = edgeA.get(e);
                int b = edgeB.get(e);
                double dx = px.get(a) - px.get(b);
                double dy = py.get(a) - py.get(b);
                double dz = pz.get(a) - pz.get(b);
                float c = (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.5 * (weight[a] + weight[b]) + CLIMB_WEIGHT * Math.abs(dy));
                adj[fill[a]] = b;
                cost[fill[a]++] = c;
                adj[fill[b]] = a;
                cost[fill[b]++] = c;
            }
            Arrays.fill(dist, INF);
            Arrays.fill(next, -1);
            LongHeap heap = new LongHeap(Math.max(16, count));
            for (int v = 0; v < count; v++) {
                if (allMouth(v, mouth)) {
                    dist[v] = 0.0F;
                    heap.push(pack(0.0F, v));
                }
            }
            while (!heap.isEmpty()) {
                long top = heap.pop();
                int v = (int) (top & 0xFFFFFFFFL);
                float d = Float.intBitsToFloat((int) (top >>> 32));
                if (d > dist[v]) {
                    continue;
                }
                for (int k = adjStart[v]; k < adjStart[v + 1]; k++) {
                    int w = adj[k];
                    float nd = d + cost[k];
                    if (nd < dist[w]) {
                        dist[w] = nd;
                        next[w] = v;
                        heap.push(pack(nd, w));
                    }
                }
            }
        }

        /** True when every cell around the node is a mouth cell. */
        private boolean allMouth(int v, boolean[] mouth) {
            for (int k = cellStart.get(v); k < cellStart.get(v + 1); k++) {
                if (!mouth[cells.get(k)]) {
                    return false;
                }
            }
            return true;
        }

        private static long pack(float dist, int node) {
            return ((long) Float.floatToIntBits(dist) << 32) | (node & 0xFFFFFFFFL);
        }
    }

    /** A growable float array. */
    private static final class FloatList {
        private float[] data = new float[256];
        private int size;

        void add(float value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            data[size++] = value;
        }

        float get(int i) {
            return data[i];
        }

        int size() {
            return size;
        }

        float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    /** A binary min-heap of longs. */
    private static final class LongHeap {
        private long[] data;
        private int size;

        LongHeap(int capacity) {
            data = new long[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push(long value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            int i = size++;
            while (i > 0) {
                int parent = (i - 1) >> 1;
                if (data[parent] <= value) {
                    break;
                }
                data[i] = data[parent];
                i = parent;
            }
            data[i] = value;
        }

        long pop() {
            long result = data[0];
            long last = data[--size];
            if (size > 0) {
                int i = 0;
                while (true) {
                    int child = 2 * i + 1;
                    if (child >= size) {
                        break;
                    }
                    if (child + 1 < size && data[child + 1] < data[child]) {
                        child++;
                    }
                    if (data[child] >= last) {
                        break;
                    }
                    data[i] = data[child];
                    i = child;
                }
                data[i] = last;
            }
            return result;
        }
    }
}
