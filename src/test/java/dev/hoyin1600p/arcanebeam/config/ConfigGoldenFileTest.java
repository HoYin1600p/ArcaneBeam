package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import dev.hoyin1600p.arcanebeam.config.model.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pins the exact bytes the config writes for the bundled defaults and for a legacy unversioned file. */
class ConfigGoldenFileTest {
    private static final Gson WRITER = new GsonBuilder().setPrettyPrinting().create();
    private Object original;

    @BeforeEach
    void remember() {
        original = ArcaneBeamConfig.INSTANCE;
    }

    @AfterEach
    void restore() {
        ArcaneBeamConfig.INSTANCE = (Config) original;
    }

    private static String resource(String name) throws IOException {
        try (InputStream stream = ConfigGoldenFileTest.class.getResourceAsStream("/" + name)) {
            if (stream == null) {
                throw new IOException("Missing test resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static String loadAndSerialize(String json) {
        ArcaneBeamConfig.INSTANCE = ArcaneBeamConfig.parseConfig(JsonParser.parseString(json).getAsJsonObject(), new ArrayList<>());
        ArcaneBeamConfig.validate();
        return WRITER.toJson(ArcaneBeamConfig.INSTANCE);
    }

    private static void maybeWrite(String name, String actual) throws IOException {
        String out = System.getenv("ARCANEBEAM_GOLDEN_OUT");
        if (out != null) {
            Files.writeString(Path.of(out, name), actual, StandardCharsets.UTF_8);
        }
    }

    @Test
    void bundledDefaultsSerializeToGoldenFile() throws IOException {
        String actual = loadAndSerialize(resource("assets/arcanebeam/defaults/ArcaneBeam.json"));
        maybeWrite("bundled-defaults.expected.json", actual);
        assertEquals(resource("config-golden/bundled-defaults.expected.json"), actual);
    }

    @Test
    void legacyUnversionedConfigSerializesToGoldenFile() throws IOException {
        String actual = loadAndSerialize(resource("config-golden/legacy-input.json"));
        maybeWrite("legacy.expected.json", actual);
        assertEquals(resource("config-golden/legacy.expected.json"), actual);
    }
}
