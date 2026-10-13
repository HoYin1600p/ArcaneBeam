package dev.hoyin1600p.arcanebeam.ability.lightning;

import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.config.model.LightningSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.LightningStrikeSettings;
import iskallia.vault.init.ModEntities;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class LightningStrikeShockwaveManager {
    private static final long CAST_SOUND_SUPPRESSION_TICKS = 4L;
    private static final long IMPACT_SOUND_SUPPRESSION_TICKS = 4L;
    // Vault spawns the Lightning Strike bolts on the struck mob and on the mobs the strike chains to, so a bolt
    // is hidden only near a recorded impact point of one of our own projectiles and only briefly (this used to
    // be 24 blocks around the latest impact, which also hid other players Smite bolts).
    private static final long VAULT_LIGHTNING_VISUAL_SUPPRESSION_TICKS = 6L;
    private static final int MAX_IMPACT_SUPPRESSIONS = 8;
    private static final long PENDING_LIGHTNING_STRIKE_TICKS = 100L;
    private static final double CAST_SOUND_SUPPRESSION_DISTANCE_SQR = 16.0D;
    private static final double IMPACT_SOUND_SUPPRESSION_DISTANCE_SQR = 16.0D;
    private static final double VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR = 144.0D;
    private static final double PENDING_LIGHTNING_STRIKE_DISTANCE_SQR = 16.0D;
    private static final List<ActiveShockwave> activeShockwaves = new ArrayList<>();
    private static final List<ChainLightningAbility.ChainLightningProjectile> activeProjectiles = new ArrayList<>();
    private static final int LOCAL_SPAWN_MAX_AGE_TICKS = 3;
    private static final double LOCAL_SPAWN_RADIUS_SQUARED = 9.0D;
    private static final Map<Entity, Boolean> SPAWN_OWNERSHIP = new WeakHashMap<>();
    // Rapid fire keeps several projectiles in flight; each one's Vault bolt is hidden near where it ended.
    private static final long ENDED_PROJECTILE_SUPPRESSION_TICKS = 10L;
    private static final double ENDED_PROJECTILE_SUPPRESSION_DISTANCE_SQR = 16.0D;
    private static final List<EndedProjectile> endedProjectiles = new ArrayList<>();
    // One impact can report itself through the client hit, the hidden bolt and the impact sound.
    private static final long DUPLICATE_SHOCKWAVE_TICKS = 5L;
    private static final double DUPLICATE_SHOCKWAVE_DISTANCE_SQR = 9.0D;
    private static long suppressCastSoundUntilGameTime = Long.MIN_VALUE;
    private static long suppressImpactSoundUntilGameTime = Long.MIN_VALUE;
    private static long pendingLightningStrikeStartGameTime = Long.MIN_VALUE;
    private static long pendingLightningStrikeUntilGameTime = Long.MIN_VALUE;
    private static Vec3 suppressCastSoundPosition;
    private static Vec3 suppressImpactSoundPosition;
    private static final List<ImpactSuppression> impactSuppressions = new ArrayList<>();
    private static Vec3 pendingLightningStrikePosition;

    private LightningStrikeShockwaveManager() {
    }

    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ChainLightningAbility.ChainLightningProjectile projectile) {
            if (shouldReplaceProjectileRender(projectile) && !activeProjectiles.contains(projectile)) {
                activeProjectiles.add(projectile);
                observeProjectileSpawn(projectile.position());
            }
            return;
        }
        if (event.getEntity().getType() == ModEntities.SMITE_ABILITY_BOLT && shouldSuppressDefaultVaultLightningVisual(event.getEntity().position())) {
            event.setCanceled(true);
            // In rapid fire the client never simulates the earlier projectiles' hits, so the hidden bolt
            // is the only sign of their impact; spawn() skips it when that impact already has a shockwave.
            if (matchesOwnProjectile(event.getEntity().position())) {
                spawn(event.getEntity().position());
            }
        }
    }

    public static void spawn(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || settings == null || !settings.enabled) {
            return;
        }

        if (hasRecentShockwaveNear(position, level.getGameTime())) {
            return;
        }
        activeShockwaves.add(new ActiveShockwave(position.add(0.0D, settings.renderYOffset, 0.0D), level.getGameTime(),
                ShockwaveRenderSettings.from(settings)));
        observeImpact(position);
        observeVaultLightningVisual(position);
        clearPendingLightningStrike();
        ArcaneBeamSoundController.playLightningStrikeImpact(minecraft, position);
    }

    private static void observeProjectileSpawn(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || settings == null || !settings.enabled) {
            return;
        }

        observePendingLightningStrike(position);
        if (lightningSoundMode() == LightningSoundMode.DEFAULT
                || !ArcaneBeamSoundController.canPlayLightningStrikeCast(minecraft)) {
            return;
        }
        suppressCastSoundUntilGameTime = level.getGameTime() + CAST_SOUND_SUPPRESSION_TICKS;
        suppressCastSoundPosition = position;
        ArcaneBeamSoundController.playLightningStrikeCast(minecraft, position);
    }

    private static void observePendingLightningStrike(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || settings == null || !settings.enabled) {
            return;
        }

        pendingLightningStrikeStartGameTime = level.getGameTime();
        pendingLightningStrikeUntilGameTime = pendingLightningStrikeStartGameTime + PENDING_LIGHTNING_STRIKE_TICKS;
        pendingLightningStrikePosition = position;
    }

    private static void observeImpact(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || settings == null || !settings.enabled
                || lightningSoundMode() == LightningSoundMode.DEFAULT
                || !ArcaneBeamSoundController.canPlayLightningStrikeImpact(minecraft)) {
            return;
        }

        suppressImpactSoundUntilGameTime = level.getGameTime() + IMPACT_SOUND_SUPPRESSION_TICKS;
        suppressImpactSoundPosition = position;
    }

    private static void observeVaultLightningVisual(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || position == null) {
            return;
        }

        impactSuppressions.add(new ImpactSuppression(position, level.getGameTime() + VAULT_LIGHTNING_VISUAL_SUPPRESSION_TICKS));
        while (impactSuppressions.size() > MAX_IMPACT_SUPPRESSIONS) {
            impactSuppressions.remove(0);
        }
    }

    public static boolean shouldSuppressLightningCastSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || settings == null || !settings.enabled || suppressCastSoundPosition == null
                || lightningSoundMode() == LightningSoundMode.DEFAULT
                || !ArcaneBeamSoundController.canPlayLightningStrikeCast(minecraft)) {
            return false;
        }
        if (level.getGameTime() > suppressCastSoundUntilGameTime) {
            return false;
        }
        return suppressCastSoundPosition.distanceToSqr(x, y, z) <= CAST_SOUND_SUPPRESSION_DISTANCE_SQR;
    }

    public static boolean handleLightningImpactSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || settings == null || !settings.enabled) {
            return false;
        }
        boolean suppressDefault = lightningSoundMode() != LightningSoundMode.DEFAULT
                && ArcaneBeamSoundController.canPlayLightningStrikeImpact(minecraft);
        if (suppressImpactSoundPosition != null
                && level.getGameTime() <= suppressImpactSoundUntilGameTime
                && suppressImpactSoundPosition.distanceToSqr(x, y, z) <= IMPACT_SOUND_SUPPRESSION_DISTANCE_SQR) {
            return suppressDefault;
        }
        return spawnFromPendingLightningImpactSound(x, y, z) && suppressDefault;
    }

    private static boolean spawnFromPendingLightningImpactSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || settings == null || !settings.enabled || pendingLightningStrikePosition == null) {
            return false;
        }
        if (level.getGameTime() > pendingLightningStrikeUntilGameTime) {
            return false;
        }
        if (pendingLightningStrikePosition.distanceToSqr(x, y, z) > PENDING_LIGHTNING_STRIKE_DISTANCE_SQR) {
            return false;
        }

        spawn(new Vec3(x, y, z));
        return true;
    }

    /** True only for projectiles cast by the local player; teammates' arrows keep their vanilla look. */
    public static boolean isLocallyOwned(Entity projectile) {
        if (!(projectile instanceof Projectile owned)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Entity owner = owned.getOwner();
        if (owner != null) {
            return owner.getUUID().equals(minecraft.player.getUUID());
        }
        // Vault spawns this projectile through Forge's spawn packet, which carries no owner, so the
        // client attributes it once, on first sight: a cast leaves from the caster's hand.
        return SPAWN_OWNERSHIP.computeIfAbsent(projectile, entity -> entity.tickCount <= LOCAL_SPAWN_MAX_AGE_TICKS
                && entity.position().distanceToSqr(minecraft.player.getEyePosition()) <= LOCAL_SPAWN_RADIUS_SQUARED);
    }

    public static boolean shouldReplaceProjectileRender(Entity projectile) {
        return shouldReplaceProjectileRender() && isLocallyOwned(projectile);
    }

    public static boolean shouldReplaceProjectileRender() {
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        return settings != null && settings.enabled;
    }

    public static boolean shouldSuppressDefaultVaultLightningVisual(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || settings == null || !settings.enabled) {
            return false;
        }

        long gameTime = level.getGameTime();
        for (ImpactSuppression suppression : impactSuppressions) {
            if (gameTime <= suppression.untilGameTime()
                    && suppression.position().distanceToSqr(position) <= VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR) {
                return true;
            }
        }
        return matchesOwnProjectile(position);
    }

    /** True when a position is where one of our own projectiles is, was heading, or just ended. */
    private static boolean matchesOwnProjectile(Vec3 position) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || position == null) {
            return false;
        }
        long gameTime = level.getGameTime();
        if (pendingLightningStrikePosition != null
                && gameTime <= pendingLightningStrikeUntilGameTime
                && pendingLightningStrikePosition.distanceToSqr(position) <= PENDING_LIGHTNING_STRIKE_DISTANCE_SQR) {
            return true;
        }
        for (ChainLightningAbility.ChainLightningProjectile projectile : activeProjectiles) {
            if (projectile != null && projectile.position().distanceToSqr(position) <= ENDED_PROJECTILE_SUPPRESSION_DISTANCE_SQR) {
                return true;
            }
        }
        for (EndedProjectile ended : endedProjectiles) {
            if (gameTime <= ended.untilGameTime() && ended.position().distanceToSqr(position) <= ENDED_PROJECTILE_SUPPRESSION_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecentShockwaveNear(Vec3 position, long gameTime) {
        for (ActiveShockwave shockwave : activeShockwaves) {
            if (gameTime - shockwave.startGameTime() <= DUPLICATE_SHOCKWAVE_TICKS
                    && shockwave.position().distanceToSqr(position) <= DUPLICATE_SHOCKWAVE_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private static void clearPendingLightningStrike() {
        pendingLightningStrikeStartGameTime = Long.MIN_VALUE;
        pendingLightningStrikeUntilGameTime = Long.MIN_VALUE;
        pendingLightningStrikePosition = null;
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (minecraft.level == null || settings == null || !settings.enabled) {
            clear();
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (!activeProjectiles.isEmpty() && pendingLightningStrikePosition != null && gameTime <= pendingLightningStrikeUntilGameTime) {
            pendingLightningStrikePosition = activeProjectiles.get(activeProjectiles.size() - 1).position();
        }
        Iterator<ActiveShockwave> iterator = activeShockwaves.iterator();
        while (iterator.hasNext()) {
            ActiveShockwave shockwave = iterator.next();
            int lifetime = shockwave.settings().lifetimeTicks();
            int extraDelay = shockwave.settings().secondaryRippleCount() * shockwave.settings().secondaryRippleDelayTicks();
            if (gameTime - shockwave.startGameTime > lifetime + extraDelay) {
                iterator.remove();
            }
        }
        Iterator<ChainLightningAbility.ChainLightningProjectile> projectileIterator = activeProjectiles.iterator();
        while (projectileIterator.hasNext()) {
            ChainLightningAbility.ChainLightningProjectile projectile = projectileIterator.next();
            if (projectile == null || projectile.isRemoved() || !projectile.isAlive()) {
                if (projectile != null) {
                    endedProjectiles.add(new EndedProjectile(projectile.position(), gameTime + ENDED_PROJECTILE_SUPPRESSION_TICKS));
                }
                projectileIterator.remove();
            }
        }
        endedProjectiles.removeIf(ended -> gameTime > ended.untilGameTime()
                || ended.untilGameTime() - gameTime > ENDED_PROJECTILE_SUPPRESSION_TICKS);
        if (activeProjectiles.isEmpty() && pendingLightningStrikePosition != null && gameTime > pendingLightningStrikeStartGameTime + 2L) {
            clearPendingLightningStrike();
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (!activeProjectiles.isEmpty() && shouldReplaceProjectileRender()) {
            LightningStrikeChargeRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeProjectiles);
        }
        if (!activeShockwaves.isEmpty()) {
            LightningStrikeShockwaveRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeShockwaves);
        }
    }

    private record ImpactSuppression(Vec3 position, long untilGameTime) {
    }

    private record EndedProjectile(Vec3 position, long untilGameTime) {
    }

    public record ActiveShockwave(Vec3 position, long startGameTime, ShockwaveRenderSettings settings) {
        public float age(float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft.level == null ? startGameTime : minecraft.level.getGameTime();
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }
    }

    /** Drops all per-level state; called when the client level instance changes. */
    public static void clear() {
        activeShockwaves.clear();
        activeProjectiles.clear();
        SPAWN_OWNERSHIP.clear();
        endedProjectiles.clear();
        suppressCastSoundUntilGameTime = Long.MIN_VALUE;
        suppressImpactSoundUntilGameTime = Long.MIN_VALUE;
        clearPendingLightningStrike();
        suppressCastSoundPosition = null;
        suppressImpactSoundPosition = null;
        impactSuppressions.clear();
    }

    public record ShockwaveRenderSettings(
            float startRadius,
            float endRadius,
            int lifetimeTicks,
            float ringThickness,
            int ringSideCount,
            int ringColor,
            int centerFlashColor,
            float alpha,
            float ringInteriorOpacity,
            int sphereColor,
            float sphereRadius,
            float sphereOpacity,
            int coneColor,
            float coneHeight,
            float coneRadius,
            float coneOpacity,
            int spotColor,
            int spotCount,
            float spotSize,
            float spotOpacity,
            boolean fullbright,
            String shaderCompatibility,
            int secondaryRippleCount,
            float secondaryRippleSize,
            int secondaryRippleDelayTicks
    ) {
        private static ShockwaveRenderSettings from(LightningStrikeSettings settings) {
            return new ShockwaveRenderSettings(
                    settings.startRadius,
                    settings.endRadius,
                    settings.lifetimeTicks,
                    settings.ringThickness,
                    settings.ringSideCount,
                    settings.ringColor,
                    settings.centerFlashColor,
                    settings.alpha,
                    settings.ringInteriorOpacity,
                    settings.sphereColor,
                    settings.sphereRadius,
                    settings.sphereOpacity,
                    settings.coneColor,
                    settings.coneHeight,
                    settings.coneRadius,
                    settings.coneOpacity,
                    settings.spotColor,
                    settings.spotCount,
                    settings.spotSize,
                    settings.spotOpacity,
                    settings.fullbright,
                    settings.shaderCompatibility,
                    settings.secondaryRippleCount,
                    settings.secondaryRippleSize,
                    settings.secondaryRippleDelayTicks
            );
        }
    }

    private static LightningSoundMode lightningSoundMode() {
        LightningSoundMode mode = LightningSoundMode.fromId(ArcaneBeamConfig.INSTANCE.lightningStrike.soundMode);
        return mode == null ? LightningSoundMode.DEFAULT : mode;
    }
}
