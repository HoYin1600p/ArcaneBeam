package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class BlastWaveVisualManager {
    private static final ResourceLocation BLAST_WAVE_EFFECT = new ResourceLocation("the_vault", "smite_blast_wave");
    private static final float FALLBACK_CIRCLE_RADIUS = 5.0F;
    private static final long CIRCLE_LINGER_TICKS = 60L;
    private static final long SOUND_MATCH_TICKS = 4L;
    private static final long ACTIVATION_GRACE_TICKS = 8L;
    private static final double SOUND_MATCH_DISTANCE_SQR = 9.0D;
    private static final Map<Integer, ActiveBlastWaveMissile> ACTIVE_MISSILES = new LinkedHashMap<>();
    private static final List<PendingStrikeSound> PENDING_SOUNDS = new ArrayList<>();
    private static int nextMissileId = 1;
    private static Vec3 lastPulseOrigin;
    private static float lastPulseRadius;
    private static long lastPulseGameTime = Long.MIN_VALUE;
    private static long localCircleUntilGameTime = Long.MIN_VALUE;
    private static long lastActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastActivationSoundPosition;
    private static long localActivationUntilGameTime = Long.MIN_VALUE;

    private BlastWaveVisualManager() {
    }

    public static boolean handlePulse(Vec3 packetOrigin, float radius, int[] targetIds) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.BlastWaveSettings settings = ArcaneBeamConfig.INSTANCE.blastWave;
        if (level == null || packetOrigin == null || settings == null || !settings.enabled) {
            return false;
        }

        long now = level.getGameTime();
        Player source = nearestPlayer(level, packetOrigin);
        Vec3 missileOrigin = configuredOrigin(source, packetOrigin, settings);
        List<LivingEntity> targets = resolveTargets(level, targetIds, packetOrigin, settings.maxMissiles);
        StormArrowVisualManager.StormArrowRenderSettings renderSettings = StormArrowVisualManager.StormArrowRenderSettings.from(settings);
        for (LivingEntity target : targets) {
            Vec3 targetPoint = target.position().add(0.0D, Math.max(0.30D, target.getBbHeight() * 0.48D), 0.0D);
            Vec3 lateral = lateralOffset(missileOrigin, targetPoint, target.getId(), settings.lateralSpread);
            Vec3 controlOne = missileOrigin.add(lateral.scale(0.35D)).add(0.0D, settings.originHeight * 1.35D, 0.0D);
            Vec3 controlTwo = new Vec3(
                    Mth.lerp(0.48D, missileOrigin.x, targetPoint.x) + lateral.x * 1.33D,
                    Math.max(missileOrigin.y, targetPoint.y) + settings.originHeight,
                    Mth.lerp(0.48D, missileOrigin.z, targetPoint.z) + lateral.z * 1.33D
            );
            ACTIVE_MISSILES.put(nextMissileId++, new ActiveBlastWaveMissile(
                    target.getId(), missileOrigin, targetPoint, controlOne, controlTwo, now, settings.missileScale, renderSettings
            ));
            if (nextMissileId == Integer.MAX_VALUE) {
                nextMissileId = 1;
            }
        }

        lastPulseOrigin = packetOrigin;
        lastPulseRadius = radius;
        lastPulseGameTime = now;
        if (source == minecraft.player) {
            localCircleUntilGameTime = now + CIRCLE_LINGER_TICKS;
        }
        queueStrikeSounds(minecraft, missileOrigin, targets.size(), now, settings);
        return true;
    }

    public static void observeAbilityActivity(String abilityId, String activeFlagName) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !isBlastWaveAbility(abilityId) || activeFlagName == null) {
            return;
        }
        if ("ACTIVATE_ABILITY".equals(activeFlagName)) {
            localActivationUntilGameTime = level.getGameTime() + ACTIVATION_GRACE_TICKS;
        } else if ("DEACTIVATE_ABILITY".equals(activeFlagName)) {
            localActivationUntilGameTime = Long.MIN_VALUE;
        }
    }

    public static boolean matchesBlastWaveSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }
        Vec3 position = new Vec3(x, y, z);
        boolean localActivation = minecraft.player != null
                && level.getGameTime() <= localActivationUntilGameTime
                && minecraft.player.position().distanceToSqr(position) <= SOUND_MATCH_DISTANCE_SQR;
        return hasBlastWavePlayerNear(level, position)
                || localActivation
                || (lastPulseOrigin != null
                && level.getGameTime() - lastPulseGameTime <= SOUND_MATCH_TICKS
                && lastPulseOrigin.distanceToSqr(position) <= SOUND_MATCH_DISTANCE_SQR);
    }

    public static boolean handleActivationSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.BlastWaveSettings settings = ArcaneBeamConfig.INSTANCE.blastWave;
        Vec3 position = new Vec3(x, y, z);
        if (level == null || settings == null || !settings.enabled || !matchesBlastWaveSound(x, y, z)
                || !ArcaneBeamSoundController.canPlayBlastWaveActivation(minecraft)) {
            return false;
        }

        long now = level.getGameTime();
        if (lastActivationSoundPosition != null && now - lastActivationSoundGameTime <= 3L
                && lastActivationSoundPosition.distanceToSqr(position) <= SOUND_MATCH_DISTANCE_SQR) {
            return true;
        }
        ArcaneBeamSoundController.playBlastWaveActivation(minecraft, position);
        lastActivationSoundGameTime = now;
        lastActivationSoundPosition = position;
        return true;
    }

    public static boolean handleStrikeSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.BlastWaveSettings settings = ArcaneBeamConfig.INSTANCE.blastWave;
        if (level == null || settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlayBlastWaveStrike(minecraft)) {
            return false;
        }
        Vec3 position = new Vec3(x, y, z);
        long now = level.getGameTime();
        return matchesBlastWaveSound(x, y, z);
    }

    private static void queueStrikeSounds(Minecraft minecraft, Vec3 origin, int missileCount, long now, ArcaneBeamConfig.BlastWaveSettings settings) {
        if (missileCount <= 0 || !ArcaneBeamSoundController.canPlayBlastWaveStrike(minecraft)) {
            return;
        }
        int layers = Math.min(missileCount, settings.maxSoundLayers);
        for (int index = 0; index < layers; index++) {
            float normalized = layers == 1 ? 0.5F : (float) index / (layers - 1);
            float pitch = 1.0F + Mth.lerp(normalized, -settings.pitchVariance, settings.pitchVariance);
            PENDING_SOUNDS.add(new PendingStrikeSound(now + (long) index * settings.soundStaggerTicks, origin, pitch));
        }
    }

    private static List<LivingEntity> resolveTargets(ClientLevel level, int[] targetIds, Vec3 origin, int cap) {
        if (targetIds == null || targetIds.length == 0) {
            return Collections.emptyList();
        }
        List<LivingEntity> targets = new ArrayList<>();
        for (int targetId : targetIds) {
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living && !living.isRemoved()) {
                targets.add(living);
            }
        }
        targets.sort(Comparator.comparingDouble((LivingEntity target) -> target.distanceToSqr(origin)).thenComparingInt(Entity::getId));
        return targets.size() <= cap ? targets : new ArrayList<>(targets.subList(0, cap));
    }

    private static Player nearestPlayer(ClientLevel level, Vec3 packetOrigin) {
        Player nearest = null;
        double nearestDistance = 9.0D;
        for (Player player : level.players()) {
            double distance = player.position().distanceToSqr(packetOrigin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static Vec3 configuredOrigin(Player player, Vec3 fallback, ArcaneBeamConfig.BlastWaveSettings settings) {
        if (player == null) {
            return fallback.add(settings.originOffsetX, 1.50D + settings.originOffsetY, settings.originOffsetZ);
        }
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-5D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        return player.getEyePosition(1.0F)
                .add(right.scale(settings.originOffsetX))
                .add(0.0D, settings.originOffsetY, 0.0D)
                .add(horizontalLook.scale(settings.originOffsetZ));
    }

    private static Vec3 lateralOffset(Vec3 origin, Vec3 target, int targetId, float maximum) {
        Vec3 horizontal = new Vec3(target.x - origin.x, 0.0D, target.z - origin.z);
        Vec3 perpendicular = horizontal.lengthSqr() < 1.0E-5D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        int mixed = targetId * 1103515245 + 12345;
        double direction = (mixed & 1) == 0 ? -1.0D : 1.0D;
        double amount = maximum * (0.60D + ((mixed >>> 8) & 0xFF) / 255.0D * 0.40D);
        return perpendicular.scale(direction * amount);
    }

    private static boolean hasBlastWavePlayerNear(ClientLevel level, Vec3 position) {
        for (Player player : level.players()) {
            if (player.position().distanceToSqr(position) <= SOUND_MATCH_DISTANCE_SQR && hasBlastWaveEffect(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlastWaveEffect(Player player) {
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (BLAST_WAVE_EFFECT.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlastWaveAbility(String abilityId) {
        String normalized = abilityId == null ? "" : abilityId.toLowerCase().replace('-', '_').replace(' ', '_');
        return normalized.contains("smite_blast_wave") || normalized.contains("blast_wave") || normalized.contains("blastwave");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        long now = level.getGameTime();
        Iterator<ActiveBlastWaveMissile> missileIterator = ACTIVE_MISSILES.values().iterator();
        while (missileIterator.hasNext()) {
            ActiveBlastWaveMissile missile = missileIterator.next();
            if (missile.age(now, 0.0F) >= Math.max(1, missile.settings().lifetimeTicks())) {
                missileIterator.remove();
            }
        }
        Iterator<PendingStrikeSound> soundIterator = PENDING_SOUNDS.iterator();
        while (soundIterator.hasNext()) {
            PendingStrikeSound sound = soundIterator.next();
            if (sound.dueGameTime() <= now) {
                ArcaneBeamSoundController.playBlastWaveStrike(minecraft, sound.position(), sound.pitch());
                soundIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ArcaneBeamConfig.BlastWaveSettings settings = ArcaneBeamConfig.INSTANCE.blastWave;
        if (settings == null) {
            return;
        }

        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        List<BlastWaveCircle> circles = Collections.emptyList();
        if (settings.enabled && minecraft.player != null && now <= localCircleUntilGameTime) {
            float radius = settings.useActualRadius ? lastPulseRadius : FALLBACK_CIRCLE_RADIUS;
            circles = Collections.singletonList(new BlastWaveCircle(
                    minecraft.player, radius,
                    StormArrowVisualManager.StormArrowRenderSettings.from(settings)
            ));
        }
        if (!circles.isEmpty()) {
            StormArrowVisualRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), circles, Collections.emptyList());
        }
        if (settings.enabled && !ACTIVE_MISSILES.isEmpty()) {
            ArchonMissileRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), ACTIVE_MISSILES.values());
        }
        if (minecraft.screen instanceof ArcaneBeamConfigScreen screen && screen.blastWaveSelected() && minecraft.player != null) {
            Vec3 origin = configuredOrigin(minecraft.player, minecraft.player.position(), settings);
            PreviewMissile preview = new PreviewMissile(origin, settings.missileScale, StormArrowVisualManager.StormArrowRenderSettings.from(settings));
            ArchonMissileRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), Collections.singletonList(preview));
        }
    }

    private static void clear() {
        ACTIVE_MISSILES.clear();
        PENDING_SOUNDS.clear();
        lastPulseOrigin = null;
        lastPulseGameTime = Long.MIN_VALUE;
        localCircleUntilGameTime = Long.MIN_VALUE;
        lastActivationSoundGameTime = Long.MIN_VALUE;
        lastActivationSoundPosition = null;
        localActivationUntilGameTime = Long.MIN_VALUE;
    }

    private record PendingStrikeSound(long dueGameTime, Vec3 position, float pitch) {
    }

    private record BlastWaveCircle(Player player, float radius,
                                   StormArrowVisualManager.StormArrowRenderSettings settings)
            implements StormArrowVisualRenderer.CircleVisual {
        @Override
        public Vec3 groundCenter(float partialTick) {
            return new Vec3(
                    Mth.lerp((double) partialTick, player.xOld, player.getX()),
                    Mth.lerp((double) partialTick, player.yOld, player.getY()) + 0.08D,
                    Mth.lerp((double) partialTick, player.zOld, player.getZ())
            );
        }
    }

    private record PreviewMissile(Vec3 origin, float scale, StormArrowVisualManager.StormArrowRenderSettings settings)
            implements ArchonMissileRenderer.MissileVisual {
        @Override
        public Vec3 impact() {
            return origin.add(0.0D, 1.0D, 0.0D);
        }

        @Override
        public float progress(long gameTime, float partialTick) {
            return 0.0F;
        }
    }

    private record ActiveBlastWaveMissile(
            int targetId,
            Vec3 origin,
            Vec3 fallbackTargetPoint,
            Vec3 controlOne,
            Vec3 controlTwo,
            long startGameTime,
            float scale,
            StormArrowVisualManager.StormArrowRenderSettings settings
    ) implements ArchonMissileRenderer.MissileVisual {
        private Vec3 targetPoint() {
            ClientLevel level = Minecraft.getInstance().level;
            Entity target = level == null ? null : level.getEntity(targetId);
            if (target instanceof LivingEntity living && !living.isRemoved()) {
                return living.position().add(0.0D, Math.max(0.30D, living.getBbHeight() * 0.48D), 0.0D);
            }
            return fallbackTargetPoint;
        }

        @Override
        public Vec3 impact() {
            return targetPoint().subtract(0.0D, 0.42D, 0.0D);
        }

        @Override
        public Vec3 positionAt(float progress) {
            float t = Mth.clamp(progress, 0.0F, 1.0F);
            double inverse = 1.0D - t;
            return origin.scale(inverse * inverse * inverse)
                    .add(controlOne.scale(3.0D * inverse * inverse * t))
                    .add(controlTwo.scale(3.0D * inverse * t * t))
                    .add(targetPoint().scale(t * t * t));
        }

        private float age(long gameTime, float partialTick) {
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }

        @Override
        public float progress(long gameTime, float partialTick) {
            return Mth.clamp(age(gameTime, partialTick) / Math.max(1.0F, settings.lifetimeTicks()), 0.0F, 1.0F);
        }
    }
}
