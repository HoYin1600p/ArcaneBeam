package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Default configuration: loads the bundled defaults resource and picks per-beam-kind defaults. */
final class ConfigDefaults {
    private ConfigDefaults() {
    }

    static final String DEFAULT_CONFIG_RESOURCE = "/assets/arcanebeam/defaults/ArcaneBeam.json";

    static Config defaultConfig() {
        try (InputStream stream = ArcaneBeamConfig.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (stream == null) {
                return new Config();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (parsed == null || !parsed.isJsonObject()) {
                    return new Config();
                }
                JsonObject root = parsed.getAsJsonObject();
                // The bundled defaults are already current; never run legacy migrations on them.
                root.addProperty("configVersion", ConfigMigrations.CONFIG_VERSION);
                return ConfigParser.parseConfig(root, new ArrayList<>());
            }
        } catch (IOException | RuntimeException ignored) {
            return new Config();
        }
    }

    static BeamSettings defaultSettings(boolean rail) {
        return rail ? BeamSettings.defaultRail() : BeamSettings.defaultArcane();
    }
}
