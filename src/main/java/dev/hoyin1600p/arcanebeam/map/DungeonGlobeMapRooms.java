package dev.hoyin1600p.arcanebeam.map;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapOpacity.Factors;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapOpacity.RevealKind;
import iskallia.vault.client.map.VaultMapData;
import iskallia.vault.core.event.common.DiscoverVaultRoomEvent;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.ClientVaults;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.vault.stat.DetailedRoomEntry;
import iskallia.vault.core.vault.stat.DetailedRoomStat;
import iskallia.vault.core.vault.stat.StatCollector;
import iskallia.vault.core.vault.stat.StatsCollector;
import iskallia.vault.core.world.generator.GridGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fades rooms on Vault's map renderers that the local player did not discover by walking, until the player
 * enters them (see {@link DungeonGlobeMapOpacity} for the exact context rule).
 *
 * <p>Every entry point is fail safe: {@link dev.hoyin1600p.arcanebeam.hook.MapHooks} routes exceptions to
 * {@link #fail(String, Throwable)}, which logs once and switches the feature off for the session. All render
 * state is touched on the render thread only, and only when at least one room of the frame will really fade.
 */
public final class DungeonGlobeMapRooms {
    /** Which Vault renderer is drawing; each has its own hook-health counters. */
    public enum Surface {
        OVERLAY("map overlay"),
        MINIMAP("minimap"),
        MAP_SCREEN("full map screen");

        final String label;
        private int framesWithoutRoomsHook;
        private boolean roomsHookEverFired;
        private boolean warnedMissingRoomsHook;

        Surface(String label) {
            this.label = label;
        }
    }

    /** Frames with a fadable room but no getRooms hit before the missing hook is reported. */
    private static final int MISSING_HOOK_FRAME_THRESHOLD = 600;

    private static final GlobeFadeFrame FADE = new GlobeFadeFrame(new GlGlobeState());
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ArcaneBeam-visited-rooms");
        thread.setDaemon(true);
        return thread;
    });
    private static VisitedRoomStore store;
    private static boolean disabled;
    private static boolean loggedFailure;
    private static boolean loggedLateRestore;
    private static boolean loggedMissingRoomHook;
    private static UUID loggedVaultId;

    private static Surface frameSurface;
    private static DetailedRoomStat frameRooms;
    private static UUID frameVaultId;
    private static Factors frameFactors = Factors.NONE;
    private static boolean frameFadeNeeded;
    private static boolean frameRoomsHook;
    private static boolean frameRoomRedirect;
    private static int frameRoomCount;
    private static int frameSystemCount;
    private static int frameSharedCount;
    private static int frameFadedCount;

    private DungeonGlobeMapRooms() {
    }

    public static boolean isDisabled() {
        return disabled;
    }

    /** Called when any globe-map hook threw: log once with the stack, switch the feature off, restore state. */
    public static void fail(String where, Throwable error) {
        disabled = true;
        if (!loggedFailure) {
            loggedFailure = true;
            ArcaneBeam.LOGGER.warn("Dungeon globe map opacity failed in {} and is disabled for this session", where, error);
        }
        try {
            FADE.end();
        } catch (RuntimeException restoreFailure) {
            ArcaneBeam.LOGGER.warn("Dungeon globe map opacity could not restore render state", restoreFailure);
        }
        clearFrame();
    }

    public static void tick(Minecraft minecraft) {
        try {
            LocalPlayer player = minecraft.player;
            if (minecraft.level == null || player == null) {
                submit(store().snapshotIfDue(true));
                return;
            }
            if (VaultUtils.isVaultLevel(minecraft.level)) {
                ClientVaults.getActive().ifPresent(vault -> recordVisit(vault, player));
            }
            submit(store().snapshotIfDue(false));
        } catch (RuntimeException | LinkageError error) {
            fail("tick", error);
        }
    }

    public static void beginFrame(Surface surface, Vault vault) {
        clearFrame();
        if (disabled) {
            return;
        }
        Factors factors = DungeonGlobeMapOpacity.configuredFactors();
        LocalPlayer player = Minecraft.getInstance().player;
        if (!factors.anyFade() || player == null || vault == null) {
            return;
        }
        UUID vaultId = vaultId(vault);
        if (vaultId == null) {
            return;
        }
        recordVisit(vault, player);
        DetailedRoomStat rooms = readRooms(vault, player);
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        frameSurface = surface;
        frameVaultId = vaultId;
        frameFactors = factors;
        frameRooms = rooms;
        scanRooms();
        if (!frameFadeNeeded) {
            // Nothing will fade this frame: leave every render call untouched.
            frameRooms = null;
        }
    }

    /** Decides before any GL work whether at least one room will be drawn faded. */
    private static void scanRooms() {
        frameFadeNeeded = false;
        frameSystemCount = 0;
        frameSharedCount = 0;
        int faded = 0;
        for (Map.Entry<BlockPos, DetailedRoomEntry> room : frameRooms.entrySet()) {
            RevealKind kind = kindOf(room.getValue());
            if (kind == RevealKind.NONE) {
                continue;
            }
            if (kind == RevealKind.SYSTEM) {
                frameSystemCount++;
            } else {
                frameSharedCount++;
            }
            if (DungeonGlobeMapOpacity.fades(kind, isVisited(room.getKey()), frameFactors)) {
                faded++;
            }
        }
        frameFadedCount = faded;
        frameFadeNeeded = faded > 0;
    }

    public static void beginRooms() {
        if (frameRooms == null) {
            return;
        }
        frameRoomsHook = true;
        frameSurface.roomsHookEverFired = true;
        frameSurface.framesWithoutRoomsHook = 0;
        frameRoomCount = 0;
        FADE.begin();
    }

    public static void applyRoom(VaultMapData.Room room) {
        if (!FADE.isActive() || frameRooms == null) {
            return;
        }
        frameRoomRedirect = true;
        BlockPos pos = room.getPos();
        RevealKind kind = kindOf(frameRooms.get(pos));
        frameRoomCount++;
        FADE.setFactor(DungeonGlobeMapOpacity.roomFactor(kind, isVisited(pos), frameFactors));
    }

    public static void endRooms() {
        if (!FADE.isActive()) {
            return;
        }
        FADE.end();
        if (!frameRoomRedirect && !loggedMissingRoomHook) {
            loggedMissingRoomHook = true;
            ArcaneBeam.LOGGER.warn("Dungeon globe map opacity could not hook Vault room drawing on the {}; "
                    + "revealed rooms will not fade (Vault probably changed Room.getRoomId in that renderer)", frameSurface.label);
        }
        logVaultSummary();
    }

    /** Always-run cleanup: restores render state if the rooms pass was begun but not ended, then clears the frame. */
    public static void endFrame() {
        if (FADE.isActive()) {
            Surface surface = frameSurface;
            FADE.end();
            if (!loggedLateRestore) {
                loggedLateRestore = true;
                ArcaneBeam.LOGGER.warn("Dungeon globe map opacity restored render state late on the {}; "
                        + "the Vault getTunnels() call site was not reached (Vault probably changed that renderer)",
                        surface == null ? "unknown surface" : surface.label);
            }
        }
        if (frameSurface != null && frameFadeNeeded && !frameRoomsHook) {
            noteMissingRoomsHook(frameSurface);
        }
        clearFrame();
    }

    private static void noteMissingRoomsHook(Surface surface) {
        if (surface.roomsHookEverFired || surface.warnedMissingRoomsHook) {
            return;
        }
        if (++surface.framesWithoutRoomsHook >= MISSING_HOOK_FRAME_THRESHOLD) {
            surface.warnedMissingRoomsHook = true;
            ArcaneBeam.LOGGER.warn("Dungeon globe map opacity never saw Vault draw rooms on the {} although rooms are "
                    + "revealed. Expected if that map is hidden or you carry no map item; otherwise Vault probably "
                    + "changed the VaultMapData.getRooms() call site there and those rooms will not fade", surface.label);
        }
    }

    private static void clearFrame() {
        frameSurface = null;
        frameRooms = null;
        frameVaultId = null;
        frameFactors = Factors.NONE;
        frameFadeNeeded = false;
        frameRoomsHook = false;
        frameRoomRedirect = false;
    }

    /** One line per vault so a play-test log shows what the fade saw. */
    private static void logVaultSummary() {
        if (frameVaultId == null || frameVaultId.equals(loggedVaultId)) {
            return;
        }
        loggedVaultId = frameVaultId;
        ArcaneBeam.LOGGER.debug("Dungeon globe map ({}): {} rooms drawn, {} revealed by system, {} shared by teammates, "
                        + "{} faded (revealed {}%, teammate {}%, map alpha {})",
                frameSurface.label, frameRoomCount, frameSystemCount, frameSharedCount, frameFadedCount,
                Math.round(frameFactors.revealed() * 100.0F), Math.round(frameFactors.shared() * 100.0F), FADE.baseAlpha());
    }

    private static RevealKind kindOf(DetailedRoomEntry entry) {
        if (entry == null || !entry.has(DetailedRoomEntry.CONTEXT)) {
            return RevealKind.NONE;
        }
        DiscoverVaultRoomEvent.Context context = entry.get(DetailedRoomEntry.CONTEXT);
        return DungeonGlobeMapOpacity.classify(context == null ? null : context.name());
    }

    private static boolean isVisited(BlockPos pos) {
        return store().contains(frameVaultId, pos.getX(), pos.getZ());
    }

    private static DetailedRoomStat readRooms(Vault vault, LocalPlayer player) {
        StatsCollector stats = vault.getOptional(Vault.STATS).orElse(null);
        if (stats == null || stats.getMap() == null) {
            return null;
        }
        StatCollector collector = stats.get(player.getUUID());
        return collector == null ? null : collector.getDetailedRoomsDiscovered();
    }

    private static void recordVisit(Vault vault, LocalPlayer player) {
        UUID vaultId = vaultId(vault);
        if (vaultId == null) {
            return;
        }
        if (!(vault.getOptional(Vault.WORLD).map(world -> world.get(WorldManager.GENERATOR)).orElse(null) instanceof GridGenerator generator)) {
            return;
        }
        Integer cellX = generator.get(GridGenerator.CELL_X);
        Integer cellZ = generator.get(GridGenerator.CELL_Z);
        if (cellX == null || cellZ == null || cellX <= 0 || cellZ <= 0) {
            return;
        }
        RegionPos region = RegionPos.ofBlockPos(player.blockPosition(), cellX, cellZ);
        store().record(vaultId, region.getX(), region.getZ());
    }

    private static UUID vaultId(Vault vault) {
        return vault.has(Vault.ID) ? vault.get(Vault.ID) : null;
    }

    private static VisitedRoomStore store() {
        if (store == null) {
            store = new VisitedRoomStore(FMLPaths.CONFIGDIR.get().resolve("arcanebeam").resolve("visited_rooms.json"),
                    System::currentTimeMillis);
        }
        return store;
    }

    private static void submit(VisitedRoomStore.Snapshot snapshot) {
        if (snapshot != null) {
            WRITER.execute(snapshot::write);
        }
    }

    /** Real GL/RenderSystem access for {@link GlobeFadeFrame}. */
    private static final class GlGlobeState implements GlobeFadeFrame.Gl {
        private boolean enabled;
        private int srcRgb;
        private int dstRgb;
        private int srcAlpha;
        private int dstAlpha;

        @Override
        public void captureAndEnableAlphaBlend(float[] out) {
            System.arraycopy(RenderSystem.getShaderColor(), 0, out, 0, out.length);
            enabled = GL11.glIsEnabled(GL11.GL_BLEND);
            srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            // The HUD enables blending but inherits whatever function the previous element left; without a
            // source-alpha function the faded alpha is ignored and rooms draw solid.
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            RenderSystem.setShaderColor(r, g, b, a);
        }

        @Override
        public void restoreBlend() {
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (enabled) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
        }
    }
}
