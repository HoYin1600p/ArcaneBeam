package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.mixin.VaultStormEntityAccessor;
import iskallia.vault.entity.entity.VaultStormEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
public final class StormArrowVisualManager {
    private static final double STORM_ENTITY_VERTICAL_OFFSET = 8.0D;
    private static final float FALLBACK_RADIUS = 5.0F;
    private static final Map<Integer, ActiveStorm> activeStorms = new LinkedHashMap<>();
    private static final Map<Integer, ActiveBlasterStrike> activeStrikes = new LinkedHashMap<>();

    private StormArrowVisualManager() {
    }

    public static void observeStormEntity(VaultStormEntity stormEntity) {
        ArcaneBeamConfig.StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !stormEntity.level.isClientSide) {
            return;
        }

        activeStorms.put(stormEntity.getId(), new ActiveStorm(stormEntity, StormArrowRenderSettings.from(settings)));
    }

    public static boolean shouldSuppressCloudParticle(VaultStormEntity stormEntity) {
        ArcaneBeamConfig.StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !stormEntity.level.isClientSide) {
            return false;
        }
        observeStormEntity(stormEntity);
        return true;
    }

    public static boolean handleSmiteBoltRender(Entity smiteBolt) {
        ArcaneBeamConfig.StormArrowSettings settings = ArcaneBeamConfig.INSTANCE.stormArrow;
        if (settings == null || !settings.enabled || !smiteBolt.level.isClientSide) {
            return false;
        }

        activeStrikes.computeIfAbsent(smiteBolt.getId(), id -> new ActiveBlasterStrike(
                smiteBolt.position(),
                gameTime(),
                StormArrowRenderSettings.from(settings)
        ));
        return true;
    }

    private static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    private static float effectiveRadius(VaultStormEntity stormEntity) {
        try {
            // The Vault storm entity receives the server-calculated radius after all AOE modifiers are applied.
            EntityDataAccessor<Float> radiusAccessor = VaultStormEntityAccessor.arcanebeam$getRadiusAccessor();
            Float radius = stormEntity.getEntityData().get(radiusAccessor);
            return radius == null || radius <= 0.0F ? FALLBACK_RADIUS : radius;
        } catch (RuntimeException error) {
            return FALLBACK_RADIUS;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            activeStorms.clear();
            activeStrikes.clear();
            return;
        }

        Iterator<Map.Entry<Integer, ActiveStorm>> stormIterator = activeStorms.entrySet().iterator();
        while (stormIterator.hasNext()) {
            ActiveStorm storm = stormIterator.next().getValue();
            if (storm.entity().isRemoved()) {
                stormIterator.remove();
            }
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, ActiveBlasterStrike>> strikeIterator = activeStrikes.entrySet().iterator();
        while (strikeIterator.hasNext()) {
            ActiveBlasterStrike strike = strikeIterator.next().getValue();
            if (strike.age(now, 0.0F) >= Math.max(1, strike.settings().lifetimeTicks())) {
                strikeIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (activeStorms.isEmpty() && activeStrikes.isEmpty())) {
            return;
        }

        StormArrowVisualRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeStorms.values(), activeStrikes.values());
    }

    public record ActiveStorm(VaultStormEntity entity, StormArrowRenderSettings settings) {
        public Vec3 groundCenter() {
            return new Vec3(entity.getX(), entity.getY() - STORM_ENTITY_VERTICAL_OFFSET + 0.08D, entity.getZ());
        }

        public float radius() {
            return settings.useActualRadius() ? effectiveRadius(entity) : FALLBACK_RADIUS;
        }
    }

    public record ActiveBlasterStrike(Vec3 impact, long startGameTime, StormArrowRenderSettings settings) {
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
            float segmentGap,
            int lifetimeTicks,
            float originHeight,
            boolean impactFlashEnabled,
            int impactFlashColor,
            float impactFlashSize,
            boolean fullbright,
            String shaderCompatibility
    ) {
        private static StormArrowRenderSettings from(ArcaneBeamConfig.StormArrowSettings settings) {
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
                    settings.segmentGap,
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
