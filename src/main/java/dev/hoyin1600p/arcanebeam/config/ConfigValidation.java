package dev.hoyin1600p.arcanebeam.config;

import dev.hoyin1600p.arcanebeam.config.model.ArchonSettings;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.BlastWaveSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.DungeonGlobeMapSettings;
import dev.hoyin1600p.arcanebeam.config.model.FadeInStyle;
import dev.hoyin1600p.arcanebeam.config.model.FadeOutStyle;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthVisibility;
import dev.hoyin1600p.arcanebeam.config.model.LightningSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.LightningStrikeSettings;
import dev.hoyin1600p.arcanebeam.config.model.ShaderCompatibility;
import dev.hoyin1600p.arcanebeam.config.model.SmiteSettings;
import dev.hoyin1600p.arcanebeam.config.model.SoundChoice;
import dev.hoyin1600p.arcanebeam.config.model.StartHand;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowProjectileSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import dev.hoyin1600p.arcanebeam.config.model.UiSettings;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.UpdateSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSoundMode;
import dev.hoyin1600p.arcanebeam.update.UpdateNoticeFilter;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig.INSTANCE;
import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.DEFAULT_PROFILE;
import static dev.hoyin1600p.arcanebeam.util.IntMath.clamp;

/** Clamps, normalises and repairs every config value after a load or an edit. */
final class ConfigValidation {
    private ConfigValidation() {
    }

    /** Resource-pack particle size bounds; the settings screen slider lower bound is the same value. */
    static final float MIN_PARTICLE_SCALE = 0.1F;
    static final float MAX_PARTICLE_SCALE = 8.0F;

