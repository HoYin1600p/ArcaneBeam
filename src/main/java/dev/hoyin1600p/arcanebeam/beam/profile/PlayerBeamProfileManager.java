package dev.hoyin1600p.arcanebeam.beam.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.beam.BeamKind;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class PlayerBeamProfileManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PROFILE_DIRECTORY = "arcanebeam/beam_profiles";
    private static volatile Map<ResourceLocation, PlayerBeamProfile> profiles = Map.of();

    public PlayerBeamProfileManager() {
        super(GSON, PROFILE_DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, PlayerBeamProfile> loaded = new HashMap<>();
        resources.forEach((id, json) -> {
            try {
                if (json == null || !json.isJsonObject()) {
                    return;
                }
                JsonObject object = json.getAsJsonObject();
                PlayerBeamProfile profile = new PlayerBeamProfile();
                // Each section is read on its own so one bad section cannot discard the rest of the profile.
                profile.arcane = readBeam(object, "arcane", false, id);
                profile.rail = readBeam(object, "rail", true, id);
                profile.arcaneParticle = validateParticleSettings(readParticle(object, "arcaneParticle", id), id, "arcaneParticle");
                profile.railParticle = validateParticleSettings(readParticle(object, "railParticle", id), id, "railParticle");
                if (profile.arcane != null || profile.rail != null
                        || profile.arcaneParticle != null || profile.railParticle != null) {
                    loaded.put(id, profile);
                }
            } catch (RuntimeException exception) {
                ArcaneBeam.LOGGER.warn("Unable to load player beam profile {}", id, exception);
            }
        });
        profiles = Map.copyOf(loaded);
        PlayerBeamAssignments.load();
        ArcaneBeam.LOGGER.info("Loaded {} remote player beam profile(s)", profiles.size());
    }

    public static BeamSettings settings(ResourceLocation profileId, BeamKind kind) {
        PlayerBeamProfile profile = profileId == null ? null : profiles.get(profileId);
        if (profile == null) {
            return null;
        }
        return kind == BeamKind.RAIL ? profile.rail : profile.arcane;
    }

    public static ParticleSettings particleSettings(ResourceLocation profileId, BeamKind kind) {
        PlayerBeamProfile profile = profileId == null ? null : profiles.get(profileId);
        if (profile == null) {
            return null;
        }
        return kind == BeamKind.RAIL ? profile.railParticle : profile.arcaneParticle;
    }

    private static BeamSettings readBeam(JsonObject object, String field, boolean rail, ResourceLocation profileId) {
        try {
            return ArcaneBeamConfig.parseExternalBeamSettings(object.get(field), rail);
        } catch (RuntimeException exception) {
            ArcaneBeam.LOGGER.warn("Ignoring invalid {} section in player beam profile {}", field, profileId, exception);
            return null;
        }
    }

    private static ParticleSettings readParticle(JsonObject object, String field, ResourceLocation profileId) {
        try {
            JsonElement element = object.get(field);
            return element == null || !element.isJsonObject() ? null : GSON.fromJson(element, ParticleSettings.class);
        } catch (RuntimeException exception) {
            ArcaneBeam.LOGGER.warn("Ignoring invalid {} section in player beam profile {}", field, profileId, exception);
            return null;
        }
    }

    private static float finiteClamp(float value, float fallback, float min, float max) {
        return Mth.clamp(Float.isFinite(value) ? value : fallback, min, max);
    }

    private static ParticleSettings validateParticleSettings(ParticleSettings settings, ResourceLocation profileId, String field) {
        if (settings == null) {
            return null;
        }

        List<ResourceLocation> textures = new ArrayList<>();
        addTexture(textures, settings.texture, profileId, field);
        if (settings.textures != null) {
            for (String texture : settings.textures) {
                addTexture(textures, texture, profileId, field);
            }
        }
        if (textures.isEmpty()) {
            return null;
        }
        settings.resolvedTextures = List.copyOf(textures);
        settings.lifetimeTicks = Mth.clamp(settings.lifetimeTicks, 1, 40);
        settings.size = finiteClamp(settings.size, 0.4F, 0.01F, 4.0F);
        settings.alpha = finiteClamp(settings.alpha, 1.0F, 0.0F, 1.0F);
        return settings;
    }

    private static void addTexture(List<ResourceLocation> textures, String rawTexture,
                                   ResourceLocation profileId, String field) {
        if (rawTexture == null || rawTexture.isBlank()) {
            return;
        }
        ResourceLocation texture = ResourceLocation.tryParse(rawTexture.trim());
        if (texture == null) {
            ArcaneBeam.LOGGER.warn("Ignoring invalid {} texture '{}' in player beam profile {}",
                    field, rawTexture, profileId);
            return;
        }
        textures.add(texture);
    }

    public static final class ParticleSettings {
        private String texture;
        private List<String> textures;
        private int lifetimeTicks = 5;
        private float size = 0.4F;
        private float alpha = 1.0F;
        private boolean fullbright = true;
        private transient List<ResourceLocation> resolvedTextures = List.of();

        public ResourceLocation chooseTexture(Random random) {
            return resolvedTextures.get(random.nextInt(resolvedTextures.size()));
        }

        public int lifetimeTicks() {
            return lifetimeTicks;
        }

        public float size() {
            return size;
        }

        public float alpha() {
            return alpha;
        }

        public boolean fullbright() {
            return fullbright;
        }
    }

    private static final class PlayerBeamProfile {
        private BeamSettings arcane;
        private BeamSettings rail;
        private ParticleSettings arcaneParticle;
        private ParticleSettings railParticle;
    }
}
