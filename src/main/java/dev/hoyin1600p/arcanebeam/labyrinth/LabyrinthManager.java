package dev.hoyin1600p.arcanebeam.labyrinth;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Labyrinth Breadcrumbs: while the player is inside the labyrinth of a Vault village challenge room (or the Wolds
 * labyrinth room), scans it from the client level, keeps centre lines and a route to the exit and draws them.
 * <p>
 * Everything runs on the client thread (the pure build runs on a worker). Both entry points are registered through
 * {@code GuardedEvents}, so an exception disables the feature for the session instead of reaching the game loop.
 */
public final class LabyrinthManager {
    private static final LabyrinthRooms ROOMS = new LabyrinthRooms();
    private static LabyrinthSession session;
    /** The session the player is inside right now (null when outside every labyrinth). */
    private static LabyrinthSession active;
    private static ClientLevel sessionLevel;
    private static int tick;
    private static boolean everActive;

    private LabyrinthManager() {
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;
        Minecraft minecraft = Minecraft.getInstance();
        LabyrinthSettings cfg = ArcaneBeamConfig.INSTANCE.labyrinth;
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (cfg == null || !cfg.enabled || player == null || level == null) {
            if (session != null) {
                clear();
            }
            return;
        }
        if (level != sessionLevel) {
            clear();
            sessionLevel = level;
        }
        LabyrinthRooms.Room room = ROOMS.find(player, tick);
        if (room == null || !room.containsColumn(player.getX(), player.getZ()) || !room.containsFeet(player.getY())) {
            active = null;
            return;
        }
        if (session == null || !session.room.sameRoom(room)) {
            if (session != null) {
                session.close();
            }
            session = new LabyrinthSession(room);
            ArcaneBeam.LOGGER.debug("Labyrinth: entered {} (region {}, {}), origin {}, {}, {}", room.variant().label, room.regionX(),
                    room.regionZ(), room.originX(), room.originY(), room.originZ());
        }
        active = session;
        everActive = true;
        boolean show = shown(cfg);
        float margin = Math.max(cfg.width, cfg.thickness) * 1.5F + 0.4F;
        session.tick(level, player, tick, cfg.elevation, margin, cfg.maxDistance, show);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        LabyrinthSession current = active;
        LabyrinthSettings cfg = ArcaneBeamConfig.INSTANCE.labyrinth;
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || cfg == null || !cfg.enabled || !current.ready() || minecraft.player == null || !shown(cfg)) {
            return;
        }
        float partial = event.getPartialTick();
        LocalPlayer player = minecraft.player;
        double px = Mth.lerp((double) partial, player.xOld, player.getX());
        double pz = Mth.lerp((double) partial, player.zOld, player.getZ());
        Vec3 camera = event.getCamera().getPosition();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        LabyrinthRenderer.render(event.getPoseStack(), camera, event.getFrustum(), partial, gameTime, current, cfg, px, pz,
                LabyrinthRenderer.compat());
    }

    private static boolean shown(LabyrinthSettings cfg) {
        return cfg.visibilityMode() == LabyrinthVisibility.ALWAYS || LabyrinthKeys.held();
    }

    /**
     * Called for every client block change; marks the scanned room dirty when the block lies inside it and the change
     * matters to the scan (a villager toggling a door, a furnace lighting or a crop growing does not).
     */
    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        LabyrinthSession current = session;
        if (current == null || !current.contains(pos)) {
            return;
        }
        ClientLevel level = sessionLevel;
        if (level != null && LevelSampler.classify(oldState, level, pos) == LevelSampler.classify(newState, level, pos)) {
            return;
        }
        current.markChanged(pos, tick);
    }

    /** Drops all state: on a level change, when the feature is switched off and when the player leaves the Vault. */
    public static void clear() {
        LabyrinthSession old = session;
        if (old != null) {
            old.close();
        }
        session = null;
        active = null;
        sessionLevel = null;
        ROOMS.reset();
        if (everActive) {
            ArcaneBeam.LOGGER.debug("Labyrinth: state cleared");
            everActive = false;
        }
    }
}
