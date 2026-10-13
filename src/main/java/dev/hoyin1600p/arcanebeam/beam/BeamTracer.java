package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.StartHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Computes where a beam starts and ends on screen: look vector, hand origin, pose smoothing and the crosshair hit. */
final class BeamTracer {
    private BeamTracer() {
    }

    private static final double ORIGIN_VERTICAL_SMOOTHING_TICKS = 5.0D;
    // Pose-change thresholds are blocks per tick so smoothing does not depend on frame rate.
    private static final double POSE_SMOOTHING_MIN_RATE = 0.24D;
    private static final double POSE_SMOOTHING_MAX_RATE = 3.3D;
    private static final double MIN_POSE_FRAME_TICKS = 0.05D;
    private static final double TRACE_CACHE_LOOK_DELTA_SQR = 2.0E-5D;
    private static final double ENTITY_QUERY_SEGMENT = 24.0D;
    private static final double TRACE_CACHE_START_DELTA_SQR = 0.01D;
    private static final double MIN_BEAM_RENDER_LENGTH = 0.1D;
    private static final double MIN_LOOK_VECTOR_LENGTH_SQR = 1.0E-6D;

    private static final Map<UUID, SmoothedStartState> smoothedStarts = new LinkedHashMap<>();
    private static final Map<UUID, Vec3> lastLookVectors = new LinkedHashMap<>();
    private static final Map<UUID, HitCache> hitCaches = new LinkedHashMap<>();

    static BeamTrace traceBeam(ActiveBeam beam, float partialTick) {
        if (!beam.settings().enabled) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }

