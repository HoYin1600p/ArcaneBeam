package dev.hoyin1600p.arcanebeam.config;

import dev.hoyin1600p.arcanebeam.config.model.ArchonSettings;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.BlastWaveSettings;
import dev.hoyin1600p.arcanebeam.config.model.LightningStrikeSettings;
import dev.hoyin1600p.arcanebeam.config.model.SmiteSettings;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig.INSTANCE;

/** Profile selection and creation for every ability family. */
final class ConfigProfiles {
    private ConfigProfiles() {
    }

    static void activateSelectedProfiles() {
        INSTANCE.arcane = INSTANCE.arcaneProfiles.get(INSTANCE.selectedArcaneProfile);
        INSTANCE.rail = INSTANCE.railProfiles.get(INSTANCE.selectedRailProfile);
        INSTANCE.lightningStrike = INSTANCE.lightningStrikeProfiles.get(INSTANCE.selectedLightningStrikeProfile);
        INSTANCE.vaultAltar = INSTANCE.vaultAltarProfiles.get(INSTANCE.selectedVaultAltarProfile);
        INSTANCE.stormArrow = INSTANCE.stormArrowProfiles.get(INSTANCE.selectedStormArrowProfile);
        INSTANCE.smite = INSTANCE.smiteProfiles.get(INSTANCE.selectedSmiteProfile);
        INSTANCE.archon = INSTANCE.archonProfiles.get(INSTANCE.selectedArchonProfile);
        INSTANCE.blastWave = INSTANCE.blastWaveProfiles.get(INSTANCE.selectedBlastWaveProfile);
        INSTANCE.shaderCompatibility = INSTANCE.arcane.shaderCompatibility;
    }

    static void syncActiveProfiles() {
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
        if (INSTANCE.stormArrowProfiles != null && INSTANCE.selectedStormArrowProfile != null && INSTANCE.stormArrow != null) {
            INSTANCE.stormArrowProfiles.put(INSTANCE.selectedStormArrowProfile, INSTANCE.stormArrow);
        }
        if (INSTANCE.smiteProfiles != null && INSTANCE.selectedSmiteProfile != null && INSTANCE.smite != null) {
            INSTANCE.smiteProfiles.put(INSTANCE.selectedSmiteProfile, INSTANCE.smite);
        }
        if (INSTANCE.archonProfiles != null && INSTANCE.selectedArchonProfile != null && INSTANCE.archon != null) {
            INSTANCE.archonProfiles.put(INSTANCE.selectedArchonProfile, INSTANCE.archon);
        }
        if (INSTANCE.blastWaveProfiles != null && INSTANCE.selectedBlastWaveProfile != null && INSTANCE.blastWave != null) {
            INSTANCE.blastWaveProfiles.put(INSTANCE.selectedBlastWaveProfile, INSTANCE.blastWave);
        }
    }

    static List<String> profileNames(boolean rail) {
        return new ArrayList<>(profileMap(rail).keySet());
    }

    static String selectedProfileName(boolean rail) {
        return rail ? INSTANCE.selectedRailProfile : INSTANCE.selectedArcaneProfile;
    }

    static List<String> lightningProfileNames() {
        return new ArrayList<>(INSTANCE.lightningStrikeProfiles.keySet());
    }

    static String selectedLightningProfileName() {
        return INSTANCE.selectedLightningStrikeProfile;
    }

    static List<String> vaultAltarProfileNames() {
        return new ArrayList<>(INSTANCE.vaultAltarProfiles.keySet());
    }

    static String selectedVaultAltarProfileName() {
        return INSTANCE.selectedVaultAltarProfile;
    }

    static List<String> stormArrowProfileNames() {
        return new ArrayList<>(INSTANCE.stormArrowProfiles.keySet());
    }

    static String selectedStormArrowProfileName() {
        return INSTANCE.selectedStormArrowProfile;
    }

    static List<String> smiteProfileNames() {
        return new ArrayList<>(INSTANCE.smiteProfiles.keySet());
    }

    static String selectedSmiteProfileName() {
        return INSTANCE.selectedSmiteProfile;
    }

    static List<String> archonProfileNames() {
        return new ArrayList<>(INSTANCE.archonProfiles.keySet());
    }

    static String selectedArchonProfileName() {
        return INSTANCE.selectedArchonProfile;
    }

    static List<String> blastWaveProfileNames() {
        return new ArrayList<>(INSTANCE.blastWaveProfiles.keySet());
    }

    static String selectedBlastWaveProfileName() {
        return INSTANCE.selectedBlastWaveProfile;
    }

    static void selectProfile(boolean rail, String profileName) {
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
        ArcaneBeamConfig.save();
    }

