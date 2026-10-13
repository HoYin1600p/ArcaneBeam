// Declared in the config package to reach the package-private parse/validate hooks of ArcaneBeamConfig.
package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthVisibility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabyrinthConfigTest {
    private Config original;

    @BeforeEach
    void rememberInstance() {
        original = ArcaneBeamConfig.INSTANCE;
    }

    @AfterEach
    void restoreInstance() {
        ArcaneBeamConfig.INSTANCE = original;
    }

    private static LabyrinthSettings load(String json) {
        return load(json, new ArrayList<>());
    }

    private static LabyrinthSettings load(String json, List<String> failed) {
        ArcaneBeamConfig.INSTANCE = ArcaneBeamConfig.parseConfig(JsonParser.parseString(json).getAsJsonObject(), failed);
        ArcaneBeamConfig.validate();
        return ArcaneBeamConfig.INSTANCE.labyrinth;
    }

    @Test
    void defaultsAreOptInWithTheSpecifiedValues() {
        LabyrinthSettings s = load("{}");
        assertFalse(s.enabled);
        assertEquals(LabyrinthVisibility.ALWAYS, s.visibilityMode());
        assertEquals(0x33FF66, s.routeColor);
        assertEquals(0.85F, s.opacity);
        assertEquals(0.08F, s.width);
        assertEquals(0.04F, s.thickness);
        assertEquals(1.10F, s.elevation);
        assertTrue(s.fullbright);
        assertFalse(s.chevrons);
        assertEquals(48, s.maxDistance);
    }

    @Test
    void outOfRangeNumbersAreClamped() {
        LabyrinthSettings s = load("{\"labyrinth\":{\"opacity\":5.0,\"width\":-1,\"thickness\":9,\"elevation\":100,\"maxDistance\":100000}}");
        assertEquals(LabyrinthSettings.MAX_OPACITY, s.opacity);
        assertEquals(LabyrinthSettings.MIN_WIDTH, s.width);
        assertEquals(LabyrinthSettings.MAX_THICKNESS, s.thickness);
        assertEquals(LabyrinthSettings.MAX_ELEVATION, s.elevation);
        assertEquals(LabyrinthSettings.MAX_DISTANCE, s.maxDistance);
        s = load("{\"labyrinth\":{\"maxDistance\":-4}}");
        assertEquals(LabyrinthSettings.MIN_DISTANCE, s.maxDistance);
    }

    @Test
    void unknownValuesFallBackToTheDefaults() {
        LabyrinthSettings s = load("{\"labyrinth\":{\"visibility\":\"sometimes\",\"fullbright\":\"bright\"}}");
        assertEquals(LabyrinthVisibility.ALWAYS.id, s.visibility);
        assertTrue(s.fullbright);
    }

    @Test
    void knownValuesSurvive() {
        LabyrinthSettings s = load("{\"labyrinth\":{\"enabled\":true,\"visibility\":\"hold_key\",\"fullbright\":false,"
                + "\"routeColor\":255,\"chevrons\":true}}");
        assertTrue(s.enabled);
        assertEquals(LabyrinthVisibility.HOLD_KEY, s.visibilityMode());
        assertFalse(s.fullbright);
        assertEquals(0x0000FF, s.routeColor);
        assertTrue(s.chevrons);
    }

    /** The section as the hall-line builds wrote it: a line colour and the three-way fullbright mode. */
    private static String oldSection(String fullbright) {
        return "{\"labyrinth\":{\"enabled\":true,\"visibility\":\"hold_key\",\"lineColor\":12121343,\"routeColor\":3407718,"
                + "\"opacity\":0.6,\"width\":0.12,\"thickness\":0.05,\"elevation\":1.4,\"fullbright\":\"" + fullbright + "\","
                + "\"chevrons\":true,\"maxDistance\":64}}";
    }

    @Test
    void anOldLabyrinthSectionMigratesWithoutErrors() {
        for (String mode : new String[]{"route_only", "all", "none", "ROUTE_ONLY"}) {
            List<String> failed = new ArrayList<>();
            LabyrinthSettings s = load(oldSection(mode), failed);
            assertTrue(failed.isEmpty(), mode + ": " + failed);
            assertEquals(!mode.equalsIgnoreCase("none"), s.fullbright, mode);
            assertTrue(s.enabled);
            assertEquals(LabyrinthVisibility.HOLD_KEY, s.visibilityMode());
            assertEquals(0x33FF66, s.routeColor);
            assertEquals(0.6F, s.opacity);
            assertEquals(0.12F, s.width);
            assertEquals(0.05F, s.thickness);
            assertEquals(1.4F, s.elevation);
            assertTrue(s.chevrons);
            assertEquals(64, s.maxDistance);
        }
    }

    @Test
    void theOldKeysAreNotWrittenBack() {
        load(oldSection("none"));
        JsonObject written = ConfigParser.GSON.toJsonTree(ArcaneBeamConfig.INSTANCE.labyrinth).getAsJsonObject();
        assertFalse(written.has("lineColor"));
        assertTrue(written.get("fullbright").getAsJsonPrimitive().isBoolean());
        assertFalse(written.get("fullbright").getAsBoolean());
    }

    @Test
    void aBrokenSectionKeepsTheDefaults() {
        LabyrinthSettings s = load("{\"labyrinth\":\"nope\"}");
        assertFalse(s.enabled);
        assertEquals(48, s.maxDistance);
    }
}
