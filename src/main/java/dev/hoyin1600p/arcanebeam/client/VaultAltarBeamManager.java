package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.block.entity.VaultAltarTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class VaultAltarBeamManager {
    private static final ResourceLocation PORTAL = new ResourceLocation("minecraft", "portal");
    private static final int PARTICLE_REFRESH_GRACE_TICKS = 12;
    private static final double ALTAR_CENTER_TOLERANCE_SQR = 0.0125D;
    private static final double ALTAR_COMPLETION_DUST_TOLERANCE_SQR = 4.0D;
    private static final double ALTAR_SOUND_TOLERANCE_SQR = 9.0D;
    private static final double ALTAR_TOP_OFFSET = 17.25D / 16.0D;
    private static final Map<BlockPos, ActiveAltarBeam> activeBeams = new LinkedHashMap<>();

    private VaultAltarBeamManager() {
    }

    public static boolean captureParticle(ParticleOptions particle, double x, double y, double z) {
        ArcaneBeamConfig.VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled) {
            return false;
        }

        boolean portalParticle = PORTAL.equals(Registry.PARTICLE_TYPE.getKey(particle.getType()));
        boolean greenDust = isGreenDust(particle);
        if (!portalParticle && !greenDust) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        BlockPos altarPos = findMatchingAltar(level, x, y, z, portalParticle ? ALTAR_CENTER_TOLERANCE_SQR : ALTAR_COMPLETION_DUST_TOLERANCE_SQR);
        if (altarPos == null) {
            return false;
        }

        if (portalParticle) {
            refreshAltar(level, altarPos, settings);
            return true;
        }

        // The completion burst spreads green DustParticleOptions around the same altar top-center.
        return activeBeams.containsKey(altarPos);
    }

    public static boolean handleVaultAltarStartSound(double x, double y, double z) {
        ArcaneBeamConfig.VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled || vaultAltarSoundMode() == ArcaneBeamConfig.VaultAltarSoundMode.DEFAULT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
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
        ArcaneBeamConfig.VaultAltarSettings settings = ArcaneBeamConfig.INSTANCE.vaultAltar;
        if (settings == null || !settings.enabled || vaultAltarSoundMode() == ArcaneBeamConfig.VaultAltarSoundMode.DEFAULT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        BlockPos altarPos = findAltarNearSound(level, x, y, z);
        return altarPos != null && activeBeams.containsKey(altarPos);
    }

    private static void refreshAltar(ClientLevel level, BlockPos altarPos, ArcaneBeamConfig.VaultAltarSettings settings) {
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
        int searchRadius = toleranceSqr > ALTAR_CENTER_TOLERANCE_SQR ? 3 : 1;
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

    private static ArcaneBeamConfig.VaultAltarSoundMode vaultAltarSoundMode() {
        ArcaneBeamConfig.VaultAltarSoundMode mode = ArcaneBeamConfig.VaultAltarSoundMode.fromId(ArcaneBeamConfig.INSTANCE.vaultAltar.soundMode);
        return mode == null ? ArcaneBeamConfig.VaultAltarSoundMode.DEFAULT : mode;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            activeBeams.clear();
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<Map.Entry<BlockPos, ActiveAltarBeam>> iterator = activeBeams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ActiveAltarBeam> entry = iterator.next();
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof VaultAltarTileEntity altar)) {
                iterator.remove();
                continue;
            }

            int timer = altar.getInfusionTimer();
            if (timer <= 0 && gameTime - entry.getValue().lastParticleGameTime() > PARTICLE_REFRESH_GRACE_TICKS) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || activeBeams.isEmpty()) {
            return;
        }

        VaultAltarBeamRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeBeams.values());
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
        private static VaultAltarRenderSettings from(ArcaneBeamConfig.VaultAltarSettings settings) {
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
