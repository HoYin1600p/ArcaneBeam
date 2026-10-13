package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.BlastWaveSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.UpdateSettings;
import dev.hoyin1600p.arcanebeam.update.UpdateNoticeFilter;
import dev.hoyin1600p.arcanebeam.update.UpdateNoticeService;

import java.util.List;

/**
 * Entry point for the Arcane Beam config. Holds the live {@link #INSTANCE} and forwards to the classes that
 * load, migrate, validate and save it.
 */
public final class ArcaneBeamConfig {
    /** Bumped whenever a value migration is added; migrations run only for older files. */
    static final int CONFIG_VERSION = ConfigMigrations.CONFIG_VERSION;

    public static Config INSTANCE = ConfigDefaults.defaultConfig();

    private ArcaneBeamConfig() {
    }

    /** The bundled default settings for Arcane or Rail, for the settings screen's Reset buttons. */
    public static BeamSettings bundledBeamDefaults(boolean rail) {
        Config defaults = ConfigDefaults.defaultConfig();
        BeamSettings settings = rail ? defaults.rail : defaults.arcane;
        return settings != null ? settings : ConfigDefaults.defaultSettings(rail);
    }

    /** The bundled default Blast Wave settings, for the settings screen's Reset buttons. */
    public static BlastWaveSettings bundledBlastWaveDefaults() {
        Config defaults = ConfigDefaults.defaultConfig();
        return defaults.blastWave != null ? defaults.blastWave : new BlastWaveSettings();
    }

    public static void load() {
        ConfigIo.load();
    }

    /** Marks the config dirty; the write happens from {@link #tickPendingSave()} or {@link #flushPendingSave()}. */
    public static void requestSave() {
        ConfigIo.requestSave();
    }

    /** Writes a save requested through {@link #requestSave()} once it has been quiet for about 500 ms. */
    public static void tickPendingSave() {
        ConfigIo.tickPendingSave();
    }

    /** Saves a pending change now and waits briefly until it is on disk. */
    public static void flushPendingSave() {
        ConfigIo.flushPendingSave();
    }

    /** Makes sure a pending or still-retrying save is written when the game exits. */
    public static void installShutdownFlush() {
        ConfigIo.installShutdownFlush();
    }

    public static synchronized void save() {
        ConfigIo.write();
    }

    public static boolean updateChecksEnabled() {
        return INSTANCE.updates == null || INSTANCE.updates.checkForUpdates;
    }

    public static void setUpdateChecksEnabled(boolean enabled) {
        if (INSTANCE.updates == null) {
            INSTANCE.updates = new UpdateSettings();
        }
        INSTANCE.updates.checkForUpdates = enabled;
        save();
        UpdateNoticeService.setEnabled(enabled);
    }

    public static UpdateNoticeFilter updateNoticeFilter() {
        return UpdateNoticeFilter.fromConfigValue(
                INSTANCE.updates == null ? null : INSTANCE.updates.updateTypes,
                UpdateNoticeFilter.CRITICAL
        );
    }

    public static void setUpdateNoticeFilter(UpdateNoticeFilter filter) {
        if (INSTANCE.updates == null) {
            INSTANCE.updates = new UpdateSettings();
        }
        INSTANCE.updates.updateTypes = filter.name();
        save();
        UpdateNoticeService.setFilter(filter);
    }

    /**
     * Reads each top-level section independently. A section that cannot be read keeps its default and is
     * named in {@code failedSections}. Legacy value migrations run only when the file predates
     * {@link #CONFIG_VERSION}.
     */
    static Config parseConfig(JsonObject root, List<String> failedSections) {
        return ConfigParser.parseConfig(root, failedSections);
    }

    static void validate() {
        ConfigValidation.validate();
    }

    static int[] normalizePalette(int[] palette, int length, int[] fallback) {
        return ConfigValidation.normalizePalette(palette, length, fallback);
    }

