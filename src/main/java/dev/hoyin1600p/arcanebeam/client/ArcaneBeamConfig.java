package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArcaneBeamConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ArcaneBeam.json");
    private static final String DEFAULT_PROFILE = "Default";

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
            INSTANCE.arcane = defaultArcaneSettings();
        }
        if (INSTANCE.rail == null) {
            INSTANCE.rail = defaultRailSettings();
        }
        validateShaderCompatibility();
        validateBeamSettings(INSTANCE.arcane, false);
        validateBeamSettings(INSTANCE.rail, true);
        INSTANCE.arcaneProfiles = validateProfiles(INSTANCE.arcaneProfiles, INSTANCE.arcane, false);
        INSTANCE.railProfiles = validateProfiles(INSTANCE.railProfiles, INSTANCE.rail, true);
        INSTANCE.selectedArcaneProfile = validateSelectedProfile(INSTANCE.selectedArcaneProfile, INSTANCE.arcaneProfiles);
        INSTANCE.selectedRailProfile = validateSelectedProfile(INSTANCE.selectedRailProfile, INSTANCE.railProfiles);
        activateSelectedProfiles();
    }

    private static void validateBeamSettings(BeamSettings settings, boolean rail) {
        if (settings.maxRange <= 0.0D) {
            settings.maxRange = rail ? 128.0D : 16.0D;
        }
        if (settings.lifetimeTicks <= 0) {
            settings.lifetimeTicks = rail ? 8 : 3;
        }
        if (!rail && settings.radius >= 0.16F) {
            settings.radius = 0.08F;
        }
        if (rail && settings.radius >= 0.22F) {
            settings.radius = 0.11F;
        }
        if (rail && settings.color == 0x35D7FF) {
            settings.color = 0x00FF44;
        }
        validateShape(settings, rail ? 0.11F : 0.08F, rail ? 0.95F : 0.65F, rail ? 0.18F : 0.14F);
        validateColors(settings);
        validateColorShift(settings);
        validateSound(settings);
        validateOrigin(settings);
        validateTransitions(settings, FadeInStyle.FADE, rail ? 1 : 5, rail ? FadeOutStyle.FADE : FadeOutStyle.SHRINK, rail ? 4 : 10);
        validateShaderCompatibility(settings);
    }

    private static LinkedHashMap<String, BeamSettings> validateProfiles(Map<String, BeamSettings> profiles, BeamSettings migrationSettings, boolean rail) {
        LinkedHashMap<String, BeamSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, BeamSettings> entry : profiles.entrySet()) {
                String name = normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                BeamSettings settings = entry.getValue() == null ? defaultSettings(rail) : entry.getValue();
                validateBeamSettings(settings, rail);
                validated.put(uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            BeamSettings settings = copyOf(migrationSettings == null ? defaultSettings(rail) : migrationSettings);
            validateBeamSettings(settings, rail);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    private static String validateSelectedProfile(String selectedProfile, LinkedHashMap<String, BeamSettings> profiles) {
        String normalized = normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    private static void activateSelectedProfiles() {
        INSTANCE.arcane = INSTANCE.arcaneProfiles.get(INSTANCE.selectedArcaneProfile);
        INSTANCE.rail = INSTANCE.railProfiles.get(INSTANCE.selectedRailProfile);
        INSTANCE.shaderCompatibility = INSTANCE.arcane.shaderCompatibility;
    }

    private static void syncActiveProfiles() {
        if (INSTANCE.arcaneProfiles != null && INSTANCE.selectedArcaneProfile != null && INSTANCE.arcane != null) {
            INSTANCE.arcaneProfiles.put(INSTANCE.selectedArcaneProfile, INSTANCE.arcane);
        }
        if (INSTANCE.railProfiles != null && INSTANCE.selectedRailProfile != null && INSTANCE.rail != null) {
            INSTANCE.railProfiles.put(INSTANCE.selectedRailProfile, INSTANCE.rail);
        }
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

    private static void validateShaderCompatibility(BeamSettings settings) {
        if (settings.shaderCompatibility == null) {
            settings.shaderCompatibility = INSTANCE.shaderCompatibility;
        }
        if (ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.OFF.id;
        }
    }

    public static void save() {
        syncActiveProfiles();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static List<String> profileNames(boolean rail) {
        return new ArrayList<>(profileMap(rail).keySet());
    }

    public static String selectedProfileName(boolean rail) {
        return rail ? INSTANCE.selectedRailProfile : INSTANCE.selectedArcaneProfile;
    }

    public static void selectProfile(boolean rail, String profileName) {
        LinkedHashMap<String, BeamSettings> profiles = profileMap(rail);
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !profiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        if (rail) {
            INSTANCE.selectedRailProfile = normalized;
            INSTANCE.rail = profiles.get(normalized);
        } else {
            INSTANCE.selectedArcaneProfile = normalized;
            INSTANCE.arcane = profiles.get(normalized);
            INSTANCE.shaderCompatibility = INSTANCE.arcane.shaderCompatibility;
        }
        save();
    }

    public static String addProfile(boolean rail, String requestedName) {
        LinkedHashMap<String, BeamSettings> profiles = profileMap(rail);
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(profiles, baseName);
        BeamSettings settings = copyOf(rail ? INSTANCE.rail : INSTANCE.arcane);
        validateBeamSettings(settings, rail);
        profiles.put(profileName, settings);
        if (rail) {
            INSTANCE.selectedRailProfile = profileName;
            INSTANCE.rail = settings;
        } else {
            INSTANCE.selectedArcaneProfile = profileName;
            INSTANCE.arcane = settings;
            INSTANCE.shaderCompatibility = settings.shaderCompatibility;
        }
        save();
        return profileName;
    }

    private static LinkedHashMap<String, BeamSettings> profileMap(boolean rail) {
        return rail ? INSTANCE.railProfiles : INSTANCE.arcaneProfiles;
    }

    private static String normalizeProfileName(String profileName) {
        if (profileName == null) {
            return "";
        }
        return profileName.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private static String uniqueProfileName(Map<String, BeamSettings> profiles, String baseName) {
        if (!profiles.containsKey(baseName)) {
            return baseName;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = baseName + " " + suffix++;
        } while (profiles.containsKey(candidate));
        return candidate;
    }

    private static BeamSettings defaultSettings(boolean rail) {
        return rail ? defaultRailSettings() : defaultArcaneSettings();
    }

    private static BeamSettings defaultArcaneSettings() {
        return new BeamSettings(0x8F35FF, new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF}, 0.08F, 0.65F, 0.14F, 3, 16.0D);
    }

    private static BeamSettings defaultRailSettings() {
        return new BeamSettings(0x00FF44, new int[]{0x00FF44, 0x7CFF5C, 0x00FFC8, 0xFFFFFF}, 0.11F, 0.95F, 0.18F, 8, 128.0D);
    }

    private static BeamSettings copyOf(BeamSettings source) {
        BeamSettings copy = new BeamSettings();
        copy.color = source.color;
        copy.colors = source.colors == null ? null : source.colors.clone();
        copy.glowColor = source.glowColor;
        copy.glowColors = source.glowColors == null ? null : source.glowColors.clone();
        copy.radius = source.radius;
        copy.alpha = source.alpha;
        copy.intensity = source.intensity;
        copy.opacity = source.opacity;
        copy.glowRadius = source.glowRadius;
        copy.glowOpacity = source.glowOpacity;
        copy.colorShiftTicks = source.colorShiftTicks;
        copy.glowRotationRpm = source.glowRotationRpm;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.maxRange = source.maxRange;
        copy.sound = source.sound;
        copy.soundVolume = source.soundVolume;
        copy.fadeInStyle = source.fadeInStyle;
        copy.fadeInTicks = source.fadeInTicks;
        copy.fadeOutStyle = source.fadeOutStyle;
        copy.fadeOutTicks = source.fadeOutTicks;
        copy.startHand = source.startHand;
        copy.startOffsetX = source.startOffsetX;
        copy.startOffsetY = source.startOffsetY;
        copy.startOffsetZ = source.startOffsetZ;
        copy.shaderCompatibility = source.shaderCompatibility;
        return copy;
    }

    public static final class Config {
        public String shaderCompatibility;
        public String selectedArcaneProfile = DEFAULT_PROFILE;
        public String selectedRailProfile = DEFAULT_PROFILE;
        public BeamSettings arcane = defaultArcaneSettings();
        public BeamSettings rail = defaultRailSettings();
        public LinkedHashMap<String, BeamSettings> arcaneProfiles;
        public LinkedHashMap<String, BeamSettings> railProfiles;
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
        public String shaderCompatibility;

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
            this.shaderCompatibility = ShaderCompatibility.OFF.id;
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
        OPTION_2("option_2", "Option 2"),
        RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
        RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

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
