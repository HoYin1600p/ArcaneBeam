package dev.hoyin1600p.arcanebeam.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.model.Config;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig.INSTANCE;

/**
 * Reads and writes ArcaneBeam.json: path, load, debounced save, atomic write and corrupt-file preservation.
 *
 * <p>Loading tells a file that cannot be read right now (locked, access denied) from one whose text is damaged:
 * only damaged text is moved to {@code .corrupt-<time>}. Saving serialises on the calling thread and writes on one
 * background daemon thread, so the fsync never stalls a frame; a failed write is retried with a growing delay.
 */
final class ConfigIo {
    private ConfigIo() {
    }

    static final long SAVE_DEBOUNCE_NANOS = 500_000_000L;
    static final int READ_ATTEMPTS = 4;
    static final long READ_RETRY_BASE_MILLIS = 100L;
    static final long WRITE_RETRY_BASE_NANOS = 2_000_000_000L;
    static final long WRITE_RETRY_MAX_NANOS = 60_000_000_000L;
    static final int MAX_AUTOMATIC_WRITE_RETRIES = 8;
    private static final long FLUSH_TIMEOUT_MILLIS = 3_000L;
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 5_000L;

    static volatile boolean savePending;
    static volatile long lastSaveRequestNanos;
    static volatile boolean persistenceBlocked;

    /** Test seams: the config path, how bytes are read and how the loader waits between read attempts. */
    interface FileReader {
        byte[] read(Path path) throws IOException;
    }

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    static Path pathOverride;
    static FileReader reader = Files::readAllBytes;
    static Sleeper sleeper = Thread::sleep;

    private static final Object IO_LOCK = new Object();
    private static ExecutorService executor;
    private static byte[] pendingBytes;
    private static boolean workerScheduled;
    private static boolean inFlight;
    private static volatile byte[] lastKnownFileBytes;
    private static int writeFailures;
    private static boolean blockedWarned;
    private static boolean shutdownHookInstalled;

    /** Resolved lazily so the pure parse/validate logic can run without Forge. */
    static final class PathHolder {
        private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("ArcaneBeam.json");
    }

    static Path configPath() {
        return pathOverride != null ? pathOverride : PathHolder.CONFIG_PATH;
    }

    static void load() {
        loadFrom(configPath());
    }

    static void loadFrom(Path path) {
        persistenceBlocked = false;
        blockedWarned = false;
        writeFailures = 0;
        lastKnownFileBytes = null;
        byte[] bytes = null;
        boolean unreadable = false;
        boolean transientFailure = false;
        List<String> failedSections = new ArrayList<>();
        Config loaded = null;
        if (Files.exists(path)) {
            try {
                bytes = readWithRetry(path);
            } catch (IOException failure) {
                transientFailure = true;
                persistenceBlocked = true;
                ArcaneBeam.LOGGER.warn("Arcane Beam config {} could not be read after {} attempts ({}); defaults are used for "
                                + "this session and the file will NOT be changed or saved over. Close whatever holds the file "
                                + "(another game instance, a sync or backup tool) and restart.",
                        path, READ_ATTEMPTS, failure.toString());
            }
            if (bytes != null) {
                try {
                    JsonElement parsed = JsonParser.parseString(decode(bytes));
                    if (parsed != null && parsed.isJsonObject()) {
                        loaded = ConfigParser.parseConfig(parsed.getAsJsonObject(), failedSections);
                    } else if (parsed != null && !parsed.isJsonNull()) {
                        throw new IllegalStateException("Config root is not a JSON object");
                    }
                } catch (JsonParseException | IllegalStateException | UnsupportedOperationException failure) {
                    unreadable = true;
                    ArcaneBeam.LOGGER.warn("Arcane Beam config {} is damaged and could not be parsed; defaults will be restored", path, failure);
                }
            }
            if (!transientFailure && (unreadable || !failedSections.isEmpty())) {
                if (!preserveOriginalConfig(path, unreadable)) {
                    persistenceBlocked = true;
                    ArcaneBeam.LOGGER.error("Arcane Beam config {} could not be backed up; it will not be overwritten this session", path);
                }
            } else if (!transientFailure && bytes != null) {
                lastKnownFileBytes = bytes;
            }
        }
        INSTANCE = loaded != null ? loaded : ConfigDefaults.defaultConfig();
        ConfigValidation.validate();
        ArcaneBeamConfig.save();
    }

