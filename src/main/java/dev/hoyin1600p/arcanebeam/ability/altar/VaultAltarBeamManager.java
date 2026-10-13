package dev.hoyin1600p.arcanebeam.ability.altar;

import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSoundMode;
import iskallia.vault.block.entity.VaultAltarTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VaultAltarBeamManager {
    private static final int PARTICLE_REFRESH_GRACE_TICKS = 12;
    private static final double ALTAR_CENTER_TOLERANCE_SQR = 0.0125D;
    private static final double ALTAR_CENTER_AXIS_TOLERANCE = 0.12D;
    private static final long PARTICLE_STALE_TICKS = 100L;
    private static final long MAX_BEAM_LIFETIME_TICKS = 1200L;
    private static final double ALTAR_COMPLETION_DUST_TOLERANCE_SQR = 4.0D;
    private static final double ALTAR_SOUND_TOLERANCE_SQR = 9.0D;
    private static final double ALTAR_TOP_OFFSET = 17.25D / 16.0D;
    private static final int ORIGIN_MARKER_SCAN_INTERVAL_TICKS = 10;
    private static final int ORIGIN_MARKER_HORIZONTAL_RADIUS = 32;
    private static final int ORIGIN_MARKER_VERTICAL_RADIUS = 16;
    private static final Map<BlockPos, ActiveAltarBeam> activeBeams = new LinkedHashMap<>();
    private static final List<BlockPos> originMarkerAltars = new ArrayList<>();
    private static long nextOriginMarkerScanGameTime = Long.MIN_VALUE;

    private VaultAltarBeamManager() {
    }

    public static boolean captureParticle(ParticleOptions particle, double x, double y, double z) {
        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled) {
            return false;
        }

        // Identity checks first: this runs for every client particle.
        boolean portalParticle = particle.getType() == ParticleTypes.PORTAL;
        boolean greenDust = !portalParticle && particle instanceof DustParticleOptions && isGreenDust(particle);
        if (!portalParticle && !greenDust) {
            return false;
        }

        if (greenDust) {
            // The completion burst only matters for altars we already render; never scan the world for it.
            if (activeBeams.isEmpty()) {
                return false;
            }
            for (BlockPos tracked : activeBeams.keySet()) {
                if (altarParticleCenter(tracked).distanceToSqr(x, y, z) <= ALTAR_COMPLETION_DUST_TOLERANCE_SQR) {
                    return true;
                }
            }
            return false;
        }

        // Altar portal particles sit at x.5 / y+1.6 / z.5; reject everything else without touching the level.
        if (Math.abs(x - Math.floor(x) - 0.5D) > ALTAR_CENTER_AXIS_TOLERANCE
                || Math.abs(z - Math.floor(z) - 0.5D) > ALTAR_CENTER_AXIS_TOLERANCE
                || Math.abs(y - Math.floor(y) - 0.6D) > ALTAR_CENTER_AXIS_TOLERANCE) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        BlockPos altarPos = findMatchingAltar(level, x, y, z, ALTAR_CENTER_TOLERANCE_SQR);
        if (altarPos == null) {
            return false;
        }
        refreshAltar(level, altarPos, settings);
        return true;
    }

    public static boolean handleVaultAltarStartSound(double x, double y, double z) {
        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled || vaultAltarSoundMode() == VaultAltarSoundMode.DEFAULT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !ArcaneBeamSoundController.canPlayVaultAltarBeamStart(minecraft)) {
            return false;
        }

        BlockPos altarPos = findAltarNearSound(level, x, y, z);
        if (altarPos == null) {
            return false;
        }

        refreshAltar(level, altarPos, settings);
        return true;
    }

    public static boolean handleVaultAltarCompletionSound(double x, double y, double z) {
        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled || vaultAltarSoundMode() == VaultAltarSoundMode.DEFAULT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !ArcaneBeamSoundController.canPlayVaultAltarBeamStart(minecraft)) {
            return false;
        }

        BlockPos altarPos = findAltarNearSound(level, x, y, z);
        return altarPos != null && activeBeams.containsKey(altarPos);
    }

    private static void refreshAltar(ClientLevel level, BlockPos altarPos, VaultAltarSettings settings) {
        long gameTime = level.getGameTime();
        ActiveAltarBeam existing = activeBeams.get(altarPos);
        if (existing == null) {
            activeBeams.put(altarPos, new ActiveAltarBeam(altarPos.immutable(), gameTime, gameTime, VaultAltarRenderSettings.from(settings)));
            ArcaneBeamSoundController.playVaultAltarBeamStart(Minecraft.getInstance(), altarTopCenter(altarPos), settings.soundVolume);
            return;
        }
        activeBeams.put(altarPos, new ActiveAltarBeam(altarPos.immutable(), existing.startGameTime(), gameTime, VaultAltarRenderSettings.from(settings)));
    }

    private static boolean isGreenDust(ParticleOptions particle) {
        if (!(particle instanceof DustParticleOptions dust)) {
            return false;
        }
        return dust.getColor().y() >= 0.9F && dust.getColor().x() <= 0.15F && dust.getColor().z() <= 0.15F;
    }

    private static BlockPos findMatchingAltar(ClientLevel level, double x, double y, double z, double toleranceSqr) {
        BlockPos base = new BlockPos(x, y - 1.6D, z);
        int searchRadius = 0;
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos candidate = base.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(candidate);
                    if (!(blockEntity instanceof VaultAltarTileEntity)) {
                        continue;
                    }
                    Vec3 expected = altarParticleCenter(candidate);
                    if (expected.distanceToSqr(x, y, z) <= toleranceSqr) {
                        return candidate.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static Vec3 altarParticleCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 1.6D, pos.getZ() + 0.5D);
    }

    private static Vec3 altarTopCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + ALTAR_TOP_OFFSET, pos.getZ() + 0.5D);
    }

    private static BlockPos findAltarNearSound(ClientLevel level, double x, double y, double z) {
        BlockPos base = new BlockPos(x, y, z);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = base.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(candidate);
                    if (!(blockEntity instanceof VaultAltarTileEntity)) {
                        continue;
                    }
                    double distance = Vec3.atCenterOf(candidate).distanceToSqr(x, y, z);
                    if (distance <= ALTAR_SOUND_TOLERANCE_SQR && distance < bestDistance) {
                        best = candidate.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private static VaultAltarSoundMode vaultAltarSoundMode() {
        VaultAltarSoundMode mode = VaultAltarSoundMode.fromId(ArcaneBeamConfig.INSTANCE.vaultAltar.soundMode);
        return mode == null ? VaultAltarSoundMode.DEFAULT : mode;
    }

    /** Drops all per-level state; called when the client level instance changes. */
    public static void clear() {
        activeBeams.clear();
        originMarkerAltars.clear();
        nextOriginMarkerScanGameTime = Long.MIN_VALUE;
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            activeBeams.clear();
            originMarkerAltars.clear();
            nextOriginMarkerScanGameTime = Long.MIN_VALUE;
            return;
        }

        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled) {
            activeBeams.clear();
            originMarkerAltars.clear();
            nextOriginMarkerScanGameTime = Long.MIN_VALUE;
            return;
        }

        long gameTime = level.getGameTime();
        updateOriginMarkerAltars(minecraft, level, gameTime);
        Iterator<Map.Entry<BlockPos, ActiveAltarBeam>> iterator = activeBeams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ActiveAltarBeam> entry = iterator.next();
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof VaultAltarTileEntity altar)) {
                iterator.remove();
                continue;
            }

            int timer = altar.getInfusionTimer();
            ActiveAltarBeam beam = entry.getValue();
            long sinceParticle = gameTime - beam.lastParticleGameTime();
            if ((timer <= 0 && sinceParticle > PARTICLE_REFRESH_GRACE_TICKS)
                    || sinceParticle > PARTICLE_STALE_TICKS
                    || gameTime < beam.startGameTime()
                    || gameTime - beam.startGameTime() > MAX_BEAM_LIFETIME_TICKS) {
                iterator.remove();
            }
        }
    }

    private static void updateOriginMarkerAltars(Minecraft minecraft, ClientLevel level, long gameTime) {
        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.originMarkersEnabled) {
            originMarkerAltars.clear();
            nextOriginMarkerScanGameTime = Long.MIN_VALUE;
            return;
        }
        if (minecraft.player == null) {
            return;
        }
        if (gameTime < nextOriginMarkerScanGameTime) {
            return;
        }

        nextOriginMarkerScanGameTime = gameTime + ORIGIN_MARKER_SCAN_INTERVAL_TICKS;
        originMarkerAltars.clear();
        BlockPos center = minecraft.player.blockPosition();
        int radiusSqr = ORIGIN_MARKER_HORIZONTAL_RADIUS * ORIGIN_MARKER_HORIZONTAL_RADIUS;
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - ORIGIN_MARKER_HORIZONTAL_RADIUS);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + ORIGIN_MARKER_HORIZONTAL_RADIUS);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - ORIGIN_MARKER_HORIZONTAL_RADIUS);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + ORIGIN_MARKER_HORIZONTAL_RADIUS);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    int dx = pos.getX() - center.getX();
                    int dz = pos.getZ() - center.getZ();
                    if (dx * dx + dz * dz <= radiusSqr
                            && Math.abs(pos.getY() - center.getY()) <= ORIGIN_MARKER_VERTICAL_RADIUS
                            && entry.getValue() instanceof VaultAltarTileEntity) {
                        originMarkerAltars.add(pos.immutable());
                    }
                }
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled) {
            return;
        }
        boolean renderOriginMarkers = settings.originMarkersEnabled && !originMarkerAltars.isEmpty();
        if (activeBeams.isEmpty() && !renderOriginMarkers) {
            return;
        }

        VaultAltarBeamRenderer.render(
                event.getPoseStack(),
                event.getCamera().getPosition(),
                event.getPartialTick(),
                activeBeams.values(),
                renderOriginMarkers ? originMarkerAltars : List.of(),
                renderOriginMarkers ? VaultAltarRenderSettings.from(settings) : null
        );
    }

    public record ActiveAltarBeam(BlockPos pos, long startGameTime, long lastParticleGameTime, VaultAltarRenderSettings settings) {
        public float age(float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft.level == null ? startGameTime : minecraft.level.getGameTime();
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }
    }

    public record VaultAltarRenderSettings(
            int[] cornerColors,
            float cornerRadius,
            float cornerOpacity,
            int cornerVerticalTicks,
            int cornerConvergeTicks,
            int centerGrowTicks,
            float cornerOriginHeight,
            float cornerOriginRadius,
            int[] centerColors,
            float centerHeight,
            float centerFadeHeight,
            float centerBottomRadius,
            float centerTopRadius,
            float centerOpacity,
            int[] centerGlowColors,
            float centerGlowHeight,
            float centerGlowFadeHeight,
            float centerGlowBottomRadius,
            float centerGlowTopRadius,
            float centerGlowOpacity,
            float centerGlowRotationRpm,
            boolean fullbright,
            String shaderCompatibility
    ) {
        private static VaultAltarRenderSettings from(VaultAltarSettings settings) {
            return new VaultAltarRenderSettings(
                    settings.cornerColors == null ? new int[]{0x66DDFF, 0xFFFFFF} : settings.cornerColors.clone(),
                    settings.cornerRadius,
                    settings.cornerOpacity,
                    settings.cornerVerticalTicks,
                    settings.cornerConvergeTicks,
                    settings.centerGrowTicks,
                    settings.cornerOriginHeight,
                    settings.cornerOriginRadius,
                    settings.centerColors == null ? new int[]{0xD8FFFF, 0x5CB8FF} : settings.centerColors.clone(),
                    settings.centerHeight,
                    settings.centerFadeHeight,
                    settings.centerBottomRadius,
                    settings.centerTopRadius,
                    settings.centerOpacity,
                    settings.centerGlowColors == null ? new int[]{0x55CFFF, 0xFFFFFF} : settings.centerGlowColors.clone(),
                    settings.centerGlowHeight,
                    settings.centerGlowFadeHeight,
                    settings.centerGlowBottomRadius,
                    settings.centerGlowTopRadius,
                    settings.centerGlowOpacity,
                    settings.centerGlowRotationRpm,
                    settings.fullbright,
                    settings.shaderCompatibility
            );
        }
    }
}
