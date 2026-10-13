package dev.hoyin1600p.arcanebeam.ability.stormarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.vault.VaultFields;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import iskallia.vault.entity.entity.VaultStormEntity;
import iskallia.vault.entity.entity.VaultStormArrow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StormArrowVisualManager {
    private static final double STORM_ENTITY_VERTICAL_OFFSET = 8.0D;
    private static final float FALLBACK_RADIUS = 5.0F;
    /** A bolt is generated inside the storm's circle; this only absorbs float error at the rim. */
    private static final double STORM_BOLT_RIM_TOLERANCE = 0.5D;
    private static final double STORM_BOLT_VERTICAL_REACH = 24.0D;
    private static final long PROJECTILE_SOUND_SUPPRESSION_TICKS = 4L;
    private static final double PROJECTILE_SOUND_SUPPRESSION_DISTANCE_SQR = 16.0D;
    // The bolt entity and the sound of one strike arrive together at the same spot.
    private static final long STRIKE_VISUAL_DUPLICATE_TICKS = 3L;
    private static final double STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR = 4.0D;
    // Vault's final bolts can arrive after the storm entity is removed on the client.
    private static final long ENDED_STORM_GRACE_TICKS = 40L;
    private static final Map<Integer, ActiveStorm> activeStorms = new LinkedHashMap<>();
    private static final List<EndedStorm> endedStorms = new ArrayList<>();
    private static final Map<UUID, ActiveBlasterStrike> activeStrikes = new LinkedHashMap<>();
    private static final Map<Integer, VaultStormArrow> activeProjectiles = new LinkedHashMap<>();
    // Arrows seen joining the level whose storm type has not been confirmed yet; see onEntityJoinWorld.
    private static final Map<Integer, PendingProjectile> pendingProjectiles = new LinkedHashMap<>();
    private static long nextSoundStrikeKey = 1L;
    private static long suppressProjectileSoundUntilGameTime = Long.MIN_VALUE;
    private static Vec3 suppressProjectileSoundPosition;

    private StormArrowVisualManager() {
    }

    public static void observeStormEntity(VaultStormEntity stormEntity) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !stormEntity.level.isClientSide || !isReplacedStormType(stormEntity)) {
            return;
        }

        activeStorms.put(stormEntity.getId(), new ActiveStorm(stormEntity, StormArrowRenderSettings.from(settings)));
    }

    public static boolean shouldSuppressCloudParticle(VaultStormEntity stormEntity) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !stormEntity.level.isClientSide || !isReplacedStormType(stormEntity)) {
            return false;
        }
        observeStormEntity(stormEntity);
        return true;
    }

    /** Only the base Storm Arrow storm has a replacement so far; Blizzard stays vanilla. */
    private static boolean isReplacedStormType(VaultStormEntity stormEntity) {
        return stormEntity.getStormArrowType() == VaultStormArrow.StormType.BASE;
    }

    public static boolean handleSmiteBoltRender(Entity smiteBolt) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !smiteBolt.level.isClientSide) {
            return false;
        }
        // Only bolts inside a storm we are drawing are replaced; anything else stays vanilla.
        return registerStormStrike(smiteBolt.getUUID(), smiteBolt.position(), settings);
    }

    /**
     * Registers one strike of an owned storm (drawing it and playing its sound) exactly once, whichever of the
     * three signals reaches the client first: the bolt entity joining the level, the bolt being rendered, or
     * Vault's strike sound. The join and the sound do not depend on the bolt being on screen, so strikes behind
     * the player are still heard. Returns false when no owned storm contains the point.
     */
    private static boolean registerStormStrike(UUID key, Vec3 impact, StormArrowSettings settings) {
        if (owningStorm(impact) == null) {
            return false;
        }
        long now = gameTime();
        if (activeStrikes.containsKey(key) || hasRecentStrikeAt(impact, now)) {
            return true;
        }
        activeStrikes.put(key, new ActiveBlasterStrike(impact, now, StormArrowRenderSettings.from(settings)));
        ArcaneBeamSoundController.playStormArrowStrike(Minecraft.getInstance(), impact);
        return true;
    }

    /** Only the base Storm Arrow arrow is replaced; the Blizzard arrow, like the Blizzard storm, stays Vault's. */
    private static boolean isReplacedProjectile(VaultStormArrow arrow) {
        return arrow.getStormArrowType() == VaultStormArrow.StormType.BASE;
    }

    public static boolean handleProjectileRender(VaultStormArrow arrow, PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || arrow == null || !arrow.level.isClientSide || !isReplacedProjectile(arrow)) {
            return false;
        }

        observeProjectileSpawn(arrow);
        StormArrowProjectileRenderer.renderLocal(poseStack, buffer, arrow, partialTick, settings.shaderCompatibility);
        return true;
    }

    public static boolean shouldSuppressProjectileTrail(VaultStormArrow arrow) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || arrow == null || !arrow.level.isClientSide || !isReplacedProjectile(arrow)) {
            return false;
        }

        observeProjectileSpawn(arrow);
        return true;
    }

    private static void observeProjectileSpawn(VaultStormArrow arrow) {
        if (arrow == null || !arrow.level.isClientSide || activeProjectiles.containsKey(arrow.getId())) {
            return;
        }

        activeProjectiles.put(arrow.getId(), arrow);
        Vec3 position = arrow.position();
        if (ArcaneBeamSoundController.playStormArrowProjectile(Minecraft.getInstance(), position)) {
            suppressProjectileSoundUntilGameTime = gameTime() + PROJECTILE_SOUND_SUPPRESSION_TICKS;
            suppressProjectileSoundPosition = position;
        }
    }

    public static boolean handleStormArrowProjectileSound(double x, double y, double z) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (settings == null || !settings.enabled || level == null
                || !ArcaneBeamSoundController.canPlayStormArrowProjectile(minecraft)) {
            return false;
        }
        long now = level.getGameTime();
        if (suppressProjectileSoundPosition != null && now <= suppressProjectileSoundUntilGameTime
                && suppressProjectileSoundPosition.distanceToSqr(x, y, z) <= PROJECTILE_SOUND_SUPPRESSION_DISTANCE_SQR) {
            return true;
        }
        // An arrow that joined this tick has not been confirmed yet, but its type is already synced.
        for (PendingProjectile pending : pendingProjectiles.values()) {
            if (now - pending.joinedAt() <= PROJECTILE_SOUND_SUPPRESSION_TICKS && !pending.arrow().isRemoved()
                    && isReplacedProjectile(pending.arrow())
                    && pending.arrow().position().distanceToSqr(x, y, z) <= PROJECTILE_SOUND_SUPPRESSION_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Claims Vault's {@code smite_bolt} sound for a strike of a storm this manager owns (or one it just drew).
     * The strike is registered here too, so a strike whose bolt entity never reaches the renderer is still heard.
     */
    public static boolean handleStormArrowStrikeSound(double x, double y, double z) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        Minecraft minecraft = Minecraft.getInstance();
        if (settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlayStormArrowStrike(minecraft)) {
            return false;
        }

        Vec3 position = new Vec3(x, y, z);
        if (registerStormStrike(new UUID(0L, nextSoundStrikeKey++), position, settings)) {
            return true;
        }
        long now = gameTime();
        return activeStrikes.values().stream().anyMatch(strike ->
                strike.impact().distanceToSqr(position) <= STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR
                        && strike.age(now, 0.0F) <= STRIKE_VISUAL_DUPLICATE_TICKS
        );
    }

    /**
     * True when a storm this manager draws (or ended within the last two seconds) contains the position. Other
     * managers use it to leave bolt sounds inside a storm to Storm Arrow.
     */
    public static boolean ownsBoltSound(Vec3 position) {
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        return settings != null && settings.enabled && owningStorm(position) != null;
    }

    /**
     * The storm that owns a bolt at this position: the nearest one whose circle (its synced radius, plus a half
     * block of float tolerance) contains it. A storm removed this tick still owns its last bolts, so removed
     * entries are not skipped.
     */
    private static StormOwner owningStorm(Vec3 position) {
        StormOwner best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ActiveStorm storm : activeStorms.values()) {
            VaultStormEntity entity = storm.entity();
            double distance = horizontalDistanceSqrIfOwned(entity.getX(), entity.getY(), entity.getZ(),
                    effectiveRadius(entity) + STORM_BOLT_RIM_TOLERANCE, position);
            if (distance >= 0.0D && distance < bestDistance) {
                bestDistance = distance;
                best = new StormOwner(entity.getX(), entity.getY(), entity.getZ());
            }
        }
        long now = gameTime();
        for (EndedStorm storm : endedStorms) {
            if (now - storm.endedAt() > ENDED_STORM_GRACE_TICKS) {
                continue;
            }
            double distance = horizontalDistanceSqrIfOwned(storm.x(), storm.y(), storm.z(), storm.reach(), position);
            if (distance >= 0.0D && distance < bestDistance) {
                bestDistance = distance;
                best = new StormOwner(storm.x(), storm.y(), storm.z());
            }
        }
        return best;
    }

    /** Squared horizontal distance to the storm centre when the position is inside its reach, else -1. */
    private static double horizontalDistanceSqrIfOwned(double stormX, double stormY, double stormZ, double reach, Vec3 position) {
        double dx = stormX - position.x;
        double dz = stormZ - position.z;
        double dy = Math.abs(stormY - STORM_ENTITY_VERTICAL_OFFSET - position.y);
        double distanceSqr = dx * dx + dz * dz;
        return distanceSqr <= reach * reach && dy <= STORM_BOLT_VERTICAL_REACH ? distanceSqr : -1.0D;
    }

    private record StormOwner(double x, double y, double z) {
    }

    private record PendingProjectile(VaultStormArrow arrow, long joinedAt) {
    }

    private static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    private static boolean hasRecentStrikeAt(Vec3 impact, long gameTime) {
        return activeStrikes.values().stream().anyMatch(strike ->
                strike.impact().distanceToSqr(impact) <= STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR
                        && strike.age(gameTime, 0.0F) <= STRIKE_VISUAL_DUPLICATE_TICKS
        );
    }

    private static float effectiveRadius(VaultStormEntity stormEntity) {
        try {
            // The Vault storm entity receives the server-calculated radius after all AOE modifiers are applied.
            EntityDataAccessor<Float> radiusAccessor = VaultFields.stormRadiusAccessor();
            Float radius = stormEntity.getEntityData().get(radiusAccessor);
            return radius == null || radius <= 0.0F ? FALLBACK_RADIUS : radius;
        } catch (RuntimeException error) {
            return FALLBACK_RADIUS;
        }
    }

    /** Drops all per-level state; called when the client level instance changes. */
    public static void clear() {
        clearVisuals();
        suppressProjectileSoundUntilGameTime = Long.MIN_VALUE;
        suppressProjectileSoundPosition = null;
    }

    /** Forgets every drawn storm, strike and tracked arrow (a re-enabled feature re-observes live storms). */
    private static void clearVisuals() {
        activeStorms.clear();
        endedStorms.clear();
        activeStrikes.clear();
        activeProjectiles.clear();
        pendingProjectiles.clear();
    }

    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide()) {
            return;
        }
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled) {
            return;
        }

        if (event.getEntity() instanceof VaultStormEntity.SmiteBolt bolt) {
            // Strikes are registered (and heard) when the bolt arrives, not when it happens to be drawn.
            registerStormStrike(bolt.getUUID(), bolt.position(), settings);
            return;
        }
        if (event.getEntity() instanceof VaultStormArrow arrow) {
            // The arrow's storm type is synced entity data that arrives right after the spawn packet, so it is
            // confirmed at the end of the tick instead of being guessed here (the Blizzard arrow stays Vault's).
            pendingProjectiles.put(arrow.getId(), new PendingProjectile(arrow, gameTime()));
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }

        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled) {
            // Turning the feature off mid-storm must stop its circle and strikes at once.
            clearVisuals();
            return;
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, PendingProjectile>> pendingIterator = pendingProjectiles.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            PendingProjectile pending = pendingIterator.next().getValue();
            if (!pending.arrow().isRemoved() && isReplacedProjectile(pending.arrow())) {
                observeProjectileSpawn(pending.arrow());
            }
            if (now - pending.joinedAt() >= 1L || pending.arrow().isRemoved()) {
                pendingIterator.remove();
            }
        }
        Iterator<Map.Entry<Integer, ActiveStorm>> stormIterator = activeStorms.entrySet().iterator();
        while (stormIterator.hasNext()) {
            ActiveStorm storm = stormIterator.next().getValue();
            if (storm.entity().isRemoved()) {
                VaultStormEntity entity = storm.entity();
                endedStorms.add(new EndedStorm(entity.getX(), entity.getY(), entity.getZ(), effectiveRadius(entity) + STORM_BOLT_RIM_TOLERANCE, now));
                stormIterator.remove();
            }
        }
        endedStorms.removeIf(storm -> now - storm.endedAt() > ENDED_STORM_GRACE_TICKS || now < storm.endedAt());

        Iterator<Map.Entry<UUID, ActiveBlasterStrike>> strikeIterator = activeStrikes.entrySet().iterator();
        while (strikeIterator.hasNext()) {
            ActiveBlasterStrike strike = strikeIterator.next().getValue();
            if (strike.age(now, 0.0F) >= Math.max(1, strike.settings().lifetimeTicks())) {
                strikeIterator.remove();
            }
        }

        Iterator<Map.Entry<Integer, VaultStormArrow>> projectileIterator = activeProjectiles.entrySet().iterator();
        while (projectileIterator.hasNext()) {
            VaultStormArrow projectile = projectileIterator.next().getValue();
            if (projectile == null || projectile.isRemoved() || !projectile.isAlive()) {
                projectileIterator.remove();
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled) {
            clearVisuals();
            return;
        }
        if (activeStorms.isEmpty() && activeStrikes.isEmpty()) {
            return;
        }

        StormArrowVisualRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeStorms.values(),
                activeStrikes.values());
    }

    private record EndedStorm(double x, double y, double z, double reach, long endedAt) {
    }

    public record ActiveStorm(VaultStormEntity entity, StormArrowRenderSettings settings) implements StormArrowVisualRenderer.CircleVisual {
        public Vec3 groundCenter(float partialTick) {
            return new Vec3(
                    Mth.lerp((double) partialTick, entity.xOld, entity.getX()),
                    Mth.lerp((double) partialTick, entity.yOld, entity.getY()) - STORM_ENTITY_VERTICAL_OFFSET + 0.08D,
                    Mth.lerp((double) partialTick, entity.zOld, entity.getZ())
            );
        }

        public float radius() {
            return settings.useActualRadius() ? effectiveRadius(entity) : FALLBACK_RADIUS;
        }
    }

    public record ActiveBlasterStrike(Vec3 impact, long startGameTime, StormArrowRenderSettings settings) implements StormArrowVisualRenderer.StrikeVisual {
        public float age(long gameTime, float partialTick) {
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }

        public float progress(long gameTime, float partialTick) {
            return Mth.clamp(age(gameTime, partialTick) / Math.max(1.0F, settings.lifetimeTicks()), 0.0F, 1.0F);
        }
    }

    public record StormArrowRenderSettings(
            boolean showTargetingCircle,
            boolean useActualRadius,
            int circleColor,
            float circleAlpha,
            float circleThickness,
            int blasterColor,
            int coreColor,
            float blasterAlpha,
            float blasterWidth,
            float segmentLength,
            int lifetimeTicks,
            float originHeight,
            boolean impactFlashEnabled,
            int impactFlashColor,
            float impactFlashSize,
            boolean fullbright,
            String shaderCompatibility
    ) {
        public static StormArrowRenderSettings from(StormArrowSettings settings) {
            return new StormArrowRenderSettings(
                    settings.showTargetingCircle,
                    settings.useActualRadius,
                    settings.circleColor,
                    settings.circleAlpha,
                    settings.circleThickness,
                    settings.blasterColor,
                    settings.coreColor,
                    settings.blasterAlpha,
                    settings.blasterWidth,
                    settings.segmentLength,
                    settings.lifetimeTicks,
                    settings.originHeight,
                    settings.impactFlashEnabled,
                    settings.impactFlashColor,
                    settings.impactFlashSize,
                    settings.fullbright,
                    settings.shaderCompatibility
            );
        }
    }
}
