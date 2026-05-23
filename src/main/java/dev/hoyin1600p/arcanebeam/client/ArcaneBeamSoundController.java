package dev.hoyin1600p.arcanebeam.client;

import com.mojang.logging.LogUtils;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

public final class ArcaneBeamSoundController {
    private static final int ARCANE_OPTION_2_STARTUP_TICKS = 49;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ARCANE_1_PATH = "abilities/arcane_1";
    private static final String ARCANE_2_STARTUP_PATH = "abilities/arcane_2_startup";
    private static final String ARCANE_2_LOOP_PATH = "abilities/arcane_2_loop";
    private static final String RAIL_1_PATH = "abilities/rail_1";
    private static final String RAIL_2_PATH = "abilities/rail_2";

    private static FileSoundInstance arcaneStartupSound;
    private static ArcaneLoopSound arcaneLoopSound;
    private static long lastArcaneFirstSeen = Long.MIN_VALUE;
    private static long pendingArcaneLoopStart = Long.MIN_VALUE;
    private static long lastRailFirstSeen = Long.MIN_VALUE;

    private ArcaneBeamSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            stopArcaneSounds(minecraft);
            lastArcaneFirstSeen = Long.MIN_VALUE;
            pendingArcaneLoopStart = Long.MIN_VALUE;
            lastRailFirstSeen = Long.MIN_VALUE;
            return;
        }

        syncArcane(minecraft, minecraft.player, ArcaneBeamManager.getLocalActiveBeam(ArcaneBeamManager.BeamKind.ARCANE));
        syncRail(minecraft, ArcaneBeamManager.getLocalActiveBeam(ArcaneBeamManager.BeamKind.RAIL));
    }

    private static void syncArcane(Minecraft minecraft, LocalPlayer player, ArcaneBeamManager.ActiveBeam beam) {
        ArcaneBeamConfig.SoundChoice choice = soundChoice(ArcaneBeamConfig.INSTANCE.arcane.sound);
        long gameTime = minecraft.level.getGameTime();
        if (beam == null || choice == ArcaneBeamConfig.SoundChoice.DEFAULT) {
            stopArcaneSounds(minecraft);
            lastArcaneFirstSeen = Long.MIN_VALUE;
            pendingArcaneLoopStart = Long.MIN_VALUE;
            return;
        }

        if (beam.firstSeenGameTime() != lastArcaneFirstSeen) {
            lastArcaneFirstSeen = beam.firstSeenGameTime();
            stopArcaneSounds(minecraft);
            if (choice == ArcaneBeamConfig.SoundChoice.OPTION_1) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_1_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                LOGGER.info("[ArcaneBeam] Starting Arcane option 1 loop");
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }

            LOGGER.info("[ArcaneBeam] Playing Arcane option 2 startup");
            arcaneStartupSound = new FileSoundInstance(ARCANE_2_STARTUP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
            minecraft.getSoundManager().play(arcaneStartupSound);
            pendingArcaneLoopStart = gameTime + ARCANE_OPTION_2_STARTUP_TICKS;
        }

        if (choice == ArcaneBeamConfig.SoundChoice.OPTION_2 && pendingArcaneLoopStart != Long.MIN_VALUE && gameTime >= pendingArcaneLoopStart) {
            if (arcaneLoopSound == null || arcaneLoopSound.isStopped()) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_2_LOOP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                LOGGER.info("[ArcaneBeam] Starting Arcane option 2 loop");
                minecraft.getSoundManager().play(arcaneLoopSound);
            }
            pendingArcaneLoopStart = Long.MIN_VALUE;
        }
    }

    private static void syncRail(Minecraft minecraft, ArcaneBeamManager.ActiveBeam beam) {
        if (beam == null) {
            return;
        }
        if (beam.firstSeenGameTime() == lastRailFirstSeen) {
            return;
        }
        lastRailFirstSeen = beam.firstSeenGameTime();

        String soundPath = switch (soundChoice(ArcaneBeamConfig.INSTANCE.rail.sound)) {
            case OPTION_1 -> RAIL_1_PATH;
            case OPTION_2 -> RAIL_2_PATH;
            default -> null;
        };
        if (soundPath != null) {
            LOGGER.info("[ArcaneBeam] Playing Rail sound {}", soundPath);
            minecraft.getSoundManager().play(new FileSoundInstance(soundPath, ArcaneBeamConfig.INSTANCE.rail.soundVolume));
        }
    }

    private static void stopArcaneSounds(Minecraft minecraft) {
        if (arcaneStartupSound != null) {
            LOGGER.info("[ArcaneBeam] Stopping Arcane startup");
            minecraft.getSoundManager().stop(arcaneStartupSound);
            arcaneStartupSound = null;
        }
        if (arcaneLoopSound != null) {
            LOGGER.info("[ArcaneBeam] Stopping Arcane loop");
            minecraft.getSoundManager().stop(arcaneLoopSound);
            arcaneLoopSound = null;
        }
    }

    private static ArcaneBeamConfig.SoundChoice soundChoice(String id) {
        ArcaneBeamConfig.SoundChoice choice = ArcaneBeamConfig.SoundChoice.fromId(id);
        return choice == null ? ArcaneBeamConfig.SoundChoice.DEFAULT : choice;
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
                stop();
            }
        }
    }

    private static class FileSoundInstance extends AbstractTickableSoundInstance {
        private final WeighedSoundEvents soundSet;

        private FileSoundInstance(String path) {
            this(path, 1.0F);
        }

        private FileSoundInstance(String path, float volume) {
            this(new ResourceLocation(ArcaneBeam.MOD_ID, path), volume);
        }

        private FileSoundInstance(ResourceLocation soundId, float volume) {
            super(new SoundEvent(soundId), SoundSource.PLAYERS);
            this.soundSet = new WeighedSoundEvents(soundId, null);
            this.sound = new Sound(soundId.toString(), 1.0F, 1.0F, 1, Sound.Type.FILE, true, false, 16);
            this.soundSet.addSound(this.sound);
            this.looping = false;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return this.soundSet;
        }

        @Override
        public void tick() {
        }
    }
}
