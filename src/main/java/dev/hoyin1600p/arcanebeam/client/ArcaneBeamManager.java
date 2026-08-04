package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.network.message.AbilityActivityMessage;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import iskallia.vault.skill.ability.effect.ArcaneAbility;
import iskallia.vault.skill.ability.effect.ArcaneRailAbility;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
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
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class ArcaneBeamManager {
    private static final double MAX_PARTICLE_RAY_DISTANCE_SQR = 0.36D;
    private static final double OWNERSHIP_AMBIGUITY_MARGIN = 0.04D;
    private static final double REMOTE_ACQUIRE_DISTANCE = 3.0D;
    private static final double LOCAL_SEQUENCE_ORIGIN_DISTANCE_SQR = 0.36D;
    private static final double LOCAL_SEQUENCE_PATH_DISTANCE_SQR = 1.0D;
    private static final double LOCAL_SEQUENCE_INITIAL_DISTANCE = 1.25D;
    private static final double LOCAL_SEQUENCE_MIN_LOOK_DOT = 0.866D;
    private static final double SOUND_OWNER_DISTANCE_SQR = 1.0D;
    private static final double SOUND_OWNER_AMBIGUITY_MARGIN = 0.25D;
    private static final double ORIGIN_VERTICAL_SMOOTHING_TICKS = 5.0D;
    private static final double POSE_SMOOTHING_MIN_DELTA = 0.08D;
    private static final double POSE_SMOOTHING_MAX_DELTA = 1.1D;
    private static final double MIN_BEAM_RENDER_LENGTH = 0.1D;
    private static final double MIN_LOOK_VECTOR_LENGTH_SQR = 1.0E-6D;
    private static final float FADE_OUT_GRACE_TICKS = 2.0F;
    private static final int LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS = 8;
    private static final int REMOTE_ACTIVITY_GRACE_TICKS = 4;
    private static final Map<UUID, ActiveBeam> activeBeams = new LinkedHashMap<>();
    private static final Map<UUID, SmoothedStartState> smoothedStarts = new LinkedHashMap<>();
    private static final Map<UUID, Vec3> lastLookVectors = new LinkedHashMap<>();
    private static long lastArcaneSeenGameTime = Long.MIN_VALUE;
    private static boolean localArcaneActive = false;
    private static boolean localRailSelected = false;
    private static long localArcaneFirstSeenGameTime = Long.MIN_VALUE;
    private static long lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
    private static long localRailFirstSeenGameTime = Long.MIN_VALUE;
    private static long localRailLatchedUntilGameTime = Long.MIN_VALUE;
    private static LocalParticleSequence localParticleSequence;

    private ArcaneBeamManager() {
    }

    public static boolean handleArcaneParticle(boolean rail, double x, double y, double z,
                                               double xSpeed, double ySpeed, double zSpeed) {
        BeamKind kind = rail ? BeamKind.RAIL : BeamKind.ARCANE;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }

        ParticleOwner owner = findParticleOwner(minecraft.level, new Vec3(x, y, z), kind);
        if (owner == null) {
            return shouldSuppressLocalSequence(kind, new Vec3(x, y, z), minecraft.level.getGameTime());
        }

        if (owner.player.getUUID().equals(minecraft.player.getUUID())) {
            boolean replaceLocal = kind == BeamKind.RAIL
                    ? replacementEnabled(kind) && localRailSelected
                    : getLocalActiveBeam(kind) != null;
            if (replaceLocal) {
                trackLocalSequence(kind, new Vec3(x, y, z), minecraft.level.getGameTime());
            }
            if (kind == BeamKind.RAIL && replacementEnabled(kind) && localRailSelected
                    && owner.alongRay <= REMOTE_ACQUIRE_DISTANCE) {
                latchLocalRail(minecraft.level.getGameTime());
            }
            return replaceLocal;
        }

        ResourceLocation profileId = PlayerBeamAssignments.profileFor(owner.player);
        PlayerBeamProfileManager.ParticleSettings particleSettings =
                PlayerBeamProfileManager.particleSettings(profileId, kind);
        if (particleSettings != null) {
            RemotePlayerParticleManager.addParticle(x, y, z, xSpeed, ySpeed, zSpeed, particleSettings);
            return true;
        }

        ArcaneBeamConfig.BeamSettings settings = PlayerBeamProfileManager.settings(profileId, kind);
        if (settings == null || !settings.enabled) {
            return false;
        }

        ActiveBeam existing = activeBeams.get(owner.player.getUUID());
        boolean continuing = existing != null && existing.kind == kind && existing.settings == settings;
        if (!continuing && owner.alongRay > REMOTE_ACQUIRE_DISTANCE) {
            return false;
        }

        refreshBeam(owner.player.getUUID(), kind, settings, minecraft.level.getGameTime());
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearAllState();
            return;
        }

        clearDisabledReplacementState();

        if (minecraft.player != null) {
            long gameTime = minecraft.level.getGameTime();
            if (localArcaneActive && replacementEnabled(BeamKind.ARCANE)) {
                if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                    localArcaneFirstSeenGameTime = gameTime;
                }
                putLocalBeam(BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime);
                lastArcaneSeenGameTime = gameTime;
            }

            if (isLocalRailLatched(gameTime)) {
                if (localRailFirstSeenGameTime == Long.MIN_VALUE) {
                    localRailFirstSeenGameTime = gameTime;
                }
                putLocalBeam(BeamKind.RAIL, localRailFirstSeenGameTime, gameTime);
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

    private static ParticleOwner findParticleOwner(ClientLevel level, Vec3 particlePosition, BeamKind kind) {
        ParticleOwner best = null;
        double bestDistance = Double.MAX_VALUE;
        double secondBestDistance = Double.MAX_VALUE;
        double maxRange = kind == BeamKind.RAIL ? 160.0D : 64.0D;

        for (AbstractClientPlayer player : level.players()) {
            Vec3 start = serverBeamStart(player, 1.0F);
            Vec3 look = safeLookVector(player.getUUID(), player.getViewVector(1.0F), player.getLookAngle());
            Vec3 toParticle = particlePosition.subtract(start);
            double alongRay = toParticle.dot(look);
            if (alongRay < -0.5D || alongRay > maxRange + 1.0D) {
                continue;
            }

            Vec3 closestPoint = start.add(look.scale(alongRay));
            double distance = closestPoint.distanceToSqr(particlePosition);
            if (distance < bestDistance) {
                secondBestDistance = bestDistance;
                bestDistance = distance;
                best = new ParticleOwner(player, alongRay);
            } else if (distance < secondBestDistance) {
                secondBestDistance = distance;
            }
        }

        if (best == null || bestDistance > MAX_PARTICLE_RAY_DISTANCE_SQR) {
            return null;
        }
        if (secondBestDistance < Double.MAX_VALUE && secondBestDistance - bestDistance < OWNERSHIP_AMBIGUITY_MARGIN) {
            return null;
        }
        return best;
    }

    // Vault emits each ray in order from the caster; latch that sequence so delayed server aim does not leak particles.
    private static boolean shouldSuppressLocalSequence(BeamKind kind, Vec3 particlePosition, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !localReplacementReady(kind)) {
            localParticleSequence = null;
            return false;
        }

        Vec3 origin = serverBeamStart(minecraft.player, 1.0F);
        if (particlePosition.distanceToSqr(origin) <= LOCAL_SEQUENCE_ORIGIN_DISTANCE_SQR) {
            localParticleSequence = new LocalParticleSequence(kind, origin, gameTime);
            if (kind == BeamKind.RAIL) {
                latchLocalRail(gameTime);
            }
            return true;
        }

        LocalParticleSequence sequence = localParticleSequence;
        if (sequence == null || sequence.kind != kind || gameTime - sequence.gameTime > 1L) {
            return false;
        }

        Vec3 fromOrigin = particlePosition.subtract(sequence.origin);
        double distance = fromOrigin.length();
        if (distance < 1.0E-4D) {
            return true;
        }

        if (sequence.direction == null) {
            Vec3 direction = fromOrigin.scale(1.0D / distance);
            Vec3 look = safeLookVector(minecraft.player.getUUID(), minecraft.player.getViewVector(1.0F), minecraft.player.getLookAngle());
            if (distance > LOCAL_SEQUENCE_INITIAL_DISTANCE || direction.dot(look) < LOCAL_SEQUENCE_MIN_LOOK_DOT) {
                return false;
            }
            sequence.direction = direction;
            sequence.farthestDistance = distance;
            return true;
        }

        double alongRay = fromOrigin.dot(sequence.direction);
        double maxRange = kind == BeamKind.RAIL ? 160.0D : 64.0D;
        if (alongRay < -0.5D || alongRay > maxRange + 1.0D) {
            return false;
        }
        Vec3 closestPoint = sequence.origin.add(sequence.direction.scale(alongRay));
        if (closestPoint.distanceToSqr(particlePosition) > LOCAL_SEQUENCE_PATH_DISTANCE_SQR) {
            return false;
        }

        if (distance > sequence.farthestDistance) {
            sequence.direction = fromOrigin.scale(1.0D / distance);
            sequence.farthestDistance = distance;
        }
        return true;
    }

    private static void trackLocalSequence(BeamKind kind, Vec3 particlePosition, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !localReplacementReady(kind)) {
            return;
        }
        Vec3 origin = serverBeamStart(minecraft.player, 1.0F);
        if (particlePosition.distanceToSqr(origin) <= LOCAL_SEQUENCE_ORIGIN_DISTANCE_SQR) {
            localParticleSequence = new LocalParticleSequence(kind, origin, gameTime);
            return;
        }
        shouldSuppressLocalSequence(kind, particlePosition, gameTime);
    }

    private static boolean localReplacementReady(BeamKind kind) {
        return replacementEnabled(kind) && (kind == BeamKind.RAIL ? localRailSelected : localArcaneActive);
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

    public static void syncLocalAbilityState(AbilityKnownOnesMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || message == null || message.getTree() == null) {
            return;
        }

        boolean[] active = new boolean[]{false};
        message.getTree().iterate(ArcaneAbility.class, ability -> active[0] |= ability.isActive());
        localRailSelected = message.getTree().getSelectedAbility()
                .map(ability -> ability instanceof ArcaneRailAbility)
                .orElse(false);
        setLocalArcaneActive(active[0]);
    }

    public static void syncLocalAbilityActivity(AbilityActivityMessage message) {
        if (message == null || !isBaseArcaneAbility(message.getAbility())) {
            return;
        }
        if (message.getActiveFlag() == Ability.ActivityFlag.ACTIVATE_ABILITY) {
            setLocalArcaneActive(true);
        } else if (message.getActiveFlag() == Ability.ActivityFlag.DEACTIVATE_ABILITY) {
            setLocalArcaneActive(false);
        }
    }

    private static void setLocalArcaneActive(boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        if (active) {
            if (!localArcaneActive || localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                localArcaneFirstSeenGameTime = gameTime;
            }
            localArcaneActive = true;
            lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
            lastArcaneSeenGameTime = gameTime;
            if (replacementEnabled(BeamKind.ARCANE)) {
                putLocalBeam(BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime);
            } else {
                removeLocalBeam(BeamKind.ARCANE);
            }
        } else if (localArcaneActive || getLocalActiveBeam(BeamKind.ARCANE) != null) {
            localArcaneActive = false;
            localArcaneFirstSeenGameTime = Long.MIN_VALUE;
            lastLocalArcaneDeactivateGameTime = gameTime;
            removeLocalBeam(BeamKind.ARCANE);
        }
    }

    private static boolean isBaseArcaneAbility(String abilityId) {
        if (abilityId == null) {
            return false;
        }
        String normalized = abilityId.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("arcane") && !normalized.contains("rail") && !normalized.contains("prism");
    }

    public static boolean handleArcaneCastSound(SoundInstance sound) {
        if (!replacementEnabled(BeamKind.ARCANE)
                || soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound) == ArcaneBeamConfig.SoundChoice.DEFAULT
                || !isLocalSoundOwner(sound)) {
            return false;
        }
        return localArcaneActive || isWithinRecentWindow(lastLocalArcaneDeactivateGameTime, LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS);
    }

    public static boolean handleRailCastSound(SoundInstance sound) {
        if (!replacementEnabled(BeamKind.RAIL) || !isVaultRailSound(sound) || !isLocalSoundOwner(sound)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        latchLocalRail(minecraft.level.getGameTime());
        return soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound) != ArcaneBeamConfig.SoundChoice.DEFAULT;
    }

    private static void putLocalBeam(BeamKind kind, long firstSeen, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        UUID casterId = minecraft.player.getUUID();
        activeBeams.put(casterId, new ActiveBeam(casterId, kind, kind.settings(), firstSeen, gameTime));
    }

    private static void removeLocalBeam(BeamKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        UUID casterId = minecraft.player.getUUID();
        ActiveBeam existing = activeBeams.get(casterId);
        if (existing != null && existing.kind == kind) {
            activeBeams.remove(casterId);
            smoothedStarts.remove(casterId);
            lastLookVectors.remove(casterId);
        }
    }

    private static boolean isLocalSoundOwner(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        Vec3 soundPosition = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        AbstractClientPlayer closest = null;
        double bestDistance = Double.MAX_VALUE;
        double secondBestDistance = Double.MAX_VALUE;
        for (AbstractClientPlayer player : minecraft.level.players()) {
            double distance = player.position().distanceToSqr(soundPosition);
            if (distance < bestDistance) {
                secondBestDistance = bestDistance;
                bestDistance = distance;
                closest = player;
            } else if (distance < secondBestDistance) {
                secondBestDistance = distance;
            }
        }
        if (closest == null || bestDistance > SOUND_OWNER_DISTANCE_SQR
                || secondBestDistance < Double.MAX_VALUE && secondBestDistance - bestDistance < SOUND_OWNER_AMBIGUITY_MARGIN) {
            return false;
        }
        return closest.getUUID().equals(minecraft.player.getUUID());
    }

    private static boolean isVaultRailSound(SoundInstance sound) {
        return sound.getPitch() >= 1.75F && sound.getVolume() <= 0.75F;
    }

    private static Vec3 serverBeamStart(LivingEntity caster, float partialTick) {
        return caster.getEyePosition(partialTick).add(0.0D, -0.5D, 0.0D);
    }

    private static void latchLocalRail(long gameTime) {
        if (!replacementEnabled(BeamKind.RAIL)) {
            return;
        }
        ArcaneBeamConfig.BeamSettings settings = BeamKind.RAIL.settings();
        long latchTicks = Math.max(Math.max(settings.lifetimeTicks, settings.fadeOutTicks), 2);
        if (gameTime > localRailLatchedUntilGameTime) {
            localRailFirstSeenGameTime = gameTime;
        }
        localRailLatchedUntilGameTime = gameTime + latchTicks;
        putLocalBeam(BeamKind.RAIL, localRailFirstSeenGameTime, gameTime);
    }

    private static void refreshBeam(UUID casterId, BeamKind kind, ArcaneBeamConfig.BeamSettings settings, long gameTime) {
        ActiveBeam existing = activeBeams.get(casterId);
        long firstSeen = existing != null && existing.kind == kind && existing.settings == settings ? existing.firstSeenGameTime : gameTime;
        activeBeams.put(casterId, new ActiveBeam(casterId, kind, settings, firstSeen, gameTime));
    }

    public static BeamTrace traceBeam(ActiveBeam beam, float partialTick) {
        if (!beam.settings.enabled) {
            return null;
        }
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
        Vec3 hitEnd = resolveCrosshairHit(level, caster, aimStart, look, beam.settings().maxRange);
        Vec3 end = beam.settings().crosshairAim ? crosshairRenderEnd(start, hitEnd) : directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        Vec3 renderDirection = beam.settings().crosshairAim ? end.subtract(start).normalize() : look;
        return new BeamTrace(
                beam.kind,
                beam.settings,
                start,
                end,
                renderDirection,
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
        Vec3 hitEnd = resolveCrosshairHit(level, caster, aimStart, look, settings.maxRange);
        Vec3 end = settings.crosshairAim ? crosshairRenderEnd(start, hitEnd) : directionalRenderEnd(start, look, hitEnd);

        if (end == null) {
            return null;
        }
        Vec3 renderDirection = settings.crosshairAim ? end.subtract(start).normalize() : look;
        return new BeamTrace(kind, settings, start, end, renderDirection, 1.0F, 1.0F, 1.0F);
    }

    public static ActiveBeam getLocalActiveBeam(BeamKind kind) {
        if (!replacementEnabled(kind)) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ActiveBeam beam = activeBeams.get(minecraft.player.getUUID());
        return beam != null && beam.kind == kind ? beam : null;
    }

    public static boolean shouldSuppressAbilityCooldownSound() {
        return replacementEnabled(BeamKind.ARCANE) && isWithinRecentWindow(lastArcaneSeenGameTime, 5L);
    }

    private static boolean replacementEnabled(BeamKind kind) {
        return kind.settings().enabled;
    }

    private static void clearDisabledReplacementState() {
        for (BeamKind kind : BeamKind.values()) {
            if (!replacementEnabled(kind)) {
                clearReplacementState(kind);
            }
        }
    }

    private static void clearReplacementState(BeamKind kind) {
        removeLocalBeam(kind);
        if (localParticleSequence != null && localParticleSequence.kind == kind) {
            localParticleSequence = null;
        }

        if (kind == BeamKind.ARCANE) {
            localArcaneFirstSeenGameTime = Long.MIN_VALUE;
            lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
            lastArcaneSeenGameTime = Long.MIN_VALUE;
        } else {
            localRailFirstSeenGameTime = Long.MIN_VALUE;
            localRailLatchedUntilGameTime = Long.MIN_VALUE;
        }
    }

    private static void clearAllState() {
        activeBeams.clear();
        smoothedStarts.clear();
        lastLookVectors.clear();
        localArcaneActive = false;
        localRailSelected = false;
        localArcaneFirstSeenGameTime = Long.MIN_VALUE;
        lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
        localRailFirstSeenGameTime = Long.MIN_VALUE;
        localRailLatchedUntilGameTime = Long.MIN_VALUE;
        lastArcaneSeenGameTime = Long.MIN_VALUE;
        localParticleSequence = null;
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

    private static Vec3 crosshairRenderEnd(Vec3 start, Vec3 hitEnd) {
        Vec3 direction = hitEnd.subtract(start);
        if (!isUsableVector(direction) || direction.length() < MIN_BEAM_RENDER_LENGTH) {
            return null;
        }
        return hitEnd;
    }

    private static Vec3 resolveCrosshairHit(ClientLevel level, LivingEntity caster, Vec3 aimStart, Vec3 look, double maxRange) {
        Vec3 rangeEnd = aimStart.add(look.scale(maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 closestHit = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();
        double closestDistanceSqr = aimStart.distanceToSqr(closestHit);

        AABB searchBox = boundsBetween(aimStart, rangeEnd).inflate(1.0D);
        for (Entity entity : level.getEntities(caster, searchBox, ArcaneBeamManager::canBeamHitEntity)) {
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

        return closestHit;
    }

    private static boolean canBeamHitEntity(Entity entity) {
        return entity != null && !entity.isSpectator() && entity.isPickable();
    }

    private static AABB boundsBetween(Vec3 first, Vec3 second) {
        return new AABB(
                Math.min(first.x, second.x),
                Math.min(first.y, second.y),
                Math.min(first.z, second.z),
                Math.max(first.x, second.x),
                Math.max(first.y, second.y),
                Math.max(first.z, second.z)
        );
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

    private record ParticleOwner(AbstractClientPlayer player, double alongRay) {
    }

    private static final class LocalParticleSequence {
        private final BeamKind kind;
        private final Vec3 origin;
        private final long gameTime;
        private Vec3 direction;
        private double farthestDistance;

        private LocalParticleSequence(BeamKind kind, Vec3 origin, long gameTime) {
            this.kind = kind;
            this.origin = origin;
            this.gameTime = gameTime;
        }
    }

    public enum BeamKind {
        ARCANE,
        RAIL;

        public ArcaneBeamConfig.BeamSettings settings() {
            return this == RAIL ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
        }
    }

    public record ActiveBeam(UUID casterId, BeamKind kind, ArcaneBeamConfig.BeamSettings settings,
                             long firstSeenGameTime, long lastSeenGameTime) {
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

    public record BeamTrace(BeamKind kind, ArcaneBeamConfig.BeamSettings settings,
                            Vec3 start, Vec3 end, Vec3 direction,
                            float alphaMultiplier, float beamRadiusMultiplier, float glowRadiusMultiplier) {
    }
}