        Entity entity = level.getPlayerByUUID(beam.casterId());
        if (!(entity instanceof LivingEntity caster)) {
            return null;
        }

        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = visualBeamStart(caster, partialTick, beam.settings());
        Vec3 look = resolveLookVector(beam.casterId(), caster, partialTick);
        Vec3 hitEnd = cachedCrosshairHit(level, caster, aimStart, look, beam.settings().maxRange);
        Vec3 end = beam.settings().crosshairAim ? crosshairRenderEnd(start, hitEnd) : directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        Vec3 renderDirection = beam.settings().crosshairAim ? end.subtract(start).normalize() : look;
        return new BeamTrace(
                beam.kind(),
                beam.settings(),
                start,
                end,
                renderDirection,
                beam.alphaMultiplier(level.getGameTime(), partialTick),
                beam.beamRadiusMultiplier(level.getGameTime(), partialTick),
                beam.glowRadiusMultiplier(level.getGameTime(), partialTick)
        );
    }

    static BeamTrace tracePreviewBeam(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Screen screen = minecraft.screen;
        if (level == null || !(screen instanceof PreviewScreenState configScreen) || configScreen.lightningSelected() || minecraft.player == null) {
            return null;
        }

        LivingEntity caster = minecraft.player;
        BeamKind kind = configScreen.previewKind();
        BeamSettings settings = kind.settings();
        if (ArcaneBeamManager.usesResourcePackParticles(kind, settings)) {
            return null;
        }
        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = visualBeamStart(caster, partialTick, settings);
        Vec3 look = resolveLookVector(caster.getUUID(), caster, partialTick);
        Vec3 hitEnd = resolveCrosshairHit(level, caster, aimStart, look, settings.maxRange);
        Vec3 end = settings.crosshairAim ? crosshairRenderEnd(start, hitEnd) : directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        Vec3 renderDirection = settings.crosshairAim ? end.subtract(start).normalize() : look;
        return new BeamTrace(kind, settings, start, end, renderDirection, 1.0F, 1.0F, 1.0F);
    }

    static Vec3 serverBeamStart(LivingEntity caster, float partialTick) {
        return caster.getEyePosition(partialTick).add(0.0D, -0.5D, 0.0D);
    }

    static Vec3 resolveLookVector(UUID casterId, LivingEntity caster, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(casterId)) {
            return safeLookVector(casterId, minecraft.player.getViewVector(partialTick), minecraft.player.getLookAngle());
        }
        return safeLookVector(casterId, caster.getViewVector(partialTick), caster.getLookAngle());
    }

    static Vec3 rawBeamStart(LivingEntity caster, float partialTick, BeamSettings settings) {
        Vec3 eye = caster.getEyePosition(partialTick);
        Vec3 look = resolveLookVector(caster.getUUID(), caster, partialTick);
        // Derived from yaw, not from up x look: that cross product collapses near vertical aim and its
        // fallback axis made the hand offset pop sideways within about 0.6 degrees of straight up or down.
        Vec3 right = horizontalLeftFromYaw(caster.getViewYRot(partialTick));

        StartHand startHand = StartHand.fromId(settings.startHand);
        HumanoidArm arm = startHand == StartHand.MAIN_HAND ? caster.getMainArm() : caster.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -1.0D : 1.0D;
        return eye.add(right.scale(settings.startOffsetX * side)).add(0.0D, settings.startOffsetY, 0.0D).add(look.scale(settings.startOffsetZ));
    }

    /**
     * The horizontal unit vector equal to {@code up x look} for any non-vertical look at this yaw (the caster's
     * left). It depends on yaw alone, so it is continuous through straight up and straight down.
     */
    static Vec3 horizontalLeftFromYaw(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
    }

    static Vec3 visualBeamStart(LivingEntity caster, float partialTick, BeamSettings settings) {
        Vec3 target = rawBeamStart(caster, partialTick, settings);
        return smoothPoseStart(caster, partialTick, settings, target);
    }

    static Vec3 smoothPoseStart(LivingEntity caster, float partialTick, BeamSettings settings, Vec3 target) {
        UUID casterId = caster.getUUID();
        SmoothedStartState state = smoothedStarts.get(casterId);
        double poseY = poseContributionY(caster, partialTick, settings, target);
        boolean crouching = caster.getPose() == Pose.CROUCHING || caster.isShiftKeyDown();
        if (state == null) {
            SmoothedStartState created = new SmoothedStartState(poseY, poseY, crouching);
            created.lastFrameTime = currentGameTime() + (double) partialTick;
            smoothedStarts.put(casterId, created);
            return target;
        }

        double now = currentGameTime() + (double) partialTick;
        double frameTicks = now - state.lastFrameTime;
        boolean poseChanged = crouching != state.lastCrouching;
        boolean advanced = frameTicks > 1.0E-3D;
        if (advanced || poseChanged) {
            double rate = Math.abs(poseY - state.lastPoseY) / Math.max(frameTicks, MIN_POSE_FRAME_TICKS);
            if (poseChanged || (rate >= POSE_SMOOTHING_MIN_RATE && rate <= POSE_SMOOTHING_MAX_RATE)) {
                state.transitionStartPoseY = state.smoothedPoseY;
                state.transitionStartGameTime = currentGameTime();
                state.smoothingActive = true;
            } else if (rate > POSE_SMOOTHING_MAX_RATE) {
                state.smoothedPoseY = poseY;
                state.smoothingActive = false;
            }
            state.lastPoseY = poseY;
            state.lastCrouching = crouching;
            state.lastFrameTime = now;
        }
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

    static double poseContributionY(LivingEntity caster, float partialTick, BeamSettings settings, Vec3 target) {
        double bodyY = Mth.lerp((double) partialTick, caster.yo, caster.getY());
        double lookOffsetY = resolveLookVector(caster.getUUID(), caster, partialTick).y * settings.startOffsetZ;
        return target.y - bodyY - settings.startOffsetY - lookOffsetY;
    }

    static Vec3 directionalRenderEnd(Vec3 start, Vec3 look, Vec3 hitEnd) {
        // Keep the rendered axis locked to aim; collision can only shorten the beam.
        double length = hitEnd.subtract(start).dot(look);
        if (!Double.isFinite(length) || length < MIN_BEAM_RENDER_LENGTH) {
            return null;
        }
        return start.add(look.scale(length));
    }

    static Vec3 crosshairRenderEnd(Vec3 start, Vec3 hitEnd) {
        Vec3 direction = hitEnd.subtract(start);
        if (!isUsableVector(direction) || direction.length() < MIN_BEAM_RENDER_LENGTH) {
            return null;
        }
        return hitEnd;
    }

    static Vec3 cachedCrosshairHit(ClientLevel level, LivingEntity caster, Vec3 aimStart, Vec3 look, double maxRange) {
        long gameTime = level.getGameTime();
        UUID casterId = caster.getUUID();
        HitCache cache = hitCaches.get(casterId);
        if (cache != null && cache.gameTime == gameTime && cache.maxRange == maxRange
                && cache.look.subtract(look).lengthSqr() <= TRACE_CACHE_LOOK_DELTA_SQR
                && cache.aimStart.distanceToSqr(aimStart) <= TRACE_CACHE_START_DELTA_SQR) {
            return cache.hitEnd;
        }
        Vec3 hitEnd = resolveCrosshairHit(level, caster, aimStart, look, maxRange);
        hitCaches.put(casterId, new HitCache(gameTime, aimStart, look, maxRange, hitEnd));
        return hitEnd;
    }

    static Vec3 resolveCrosshairHit(ClientLevel level, LivingEntity caster, Vec3 aimStart, Vec3 look, double maxRange) {
        Vec3 rangeEnd = aimStart.add(look.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 closestHit = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();
        double closestDistanceSqr = aimStart.distanceToSqr(closestHit);

        // A long diagonal ray has a huge bounding box, so the entity query walks the ray in short segments
        // and never looks past the closest block hit. An entity seen by two segments is harmless: the
        // closest-hit test below does not care about duplicates.
        Vec3 direction = rangeEnd.subtract(aimStart);
        double length = direction.length();
        double reach = Math.min(length, Math.sqrt(closestDistanceSqr));
        int segments = (int) Math.max(1.0D, Math.ceil(reach / ENTITY_QUERY_SEGMENT));
        for (int segment = 0; segment < segments; segment++) {
            Vec3 segmentStart = length < 1.0E-6D ? aimStart : aimStart.add(direction.scale(reach * segment / segments / length));
            Vec3 segmentEnd = length < 1.0E-6D ? aimStart : aimStart.add(direction.scale(reach * (segment + 1) / segments / length));
            AABB searchBox = boundsBetween(segmentStart, segmentEnd).inflate(1.0D);
            for (Entity entity : level.getEntities(caster, searchBox, BeamTracer::canBeamHitEntity)) {
                AABB targetBox = entity.getBoundingBox().inflate(entity.getPickRadius());
                Optional<Vec3> hit = targetBox.clip(aimStart, rangeEnd);
                if (hit.isEmpty()) {
                    continue;
                }

                double distanceSqr = aimStart.distanceToSqr(hit.get());
                if (distanceSqr < closestDistanceSqr) {
                    closestHit = hit.get();
                    closestDistanceSqr = distanceSqr;
                }
            }
        }

        return closestHit;
    }

    static boolean canBeamHitEntity(Entity entity) {
        return entity != null && !entity.isSpectator() && entity.isPickable();
    }

    static AABB boundsBetween(Vec3 first, Vec3 second) {
        return new AABB(
                Math.min(first.x, second.x),
                Math.min(first.y, second.y),
                Math.min(first.z, second.z),
                Math.max(first.x, second.x),
                Math.max(first.y, second.y),
                Math.max(first.z, second.z)
        );
    }

    static Vec3 safeLookVector(UUID casterId, Vec3 preferred, Vec3 fallback) {
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

    static Vec3 rememberLookVector(UUID casterId, Vec3 vector) {
        Vec3 normalized = vector.normalize();
        lastLookVectors.put(casterId, normalized);
        return normalized;
    }

    static boolean isUsableVector(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z)
                && vector.lengthSqr() > MIN_LOOK_VECTOR_LENGTH_SQR;
    }

    static long currentGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    private static final class SmoothedStartState {
        private double smoothedPoseY;
        private double lastPoseY;
        private double transitionStartPoseY;
        private double transitionTargetPoseY;
        private long transitionStartGameTime;
        private double lastFrameTime;
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

    private record HitCache(long gameTime, Vec3 aimStart, Vec3 look, double maxRange, Vec3 hitEnd) {
    }

    /** Drops every per-caster cache for a beam that has ended. */
    static void forget(UUID casterId) {
        smoothedStarts.remove(casterId);
        lastLookVectors.remove(casterId);
        hitCaches.remove(casterId);
    }

    static void clear() {
        smoothedStarts.clear();
        lastLookVectors.clear();
        hitCaches.clear();
    }
}