    static BeamSettings validateExternalBeamSettings(BeamSettings settings, boolean rail) {
        return ConfigValidation.validateExternalBeamSettings(settings, rail);
    }

    /**
     * Reads one resource-pack beam section. Absent fields take the defaults of the beam kind, and the result
     * gets the same bounds as a user config. Returns null when the section is absent or not an object.
     */
    public static BeamSettings parseExternalBeamSettings(JsonElement element, boolean rail) {
        return ConfigParser.parseExternalBeamSettings(element, rail);
    }

    public static List<String> profileNames(boolean rail) {
        return ConfigProfiles.profileNames(rail);
    }

    public static String selectedProfileName(boolean rail) {
        return ConfigProfiles.selectedProfileName(rail);
    }

    public static List<String> lightningProfileNames() {
        return ConfigProfiles.lightningProfileNames();
    }

    public static String selectedLightningProfileName() {
        return ConfigProfiles.selectedLightningProfileName();
    }

    public static List<String> vaultAltarProfileNames() {
        return ConfigProfiles.vaultAltarProfileNames();
    }

    public static String selectedVaultAltarProfileName() {
        return ConfigProfiles.selectedVaultAltarProfileName();
    }

    public static List<String> stormArrowProfileNames() {
        return ConfigProfiles.stormArrowProfileNames();
    }

    public static String selectedStormArrowProfileName() {
        return ConfigProfiles.selectedStormArrowProfileName();
    }

    public static List<String> smiteProfileNames() {
        return ConfigProfiles.smiteProfileNames();
    }

    public static String selectedSmiteProfileName() {
        return ConfigProfiles.selectedSmiteProfileName();
    }

    public static List<String> archonProfileNames() {
        return ConfigProfiles.archonProfileNames();
    }

    public static String selectedArchonProfileName() {
        return ConfigProfiles.selectedArchonProfileName();
    }

    public static List<String> blastWaveProfileNames() {
        return ConfigProfiles.blastWaveProfileNames();
    }

    public static String selectedBlastWaveProfileName() {
        return ConfigProfiles.selectedBlastWaveProfileName();
    }

    public static void selectProfile(boolean rail, String profileName) {
        ConfigProfiles.selectProfile(rail, profileName);
    }

    public static String addProfile(boolean rail, String requestedName) {
        return ConfigProfiles.addProfile(rail, requestedName);
    }

    public static void selectLightningProfile(String profileName) {
        ConfigProfiles.selectLightningProfile(profileName);
    }

    public static String addLightningProfile(String requestedName) {
        return ConfigProfiles.addLightningProfile(requestedName);
    }

    public static void selectVaultAltarProfile(String profileName) {
        ConfigProfiles.selectVaultAltarProfile(profileName);
    }

    public static String addVaultAltarProfile(String requestedName) {
        return ConfigProfiles.addVaultAltarProfile(requestedName);
    }

    public static void selectStormArrowProfile(String profileName) {
        ConfigProfiles.selectStormArrowProfile(profileName);
    }

    public static String addStormArrowProfile(String requestedName) {
        return ConfigProfiles.addStormArrowProfile(requestedName);
    }

    public static void selectSmiteProfile(String profileName) {
        ConfigProfiles.selectSmiteProfile(profileName);
    }

    public static String addSmiteProfile(String requestedName) {
        return ConfigProfiles.addSmiteProfile(requestedName);
    }

    public static void selectArchonProfile(String profileName) {
        ConfigProfiles.selectArchonProfile(profileName);
    }

    public static String addArchonProfile(String requestedName) {
        return ConfigProfiles.addArchonProfile(requestedName);
    }

    public static void selectBlastWaveProfile(String profileName) {
        ConfigProfiles.selectBlastWaveProfile(profileName);
    }

    public static String addBlastWaveProfile(String requestedName) {
        return ConfigProfiles.addBlastWaveProfile(requestedName);
    }
}
