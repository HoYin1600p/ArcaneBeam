package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonParser;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** K3 (per-field tolerant parsing), K4 (mirror sections) and the K5 load-time clamps. */
class ConfigLenientParseTest {
    private Config original;

    @BeforeEach
    void remember() {
        original = ArcaneBeamConfig.INSTANCE;
    }

    @AfterEach
    void restore() {
        ArcaneBeamConfig.INSTANCE = original;
    }

    private static Config parse(String json, List<String> failed) {
        return ConfigParser.parseConfig(JsonParser.parseString(json).getAsJsonObject(), failed);
    }

    private static BeamSettings arcaneProfile(String body) {
        List<String> failed = new ArrayList<>();
        Config config = parse("{\"arcaneProfiles\":{\"P\":" + body + "}}", failed);
        assertTrue(failed.isEmpty(), "a wrong-typed field must not fail the profile: " + failed);
        return config.arcaneProfiles.get("P");
    }

    @Test
    void coloursAcceptEveryNotation() {
        BeamSettings settings = arcaneProfile("{\"color\":\"#FF00FF\",\"glowColor\":\"0x00FF44\","
                + "\"colors\":[\"#80FF00FF\",4294967295,16711680,\"255\"]}");
        assertEquals(0xFF00FF, settings.color);
        assertEquals(0x00FF44, settings.glowColor);
        assertArrayEquals(new int[]{0x80FF00FF, -1, 0xFF0000, 255}, settings.colors);
    }

    @Test
    void unsignedColourWrapsToTheSameBits() {
        assertEquals(-1, arcaneProfile("{\"color\":4294967295}").color);
        assertEquals(Integer.MIN_VALUE, arcaneProfile("{\"color\":2147483648}").color);
    }

    @Test
    void booleansAcceptStringsAndZeroOne() {
        assertFalse(arcaneProfile("{\"enabled\":\"false\"}").enabled);
        assertTrue(arcaneProfile("{\"crosshairAim\":\"TRUE\"}").crosshairAim);
        assertTrue(arcaneProfile("{\"crosshairAim\":1}").crosshairAim);
        assertFalse(arcaneProfile("{\"enabled\":0}").enabled);
    }

    @Test
    void numbersAcceptNumericStrings() {
        BeamSettings settings = arcaneProfile("{\"alpha\":\"0.5\",\"maxRange\":\"32\",\"lifetimeTicks\":\"7\"}");
        assertEquals(0.5F, settings.alpha);
        assertEquals(32.0D, settings.maxRange);
        assertEquals(7, settings.lifetimeTicks);
    }

    @Test
    void anUnreadableFieldFallsBackToItsDefaultWithoutDroppingTheProfile() {
        BeamSettings settings = arcaneProfile("{\"alpha\":\"abc\",\"colors\":[\"#GG0000\"],\"enabled\":\"maybe\","
                + "\"lifetimeTicks\":9,\"opacity\":0.25}");
        BeamSettings defaults = BeamSettings.defaultArcane();
        assertEquals(defaults.alpha, settings.alpha);
        assertArrayEquals(defaults.colors, settings.colors);
        assertTrue(settings.enabled);
        assertEquals(9, settings.lifetimeTicks, "neighbouring valid fields keep their values");
        assertEquals(0.25F, settings.opacity);
    }

    @Test
    void railProfilesFallBackToRailDefaults() {
        List<String> failed = new ArrayList<>();
        Config config = parse("{\"railProfiles\":{\"R\":{\"color\":\"nope\",\"opacity\":0.4}}}", failed);
        assertTrue(failed.isEmpty());
        assertEquals(BeamSettings.defaultRail().color, config.railProfiles.get("R").color);
        assertEquals(0.4F, config.railProfiles.get("R").opacity);
    }

    @Test
    void topLevelSectionsAreTolerantToo() {
        List<String> failed = new ArrayList<>();
        Config config = parse("{\"stormArrow\":{\"circleColor\":\"#FF0000\",\"enabled\":\"false\"}}", failed);
        assertTrue(failed.isEmpty());
        assertEquals(0xFF0000, config.stormArrow.circleColor);
        assertFalse(config.stormArrow.enabled);
    }

    @Test
    void resourcePackBeamSectionsAreTolerantToo() {
        ArcaneBeamConfig.INSTANCE = ConfigDefaults.defaultConfig();
        BeamSettings settings = ArcaneBeamConfig.parseExternalBeamSettings(
                JsonParser.parseString("{\"color\":\"#00FF00\",\"particleScale\":\"2\"}"), false);
        assertEquals(0x00FF00, settings.color);
        assertEquals(2.0F, settings.particleScale);
    }

    @Test
    void brokenMirrorDoesNotMarkTheFileDamagedWhenItsProfileLoaded() {
        List<String> failed = new ArrayList<>();
        Config config = parse("{\"archon\":\"not an object\",\"archonProfiles\":{\"Default\":{\"circleAlpha\":0.42}}}", failed);
        assertTrue(failed.isEmpty(), "only the mirror failed: " + failed);
        assertEquals(0.42F, config.archonProfiles.get("Default").circleAlpha);
    }

    @Test
    void brokenMirrorStillCountsWhenTheProfileIsGone() {
        List<String> failed = new ArrayList<>();
        parse("{\"archon\":\"not an object\"}", failed);
        assertEquals(List.of("archon"), failed);
        failed.clear();
        parse("{\"archon\":\"not an object\",\"archonProfiles\":{\"Default\":\"also broken\"}}", failed);
        assertTrue(failed.contains("archon"));
        assertTrue(failed.contains("archonProfiles.Default"));
    }

    @Test
    void particleScaleAndRotationAreClampedOnLoad() {
        BeamSettings settings = BeamSettings.defaultArcane();
        settings.particleScale = 100.0F;
        settings.particleRotationDegrees = -90.0F;
        ConfigValidation.validateBeamSettings(settings, false);
        assertEquals(8.0F, settings.particleScale);
        assertEquals(270.0F, settings.particleRotationDegrees);

        settings.particleScale = 0.0F;
        settings.particleRotationDegrees = 720.0F;
        ConfigValidation.validateBeamSettings(settings, false);
        assertEquals(0.1F, settings.particleScale);
        assertEquals(0.0F, settings.particleRotationDegrees);

        settings.particleScale = Float.NaN;
        settings.particleRotationDegrees = Float.POSITIVE_INFINITY;
        ConfigValidation.validateBeamSettings(settings, false);
        assertEquals(1.0F, settings.particleScale);
        assertEquals(0.0F, settings.particleRotationDegrees);
    }

    @Test
    void normalizeDegreesKeepsValuesInRange() {
        assertEquals(359.0F, ConfigValidation.normalizeDegrees(-1.0F, 0.0F));
        assertEquals(90.0F, ConfigValidation.normalizeDegrees(450.0F, 0.0F));
        assertEquals(0.0F, ConfigValidation.normalizeDegrees(360.0F, 0.0F));
    }
}
