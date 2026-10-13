package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * What the scan needs to know about one block, packed into an int so the pure algorithms never see Minecraft
 * classes. Heights are in sixteenths of a block above the block's own floor; {@code lo} and {@code hi} are the
 * lowest and highest point of the block's collision shape.
 */
public final class BlockCode {
    /** The block is climbable (ladder, vines, scaffolding). */
    public static final int CLIMB = 1;
    public static final int WATER = 2;
    /** The block's chunk is not loaded; it is treated as a full solid block. */
    public static final int UNKNOWN = 4;
    /** The block has a collision shape that stops a player. */
    public static final int SOLID = 8;
    /**
     * The collision shape is not a full cube (stairs, slabs, carpet, chests, ...): it has a tread lower than a
     * block, so a player climbs it in half steps.
     */
    public static final int PARTIAL = 16;
    /** Standing in or on this block hurts or kills (lava, the Vault's void liquid, fire, magma, cactus, powder snow). */
    public static final int HAZARD = 32;
    /** A slab or stair: its top has an intermediate tread, so a player walks onto it in half steps. */
    public static final int TREAD = 64;
    /** A stair (a tread that rises a whole block over two steps). */
    public static final int STAIR = 128;

    public static final int AIR = 0;

    private BlockCode() {
    }

    public static int pack(int flags, int lo16, int hi16) {
        return (flags & 0xFF) | (clamp(lo16) << 8) | (clamp(hi16) << 16);
    }

    /** A block with a collision box from {@code lo16} to {@code hi16}; partial unless it fills the cube. */
    public static int solid(int lo16, int hi16) {
        boolean full = lo16 == 0 && hi16 == 16;
        // A half block tall shape (bottom or top slab) is walked onto in half steps; carpets, chests and beds are not.
        return pack(SOLID | (full ? 0 : PARTIAL) | (hi16 - lo16 == 8 ? TREAD : 0), lo16, hi16);
    }

    /** A stair: as tall as a block with a half-height tread. */
    public static int stair() {
        return pack(SOLID | PARTIAL | TREAD | STAIR, 0, 16);
    }

    /** A hazard with no collision (lava, void liquid, fire) or the hazard flag added to another code. */
    public static int hazard() {
        return pack(HAZARD, 0, 0);
    }

    public static int fullBlock() {
        return solid(0, 16);
    }

    public static int unknown() {
        return pack(UNKNOWN | SOLID, 0, 16);
    }

    public static int ladder() {
        return pack(CLIMB, 0, 0);
    }

    public static int water() {
        return pack(WATER, 0, 0);
    }

    public static boolean collides(int code) {
        return (code & SOLID) != 0;
    }

    public static boolean climbable(int code) {
        return (code & CLIMB) != 0;
    }

    public static boolean partial(int code) {
        return (code & PARTIAL) != 0;
    }

    public static boolean hazardous(int code) {
        return (code & HAZARD) != 0;
    }

    public static boolean tread(int code) {
        return (code & TREAD) != 0;
    }

    public static boolean stair(int code) {
        return (code & STAIR) != 0;
    }

    public static boolean unknownBlock(int code) {
        return (code & UNKNOWN) != 0;
    }

    public static int lo(int code) {
        return (code >> 8) & 0xFF;
    }

    public static int hi(int code) {
        return (code >> 16) & 0xFF;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
