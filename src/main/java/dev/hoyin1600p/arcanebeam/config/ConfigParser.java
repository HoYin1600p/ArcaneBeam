package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig.INSTANCE;

/** Reads a config file section by section; owns the Gson instances that fill absent beam fields per beam kind. */
final class ConfigParser {
    private ConfigParser() {
    }

    // Absent beam fields must fall back to the defaults of the beam kind being read.
    static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(BeamSettings.class, (InstanceCreator<BeamSettings>) type -> BeamSettings.defaultArcane())
            .create();
    static final Gson RAIL_GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(BeamSettings.class, (InstanceCreator<BeamSettings>) type -> BeamSettings.defaultRail())
            .create();

    /**
     * Reads each top-level section independently. A section that cannot be read keeps its default and is
     * named in {@code failedSections}. Legacy value migrations run only when the file predates
     * {@link ConfigMigrations#CONFIG_VERSION}.
     */
    static Config parseConfig(JsonObject root, List<String> failedSections) {
        Config config = new Config();
        int fileVersion = 0;
        JsonElement versionElement = root.get("configVersion");
        if (versionElement != null && versionElement.isJsonPrimitive() && versionElement.getAsJsonPrimitive().isNumber()) {
            try {
                fileVersion = versionElement.getAsInt();
            } catch (RuntimeException ignored) {
                fileVersion = 0;
            }
        }
        String globalShader = null;
        JsonElement shaderElement = root.get("shaderCompatibility");
        if (shaderElement != null && shaderElement.isJsonPrimitive()) {
            globalShader = shaderElement.getAsString();
        }
        List<String> failedMirrors = new ArrayList<>();
        for (Field field : Config.class.getFields()) {
            String name = field.getName();
            if (Modifier.isStatic(field.getModifiers()) || "configVersion".equals(name)) {
                continue;
            }
            JsonElement element = root.get(name);
            if (element == null) {
                continue;
            }
            try {
                boolean rail = name.startsWith("rail");
                Gson gson = rail ? RAIL_GSON : GSON;
                boolean beam = "arcane".equals(name) || "rail".equals(name);
                if (LinkedHashMap.class.equals(field.getType()) && element.isJsonObject()) {
                    field.set(config, parseProfileMap(gson, element.getAsJsonObject(), field, name, failedSections, globalShader));
                } else {
                    if ("labyrinth".equals(name) && element.isJsonObject()) {
                        ConfigMigrations.normalizeLegacyLabyrinth(element.getAsJsonObject());
                    }
                    if (element.isJsonObject() && LenientJson.isSettingsClass(field.getType())) {
                        LenientJson.coerce(element.getAsJsonObject(), field.getType(), name);
                    }
                    if (beam && element.isJsonObject()) {
                        ConfigMigrations.normalizeLegacyBeam(element.getAsJsonObject(), rail, globalShader);
                    }
                    field.set(config, gson.fromJson(element, field.getGenericType()));
                }
            } catch (RuntimeException | IllegalAccessException exception) {
                if (MIRROR_SECTIONS.contains(name)) {
                    failedMirrors.add(name);
                } else {
                    failedSections.add(name);
                }
                ArcaneBeam.LOGGER.warn("Arcane Beam config section '{}' could not be read; using its default", name, exception);
            }
        }
        resolveFailedMirrors(config, failedMirrors, failedSections);
        warnAboutEditedMirrors(config, root, failedMirrors);
        if (fileVersion < ConfigMigrations.CONFIG_VERSION) {
            ConfigMigrations.migrateLegacyValues(config);
            config.configVersion = ConfigMigrations.CONFIG_VERSION;
        } else {
            config.configVersion = fileVersion;
        }
        return config;
    }

    /**
     * The top-level ability sections. They mirror the selected entry of the matching {@code <name>Profiles} map,
     * which is the source of truth: on load the mirror is replaced by that entry.
     */
    static final List<String> MIRROR_SECTIONS = List.of(
            "arcane", "rail", "lightningStrike", "vaultAltar", "stormArrow", "smite", "archon", "blastWave");

    /**
     * A mirror that cannot be read only matters when the profile it mirrors is also unreadable. When the selected
     * profile loaded, the mirror is dropped without marking the file as damaged (which would copy it to .corrupt).
     */
    private static void resolveFailedMirrors(Config config, List<String> failedMirrors, List<String> failedSections) {
        for (String mirror : failedMirrors) {
            if (selectedProfileLoaded(config, mirror, failedSections)) {
                ArcaneBeam.LOGGER.warn("Arcane Beam config section '{}' is only a mirror of {}Profiles and was ignored; "
                        + "the profile '{}' loaded fine", mirror, mirror, selectedProfileName(config, mirror));
            } else {
                failedSections.add(mirror);
            }
        }
    }

