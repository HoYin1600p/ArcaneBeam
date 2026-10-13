package dev.hoyin1600p.arcanebeam.sound;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.LightningSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.SoundChoice;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowProjectileSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSoundMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class ArcaneBeamSoundController {
    private static final int ARCANE_OPTION_2_STARTUP_TICKS = 49;
    private static final String ARCANE_1_PATH = "abilities/arcane_1";
    private static final String ARCANE_2_STARTUP_PATH = "abilities/arcane_2_startup";
    private static final String ARCANE_2_LOOP_PATH = "abilities/arcane_2_loop";
    private static final String ARCANE_RESOURCEPACK_1_PATH = "abilities/arcane_resourcepack_1";
    private static final String ARCANE_RESOURCEPACK_2_PATH = "abilities/arcane_resourcepack_2";
    private static final String RAIL_1_PATH = "abilities/rail_1";
    private static final String RAIL_2_PATH = "abilities/rail_2";
    private static final String RAIL_RESOURCEPACK_1_PATH = "abilities/rail_resourcepack_1";
    private static final String RAIL_RESOURCEPACK_2_PATH = "abilities/rail_resourcepack_2";
    private static final String LIGHTNING_SEISMIC_CHARGE_CAST_PATH = "abilities/lightning_seismic_charge_cast";
    private static final String LIGHTNING_SEISMIC_CHARGE_IMPACT_PATH = "abilities/lightning_seismic_charge_impact";
    private static final String LIGHTNING_RESOURCEPACK_1_CAST_PATH = "abilities/lightning_resourcepack_1_cast";
    private static final String LIGHTNING_RESOURCEPACK_1_IMPACT_PATH = "abilities/lightning_resourcepack_1_impact";
    private static final String LIGHTNING_RESOURCEPACK_2_CAST_PATH = "abilities/lightning_resourcepack_2_cast";
    private static final String LIGHTNING_RESOURCEPACK_2_IMPACT_PATH = "abilities/lightning_resourcepack_2_impact";
    private static final String VAULT_ALTAR_BEAM_PATH = "abilities/vault_altar_beam";
    private static final String VAULT_ALTAR_RESOURCEPACK_1_PATH = "abilities/vault_altar_resourcepack_1";
    private static final String VAULT_ALTAR_RESOURCEPACK_2_PATH = "abilities/vault_altar_resourcepack_2";
    private static final String STORM_ARROW_1_PATH = "abilities/storm_arrow_1";
    private static final String STORM_ARROW_PROJECTILE_PATH = "abilities/storm_arrow_projectile";
    private static final String STORM_ARROW_RESOURCEPACK_1_PATH = "abilities/storm_arrow_resourcepack_1";
    private static final String STORM_ARROW_RESOURCEPACK_2_PATH = "abilities/storm_arrow_resourcepack_2";
    private static final String STORM_ARROW_PROJECTILE_RESOURCEPACK_1_PATH = "abilities/storm_arrow_projectile_resourcepack_1";
    private static final String STORM_ARROW_PROJECTILE_RESOURCEPACK_2_PATH = "abilities/storm_arrow_projectile_resourcepack_2";
    private static final String SMITE_1_PATH = "abilities/smite_1";
    private static final String SMITE_ACTIVATION_PATH = "abilities/smite_activation";
    private static final String SMITE_RESOURCEPACK_1_PATH = "abilities/smite_resourcepack_1";
    private static final String SMITE_RESOURCEPACK_2_PATH = "abilities/smite_resourcepack_2";
    private static final String SMITE_ACTIVATION_RESOURCEPACK_1_PATH = "abilities/smite_activation_resourcepack_1";
    private static final String SMITE_ACTIVATION_RESOURCEPACK_2_PATH = "abilities/smite_activation_resourcepack_2";
    private static final String ARCHON_STRIKE_PATH = "abilities/archon_strike";
    private static final String ARCHON_ACTIVATION_PATH = "abilities/archon_activation";
    private static final String ARCHON_RESOURCEPACK_1_PATH = "abilities/archon_resourcepack_1";
    private static final String ARCHON_RESOURCEPACK_2_PATH = "abilities/archon_resourcepack_2";
    private static final String ARCHON_ACTIVATION_RESOURCEPACK_1_PATH = "abilities/archon_activation_resourcepack_1";
    private static final String ARCHON_ACTIVATION_RESOURCEPACK_2_PATH = "abilities/archon_activation_resourcepack_2";
    private static final String VAULT_ALTAR_BEAM_EVENT = "vault_altar_beam";
    private static final String VAULT_ALTAR_RESOURCEPACK_1_EVENT = "vault_altar_resourcepack_1";
    private static final String VAULT_ALTAR_RESOURCEPACK_2_EVENT = "vault_altar_resourcepack_2";
    private static final String STORM_ARROW_1_EVENT = "storm_arrow_1";
    private static final String STORM_ARROW_PROJECTILE_EVENT = "storm_arrow_projectile";
    private static final String STORM_ARROW_RESOURCEPACK_1_EVENT = "storm_arrow_resourcepack_1";
    private static final String STORM_ARROW_RESOURCEPACK_2_EVENT = "storm_arrow_resourcepack_2";
    private static final String STORM_ARROW_PROJECTILE_RESOURCEPACK_1_EVENT = "storm_arrow_projectile_resourcepack_1";
    private static final String STORM_ARROW_PROJECTILE_RESOURCEPACK_2_EVENT = "storm_arrow_projectile_resourcepack_2";
    private static final String SMITE_1_EVENT = "smite_1";
    private static final String SMITE_ACTIVATION_EVENT = "smite_activation";
    private static final String SMITE_RESOURCEPACK_1_EVENT = "smite_resourcepack_1";
    private static final String SMITE_RESOURCEPACK_2_EVENT = "smite_resourcepack_2";
    private static final String SMITE_ACTIVATION_RESOURCEPACK_1_EVENT = "smite_activation_resourcepack_1";
    private static final String SMITE_ACTIVATION_RESOURCEPACK_2_EVENT = "smite_activation_resourcepack_2";
    private static final String ARCHON_STRIKE_EVENT = "archon_strike";
    private static final String ARCHON_ACTIVATION_EVENT = "archon_activation";
    private static final String ARCHON_RESOURCEPACK_1_EVENT = "archon_resourcepack_1";
    private static final String ARCHON_RESOURCEPACK_2_EVENT = "archon_resourcepack_2";
    private static final String ARCHON_ACTIVATION_RESOURCEPACK_1_EVENT = "archon_activation_resourcepack_1";
    private static final String ARCHON_ACTIVATION_RESOURCEPACK_2_EVENT = "archon_activation_resourcepack_2";
    private static final String LIGHTNING_SEISMIC_CHARGE_CAST_EVENT = "lightning_seismic_charge_cast";
    private static final String LIGHTNING_SEISMIC_CHARGE_IMPACT_EVENT = "lightning_seismic_charge_impact";
    private static final String LIGHTNING_RESOURCEPACK_1_CAST_EVENT = "lightning_resourcepack_1_cast";
    private static final String LIGHTNING_RESOURCEPACK_1_IMPACT_EVENT = "lightning_resourcepack_1_impact";
    private static final String LIGHTNING_RESOURCEPACK_2_CAST_EVENT = "lightning_resourcepack_2_cast";
    private static final String LIGHTNING_RESOURCEPACK_2_IMPACT_EVENT = "lightning_resourcepack_2_impact";

    private static final Map<ResourceLocation, Boolean> SOUND_FILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> SOUND_LIFETIME_CACHE = new ConcurrentHashMap<>();
    private static FileSoundInstance arcaneStartupSound;
    private static ArcaneLoopSound arcaneLoopSound;
    private static PositionedFileSoundInstance lightningCastSound;
    private static final Set<FileSoundInstance> ACTIVE_SOUNDS = Collections.newSetFromMap(new WeakHashMap<>());
    private static long lastArcaneFirstSeen = Long.MIN_VALUE;
    private static long pendingArcaneLoopStart = Long.MIN_VALUE;
    private static long lastRailFirstSeen = Long.MIN_VALUE;

    private ArcaneBeamSoundController() {
    }

    public static void tick(Minecraft minecraft, Supplier<? extends SoundBeamState> arcaneBeam,
                            Supplier<? extends SoundBeamState> railBeam) {
        if (minecraft.level == null || minecraft.player == null) {
            stopAll(minecraft);
            return;
        }

        syncArcane(minecraft, minecraft.player, arcaneBeam.get());
        syncRail(minecraft, railBeam.get());
    }

    public static void stopAll(Minecraft minecraft) {
        stopArcaneSounds(minecraft);
        stopLightningCastSound(minecraft);
        for (FileSoundInstance sound : Set.copyOf(ACTIVE_SOUNDS)) {
            stopSound(minecraft, sound);
        }
        ACTIVE_SOUNDS.clear();
        lastArcaneFirstSeen = Long.MIN_VALUE;
        pendingArcaneLoopStart = Long.MIN_VALUE;
        lastRailFirstSeen = Long.MIN_VALUE;
    }

    private static void syncArcane(Minecraft minecraft, LocalPlayer player, SoundBeamState beam) {
        SoundChoice choice = soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound);
        long gameTime = minecraft.level.getGameTime();
        if (beam == null || choice == SoundChoice.DEFAULT || !canPlayArcaneSelection(minecraft)) {
            stopArcaneSounds(minecraft);
            lastArcaneFirstSeen = Long.MIN_VALUE;
            pendingArcaneLoopStart = Long.MIN_VALUE;
            return;
        }

        if (beam.firstSeenGameTime() != lastArcaneFirstSeen) {
            lastArcaneFirstSeen = beam.firstSeenGameTime();
            stopArcaneSounds(minecraft);
            if (choice == SoundChoice.OPTION_1) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_1_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }
            if (choice == SoundChoice.RESOURCEPACK_1) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_RESOURCEPACK_1_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }
            if (choice == SoundChoice.RESOURCEPACK_2) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_RESOURCEPACK_2_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }

            arcaneStartupSound = new FileSoundInstance(ARCANE_2_STARTUP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
            minecraft.getSoundManager().play(arcaneStartupSound);
            pendingArcaneLoopStart = gameTime + ARCANE_OPTION_2_STARTUP_TICKS;
        }

        if (choice == SoundChoice.OPTION_2 && pendingArcaneLoopStart != Long.MIN_VALUE && gameTime >= pendingArcaneLoopStart) {
            if (arcaneLoopSound == null || arcaneLoopSound.isStopped()) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_2_LOOP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
            }
            pendingArcaneLoopStart = Long.MIN_VALUE;
        }
    }

    private static void syncRail(Minecraft minecraft, SoundBeamState beam) {
        if (beam == null || !canPlayRailSelection(minecraft)) {
            lastRailFirstSeen = Long.MIN_VALUE;
            return;
        }
        if (beam.firstSeenGameTime() == lastRailFirstSeen) {
            return;
        }
        lastRailFirstSeen = beam.firstSeenGameTime();

        String soundPath = switch (soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound)) {
            case OPTION_1 -> RAIL_1_PATH;
            case OPTION_2 -> RAIL_2_PATH;
            case RESOURCEPACK_1 -> RAIL_RESOURCEPACK_1_PATH;
            case RESOURCEPACK_2 -> RAIL_RESOURCEPACK_2_PATH;
            default -> null;
        };
        if (soundPath != null) {
            minecraft.getSoundManager().play(new FileSoundInstance(soundPath, ArcaneBeamConfig.INSTANCE.rail.soundVolume));
        }
    }

    public static void playLightningStrikeCast(Minecraft minecraft, Vec3 position) {
        playLightningStrikeSound(minecraft, position, false);
    }

    public static void playLightningStrikeImpact(Minecraft minecraft, Vec3 position) {
        playLightningStrikeSound(minecraft, position, true);
    }

    public static void playVaultAltarBeamStart(Minecraft minecraft, Vec3 position, float volume) {
        VaultAltarSoundSlot slot = vaultAltarSoundSlot(vaultAltarSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position, volume, false,
                lifetimeTicksFor(slot.soundPath())));
    }

    public static boolean canPlayArcaneSelection(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }
        return switch (soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound)) {
            case OPTION_1 -> hasSoundFile(minecraft, ARCANE_1_PATH);
            case OPTION_2 -> hasSoundFile(minecraft, ARCANE_2_STARTUP_PATH)
                    && hasSoundFile(minecraft, ARCANE_2_LOOP_PATH);
            case RESOURCEPACK_1 -> hasSoundFile(minecraft, ARCANE_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> hasSoundFile(minecraft, ARCANE_RESOURCEPACK_2_PATH);
            default -> false;
        };
    }

    public static boolean canPlayRailSelection(Minecraft minecraft) {
        if (minecraft == null) {
            return false;
        }
        String soundPath = switch (soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound)) {
            case OPTION_1 -> RAIL_1_PATH;
            case OPTION_2 -> RAIL_2_PATH;
            case RESOURCEPACK_1 -> RAIL_RESOURCEPACK_1_PATH;
            case RESOURCEPACK_2 -> RAIL_RESOURCEPACK_2_PATH;
            default -> null;
        };
        return soundPath != null && hasSoundFile(minecraft, soundPath);
    }

    public static boolean canPlayLightningStrikeCast(Minecraft minecraft) {
        LightningSoundSlot slot = lightningSoundSlot(lightningSoundMode(), false);
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean canPlayLightningStrikeImpact(Minecraft minecraft) {
        LightningSoundSlot slot = lightningSoundSlot(lightningSoundMode(), true);
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean canPlayVaultAltarBeamStart(Minecraft minecraft) {
        VaultAltarSoundSlot slot = vaultAltarSoundSlot(vaultAltarSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playStormArrowStrike(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = stormArrowSoundSlot(stormArrowSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.stormArrow.soundVolume, stormArrowAudioRange()));
        return true;
    }

    public static boolean canPlayStormArrowStrike(Minecraft minecraft) {
        StormArrowSoundSlot slot = stormArrowSoundSlot(stormArrowSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playStormArrowProjectile(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = stormArrowProjectileSoundSlot(stormArrowProjectileSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.stormArrow.soundVolume, stormArrowAudioRange()));
        return true;
    }

    public static boolean canPlayStormArrowProjectile(Minecraft minecraft) {
        StormArrowSoundSlot slot = stormArrowProjectileSoundSlot(stormArrowProjectileSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playSmiteStrike(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = smiteSoundSlot(smiteSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.smite.soundVolume, smiteAudioRange()));
        return true;
    }

    public static boolean canPlaySmiteStrike(Minecraft minecraft) {
        StormArrowSoundSlot slot = smiteSoundSlot(smiteSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playSmiteActivation(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = smiteActivationSoundSlot(smiteActivationSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.smite.soundVolume, smiteAudioRange()));
        return true;
    }

    public static boolean canPlaySmiteActivation(Minecraft minecraft) {
        StormArrowSoundSlot slot = smiteActivationSoundSlot(smiteActivationSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playArchonStrike(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = archonSoundSlot(archonSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.archon.soundVolume, archonAudioRange()));
        return true;
    }

    public static boolean canPlayArchonStrike(Minecraft minecraft) {
        StormArrowSoundSlot slot = archonSoundSlot(archonSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playArchonActivation(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = archonActivationSoundSlot(archonActivationSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.archon.soundVolume, archonAudioRange()));
        return true;
    }

    public static boolean canPlayArchonActivation(Minecraft minecraft) {
        StormArrowSoundSlot slot = archonActivationSoundSlot(archonActivationSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playBlastWaveStrike(Minecraft minecraft, Vec3 position, float pitch) {
        StormArrowSoundSlot slot = archonSoundSlot(blastWaveSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(
                slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.blastWave.soundVolume, blastWaveAudioRange(), pitch
        ));
        return true;
    }

    public static boolean canPlayBlastWaveStrike(Minecraft minecraft) {
        StormArrowSoundSlot slot = archonSoundSlot(blastWaveSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    public static boolean playBlastWaveActivation(Minecraft minecraft, Vec3 position) {
        StormArrowSoundSlot slot = archonActivationSoundSlot(blastWaveActivationSoundMode());
        if (minecraft == null || position == null || slot == null || !hasSoundFile(minecraft, slot.soundPath())) {
            return false;
        }
        minecraft.getSoundManager().play(new PositionedFileSoundInstance(
                slot.eventName(), slot.soundPath(), position,
                ArcaneBeamConfig.INSTANCE.blastWave.soundVolume, blastWaveAudioRange()
        ));
        return true;
    }

    public static boolean canPlayBlastWaveActivation(Minecraft minecraft) {
        StormArrowSoundSlot slot = archonActivationSoundSlot(blastWaveActivationSoundMode());
        return minecraft != null && slot != null && hasSoundFile(minecraft, slot.soundPath());
    }

    private static void playLightningStrikeSound(Minecraft minecraft, Vec3 position, boolean impact) {
        LightningSoundSlot slot = lightningSoundSlot(lightningSoundMode(), impact);
        if (impact) {
            stopLightningCastSound(minecraft);
        } else {
            stopLightningCastSound(minecraft);
        }
        if (slot != null && hasSoundFile(minecraft, slot.soundPath())) {
            PositionedFileSoundInstance sound = new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position,
                    ArcaneBeamConfig.INSTANCE.lightningStrike.soundVolume);
            if (!impact) {
                lightningCastSound = sound;
            }
            minecraft.getSoundManager().play(sound);
        }
    }

    public static void registerReloadHook(IEventBus modEventBus) {
        modEventBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> clearSoundFileCache()));
    }

    public static void clearSoundFileCache() {
        SOUND_FILE_CACHE.clear();
        SOUND_LIFETIME_CACHE.clear();
    }

    /**
     * How long a one-shot may live before the safety stop. The engine ends a sound when its file ends, so this is
     * the real length (read once from the Ogg container and cached until the next resource reload) plus slack,
     * not a fixed cap: a resource pack slot of any length plays to the end, while a disconnect or level change
     * still stops every sound immediately.
     */
    private static int lifetimeTicksFor(String soundPath) {
        ResourceLocation id = new ResourceLocation(ArcaneBeam.MOD_ID, "sounds/" + soundPath + ".ogg");
        Integer cached = SOUND_LIFETIME_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        int ticks = SoundLifetime.ticksFor(-1.0D);
        try (Resource resource = Minecraft.getInstance().getResourceManager().getResource(id)) {
            ticks = SoundLifetime.ticksFor(OggDuration.seconds(resource.getInputStream().readAllBytes()));
        } catch (IOException | RuntimeException ignored) {
            // Unreadable length: keep the long fallback.
        }
        SOUND_LIFETIME_CACHE.put(id, ticks);
        return ticks;
    }

    private static boolean hasSoundFile(Minecraft minecraft, String soundPath) {
        ResourceLocation id = new ResourceLocation(ArcaneBeam.MOD_ID, "sounds/" + soundPath + ".ogg");
        Boolean cached = SOUND_FILE_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        boolean present;
        try {
            present = minecraft.getResourceManager().hasResource(id);
        } catch (RuntimeException ignored) {
            return false;
        }
        SOUND_FILE_CACHE.put(id, present);
        return present;
    }

    private static int stormArrowAudioRange() {
        return Math.max(16, Math.min(32, ArcaneBeamConfig.INSTANCE.stormArrow.audioRange));
    }

    private static int smiteAudioRange() {
        return Math.max(16, Math.min(32, ArcaneBeamConfig.INSTANCE.smite.audioRange));
    }

    private static int archonAudioRange() {
        return Math.max(16, Math.min(32, ArcaneBeamConfig.INSTANCE.archon.audioRange));
    }

    private static int blastWaveAudioRange() {
        return Math.max(16, Math.min(32, ArcaneBeamConfig.INSTANCE.blastWave.audioRange));
    }

    private static void stopArcaneSounds(Minecraft minecraft) {
        if (arcaneStartupSound != null) {
            stopSound(minecraft, arcaneStartupSound);
            arcaneStartupSound = null;
        }
        if (arcaneLoopSound != null) {
            stopSound(minecraft, arcaneLoopSound);
            arcaneLoopSound = null;
        }
    }

    private static void stopSound(Minecraft minecraft, FileSoundInstance sound) {
        if (sound == null) {
            return;
        }
        sound.stopAndUntrack();
        if (minecraft != null) {
            minecraft.getSoundManager().stop(sound);
        }
    }

    private static SoundChoice soundChoice(String id) {
        SoundChoice choice = SoundChoice.fromId(id);
        return choice == null ? SoundChoice.DEFAULT : choice;
    }

    private static LightningSoundMode lightningSoundMode() {
        LightningSoundMode mode = LightningSoundMode.fromId(ArcaneBeamConfig.INSTANCE.lightningStrike.soundMode);
        return mode == null ? LightningSoundMode.DEFAULT : mode;
    }

    private static VaultAltarSoundMode vaultAltarSoundMode() {
        VaultAltarSoundMode mode = VaultAltarSoundMode.fromId(ArcaneBeamConfig.INSTANCE.vaultAltar.soundMode);
        return mode == null ? VaultAltarSoundMode.DEFAULT : mode;
    }

    private static StormArrowSoundMode stormArrowSoundMode() {
        StormArrowSoundMode mode = StormArrowSoundMode.fromId(ArcaneBeamConfig.INSTANCE.stormArrow.soundMode);
        return mode == null ? StormArrowSoundMode.DEFAULT : mode;
    }

    private static StormArrowProjectileSoundMode stormArrowProjectileSoundMode() {
        StormArrowProjectileSoundMode mode = StormArrowProjectileSoundMode.fromId(ArcaneBeamConfig.INSTANCE.stormArrow.projectileSoundMode);
        return mode == null ? StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private static StormArrowSoundMode smiteSoundMode() {
        StormArrowSoundMode mode = StormArrowSoundMode.fromId(ArcaneBeamConfig.INSTANCE.smite.soundMode);
        return mode == null ? StormArrowSoundMode.DEFAULT : mode;
    }

    private static StormArrowProjectileSoundMode smiteActivationSoundMode() {
        StormArrowProjectileSoundMode mode = StormArrowProjectileSoundMode.fromId(ArcaneBeamConfig.INSTANCE.smite.projectileSoundMode);
        return mode == null ? StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private static StormArrowSoundMode archonSoundMode() {
        StormArrowSoundMode mode = StormArrowSoundMode.fromId(ArcaneBeamConfig.INSTANCE.archon.soundMode);
        return mode == null ? StormArrowSoundMode.DEFAULT : mode;
    }

    private static StormArrowProjectileSoundMode archonActivationSoundMode() {
        StormArrowProjectileSoundMode mode = StormArrowProjectileSoundMode.fromId(ArcaneBeamConfig.INSTANCE.archon.projectileSoundMode);
        return mode == null ? StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private static StormArrowSoundMode blastWaveSoundMode() {
        StormArrowSoundMode mode = StormArrowSoundMode.fromId(ArcaneBeamConfig.INSTANCE.blastWave.soundMode);
        return mode == null ? StormArrowSoundMode.DEFAULT : mode;
    }

    private static StormArrowProjectileSoundMode blastWaveActivationSoundMode() {
        StormArrowProjectileSoundMode mode = StormArrowProjectileSoundMode.fromId(ArcaneBeamConfig.INSTANCE.blastWave.projectileSoundMode);
        return mode == null ? StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private static LightningSoundSlot lightningSoundSlot(LightningSoundMode mode, boolean impact) {
        return switch (mode) {
            case SEISMIC_CHARGE -> impact
                    ? new LightningSoundSlot(LIGHTNING_SEISMIC_CHARGE_IMPACT_EVENT, LIGHTNING_SEISMIC_CHARGE_IMPACT_PATH)
                    : new LightningSoundSlot(LIGHTNING_SEISMIC_CHARGE_CAST_EVENT, LIGHTNING_SEISMIC_CHARGE_CAST_PATH);
            case RESOURCEPACK_1 -> impact
                    ? new LightningSoundSlot(LIGHTNING_RESOURCEPACK_1_IMPACT_EVENT, LIGHTNING_RESOURCEPACK_1_IMPACT_PATH)
                    : new LightningSoundSlot(LIGHTNING_RESOURCEPACK_1_CAST_EVENT, LIGHTNING_RESOURCEPACK_1_CAST_PATH);
            case RESOURCEPACK_2 -> impact
                    ? new LightningSoundSlot(LIGHTNING_RESOURCEPACK_2_IMPACT_EVENT, LIGHTNING_RESOURCEPACK_2_IMPACT_PATH)
                    : new LightningSoundSlot(LIGHTNING_RESOURCEPACK_2_CAST_EVENT, LIGHTNING_RESOURCEPACK_2_CAST_PATH);
            default -> null;
        };
    }

    private record LightningSoundSlot(String eventName, String soundPath) {
    }

    private static VaultAltarSoundSlot vaultAltarSoundSlot(VaultAltarSoundMode mode) {
        return switch (mode) {
            case ALTAR_1 -> new VaultAltarSoundSlot(VAULT_ALTAR_BEAM_EVENT, VAULT_ALTAR_BEAM_PATH);
            case RESOURCEPACK_1 -> new VaultAltarSoundSlot(VAULT_ALTAR_RESOURCEPACK_1_EVENT, VAULT_ALTAR_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new VaultAltarSoundSlot(VAULT_ALTAR_RESOURCEPACK_2_EVENT, VAULT_ALTAR_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private record VaultAltarSoundSlot(String eventName, String soundPath) {
    }

    private static StormArrowSoundSlot stormArrowSoundSlot(StormArrowSoundMode mode) {
        return switch (mode) {
            case BLASTER -> new StormArrowSoundSlot(STORM_ARROW_1_EVENT, STORM_ARROW_1_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(STORM_ARROW_RESOURCEPACK_1_EVENT, STORM_ARROW_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(STORM_ARROW_RESOURCEPACK_2_EVENT, STORM_ARROW_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private static StormArrowSoundSlot stormArrowProjectileSoundSlot(StormArrowProjectileSoundMode mode) {
        return switch (mode) {
            case OPTION_1 -> new StormArrowSoundSlot(STORM_ARROW_PROJECTILE_EVENT, STORM_ARROW_PROJECTILE_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(STORM_ARROW_PROJECTILE_RESOURCEPACK_1_EVENT, STORM_ARROW_PROJECTILE_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(STORM_ARROW_PROJECTILE_RESOURCEPACK_2_EVENT, STORM_ARROW_PROJECTILE_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private static StormArrowSoundSlot smiteSoundSlot(StormArrowSoundMode mode) {
        return switch (mode) {
            case BLASTER -> new StormArrowSoundSlot(SMITE_1_EVENT, SMITE_1_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(SMITE_RESOURCEPACK_1_EVENT, SMITE_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(SMITE_RESOURCEPACK_2_EVENT, SMITE_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private static StormArrowSoundSlot smiteActivationSoundSlot(StormArrowProjectileSoundMode mode) {
        return switch (mode) {
            case OPTION_1 -> new StormArrowSoundSlot(SMITE_ACTIVATION_EVENT, SMITE_ACTIVATION_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(SMITE_ACTIVATION_RESOURCEPACK_1_EVENT, SMITE_ACTIVATION_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(SMITE_ACTIVATION_RESOURCEPACK_2_EVENT, SMITE_ACTIVATION_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private static StormArrowSoundSlot archonSoundSlot(StormArrowSoundMode mode) {
        return switch (mode) {
            case BLASTER -> new StormArrowSoundSlot(ARCHON_STRIKE_EVENT, ARCHON_STRIKE_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(ARCHON_RESOURCEPACK_1_EVENT, ARCHON_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(ARCHON_RESOURCEPACK_2_EVENT, ARCHON_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private static StormArrowSoundSlot archonActivationSoundSlot(StormArrowProjectileSoundMode mode) {
        return switch (mode) {
            case OPTION_1 -> new StormArrowSoundSlot(ARCHON_ACTIVATION_EVENT, ARCHON_ACTIVATION_PATH);
            case RESOURCEPACK_1 -> new StormArrowSoundSlot(ARCHON_ACTIVATION_RESOURCEPACK_1_EVENT, ARCHON_ACTIVATION_RESOURCEPACK_1_PATH);
            case RESOURCEPACK_2 -> new StormArrowSoundSlot(ARCHON_ACTIVATION_RESOURCEPACK_2_EVENT, ARCHON_ACTIVATION_RESOURCEPACK_2_PATH);
            default -> null;
        };
    }

    private record StormArrowSoundSlot(String eventName, String soundPath) {
    }

    private static final class ArcaneLoopSound extends FileSoundInstance {
        private final LocalPlayer player;

        private ArcaneLoopSound(LocalPlayer player, String path, float volume) {
            super(path, volume);
            this.player = player;
            this.looping = true;
        }

        @Override
        public void tick() {
            if (player.isRemoved() || player.isDeadOrDying()) {
                stopAndUntrack();
            }
        }
    }

    private static class FileSoundInstance extends AbstractTickableSoundInstance {
        private final WeighedSoundEvents soundSet;
        private final int maxAgeTicks;
        private int ageTicks;

        private FileSoundInstance(String path) {
            this(path, 1.0F);
        }

        private FileSoundInstance(String path, float volume) {
            this(new ResourceLocation(ArcaneBeam.MOD_ID, path), new ResourceLocation(ArcaneBeam.MOD_ID, path), volume, false);
        }

        private FileSoundInstance(ResourceLocation eventId, ResourceLocation fileId, float volume, boolean stream) {
            this(eventId, fileId, volume, stream, lifetimeTicksFor(fileId.getPath()));
        }

        private FileSoundInstance(ResourceLocation eventId, ResourceLocation fileId, float volume, boolean stream, int maxAgeTicks) {
            this(eventId, fileId, volume, stream, maxAgeTicks, 16);
        }

        private FileSoundInstance(ResourceLocation eventId, ResourceLocation fileId, float volume, boolean stream, int maxAgeTicks, int attenuationDistance) {
            super(new SoundEvent(eventId), SoundSource.PLAYERS);
            // The subtitle key lets the replaced Vault sound keep a subtitle (the sound is built directly, not from sounds.json).
            this.soundSet = new WeighedSoundEvents(eventId, SoundSubtitles.keyFor(fileId.getPath()));
            this.maxAgeTicks = maxAgeTicks;
            this.sound = new Sound(fileId.toString(), 1.0F, 1.0F, 1, Sound.Type.FILE, stream, false, attenuationDistance);
            this.soundSet.addSound(this.sound);
            this.looping = false;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            ACTIVE_SOUNDS.add(this);
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return this.soundSet;
        }

        @Override
        public void tick() {
            if (!this.looping && ++ageTicks >= maxAgeTicks) {
                stopAndUntrack();
            }
        }

        protected final void stopAndUntrack() {
            stop();
            ACTIVE_SOUNDS.remove(this);
        }
    }

    private static void stopLightningCastSound(Minecraft minecraft) {
        if (lightningCastSound != null) {
            stopSound(minecraft, lightningCastSound);
            lightningCastSound = null;
        }
    }

    private static final class PositionedFileSoundInstance extends FileSoundInstance {
        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume) {
            this(eventName, path, position, volume, false, lifetimeTicksFor(path));
        }

        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume, int attenuationDistance) {
            this(eventName, path, position, volume, false, lifetimeTicksFor(path), attenuationDistance);
        }

        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume, int attenuationDistance, float pitch) {
            this(eventName, path, position, volume, false, lifetimeTicksFor(path), attenuationDistance);
            this.pitch = pitch;
        }

        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume, boolean stream, int maxAgeTicks) {
            this(eventName, path, position, volume, stream, maxAgeTicks, 16);
        }

        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume, boolean stream, int maxAgeTicks,
                int attenuationDistance) {
            super(new ResourceLocation(ArcaneBeam.MOD_ID, eventName), new ResourceLocation(ArcaneBeam.MOD_ID, path), volume, stream, maxAgeTicks,
                    attenuationDistance);
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
        }
    }
}
