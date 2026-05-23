package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArcaneBeamConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ArcaneBeam.json");

    public static Config INSTANCE = new Config();

    private ArcaneBeamConfig() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (IOException ignored) {
                INSTANCE = new Config();
            }
        }
        validate();
        save();
    }

    private static void validate() {
        if (INSTANCE.shaderCompatibility == null) {
            INSTANCE.shaderCompatibility = ShaderCompatibility.OFF.id;
        }
        if (INSTANCE.arcane == null) {
            INSTANCE.arcane = new BeamSettings(0x8F35FF, new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF}, 0.08F, 0.65F, 0.14F, 3, 16.0D);
        }
        if (INSTANCE.rail == null) {
            INSTANCE.rail = new BeamSettings(0x00FF44, new int[]{0x00FF44, 0x7CFF5C, 0x00FFC8, 0xFFFFFF}, 0.11F, 0.95F, 0.18F, 8, 128.0D);
        }
        if (INSTANCE.arcane.maxRange <= 0.0D) {
            INSTANCE.arcane.maxRange = 16.0D;
        }
        if (INSTANCE.rail.maxRange <= 0.0D) {
            INSTANCE.rail.maxRange = 128.0D;
        }
        if (INSTANCE.arcane.lifetimeTicks <= 0) {
            INSTANCE.arcane.lifetimeTicks = 3;
        }
        if (INSTANCE.rail.lifetimeTicks <= 0) {
            INSTANCE.rail.lifetimeTicks = 8;
        }
        if (INSTANCE.arcane.radius >= 0.16F) {
            INSTANCE.arcane.radius = 0.08F;
        }
        if (INSTANCE.rail.radius >= 0.22F) {
            INSTANCE.rail.radius = 0.11F;
        }
        if (INSTANCE.rail.color == 0x35D7FF) {
            INSTANCE.rail.color = 0x00FF44;
        }
        validateShape(INSTANCE.arcane, 0.08F, 0.65F, 0.14F);
        validateShape(INSTANCE.rail, 0.11F, 0.95F, 0.18F);
        validateColors(INSTANCE.arcane);
        validateColors(INSTANCE.rail);
        validateColorShift(INSTANCE.arcane);
        validateColorShift(INSTANCE.rail);
        validateSound(INSTANCE.arcane);
        validateSound(INSTANCE.rail);
        validateOrigin(INSTANCE.arcane);
        validateOrigin(INSTANCE.rail);
        validateTransitions(INSTANCE.arcane, FadeInStyle.FADE, 5, FadeOutStyle.SHRINK, 10);
        validateTransitions(INSTANCE.rail, FadeInStyle.FADE, 1, FadeOutStyle.FADE, 4);
        validateShaderCompatibility();
    }

    private static void validateShape(BeamSettings settings, float defaultIntensity, float defaultOpacity, float defaultGlowRadius) {
        if (settings.intensity <= 0.0F) {
            settings.intensity = settings.radius > 0.0F ? settings.radius : defaultIntensity;
        }
        if (settings.opacity <= 0.0F) {
            settings.opacity = settings.alpha > 0.0F ? settings.alpha : defaultOpacity;
        }
        if (settings.glowRadius <= 0.0F) {
            settings.glowRadius = defaultGlowRadius;
        }
        if (settings.glowOpacity <= 0.0F) {
            settings.glowOpacity = 0.20F;
        }
        settings.glowOpacity = Math.max(0.0F, Math.min(1.0F, settings.glowOpacity));
        settings.radius = settings.intensity;
        settings.alpha = settings.opacity;
    }

    private static void validateColors(BeamSettings settings) {
        if (settings.colors == null || settings.colors.length != 4) {
            int fallback = settings.color == 0 ? 0xFFFFFF : settings.color;
            settings.colors = new int[]{fallback, fallback, fallback, 0xFFFFFF};
        }
        if (settings.glowColors == null || settings.glowColors.length != 4) {
            settings.glowColors = settings.colors.clone();
        }
        if (settings.color == 0) {
            settings.color = settings.colors[0];
        }
        if (settings.glowColor == 0) {
            settings.glowColor = settings.glowColors[0];
        }
    }

    private static void validateSound(BeamSettings settings) {
        if (settings.sound == null || settings.sound.isBlank()) {
            settings.sound = SoundChoice.DEFAULT.id;
        } else if (SoundChoice.fromId(settings.sound) == null) {
            settings.sound = SoundChoice.DEFAULT.id;
        }
        settings.soundVolume = Math.max(0.0F, Math.min(2.0F, settings.soundVolume));
    }

    private static void validateColorShift(BeamSettings settings) {
        if (settings.colorShiftTicks <= 0.0F) {
            settings.colorShiftTicks = 8.0F;
        }
        settings.colorShiftTicks = Math.max(2.0F, Math.min(60.0F, settings.colorShiftTicks));
        settings.glowRotationRpm = Math.max(0.0F, Math.min(60.0F, settings.glowRotationRpm));
    }

    private static void validateOrigin(BeamSettings settings) {
        if (settings.startHand == null || StartHand.fromId(settings.startHand) == null) {
            settings.startHand = StartHand.OFFHAND.id;
        }
        if (settings.startOffsetX == 0.0D && settings.startOffsetY == 0.0D && settings.startOffsetZ == 0.0D) {
            settings.startOffsetX = 0.38D;
            settings.startOffsetY = -0.45D;
            settings.startOffsetZ = 0.18D;
        }
    }

    private static void validateTransitions(BeamSettings settings, FadeInStyle defaultFadeInStyle, int defaultFadeInTicks, FadeOutStyle defaultFadeOutStyle, int defaultFadeOutTicks) {
        if (FadeInStyle.fromId(settings.fadeInStyle) == null) {
            settings.fadeInStyle = defaultFadeInStyle.id;
        }
        if (FadeOutStyle.fromId(settings.fadeOutStyle) == null) {
            settings.fadeOutStyle = defaultFadeOutStyle.id;
        }
        settings.fadeInTicks = settings.fadeInTicks < 0 ? defaultFadeInTicks : Math.max(0, Math.min(99, settings.fadeInTicks));
        settings.fadeOutTicks = settings.fadeOutTicks < 0 ? defaultFadeOutTicks : Math.max(0, Math.min(99, settings.fadeOutTicks));
    }

    private static void validateShaderCompatibility() {
        if (ShaderCompatibility.fromId(INSTANCE.shaderCompatibility) == null) {
            INSTANCE.shaderCompatibility = ShaderCompatibility.OFF.id;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static final class Config {
        public String shaderCompatibility = ShaderCompatibility.OFF.id;
        public BeamSettings arcane = new BeamSettings(0x8F35FF, new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF}, 0.08F, 0.65F, 0.14F, 3, 16.0D);
        public BeamSettings rail = new BeamSettings(0x00FF44, new int[]{0x00FF44, 0x7CFF5C, 0x00FFC8, 0xFFFFFF}, 0.11F, 0.95F, 0.18F, 8, 128.0D);
    }

    public static final class BeamSettings {
        public int color;
        public int[] colors;
        public int glowColor;
        public int[] glowColors;
        public float radius;
        public float alpha;
        public float intensity;
        public float opacity;
        public float glowRadius;
        public float glowOpacity = 0.20F;
        public float colorShiftTicks = 8.0F;
        public float glowRotationRpm = 0.0F;
        public int lifetimeTicks;
        public double maxRange;
        public String sound = SoundChoice.DEFAULT.id;
        public float soundVolume = 1.00F;
        public String fadeInStyle = FadeInStyle.FADE.id;
        public int fadeInTicks = -1;
        public String fadeOutStyle = FadeOutStyle.SHRINK.id;
        public int fadeOutTicks = -1;
        public String startHand = StartHand.OFFHAND.id;
        public double startOffsetX = 0.38D;
        public double startOffsetY = -0.45D;
        public double startOffsetZ = 0.18D;

        public BeamSettings() {
        }

        public BeamSettings(int color, int[] colors, float intensity, float opacity, float glowRadius, int lifetimeTicks, double maxRange) {
            this.color = color;
            this.colors = colors;
            this.glowColor = color;
            this.glowColors = colors.clone();
            this.radius = intensity;
            this.alpha = opacity;
            this.intensity = intensity;
            this.opacity = opacity;
            this.glowRadius = glowRadius;
            this.glowOpacity = 0.20F;
            this.lifetimeTicks = lifetimeTicks;
            this.maxRange = maxRange;
        }
    }

    public enum ShaderCompatibility {
        OFF("off", "Off"),
        ON("on", "On");

        public final String id;
        public final String label;

        ShaderCompatibility(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static ShaderCompatibility fromId(String id) {
            for (ShaderCompatibility compatibility : values()) {
                if (compatibility.id.equals(id)) {
                    return compatibility;
                }
            }
            return null;
        }
    }

    public enum SoundChoice {
        DEFAULT("default", "Default"),
        OPTION_1("option_1", "Option 1"),
        OPTION_2("option_2", "Option 2");

        public final String id;
        public final String label;

        SoundChoice(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static SoundChoice fromId(String id) {
            for (SoundChoice choice : values()) {
                if (choice.id.equals(id)) {
                    return choice;
                }
            }
            return null;
        }
    }

    public enum StartHand {
        MAIN_HAND("main_hand", "Main Hand"),
        OFFHAND("offhand", "Offhand");

        public final String id;
        public final String label;

        StartHand(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static StartHand fromId(String id) {
            for (StartHand hand : values()) {
                if (hand.id.equals(id)) {
                    return hand;
                }
            }
            return null;
        }
    }

    public enum FadeInStyle {
        FADE("fade", "Fade In"),
        GROW("grow", "Grow In");

        public final String id;
        public final String label;

        FadeInStyle(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static FadeInStyle fromId(String id) {
            for (FadeInStyle style : values()) {
                if (style.id.equals(id)) {
                    return style;
                }
            }
            return null;
        }
    }

    public enum FadeOutStyle {
        FADE("fade", "Fade Out"),
        SHRINK("shrink", "Shrink Out");

        public final String id;
        public final String label;

        FadeOutStyle(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static FadeOutStyle fromId(String id) {
            for (FadeOutStyle style : values()) {
                if (style.id.equals(id)) {
                    return style;
                }
            }
            return null;
        }
    }
}
