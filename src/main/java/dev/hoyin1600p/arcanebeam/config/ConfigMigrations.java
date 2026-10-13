package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;

import java.util.Map;

/** Version-gated legacy value migrations and legacy beam field normalisation. */
final class ConfigMigrations {
    private ConfigMigrations() {
    }

    /** Bumped whenever a value migration is added; migrations run only for older files. */
    static final int CONFIG_VERSION = Config.CURRENT_VERSION;

    /** Fills fields that older config files expressed differently; only touches absent keys. */
    static void normalizeLegacyBeam(JsonObject object, boolean rail, String globalShader) {
        if (!object.has("intensity") && object.has("radius") && object.get("radius").isJsonPrimitive()) {
            float radius = object.get("radius").getAsFloat();
            if (!rail && radius >= 0.16F) {
                radius = 0.08F;
            }
            if (rail && radius >= 0.22F) {
                radius = 0.11F;
            }
            object.addProperty("intensity", radius);
        }
        if (!object.has("opacity") && object.has("alpha") && object.get("alpha").isJsonPrimitive()) {
            object.add("opacity", object.get("alpha"));
        }
        if (!object.has("colors") && object.has("color") && object.get("color").isJsonPrimitive()) {
            int color = object.get("color").getAsInt();
            JsonArray palette = new JsonArray();
            palette.add(color);
            palette.add(color);
            palette.add(color);
            palette.add(0xFFFFFF);
            object.add("colors", palette);
        }
        if (!object.has("shaderCompatibility") && globalShader != null) {
            object.addProperty("shaderCompatibility", globalShader);
        }
    }

    /**
     * Labyrinth sections written before the route-only overlay: {@code lineColor} (the hall lines are gone) is dropped and
     * the three-way {@code fullbright} mode becomes the Fullbright route switch ("route_only" and "all" lit the route
     * fully, "none" did not). Runs on every load, since those builds did not bump the config version.
     */
    static void normalizeLegacyLabyrinth(JsonObject object) {
        object.remove("lineColor");
        JsonElement mode = object.get("fullbright");
        if (mode != null && mode.isJsonPrimitive() && mode.getAsJsonPrimitive().isString()) {
            String id = mode.getAsString().trim().toLowerCase(java.util.Locale.ROOT);
            if (id.equals("route_only") || id.equals("all")) {
                object.addProperty("fullbright", true);
            } else if (id.equals("none")) {
                object.addProperty("fullbright", false);
            }
        }
    }

    static void migrateLegacyValues(Config config) {
        migrateRail(config.rail);
        if (config.railProfiles != null) {
            config.railProfiles.values().forEach(ConfigMigrations::migrateRail);
        }
        migrateVaultAltar(config.vaultAltar);
        if (config.vaultAltarProfiles != null) {
            config.vaultAltarProfiles.values().forEach(ConfigMigrations::migrateVaultAltar);
        }
        migrateStormFamily(config.stormArrow);
        migrateStormFamily(config.smite);
        migrateStormFamily(config.archon);
        migrateStormFamily(config.blastWave);
        migrateStormProfiles(config.stormArrowProfiles);
        migrateStormProfiles(config.smiteProfiles);
        migrateStormProfiles(config.archonProfiles);
        migrateStormProfiles(config.blastWaveProfiles);
    }

    static void migrateStormProfiles(Map<String, ? extends StormArrowSettings> profiles) {
        if (profiles != null) {
            for (StormArrowSettings settings : profiles.values()) {
                migrateStormFamily(settings);
            }
        }
    }

    static void migrateRail(BeamSettings settings) {
        if (settings != null && settings.color == 0x35D7FF) {
            settings.color = 0x00FF44;
        }
    }

    static void migrateVaultAltar(VaultAltarSettings settings) {
        if (settings == null) {
            return;
        }
        if (settings.cornerVerticalTicks == 10) {
            settings.cornerVerticalTicks = 20;
        }
        if (settings.cornerConvergeTicks == 10) {
            settings.cornerConvergeTicks = 30;
        }
    }

    static void migrateStormFamily(StormArrowSettings settings) {
        if (settings != null && settings.lifetimeTicks == 10) {
            settings.lifetimeTicks = 5;
        }
    }
}
