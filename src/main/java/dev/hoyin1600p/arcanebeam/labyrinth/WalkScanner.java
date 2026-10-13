package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.Arrays;

/**
 * Turns the blocks of a box into a {@link WalkGraph}.
 * <p>
 * Rules (heights in sixteenths of a block):
 * <ul>
 *   <li>A cell exists where a block's collision top is at most one block high (full blocks, slabs, stairs, carpet,
 *       chests) and the player-sized space above that surface (1.8 blocks, 29 sixteenths) holds no collision, or
 *       where a climbable block (ladder, vine) has that space around it, with or without a floor under it.</li>
 *   <li>Doors, trapdoors, cobwebs, water and plants have no collision and never stop the scan; walls, bedrock,
 *       fences, glass and everything with a collision shape do. Blocks of unloaded chunks count as walls.</li>
 *   <li>Two cells in neighbouring columns (4-neighbourhood) are linked when their surfaces differ by at most one
 *       block. A one block step between two full blocks needs a jump, so both columns must be clear of collision
 *       for the player's height above the higher surface; steps that involve a stair, slab or ladder are walked in
 *       half steps and only need both cells to exist.</li>
 *   <li>Two cells stacked in one column are linked when both are on a climbable block and one block apart.</li>
 * </ul>
 */
public final class WalkScanner {
    /** Player height, 1.8 blocks, rounded up to the next sixteenth. */
    static final int BODY16 = 29;
    /** The highest step a player can make, one block. */
    static final int STEP16 = 16;
    /** From this height difference on, a step between two full blocks is a jump. */
    static final int JUMP16 = 8;
    /** The highest collision top a block can have and still be stood on, one block. */
    static final int MAX_SUPPORT16 = 16;

    private static final int FLAG_CLIMB = 1;
    /** The cell is walked onto in half steps (slab, stair, ladder), so a step to or from it needs no jump. */
    private static final int FLAG_TREAD = 2;
    /** The cell stands on a stair. */
    private static final int FLAG_STAIR = 4;

    private WalkScanner() {
    }

