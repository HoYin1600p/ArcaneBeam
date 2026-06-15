package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.init.ModEntities;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class LightningStrikeShockwaveManager {
    private static final long CAST_SOUND_SUPPRESSION_TICKS = 4L;
    private static final long IMPACT_SOUND_SUPPRESSION_TICKS = 4L;
    private static final long VAULT_LIGHTNING_VISUAL_SUPPRESSION_TICKS = 40L;
    private static final long PENDING_LIGHTNING_STRIKE_TICKS = 100L;
    private static final double CAST_SOUND_SUPPRESSION_DISTANCE_SQR = 16.0D;
    private static final double IMPACT_SOUND_SUPPRESSION_DISTANCE_SQR = 16.0D;
    private static final double VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR = 4096.0D;
    private static final List<ActiveShockwave> activeShockwaves = new ArrayList<>();
    private static final List<ChainLightningAbility.ChainLightningProjectile> activeProjectiles = new ArrayList<>();
    private static long suppressCastSoundUntilGameTime = Long.MIN_VALUE;
    private static long suppressImpactSoundUntilGameTime = Long.MIN_VALUE;
    private static long suppressVaultLightningVisualUntilGameTime = Long.MIN_VALUE;
    private static long pendingLightningStrikeUntilGameTime = Long.MIN_VALUE;
    private static Vec3 suppressCastSoundPosition;
    private static Vec3 suppressImpactSoundPosition;
    private static Vec3 suppressVaultLightningVisualPosition;
    private static Vec3 pendingLightningStrikePosition;

    private LightningStrikeShockwaveManager() {
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ChainLightningAbility.ChainLightningProjectile projectile) {
            if (!activeProjectiles.contains(projectile)) {
                activeProjectiles.add(projectile);
            }
            observeProjectileSpawn(projectile.position());
            return;
        }
        if (event.getEntity().getType() == ModEntities.SMITE_ABILITY_BOLT && handleVaultLightningVisual(event.getEntity().position())) {
            event.setCanceled(true);
        }
    }

    public static void spawn(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || !settings.enabled) {
            return;
        }

        activeShockwaves.add(new ActiveShockwave(position.add(0.0D, settings.renderYOffset, 0.0D), level.getGameTime(), ShockwaveRenderSettings.from(settings)));
        observeImpact(position);
        observeVaultLightningVisual(position);
        ArcaneBeamSoundController.playLightningStrikeImpact(minecraft, position);
    }

    private static void observeProjectileSpawn(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || position == null || lightningSoundMode() == ArcaneBeamConfig.LightningSoundMode.DEFAULT) {
            observePendingLightningStrike(position);
            return;
        }

        observePendingLightningStrike(position);
        suppressCastSoundUntilGameTime = level.getGameTime() + CAST_SOUND_SUPPRESSION_TICKS;
        suppressCastSoundPosition = position;
        ArcaneBeamSoundController.playLightningStrikeCast(minecraft, position);
    }

    private static void observePendingLightningStrike(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || !settings.enabled) {
            return;
        }

        pendingLightningStrikeUntilGameTime = level.getGameTime() + PENDING_LIGHTNING_STRIKE_TICKS;
        pendingLightningStrikePosition = position;
    }

    private static void observeImpact(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || position == null || lightningSoundMode() == ArcaneBeamConfig.LightningSoundMode.DEFAULT) {
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

        suppressVaultLightningVisualUntilGameTime = level.getGameTime() + VAULT_LIGHTNING_VISUAL_SUPPRESSION_TICKS;
        suppressVaultLightningVisualPosition = position;
    }

    public static boolean shouldSuppressLightningCastSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || suppressCastSoundPosition == null || lightningSoundMode() == ArcaneBeamConfig.LightningSoundMode.DEFAULT) {
            return false;
        }
        if (level.getGameTime() > suppressCastSoundUntilGameTime) {
            return false;
        }
        return suppressCastSoundPosition.distanceToSqr(x, y, z) <= CAST_SOUND_SUPPRESSION_DISTANCE_SQR;
    }

    public static boolean shouldSuppressLightningImpactSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || suppressImpactSoundPosition == null || lightningSoundMode() == ArcaneBeamConfig.LightningSoundMode.DEFAULT) {
            return shouldSuppressPendingLightningImpactSound(x, y, z);
        }
        if (level.getGameTime() > suppressImpactSoundUntilGameTime) {
            return shouldSuppressPendingLightningImpactSound(x, y, z);
        }
        return suppressImpactSoundPosition.distanceToSqr(x, y, z) <= IMPACT_SOUND_SUPPRESSION_DISTANCE_SQR;
    }

    private static boolean shouldSuppressPendingLightningImpactSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || pendingLightningStrikePosition == null || lightningSoundMode() == ArcaneBeamConfig.LightningSoundMode.DEFAULT) {
            return false;
        }
        if (level.getGameTime() > pendingLightningStrikeUntilGameTime) {
            return false;
        }
        return pendingLightningStrikePosition.distanceToSqr(x, y, z) <= VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR;
    }

    public static boolean shouldReplaceProjectileRender() {
        ArcaneBeamConfig.LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        return settings != null && settings.enabled;
    }

    private static boolean handleVaultLightningVisual(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ArcaneBeamConfig.LightningStrikeSettings settings = ArcaneBeamConfig.INSTANCE.lightningStrike;
        if (level == null || position == null || !settings.enabled) {
            return false;
        }

        long gameTime = level.getGameTime();
        if (suppressVaultLightningVisualPosition != null && gameTime <= suppressVaultLightningVisualUntilGameTime
                && suppressVaultLightningVisualPosition.distanceToSqr(position) <= VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR) {
            return true;
        }
        if (pendingLightningStrikePosition == null || gameTime > pendingLightningStrikeUntilGameTime
                || pendingLightningStrikePosition.distanceToSqr(position) > VAULT_LIGHTNING_VISUAL_SUPPRESSION_DISTANCE_SQR) {
            return false;
        }

        spawn(position);
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            activeShockwaves.clear();
            activeProjectiles.clear();
            suppressCastSoundUntilGameTime = Long.MIN_VALUE;
            suppressImpactSoundUntilGameTime = Long.MIN_VALUE;
            suppressVaultLightningVisualUntilGameTime = Long.MIN_VALUE;
            pendingLightningStrikeUntilGameTime = Long.MIN_VALUE;
            suppressCastSoundPosition = null;
            suppressImpactSoundPosition = null;
            suppressVaultLightningVisualPosition = null;
            pendingLightningStrikePosition = null;
            return;
        }

        long gameTime = minecraft.level.getGameTime();
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
                projectileIterator.remove();
            }
        }
    }

    @SubscribeEvent
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

    public record ActiveShockwave(Vec3 position, long startGameTime, ShockwaveRenderSettings settings) {
        public float age(float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            long gameTime = minecraft.level == null ? startGameTime : minecraft.level.getGameTime();
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }
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
        private static ShockwaveRenderSettings from(ArcaneBeamConfig.LightningStrikeSettings settings) {
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

    private static ArcaneBeamConfig.LightningSoundMode lightningSoundMode() {
        ArcaneBeamConfig.LightningSoundMode mode = ArcaneBeamConfig.LightningSoundMode.fromId(ArcaneBeamConfig.INSTANCE.lightningStrike.soundMode);
        return mode == null ? ArcaneBeamConfig.LightningSoundMode.DEFAULT : mode;
    }
}
