package dev.hoyin1600p.arcanebeam.ability.smite;

import dev.hoyin1600p.arcanebeam.ability.OwnershipMath;
import dev.hoyin1600p.arcanebeam.ability.lightning.LightningStrikeShockwaveManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualManager;
import dev.hoyin1600p.arcanebeam.ability.stormarrow.StormArrowVisualRenderer;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.vault.VaultFields;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.config.model.ArchonSettings;
import dev.hoyin1600p.arcanebeam.config.model.SmiteSettings;
import iskallia.vault.skill.ability.effect.SmiteAbility;
import iskallia.vault.skill.ability.effect.SmiteArchonAbility;
import iskallia.vault.skill.ability.effect.SmiteThunderstormAbility;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.VAULT_NAMESPACE;

/**
 * Replaces the visuals and sounds of Smite and Smite: Archon for the bolts and sounds that belong to them.
 *
 * <p>Ownership rules (nothing here is claimed by proximity alone):
 * <ul>
 *   <li>Which variant a bolt belongs to is decided by {@link SmiteBoltClassifier}: the colour the server
 *       configured (synced with the ability tree), then the local player's active Smite effect
 *       ({@code the_vault:smite}, {@code smite_archon}, {@code smite_thunderstorm}) for bolts landing within
 *       casting reach of the local player. Smite: Thunderstorm is never replaced and stays Vault's.</li>
 *   <li>Remote players' effects are not synced to this client by vanilla (only a snapshot when they come into
 *       view), so a remote caster's bolts are claimed only when their colour names exactly one variant.</li>
 *   <li>The activation sound and targeting circle belong to the local player only: the sound must be the
 *       local player's own (unique nearest player) while the local player has the matching effect active.</li>
 *   <li>A {@code smite_bolt} sound is claimed only when the local player has Smite or Archon active, it is
 *       within casting reach, no Storm Arrow storm owns it, and no recently seen bolt at the same spot was left
 *       to Vault. Overlapping duplicates (sound and bolt of one strike) are merged by a tight time and distance
 *       window.</li>
 * </ul>
 */
public final class SmiteVisualManager {
    private static final float FALLBACK_RADIUS = 5.0F;
    private static final float ARCHON_FALLBACK_RADIUS = 0.8F;
    /** Bolts land on mobs around the caster; this is how far from the local player one may be and still be theirs. */
    private static final double CASTER_REACH_HORIZONTAL = 24.0D;
    private static final double CASTER_REACH_VERTICAL = 16.0D;
    /** Vault plays the toggle sound at the caster, so it must be within this distance of the local player. */
    private static final double ACTIVATION_SOUND_OWNER_DISTANCE_SQR = 6.25D;
    private static final double ACTIVATION_SOUND_AMBIGUITY_MARGIN_SQR = 0.25D;
    private static final long SOUND_DUPLICATE_SUPPRESSION_TICKS = 3L;
    private static final double SOUND_DUPLICATE_DISTANCE_SQR = 16.0D;
    private static final long STRIKE_VISUAL_DUPLICATE_TICKS = 4L;
    private static final double STRIKE_VISUAL_DUPLICATE_DISTANCE_SQR = 4.0D;
    private static final long ARCHON_STRIKE_DUPLICATE_TICKS = 1L;
    private static final double ARCHON_STRIKE_DUPLICATE_DISTANCE_SQR = 0.16D;
    /** A bolt and the sound of the same strike arrive together at the same spot. */
    private static final long BOLT_SOUND_MATCH_TICKS = 3L;
    private static final double BOLT_SOUND_MATCH_DISTANCE_SQR = 2.25D;
    private static final int MAX_RECENT_BOLTS = 24;
    private static final Map<Integer, ActiveSmiteStrike> activeStrikes = new LinkedHashMap<>();
    private static final Map<Integer, ActiveArchonStrike> activeArchonStrikes = new LinkedHashMap<>();
    private static final Map<Integer, SmiteBoltClassifier.BoltClass> boltClasses = new HashMap<>();
    private static final List<RecentBolt> recentBolts = new ArrayList<>();
    private static SmiteColorTable colorTable = new SmiteColorTable();
    private static long cachedVariantGameTime = Long.MIN_VALUE;
    private static SmiteVariant cachedVariant;
    private static long lastActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastActivationSoundPosition;
    private static long lastArchonActivationSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastArchonActivationSoundPosition;
    private static long lastStrikeSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastStrikeSoundPosition;
    private static long lastArchonStrikeSoundGameTime = Long.MIN_VALUE;
    private static Vec3 lastArchonStrikeSoundPosition;
    private static int nextSoundStrikeId = -1;
    private static int nextArchonSoundStrikeId = -1;

