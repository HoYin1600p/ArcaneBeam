package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.ability.OwnershipMath;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.SoundChoice;
import dev.hoyin1600p.arcanebeam.sound.AbilitySoundClassifier;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import iskallia.vault.network.message.AbilityActivityMessage;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import iskallia.vault.network.message.ServerboundAbilityKeyMessage;
import iskallia.vault.skill.ability.effect.ArcaneAbility;
import iskallia.vault.skill.ability.effect.ArcanePrismAbility;
import iskallia.vault.skill.ability.effect.ArcaneRailAbility;
import iskallia.vault.skill.ability.effect.spi.core.Ability;
import iskallia.vault.skill.base.LearnableSkill;
import iskallia.vault.skill.base.Skill;
import iskallia.vault.skill.base.SpecializedSkill;
import iskallia.vault.skill.tree.AbilityTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.ARCANE_ABILITY_ID;

/** Tracks the local player's Arcane and Rail selection, activation and the windows that arm the replacement. */
final class LocalAbilityState {
    private LocalAbilityState() {
    }

    private static final double SOUND_OWNER_DISTANCE_SQR = 1.0D;
    private static final double SOUND_OWNER_AMBIGUITY_MARGIN = 0.25D;
    private static final int LOCAL_ARCANE_PRE_ACTIVATE_SOUND_TICKS = 20;
    private static final int LOCAL_RAIL_PRE_ACTIVATE_PARTICLE_TICKS = 20;
    private static final int LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS = 8;

    private static final LocalArcaneParticleTracker localArcaneParticles = new LocalArcaneParticleTracker();
    private static final LocalPositionHistory localPositions = new LocalPositionHistory();
    private static WeakReference<ClientLevel> trackedLevel = new WeakReference<>(null);
    private static UUID trackedLocalPlayerId;
    private static long lastArcaneSeenGameTime = Long.MIN_VALUE;
    private static boolean localArcaneActive = false;
    private static boolean localArcaneSelected = false;
    private static boolean localRailSelected = false;
    /** The specialization chosen in the Arcane group, from the synced tree; what a quickselect press casts. */
    private static ArcaneSpec localArcaneGroupSpec = ArcaneSpec.UNKNOWN;
    private static final RemoteCastEvidence castEvidence = new RemoteCastEvidence();
    private static long localArcaneSoundArmedUntilGameTime = Long.MIN_VALUE;
    private static long localArcaneParticleArmedUntilGameTime = Long.MIN_VALUE;
    private static long localRailParticleArmedUntilGameTime = Long.MIN_VALUE;
    private static long localArcaneFirstSeenGameTime = Long.MIN_VALUE;
    private static long lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
    private static long localRailFirstSeenGameTime = Long.MIN_VALUE;
    private static long localRailLatchedUntilGameTime = Long.MIN_VALUE;

    static void recordLocalArcanePose(Minecraft minecraft, long gameTime) {
        // Include both tick endpoints: movement packets can describe either pose by the time
        // the server's particles return. Use the server origin, never the configurable hand offset.
        for (float partialTick : new float[]{0.0F, 1.0F}) {
            localArcaneParticles.record(gameTime, BeamTracer.serverBeamStart(minecraft.player, partialTick),
                    minecraft.player.getViewVector(partialTick));
        }
    }

