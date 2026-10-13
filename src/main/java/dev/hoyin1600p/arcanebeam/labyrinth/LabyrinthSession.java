package dev.hoyin1600p.arcanebeam.labyrinth;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The live state of one labyrinth room: the block snapshot, the model built from it, the lines, the route and the
 * line-of-sight caches. Client thread only, except the build task, which only reads the snapshot it was given
 * (the snapshot is not touched while a build runs).
 * <p>
 * The first scan samples the whole box a few layers per tick. After that only what changed is read again: blocks
 * the level reported as changed, and chunks that were not loaded during the last scan and have loaded since. The
 * model is rebuilt from the updated snapshot on the worker thread.
 */
final class LabyrinthSession {
    /** Layers (47 by 47 blocks each) sampled per tick: a village scan takes about 3 ticks, the Wolds labyrinth 8. */
    static final int LAYERS_PER_TICK = 6;
    /** How often the chunks of the box are checked for having loaded, unloaded or been re-sent, in ticks. */
    static final int CHUNK_POLL_TICKS = 10;
    /** More changed blocks than this at once (a pasted structure, a regenerated room) are handled by a full scan. */
    static final int MAX_TRACKED_CHANGES = 4096;
    /** Route pieces sight-checked per tick (at most three rays each). */
    static final int ROUTE_BUDGET = 24;
    private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ArcaneBeam labyrinth builder");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    final LabyrinthRooms.Room room;
    private final int height;
    private final int size = RoomTransform.ROOM;

    /** The block snapshot; complete once the first scan has finished. */
    private BoxSnapshot snapshot;
    private boolean sampling;
    private LevelSampler sampler;
    private Future<LabyrinthModel> building;
    private LabyrinthModel model;
    /** The chunk object each chunk of the box had when it was last read (null when it was not loaded). */
    private Long2ObjectOpenHashMap<LevelChunk> chunkRefs = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet changed = new LongOpenHashSet();
    private final LongOpenHashSet chunksToRead = new LongOpenHashSet();
    private final DirtyTracker tracker = new DirtyTracker();
    private boolean fullScan = true;
    private int scans;
    private int updates;

    PieceLayer route = PieceLayer.EMPTY;
    /** The route drawn now (box-local), or null. */
    private Route current;
    private int playerCell = -1;
    /** Surface height (world) under the player's cell, for the head of the route. */
    double headSurfaceY;
    boolean hasRoute;

    private final Sighter sighter = new Sighter();

    LabyrinthSession(LabyrinthRooms.Room room) {
        this.room = room;
        this.height = room.variant().scanHeight();
        tracker.mark(Integer.MIN_VALUE / 2);
    }

    boolean ready() {
        return model != null;
    }

    /**
     * Drops the queued or running build and the sampler. Called when the session is replaced or cleared, so a build
     * that is still queued for an old room does not hold up the next room on the single builder thread.
     */
    void close() {
        Future<LabyrinthModel> pending = building;
        building = null;
        if (pending != null) {
            pending.cancel(false);
        }
        sampler = null;
        sampling = false;
    }

    /** True when the world position is inside the scanned box. */
    boolean contains(BlockPos pos) {
        int x = pos.getX() - room.originX();
        int y = pos.getY() - room.originY();
        int z = pos.getZ() - room.originZ();
        return x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height;
    }

    /** Records a block change inside the box; the room is updated once changes have been quiet for a moment. */
    void markChanged(BlockPos pos, int tick) {
        tracker.mark(tick);
        if (changed.size() >= MAX_TRACKED_CHANGES) {
            fullScan = true;
        } else {
            changed.add(pos.asLong());
        }
    }

    /** Advances scanning and route/culling work for one client tick. */
    void tick(ClientLevel level, LocalPlayer player, int tick, float elevation, float margin, int maxDistance, boolean cull) {
        if (building != null && building.isDone()) {
            finishBuild();
        }
        if (sampling) {
            advanceSampling();
        } else if (building == null) {
            if (tick % CHUNK_POLL_TICKS == 0 && collectChangedChunks(level)) {
                tracker.mark(tick - DirtyTracker.DEBOUNCE_TICKS);
            }
            if (tracker.due(tick)) {
                tracker.started(tick);
                if (snapshot == null || fullScan) {
                    startFullScan(level);
                } else {
                    updateChanged(level);
                }
            }
        }
        if (model == null) {
            return;
        }
        updateRoute(player);
        route.ensureBounds(elevation, margin);
        if (cull) {
            Vec3 eye = player.getEyePosition();
            sighter.prepare(level, player, eye, elevation);
            sighter.layer = route;
            route.culler.refresh(tick, route.pieces, eye.x, eye.y, eye.z, maxDistance, ROUTE_BUDGET, PieceCuller.DEFAULT_SCAN_LIMIT, sighter);
        }
    }

    // ---- scanning ----------------------------------------------------------------------------------------------

    private void startFullScan(ClientLevel level) {
        fullScan = false;
        changed.clear();
        chunksToRead.clear();
        snapshot = new BoxSnapshot(size, height, room.originX(), room.originZ());
        sampler = new LevelSampler(level, room.originX(), room.originY(), room.originZ());
        sampling = true;
        scans++;
    }

    private void advanceSampling() {
        if (!snapshot.sampleLayers(sampler, LAYERS_PER_TICK)) {
            return;
        }
        chunkRefs = sampler.chunkRefs();
        sampler = null;
        sampling = false;
        submitBuild();
    }

