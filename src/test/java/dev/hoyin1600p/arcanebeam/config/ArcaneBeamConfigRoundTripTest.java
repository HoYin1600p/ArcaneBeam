// Declared in the client package to reach the package-private parse/validate hooks of ArcaneBeamConfig.
package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneBeamConfigRoundTripTest {
    private static final Gson GSON = new GsonBuilder().create();
    private Config original;

    @BeforeEach
    void rememberInstance() {
        original = ArcaneBeamConfig.INSTANCE;
    }

    @AfterEach
    void restoreInstance() {
        ArcaneBeamConfig.INSTANCE = original;
    }

    private static Config parse(String json, List<String> failed) {
        return ArcaneBeamConfig.parseConfig(JsonParser.parseString(json).getAsJsonObject(), failed);
    }

    private static Config loadValidated(String json) {
        ArcaneBeamConfig.INSTANCE = parse(json, new ArrayList<>());
        ArcaneBeamConfig.validate();
        return ArcaneBeamConfig.INSTANCE;
    }

    @Test
    void zeroValuesSurviveSaveAndLoad() {
        Config config = loadValidated("{}");
        config.arcane.opacity = 0.0F;
        config.arcane.glowOpacity = 0.0F;
        config.arcane.color = 0;
        config.arcane.colors = new int[]{0, 0, 0, 0};
        config.arcane.startOffsetX = 0.0D;
        config.arcane.startOffsetY = 0.0D;
        config.arcane.startOffsetZ = 0.0D;
        config.lightningStrike.alpha = 0.0F;
        config.lightningStrike.ringColor = 0;
        config.vaultAltar.cornerOpacity = 0.0F;
        config.stormArrow.circleAlpha = 0.0F;
        config.stormArrow.circleColor = 0;
        config.blastWave.blasterAlpha = 0.0F;

        // Same pipeline as save() then load().
        JsonObject saved = GSON.toJsonTree(config).getAsJsonObject();
        ArcaneBeamConfig.INSTANCE = ArcaneBeamConfig.parseConfig(saved, new ArrayList<>());
        ArcaneBeamConfig.validate();
        Config loaded = ArcaneBeamConfig.INSTANCE;

        assertEquals(0.0F, loaded.arcane.opacity);
        assertEquals(0.0F, loaded.arcane.alpha);
        assertEquals(0.0F, loaded.arcane.glowOpacity);
        assertEquals(0, loaded.arcane.color);
        assertArrayEquals(new int[]{0, 0, 0, 0}, loaded.arcane.colors);
        assertEquals(0.0D, loaded.arcane.startOffsetX);
        assertEquals(0.0D, loaded.arcane.startOffsetY);
        assertEquals(0.0D, loaded.arcane.startOffsetZ);
        assertEquals(0.0F, loaded.lightningStrike.alpha);
        assertEquals(0, loaded.lightningStrike.ringColor);
        assertEquals(0.0F, loaded.vaultAltar.cornerOpacity);
        assertEquals(0.0F, loaded.stormArrow.circleAlpha);
        assertEquals(0, loaded.stormArrow.circleColor);
        assertEquals(0.0F, loaded.blastWave.blasterAlpha);
    }

    @Test
    void addProfileValidationKeepsZeroValues() {
        Config config = loadValidated("{}");
        config.arcane.opacity = 0.0F;
        config.arcane.colors = new int[]{0, 0, 0, 0};
        BeamSettings copy = new BeamSettings();
        copy.opacity = 0.0F;
        copy.colors = new int[]{0, 0, 0, 0};
        copy.startOffsetX = 0.0D;
        copy.startOffsetY = 0.0D;
        copy.startOffsetZ = 0.0D;
        ArcaneBeamConfig.validateExternalBeamSettings(copy, false);
        assertEquals(0.0F, copy.opacity);
        assertArrayEquals(new int[]{0, 0, 0, 0}, copy.colors);
        assertEquals(0.0D, copy.startOffsetX);
    }

    @Test
    void absentFieldsGetDefaultsPerBeamKind() {
        Config config = parse("{\"arcane\":{\"color\":1},\"rail\":{},\"stormArrow\":{}}", new ArrayList<>());
        assertEquals(16.0D, config.arcane.maxRange);
        assertEquals(3, config.arcane.lifetimeTicks);
        assertEquals(0.65F, config.arcane.opacity);
        assertEquals(128.0D, config.rail.maxRange);
        assertEquals(8, config.rail.lifetimeTicks);
        assertEquals(new StormArrowSettings().circleAlpha, config.stormArrow.circleAlpha);
    }

    @Test
    void invalidValuesAreClampedAndNonFiniteValuesGetDefaults() {
        Config config = loadValidated("{}");
        config.arcane.maxRange = Double.POSITIVE_INFINITY;
        config.arcane.opacity = Float.NaN;
        config.arcane.intensity = 50.0F;
        config.arcane.lifetimeTicks = 0;
        config.arcane.startOffsetX = 1.0E9D;
        ArcaneBeamConfig.validateExternalBeamSettings(config.arcane, false);
        assertEquals(16.0D, config.arcane.maxRange);
        assertEquals(0.65F, config.arcane.opacity);
        assertEquals(1.0F, config.arcane.intensity);
        assertEquals(1, config.arcane.lifetimeTicks);
        assertEquals(64.0D, config.arcane.startOffsetX);
    }

    @Test
    void badSectionFallsBackWithoutWipingOthers() {
        List<String> failed = new ArrayList<>();
        Config config = parse(
                "{\"arcane\":\"not an object\",\"dungeonGlobeMap\":{\"opacityPercent\":42},"
                        + "\"updates\":{\"checkForUpdates\":false}}", failed);
        assertTrue(failed.contains("arcane"));
        assertEquals(0.65F, config.arcane.opacity);
        assertEquals(42, config.dungeonGlobeMap.opacityPercent);
        assertEquals(false, config.updates.checkForUpdates);
    }

    @Test
    void oneBadProfileDoesNotDropTheOthers() {
        List<String> failed = new ArrayList<>();
        Config config = parse(
                "{\"arcaneProfiles\":{\"Good\":{\"opacity\":0.5},\"Bad\":\"not an object\"}}", failed);
        assertEquals(1, failed.size());
        assertEquals(1, config.arcaneProfiles.size());
        assertEquals(0.5F, config.arcaneProfiles.get("Good").opacity);
    }

    @Test
    void legacyMigrationsRunOnceForUnversionedConfig() {
        String legacy = "{\"rail\":{\"color\":3528703},\"stormArrow\":{\"lifetimeTicks\":10},"
                + "\"vaultAltar\":{\"cornerVerticalTicks\":10,\"cornerConvergeTicks\":10}%s}";
        Config migrated = parse(String.format(legacy, ""), new ArrayList<>());
        assertEquals(0x00FF44, migrated.rail.color);
        assertEquals(5, migrated.stormArrow.lifetimeTicks);
        assertEquals(20, migrated.vaultAltar.cornerVerticalTicks);
        assertEquals(30, migrated.vaultAltar.cornerConvergeTicks);
        assertEquals(ArcaneBeamConfig.CONFIG_VERSION, migrated.configVersion);

        // Stamped config is saved with the version and reloaded: values now stick.
        migrated.stormArrow.lifetimeTicks = 10;
        migrated.vaultAltar.cornerConvergeTicks = 10;
        JsonObject saved = GSON.toJsonTree(migrated).getAsJsonObject();
        Config reloaded = ArcaneBeamConfig.parseConfig(saved, new ArrayList<>());
        assertEquals(10, reloaded.stormArrow.lifetimeTicks);
        assertEquals(10, reloaded.vaultAltar.cornerConvergeTicks);
    }

    @Test
    void currentVersionConfigIsNotMigrated() {
        String current = "{\"configVersion\":" + ArcaneBeamConfig.CONFIG_VERSION
                + ",\"rail\":{\"color\":3528703},\"stormArrow\":{\"lifetimeTicks\":10},"
                + "\"vaultAltar\":{\"cornerVerticalTicks\":10,\"cornerConvergeTicks\":10}}";
        Config config = loadValidated(current);
        assertEquals(0x35D7FF, config.rail.color);
        assertEquals(10, config.stormArrow.lifetimeTicks);
        assertEquals(10, config.vaultAltar.cornerVerticalTicks);
        assertEquals(10, config.vaultAltar.cornerConvergeTicks);
    }

    @Test
    void newDefaultConfigStartsAtCurrentVersion() {
        assertEquals(ArcaneBeamConfig.CONFIG_VERSION, new Config().configVersion);
        assertEquals(ArcaneBeamConfig.CONFIG_VERSION, ArcaneBeamConfig.INSTANCE.configVersion);
    }

    @Test
    void palettesArePaddedOrTruncatedKeepingUserColors() {
        assertArrayEquals(new int[]{1, 2, 2, 2}, ArcaneBeamConfig.normalizePalette(new int[]{1, 2}, 4, new int[4]));
        assertArrayEquals(new int[]{1, 2, 3, 4}, ArcaneBeamConfig.normalizePalette(new int[]{1, 2, 3, 4, 5}, 4, new int[4]));
        assertArrayEquals(new int[]{7, 7, 7, 7}, ArcaneBeamConfig.normalizePalette(null, 4, new int[]{7, 7, 7, 7}));

        BeamSettings settings = new BeamSettings();
        settings.colors = new int[]{0x123456, 0x654321};
        ArcaneBeamConfig.validateExternalBeamSettings(settings, false);
        assertArrayEquals(new int[]{0x123456, 0x654321, 0x654321, 0x654321}, settings.colors);
    }
}
