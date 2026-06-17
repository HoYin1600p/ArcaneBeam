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
        if (INSTANCE.lightningStrike == null) {
            INSTANCE.lightningStrike = defaultLightningStrikeSettings();
        }
        if (INSTANCE.vaultAltar == null) {
            INSTANCE.vaultAltar = defaultVaultAltarSettings();
        }
        validateShaderCompatibility();
        validateBeamSettings(INSTANCE.arcane, false);
        validateBeamSettings(INSTANCE.rail, true);
        validateLightningStrikeSettings(INSTANCE.lightningStrike);
        validateVaultAltarSettings(INSTANCE.vaultAltar);
        INSTANCE.arcaneProfiles = validateProfiles(INSTANCE.arcaneProfiles, INSTANCE.arcane, false);
        INSTANCE.railProfiles = validateProfiles(INSTANCE.railProfiles, INSTANCE.rail, true);
        INSTANCE.lightningStrikeProfiles = validateLightningProfiles(INSTANCE.lightningStrikeProfiles, INSTANCE.lightningStrike);
        INSTANCE.vaultAltarProfiles = validateVaultAltarProfiles(INSTANCE.vaultAltarProfiles, INSTANCE.vaultAltar);
        INSTANCE.selectedArcaneProfile = validateSelectedProfile(INSTANCE.selectedArcaneProfile, INSTANCE.arcaneProfiles);
        INSTANCE.selectedRailProfile = validateSelectedProfile(INSTANCE.selectedRailProfile, INSTANCE.railProfiles);
        INSTANCE.selectedLightningStrikeProfile = validateSelectedLightningProfile(INSTANCE.selectedLightningStrikeProfile, INSTANCE.lightningStrikeProfiles);
        INSTANCE.selectedVaultAltarProfile = validateSelectedVaultAltarProfile(INSTANCE.selectedVaultAltarProfile, INSTANCE.vaultAltarProfiles);
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

    private static LinkedHashMap<String, VaultAltarSettings> validateVaultAltarProfiles(Map<String, VaultAltarSettings> profiles, VaultAltarSettings migrationSettings) {
        LinkedHashMap<String, VaultAltarSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, VaultAltarSettings> entry : profiles.entrySet()) {
                String name = normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                VaultAltarSettings settings = entry.getValue() == null ? defaultVaultAltarSettings() : entry.getValue();
                validateVaultAltarSettings(settings);
                validated.put(uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            VaultAltarSettings settings = copyOf(migrationSettings == null ? defaultVaultAltarSettings() : migrationSettings);
            validateVaultAltarSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    private static LinkedHashMap<String, LightningStrikeSettings> validateLightningProfiles(Map<String, LightningStrikeSettings> profiles, LightningStrikeSettings migrationSettings) {
        LinkedHashMap<String, LightningStrikeSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, LightningStrikeSettings> entry : profiles.entrySet()) {
                String name = normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                LightningStrikeSettings settings = entry.getValue() == null ? defaultLightningStrikeSettings() : entry.getValue();
                validateLightningStrikeSettings(settings);
                validated.put(uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            LightningStrikeSettings settings = copyOf(migrationSettings == null ? defaultLightningStrikeSettings() : migrationSettings);
            validateLightningStrikeSettings(settings);
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

    private static String validateSelectedLightningProfile(String selectedProfile, LinkedHashMap<String, LightningStrikeSettings> profiles) {
        String normalized = normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    private static String validateSelectedVaultAltarProfile(String selectedProfile, LinkedHashMap<String, VaultAltarSettings> profiles) {
        String normalized = normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    private static void activateSelectedProfiles() {
        INSTANCE.arcane = INSTANCE.arcaneProfiles.get(INSTANCE.selectedArcaneProfile);
        INSTANCE.rail = INSTANCE.railProfiles.get(INSTANCE.selectedRailProfile);
        INSTANCE.lightningStrike = INSTANCE.lightningStrikeProfiles.get(INSTANCE.selectedLightningStrikeProfile);
        INSTANCE.vaultAltar = INSTANCE.vaultAltarProfiles.get(INSTANCE.selectedVaultAltarProfile);
        INSTANCE.shaderCompatibility = INSTANCE.arcane.shaderCompatibility;
    }

    private static void syncActiveProfiles() {
        if (INSTANCE.arcaneProfiles != null && INSTANCE.selectedArcaneProfile != null && INSTANCE.arcane != null) {
            INSTANCE.arcaneProfiles.put(INSTANCE.selectedArcaneProfile, INSTANCE.arcane);
        }
        if (INSTANCE.railProfiles != null && INSTANCE.selectedRailProfile != null && INSTANCE.rail != null) {
            INSTANCE.railProfiles.put(INSTANCE.selectedRailProfile, INSTANCE.rail);
        }
        if (INSTANCE.lightningStrikeProfiles != null && INSTANCE.selectedLightningStrikeProfile != null && INSTANCE.lightningStrike != null) {
            INSTANCE.lightningStrikeProfiles.put(INSTANCE.selectedLightningStrikeProfile, INSTANCE.lightningStrike);
        }
        if (INSTANCE.vaultAltarProfiles != null && INSTANCE.selectedVaultAltarProfile != null && INSTANCE.vaultAltar != null) {
            INSTANCE.vaultAltarProfiles.put(INSTANCE.selectedVaultAltarProfile, INSTANCE.vaultAltar);
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

    private static void validateLightningStrikeSettings(LightningStrikeSettings settings) {
        settings.startRadius = clampFloat(settings.startRadius <= 0.0F ? 0.37766665F : settings.startRadius, 0.1F, 32.0F);
        settings.endRadius = clampFloat(settings.endRadius <= 0.0F ? 6.0F : settings.endRadius, settings.startRadius, 64.0F);
        settings.lifetimeTicks = clampInt(settings.lifetimeTicks <= 0 ? 30 : settings.lifetimeTicks, 1, 200);
        settings.ringThickness = clampFloat(settings.ringThickness <= 0.0F ? 0.8334F : settings.ringThickness, 0.02F, 4.0F);
        settings.ringSideCount = clampInt(settings.ringSideCount <= 0 ? 16 : settings.ringSideCount, 8, 96);
        settings.renderYOffset = clampFloat(settings.renderYOffset, -4.0F, 8.0F);
        if (settings.ringColor == 0) {
            settings.ringColor = 883199;
        }
        if (settings.centerFlashColor == 0) {
            settings.centerFlashColor = 911869;
        }
        settings.alpha = clampFloat(settings.alpha <= 0.0F ? 1.0F : settings.alpha, 0.0F, 1.0F);
        settings.ringInteriorOpacity = clampFloat(settings.ringInteriorOpacity <= 0.0F ? 0.28F : settings.ringInteriorOpacity, 0.0F, 1.0F);
        if (settings.sphereColor == 0) {
            settings.sphereColor = settings.centerFlashColor == 0 ? 911869 : settings.centerFlashColor;
        }
        settings.sphereRadius = clampFloat(settings.sphereRadius <= 0.0F ? 0.24309859F : settings.sphereRadius, 0.02F, 2.0F);
        settings.sphereOpacity = clampFloat(settings.sphereOpacity <= 0.0F ? 0.6056338F : settings.sphereOpacity, 0.0F, 1.0F);
        if (settings.coneColor == 0) {
            settings.coneColor = settings.ringColor == 0 ? 884989 : settings.ringColor;
        }
        settings.coneHeight = clampFloat(settings.coneHeight <= 0.0F ? 2.6059859F : settings.coneHeight, 0.05F, 6.0F);
        settings.coneRadius = clampFloat(settings.coneRadius <= 0.0F ? 0.4684507F : settings.coneRadius, 0.02F, 4.0F);
        settings.coneOpacity = clampFloat(settings.coneOpacity <= 0.0F ? 0.59859157F : settings.coneOpacity, 0.0F, 1.0F);
        if (settings.spotColor == 0) {
            settings.spotColor = 12975586;
        }
        settings.spotCount = clampInt(settings.spotCount, 0, 128);
        settings.spotSize = clampFloat(settings.spotSize <= 0.0F ? 0.15549296F : settings.spotSize, 0.02F, 1.5F);
        settings.spotOpacity = clampFloat(settings.spotOpacity <= 0.0F ? 0.9929578F : settings.spotOpacity, 0.0F, 1.0F);
        if (!settings.shaderCompatibilityMigrated) {
            ShaderCompatibility inherited = ShaderCompatibility.fromId(INSTANCE.shaderCompatibility);
            if (settings.shaderCompatibility == null || ShaderCompatibility.fromId(settings.shaderCompatibility) == null
                    || inherited == ShaderCompatibility.ON && ShaderCompatibility.OFF.id.equals(settings.shaderCompatibility)) {
                settings.shaderCompatibility = inherited == null ? ShaderCompatibility.OFF.id : inherited.id;
            }
            settings.shaderCompatibilityMigrated = true;
        }
        if (settings.shaderCompatibility == null || ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.ON.id;
        }
        if (settings.soundMode == null || LightningSoundMode.fromId(settings.soundMode) == null) {
            settings.soundMode = LightningSoundMode.SEISMIC_CHARGE.id;
        }
        settings.soundVolume = clampFloat(settings.soundVolume, 0.0F, 2.0F);
        settings.secondaryRippleCount = clampInt(settings.secondaryRippleCount, 0, 4);
        settings.secondaryRippleSize = clampFloat(settings.secondaryRippleSize <= 0.0F ? 1.4346666F : settings.secondaryRippleSize, 0.1F, 1.5F);
        settings.secondaryRippleDelayTicks = clampInt(settings.secondaryRippleDelayTicks < 0 ? 4 : settings.secondaryRippleDelayTicks, 0, 40);
    }

    private static void validateVaultAltarSettings(VaultAltarSettings settings) {
        if (settings.cornerColors == null || settings.cornerColors.length != 2) {
            settings.cornerColors = new int[]{0x66DDFF, 0xFFFFFF};
        }
        if (settings.centerColors == null || settings.centerColors.length != 2) {
            settings.centerColors = new int[]{0xD8FFFF, 0x5CB8FF};
        }
        if (settings.centerGlowColors == null || settings.centerGlowColors.length != 2) {
            settings.centerGlowColors = new int[]{0x55CFFF, 0xFFFFFF};
        }
        if (settings.shaderCompatibility == null || ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.ON.id;
        }
        settings.cornerRadius = clampFloat(settings.cornerRadius <= 0.0F ? 0.035F : settings.cornerRadius, 0.005F, 0.20F);
        settings.cornerOpacity = clampFloat(settings.cornerOpacity <= 0.0F ? 0.85F : settings.cornerOpacity, 0.0F, 1.0F);
        settings.cornerVerticalTicks = clampInt(settings.cornerVerticalTicks < 0 || settings.cornerVerticalTicks == 10 ? 20 : settings.cornerVerticalTicks, 0, 60);
        settings.cornerConvergeTicks = clampInt(settings.cornerConvergeTicks <= 0 || settings.cornerConvergeTicks == 10 ? 30 : settings.cornerConvergeTicks, 1, 120);
        settings.centerGrowTicks = clampInt(settings.centerGrowTicks <= 0 ? 40 : settings.centerGrowTicks, 1, 160);
        if (!settings.cornerOriginMigrated) {
            settings.cornerOriginHeight = 2.0F;
            settings.cornerOriginRadius = 0.7F;
            settings.cornerOriginMigrated = true;
        }
        settings.cornerOriginHeight = clampFloat(settings.cornerOriginHeight, 0.0F, 15.0F);
        settings.cornerOriginRadius = clampFloat(settings.cornerOriginRadius, 0.0F, 15.0F);
        settings.centerHeight = clampFloat(settings.centerHeight <= 0.0F ? 3.0F : settings.centerHeight, 0.1F, 3.0F);
        settings.centerFadeHeight = clampFloat(settings.centerFadeHeight <= 0.0F ? settings.centerHeight : settings.centerFadeHeight, 0.05F, 3.0F);
        settings.centerBottomRadius = clampFloat(settings.centerBottomRadius <= 0.0F ? 0.06F : settings.centerBottomRadius, 0.005F, 0.50F);
        settings.centerTopRadius = clampFloat(settings.centerTopRadius <= 0.0F ? 0.04F : settings.centerTopRadius, 0.005F, 0.50F);
        settings.centerOpacity = clampFloat(settings.centerOpacity <= 0.0F ? 0.80F : settings.centerOpacity, 0.0F, 1.0F);
        settings.centerGlowHeight = clampFloat(settings.centerGlowHeight <= 0.0F ? settings.centerHeight : settings.centerGlowHeight, 0.1F, 3.0F);
        settings.centerGlowFadeHeight = clampFloat(settings.centerGlowFadeHeight <= 0.0F ? settings.centerGlowHeight : settings.centerGlowFadeHeight, 0.05F, 3.0F);
        settings.centerGlowBottomRadius = clampFloat(settings.centerGlowBottomRadius <= 0.0F ? 0.11F : settings.centerGlowBottomRadius, 0.005F, 0.75F);
        settings.centerGlowTopRadius = clampFloat(settings.centerGlowTopRadius <= 0.0F ? 0.08F : settings.centerGlowTopRadius, 0.005F, 0.75F);
        settings.centerGlowOpacity = clampFloat(settings.centerGlowOpacity <= 0.0F ? 0.25F : settings.centerGlowOpacity, 0.0F, 1.0F);
        settings.centerGlowRotationRpm = clampFloat(settings.centerGlowRotationRpm, 0.0F, 120.0F);
        settings.soundVolume = clampFloat(settings.soundVolume, 0.0F, 2.0F);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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

    public static List<String> lightningProfileNames() {
        return new ArrayList<>(INSTANCE.lightningStrikeProfiles.keySet());
    }

    public static String selectedLightningProfileName() {
        return INSTANCE.selectedLightningStrikeProfile;
    }

    public static List<String> vaultAltarProfileNames() {
        return new ArrayList<>(INSTANCE.vaultAltarProfiles.keySet());
    }

    public static String selectedVaultAltarProfileName() {
        return INSTANCE.selectedVaultAltarProfile;
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

    public static void selectLightningProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.lightningStrikeProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedLightningStrikeProfile = normalized;
        INSTANCE.lightningStrike = INSTANCE.lightningStrikeProfiles.get(normalized);
        save();
    }

    public static String addLightningProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.lightningStrikeProfiles, baseName);
        LightningStrikeSettings settings = copyOf(INSTANCE.lightningStrike);
        validateLightningStrikeSettings(settings);
        INSTANCE.lightningStrikeProfiles.put(profileName, settings);
        INSTANCE.selectedLightningStrikeProfile = profileName;
        INSTANCE.lightningStrike = settings;
        save();
        return profileName;
    }

    public static void selectVaultAltarProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.vaultAltarProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedVaultAltarProfile = normalized;
        INSTANCE.vaultAltar = INSTANCE.vaultAltarProfiles.get(normalized);
        save();
    }

    public static String addVaultAltarProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.vaultAltarProfiles, baseName);
        VaultAltarSettings settings = copyOf(INSTANCE.vaultAltar);
        validateVaultAltarSettings(settings);
        INSTANCE.vaultAltarProfiles.put(profileName, settings);
        INSTANCE.selectedVaultAltarProfile = profileName;
        INSTANCE.vaultAltar = settings;
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

    private static String uniqueProfileName(Map<String, ?> profiles, String baseName) {
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

    private static LightningStrikeSettings copyOf(LightningStrikeSettings source) {
        LightningStrikeSettings copy = new LightningStrikeSettings();
        copy.enabled = source.enabled;
        copy.startRadius = source.startRadius;
        copy.endRadius = source.endRadius;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.ringThickness = source.ringThickness;
        copy.ringSideCount = source.ringSideCount;
        copy.renderYOffset = source.renderYOffset;
        copy.ringColor = source.ringColor;
        copy.centerFlashColor = source.centerFlashColor;
        copy.alpha = source.alpha;
        copy.ringInteriorOpacity = source.ringInteriorOpacity;
        copy.sphereColor = source.sphereColor;
        copy.sphereRadius = source.sphereRadius;
        copy.sphereOpacity = source.sphereOpacity;
        copy.coneColor = source.coneColor;
        copy.coneHeight = source.coneHeight;
        copy.coneRadius = source.coneRadius;
        copy.coneOpacity = source.coneOpacity;
        copy.spotColor = source.spotColor;
        copy.spotCount = source.spotCount;
        copy.spotSize = source.spotSize;
        copy.spotOpacity = source.spotOpacity;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.shaderCompatibilityMigrated = source.shaderCompatibilityMigrated;
        copy.secondaryRippleCount = source.secondaryRippleCount;
        copy.secondaryRippleSize = source.secondaryRippleSize;
        copy.secondaryRippleDelayTicks = source.secondaryRippleDelayTicks;
        copy.soundMode = source.soundMode;
        copy.soundVolume = source.soundVolume;
        return copy;
    }

    private static VaultAltarSettings copyOf(VaultAltarSettings source) {
        VaultAltarSettings copy = new VaultAltarSettings();
        copy.enabled = source.enabled;
        copy.cornerColors = source.cornerColors == null ? null : source.cornerColors.clone();
        copy.cornerRadius = source.cornerRadius;
        copy.cornerOpacity = source.cornerOpacity;
        copy.cornerVerticalTicks = source.cornerVerticalTicks;
        copy.cornerConvergeTicks = source.cornerConvergeTicks;
        copy.centerGrowTicks = source.centerGrowTicks;
        copy.cornerOriginHeight = source.cornerOriginHeight;
        copy.cornerOriginRadius = source.cornerOriginRadius;
        copy.cornerOriginMigrated = source.cornerOriginMigrated;
        copy.centerColors = source.centerColors == null ? null : source.centerColors.clone();
        copy.centerHeight = source.centerHeight;
        copy.centerFadeHeight = source.centerFadeHeight;
        copy.centerBottomRadius = source.centerBottomRadius;
        copy.centerTopRadius = source.centerTopRadius;
        copy.centerOpacity = source.centerOpacity;
        copy.centerGlowColors = source.centerGlowColors == null ? null : source.centerGlowColors.clone();
        copy.centerGlowHeight = source.centerGlowHeight;
        copy.centerGlowFadeHeight = source.centerGlowFadeHeight;
        copy.centerGlowBottomRadius = source.centerGlowBottomRadius;
        copy.centerGlowTopRadius = source.centerGlowTopRadius;
        copy.centerGlowOpacity = source.centerGlowOpacity;
        copy.centerGlowRotationRpm = source.centerGlowRotationRpm;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.soundVolume = source.soundVolume;
        return copy;
    }

    public static final class Config {
        public String shaderCompatibility;
        public String selectedArcaneProfile = DEFAULT_PROFILE;
        public String selectedRailProfile = DEFAULT_PROFILE;
        public String selectedLightningStrikeProfile = DEFAULT_PROFILE;
        public String selectedVaultAltarProfile = DEFAULT_PROFILE;
        public BeamSettings arcane = defaultArcaneSettings();
        public BeamSettings rail = defaultRailSettings();
        public LightningStrikeSettings lightningStrike = defaultLightningStrikeSettings();
        public VaultAltarSettings vaultAltar = defaultVaultAltarSettings();
        public LinkedHashMap<String, BeamSettings> arcaneProfiles;
        public LinkedHashMap<String, BeamSettings> railProfiles;
        public LinkedHashMap<String, LightningStrikeSettings> lightningStrikeProfiles;
        public LinkedHashMap<String, VaultAltarSettings> vaultAltarProfiles;
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

    public static final class LightningStrikeSettings {
        public boolean enabled = true;
        public float startRadius = 0.37766665F;
        public float endRadius = 6.0F;
        public int lifetimeTicks = 30;
        public float ringThickness = 0.8334F;
        public int ringSideCount = 16;
        public float renderYOffset = 1.0F;
        public int ringColor = 883199;
        public int centerFlashColor = 911869;
        public float alpha = 1.0F;
        public float ringInteriorOpacity = 0.28F;
        public int sphereColor = 911869;
        public float sphereRadius = 0.24309859F;
        public float sphereOpacity = 0.6056338F;
        public int coneColor = 884989;
        public float coneHeight = 2.6059859F;
        public float coneRadius = 0.4684507F;
        public float coneOpacity = 0.59859157F;
        public int spotColor = 12975586;
        public int spotCount = 105;
        public float spotSize = 0.15549296F;
        public float spotOpacity = 0.9929578F;
        public boolean fullbright = true;
        public String shaderCompatibility = ShaderCompatibility.ON.id;
        public boolean shaderCompatibilityMigrated = true;
        public int secondaryRippleCount = 0;
        public float secondaryRippleSize = 1.4346666F;
        public int secondaryRippleDelayTicks = 4;
        public String soundMode = LightningSoundMode.SEISMIC_CHARGE.id;
        public float soundVolume = 0.36F;
    }

    private static LightningStrikeSettings defaultLightningStrikeSettings() {
        return new LightningStrikeSettings();
    }

    public static final class VaultAltarSettings {
        public boolean enabled = true;
        public int[] cornerColors = new int[]{0x66DDFF, 0xFFFFFF};
        public float cornerRadius = 0.035F;
        public float cornerOpacity = 0.85F;
        public int cornerVerticalTicks = 20;
        public int cornerConvergeTicks = 30;
        public int centerGrowTicks = 40;
        public float cornerOriginHeight = 2.0F;
        public float cornerOriginRadius = 0.7F;
        public boolean cornerOriginMigrated = false;
        public int[] centerColors = new int[]{0xD8FFFF, 0x5CB8FF};
        public float centerHeight = 3.0F;
        public float centerFadeHeight = 3.0F;
        public float centerBottomRadius = 0.06F;
        public float centerTopRadius = 0.04F;
        public float centerOpacity = 0.80F;
        public int[] centerGlowColors = new int[]{0x55CFFF, 0xFFFFFF};
        public float centerGlowHeight = 3.0F;
        public float centerGlowFadeHeight = 3.0F;
        public float centerGlowBottomRadius = 0.11F;
        public float centerGlowTopRadius = 0.08F;
        public float centerGlowOpacity = 0.25F;
        public float centerGlowRotationRpm = 18.0F;
        public boolean fullbright = true;
        public String shaderCompatibility = ShaderCompatibility.ON.id;
        public float soundVolume = 0.35F;
    }

    private static VaultAltarSettings defaultVaultAltarSettings() {
        return new VaultAltarSettings();
    }

    public enum LightningSoundMode {
        DEFAULT("default", "Default"),
        SEISMIC_CHARGE("seismic_charge", "Seismic Charge"),
        RESOURCEPACK_1("resourcepack_1", "Resourcepack1"),
        RESOURCEPACK_2("resourcepack_2", "Resourcepack2");

        public final String id;
        public final String label;

        LightningSoundMode(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static LightningSoundMode fromId(String id) {
            for (LightningSoundMode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
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
