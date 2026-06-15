package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.init.ModKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class ArcaneBeamManager {
    private static final double MAX_PARTICLE_RAY_DISTANCE_SQR = 0.25D;
    private static final double LOCAL_FALLBACK_ACQUIRE_DISTANCE = 4.0D;
    private static final double REMOTE_OWNERSHIP_ACQUIRE_DISTANCE = 2.5D;
    private static final double LOCAL_EXCLUSION_RAY_DISTANCE_SQR = 0.25D;
    private static final double ORIGIN_VERTICAL_SMOOTHING_TICKS = 5.0D;
    private static final double POSE_SMOOTHING_MIN_DELTA = 0.08D;
    private static final double POSE_SMOOTHING_MAX_DELTA = 1.1D;
    private static final double MIN_BEAM_RENDER_LENGTH = 0.1D;
    private static final double MIN_LOOK_VECTOR_LENGTH_SQR = 1.0E-6D;
    private static final float FADE_OUT_GRACE_TICKS = 2.0F;
    private static final int LOCAL_ARCANE_LATCH_TICKS = 12;
    private static final int LOCAL_ARCANE_PRIME_TICKS = 20;
    private static final int LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS = 8;
    private static final int REMOTE_ACTIVITY_GRACE_TICKS = 4;
    private static final ResourceLocation ARCANE = new ResourceLocation("the_vault", "arcane");
    private static final ResourceLocation ARCANE_RAIL = new ResourceLocation("the_vault", "arcane_rail");
    private static final Map<UUID, ActiveBeam> activeBeams = new LinkedHashMap<>();
    private static final Map<UUID, SmoothedStartState> smoothedStarts = new LinkedHashMap<>();
    private static final Map<UUID, Vec3> lastLookVectors = new LinkedHashMap<>();
    private static long lastArcaneSeenGameTime = Long.MIN_VALUE;
    private static boolean localArcanePacketActive = false;
    private static boolean localArcaneKeyHeld = false;
    private static long localArcaneFirstSeenGameTime = Long.MIN_VALUE;
    private static long lastLocalArcaneSignalGameTime = Long.MIN_VALUE;
    private static long lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
    private static long localArcanePrimedUntilGameTime = Long.MIN_VALUE;
    private static long localRailFirstSeenGameTime = Long.MIN_VALUE;
    private static long localRailLatchedUntilGameTime = Long.MIN_VALUE;

    private ArcaneBeamManager() {
    }

    public static boolean captureParticle(ParticleOptions particle, double x, double y, double z) {
        ResourceLocation key = Registry.PARTICLE_TYPE.getKey(particle.getType());
        BeamKind kind;
        if (ARCANE.equals(key)) {
            kind = BeamKind.ARCANE;
        } else if (ARCANE_RAIL.equals(key)) {
            kind = BeamKind.RAIL;
        } else {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Vec3 particlePosition = new Vec3(x, y, z);
            if (tryRefreshLocalFromOriginParticle(minecraft, particlePosition, kind)) {
                return true;
            }
            if (isExcludedByLocalBeam(minecraft, particlePosition, kind)) {
                return true;
            }

            AbstractClientPlayer caster = findCaster(minecraft.level, particlePosition, kind);
            if (caster != null) {
                long gameTime = minecraft.level.getGameTime();
                refreshBeam(caster.getUUID(), kind, gameTime);
                if (kind == BeamKind.ARCANE) {
                    lastArcaneSeenGameTime = gameTime;
                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            activeBeams.clear();
            smoothedStarts.clear();
            lastLookVectors.clear();
            localArcanePacketActive = false;
            localArcaneKeyHeld = false;
            localArcaneFirstSeenGameTime = Long.MIN_VALUE;
            lastLocalArcaneSignalGameTime = Long.MIN_VALUE;
            lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
            localArcanePrimedUntilGameTime = Long.MIN_VALUE;
            localRailFirstSeenGameTime = Long.MIN_VALUE;
            localRailLatchedUntilGameTime = Long.MIN_VALUE;
            lastArcaneSeenGameTime = Long.MIN_VALUE;
            return;
        }

        if (minecraft.player != null) {
            long gameTime = minecraft.level.getGameTime();
            UUID localPlayerId = minecraft.player.getUUID();
            syncLocalArcaneKeyState(localPlayerId, gameTime);

            if (isLocalArcaneMaintained(gameTime) || isLocalArcanePrimed(gameTime)) {
                if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                    localArcaneFirstSeenGameTime = gameTime;
                }
                activeBeams.put(localPlayerId, new ActiveBeam(localPlayerId, BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime));
            }

            if (isLocalRailLatched(gameTime)) {
                if (localRailFirstSeenGameTime == Long.MIN_VALUE) {
                    localRailFirstSeenGameTime = gameTime;
                }
                activeBeams.put(localPlayerId, new ActiveBeam(localPlayerId, BeamKind.RAIL, localRailFirstSeenGameTime, gameTime));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        removeExpiredBeams(gameTime);
        if (!activeBeams.isEmpty()) {
            ArcaneBeamRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeBeams.values());
        }

        BeamTrace previewTrace = tracePreviewBeam(event.getPartialTick());
        if (previewTrace != null) {
            ArcaneBeamRenderer.renderPreview(event.getPoseStack(), event.getCamera().getPosition(), previewTrace);
        }
    }

    private static AbstractClientPlayer findCaster(ClientLevel level, Vec3 particlePosition, BeamKind kind) {
        AbstractClientPlayer bestPlayer = null;
        double bestDistance = Double.MAX_VALUE;
        double maxRange = kind.settings().maxRange;
        Minecraft minecraft = Minecraft.getInstance();

        for (AbstractClientPlayer player : level.players()) {
            if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())) {
                continue;
            }
            Vec3 start = rawBeamStart(player, 1.0F, kind.settings());
            Vec3 look = player.getLookAngle().normalize();
            Vec3 toParticle = particlePosition.subtract(start);
            double alongRay = toParticle.dot(look);
            if (alongRay < -0.5D || alongRay > maxRange + 1.0D) {
                continue;
            }

            ActiveBeam existing = activeBeams.get(player.getUUID());
            boolean existingSameKind = existing != null && existing.kind == kind;
            if (!existingSameKind && alongRay > REMOTE_OWNERSHIP_ACQUIRE_DISTANCE) {
                continue;
            }

            Vec3 closestPoint = start.add(look.scale(alongRay));
            double distance = closestPoint.distanceToSqr(particlePosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = player;
            }
        }

        return bestDistance <= MAX_PARTICLE_RAY_DISTANCE_SQR ? bestPlayer : null;
    }

    private static boolean tryRefreshLocalFromOriginParticle(Minecraft minecraft, Vec3 particlePosition, BeamKind kind) {
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        ArcaneBeamConfig.BeamSettings settings = kind.settings();
        Vec3 start = rawBeamStart(minecraft.player, 1.0F, settings);
        Vec3 look = resolveLookVector(minecraft.player.getUUID(), minecraft.player, 1.0F);
        Vec3 toParticle = particlePosition.subtract(start);
        double alongRay = toParticle.dot(look);
        if (alongRay < -0.5D || alongRay > LOCAL_FALLBACK_ACQUIRE_DISTANCE) {
            return false;
        }

        Vec3 closestPoint = start.add(look.scale(alongRay));
        if (closestPoint.distanceToSqr(particlePosition) > MAX_PARTICLE_RAY_DISTANCE_SQR) {
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        if (kind == BeamKind.ARCANE) {
            if (!isLocalArcaneCastKeyHeld()) {
                if (localArcanePacketActive || isLocalArcanePrimed(gameTime)) {
                    if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                        localArcaneFirstSeenGameTime = gameTime;
                    }
                    lastLocalArcaneSignalGameTime = gameTime;
                    lastArcaneSeenGameTime = gameTime;
                    refreshLocalArcaneBeam(minecraft.player.getUUID(), gameTime);
                } else {
                    lastLocalArcaneSignalGameTime = Long.MIN_VALUE;
                    lastLocalArcaneDeactivateGameTime = gameTime;
                    forceLocalArcaneFadeOutStart(minecraft.player.getUUID(), gameTime);
                }
                return true;
            }
            if (!localArcanePacketActive && isWithinRecentWindow(lastLocalArcaneDeactivateGameTime, LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS)) {
                return true;
            }
            if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                localArcaneFirstSeenGameTime = gameTime;
            }
            lastLocalArcaneSignalGameTime = gameTime;
            lastArcaneSeenGameTime = gameTime;
            refreshLocalArcaneBeam(minecraft.player.getUUID(), gameTime);
            return true;
        }
        latchLocalRail(minecraft.player.getUUID(), gameTime);
        return true;
    }

    private static boolean isExcludedByLocalBeam(Minecraft minecraft, Vec3 particlePosition, BeamKind kind) {
        if (minecraft.player == null) {
            return false;
        }

        ActiveBeam localBeam = getLocalActiveBeam(kind);
        if (localBeam == null) {
            return false;
        }

        ArcaneBeamConfig.BeamSettings settings = kind.settings();
        Vec3 start = rawBeamStart(minecraft.player, 1.0F, settings);
        Vec3 look = resolveLookVector(minecraft.player.getUUID(), minecraft.player, 1.0F);
        Vec3 toParticle = particlePosition.subtract(start);
        double alongRay = toParticle.dot(look);
        if (alongRay < -0.5D || alongRay > settings.maxRange + 1.0D) {
            return false;
        }

        Vec3 closestPoint = start.add(look.scale(alongRay));
        return closestPoint.distanceToSqr(particlePosition) <= LOCAL_EXCLUSION_RAY_DISTANCE_SQR;
    }

    private static void removeExpiredBeams(long gameTime) {
        Iterator<ActiveBeam> iterator = activeBeams.values().iterator();
        while (iterator.hasNext()) {
            ActiveBeam beam = iterator.next();
            if (gameTime - beam.lastSeenGameTime > beam.expireAfterTicks()) {
                smoothedStarts.remove(beam.casterId);
                lastLookVectors.remove(beam.casterId);
                iterator.remove();
            }
        }
    }

    public static void observeAbilityActivity(String abilityId, String activeFlagName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || abilityId == null || activeFlagName == null) {
            return;
        }

        BeamKind kind = classifyAbility(abilityId);
        if (kind == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (kind == BeamKind.ARCANE) {
            if ("DEACTIVATE_ABILITY".equals(activeFlagName)) {
                localArcanePacketActive = false;
                lastLocalArcaneSignalGameTime = Long.MIN_VALUE;
                lastLocalArcaneDeactivateGameTime = gameTime;
                localArcanePrimedUntilGameTime = Long.MIN_VALUE;
                forceLocalArcaneFadeOutStart(minecraft.player.getUUID(), gameTime);
            } else {
                if (!isLocalArcaneCastKeyHeld() && !isLocalArcanePrimed(gameTime)) {
                    return;
                }
                if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                    localArcaneFirstSeenGameTime = gameTime;
                }
                localArcanePacketActive = true;
                localArcanePrimedUntilGameTime = Long.MIN_VALUE;
                lastLocalArcaneSignalGameTime = gameTime;
                lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
                refreshLocalArcaneBeam(minecraft.player.getUUID(), gameTime);
                lastArcaneSeenGameTime = gameTime;
            }
            return;
        }

        if (!"DEACTIVATE_ABILITY".equals(activeFlagName)) {
            latchLocalRail(minecraft.player.getUUID(), gameTime);
        }
    }

    private static void refreshLocalArcaneBeam(UUID casterId, long gameTime) {
        if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
            localArcaneFirstSeenGameTime = gameTime;
        }
        activeBeams.put(casterId, new ActiveBeam(casterId, BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime));
    }

    private static void forceLocalArcaneFadeOutStart(UUID casterId, long gameTime) {
        ActiveBeam existing = activeBeams.get(casterId);
        if (existing == null || existing.kind != BeamKind.ARCANE) {
            return;
        }
        long adjustedLastSeen = gameTime - (long) Math.ceil(FADE_OUT_GRACE_TICKS);
        activeBeams.put(casterId, new ActiveBeam(casterId, BeamKind.ARCANE, existing.firstSeenGameTime, adjustedLastSeen));
    }

    private static void syncLocalArcaneKeyState(UUID casterId, long gameTime) {
        boolean heldNow = isLocalArcaneCastKeyHeld();
        if (heldNow == localArcaneKeyHeld) {
            return;
        }

        localArcaneKeyHeld = heldNow;
        if (heldNow) {
            primeLocalArcane(casterId, gameTime);
            return;
        }

        stopLocalArcaneImmediately(casterId, gameTime);
    }

    private static void primeLocalArcane(UUID casterId, long gameTime) {
        if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
            localArcaneFirstSeenGameTime = gameTime;
        }
        // Keep the optimistic local beam alive long enough for the server ability packet or Vault particles to confirm it.
        localArcanePrimedUntilGameTime = gameTime + LOCAL_ARCANE_PRIME_TICKS;
        lastArcaneSeenGameTime = gameTime;
        refreshLocalArcaneBeam(casterId, gameTime);
    }

    private static void stopLocalArcaneImmediately(UUID casterId, long gameTime) {
        localArcanePacketActive = false;
        localArcaneFirstSeenGameTime = Long.MIN_VALUE;
        lastLocalArcaneSignalGameTime = Long.MIN_VALUE;
        lastLocalArcaneDeactivateGameTime = gameTime;
        localArcanePrimedUntilGameTime = Long.MIN_VALUE;

        ActiveBeam existing = activeBeams.get(casterId);
        if (existing != null && existing.kind == BeamKind.ARCANE) {
            activeBeams.remove(casterId);
            smoothedStarts.remove(casterId);
        }
    }

    private static boolean isLocalArcaneCastKeyHeld() {
        if (ModKeybinds.abilityKey != null && ModKeybinds.abilityKey.isDown()) {
            return true;
        }
        if (ModKeybinds.abilityQuickfireKey != null) {
            for (Map.Entry<String, net.minecraft.client.KeyMapping> entry : ModKeybinds.abilityQuickfireKey.entrySet()) {
                if (classifyAbility(entry.getKey()) == BeamKind.ARCANE && entry.getValue() != null && entry.getValue().isDown()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void latchLocalRail(UUID casterId, long gameTime) {
        ArcaneBeamConfig.BeamSettings settings = BeamKind.RAIL.settings();
        long latchTicks = Math.max(Math.max(settings.lifetimeTicks, settings.fadeOutTicks), 2);
        if (gameTime > localRailLatchedUntilGameTime) {
            localRailFirstSeenGameTime = gameTime;
        }
        localRailLatchedUntilGameTime = gameTime + latchTicks;
        activeBeams.put(casterId, new ActiveBeam(casterId, BeamKind.RAIL, localRailFirstSeenGameTime, gameTime));
    }

    private static void refreshBeam(UUID casterId, BeamKind kind, long gameTime) {
        ActiveBeam existing = activeBeams.get(casterId);
        long firstSeen = existing != null && existing.kind == kind ? existing.firstSeenGameTime : gameTime;
        activeBeams.put(casterId, new ActiveBeam(casterId, kind, firstSeen, gameTime));
    }

    private static BeamKind classifyAbility(String abilityId) {
        String normalized = abilityId.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("arcane_rail")) {
            return BeamKind.RAIL;
        }
        if (normalized.contains("arcane")) {
            return BeamKind.ARCANE;
        }
        return null;
    }

    public static BeamTrace traceBeam(ActiveBeam beam, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }

        Entity entity = level.getPlayerByUUID(beam.casterId);
        if (!(entity instanceof LivingEntity caster)) {
            return null;
        }

        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = visualBeamStart(caster, partialTick, beam.settings());
        Vec3 look = resolveLookVector(beam.casterId, caster, partialTick);
        double maxRange = beam.settings().maxRange;
        Vec3 rangeEnd = aimStart.add(look.scale(maxRange));

        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 hitEnd = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();
        Vec3 end = directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        return new BeamTrace(
                beam.kind,
                start,
                end,
                look,
                beam.alphaMultiplier(level.getGameTime(), partialTick),
                beam.beamRadiusMultiplier(level.getGameTime(), partialTick),
                beam.glowRadiusMultiplier(level.getGameTime(), partialTick)
        );
    }

    public static BeamTrace tracePreviewBeam(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Screen screen = minecraft.screen;
        if (level == null || !(screen instanceof ArcaneBeamConfigScreen configScreen) || configScreen.lightningSelected() || minecraft.player == null) {
            return null;
        }

        LivingEntity caster = minecraft.player;
        BeamKind kind = configScreen.previewKind();
        ArcaneBeamConfig.BeamSettings settings = kind.settings();
        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = visualBeamStart(caster, partialTick, settings);
        Vec3 look = resolveLookVector(caster.getUUID(), caster, partialTick);
        Vec3 rangeEnd = aimStart.add(look.scale(settings.maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 hitEnd = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();
        Vec3 end = directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        return new BeamTrace(kind, start, end, look, 1.0F, 1.0F, 1.0F);
    }

    public static ActiveBeam getLocalActiveBeam(BeamKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ActiveBeam beam = activeBeams.get(minecraft.player.getUUID());
        return beam != null && beam.kind == kind ? beam : null;
    }

    public static boolean shouldSuppressAbilityCooldownSound() {
        return isWithinRecentWindow(lastArcaneSeenGameTime, 5L);
    }

    public static boolean shouldSuppressArcaneCastSound() {
        if (soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound) == ArcaneBeamConfig.SoundChoice.DEFAULT) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null && isLocalArcaneCastKeyHeld()) {
            localArcaneKeyHeld = true;
            primeLocalArcane(minecraft.player.getUUID(), minecraft.level.getGameTime());
            return true;
        }

        if (isWithinRecentWindow(lastLocalArcaneDeactivateGameTime, LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS)) {
            return true;
        }

        return getLocalActiveBeam(BeamKind.ARCANE) != null;
    }

    public static boolean shouldSuppressRailCastSound() {
        return getLocalActiveBeam(BeamKind.RAIL) != null
                && soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound) != ArcaneBeamConfig.SoundChoice.DEFAULT;
    }

    private static boolean isWithinRecentWindow(long lastSeenGameTime, long windowTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        long gameTime = minecraft.level.getGameTime();
        return gameTime - lastSeenGameTime >= 0L && gameTime - lastSeenGameTime <= windowTicks;
    }

    private static ArcaneBeamConfig.SoundChoice soundChoice(String id) {
        ArcaneBeamConfig.SoundChoice choice = ArcaneBeamConfig.SoundChoice.fromId(id);
        return choice == null ? ArcaneBeamConfig.SoundChoice.DEFAULT : choice;
    }

    private static boolean isLocalArcaneMaintained(long gameTime) {
        return gameTime - lastLocalArcaneSignalGameTime >= 0L
                && gameTime - lastLocalArcaneSignalGameTime <= LOCAL_ARCANE_LATCH_TICKS;
    }

    private static boolean isLocalArcanePrimed(long gameTime) {
        return gameTime <= localArcanePrimedUntilGameTime;
    }

    private static boolean isLocalRailLatched(long gameTime) {
        return gameTime <= localRailLatchedUntilGameTime;
    }

    private static Vec3 resolveLookVector(UUID casterId, LivingEntity caster, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(casterId)) {
            return safeLookVector(casterId, minecraft.player.getViewVector(partialTick), minecraft.player.getLookAngle());
        }
        return safeLookVector(casterId, caster.getViewVector(partialTick), caster.getLookAngle());
    }

    private static Vec3 rawBeamStart(LivingEntity caster, float partialTick, ArcaneBeamConfig.BeamSettings settings) {
        Vec3 eye = caster.getEyePosition(partialTick);
        Vec3 look = resolveLookVector(caster.getUUID(), caster, partialTick);
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
        if (right.lengthSqr() < 1.0E-4D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        ArcaneBeamConfig.StartHand startHand = ArcaneBeamConfig.StartHand.fromId(settings.startHand);
        HumanoidArm arm = startHand == ArcaneBeamConfig.StartHand.MAIN_HAND ? caster.getMainArm() : caster.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -1.0D : 1.0D;
        return eye.add(right.scale(settings.startOffsetX * side)).add(0.0D, settings.startOffsetY, 0.0D).add(look.scale(settings.startOffsetZ));
    }

    private static Vec3 visualBeamStart(LivingEntity caster, float partialTick, ArcaneBeamConfig.BeamSettings settings) {
        Vec3 target = rawBeamStart(caster, partialTick, settings);
        return smoothPoseStart(caster, partialTick, settings, target);
    }

    private static Vec3 smoothPoseStart(LivingEntity caster, float partialTick, ArcaneBeamConfig.BeamSettings settings, Vec3 target) {
        UUID casterId = caster.getUUID();
        SmoothedStartState state = smoothedStarts.get(casterId);
        double poseY = poseContributionY(caster, partialTick, settings, target);
        boolean crouching = caster.getPose() == Pose.CROUCHING || caster.isShiftKeyDown();
        if (state == null) {
            smoothedStarts.put(casterId, new SmoothedStartState(poseY, poseY, crouching));
            return target;
        }

        double frameDelta = Math.abs(poseY - state.lastPoseY);
        boolean poseChanged = crouching != state.lastCrouching;
        if (poseChanged || (frameDelta >= POSE_SMOOTHING_MIN_DELTA && frameDelta <= POSE_SMOOTHING_MAX_DELTA)) {
            state.transitionStartPoseY = state.smoothedPoseY;
            state.transitionStartGameTime = currentGameTime();
            state.smoothingActive = true;
        } else if (frameDelta > POSE_SMOOTHING_MAX_DELTA) {
            state.smoothedPoseY = poseY;
            state.smoothingActive = false;
        }

        state.lastPoseY = poseY;
        state.lastCrouching = crouching;
        if (state.smoothingActive) {
            state.transitionTargetPoseY = poseY;
            double elapsedTicks = Math.max(0.0D, currentGameTime() - state.transitionStartGameTime + partialTick);
            double progress = Math.min(1.0D, elapsedTicks / ORIGIN_VERTICAL_SMOOTHING_TICKS);
            state.smoothedPoseY = Mth.lerp(progress, state.transitionStartPoseY, state.transitionTargetPoseY);
            if (progress >= 1.0D || Math.abs(poseY - state.smoothedPoseY) < 0.005D) {
                state.smoothedPoseY = poseY;
                state.smoothingActive = false;
            }
            return new Vec3(target.x, target.y + state.smoothedPoseY - poseY, target.z);
        }

        state.smoothedPoseY = poseY;
        return target;
    }

    private static double poseContributionY(LivingEntity caster, float partialTick, ArcaneBeamConfig.BeamSettings settings, Vec3 target) {
        double bodyY = Mth.lerp((double) partialTick, caster.yo, caster.getY());
        double lookOffsetY = resolveLookVector(caster.getUUID(), caster, partialTick).y * settings.startOffsetZ;
        return target.y - bodyY - settings.startOffsetY - lookOffsetY;
    }

    private static Vec3 directionalRenderEnd(Vec3 start, Vec3 look, Vec3 hitEnd) {
        // Keep the rendered axis locked to aim; collision can only shorten the beam.
        double length = hitEnd.subtract(start).dot(look);
        if (!Double.isFinite(length) || length < MIN_BEAM_RENDER_LENGTH) {
            return null;
        }
        return start.add(look.scale(length));
    }

    private static Vec3 safeLookVector(UUID casterId, Vec3 preferred, Vec3 fallback) {
        if (isUsableVector(preferred)) {
            return rememberLookVector(casterId, preferred);
        }
        if (isUsableVector(fallback)) {
            return rememberLookVector(casterId, fallback);
        }
        Vec3 cached = lastLookVectors.get(casterId);
        if (isUsableVector(cached)) {
            return cached;
        }
        return rememberLookVector(casterId, new Vec3(0.0D, 0.0D, 1.0D));
    }

    private static Vec3 rememberLookVector(UUID casterId, Vec3 vector) {
        Vec3 normalized = vector.normalize();
        lastLookVectors.put(casterId, normalized);
        return normalized;
    }

    private static boolean isUsableVector(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z)
                && vector.lengthSqr() > MIN_LOOK_VECTOR_LENGTH_SQR;
    }

    private static long currentGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    private static final class SmoothedStartState {
        private double smoothedPoseY;
        private double lastPoseY;
        private double transitionStartPoseY;
        private double transitionTargetPoseY;
        private long transitionStartGameTime;
        private boolean lastCrouching;
        private boolean smoothingActive;

        private SmoothedStartState(double smoothedPoseY, double lastPoseY, boolean lastCrouching) {
            this.smoothedPoseY = smoothedPoseY;
            this.lastPoseY = lastPoseY;
            this.transitionStartPoseY = smoothedPoseY;
            this.transitionTargetPoseY = smoothedPoseY;
            this.lastCrouching = lastCrouching;
        }
    }

    public enum BeamKind {
        ARCANE,
        RAIL;

        public ArcaneBeamConfig.BeamSettings settings() {
            return this == RAIL ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
        }
    }

    public record ActiveBeam(UUID casterId, BeamKind kind, long firstSeenGameTime, long lastSeenGameTime) {
        public ArcaneBeamConfig.BeamSettings settings() {
            return kind.settings();
        }

        public long expireAfterTicks() {
            return Math.max(Math.max(settings().lifetimeTicks, settings().fadeOutTicks), REMOTE_ACTIVITY_GRACE_TICKS);
        }

        public float alphaMultiplier(long gameTime, float partialTick) {
            float alpha = 1.0F;
            float age = Math.max(0.0F, gameTime - firstSeenGameTime + partialTick);
            float sinceLastSeen = fadeOutAge(gameTime, partialTick);

            if (fadeInStyle() == ArcaneBeamConfig.FadeInStyle.FADE && settings().fadeInTicks > 0) {
                alpha *= Math.min(1.0F, age / settings().fadeInTicks);
            }
            if (fadeOutStyle() == ArcaneBeamConfig.FadeOutStyle.FADE && settings().fadeOutTicks > 0 && sinceLastSeen > 0.0F) {
                alpha *= Math.max(0.0F, 1.0F - (sinceLastSeen / settings().fadeOutTicks));
            }
            return alpha;
        }

        public float beamRadiusMultiplier(long gameTime, float partialTick) {
            float radius = 1.0F;
            float age = Math.max(0.0F, gameTime - firstSeenGameTime + partialTick);
            float sinceLastSeen = fadeOutAge(gameTime, partialTick);

            if (fadeInStyle() == ArcaneBeamConfig.FadeInStyle.GROW && settings().fadeInTicks > 0) {
                radius *= Math.min(1.0F, age / settings().fadeInTicks);
            }
            if (fadeOutStyle() == ArcaneBeamConfig.FadeOutStyle.SHRINK && settings().fadeOutTicks > 0 && sinceLastSeen > 0.0F) {
                radius *= Math.max(0.0F, 1.0F - (sinceLastSeen / settings().fadeOutTicks));
            }
            return radius;
        }

        public float glowRadiusMultiplier(long gameTime, float partialTick) {
            return beamRadiusMultiplier(gameTime, partialTick);
        }

        private ArcaneBeamConfig.FadeInStyle fadeInStyle() {
            ArcaneBeamConfig.FadeInStyle style = ArcaneBeamConfig.FadeInStyle.fromId(settings().fadeInStyle);
            return style == null ? ArcaneBeamConfig.FadeInStyle.FADE : style;
        }

        private ArcaneBeamConfig.FadeOutStyle fadeOutStyle() {
            ArcaneBeamConfig.FadeOutStyle style = ArcaneBeamConfig.FadeOutStyle.fromId(settings().fadeOutStyle);
            return style == null ? ArcaneBeamConfig.FadeOutStyle.FADE : style;
        }

        private float fadeOutAge(long gameTime, float partialTick) {
            return Math.max(0.0F, gameTime - lastSeenGameTime - FADE_OUT_GRACE_TICKS + partialTick);
        }
    }

    public record BeamTrace(BeamKind kind, Vec3 start, Vec3 end, Vec3 direction, float alphaMultiplier, float beamRadiusMultiplier, float glowRadiusMultiplier) {
        public ArcaneBeamConfig.BeamSettings settings() {
            return kind.settings();
        }
    }
}
