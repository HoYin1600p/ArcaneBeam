package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import dev.hoyin1600p.arcanebeam.config.model.UiSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads a real player config written by 0.2.1 (no labyrinth or ui section, no config version) through the full
 * file loader, and a set of damaged variants of it. None may throw: the loader runs during mod construction.
 */
class OwnerConfigLoadTest {
    @TempDir
    Path directory;

    private Config original;
    private Path file;

    @BeforeEach
    void setUp() {
        original = ArcaneBeamConfig.INSTANCE;
        ConfigIo.resetForTests();
        file = directory.resolve("ArcaneBeam.json");
        ConfigIo.pathOverride = file;
        ConfigIo.sleeper = millis -> {
        };
    }

    @AfterEach
    void tearDown() {
        ConfigIo.awaitIdle(5_000L);
        ConfigIo.resetForTests();
        ArcaneBeamConfig.INSTANCE = original;
    }

    /** 0.2.1 release file: no config version, no ui or labyrinth section. */
    private static final String RELEASE_FILE = "owner-2026-10-06.json";
    /** Written by a config-nav dev build: config version 1 with a ui section but no labyrinth section. */
    private static final String DEV_FILE = "owner-2026-10-11.json";

    private static String ownerConfig() throws IOException {
        return ownerConfig(RELEASE_FILE);
    }

