package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("arcanebeam").resolve("player_beams");
    private static final Path ASSIGNMENTS_PATH = CONFIG_DIRECTORY.resolve("assignments.json");
    private static final Path README_PATH = CONFIG_DIRECTORY.resolve("README.txt");
    private static final Map<UUID, ResourceLocation> BY_UUID = new HashMap<>();
    private static final Map<String, ResourceLocation> BY_NAME = new HashMap<>();

    private PlayerBeamAssignments() {
    }

    public static void load() {
        BY_UUID.clear();
        BY_NAME.clear();

        try {
            Files.createDirectories(CONFIG_DIRECTORY);
            writeTemplatesIfMissing();
        } catch (IOException exception) {
            ArcaneBeam.LOGGER.warn("Unable to create player beam assignment directory {}", CONFIG_DIRECTORY, exception);
            return;
        }

        try (Reader reader = Files.newBufferedReader(ASSIGNMENTS_PATH, StandardCharsets.UTF_8)) {
            AssignmentFile file = GSON.fromJson(reader, AssignmentFile.class);
            if (file == null || file.players == null) {
                return;
            }
            for (PlayerAssignment assignment : file.players) {
                register(assignment);
            }
        } catch (IOException | RuntimeException exception) {
            ArcaneBeam.LOGGER.warn("Unable to read player beam assignments from {}", ASSIGNMENTS_PATH, exception);
        }
    }

    public static ResourceLocation profileFor(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        ResourceLocation byUuid = BY_UUID.get(player.getUUID());
        if (byUuid != null) {
            return byUuid;
        }
        return BY_NAME.get(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
    }

    private static void register(PlayerAssignment assignment) {
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
                BY_UUID.put(UUID.fromString(assignment.uuid.trim()), profile);
                registered = true;
            } catch (IllegalArgumentException exception) {
                ArcaneBeam.LOGGER.warn("Ignoring invalid player UUID '{}' in {}", assignment.uuid, ASSIGNMENTS_PATH);
            }
        }
        if (assignment.name != null && !assignment.name.isBlank()) {
            BY_NAME.put(assignment.name.trim().toLowerCase(Locale.ROOT), profile);
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
