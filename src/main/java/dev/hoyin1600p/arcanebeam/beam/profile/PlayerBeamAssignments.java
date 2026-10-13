package dev.hoyin1600p.arcanebeam.beam.profile;

import com.google.gson.Gson;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.util.Gsons;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PlayerBeamAssignments {
    private static final Gson GSON = Gsons.PRETTY;
    private static final Path CONFIG_DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("arcanebeam").resolve("player_beams");
    private static final Path ASSIGNMENTS_PATH = CONFIG_DIRECTORY.resolve("assignments.json");
    private static final Path README_PATH = CONFIG_DIRECTORY.resolve("README.txt");
    private static volatile Map<UUID, ResourceLocation> byUuid = Map.of();
    private static volatile Map<String, ResourceLocation> byName = Map.of();

    private PlayerBeamAssignments() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIRECTORY);
            writeTemplatesIfMissing();
        } catch (IOException exception) {
            ArcaneBeam.LOGGER.warn("Unable to create player beam assignment directory {}", CONFIG_DIRECTORY, exception);
            return;
        }

        try (Reader reader = Files.newBufferedReader(ASSIGNMENTS_PATH, StandardCharsets.UTF_8)) {
            AssignmentFile file = GSON.fromJson(reader, AssignmentFile.class);
            // Build fresh maps and swap them in only on success so a failed read keeps the previous assignments.
            Map<UUID, ResourceLocation> newByUuid = new HashMap<>();
            Map<String, ResourceLocation> newByName = new HashMap<>();
            if (file != null && file.players != null) {
                for (PlayerAssignment assignment : file.players) {
                    register(assignment, newByUuid, newByName);
                }
            }
            byUuid = newByUuid;
            byName = newByName;
        } catch (IOException | RuntimeException exception) {
            ArcaneBeam.LOGGER.warn("Unable to read player beam assignments from {}", ASSIGNMENTS_PATH, exception);
        }
    }

    public static ResourceLocation profileFor(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        ResourceLocation uuidProfile = byUuid.get(player.getUUID());
        if (uuidProfile != null) {
            return uuidProfile;
        }
        return byName.get(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    private static void register(PlayerAssignment assignment, Map<UUID, ResourceLocation> uuidTarget,
                                 Map<String, ResourceLocation> nameTarget) {
        if (assignment == null || assignment.profile == null || assignment.profile.isBlank()) {
            return;
        }
        ResourceLocation profile = ResourceLocation.tryParse(assignment.profile.trim());
        if (profile == null) {
            ArcaneBeam.LOGGER.warn("Ignoring invalid player beam profile id '{}'", assignment.profile);
            return;
        }

        boolean registered = false;
        if (assignment.uuid != null && !assignment.uuid.isBlank()) {
            try {
                uuidTarget.put(UUID.fromString(assignment.uuid.trim()), profile);
                registered = true;
            } catch (IllegalArgumentException exception) {
                ArcaneBeam.LOGGER.warn("Ignoring invalid player UUID '{}' in {}", assignment.uuid, ASSIGNMENTS_PATH);
            }
        }
        if (assignment.name != null && !assignment.name.isBlank()) {
            nameTarget.put(assignment.name.trim().toLowerCase(Locale.ROOT), profile);
            registered = true;
        }
        if (!registered) {
            ArcaneBeam.LOGGER.warn("Ignoring player beam assignment without a UUID or name for profile '{}'", profile);
        }
    }

    private static void writeTemplatesIfMissing() throws IOException {
        if (!Files.exists(ASSIGNMENTS_PATH)) {
            try (Writer writer = Files.newBufferedWriter(ASSIGNMENTS_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(new AssignmentFile(), writer);
            }
        }
        if (!Files.exists(README_PATH)) {
            Files.writeString(README_PATH,
                    "Assign enabled resource-pack beam profiles to remote players.\n"
                            + "UUID matching takes priority; name is an optional fallback.\n\n"
                            + "Example assignments.json entry:\n"
                            + "{\n"
                            + "  \"players\": [\n"
                            + "    {\n"
                            + "      \"uuid\": \"00000000-0000-0000-0000-000000000000\",\n"
                            + "      \"name\": \"PlayerName\",\n"
                            + "      \"profile\": \"example_pack:blue_arcane\"\n"
                            + "    }\n"
                            + "  ]\n"
                            + "}\n\n"
                            + "Resource-pack profile location:\n"
                            + "assets/<namespace>/arcanebeam/beam_profiles/<profile>.json\n"
                            + "Profiles may contain arcane/rail beam settings or arcaneParticle/railParticle texture settings.\n"
                            + "Press F3+T after editing assignments or resource-pack profiles.\n",
                    StandardCharsets.UTF_8);
        }
    }

    private static final class AssignmentFile {
        private List<PlayerAssignment> players = new ArrayList<>();
    }

    private static final class PlayerAssignment {
        private String uuid;
        private String name;
        private String profile;
    }
}
