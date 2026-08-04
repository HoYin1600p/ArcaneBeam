package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

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
                PlayerBeamProfile profile = GSON.fromJson(json, PlayerBeamProfile.class);
                if (profile == null) {
                    return;
                }
                profile.arcane = ArcaneBeamConfig.validateExternalBeamSettings(profile.arcane, false);
                profile.rail = ArcaneBeamConfig.validateExternalBeamSettings(profile.rail, true);
                profile.arcaneParticle = validateParticleSettings(profile.arcaneParticle, id, "arcaneParticle");
                profile.railParticle = validateParticleSettings(profile.railParticle, id, "railParticle");
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

    public static ArcaneBeamConfig.BeamSettings settings(ResourceLocation profileId, ArcaneBeamManager.BeamKind kind) {
        PlayerBeamProfile profile = profileId == null ? null : profiles.get(profileId);
        if (profile == null) {
            return null;
        }
        return kind == ArcaneBeamManager.BeamKind.RAIL ? profile.rail : profile.arcane;
    }

    public static ParticleSettings particleSettings(ResourceLocation profileId, ArcaneBeamManager.BeamKind kind) {
        PlayerBeamProfile profile = profileId == null ? null : profiles.get(profileId);
        if (profile == null) {
            return null;
        }
        return kind == ArcaneBeamManager.BeamKind.RAIL ? profile.railParticle : profile.arcaneParticle;
    }

    private static ParticleSettings validateParticleSettings(ParticleSettings settings, ResourceLocation profileId, String field) {
        if (settings == null || settings.texture == null || settings.texture.isBlank()) {
            return null;
        }
        ResourceLocation texture = ResourceLocation.tryParse(settings.texture.trim());
        if (texture == null) {
            ArcaneBeam.LOGGER.warn("Ignoring invalid {} texture '{}' in player beam profile {}",
                    field, settings.texture, profileId);
            return null;
        }
        settings.texture = texture.toString();
        settings.lifetimeTicks = Mth.clamp(settings.lifetimeTicks, 1, 40);
        settings.size = Mth.clamp(settings.size, 0.01F, 4.0F);
        settings.alpha = Mth.clamp(settings.alpha, 0.0F, 1.0F);
        return settings;
    }

    public static final class ParticleSettings {
        private String texture;
        private int lifetimeTicks = 5;
        private float size = 0.4F;
        private float alpha = 1.0F;
        private boolean fullbright = true;

        public ResourceLocation texture() {
            return new ResourceLocation(texture);
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
        private ArcaneBeamConfig.BeamSettings arcane;
        private ArcaneBeamConfig.BeamSettings rail;
        private ParticleSettings arcaneParticle;
        private ParticleSettings railParticle;
    }
}