    /** Reads the file, retrying a few times briefly when it is locked. A file that vanished reads as empty. */
    private static byte[] readWithRetry(Path path) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= READ_ATTEMPTS; attempt++) {
            try {
                return reader.read(path);
            } catch (NoSuchFileException missing) {
                return null;
            } catch (IOException failure) {
                last = failure;
                if (attempt < READ_ATTEMPTS) {
                    try {
                        sleeper.sleep(READ_RETRY_BASE_MILLIS << (attempt - 1));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw last;
    }

    /**
     * Decodes config text. UTF-8 first (a BOM is honoured for UTF-8 and UTF-16), then windows-1252 and finally
     * ISO-8859-1 for a file saved by an editor in a legacy encoding, so a non-UTF-8 save is not mistaken for damage.
     */
    static String decode(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        int offset = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("windows-1252"))) {
            try {
                return charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                        .toString();
            } catch (CharacterCodingException notThisCharset) {
                // Try the next charset.
            }
        }
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.ISO_8859_1);
    }

    /** Marks the config dirty; the write happens from {@link #tickPendingSave()} or {@link #flushPendingSave()}. */
    static void requestSave() {
        lastSaveRequestNanos = System.nanoTime();
        savePending = true;
    }

    /** Starts a save requested through {@link #requestSave()} once it has been quiet for about 500 ms. */
    static void tickPendingSave() {
        if (savePending && System.nanoTime() - lastSaveRequestNanos >= SAVE_DEBOUNCE_NANOS) {
            ArcaneBeamConfig.save();
        }
    }

    /** Saves a pending change now and waits (briefly) until it is on disk; used when a screen closes. */
    static void flushPendingSave() {
        if (savePending) {
            ArcaneBeamConfig.save();
        }
        awaitIdle(FLUSH_TIMEOUT_MILLIS);
    }

    /** Registers a JVM shutdown hook that writes any pending or still-failing save before the game exits. */
    static void installShutdownFlush() {
        synchronized (IO_LOCK) {
            if (shutdownHookInstalled) {
                return;
            }
            shutdownHookInstalled = true;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigIo::flushOnShutdown, "ArcaneBeam-config-flush"));
    }

    static void flushOnShutdown() {
        try {
            if (savePending) {
                ArcaneBeamConfig.save();
            }
            awaitIdle(SHUTDOWN_TIMEOUT_MILLIS);
            if (savePending) {
                // The last write failed and is waiting for its retry; try once more now.
                ArcaneBeamConfig.save();
                awaitIdle(SHUTDOWN_TIMEOUT_MILLIS);
            }
        } catch (RuntimeException failure) {
            ArcaneBeam.LOGGER.debug("Arcane Beam config shutdown flush failed", failure);
        }
    }

    /** Waits until no write is queued or running, or the timeout passes. */
    static boolean awaitIdle(long timeoutMillis) {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        synchronized (IO_LOCK) {
            while (pendingBytes != null || inFlight || workerScheduled) {
                long remaining = (deadline - System.nanoTime()) / 1_000_000L;
                if (remaining <= 0) {
                    return false;
                }
                try {
                    IO_LOCK.wait(remaining);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    /** Serialises the live config and queues it for the background writer. No disk access on this thread. */
    static void write() {
        savePending = false;
        if (persistenceBlocked) {
            if (!blockedWarned) {
                blockedWarned = true;
                ArcaneBeam.LOGGER.warn("Not saving Arcane Beam config this session: the existing file could not be read or backed up, "
                        + "so it is left untouched");
            }
            return;
        }
        byte[] bytes;
        try {
            ConfigProfiles.syncActiveProfiles();
            bytes = ConfigParser.GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException failure) {
            ArcaneBeam.LOGGER.error("Unable to serialise the Arcane Beam config; it was not saved", failure);
            return;
        }
        synchronized (IO_LOCK) {
            pendingBytes = bytes;
            if (!workerScheduled) {
                workerScheduled = true;
                if (executor == null) {
                    executor = Executors.newSingleThreadExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "ArcaneBeam-config-io");
                        thread.setDaemon(true);
                        return thread;
                    });
                }
                executor.execute(ConfigIo::drain);
            }
        }
    }

    private static void drain() {
        while (true) {
            byte[] bytes;
            synchronized (IO_LOCK) {
                bytes = pendingBytes;
                pendingBytes = null;
                if (bytes == null) {
                    workerScheduled = false;
                    IO_LOCK.notifyAll();
                    return;
                }
                inFlight = true;
            }
            try {
                persist(bytes);
            } catch (RuntimeException failure) {
                ArcaneBeam.LOGGER.error("Unexpected error while saving the Arcane Beam config", failure);
            } finally {
                synchronized (IO_LOCK) {
                    inFlight = false;
                    IO_LOCK.notifyAll();
                }
            }
        }
    }

    /** Writes {@code bytes} atomically (temp file, fsync, move). Runs on the writer thread. */
    private static void persist(byte[] bytes) {
        Path configPath = configPath();
        if (Arrays.equals(bytes, lastKnownFileBytes)) {
            return;
        }
        Path temporaryPath = null;
        try {
            Path directory = configPath.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            temporaryPath = Files.createTempFile(directory, configPath.getFileName() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporaryPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporaryPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
            lastKnownFileBytes = bytes;
            onWriteSucceeded(configPath);
        } catch (IOException | RuntimeException failure) {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException | RuntimeException cleanupException) {
                    ArcaneBeam.LOGGER.debug("Unable to remove incomplete Arcane Beam config file {}", temporaryPath, cleanupException);
                }
            }
            onWriteFailed(configPath, failure);
        }
    }

    private static void onWriteSucceeded(Path configPath) {
        if (writeFailures > 0) {
            ArcaneBeam.LOGGER.info("Arcane Beam config saved to {} after {} failed attempt(s)", configPath, writeFailures);
        }
        writeFailures = 0;
    }

    /**
     * Re-arms the save so the change is not lost, with a delay that doubles up to a minute and stops after a few
     * automatic attempts; only the first failure of a streak is logged above debug level.
     */
    private static void onWriteFailed(Path configPath, Throwable failure) {
        writeFailures++;
        if (writeFailures == 1) {
            ArcaneBeam.LOGGER.warn("Unable to save the Arcane Beam config to {} ({}); settings stay active in memory and the save is "
                    + "retried with a growing delay", configPath, failure.toString());
        } else {
            ArcaneBeam.LOGGER.debug("Arcane Beam config save to {} failed again (attempt {})", configPath, writeFailures, failure);
        }
        if (writeFailures > MAX_AUTOMATIC_WRITE_RETRIES) {
            return;
        }
        long delay = Math.min(WRITE_RETRY_MAX_NANOS, WRITE_RETRY_BASE_NANOS << Math.min(writeFailures - 1, 5));
        // tickPendingSave fires once SAVE_DEBOUNCE_NANOS have passed since this timestamp.
        lastSaveRequestNanos = System.nanoTime() + delay - SAVE_DEBOUNCE_NANOS;
        savePending = true;
    }

    /** Clears all load/save state; for tests. */
    static void resetForTests() {
        awaitIdle(5_000L);
        synchronized (IO_LOCK) {
            pendingBytes = null;
        }
        savePending = false;
        lastSaveRequestNanos = 0L;
        persistenceBlocked = false;
        blockedWarned = false;
        writeFailures = 0;
        lastKnownFileBytes = null;
        pathOverride = null;
        reader = Files::readAllBytes;
        sleeper = Thread::sleep;
    }

    static int writeFailureCount() {
        return writeFailures;
    }

    /**
     * Keeps the original file as {@code .corrupt-<time>} before it is overwritten. An unreadable file is moved
     * (copied if the move fails); a partly readable one is copied. Returns false when no backup could be made.
     */
    static boolean preserveOriginalConfig(Path configPath, boolean move) {
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".corrupt-" + System.currentTimeMillis());
        try {
            if (move) {
                try {
                    Files.move(configPath, backupPath);
                } catch (IOException | RuntimeException moveException) {
                    Files.copy(configPath, backupPath);
                }
            } else {
                Files.copy(configPath, backupPath);
            }
            ArcaneBeam.LOGGER.warn("Preserved the original Arcane Beam config as {}", backupPath);
            return true;
        } catch (IOException | RuntimeException backupException) {
            ArcaneBeam.LOGGER.error("Unable to preserve Arcane Beam config {}", configPath, backupException);
            return false;
        }
    }
}
