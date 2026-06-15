package dev.hoyin1600p.arcanebeam.client;

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
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

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
    private static final String LIGHTNING_SEISMIC_CHARGE_CAST_EVENT = "lightning_seismic_charge_cast";
    private static final String LIGHTNING_SEISMIC_CHARGE_IMPACT_EVENT = "lightning_seismic_charge_impact";
    private static final String LIGHTNING_RESOURCEPACK_1_CAST_EVENT = "lightning_resourcepack_1_cast";
    private static final String LIGHTNING_RESOURCEPACK_1_IMPACT_EVENT = "lightning_resourcepack_1_impact";
    private static final String LIGHTNING_RESOURCEPACK_2_CAST_EVENT = "lightning_resourcepack_2_cast";
    private static final String LIGHTNING_RESOURCEPACK_2_IMPACT_EVENT = "lightning_resourcepack_2_impact";

    private static FileSoundInstance arcaneStartupSound;
    private static ArcaneLoopSound arcaneLoopSound;
    private static PositionedFileSoundInstance lightningCastSound;
    private static long lastArcaneFirstSeen = Long.MIN_VALUE;
    private static long pendingArcaneLoopStart = Long.MIN_VALUE;
    private static long lastRailFirstSeen = Long.MIN_VALUE;

    private ArcaneBeamSoundController() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            stopArcaneSounds(minecraft);
            stopLightningCastSound(minecraft);
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
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }
            if (choice == ArcaneBeamConfig.SoundChoice.RESOURCEPACK_1) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_RESOURCEPACK_1_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }
            if (choice == ArcaneBeamConfig.SoundChoice.RESOURCEPACK_2) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_RESOURCEPACK_2_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
                minecraft.getSoundManager().play(arcaneLoopSound);
                pendingArcaneLoopStart = Long.MIN_VALUE;
                return;
            }

            arcaneStartupSound = new FileSoundInstance(ARCANE_2_STARTUP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
            minecraft.getSoundManager().play(arcaneStartupSound);
            pendingArcaneLoopStart = gameTime + ARCANE_OPTION_2_STARTUP_TICKS;
        }

        if (choice == ArcaneBeamConfig.SoundChoice.OPTION_2 && pendingArcaneLoopStart != Long.MIN_VALUE && gameTime >= pendingArcaneLoopStart) {
            if (arcaneLoopSound == null || arcaneLoopSound.isStopped()) {
                arcaneLoopSound = new ArcaneLoopSound(player, ARCANE_2_LOOP_PATH, ArcaneBeamConfig.INSTANCE.arcane.soundVolume);
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

    private static void playLightningStrikeSound(Minecraft minecraft, Vec3 position, boolean impact) {
        LightningSoundSlot slot = lightningSoundSlot(lightningSoundMode(), impact);
        if (impact) {
            stopLightningCastSound(minecraft);
        } else {
            stopLightningCastSound(minecraft);
        }
        if (slot != null && hasSoundFile(minecraft, slot.soundPath())) {
            PositionedFileSoundInstance sound = new PositionedFileSoundInstance(slot.eventName(), slot.soundPath(), position, ArcaneBeamConfig.INSTANCE.lightningStrike.soundVolume);
            if (!impact) {
                lightningCastSound = sound;
            }
            minecraft.getSoundManager().play(sound);
        }
    }

    private static boolean hasSoundFile(Minecraft minecraft, String soundPath) {
        try {
            minecraft.getResourceManager().getResource(new ResourceLocation(ArcaneBeam.MOD_ID, "sounds/" + soundPath + ".ogg"));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void stopArcaneSounds(Minecraft minecraft) {
        if (arcaneStartupSound != null) {
            minecraft.getSoundManager().stop(arcaneStartupSound);
            arcaneStartupSound = null;
        }
        if (arcaneLoopSound != null) {
            minecraft.getSoundManager().stop(arcaneLoopSound);
            arcaneLoopSound = null;
        }
    }

    private static ArcaneBeamConfig.SoundChoice soundChoice(String id) {
        ArcaneBeamConfig.SoundChoice choice = ArcaneBeamConfig.SoundChoice.fromId(id);
        return choice == null ? ArcaneBeamConfig.SoundChoice.DEFAULT : choice;
    }

    private static ArcaneBeamConfig.LightningSoundMode lightningSoundMode() {
        ArcaneBeamConfig.LightningSoundMode mode = ArcaneBeamConfig.LightningSoundMode.fromId(ArcaneBeamConfig.INSTANCE.lightningStrike.soundMode);
        return mode == null ? ArcaneBeamConfig.LightningSoundMode.DEFAULT : mode;
    }

    private static LightningSoundSlot lightningSoundSlot(ArcaneBeamConfig.LightningSoundMode mode, boolean impact) {
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
        private static final int ONE_SHOT_LIFETIME_TICKS = 120;

        private final WeighedSoundEvents soundSet;
        private int ageTicks;

        private FileSoundInstance(String path) {
            this(path, 1.0F);
        }

        private FileSoundInstance(String path, float volume) {
            this(new ResourceLocation(ArcaneBeam.MOD_ID, path), new ResourceLocation(ArcaneBeam.MOD_ID, path), volume, true);
        }

        private FileSoundInstance(ResourceLocation eventId, ResourceLocation fileId, float volume, boolean stream) {
            super(new SoundEvent(eventId), SoundSource.PLAYERS);
            this.soundSet = new WeighedSoundEvents(eventId, null);
            this.sound = new Sound(fileId.toString(), 1.0F, 1.0F, 1, Sound.Type.FILE, stream, false, 16);
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
            if (!this.looping && ++ageTicks >= ONE_SHOT_LIFETIME_TICKS) {
                stop();
            }
        }
    }

    private static void stopLightningCastSound(Minecraft minecraft) {
        if (lightningCastSound != null) {
            minecraft.getSoundManager().stop(lightningCastSound);
            lightningCastSound = null;
        }
    }

    private static final class PositionedFileSoundInstance extends FileSoundInstance {
        private PositionedFileSoundInstance(String eventName, String path, Vec3 position, float volume) {
            super(new ResourceLocation(ArcaneBeam.MOD_ID, eventName), new ResourceLocation(ArcaneBeam.MOD_ID, path), volume, false);
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
        }
    }
}
