package dev.hoyin1600p.arcanebeam.config.model;

import java.util.LinkedHashMap;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.DEFAULT_PROFILE;

public final class Config {
    /** Bumped whenever a value migration is added; migrations run only for older files. */
    public static final int CURRENT_VERSION = 1;

    public int configVersion = CURRENT_VERSION;
    public UpdateSettings updates = new UpdateSettings();
    public DungeonGlobeMapSettings dungeonGlobeMap = new DungeonGlobeMapSettings();
    public UiSettings ui = new UiSettings();
    public LabyrinthSettings labyrinth = new LabyrinthSettings();
    public String shaderCompatibility;
    public String selectedArcaneProfile = DEFAULT_PROFILE;
    public String selectedRailProfile = DEFAULT_PROFILE;
    public String selectedLightningStrikeProfile = DEFAULT_PROFILE;
    public String selectedVaultAltarProfile = DEFAULT_PROFILE;
    public String selectedStormArrowProfile = DEFAULT_PROFILE;
    public String selectedSmiteProfile = DEFAULT_PROFILE;
    public String selectedArchonProfile = DEFAULT_PROFILE;
    public String selectedBlastWaveProfile = DEFAULT_PROFILE;
    public BeamSettings arcane = BeamSettings.defaultArcane();
    public BeamSettings rail = BeamSettings.defaultRail();
    public LightningStrikeSettings lightningStrike = new LightningStrikeSettings();
    public VaultAltarSettings vaultAltar = new VaultAltarSettings();
    public StormArrowSettings stormArrow = new StormArrowSettings();
    public SmiteSettings smite = new SmiteSettings();
    public ArchonSettings archon = new ArchonSettings();
    public BlastWaveSettings blastWave = new BlastWaveSettings();
    public LinkedHashMap<String, BeamSettings> arcaneProfiles;
    public LinkedHashMap<String, BeamSettings> railProfiles;
    public LinkedHashMap<String, LightningStrikeSettings> lightningStrikeProfiles;
    public LinkedHashMap<String, VaultAltarSettings> vaultAltarProfiles;
    public LinkedHashMap<String, StormArrowSettings> stormArrowProfiles;
    public LinkedHashMap<String, SmiteSettings> smiteProfiles;
    public LinkedHashMap<String, ArchonSettings> archonProfiles;
    public LinkedHashMap<String, BlastWaveSettings> blastWaveProfiles;
}