    static void validate() {
        Config defaults = ConfigDefaults.defaultConfig();
        if (INSTANCE.updates == null) {
            INSTANCE.updates = new UpdateSettings();
        }
        INSTANCE.updates.updateTypes = UpdateNoticeFilter.fromConfigValue(
                INSTANCE.updates.updateTypes,
                UpdateNoticeFilter.CRITICAL
        ).name();
        if (INSTANCE.dungeonGlobeMap == null) {
            INSTANCE.dungeonGlobeMap = new DungeonGlobeMapSettings();
        }
        if (INSTANCE.shaderCompatibility == null) {
            INSTANCE.shaderCompatibility = defaults.shaderCompatibility == null ? ShaderCompatibility.OFF.id : defaults.shaderCompatibility;
        }
        if (INSTANCE.arcane == null) {
            INSTANCE.arcane = ConfigCopies.copyOf(defaults.arcane == null ? BeamSettings.defaultArcane() : defaults.arcane);
        }
        if (INSTANCE.rail == null) {
            INSTANCE.rail = ConfigCopies.copyOf(defaults.rail == null ? BeamSettings.defaultRail() : defaults.rail);
        }
        if (INSTANCE.lightningStrike == null) {
            INSTANCE.lightningStrike = ConfigCopies.copyOf(defaults.lightningStrike == null ? new LightningStrikeSettings() : defaults.lightningStrike);
        }
        if (INSTANCE.vaultAltar == null) {
            INSTANCE.vaultAltar = ConfigCopies.copyOf(defaults.vaultAltar == null ? new VaultAltarSettings() : defaults.vaultAltar);
        }
        if (INSTANCE.stormArrow == null) {
            INSTANCE.stormArrow = ConfigCopies.copyOf(defaults.stormArrow == null ? new StormArrowSettings() : defaults.stormArrow);
        }
        if (INSTANCE.smite == null) {
            INSTANCE.smite = ConfigCopies.copyOf(defaults.smite == null ? new SmiteSettings() : defaults.smite);
        }
        if (INSTANCE.archon == null) {
            INSTANCE.archon = ConfigCopies.copyOf(defaults.archon == null ? new ArchonSettings() : defaults.archon);
        }
        if (INSTANCE.blastWave == null) {
            INSTANCE.blastWave = ConfigCopies.copyOf(defaults.blastWave == null ? new BlastWaveSettings() : defaults.blastWave);
        }
        if (INSTANCE.ui == null) {
            INSTANCE.ui = new UiSettings();
        }
        INSTANCE.ui.panelSide = UiSettings.RIGHT.equalsIgnoreCase(INSTANCE.ui.panelSide) ? UiSettings.RIGHT : UiSettings.LEFT;
        if (INSTANCE.labyrinth == null) {
            INSTANCE.labyrinth = new LabyrinthSettings();
        }
        validateLabyrinthSettings(INSTANCE.labyrinth);
        validateShaderCompatibility();
        validateDungeonGlobeMapSettings(INSTANCE.dungeonGlobeMap);
        validateBeamSettings(INSTANCE.arcane, false);
        validateBeamSettings(INSTANCE.rail, true);
        validateLightningStrikeSettings(INSTANCE.lightningStrike);
        validateVaultAltarSettings(INSTANCE.vaultAltar);
        validateStormArrowSettings(INSTANCE.stormArrow);
        validateStormArrowSettings(INSTANCE.smite);
        validateArchonSettings(INSTANCE.archon);
        validateBlastWaveSettings(INSTANCE.blastWave);
        INSTANCE.arcaneProfiles = validateProfiles(INSTANCE.arcaneProfiles, INSTANCE.arcane, false);
        INSTANCE.railProfiles = validateProfiles(INSTANCE.railProfiles, INSTANCE.rail, true);
        INSTANCE.lightningStrikeProfiles = validateLightningProfiles(INSTANCE.lightningStrikeProfiles, INSTANCE.lightningStrike);
        INSTANCE.vaultAltarProfiles = validateVaultAltarProfiles(INSTANCE.vaultAltarProfiles, INSTANCE.vaultAltar);
        INSTANCE.stormArrowProfiles = validateStormArrowProfiles(INSTANCE.stormArrowProfiles, INSTANCE.stormArrow);
        INSTANCE.smiteProfiles = validateSmiteProfiles(INSTANCE.smiteProfiles, INSTANCE.smite);
        INSTANCE.archonProfiles = validateArchonProfiles(INSTANCE.archonProfiles, INSTANCE.archon);
        INSTANCE.blastWaveProfiles = validateBlastWaveProfiles(INSTANCE.blastWaveProfiles, INSTANCE.blastWave);
        INSTANCE.selectedArcaneProfile = validateSelectedProfile(INSTANCE.selectedArcaneProfile, INSTANCE.arcaneProfiles);
        INSTANCE.selectedRailProfile = validateSelectedProfile(INSTANCE.selectedRailProfile, INSTANCE.railProfiles);
        INSTANCE.selectedLightningStrikeProfile = validateSelectedLightningProfile(INSTANCE.selectedLightningStrikeProfile, INSTANCE.lightningStrikeProfiles);
        INSTANCE.selectedVaultAltarProfile = validateSelectedVaultAltarProfile(INSTANCE.selectedVaultAltarProfile, INSTANCE.vaultAltarProfiles);
        INSTANCE.selectedStormArrowProfile = validateSelectedStormArrowProfile(INSTANCE.selectedStormArrowProfile, INSTANCE.stormArrowProfiles);
        INSTANCE.selectedSmiteProfile = validateSelectedSmiteProfile(INSTANCE.selectedSmiteProfile, INSTANCE.smiteProfiles);
        INSTANCE.selectedArchonProfile = validateSelectedArchonProfile(INSTANCE.selectedArchonProfile, INSTANCE.archonProfiles);
        INSTANCE.selectedBlastWaveProfile = validateSelectedBlastWaveProfile(INSTANCE.selectedBlastWaveProfile, INSTANCE.blastWaveProfiles);
        ConfigProfiles.activateSelectedProfiles();
    }

    static void validateBeamSettings(BeamSettings settings, boolean rail) {
        BeamSettings defaults = ConfigDefaults.defaultSettings(rail);
        settings.maxRange = clampDouble(settings.maxRange, defaults.maxRange, 1.0D, 512.0D);
        settings.lifetimeTicks = clamp(settings.lifetimeTicks, 1, 200);
        settings.particleScale = clampFloat(settings.particleScale, defaults.particleScale, MIN_PARTICLE_SCALE, MAX_PARTICLE_SCALE);
        settings.particleRotationDegrees = normalizeDegrees(settings.particleRotationDegrees, defaults.particleRotationDegrees);
        validateShape(settings, defaults);
        validateColors(settings);
        validateColorShift(settings);
        validateSound(settings);
        validateOrigin(settings);
        validateTransitions(settings, FadeInStyle.FADE, rail ? 1 : 5, rail ? FadeOutStyle.FADE : FadeOutStyle.SHRINK, rail ? 4 : 10);
        validateShaderCompatibility(settings);
    }