    /**
     * Scans a box {@code size} wide in x and z and {@code height} blocks tall; cells whose feet are above
     * {@code maxFeetY} (the roof over the labyrinth) are dropped. {@code sampler} is asked for every block exactly
     * once.
     */
    public static WalkGraph scan(VoxelSampler sampler, int size, int height, int maxFeetY) {
        int[] codes = new int[size * size * height];
        int unknown = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int code = sampler.sample(x, y, z);
                    codes[(y * size + z) * size + x] = code;
                    if (BlockCode.unknownBlock(code)) {
                        unknown++;
                    }
                }
            }
        }
        return build(codes, size, height, unknown, maxFeetY);
    }

    /** Builds the graph from an already sampled block array indexed {@code (y * size + z) * size + x}. */
    public static WalkGraph build(int[] codes, int size, int height, int unknownBlocks, int maxFeetY) {
        Grid grid = new Grid(codes, size, height);
        int[] index = new int[size * size * height];
        Arrays.fill(index, -1);
        IntList cells = new IntList(4096);
        IntList surfaces = new IntList(4096);
        IntList flagList = new IntList(4096);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int code = codes[(y * size + z) * size + x];
                    int above = y + 1 < height ? codes[((y + 1) * size + z) * size + x] : BlockCode.AIR;
                    int surface = -1;
                    int flags = BlockCode.climbable(above) ? FLAG_CLIMB : 0;
                    if (BlockCode.collides(code) && !BlockCode.unknownBlock(code) && !BlockCode.hazardous(code)
                            && BlockCode.hi(code) > 0 && BlockCode.hi(code) <= MAX_SUPPORT16) {
                        int candidate = y * 16 + BlockCode.hi(code);
                        if (grid.free(x, z, candidate, BODY16) && !grid.hazard(x, z, candidate, BODY16)) {
                            surface = candidate;
                            if (BlockCode.tread(code)) {
                                flags |= FLAG_TREAD;
                            }
                            if (BlockCode.stair(code)) {
                                flags |= FLAG_STAIR;
                            }
                        }
                    }
                    if (surface < 0 && (flags & FLAG_CLIMB) != 0 && !BlockCode.collides(above)) {
                        int candidate = (y + 1) * 16;
                        if (grid.free(x, z, candidate, BODY16) && !grid.hazard(x, z, candidate, BODY16)) {
                            surface = candidate;
                            flags |= FLAG_TREAD;
                        }
                    }
                    if (surface < 0 || (surface >> 4) > maxFeetY) {
                        continue;
                    }
                    index[(y * size + z) * size + x] = cells.size();
                    cells.add((y * size + z) * size + x);
                    surfaces.add(surface);
                    flagList.add(flags);
                }
            }
        }
        int count = cells.size();
        short[] cx = new short[count];
        short[] cy = new short[count];
        short[] cz = new short[count];
        short[] cs = new short[count];
        boolean[] climb = new boolean[count];
        boolean[] tread = new boolean[count];
        boolean[] stair = new boolean[count];
        for (int c = 0; c < count; c++) {
            int volume = cells.get(c);
            cx[c] = (short) (volume % size);
            cz[c] = (short) ((volume / size) % size);
            cy[c] = (short) (volume / (size * size));
            cs[c] = (short) surfaces.get(c);
            climb[c] = (flagList.get(c) & FLAG_CLIMB) != 0;
            tread[c] = (flagList.get(c) & FLAG_TREAD) != 0;
            stair[c] = (flagList.get(c) & FLAG_STAIR) != 0;
        }

        // Links are symmetric, so each pair is found once from one side and stored both ways.
        IntList from = new IntList(count * 3);
        IntList to = new IntList(count * 3);
        int[] degree = new int[count];
        for (int c = 0; c < count; c++) {
            link(grid, index, size, height, cx, cy, cz, cs, tread, stair, climb, c, 1, 0, from, to, degree);
            link(grid, index, size, height, cx, cy, cz, cs, tread, stair, climb, c, 0, 1, from, to, degree);
            if (climb[c] && cy[c] + 1 < height) {
                int above = index[((cy[c] + 1) * size + cz[c]) * size + cx[c]];
                if (above >= 0 && climb[above] && cs[above] - cs[c] == 16) {
                    add(from, to, degree, c, above);
                }
            }
        }
        int[] adjStart = new int[count + 1];
        for (int c = 0; c < count; c++) {
            adjStart[c + 1] = adjStart[c] + degree[c];
        }
        int[] fill = new int[count];
        int[] adj = new int[adjStart[count]];
        for (int e = 0; e < from.size(); e++) {
            int a = from.get(e);
            int b = to.get(e);
            adj[adjStart[a] + fill[a]++] = b;
            adj[adjStart[b] + fill[b]++] = a;
        }
        return new WalkGraph(size, height, count, cx, cy, cz, cs, climb, adjStart, adj, index, unknownBlocks);
    }

    private static void add(IntList from, IntList to, int[] degree, int a, int b) {
        from.add(a);
        to.add(b);
        degree[a]++;
        degree[b]++;
    }

    /** Links cell {@code c} with the cells of the column at (+dx, +dz); a pair is added once. */
    private static void link(Grid grid, int[] index, int size, int height, short[] cx, short[] cy, short[] cz, short[] cs,
            boolean[] tread, boolean[] stair, boolean[] climb, int c, int dx, int dz, IntList from, IntList to, int[] degree) {
        int nx = cx[c] + dx;
        int nz = cz[c] + dz;
        if (nx >= size || nz >= size) {
            return;
        }
        int lowest = Math.max(0, cy[c] - 2);
        int highest = Math.min(height - 1, cy[c] + 2);
        for (int level = lowest; level <= highest; level++) {
            int other = index[(level * size + nz) * size + nx];
            if (other < 0) {
                continue;
            }
            int delta = cs[other] - cs[c];
            if (delta > STEP16 || delta < -STEP16) {
                continue;
            }
            // Walking onto a slab or stair (or a ladder) is a half step; any other rise of half a block or more is a jump.
            boolean jump = !tread[c] && !tread[other] && Math.abs(delta) >= JUMP16;
            // Moving between two heights needs room for the body above the higher surface in both columns, except on
            // stairs, which are built for it: stepping off a slab into a two high corridor is blocked, a jump is too.
            boolean flat = delta == 0;
            boolean onStairs = stair[c] || stair[other];
            boolean ladder = climb[c] || climb[other];
            if (jump || !flat && !onStairs && !ladder) {
                int top = Math.max(cs[c], cs[other]);
                if (!grid.free(cx[c], cz[c], top, BODY16) || !grid.free(nx, nz, top, BODY16)) {
                    continue;
                }
            }
            add(from, to, degree, c, other);
        }
    }

    /** Read access to the sampled blocks. */
    private static final class Grid {
        private final int[] codes;
        private final int size;
        private final int height;

        Grid(int[] codes, int size, int height) {
            this.codes = codes;
            this.size = size;
            this.height = height;
        }

        /** True when a hazard block (lava, void liquid, fire, magma, cactus) is in the body space above the surface. */
        boolean hazard(int x, int z, int from16, int body16) {
            int first = Math.max(0, from16 >> 4);
            int last = Math.min(height - 1, (from16 + body16 - 1) >> 4);
            for (int k = first; k <= last; k++) {
                if (BlockCode.hazardous(codes[(k * size + z) * size + x])) {
                    return true;
                }
            }
            return false;
        }

        /**
         * True when column (x, z) has no collision between {@code from16} and {@code from16 + body16}. The space
         * above the scanned box counts as free.
         */
        boolean free(int x, int z, int from16, int body16) {
            int first = Math.max(0, from16 >> 4);
            int last = Math.min(height - 1, (from16 + body16 - 1) >> 4);
            for (int k = first; k <= last; k++) {
                int code = codes[(k * size + z) * size + x];
                if (!BlockCode.collides(code)) {
                    continue;
                }
                int bottom = k * 16 + BlockCode.lo(code);
                int top = k * 16 + BlockCode.hi(code);
                if (bottom < from16 + body16 && top > from16) {
                    return false;
                }
            }
            return true;
        }
    }
}
