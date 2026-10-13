package dev.hoyin1600p.arcanebeam.config;

import dev.hoyin1600p.arcanebeam.config.model.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** K1 (transient read failure vs damage), K2 (failed save is retried) and K5 (read-only directory). */
class ConfigIoTest {
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

    private List<Path> corruptBackups() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().contains(".corrupt-")).toList();
        }
    }

    private static String validConfigWithProfile(String profile) {
        ConfigIo.persistenceBlocked = false;
        Config config = ConfigDefaults.defaultConfig();
        config.arcaneProfiles = new java.util.LinkedHashMap<>();
        config.arcaneProfiles.put(profile, config.arcane);
        config.selectedArcaneProfile = profile;
        return ConfigParser.GSON.toJson(config);
    }

    @Test
    void transientReadFailureKeepsTheFileAndBlocksSaving() throws IOException {
        byte[] content = validConfigWithProfile("Mine").getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);
        AtomicInteger attempts = new AtomicInteger();
        ConfigIo.reader = path -> {
            attempts.incrementAndGet();
            throw new AccessDeniedException(path.toString());
        };

        ConfigIo.load();
        ConfigIo.awaitIdle(5_000L);

        assertEquals(ConfigIo.READ_ATTEMPTS, attempts.get(), "a locked file is retried a few times");
        assertTrue(ConfigIo.persistenceBlocked);
        assertTrue(corruptBackups().isEmpty(), "a locked file is not corruption");
        assertArrayEquals(content, Files.readAllBytes(file), "the user's file must stay untouched");
        assertTrue(ArcaneBeamConfig.INSTANCE.arcaneProfiles.containsKey("Default"), "defaults are used for the session");

        ArcaneBeamConfig.save();
        ConfigIo.awaitIdle(5_000L);
        assertArrayEquals(content, Files.readAllBytes(file), "saving stays blocked");
    }

    @Test
    void aReadThatRecoversInsideTheRetriesLoadsTheFile() throws IOException {
        Files.writeString(file, validConfigWithProfile("Mine"), StandardCharsets.UTF_8);
        AtomicInteger attempts = new AtomicInteger();
        ConfigIo.reader = path -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("The process cannot access the file because it is being used by another process");
            }
            return Files.readAllBytes(path);
        };

        ConfigIo.load();

        assertFalse(ConfigIo.persistenceBlocked);
        assertEquals("Mine", ArcaneBeamConfig.INSTANCE.selectedArcaneProfile);
        assertEquals(3, attempts.get());
    }

    @Test
    void legacyCharsetFileIsReadNotTreatedAsCorrupt() throws IOException {
        String text = validConfigWithProfile("Café");
        Files.write(file, text.getBytes(Charset.forName("windows-1252")));

        ConfigIo.load();

        assertTrue(corruptBackups().isEmpty());
        assertEquals("Café", ArcaneBeamConfig.INSTANCE.selectedArcaneProfile);
    }

    @Test
    void utf8WithBomAndUtf16AreDecoded() {
        byte[] json = "{\"a\":\"é\"}".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[json.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(json, 0, withBom, 3, json.length);
        assertEquals("{\"a\":\"é\"}", ConfigIo.decode(withBom));
        byte[] utf16 = ("﻿" + "{\"a\":\"é\"}").getBytes(StandardCharsets.UTF_16LE);
        byte[] marked = new byte[utf16.length + 2];
        marked[0] = (byte) 0xFF;
        marked[1] = (byte) 0xFE;
        System.arraycopy(utf16, 0, marked, 2, utf16.length);
        assertTrue(ConfigIo.decode(marked).endsWith("{\"a\":\"é\"}"));
    }

    @Test
    void trulyDamagedTextIsPreservedAndReplacedWithDefaults() throws IOException {
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        ConfigIo.load();
        ConfigIo.awaitIdle(5_000L);

        assertEquals(1, corruptBackups().size());
        assertEquals("{ this is not json", Files.readString(corruptBackups().get(0)));
        assertFalse(ConfigIo.persistenceBlocked);
        assertTrue(Files.readString(file).contains("\"arcaneProfiles\""), "defaults were written");
    }

    @Test
    void brokenMirrorWithGoodProfileLeavesNoCorruptBackup() throws IOException {
        Files.writeString(file, "{\"archon\":\"oops\",\"archonProfiles\":{\"Default\":{\"circleAlpha\":0.42}}}", StandardCharsets.UTF_8);

        ConfigIo.load();

        assertTrue(corruptBackups().isEmpty());
        assertEquals(0.42F, ArcaneBeamConfig.INSTANCE.archon.circleAlpha);
    }

    @Test
    void brokenProfileStillCopiesTheFileToACorruptBackup() throws IOException {
        Files.writeString(file, "{\"archonProfiles\":{\"Default\":\"oops\"}}", StandardCharsets.UTF_8);

        ConfigIo.load();

        assertEquals(1, corruptBackups().size());
    }

    @Test
    void unchangedConfigIsNotRewrittenOnLoad() throws IOException {
        Files.writeString(file, validConfigWithProfile("Default"), StandardCharsets.UTF_8);
        ConfigIo.load();
        ConfigIo.awaitIdle(5_000L);
        byte[] written = Files.readAllBytes(file);
        long modified = Files.getLastModifiedTime(file).toMillis();

        ConfigIo.load();
        ConfigIo.awaitIdle(5_000L);

        assertArrayEquals(written, Files.readAllBytes(file));
        assertEquals(modified, Files.getLastModifiedTime(file).toMillis());
    }

    @Test
    void failedWriteIsReArmedWithBackoffAndSucceedsOnceTheDirectoryWorks() throws IOException {
        ArcaneBeamConfig.INSTANCE = ConfigDefaults.defaultConfig();
        ConfigValidation.validate();
        Path blocker = directory.resolve("not-a-directory");
        Files.writeString(blocker, "x");
        ConfigIo.pathOverride = blocker.resolve("ArcaneBeam.json");

        ArcaneBeamConfig.save();
        assertTrue(ConfigIo.awaitIdle(5_000L));

        assertEquals(1, ConfigIo.writeFailureCount());
        assertTrue(ConfigIo.savePending, "the failed save is re-armed");
        long dueIn = ConfigIo.lastSaveRequestNanos + ConfigIo.SAVE_DEBOUNCE_NANOS - System.nanoTime();
        assertTrue(dueIn > 1_000_000_000L, "the retry waits for the backoff instead of spinning: " + dueIn);
        ConfigIo.tickPendingSave();
        ConfigIo.awaitIdle(5_000L);
        assertEquals(1, ConfigIo.writeFailureCount(), "no retry before the backoff has passed");

        ConfigIo.pathOverride = file;
        ArcaneBeamConfig.save();
        assertTrue(ConfigIo.awaitIdle(5_000L));

        assertTrue(Files.exists(file));
        assertEquals(0, ConfigIo.writeFailureCount());
    }

    @Test
    void repeatedFailuresStopRetryingAutomaticallyAfterTheCap() throws IOException {
        ArcaneBeamConfig.INSTANCE = ConfigDefaults.defaultConfig();
        ConfigValidation.validate();
        Path blocker = directory.resolve("not-a-directory");
        Files.writeString(blocker, "x");
        ConfigIo.pathOverride = blocker.resolve("ArcaneBeam.json");

        for (int attempt = 0; attempt <= ConfigIo.MAX_AUTOMATIC_WRITE_RETRIES + 1; attempt++) {
            ArcaneBeamConfig.save();
            ConfigIo.awaitIdle(5_000L);
        }

        assertFalse(ConfigIo.savePending, "a permanently read-only directory must not retry forever");
    }

    @Test
    void flushWaitsForTheBackgroundWrite() throws IOException {
        ArcaneBeamConfig.INSTANCE = ConfigDefaults.defaultConfig();
        ConfigValidation.validate();
        ArcaneBeamConfig.requestSave();

        ArcaneBeamConfig.flushPendingSave();

        assertTrue(Files.exists(file), "flush returns only after the file is written");
    }
}
