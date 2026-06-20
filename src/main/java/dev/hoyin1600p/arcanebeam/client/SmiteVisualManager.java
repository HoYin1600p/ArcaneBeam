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
    private static final int VAULT_ARCHON_SMITE_BOLT_COLOR = 14282239;
    private static final float FALLBACK_RADIUS = 5.0F;
    private static final float ARCHON_FALLBACK_RADIUS = 0.8F;
    private static final long ACTIVATION_CIRCLE_GRACE_TICKS = 120L;
    private static final long SOUND_DUPLICATE_SUPPRESSION_TICKS = 3L;
    private static final double SOUND_DUPLICATE_DISTANCE_SQR = 16.0D;
    private static final long STRIKE_VISUAL_DUPLICATE_TICKS = 2L;
    private static final double STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR = 2.25D;
    private static final long ARCHON_STRIKE_DUPLICATE_TICKS = 1L;
    private static final double ARCHON_STRIKE_DUPLICATE_DISTANCE_SQR = 0.16D;
    private static final Map<Integer, ActiveSmiteStrike> activeStrikes = new LinkedHashMap<>();
    private static final Map<Integer, ActiveArchonStrike> activeArchonStrikes = new LinkedHashMap<>();
    private static long activeCircleUntilGameTime = Long.MIN_VALUE;
    private static long activeArchonCircleUntilGameTime = Long.MIN_VALUE;
    private static long lastActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastActivationSoundPosition;
    private static long lastArchonActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastArchonActivationSoundPosition;
    private static long lastStrikeSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastStrikeSoundPosition;
    private static long lastArchonStrikeSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastArchonStrikeSoundPosition;
    private static long lastStrikeVisualGameTime = Long.MIN_VALUE;
    private static long lastArchonStrikeVisualGameTime = Long.MIN_VALUE;
    private static int nextSoundStrikeId = -1;
    private static int nextArchonSoundStrikeId = -1;

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
        if (boltColor == VAULT_ARCHON_SMITE_BOLT_COLOR) {
            ArcaneBeamConfig.ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
            if (settings != null && settings.enabled) {
                long now = gameTime();
                if (!hasRecentArchonStrikeAt(impact, now)) {
                    activeArchonStrikes.computeIfAbsent(smiteBolt.getId(), id -> {
                        playArchonStrikeOnce(Minecraft.getInstance(), impact);
                        lastArchonStrikeVisualGameTime = now;
                        return new ActiveArchonStrike(
                                createArchonMissileOrigin(impact, settings),
                                impact,
                                now,
                                StormArrowVisualManager.StormArrowRenderSettings.from(settings)
                        );
                    });
                }
            }
            return true;
        }
        if (hasActiveArchonCircle(Minecraft.getInstance().player, gameTime())) {
            return true;
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
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Vec3 position = new Vec3(x, y, z);
        long now = level == null ? 0L : level.getGameTime();
        if (level != null && hasActiveArchonCircle(minecraft.player, now)) {
            return handleArchonActivationSound(minecraft, level, position, now);
        }

        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlaySmiteActivation(minecraft)) {
            return false;
        }

        activeCircleUntilGameTime = now + ACTIVATION_CIRCLE_GRACE_TICKS;
        if (isDuplicate(now, position, lastActivationSoundGameTime, lastActivationSoundPosition)) {
            return true;
        }

        ArcaneBeamSoundController.playSmiteActivation(minecraft, position);
        lastActivationSoundGameTime = now;
        lastActivationSoundPosition = position;
        return true;
    }

    private static boolean handleArchonActivationSound(Minecraft minecraft, ClientLevel level, Vec3 position, long now) {
        ArcaneBeamConfig.ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlayArchonActivation(minecraft)) {
            return false;
        }

        activeArchonCircleUntilGameTime = now + ACTIVATION_CIRCLE_GRACE_TICKS;
        if (isDuplicate(now, position, lastArchonActivationSoundGameTime, lastArchonActivationSoundPosition)) {
            return true;
        }

        ArcaneBeamSoundController.playArchonActivation(minecraft, position);
        lastArchonActivationSoundGameTime = now;
        lastArchonActivationSoundPosition = position;
        return true;
    }

    public static boolean handleSmiteStrikeSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Vec3 position = new Vec3(x, y, z);
        long now = level == null ? 0L : level.getGameTime();
        if (level != null && hasActiveArchonCircle(minecraft.player, now)) {
            return handleArchonStrikeSound(minecraft, level, position, now);
        }

        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlaySmiteStrike(minecraft)) {
            return false;
        }

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
        spawnSmiteStrikeVisual(position, now, settings);
        return true;
    }

    private static boolean handleArchonStrikeSound(Minecraft minecraft, ClientLevel level, Vec3 position, long now) {
        ArcaneBeamConfig.ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled || level == null || !ArcaneBeamSoundController.canPlayArchonStrike(minecraft)) {
            return false;
        }
        if (isArchonDuplicate(now, position, lastArchonStrikeSoundGameTime, lastArchonStrikeSoundPosition)) {
            return true;
        }

        playArchonStrikeOnce(minecraft, position);
        spawnArchonStrikeVisual(position, now, settings);
        return true;
    }

    private static void spawnSmiteStrikeVisual(Vec3 impact, long now, ArcaneBeamConfig.SmiteSettings settings) {
        if (now == lastStrikeVisualGameTime || hasRecentStrikeAt(impact, now)) {
            return;
        }

        activeStrikes.put(nextSoundStrikeId--, new ActiveSmiteStrike(
                impact,
                now,
                StormArrowVisualManager.StormArrowRenderSettings.from(settings)
        ));
        lastStrikeVisualGameTime = now;
        if (nextSoundStrikeId == Integer.MIN_VALUE) {
            nextSoundStrikeId = -1;
        }
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

    private static void spawnArchonStrikeVisual(Vec3 impact, long now, ArcaneBeamConfig.ArchonSettings settings) {
        if (hasRecentArchonStrikeAt(impact, now)) {
            return;
        }

        activeArchonStrikes.put(nextArchonSoundStrikeId--, new ActiveArchonStrike(
                createArchonMissileOrigin(impact, settings),
                impact,
                now,
                StormArrowVisualManager.StormArrowRenderSettings.from(settings)
        ));
        lastArchonStrikeVisualGameTime = now;
        if (nextArchonSoundStrikeId == Integer.MIN_VALUE) {
            nextArchonSoundStrikeId = -1;
        }
    }

    private static void playArchonStrikeOnce(Minecraft minecraft, Vec3 position) {
        long now = gameTime();
        if (isArchonDuplicate(now, position, lastArchonStrikeSoundGameTime, lastArchonStrikeSoundPosition)) {
            return;
        }
        if (ArcaneBeamSoundController.playArchonStrike(minecraft, position)) {
            lastArchonStrikeSoundGameTime = now;
            lastArchonStrikeSoundPosition = position;
        }
    }

    private static Vec3 createArchonMissileOrigin(Vec3 impact, ArcaneBeamConfig.ArchonSettings settings) {
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 base = player == null ? impact : player.position();
        Vec3 horizontal = new Vec3(impact.x - base.x, 0.0D, impact.z - base.z);
        if (horizontal.lengthSqr() < 1.0E-4D && player != null) {
            Vec3 look = player.getLookAngle();
            horizontal = new Vec3(look.x, 0.0D, look.z);
        }
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 outward = horizontal.normalize().scale(settings.missileOriginRadius);
        return new Vec3(base.x + outward.x, impact.y + Math.max(0.5F, settings.originHeight), base.z + outward.z);
    }

    private static boolean isDuplicate(long now, Vec3 position, long lastGameTime, Vec3 lastPosition) {
        return lastPosition != null
                && now - lastGameTime <= SOUND_DUPLICATE_SUPPRESSION_TICKS
                && lastPosition.distanceToSqr(position) <= SOUND_DUPLICATE_DISTANCE_SQR;
    }

    private static boolean isArchonDuplicate(long now, Vec3 position, long lastGameTime, Vec3 lastPosition) {
        return lastPosition != null
                && now - lastGameTime <= ARCHON_STRIKE_DUPLICATE_TICKS
                && lastPosition.distanceToSqr(position) <= ARCHON_STRIKE_DUPLICATE_DISTANCE_SQR;
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

    private static boolean hasRecentArchonStrikeAt(Vec3 impact, long gameTime) {
        return activeArchonStrikes.values().stream().anyMatch(strike ->
                strike.impact().distanceToSqr(impact) <= ARCHON_STRIKE_DUPLICATE_DISTANCE_SQR
                        && strike.age(gameTime, 0.0F) <= ARCHON_STRIKE_DUPLICATE_TICKS
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
            if (id != null && "the_vault".equals(id.getNamespace()) && "smite".equals(id.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveArchonCircle(LocalPlayer player, long gameTime) {
        if (player == null) {
            return false;
        }
        if (gameTime <= activeArchonCircleUntilGameTime) {
            return true;
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (id != null && "the_vault".equals(id.getNamespace()) && "smite_archon".equals(id.getPath())) {
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
            activeArchonStrikes.clear();
            activeCircleUntilGameTime = Long.MIN_VALUE;
            activeArchonCircleUntilGameTime = Long.MIN_VALUE;
            lastActivationSoundGameTime = Long.MIN_VALUE;
            lastActivationSoundPosition = null;
            lastArchonActivationSoundGameTime = Long.MIN_VALUE;
            lastArchonActivationSoundPosition = null;
            lastStrikeSoundGameTime = Long.MIN_VALUE;
            lastStrikeSoundPosition = null;
            lastArchonStrikeSoundGameTime = Long.MIN_VALUE;
            lastArchonStrikeSoundPosition = null;
            lastStrikeVisualGameTime = Long.MIN_VALUE;
            lastArchonStrikeVisualGameTime = Long.MIN_VALUE;
            nextSoundStrikeId = -1;
            nextArchonSoundStrikeId = -1;
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
        Iterator<Map.Entry<Integer, ActiveArchonStrike>> archonStrikeIterator = activeArchonStrikes.entrySet().iterator();
        while (archonStrikeIterator.hasNext()) {
            ActiveArchonStrike strike = archonStrikeIterator.next().getValue();
            if (strike.age(now, 0.0F) >= Math.max(1, strike.settings().lifetimeTicks())) {
                archonStrikeIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ArcaneBeamConfig.SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        ArcaneBeamConfig.ArchonSettings archonSettings = ArcaneBeamConfig.INSTANCE.archon;
        if ((settings == null || !settings.enabled) && (archonSettings == null || !archonSettings.enabled)) {
            activeStrikes.clear();
            activeArchonStrikes.clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        boolean showCircle = settings != null && settings.enabled && hasActiveSmiteCircle(minecraft.player, now);
        boolean showArchonCircle = archonSettings != null && archonSettings.enabled && hasActiveArchonCircle(minecraft.player, now);
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || (!showCircle && !showArchonCircle && activeStrikes.isEmpty() && activeArchonStrikes.isEmpty())) {
            return;
        }

        ActiveSmiteCircle circle = showCircle && minecraft.player != null
                ? new ActiveSmiteCircle(minecraft.player.position().add(0.0D, 0.08D, 0.0D), StormArrowVisualManager.StormArrowRenderSettings.from(settings))
                : null;
        ActiveSmiteCircle archonCircle = showArchonCircle && minecraft.player != null
                ? new ActiveSmiteCircle(minecraft.player.position().add(0.0D, 0.08D, 0.0D), StormArrowVisualManager.StormArrowRenderSettings.from(archonSettings), ARCHON_FALLBACK_RADIUS)
                : null;
        StormArrowVisualRenderer.render(
                event.getPoseStack(),
                event.getCamera().getPosition(),
                event.getPartialTick(),
                circle == null && archonCircle == null
                        ? Collections.emptyList()
                        : circle != null ? Collections.singletonList(circle) : Collections.singletonList(archonCircle),
                activeStrikes.values()
        );
        if (!activeArchonStrikes.isEmpty()) {
            ArchonMissileRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeArchonStrikes.values());
        }
    }

    public record ActiveSmiteCircle(Vec3 groundCenter, StormArrowVisualManager.StormArrowRenderSettings settings, float radius) implements StormArrowVisualRenderer.CircleVisual {
        public ActiveSmiteCircle(Vec3 groundCenter, StormArrowVisualManager.StormArrowRenderSettings settings) {
            this(groundCenter, settings, FALLBACK_RADIUS);
        }

        @Override
        public float radius() {
            return radius;
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

    public record ActiveArchonStrike(Vec3 origin, Vec3 impact, long startGameTime, StormArrowVisualManager.StormArrowRenderSettings settings) implements ArchonMissileRenderer.MissileVisual {
        public float age(long gameTime, float partialTick) {
            return Math.max(0.0F, gameTime - startGameTime + partialTick);
        }

        @Override
        public float progress(long gameTime, float partialTick) {
            return Mth.clamp(age(gameTime, partialTick) / Math.max(1.0F, settings.lifetimeTicks()), 0.0F, 1.0F);
        }
    }
}
