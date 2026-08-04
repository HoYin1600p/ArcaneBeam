package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

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
                if (profile.arcane != null || profile.rail != null) {
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

    private static final class PlayerBeamProfile {
        private ArcaneBeamConfig.BeamSettings arcane;
        private ArcaneBeamConfig.BeamSettings rail;
    }
}