    /** Reads again the blocks that changed and the chunks that have loaded or been replaced since the last scan. */
    private void updateChanged(ClientLevel level) {
        LevelSampler reader = new LevelSampler(level, room.originX(), room.originY(), room.originZ());
        boolean any = false;
        int blocks = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long packed : changed) {
            pos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (!contains(pos)) {
                continue;
            }
            blocks++;
            any |= snapshot.reread(reader, pos.getX(), pos.getY() - room.originY(), pos.getZ());
        }
        changed.clear();
        for (long key : chunksToRead) {
            snapshot.rereadChunk(reader, ChunkPos.getX(key), ChunkPos.getZ(key));
            blocks += height * 256;
            any = true;
        }
        chunksToRead.clear();
        // Chunks read now have a new reference; the others keep the one they had.
        Long2ObjectOpenHashMap<LevelChunk> refs = new Long2ObjectOpenHashMap<>(chunkRefs);
        refs.putAll(reader.chunkRefs());
        chunkRefs = refs;
        updates++;
        ArcaneBeam.LOGGER.debug("Labyrinth {}: update #{} re-read {} blocks, {}", room.variant().label, updates, blocks,
                any ? "rebuilding" : "nothing that matters changed");
        if (any) {
            submitBuild();
        }
    }

    private void submitBuild() {
        int[] copy = snapshot.copy();
        int unknownBlocks = snapshot.unknownBlocks();
        LabyrinthVariant variant = room.variant();
        // The copy is private to the build thread.
        building = BUILDER.submit(() -> LabyrinthModel.build(copy, variant, unknownBlocks));
    }

    private void finishBuild() {
        Future<LabyrinthModel> done = building;
        building = null;
        try {
            model = done.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException failure) {
            throw new IllegalStateException("labyrinth build failed", failure.getCause());
        }
        // The old route belongs to the old model (a door may have closed since): drop it, it is placed again this tick.
        playerCell = -1;
        current = null;
        route = PieceLayer.EMPTY;
        hasRoute = false;
        ArcaneBeam.LOGGER.debug("Labyrinth {}: {} cells, exit via {}{} ({} cells), built in {} ms, {} unknown blocks in {} chunks not loaded",
                room.variant().label, model.graph.count, model.exit.method(),
                model.exit.transform() == null ? "" : " " + model.exit.transform(), model.exit.cells().length, model.buildMillis,
                model.graph.unknownBlocks, missingCount());
        if (!model.exit.found()) {
            ArcaneBeam.LOGGER.debug("Labyrinth {}: no exit cell found, no route is shown", room.variant().label);
        }
    }

    private int missingCount() {
        int missing = 0;
        for (LevelChunk chunk : chunkRefs.values()) {
            if (chunk == null) {
                missing++;
            }
        }
        return missing;
    }

    /**
     * True when a loaded chunk of the box is a different object than at the last read: it was not loaded and is now, or was
     * re-sent by the server (which replaces its blocks without a block update); those chunks are
     * queued for reading.
     */
    private boolean collectChangedChunks(ClientLevel level) {
        boolean any = false;
        for (Long2ObjectOpenHashMap.Entry<LevelChunk> entry : chunkRefs.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            LevelChunk now = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (now != null && now != entry.getValue()) {
                chunksToRead.add(key);
                any = true;
            }
        }
        return any;
    }

    // ---- route ----------------------------------------------------------------------------------------------------

    private void updateRoute(LocalPlayer player) {
        int cell = PlayerCell.find(model.graph, player.getX(), player.getY(), player.getZ(), room.originX(), room.originY(), room.originZ());
        if (cell < 0 || cell == playerCell) {
            return;
        }
        playerCell = cell;
        headSurfaceY = room.originY() + model.graph.surface(cell);
        Route found = nextRoute(model, current, cell);
        if (found == current) {
            return;
        }
        current = found;
        hasRoute = found != null;
        PieceLayer next = found == null ? PieceLayer.EMPTY
                : new PieceLayer(LinePieces.fromRoute(found, room.originX(), room.originY(), room.originZ()));
        next.adoptFrom(route);
        route = next;
    }

    /**
     * The route to show once the player stands on {@code cell}: none in the exit passage, at the mouth or anywhere not
     * connected to it; the rest of the route already shown while the player is still on it (so it does not jump
     * about); otherwise a new route from the cell.
     */
    static Route nextRoute(LabyrinthModel model, Route shown, int cell) {
        RouteField field = model.field;
        if (model.inPassage(cell) || !field.reachable(cell) || field.atMouth(cell)) {
            return null;
        }
        WalkGraph g = model.graph;
        Route kept = RouteField.follow(shown, g.x(cell) + 0.5, g.surface(cell), g.z(cell) + 0.5);
        return kept != null ? kept : field.route(cell);
    }

    // ---- line of sight ----------------------------------------------------------------------------------------------

    /** Ray tests from the eye to a piece: visible when its middle or either end can be seen. */
    private static final class Sighter implements PieceCuller.Sight {
        PieceLayer layer = PieceLayer.EMPTY;
        private ClientLevel level;
        private LocalPlayer player;
        private Vec3 eye;
        private float elevation;
        private final BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

        void prepare(ClientLevel level, LocalPlayer player, Vec3 eye, float elevation) {
            this.level = level;
            this.player = player;
            this.eye = eye;
            this.elevation = elevation;
        }

        @Override
        public boolean visible(int i) {
            LinePieces p = layer.pieces;
            double mx = p.midX(i);
            double my = p.midY(i) + elevation;
            double mz = p.midZ(i);
            layer.light[i] = LevelRenderer.getLightColor(level, lightPos.set(mx, my, mz));
            return clear(mx, my, mz) || clear(p.startX(i), p.startY(i) + elevation, p.startZ(i))
                    || clear(p.endX(i), p.endY(i) + elevation, p.endZ(i));
        }

        private boolean clear(double x, double y, double z) {
            ClipContext context = new ClipContext(eye, new Vec3(x, y, z), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);
            return level.clip(context).getType() == HitResult.Type.MISS;
        }
    }
}