    private SmiteVisualManager() {
    }

    /** Reads the bolt colour of every Smite variant from the ability tree the server synced to this client. */
    public static void observeAbilityTree(AbilityTree tree) {
        if (tree == null) {
            return;
        }
        SmiteColorTable fresh = new SmiteColorTable();
        try {
            tree.iterate(SmiteAbility.class, ability -> fresh.add(SmiteVariant.SMITE, ability.getColor()));
            tree.iterate(SmiteArchonAbility.class, ability -> fresh.add(SmiteVariant.ARCHON, ability.getColor()));
            tree.iterate(SmiteThunderstormAbility.class, ability -> fresh.add(SmiteVariant.THUNDERSTORM, ability.getColor()));
        } catch (RuntimeException error) {
            return;
        }
        colorTable = fresh;
        boltClasses.clear();
    }

    public static boolean handleSmiteBoltRender(Entity smiteBolt) {
        if (smiteBolt == null || !smiteBolt.level.isClientSide) {
            return false;
        }

        Vec3 impact = smiteBolt.position();
        long now = gameTime();
        SmiteBoltClassifier.BoltClass boltClass = boltClasses.get(smiteBolt.getId());
        if (boltClass == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            SmiteVariant local = localVariant(player, now);
            boolean near = local != null && withinCasterReach(player, impact);
            boltClass = SmiteBoltClassifier.classify(smiteBoltColor(smiteBolt), colorTable, local, near);
            boltClasses.put(smiteBolt.getId(), boltClass);
            noteBolt(impact, now, boltClass == SmiteBoltClassifier.BoltClass.SMITE || boltClass == SmiteBoltClassifier.BoltClass.ARCHON);
        }

        switch (boltClass) {
            case LIGHTNING_DEFAULT:
                // Lightning Strike's AOE reuses Vault's smite bolt entity without applying a Smite colour.
                return LightningStrikeShockwaveManager.shouldSuppressDefaultVaultLightningVisual(impact);
            case ARCHON:
                return renderArchonBolt(smiteBolt, impact, now);
            case SMITE:
                return renderSmiteBolt(smiteBolt, impact, now);
            default:
                // Thunderstorm, other players' unidentifiable bolts and foreign bolts stay Vault's.
                return false;
        }
    }