    private static String ownerConfig(String name) throws IOException {
        try (InputStream stream = OwnerConfigLoadTest.class.getResourceAsStream("/config-real/" + name)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void loadText(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
        assertDoesNotThrow(ConfigIo::load);
        ConfigIo.awaitIdle(5_000L);
        assertSane(ArcaneBeamConfig.INSTANCE);
    }

    private static void assertSane(Config config) {
        assertNotNull(config);
        assertNotNull(config.labyrinth);
        assertNotNull(config.ui);
        assertTrue(UiSettings.LEFT.equals(config.ui.panelSide) || UiSettings.RIGHT.equals(config.ui.panelSide));
        LabyrinthSettings l = config.labyrinth;
        assertTrue(l.opacity >= LabyrinthSettings.MIN_OPACITY && l.opacity <= LabyrinthSettings.MAX_OPACITY, "opacity " + l.opacity);
        assertTrue(l.width >= LabyrinthSettings.MIN_WIDTH && l.width <= LabyrinthSettings.MAX_WIDTH, "width " + l.width);
        assertTrue(l.thickness >= LabyrinthSettings.MIN_THICKNESS && l.thickness <= LabyrinthSettings.MAX_THICKNESS);
        assertTrue(l.elevation >= LabyrinthSettings.MIN_ELEVATION && l.elevation <= LabyrinthSettings.MAX_ELEVATION);
        assertTrue(l.maxDistance >= LabyrinthSettings.MIN_DISTANCE && l.maxDistance <= LabyrinthSettings.MAX_DISTANCE);
        assertNotNull(l.visibilityMode());
        assertNotNull(config.arcane);
        assertNotNull(config.arcaneProfiles);
        assertTrue(config.arcaneProfiles.containsKey(config.selectedArcaneProfile));
    }

    private List<Path> corruptBackups() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().contains(".corrupt-")).toList();
        }
    }

    /** Flattens a JSON tree to path -> primitive text. */
    private static void flatten(String prefix, JsonElement element, Map<String, String> out) {
        if (element == null || element.isJsonNull()) {
            out.put(prefix, "null");
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                flatten(prefix + "/" + entry.getKey(), entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                flatten(prefix + "[" + i + "]", array.get(i), out);
            }
        } else if (element.getAsJsonPrimitive().isNumber()) {
            out.put(prefix, Double.toString(element.getAsDouble()));
        } else {
            out.put(prefix, element.getAsString());
        }
    }

    private static Map<String, String> flatten(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        flatten("", JsonParser.parseString(json), out);
        return out;
    }

    /** Differences between two flattened files, ignoring float rounding below 1e-4. */
    private static List<String> lostValues(Map<String, String> before, Map<String, String> after) {
        List<String> lost = new ArrayList<>();
        for (Map.Entry<String, String> entry : before.entrySet()) {
            String now = after.get(entry.getKey());
            String was = entry.getValue();
            if (now == null) {
                lost.add(entry.getKey() + " missing (was " + was + ")");
            } else if (!now.equals(was) && !closeNumbers(was, now)) {
                lost.add(entry.getKey() + " " + was + " -> " + now);
            }
        }
        return lost;
    }

    private static boolean closeNumbers(String a, String b) {
        try {
            return Math.abs(Double.parseDouble(a) - Double.parseDouble(b)) < 1e-4;
        } catch (NumberFormatException notNumbers) {
            return false;
        }
    }

    /** Values 0.2.1 wrote that this branch drops or rewrites on purpose; each entry needs a reason. */
    private static final List<String> EXPECTED_MIGRATION_CHANGES = List.of(
            "/segmentGap missing"); // removed on purpose in cf57ff9: the setting never affected rendering

    @Test
    void releaseConfigLoadsWithoutLosingAnyValue() throws IOException {
        loadWithoutLosingAnyValue(RELEASE_FILE);
    }

    @Test
    void devBuildConfigLoadsWithoutLosingAnyValue() throws IOException {
        loadWithoutLosingAnyValue(DEV_FILE);
    }

    private void loadWithoutLosingAnyValue(String name) throws IOException {
        String text = ownerConfig(name);
        loadText(text);

        assertTrue(corruptBackups().isEmpty(), "a valid file is not treated as damaged");
        assertFalse(ConfigIo.persistenceBlocked);
        String saved = Files.readString(file, StandardCharsets.UTF_8);
        List<String> lost = lostValues(flatten(text), flatten(saved));
        lost.removeIf(line -> EXPECTED_MIGRATION_CHANGES.stream().anyMatch(line::contains));
        assertEquals(List.of(), lost, "values changed or lost by load + save");

        JsonObject root = JsonParser.parseString(saved).getAsJsonObject();
        assertTrue(root.has("labyrinth"), "the new labyrinth section is written with defaults");
        assertTrue(root.has("ui"), "the new ui section is written with defaults");
        assertFalse(ArcaneBeamConfig.INSTANCE.labyrinth.enabled, "labyrinth stays opt-in");
        assertEquals(UiSettings.LEFT, ArcaneBeamConfig.INSTANCE.ui.panelSide);
        assertEquals("Test", ArcaneBeamConfig.INSTANCE.selectedArcaneProfile);
        assertEquals("Main", ArcaneBeamConfig.INSTANCE.selectedLightningStrikeProfile);
    }

    @Test
    void secondLoadOfTheSavedFileIsStable() throws IOException {
        for (String name : new String[] {RELEASE_FILE, DEV_FILE}) {
            loadText(ownerConfig(name));
            String first = Files.readString(file, StandardCharsets.UTF_8);
            loadText(first);
            String second = Files.readString(file, StandardCharsets.UTF_8);
            assertEquals(first, second, name + ": load + save of a saved file must be a fixed point");
        }
    }

    private String withSection(String name, String sectionJson) throws IOException {
        JsonObject root = JsonParser.parseString(ownerConfig()).getAsJsonObject();
        root.add(name, JsonParser.parseString(sectionJson));
        return root.toString();
    }

    @Test
    void hostileLabyrinthAndUiSectionsFallBackSafely() throws IOException {
        String[] labyrinths = {
                "null", "[]", "\"x\"", "42", "{}",
                "{\"opacity\":\"NaN\",\"width\":\"Infinity\",\"thickness\":\"-Infinity\",\"elevation\":-5,\"maxDistance\":2147483647}",
                "{\"opacity\":-1e30,\"width\":1e30,\"maxDistance\":-2147483648}",
                "{\"opacity\":\"abc\",\"enabled\":\"yes\",\"chevrons\":7}",
                "{\"lineColor\":\"#FFF\",\"routeColor\":\"zzz\"}",
                "{\"lineColor\":-1,\"routeColor\":99999999999}",
                "{\"visibility\":\"NOPE\",\"fullbright\":null}",
                "{\"visibility\":123,\"fullbright\":{}}",
                "{\"enabled\":true,\"unknownKey\":[1,2,3]}",
        };
        for (String labyrinth : labyrinths) {
            loadText(withSection("labyrinth", labyrinth));
        }
        String[] uis = {"null", "[]", "{}", "{\"panelSide\":null}", "{\"panelSide\":\"MIDDLE\"}", "{\"panelSide\":\"right\"}",
                "{\"panelSide\":5}", "{\"panelSide\":{}}"};
        for (String ui : uis) {
            loadText(withSection("ui", ui));
        }
        loadText(withSection("ui", "{\"panelSide\":\"right\"}"));
        assertEquals(UiSettings.RIGHT, ArcaneBeamConfig.INSTANCE.ui.panelSide);
    }

    @Test
    void hostileTopLevelShapesFallBackSafely() throws IOException {
        String owner = ownerConfig();
        String[] texts = {
                "", " ", "null", "[]", "42", "\"text\"", "{",
                owner.substring(0, owner.length() / 2),
                owner.substring(0, owner.length() - 2),
                "{\"arcaneProfiles\":null,\"selectedArcaneProfile\":null}",
                "{\"arcaneProfiles\":{},\"selectedArcaneProfile\":\"Gone\"}",
                "{\"arcaneProfiles\":{\"X\":null},\"selectedArcaneProfile\":\"X\"}",
                "{\"arcaneProfiles\":[],\"railProfiles\":\"x\",\"lightningStrikeProfiles\":5}",
                "{\"arcane\":{\"colors\":[],\"glowColors\":null,\"startHand\":\"elbow\",\"fadeInStyle\":\"zoom\"}}",
                "{\"arcane\":{\"colors\":[\"red\"],\"intensity\":\"NaN\"}}",
                "{\"configVersion\":999999,\"labyrinth\":{\"enabled\":true}}",
                "{\"configVersion\":-1}",
                "{\"configVersion\":\"x\"}",
                "{\"dungeonGlobeMap\":{\"opacityPercent\":-50},\"updates\":null,\"shaderCompatibility\":[]}",
        };
        for (String text : texts) {
            loadText(text);
        }
        loadText(owner.substring(0, owner.length() / 2));
        assertFalse(corruptBackups().isEmpty(), "a truncated file is backed up before defaults replace it");
    }
}
