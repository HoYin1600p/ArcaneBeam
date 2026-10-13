package dev.hoyin1600p.arcanebeam.labyrinth;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reads a box of the client level as {@link BlockCode}s. The box is 47 by 47 blocks from {@code (originX, originY,
 * originZ)}. Blocks in chunks that are not loaded are reported as unknown (and the chunk is remembered, see
 * {@link #missingChunks()}). Client thread only.
 */
final class LevelSampler implements VoxelSampler {
    private final ClientLevel level;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final LongOpenHashSet missing = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<LevelChunk> refs = new Long2ObjectOpenHashMap<>();
    private int chunkX = Integer.MIN_VALUE;
    private int chunkZ = Integer.MIN_VALUE;
    private LevelChunk chunk;

    LevelSampler(ClientLevel level, int originX, int originY, int originZ) {
        this.level = level;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    @Override
    public int sample(int x, int y, int z) {
        int wx = originX + x;
        int wz = originZ + z;
        int cx = wx >> 4;
        int cz = wz >> 4;
        if (cx != chunkX || cz != chunkZ) {
            chunkX = cx;
            chunkZ = cz;
            chunk = level.getChunkSource().getChunkNow(cx, cz);
            refs.put(ChunkPos.asLong(cx, cz), chunk);
            if (chunk == null) {
                missing.add(ChunkPos.asLong(cx, cz));
            }
        }
        if (chunk == null) {
            return BlockCode.unknown();
        }
        pos.set(wx, originY + y, wz);
        return classify(chunk.getBlockState(pos), level, pos);
    }

    /** The chunk object read for each chunk position ({@link ChunkPos#asLong}) of the box; null where it was not loaded. */
    Long2ObjectOpenHashMap<LevelChunk> chunkRefs() {
        return refs;
    }

    /** Chunk positions ({@link ChunkPos#asLong}) of the box that were not loaded while sampling. */
    LongArrayList missingChunks() {
        return new LongArrayList(missing);
    }

    /**
     * The scan's view of a block. Doors, trapdoors and the trapdoor-like panels of other mods (see {@link #doorLike}) are treated as open air because players break or open them; only a
     * closed bottom trapdoor keeps a floor of 3/16 so a trapdoor over a hole stays a place to stand. Everything else
     * follows its real collision shape, so slabs, stairs, carpets and modded blocks are exact. Fluids other than water
     * and blocks that hurt are flagged as hazards.
     */
    static int classify(BlockState state, ClientLevel level, BlockPos pos) {
        if (state.isAir()) {
            return BlockCode.AIR;
        }
        int flags = 0;
        if (state.is(BlockTags.CLIMBABLE)) {
            flags |= BlockCode.CLIMB;
        }
        FluidState fluid = state.getFluidState();
        if (fluid.is(FluidTags.WATER)) {
            flags |= BlockCode.WATER;
        } else if (!fluid.isEmpty()) {
            flags |= BlockCode.HAZARD;
        }
        Block block = state.getBlock();
        if (hurts(state, block)) {
            flags |= BlockCode.HAZARD;
        }
        if (doorLike(state, block)) {
            if (block instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN) && state.getValue(TrapDoorBlock.HALF) == Half.BOTTOM) {
                return flags | BlockCode.solid(0, 3);
            }
            return flags;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return flags;
        }
        int lo = (int) Math.floor(shape.min(Direction.Axis.Y) * 16.0 + 1.0E-6);
        int hi = (int) Math.ceil(shape.max(Direction.Axis.Y) * 16.0 - 1.0E-6);
        if (hi <= lo) {
            return flags;
        }
        flags |= BlockCode.SOLID;
        if (!Block.isShapeFullBlock(shape)) {
            flags |= BlockCode.PARTIAL;
        }
        if (block instanceof StairBlock) {
            flags |= BlockCode.TREAD | BlockCode.STAIR;
        } else if (block instanceof SlabBlock && hi - lo == 8) {
            flags |= BlockCode.TREAD;
        }
        return BlockCode.pack(flags, lo, hi);
    }

    /**
     * Doors and trapdoors of every kind, also those of mods that build them on their own block classes (a copycat iron
     * door): by class, by tag, or by a block id ending in {@code _door} or {@code _trapdoor}.
     */
    private static boolean doorLike(BlockState state, Block block) {
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null && (key.getPath().endsWith("_door") || key.getPath().endsWith("_trapdoor"));
    }

    private static boolean hurts(BlockState state, Block block) {
        return state.is(BlockTags.FIRE) || block instanceof MagmaBlock || block instanceof CactusBlock
                || block instanceof SweetBerryBushBlock || block instanceof PowderSnowBlock
                || block instanceof CampfireBlock && state.getValue(CampfireBlock.LIT);
    }
}
