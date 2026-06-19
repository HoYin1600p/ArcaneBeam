package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.mixin.SmiteBoltAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class SmiteVisualManager {
    private static final int VAULT_DEFAULT_SMITE_BOLT_COLOR = -1864448;
    private static final float FALLBACK_RADIUS = 5.0F;
    private static final long ACTIVATION_CIRCLE_GRACE_TICKS = 120L;
    private static final long SOUND_DUPLICATE_SUPPRESSION_TICKS = 3L;
    private static final double SOUND_DUPLICATE_DISTANCE_SQR = 16.0D;
    private static final long STRIKE_VISUAL_DUPLICATE_TICKS = 2L;
    private static final double STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR = 2.25D;
    private static final Map<Integer, ActiveSmiteStrike> activeStrikes = new LinkedHashMap<>();
    private static long activeCircleUntilGameTime = Long.MIN_VALUE;
    private static long lastActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastActivationSoundPosition;
    private static long lastStrikeSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastStrikeSoundPosition;
    private static long lastStrikeVisualGameTime = Long.MIN_VALUE;

    private SmiteVisualManager() {
    }

    public static boolean handleSmiteBoltRender(Entity smiteBolt) {
        if (smiteBolt == null || !smiteBolt.level.isClientSide) {
            return false;
        }

        Vec3 impact = smiteBolt.position();
        int boltColor = smiteBoltColor(smiteBolt);
        // Lightning Strike's AOE reuses Vault's smite bolt entity without applying a Smite ability color.
        if (boltColor == VAULT_DEFAULT_SMITE_BOLT_COLOR) {
            return LightningStrikeShockwaveManager.shouldSuppressDefaultVaultLightningVisual(impact);
        }

        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled) {
            return false;
        }
        long now = gameTime();
        if (now == lastStrikeVisualGameTime) {
            return true;
        }
        if (hasRecentStrikeAt(impact, now)) {
            return true;
        }
        activeStrikes.computeIfAbsent(smiteBolt.getId(), id -> {
            playSmiteStrikeOnce(Minecraft.getInstance(), impact);
            lastStrikeVisualGameTime = now;
            return new ActiveSmiteStrike(
                    impact,
                    now,
                    StormArrowVisualManager.StormArrowRenderSettings.from(settings)
            );
        });
        return true;
    }

    private static int smiteBoltColor(Entity smiteBolt) {
        try {
            Integer color = smiteBolt.getEntityData().get(SmiteBoltAccessor.arcanebeam$getColorAccessor());
            return color == null ? 0 : color;
        } catch (RuntimeException error) {
            return 0;
        }
    }

    public static boolean handleSmiteActivationSound(double x, double y, double z) {
        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlaySmiteActivation(minecraft)) {
            return false;
        }

        Vec3 position = new Vec3(x, y, z);
        long now = level.getGameTime();
        activeCircleUntilGameTime = now + ACTIVATION_CIRCLE_GRACE_TICKS;
        if (isDuplicate(now, position, lastActivationSoundGameTime, lastActivationSoundPosition)) {
            return true;
        }

        ArcaneBeamSoundController.playSmiteActivation(minecraft, position);
        lastActivationSoundGameTime = now;
        lastActivationSoundPosition = position;
        return true;
    }

    public static boolean handleSmiteStrikeSound(double x, double y, double z) {
        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlaySmiteStrike(minecraft)) {
            return false;
        }

        Vec3 position = new Vec3(x, y, z);
        long now = level.getGameTime();
        if (now == lastStrikeSoundGameTime) {
            return true;
        }
        if (isDuplicate(now, position, lastStrikeSoundGameTime, lastStrikeSoundPosition)) {
            return true;
        }
        if (activeStrikes.values().stream().anyMatch(strike ->
                strike.impact().distanceToSqr(position) <= 64.0D && strike.age(now, 0.0F) <= 5.0F
        )) {
            return true;
        }
        if (!hasActiveSmiteCircle(minecraft.player, now)) {
            return false;
        }

        playSmiteStrikeOnce(minecraft, position);
        return true;
    }

    private static void playSmiteStrikeOnce(Minecraft minecraft, Vec3 position) {
        long now = gameTime();
        if (isDuplicate(now, position, lastStrikeSoundGameTime, lastStrikeSoundPosition)) {
            return;
        }
        if (ArcaneBeamSoundController.playSmiteStrike(minecraft, position)) {
            lastStrikeSoundGameTime = now;
            lastStrikeSoundPosition = position;
        }
    }

    private static boolean isDuplicate(long now, Vec3 position, long lastGameTime, Vec3 lastPosition) {
        return lastPosition != null
                && now - lastGameTime <= SOUND_DUPLICATE_SUPPRESSION_TICKS
                && lastPosition.distanceToSqr(position) <= SOUND_DUPLICATE_DISTANCE_SQR;
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

    private static boolean hasActiveSmiteCircle(LocalPlayer player, long gameTime) {
        if (player == null) {
            return false;
        }
        if (gameTime <= activeCircleUntilGameTime) {
            return true;
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (id != null && "the_vault".equals(id.getNamespace()) && id.getPath().startsWith("smite")) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            activeStrikes.clear();
            activeCircleUntilGameTime = Long.MIN_VALUE;
            lastActivationSoundGameTime = Long.MIN_VALUE;
            lastActivationSoundPosition = null;
            lastStrikeSoundGameTime = Long.MIN_VALUE;
            lastStrikeSoundPosition = null;
            lastStrikeVisualGameTime = Long.MIN_VALUE;
            return;
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, ActiveSmiteStrike>> strikeIterator = activeStrikes.entrySet().iterator();
        while (strikeIterator.hasNext()) {
            ActiveSmiteStrike strike = strikeIterator.next().getValue();
            if (strike.age(now, 0.0F) >= Math.max(1, strike.settings().lifetimeTicks())) {
                strikeIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled) {
            activeStrikes.clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        boolean showCircle = hasActiveSmiteCircle(minecraft.player, now);
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (!showCircle && activeStrikes.isEmpty())) {
            return;
        }

        ActiveSmiteCircle circle = showCircle && minecraft.player != null
                ? new ActiveSmiteCircle(minecraft.player.position().add(0.0D, 0.08D, 0.0D), StormArrowVisualManager.StormArrowRenderSettings.from(settings))
                : null;
        StormArrowVisualRenderer.render(
                event.getPoseStack(),
                event.getCamera().getPosition(),
                event.getPartialTick(),
                circle == null ? Collections.emptyList() : Collections.singletonList(circle),
                activeStrikes.values()
        );
    }

    public record ActiveSmiteCircle(Vec3 groundCenter, StormArrowVisualManager.StormArrowRenderSettings settings) implements StormArrowVisualRenderer.CircleVisual {
        @Override
        public float radius() {
            return FALLBACK_RADIUS;
        }
    }

    public record ActiveSmiteStrike(Vec3 impact, long startGameTime, StormArrowVisualManager.StormArrowRenderSettings settings) implements StormArrowVisualRenderer.StrikeVisual {
        public float age(long gameTime, float partialTick) {
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }

        @Override
        public float progress(long gameTime, float partialTick) {
            return Mth.clamp(age(gameTime, partialTick) / Math.max(1.0F, settings.lifetimeTicks()), 0.0F, 1.0F);
        }
    }
}