    private static boolean renderArchonBolt(Entity smiteBolt, Vec3 impact, long now) {
        ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled) {
            return false;
        }
        if (!hasRecentArchonStrikeAt(impact, now)) {
            activeArchonStrikes.computeIfAbsent(smiteBolt.getId(), id -> {
                playArchonStrikeOnce(Minecraft.getInstance(), impact);
                return new ActiveArchonStrike(
                        createArchonMissileOrigin(impact, settings),
                        impact,
                        now,
                        StormArrowVisualManager.StormArrowRenderSettings.from(settings)
                );
            });
        }
        return true;
    }

    private static boolean renderSmiteBolt(Entity smiteBolt, Vec3 impact, long now) {
        SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled) {
            return false;
        }
        if (hasRecentStrikeAt(impact, now)) {
            return true;
        }
        activeStrikes.computeIfAbsent(smiteBolt.getId(), id -> {
            playSmiteStrikeOnce(Minecraft.getInstance(), impact);
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
            Integer color = smiteBolt.getEntityData().get(VaultFields.smiteBoltColorAccessor());
            return color == null ? 0 : color;
        } catch (RuntimeException error) {
            return 0;
        }
    }

    /**
     * The Smite variant the local player has active, read from their mob effects (once per tick). Vault's toggle
     * adds the effect before it plays the toggle sound, so at sound time the effect is already present when
     * switching on and already gone when switching off.
     */
    private static SmiteVariant localVariant(LocalPlayer player, long now) {
        if (player == null) {
            return null;
        }
        if (cachedVariantGameTime == now) {
            return cachedVariant;
        }
        SmiteVariant found = null;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (id != null && VAULT_NAMESPACE.equals(id.getNamespace())) {
                SmiteVariant variant = SmiteVariant.fromEffectPath(id.getPath());
                if (variant != null) {
                    found = variant;
                    break;
                }
            }
        }
        cachedVariantGameTime = now;
        cachedVariant = found;
        return found;
    }

    private static boolean withinCasterReach(LocalPlayer player, Vec3 position) {
        return player != null && OwnershipMath.withinReach(position.x - player.getX(), position.y - player.getY(),
                position.z - player.getZ(), CASTER_REACH_HORIZONTAL, CASTER_REACH_VERTICAL);
    }

    /** True when the sound is the local player's own: the unique nearest player to it, close by. */
    private static boolean isLocalActivationSound(ClientLevel level, LocalPlayer player, Vec3 position) {
        List<AbstractClientPlayer> players = level.players();
        double[] distances = new double[players.size()];
        int localIndex = -1;
        for (int index = 0; index < distances.length; index++) {
            AbstractClientPlayer candidate = players.get(index);
            distances[index] = candidate.position().distanceToSqr(position);
            if (candidate.getUUID().equals(player.getUUID())) {
                localIndex = index;
            }
        }
        return OwnershipMath.isUniqueNearest(distances, localIndex, ACTIVATION_SOUND_OWNER_DISTANCE_SQR,
                ACTIVATION_SOUND_AMBIGUITY_MARGIN_SQR);
    }

    public static boolean handleSmiteActivationSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return false;
        }
        long now = level.getGameTime();
        SmiteVariant variant = localVariant(player, now);
        Vec3 position = new Vec3(x, y, z);
        // Only the local player's own toggle (their effect active, their position) is replaced; a teammate's
        // toggle sound, the sound of switching off, and Thunderstorm (still Vault's) all keep Vault's sound.
        if (variant == null || variant == SmiteVariant.THUNDERSTORM || !isLocalActivationSound(level, player, position)) {
            return false;
        }
        if (variant == SmiteVariant.ARCHON) {
            return handleArchonActivationSound(minecraft, position, now);
        }

        SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlaySmiteActivation(minecraft)) {
            return false;
        }
        if (isDuplicate(now, position, lastActivationSoundGameTime, lastActivationSoundPosition)) {
            return true;
        }

        ArcaneBeamSoundController.playSmiteActivation(minecraft, position);
        lastActivationSoundGameTime = now;
        lastActivationSoundPosition = position;
        return true;
    }

    private static boolean handleArchonActivationSound(Minecraft minecraft, Vec3 position, long now) {
        ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlayArchonActivation(minecraft)) {
            return false;
        }
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
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return false;
        }
        long now = level.getGameTime();
        SmiteVariant variant = localVariant(player, now);
        if (variant == null || variant == SmiteVariant.THUNDERSTORM) {
            return false;
        }
        Vec3 position = new Vec3(x, y, z);
        if (!withinCasterReach(player, position) || StormArrowVisualManager.ownsBoltSound(position)) {
            return false;
        }
        RecentBolt bolt = recentBoltAt(position, now);
        if (bolt != null && !bolt.claimed()) {
            // A bolt seen at this spot was left to Vault (another ability's or player's), so its sound is too.
            return false;
        }
        return variant == SmiteVariant.ARCHON
                ? handleArchonStrikeSound(minecraft, position, now)
                : handleSmiteVariantStrikeSound(minecraft, position, now);
    }

    private static boolean handleSmiteVariantStrikeSound(Minecraft minecraft, Vec3 position, long now) {
        SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        if (settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlaySmiteStrike(minecraft)) {
            return false;
        }

        if (isDuplicate(now, position, lastStrikeSoundGameTime, lastStrikeSoundPosition)) {
            return true;
        }
        if (hasRecentStrikeAt(position, now)) {
            return true;
        }

        playSmiteStrikeOnce(minecraft, position);
        spawnSmiteStrikeVisual(position, now, settings);
        return true;
    }

    private static boolean handleArchonStrikeSound(Minecraft minecraft, Vec3 position, long now) {
        ArchonSettings settings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled || !ArcaneBeamSoundController.canPlayArchonStrike(minecraft)) {
            return false;
        }
        if (isArchonDuplicate(now, position, lastArchonStrikeSoundGameTime, lastArchonStrikeSoundPosition)) {
            return true;
        }

        playArchonStrikeOnce(minecraft, position);
        spawnArchonStrikeVisual(position, now, settings);
        return true;
    }

    private static void spawnSmiteStrikeVisual(Vec3 impact, long now, SmiteSettings settings) {
        if (hasRecentStrikeAt(impact, now)) {
            return;
        }

        activeStrikes.put(nextSoundStrikeId--, new ActiveSmiteStrike(
                impact,
                now,
                StormArrowVisualManager.StormArrowRenderSettings.from(settings)
        ));
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

    private static void spawnArchonStrikeVisual(Vec3 impact, long now, ArchonSettings settings) {
        if (hasRecentArchonStrikeAt(impact, now)) {
            return;
        }

        activeArchonStrikes.put(nextArchonSoundStrikeId--, new ActiveArchonStrike(
                createArchonMissileOrigin(impact, settings),
                impact,
                now,
                StormArrowVisualManager.StormArrowRenderSettings.from(settings)
        ));
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

    private static Vec3 createArchonMissileOrigin(Vec3 impact, ArchonSettings settings) {
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

    private static void noteBolt(Vec3 position, long now, boolean claimed) {
        recentBolts.add(new RecentBolt(position, now, claimed));
        while (recentBolts.size() > MAX_RECENT_BOLTS) {
            recentBolts.remove(0);
        }
    }

    private static RecentBolt recentBoltAt(Vec3 position, long now) {
        for (int index = recentBolts.size() - 1; index >= 0; index--) {
            RecentBolt bolt = recentBolts.get(index);
            if (now - bolt.gameTime() <= BOLT_SOUND_MATCH_TICKS
                    && bolt.position().distanceToSqr(position) <= BOLT_SOUND_MATCH_DISTANCE_SQR) {
                return bolt;
            }
        }
        return null;
    }

    private record RecentBolt(Vec3 position, long gameTime, boolean claimed) {
    }

    /** Drops all per-level state; called when the client level instance changes. */
    public static void clear() {
        activeStrikes.clear();
        activeArchonStrikes.clear();
        boltClasses.clear();
        recentBolts.clear();
        cachedVariantGameTime = Long.MIN_VALUE;
        cachedVariant = null;
        lastActivationSoundGameTime = Long.MIN_VALUE;
        lastActivationSoundPosition = null;
        lastArchonActivationSoundGameTime = Long.MIN_VALUE;
        lastArchonActivationSoundPosition = null;
        lastStrikeSoundGameTime = Long.MIN_VALUE;
        lastStrikeSoundPosition = null;
        lastArchonStrikeSoundGameTime = Long.MIN_VALUE;
        lastArchonStrikeSoundPosition = null;
        nextSoundStrikeId = -1;
        nextArchonSoundStrikeId = -1;
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
        recentBolts.removeIf(bolt -> now - bolt.gameTime() > BOLT_SOUND_MATCH_TICKS + 1L || now < bolt.gameTime());
        // Entity ids are reused by the client; classifications only need to outlive one bolt.
        if (boltClasses.size() > 64 || now % 100L == 0L) {
            boltClasses.clear();
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Render events fire for a dozen stages per frame; everything below only matters for one of them.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        SmiteSettings settings = ArcaneBeamConfig.INSTANCE.smite;
        ArchonSettings archonSettings = ArcaneBeamConfig.INSTANCE.archon;
        if (settings == null || !settings.enabled) {
            activeStrikes.clear();
        }
        if (archonSettings == null || !archonSettings.enabled) {
            activeArchonStrikes.clear();
        }
        if ((settings == null || !settings.enabled) && (archonSettings == null || !archonSettings.enabled)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        SmiteVariant variant = localVariant(minecraft.player, now);
        boolean showCircle = settings != null && settings.enabled && variant == SmiteVariant.SMITE;
        boolean showArchonCircle = archonSettings != null && archonSettings.enabled && variant == SmiteVariant.ARCHON;
        if (!showCircle && !showArchonCircle && activeStrikes.isEmpty() && activeArchonStrikes.isEmpty()) {
            return;
        }

        ActiveSmiteCircle circle = showCircle && minecraft.player != null
                ? new ActiveSmiteCircle(minecraft.player, StormArrowVisualManager.StormArrowRenderSettings.from(settings))
                : null;
        ActiveSmiteCircle archonCircle = showArchonCircle && minecraft.player != null
                ? new ActiveSmiteCircle(minecraft.player, StormArrowVisualManager.StormArrowRenderSettings.from(archonSettings), ARCHON_FALLBACK_RADIUS)
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

    public record ActiveSmiteCircle(LocalPlayer player, StormArrowVisualManager.StormArrowRenderSettings settings, float radius) implements StormArrowVisualRenderer.CircleVisual {
        public ActiveSmiteCircle(LocalPlayer player, StormArrowVisualManager.StormArrowRenderSettings settings) {
            this(player, settings, FALLBACK_RADIUS);
        }

        @Override
        public Vec3 groundCenter(float partialTick) {
            return new Vec3(
                    Mth.lerp((double) partialTick, player.xOld, player.getX()),
                    Mth.lerp((double) partialTick, player.yOld, player.getY()) + 0.08D,
                    Mth.lerp((double) partialTick, player.zOld, player.getZ())
            );
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