    private static boolean selectedProfileLoaded(Config config, String mirror, List<String> failedSections) {
        String profilesName = mirror + "Profiles";
        for (String failed : failedSections) {
            if (failed.equals(profilesName) || failed.startsWith(profilesName + ".")) {
                return false;
            }
        }
        Map<?, ?> profiles = (Map<?, ?>) readField(config, profilesName);
        String selected = selectedProfileName(config, mirror);
        return profiles != null && selected != null && profiles.get(selected) != null;
    }

    private static String selectedProfileName(Config config, String mirror) {
        Object selected = readField(config, "selected" + Character.toUpperCase(mirror.charAt(0)) + mirror.substring(1) + "Profile");
        return selected instanceof String name ? name : null;
    }

    private static Object readField(Config config, String name) {
        try {
            return Config.class.getField(name).get(config);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    /**
     * Tells the user when a hand edit went into a mirror section: the selected profile wins on load, so the edit is
     * discarded on the next save. Which of the two the user meant cannot be known, hence a warning, not a merge.
     */
    private static void warnAboutEditedMirrors(Config config, JsonObject root, List<String> failedMirrors) {
        for (String mirror : MIRROR_SECTIONS) {
            if (failedMirrors.contains(mirror)) {
                continue;
            }
            try {
                Object mirrorValue = readField(config, mirror);
                Map<?, ?> profiles = (Map<?, ?>) readField(config, mirror + "Profiles");
                String selected = selectedProfileName(config, mirror);
                if (mirrorValue == null || profiles == null || selected == null || profiles.get(selected) == null
                        || !root.has(mirror) || !root.has(mirror + "Profiles")) {
                    continue;
                }
                Gson gson = mirror.startsWith("rail") ? RAIL_GSON : GSON;
                if (!gson.toJsonTree(mirrorValue).equals(gson.toJsonTree(profiles.get(selected)))) {
                    ArcaneBeam.LOGGER.warn("Arcane Beam config section '{}' differs from {}Profiles.{}; the profile is used and the "
                            + "section is overwritten on the next save. Edit {}Profiles.{} instead.",
                            mirror, mirror, selected, mirror, selected);
                }
            } catch (RuntimeException ignored) {
                // The comparison is advisory only.
            }
        }
    }

    static Object parseProfileMap(Gson gson, JsonObject entries, Field field, String sectionName,
                                          List<String> failedSections, String globalShader) {
        Type valueType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
        boolean beam = "arcaneProfiles".equals(sectionName) || "railProfiles".equals(sectionName);
        boolean rail = "railProfiles".equals(sectionName);
        LinkedHashMap<String, Object> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            try {
                JsonElement value = entry.getValue();
                if (value.isJsonObject() && valueType instanceof Class<?> valueClass) {
                    LenientJson.coerce(value.getAsJsonObject(), valueClass, sectionName + "." + entry.getKey());
                }
                if (beam && value.isJsonObject()) {
                    ConfigMigrations.normalizeLegacyBeam(value.getAsJsonObject(), rail, globalShader);
                }
                parsed.put(entry.getKey(), gson.fromJson(value, valueType));
            } catch (RuntimeException exception) {
                failedSections.add(sectionName + "." + entry.getKey());
                ArcaneBeam.LOGGER.warn("Arcane Beam config profile '{}' in section '{}' could not be read; skipping it",
                        entry.getKey(), sectionName, exception);
            }
        }
        return parsed;
    }

    /**
     * Reads one resource-pack beam section. Absent fields take the defaults of the beam kind, and the result
     * gets the same bounds as a user config. Returns null when the section is absent or not an object.
     */
    static BeamSettings parseExternalBeamSettings(JsonElement element, boolean rail) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject copy = element.getAsJsonObject().deepCopy();
        LenientJson.coerce(copy, BeamSettings.class, rail ? "resource pack rail" : "resource pack arcane");
        ConfigMigrations.normalizeLegacyBeam(copy, rail, INSTANCE.shaderCompatibility);
        return ConfigValidation.validateExternalBeamSettings((rail ? RAIL_GSON : GSON).fromJson(copy, BeamSettings.class), rail);
    }
}