    static LinkedHashMap<String, BeamSettings> validateProfiles(Map<String, BeamSettings> profiles, BeamSettings migrationSettings, boolean rail) {
        LinkedHashMap<String, BeamSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, BeamSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                BeamSettings settings = entry.getValue() == null ? ConfigDefaults.defaultSettings(rail) : entry.getValue();
                validateBeamSettings(settings, rail);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            BeamSettings settings = ConfigCopies.copyOf(migrationSettings == null ? ConfigDefaults.defaultSettings(rail) : migrationSettings);
            validateBeamSettings(settings, rail);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, VaultAltarSettings> validateVaultAltarProfiles(Map<String, VaultAltarSettings> profiles,
            VaultAltarSettings migrationSettings) {
        LinkedHashMap<String, VaultAltarSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, VaultAltarSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                VaultAltarSettings settings = entry.getValue() == null ? new VaultAltarSettings() : entry.getValue();
                validateVaultAltarSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            VaultAltarSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new VaultAltarSettings() : migrationSettings);
            validateVaultAltarSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, LightningStrikeSettings> validateLightningProfiles(Map<String, LightningStrikeSettings> profiles,
            LightningStrikeSettings migrationSettings) {
        LinkedHashMap<String, LightningStrikeSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, LightningStrikeSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                LightningStrikeSettings settings = entry.getValue() == null ? new LightningStrikeSettings() : entry.getValue();
                validateLightningStrikeSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            LightningStrikeSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new LightningStrikeSettings() : migrationSettings);
            validateLightningStrikeSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, StormArrowSettings> validateStormArrowProfiles(Map<String, StormArrowSettings> profiles,
            StormArrowSettings migrationSettings) {
        LinkedHashMap<String, StormArrowSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, StormArrowSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                StormArrowSettings settings = entry.getValue() == null ? new StormArrowSettings() : entry.getValue();
                validateStormArrowSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            StormArrowSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new StormArrowSettings() : migrationSettings);
            validateStormArrowSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, SmiteSettings> validateSmiteProfiles(Map<String, SmiteSettings> profiles, SmiteSettings migrationSettings) {
        LinkedHashMap<String, SmiteSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, SmiteSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                SmiteSettings settings = entry.getValue() == null ? new SmiteSettings() : entry.getValue();
                validateStormArrowSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            SmiteSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new SmiteSettings() : migrationSettings);
            validateStormArrowSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, ArchonSettings> validateArchonProfiles(Map<String, ArchonSettings> profiles, ArchonSettings migrationSettings) {
        LinkedHashMap<String, ArchonSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, ArchonSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                ArchonSettings settings = entry.getValue() == null ? new ArchonSettings() : entry.getValue();
                validateArchonSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            ArchonSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new ArchonSettings() : migrationSettings);
            validateArchonSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static LinkedHashMap<String, BlastWaveSettings> validateBlastWaveProfiles(Map<String, BlastWaveSettings> profiles,
            BlastWaveSettings migrationSettings) {
        LinkedHashMap<String, BlastWaveSettings> validated = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map.Entry<String, BlastWaveSettings> entry : profiles.entrySet()) {
                String name = ConfigProfiles.normalizeProfileName(entry.getKey());
                if (name.isEmpty()) {
                    continue;
                }
                BlastWaveSettings settings = entry.getValue() == null ? new BlastWaveSettings() : entry.getValue();
                validateBlastWaveSettings(settings);
                validated.put(ConfigProfiles.uniqueProfileName(validated, name), settings);
            }
        }
        if (validated.isEmpty()) {
            BlastWaveSettings settings = ConfigCopies.copyOf(migrationSettings == null ? new BlastWaveSettings() : migrationSettings);
            validateBlastWaveSettings(settings);
            validated.put(DEFAULT_PROFILE, settings);
        }
        return validated;
    }

    static String validateSelectedProfile(String selectedProfile, LinkedHashMap<String, BeamSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedLightningProfile(String selectedProfile, LinkedHashMap<String, LightningStrikeSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedVaultAltarProfile(String selectedProfile, LinkedHashMap<String, VaultAltarSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedStormArrowProfile(String selectedProfile, LinkedHashMap<String, StormArrowSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedSmiteProfile(String selectedProfile, LinkedHashMap<String, SmiteSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedArchonProfile(String selectedProfile, LinkedHashMap<String, ArchonSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static String validateSelectedBlastWaveProfile(String selectedProfile, LinkedHashMap<String, BlastWaveSettings> profiles) {
        String normalized = ConfigProfiles.normalizeProfileName(selectedProfile);
        if (!normalized.isEmpty() && profiles.containsKey(normalized)) {
            return normalized;
        }
        return profiles.keySet().iterator().next();
    }

    static void validateShape(BeamSettings settings, BeamSettings defaults) {
        settings.intensity = clampFloat(settings.intensity, defaults.intensity, 0.005F, 1.0F);
        settings.opacity = clampFloat(settings.opacity, defaults.opacity, 0.0F, 1.0F);
        settings.glowRadius = clampFloat(settings.glowRadius, defaults.glowRadius, 0.005F, 2.0F);
        settings.glowOpacity = clampFloat(settings.glowOpacity, 0.20F, 0.0F, 1.0F);
        settings.radius = settings.intensity;
        settings.alpha = settings.opacity;
    }

    static void validateColors(BeamSettings settings) {
        settings.colors = normalizePalette(settings.colors, 4, new int[]{settings.color, settings.color, settings.color, 0xFFFFFF});
        settings.glowColors = normalizePalette(settings.glowColors, 4, settings.colors);
    }

    /** Pads (repeating the last color) or truncates to {@code length}, keeping the caller's colors. */
    static int[] normalizePalette(int[] palette, int length, int[] fallback) {
        if (palette == null || palette.length == 0) {
            int[] result = normalizePalette(fallback, length, new int[length]);
            return result == fallback ? result.clone() : result;
        }
        if (palette.length == length) {
            return palette;
        }
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = i < palette.length ? palette[i] : palette[palette.length - 1];
        }
        return result;
    }

    static void validateSound(BeamSettings settings) {
        if (settings.sound == null || settings.sound.isBlank()) {
            settings.sound = SoundChoice.DEFAULT.id;
        } else if (SoundChoice.fromId(settings.sound) == null) {
            settings.sound = SoundChoice.DEFAULT.id;
        }
        settings.soundVolume = clampFloat(settings.soundVolume, 1.0F, 0.0F, 2.0F);
    }

    static void validateColorShift(BeamSettings settings) {
        settings.colorShiftTicks = clampFloat(settings.colorShiftTicks, 8.0F, 2.0F, 60.0F);
        settings.glowRotationRpm = clampFloat(settings.glowRotationRpm, 0.0F, 0.0F, 60.0F);
    }

    static void validateOrigin(BeamSettings settings) {
        if (settings.startHand == null || StartHand.fromId(settings.startHand) == null) {
            settings.startHand = StartHand.OFFHAND.id;
        }
        settings.startOffsetX = clampDouble(settings.startOffsetX, 0.38D, -64.0D, 64.0D);
        settings.startOffsetY = clampDouble(settings.startOffsetY, -0.45D, -64.0D, 64.0D);
        settings.startOffsetZ = clampDouble(settings.startOffsetZ, 0.18D, -64.0D, 64.0D);
    }

    static void validateTransitions(BeamSettings settings, FadeInStyle defaultFadeInStyle, int defaultFadeInTicks,
            FadeOutStyle defaultFadeOutStyle, int defaultFadeOutTicks) {
        if (FadeInStyle.fromId(settings.fadeInStyle) == null) {
            settings.fadeInStyle = defaultFadeInStyle.id;
        }
        if (FadeOutStyle.fromId(settings.fadeOutStyle) == null) {
            settings.fadeOutStyle = defaultFadeOutStyle.id;
        }
        settings.fadeInTicks = settings.fadeInTicks < 0 ? defaultFadeInTicks : Math.max(0, Math.min(99, settings.fadeInTicks));
        settings.fadeOutTicks = settings.fadeOutTicks < 0 ? defaultFadeOutTicks : Math.max(0, Math.min(99, settings.fadeOutTicks));
    }

    static void validateShaderCompatibility() {
        if (ShaderCompatibility.fromId(INSTANCE.shaderCompatibility) == null) {
            INSTANCE.shaderCompatibility = ShaderCompatibility.OFF.id;
        }
    }

    static void validateShaderCompatibility(BeamSettings settings) {
        if (settings.shaderCompatibility == null) {
            settings.shaderCompatibility = INSTANCE.shaderCompatibility;
        }
        if (ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.OFF.id;
        }
    }

    static void validateLightningStrikeSettings(LightningStrikeSettings settings) {
        LightningStrikeSettings d = new LightningStrikeSettings();
        settings.startRadius = clampFloat(settings.startRadius, d.startRadius, 0.1F, 32.0F);
        settings.endRadius = clampFloat(settings.endRadius, d.endRadius, settings.startRadius, 64.0F);
        settings.lifetimeTicks = clamp(settings.lifetimeTicks, 1, 200);
        settings.ringThickness = clampFloat(settings.ringThickness, d.ringThickness, 0.02F, 4.0F);
        settings.ringSideCount = clamp(settings.ringSideCount, 8, 96);
        settings.renderYOffset = clampFloat(settings.renderYOffset, d.renderYOffset, -4.0F, 8.0F);
        settings.alpha = clampFloat(settings.alpha, d.alpha, 0.0F, 1.0F);
        settings.ringInteriorOpacity = clampFloat(settings.ringInteriorOpacity, d.ringInteriorOpacity, 0.0F, 1.0F);
        settings.sphereRadius = clampFloat(settings.sphereRadius, d.sphereRadius, 0.02F, 2.0F);
        settings.sphereOpacity = clampFloat(settings.sphereOpacity, d.sphereOpacity, 0.0F, 1.0F);
        settings.coneHeight = clampFloat(settings.coneHeight, d.coneHeight, 0.05F, 6.0F);
        settings.coneRadius = clampFloat(settings.coneRadius, d.coneRadius, 0.02F, 4.0F);
        settings.coneOpacity = clampFloat(settings.coneOpacity, d.coneOpacity, 0.0F, 1.0F);
        settings.spotCount = clamp(settings.spotCount, 0, 128);
        settings.spotSize = clampFloat(settings.spotSize, d.spotSize, 0.02F, 1.5F);
        settings.spotOpacity = clampFloat(settings.spotOpacity, d.spotOpacity, 0.0F, 1.0F);
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
        settings.soundVolume = clampFloat(settings.soundVolume, d.soundVolume, 0.0F, 2.0F);
        settings.secondaryRippleCount = clamp(settings.secondaryRippleCount, 0, 4);
        settings.secondaryRippleSize = clampFloat(settings.secondaryRippleSize, d.secondaryRippleSize, 0.1F, 1.5F);
        settings.secondaryRippleDelayTicks = clamp(settings.secondaryRippleDelayTicks, 0, 40);
    }

    static void validateVaultAltarSettings(VaultAltarSettings settings) {
        VaultAltarSettings d = new VaultAltarSettings();
        settings.cornerColors = normalizePalette(settings.cornerColors, 2, d.cornerColors);
        settings.centerColors = normalizePalette(settings.centerColors, 2, d.centerColors);
        settings.centerGlowColors = normalizePalette(settings.centerGlowColors, 2, d.centerGlowColors);
        if (settings.shaderCompatibility == null || ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.ON.id;
        }
        settings.cornerRadius = clampFloat(settings.cornerRadius, d.cornerRadius, 0.005F, 0.20F);
        settings.cornerOpacity = clampFloat(settings.cornerOpacity, d.cornerOpacity, 0.0F, 1.0F);
        settings.cornerVerticalTicks = clamp(settings.cornerVerticalTicks, 0, 60);
        settings.cornerConvergeTicks = clamp(settings.cornerConvergeTicks, 1, 120);
        settings.centerGrowTicks = clamp(settings.centerGrowTicks, 1, 160);
        if (!settings.cornerOriginMigrated) {
            settings.cornerOriginHeight = 2.0F;
            settings.cornerOriginRadius = 0.7F;
            settings.cornerOriginMigrated = true;
        }
        settings.cornerOriginHeight = clampFloat(settings.cornerOriginHeight, d.cornerOriginHeight, 0.0F, 15.0F);
        settings.cornerOriginRadius = clampFloat(settings.cornerOriginRadius, d.cornerOriginRadius, 0.0F, 15.0F);
        settings.centerHeight = clampFloat(settings.centerHeight, d.centerHeight, 0.1F, 3.0F);
        settings.centerFadeHeight = clampFloat(settings.centerFadeHeight, settings.centerHeight, 0.05F, 3.0F);
        settings.centerBottomRadius = clampFloat(settings.centerBottomRadius, d.centerBottomRadius, 0.005F, 0.50F);
        settings.centerTopRadius = clampFloat(settings.centerTopRadius, d.centerTopRadius, 0.005F, 0.50F);
        settings.centerOpacity = clampFloat(settings.centerOpacity, d.centerOpacity, 0.0F, 1.0F);
        settings.centerGlowHeight = clampFloat(settings.centerGlowHeight, settings.centerHeight, 0.1F, 3.0F);
        settings.centerGlowFadeHeight = clampFloat(settings.centerGlowFadeHeight, settings.centerGlowHeight, 0.05F, 3.0F);
        settings.centerGlowBottomRadius = clampFloat(settings.centerGlowBottomRadius, d.centerGlowBottomRadius, 0.005F, 0.75F);
        settings.centerGlowTopRadius = clampFloat(settings.centerGlowTopRadius, d.centerGlowTopRadius, 0.005F, 0.75F);
        settings.centerGlowOpacity = clampFloat(settings.centerGlowOpacity, d.centerGlowOpacity, 0.0F, 1.0F);
        settings.centerGlowRotationRpm = clampFloat(settings.centerGlowRotationRpm, d.centerGlowRotationRpm, 0.0F, 120.0F);
        if (settings.soundMode == null || VaultAltarSoundMode.fromId(settings.soundMode) == null) {
            settings.soundMode = VaultAltarSoundMode.DEFAULT.id;
        }
        settings.soundVolume = clampFloat(settings.soundVolume, d.soundVolume, 0.0F, 2.0F);
    }

    static void validateDungeonGlobeMapSettings(DungeonGlobeMapSettings settings) {
        settings.opacityPercent = clamp(settings.opacityPercent, 0, 100);
        settings.sharedOpacityPercent = clamp(settings.sharedOpacityPercent, 0, 100);
    }

    /** Out-of-range numbers are clamped; NaN, infinite and unknown values fall back to the defaults. */
    static void validateLabyrinthSettings(LabyrinthSettings settings) {
        settings.visibility = LabyrinthVisibility.fromId(settings.visibility, LabyrinthVisibility.ALWAYS).id;
        settings.routeColor &= 0xFFFFFF;
        settings.opacity = clampFloat(settings.opacity, LabyrinthSettings.DEFAULT_OPACITY,
                LabyrinthSettings.MIN_OPACITY, LabyrinthSettings.MAX_OPACITY);
        settings.width = clampFloat(settings.width, LabyrinthSettings.DEFAULT_WIDTH,
                LabyrinthSettings.MIN_WIDTH, LabyrinthSettings.MAX_WIDTH);
        settings.thickness = clampFloat(settings.thickness, LabyrinthSettings.DEFAULT_THICKNESS,
                LabyrinthSettings.MIN_THICKNESS, LabyrinthSettings.MAX_THICKNESS);
        settings.elevation = clampFloat(settings.elevation, LabyrinthSettings.DEFAULT_ELEVATION,
                LabyrinthSettings.MIN_ELEVATION, LabyrinthSettings.MAX_ELEVATION);
        settings.maxDistance = clamp(settings.maxDistance, LabyrinthSettings.MIN_DISTANCE, LabyrinthSettings.MAX_DISTANCE);
    }

    static StormArrowSettings stormDefaults(StormArrowSettings settings) {
        if (settings instanceof BlastWaveSettings) {
            return new BlastWaveSettings();
        }
        if (settings instanceof ArchonSettings) {
            return new ArchonSettings();
        }
        if (settings instanceof SmiteSettings) {
            return new SmiteSettings();
        }
        return new StormArrowSettings();
    }

    static void validateStormArrowSettings(StormArrowSettings settings) {
        StormArrowSettings d = stormDefaults(settings);
        boolean blast = settings instanceof BlastWaveSettings;
        settings.circleAlpha = clampFloat(settings.circleAlpha, d.circleAlpha, 0.0F, 1.0F);
        settings.circleThickness = clampFloat(settings.circleThickness, d.circleThickness, 0.02F, 1.0F);
        settings.blasterAlpha = clampFloat(settings.blasterAlpha, d.blasterAlpha, 0.0F, 1.0F);
        settings.blasterWidth = clampFloat(settings.blasterWidth, d.blasterWidth, 0.01F, 1.0F);
        settings.segmentLength = clampFloat(settings.segmentLength, d.segmentLength, 0.1F, 12.0F);
        settings.lifetimeTicks = clamp(settings.lifetimeTicks, 1, blast ? 20 : 80);
        settings.originHeight = clampFloat(settings.originHeight, d.originHeight, blast ? 0.5F : 2.0F, blast ? 16.0F : 64.0F);
        settings.impactFlashSize = clampFloat(settings.impactFlashSize, d.impactFlashSize, 0.05F, 4.0F);
        if (settings.shaderCompatibility == null || ShaderCompatibility.fromId(settings.shaderCompatibility) == null) {
            settings.shaderCompatibility = ShaderCompatibility.ON.id;
        }
        if (settings.soundMode == null || StormArrowSoundMode.fromId(settings.soundMode) == null) {
            settings.soundMode = StormArrowSoundMode.DEFAULT.id;
        }
        if (settings.projectileSoundMode == null || StormArrowProjectileSoundMode.fromId(settings.projectileSoundMode) == null) {
            settings.projectileSoundMode = StormArrowProjectileSoundMode.OPTION_1.id;
        }
        settings.soundVolume = clampFloat(settings.soundVolume, d.soundVolume, 0.0F, 2.0F);
        settings.audioRange = clamp(settings.audioRange, 16, 32);
    }

    static void validateArchonSettings(ArchonSettings settings) {
        validateStormArrowSettings(settings);
        settings.missileOriginRadius = clampFloat(settings.missileOriginRadius, 2.0F, 0.0F, 16.0F);
    }

    static void validateBlastWaveSettings(BlastWaveSettings settings) {
        BlastWaveSettings d = new BlastWaveSettings();
        validateStormArrowSettings(settings);
        settings.originOffsetX = clampFloat(settings.originOffsetX, d.originOffsetX, -4.0F, 4.0F);
        settings.originOffsetY = clampFloat(settings.originOffsetY, d.originOffsetY, -4.0F, 4.0F);
        settings.originOffsetZ = clampFloat(settings.originOffsetZ, d.originOffsetZ, -4.0F, 4.0F);
        settings.lateralSpread = clampFloat(settings.lateralSpread, d.lateralSpread, 0.0F, 4.0F);
        settings.missileScale = clampFloat(settings.missileScale, d.missileScale, 0.25F, 3.0F);
        settings.maxMissiles = clamp(settings.maxMissiles, 1, 64);
        settings.maxSoundLayers = clamp(settings.maxSoundLayers, 1, 12);
        settings.soundStaggerTicks = clamp(settings.soundStaggerTicks, 0, 4);
        settings.pitchVariance = clampFloat(settings.pitchVariance, d.pitchVariance, 0.0F, 0.20F);
    }

    /** Clamps to [min, max]; NaN and infinities become {@code fallback} (itself clamped). */
    /** Brings an angle into 0 (inclusive) to 360 (exclusive); a non-finite value becomes the fallback. */
    static float normalizeDegrees(float degrees, float fallback) {
        float safe = Float.isFinite(degrees) ? degrees : fallback;
        if (!Float.isFinite(safe)) {
            safe = 0.0F;
        }
        float wrapped = safe % 360.0F;
        if (wrapped < 0.0F) {
            wrapped += 360.0F;
        }
        return wrapped >= 360.0F ? 0.0F : wrapped;
    }

    static float clampFloat(float value, float fallback, float min, float max) {
        float safe = Float.isNaN(value) || Float.isInfinite(value) ? fallback : value;
        if (Float.isNaN(safe) || Float.isInfinite(safe)) {
            safe = min;
        }
        return Math.max(min, Math.min(max, safe));
    }

    static double clampDouble(double value, double fallback, double min, double max) {
        double safe = Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
        if (Double.isNaN(safe) || Double.isInfinite(safe)) {
            safe = min;
        }
        return Math.max(min, Math.min(max, safe));
    }

    static BeamSettings validateExternalBeamSettings(BeamSettings settings, boolean rail) {
        if (settings == null) {
            return null;
        }
        validateBeamSettings(settings, rail);
        return settings;
    }
}