    static String addProfile(boolean rail, String requestedName) {
        LinkedHashMap<String, BeamSettings> profiles = profileMap(rail);
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(profiles, baseName);
        BeamSettings settings = ConfigCopies.copyOf(rail ? INSTANCE.rail : INSTANCE.arcane);
        ConfigValidation.validateBeamSettings(settings, rail);
        profiles.put(profileName, settings);
        if (rail) {
            INSTANCE.selectedRailProfile = profileName;
            INSTANCE.rail = settings;
        } else {
            INSTANCE.selectedArcaneProfile = profileName;
            INSTANCE.arcane = settings;
            INSTANCE.shaderCompatibility = settings.shaderCompatibility;
        }
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectLightningProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.lightningStrikeProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedLightningStrikeProfile = normalized;
        INSTANCE.lightningStrike = INSTANCE.lightningStrikeProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addLightningProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.lightningStrikeProfiles, baseName);
        LightningStrikeSettings settings = ConfigCopies.copyOf(INSTANCE.lightningStrike);
        ConfigValidation.validateLightningStrikeSettings(settings);
        INSTANCE.lightningStrikeProfiles.put(profileName, settings);
        INSTANCE.selectedLightningStrikeProfile = profileName;
        INSTANCE.lightningStrike = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectVaultAltarProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.vaultAltarProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedVaultAltarProfile = normalized;
        INSTANCE.vaultAltar = INSTANCE.vaultAltarProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addVaultAltarProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.vaultAltarProfiles, baseName);
        VaultAltarSettings settings = ConfigCopies.copyOf(INSTANCE.vaultAltar);
        ConfigValidation.validateVaultAltarSettings(settings);
        INSTANCE.vaultAltarProfiles.put(profileName, settings);
        INSTANCE.selectedVaultAltarProfile = profileName;
        INSTANCE.vaultAltar = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectStormArrowProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.stormArrowProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedStormArrowProfile = normalized;
        INSTANCE.stormArrow = INSTANCE.stormArrowProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addStormArrowProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.stormArrowProfiles, baseName);
        StormArrowSettings settings = ConfigCopies.copyOf(INSTANCE.stormArrow);
        ConfigValidation.validateStormArrowSettings(settings);
        INSTANCE.stormArrowProfiles.put(profileName, settings);
        INSTANCE.selectedStormArrowProfile = profileName;
        INSTANCE.stormArrow = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectSmiteProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.smiteProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedSmiteProfile = normalized;
        INSTANCE.smite = INSTANCE.smiteProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addSmiteProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.smiteProfiles, baseName);
        SmiteSettings settings = ConfigCopies.copyOf(INSTANCE.smite);
        ConfigValidation.validateStormArrowSettings(settings);
        INSTANCE.smiteProfiles.put(profileName, settings);
        INSTANCE.selectedSmiteProfile = profileName;
        INSTANCE.smite = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectArchonProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.archonProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedArchonProfile = normalized;
        INSTANCE.archon = INSTANCE.archonProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addArchonProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.archonProfiles, baseName);
        ArchonSettings settings = ConfigCopies.copyOf(INSTANCE.archon);
        ConfigValidation.validateArchonSettings(settings);
        INSTANCE.archonProfiles.put(profileName, settings);
        INSTANCE.selectedArchonProfile = profileName;
        INSTANCE.archon = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static void selectBlastWaveProfile(String profileName) {
        String normalized = normalizeProfileName(profileName);
        if (normalized.isEmpty() || !INSTANCE.blastWaveProfiles.containsKey(normalized)) {
            return;
        }
        syncActiveProfiles();
        INSTANCE.selectedBlastWaveProfile = normalized;
        INSTANCE.blastWave = INSTANCE.blastWaveProfiles.get(normalized);
        ArcaneBeamConfig.save();
    }

    static String addBlastWaveProfile(String requestedName) {
        String baseName = normalizeProfileName(requestedName);
        if (baseName.isEmpty()) {
            baseName = "Profile";
        }
        syncActiveProfiles();
        String profileName = uniqueProfileName(INSTANCE.blastWaveProfiles, baseName);
        BlastWaveSettings settings = ConfigCopies.copyOf(INSTANCE.blastWave);
        ConfigValidation.validateBlastWaveSettings(settings);
        INSTANCE.blastWaveProfiles.put(profileName, settings);
        INSTANCE.selectedBlastWaveProfile = profileName;
        INSTANCE.blastWave = settings;
        ArcaneBeamConfig.save();
        return profileName;
    }

    static LinkedHashMap<String, BeamSettings> profileMap(boolean rail) {
        return rail ? INSTANCE.railProfiles : INSTANCE.arcaneProfiles;
    }

    static String normalizeProfileName(String profileName) {
        if (profileName == null) {
            return "";
        }
        return profileName.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    static String uniqueProfileName(Map<String, ?> profiles, String baseName) {
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
}