    static void ensureClientContext(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            // Disconnected: drop everything including the remembered ability selection.
            if (trackedLevel.get() != null || trackedLocalPlayerId != null) {
                ArcaneBeamManager.clearAllState(false);
                trackedLevel = new WeakReference<>(null);
                trackedLocalPlayerId = null;
            }
            return;
        }
        UUID playerId = minecraft.player == null ? null : minecraft.player.getUUID();
        boolean playerChanged = playerId != null && trackedLocalPlayerId != null && !playerId.equals(trackedLocalPlayerId);
        if (trackedLevel.get() != level || playerChanged) {
            // A respawn or dimension swap keeps the ability selection for the same player;
            // the next AbilityKnownOnes sync would otherwise be the only way to restore it.
            ArcaneBeamManager.clearAllState(!playerChanged);
            trackedLevel = new WeakReference<>(level);
        }
        if (playerId != null) {
            trackedLocalPlayerId = playerId;
        }
    }

    static boolean isLocalArcaneReplacementWindow() {
        return ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)
                && (localArcaneActive
                || ArcaneBeamManager.getLocalActiveBeam(BeamKind.ARCANE) != null
                || isWithinRecentWindow(lastLocalArcaneDeactivateGameTime, LOCAL_ARCANE_POST_DEACTIVATE_IGNORE_TICKS));
    }

    static void syncLocalAbilityState(AbilityKnownOnesMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureClientContext(minecraft);
        if (minecraft.level == null || minecraft.player == null || message == null || message.getTree() == null) {
            return;
        }

        boolean[] active = new boolean[]{false};
        message.getTree().iterate(ArcaneAbility.class, ability -> active[0] |= ability.isActive());
        message.getTree().getSelectedAbility().ifPresentOrElse(ability -> {
            localArcaneSelected = ability.getClass() == ArcaneAbility.class;
            localRailSelected = ability instanceof ArcaneRailAbility;
        }, () -> {
            localArcaneSelected = false;
            localRailSelected = false;
        });
        localArcaneGroupSpec = readArcaneGroupSpec(message.getTree());
        setLocalArcaneActive(active[0]);
    }

    /** Reads which Arcane specialization (Base, Rail or Prism) the synced tree has selected in the Arcane group. */
    private static ArcaneSpec readArcaneGroupSpec(AbilityTree tree) {
        try {
            Skill group = tree.getForId(ARCANE_ABILITY_ID).orElse(null);
            if (!(group instanceof SpecializedSkill specialized)) {
                return ArcaneSpec.UNKNOWN;
            }
            LearnableSkill chosen = specialized.getSpecialization();
            if (chosen == null) {
                return ArcaneSpec.UNKNOWN;
            }
            boolean[] found = new boolean[3];
            chosen.iterate(ArcaneRailAbility.class, ability -> found[0] = true);
            chosen.iterate(ArcanePrismAbility.class, ability -> found[1] = true);
            chosen.iterate(ArcaneAbility.class, ability -> found[2] = true);
            return ArcaneSpec.of(found[0], found[1], found[2]);
        } catch (RuntimeException error) {
            return ArcaneSpec.UNKNOWN;
        }
    }

    /** True while the Arcane group's chosen specialization is Prism, which Vault keeps drawing itself. */
    static boolean isLocalPrismSelected() {
        return localArcaneGroupSpec == ArcaneSpec.PRISM;
    }

    static void observeLocalAbilityKey(ServerboundAbilityKeyMessage.Opcode opcode) {
        if (opcode != ServerboundAbilityKeyMessage.Opcode.KeyDown) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ensureClientContext(minecraft);
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (localArcaneSelected && ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)) {
            localArcaneParticleArmedUntilGameTime = minecraft.level.getGameTime() + 20L;
            recordLocalArcanePose(minecraft, minecraft.level.getGameTime());
        }
        if (localArcaneSelected
                && ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)
                && soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound) != SoundChoice.DEFAULT) {
            localArcaneSoundArmedUntilGameTime = minecraft.level.getGameTime() + LOCAL_ARCANE_PRE_ACTIVATE_SOUND_TICKS;
        }
        if (localRailSelected && ArcaneBeamManager.replacementEnabled(BeamKind.RAIL)) {
            localRailParticleArmedUntilGameTime = minecraft.level.getGameTime() + LOCAL_RAIL_PRE_ACTIVATE_PARTICLE_TICKS;
        }
    }

    /**
     * A quickselect press casts the group's chosen specialization directly, so only that specialization's
     * window is armed: Base arms Arcane, Rail arms Rail, Prism (Vault's own visuals) and an unsynced
     * selection arm nothing.
     */
    static void observeLocalQuickselect(String abilityId) {
        if (abilityId == null || !abilityId.equalsIgnoreCase(ARCANE_ABILITY_ID)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ensureClientContext(minecraft);
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        long gameTime = minecraft.level.getGameTime();
        ArcaneSpec spec = localArcaneGroupSpec;
        if (spec.armsArcaneOnQuickselect() && ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)) {
            localArcaneParticleArmedUntilGameTime = gameTime + 20L;
            recordLocalArcanePose(minecraft, gameTime);
            if (soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound) != SoundChoice.DEFAULT) {
                localArcaneSoundArmedUntilGameTime = gameTime + LOCAL_ARCANE_PRE_ACTIVATE_SOUND_TICKS;
            }
        }
        if (spec.armsRailOnQuickselect() && ArcaneBeamManager.replacementEnabled(BeamKind.RAIL)) {
            localRailParticleArmedUntilGameTime = gameTime + LOCAL_RAIL_PRE_ACTIVATE_PARTICLE_TICKS;
        }
    }

    static void syncLocalAbilityActivity(AbilityActivityMessage message) {
        if (message == null || !isBaseArcaneAbility(message.getAbility())) {
            return;
        }
        if (message.getActiveFlag() == Ability.ActivityFlag.ACTIVATE_ABILITY) {
            setLocalArcaneActive(true);
        } else if (message.getActiveFlag() == Ability.ActivityFlag.DEACTIVATE_ABILITY) {
            setLocalArcaneActive(false);
        }
    }

    static void setLocalArcaneActive(boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureClientContext(minecraft);
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
            if (ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)) {
                localArcaneParticleArmedUntilGameTime = Long.MIN_VALUE;
                recordLocalArcanePose(minecraft, gameTime);
                ArcaneBeamManager.putLocalBeam(BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime);
            } else {
                ArcaneBeamManager.removeLocalBeam(BeamKind.ARCANE);
            }
        } else if (localArcaneActive || ArcaneBeamManager.getLocalActiveBeam(BeamKind.ARCANE) != null) {
            localArcaneParticleArmedUntilGameTime = Long.MIN_VALUE;
            localArcaneActive = false;
            localArcaneFirstSeenGameTime = Long.MIN_VALUE;
            lastLocalArcaneDeactivateGameTime = gameTime;
            ArcaneBeamManager.removeLocalBeam(BeamKind.ARCANE);
        }
    }

    static boolean isBaseArcaneAbility(String abilityId) {
        if (abilityId == null) {
            return false;
        }
        String normalized = abilityId.toLowerCase(Locale.ROOT);
        return normalized.contains("arcane") && !normalized.contains("rail") && !normalized.contains("prism");
    }

    private static void recordCastEvidence(BeamKind kind, SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            castEvidence.record(kind, new Vec3(sound.getX(), sound.getY(), sound.getZ()), minecraft.level.getGameTime());
        }
    }

    /** True when an Arcane/Rail cast sound of this kind was heard recently at this player's position. */
    static boolean hasCastEvidence(BeamKind kind, Vec3 playerPosition, long gameTime) {
        return castEvidence.hasEvidence(kind, playerPosition, gameTime);
    }

    static boolean handleArcaneCastSound(SoundInstance sound) {
        if (AbilitySoundClassifier.isVaultArcaneSound(sound)) {
            recordCastEvidence(BeamKind.ARCANE, sound);
        }
        if (!ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)
                || soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound) == SoundChoice.DEFAULT
                || !AbilitySoundClassifier.isVaultArcaneSound(sound)
                || !isLocalSoundOwner(sound)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return ArcaneBeamSoundController.canPlayArcaneSelection(minecraft)
                && (isLocalArcaneReplacementWindow()
                || minecraft.level != null && minecraft.level.getGameTime() <= localArcaneSoundArmedUntilGameTime);
    }

    static boolean handleRailCastSound(SoundInstance sound) {
        if (AbilitySoundClassifier.isVaultRailSound(sound)) {
            recordCastEvidence(BeamKind.RAIL, sound);
        }
        if (!ArcaneBeamManager.replacementEnabled(BeamKind.RAIL)
                || !AbilitySoundClassifier.isVaultRailSound(sound)
                || !isLocalSoundOwner(sound)
                || !ArcaneBeamSoundController.canPlayRailSelection(Minecraft.getInstance())) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        latchLocalRail(minecraft.level.getGameTime());
        return soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound) != SoundChoice.DEFAULT;
    }

    static boolean isLocalSoundOwner(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        Vec3 soundPosition = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        UUID localId = minecraft.player.getUUID();
        // Vault plays the sound at the caster's server position, which trails the client while moving: compare
        // the local player against its recent positions too, other players against where they are now.
        double[] distances = new double[minecraft.level.players().size() + 1];
        distances[0] = localPositions.nearestDistanceSqr(minecraft.player.position(), soundPosition,
                minecraft.level.getGameTime());
        int count = 1;
        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (!player.getUUID().equals(localId) && count < distances.length) {
                distances[count++] = player.position().distanceToSqr(soundPosition);
            }
        }
        return OwnershipMath.isUniqueNearest(Arrays.copyOf(distances, count), 0,
                SOUND_OWNER_DISTANCE_SQR, SOUND_OWNER_AMBIGUITY_MARGIN);
    }

    static void latchLocalRail(long gameTime) {
        if (!ArcaneBeamManager.replacementEnabled(BeamKind.RAIL)) {
            return;
        }
        BeamSettings settings = BeamKind.RAIL.settings();
        long latchTicks = Math.max(Math.max(settings.lifetimeTicks, settings.fadeOutTicks), 2);
        if (gameTime > localRailLatchedUntilGameTime) {
            localRailFirstSeenGameTime = gameTime;
        }
        localRailLatchedUntilGameTime = gameTime + latchTicks;
        ArcaneBeamManager.putLocalBeam(BeamKind.RAIL, localRailFirstSeenGameTime, gameTime);
    }

    static boolean shouldSuppressAbilityCooldownSound() {
        return ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE) && isWithinRecentWindow(lastArcaneSeenGameTime, 5L);
    }

    static boolean isWithinRecentWindow(long lastSeenGameTime, long windowTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        long gameTime = minecraft.level.getGameTime();
        return gameTime - lastSeenGameTime >= 0L && gameTime - lastSeenGameTime <= windowTicks;
    }

    static SoundChoice soundChoice(String id) {
        SoundChoice choice = SoundChoice.fromId(id);
        return choice == null ? SoundChoice.DEFAULT : choice;
    }

    static boolean isLocalRailLatched(long gameTime) {
        return gameTime <= localRailLatchedUntilGameTime;
    }

    static boolean shouldSuppressArcaneParticle(UUID localPlayerId, UUID ownerId, long gameTime, Vec3 particlePosition) {
        boolean window = ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)
                && LocalArcaneParticleTracker.suppressionWindowOpen(isLocalArcaneReplacementWindow(), gameTime,
                localArcaneParticleArmedUntilGameTime);
        return localArcaneParticles.shouldSuppress(window, localPlayerId, ownerId, gameTime, particlePosition);
    }

    static boolean isRailParticleArmedOrLatched(long gameTime) {
        return gameTime <= localRailParticleArmedUntilGameTime || isLocalRailLatched(gameTime);
    }

    static void acquireRailFromParticle(long gameTime) {
        localRailParticleArmedUntilGameTime = Long.MIN_VALUE;
        latchLocalRail(gameTime);
    }

    /** Per-tick upkeep of the local beams while the player is in a world. */
    static void tick(Minecraft minecraft, long gameTime) {
        localPositions.record(gameTime, minecraft.player.position());
        if (ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)
                && (localArcaneActive || gameTime <= localArcaneParticleArmedUntilGameTime)) {
            recordLocalArcanePose(minecraft, gameTime);
        }
        if (ArcaneBeamManager.usesResourcePackParticles(BeamKind.ARCANE, BeamKind.ARCANE.settings())) {
            ArcaneBeamManager.removeLocalBeam(BeamKind.ARCANE);
        } else if (localArcaneActive && ArcaneBeamManager.replacementEnabled(BeamKind.ARCANE)) {
            if (localArcaneFirstSeenGameTime == Long.MIN_VALUE) {
                localArcaneFirstSeenGameTime = gameTime;
            }
            ArcaneBeamManager.putLocalBeam(BeamKind.ARCANE, localArcaneFirstSeenGameTime, gameTime);
            lastArcaneSeenGameTime = gameTime;
        }

        if (isLocalRailLatched(gameTime)) {
            if (localRailFirstSeenGameTime == Long.MIN_VALUE) {
                localRailFirstSeenGameTime = gameTime;
            }
            ArcaneBeamManager.putLocalBeam(BeamKind.RAIL, localRailFirstSeenGameTime, gameTime);
        }
    }

    /** Forgets the local state tied to one disabled replacement; the caller has already removed its beam. */
    static void resetReplacement(BeamKind kind) {
        if (kind == BeamKind.ARCANE) {
            localArcaneParticles.clear();
            localArcaneParticleArmedUntilGameTime = Long.MIN_VALUE;
            localArcaneFirstSeenGameTime = Long.MIN_VALUE;
            lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
            lastArcaneSeenGameTime = Long.MIN_VALUE;
        } else {
            localRailFirstSeenGameTime = Long.MIN_VALUE;
            localRailLatchedUntilGameTime = Long.MIN_VALUE;
            localRailParticleArmedUntilGameTime = Long.MIN_VALUE;
        }
    }

    static void reset(boolean keepSelections) {
        localArcaneParticles.clear();
        localPositions.clear();
        localArcaneParticleArmedUntilGameTime = Long.MIN_VALUE;
        localArcaneActive = false;
        castEvidence.clear();
        if (!keepSelections) {
            localArcaneSelected = false;
            localRailSelected = false;
            localArcaneGroupSpec = ArcaneSpec.UNKNOWN;
        }
        localArcaneSoundArmedUntilGameTime = Long.MIN_VALUE;
        localRailParticleArmedUntilGameTime = Long.MIN_VALUE;
        localArcaneFirstSeenGameTime = Long.MIN_VALUE;
        lastLocalArcaneDeactivateGameTime = Long.MIN_VALUE;
        localRailFirstSeenGameTime = Long.MIN_VALUE;
        localRailLatchedUntilGameTime = Long.MIN_VALUE;
        lastArcaneSeenGameTime = Long.MIN_VALUE;
    }
}
