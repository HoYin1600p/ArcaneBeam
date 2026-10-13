package dev.hoyin1600p.arcanebeam.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Remembers which map regions the local player has stood in, per vault, across relogs.
 *
 * <p>Vault never re-tags a room that was revealed before it was entered, so the client has to remember
 * the player's own visits itself. The data is small JSON under the config folder, loaded lazily on first use,
 * written atomically and debounced, and bounded by age and count. A missing or corrupt file starts empty.
 * Used from the client main thread only; the disk write itself runs on a background thread via
 * {@link #flushIfDue(boolean)}.
 */
final class VisitedRoomStore {
    static final long WRITE_DEBOUNCE_MILLIS = 5_000L;
    static final long MAX_AGE_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    static final int MAX_VAULTS = 50;
    private static final int FORMAT_VERSION = 1;
    private static final Logger LOGGER = LogManager.getLogger("arcanebeam");
    private static final Gson GSON = new GsonBuilder().create();

    private final Path file;
    private final LongSupplier clock;
    private final Map<UUID, Entry> vaults = new HashMap<>();
    private boolean loaded;
    private boolean dirty;
    private long lastWriteMillis;

    VisitedRoomStore(Path file, LongSupplier clock) {
        this.file = file;
        this.clock = clock;
    }

    private static final class Entry {
        final Set<Long> rooms = new HashSet<>();
        long updatedMillis;
    }

    void record(UUID vaultId, int regionX, int regionZ) {
        if (vaultId == null) {
            return;
        }
        ensureLoaded();
        Entry entry = vaults.computeIfAbsent(vaultId, id -> new Entry());
        if (entry.rooms.add(pack(regionX, regionZ))) {
            entry.updatedMillis = clock.getAsLong();
            dirty = true;
        }
    }

    boolean contains(UUID vaultId, int regionX, int regionZ) {
        if (vaultId == null) {
            return false;
        }
        ensureLoaded();
        Entry entry = vaults.get(vaultId);
        return entry != null && entry.rooms.contains(pack(regionX, regionZ));
    }

    boolean isDirty() {
        return dirty;
    }

    int vaultCount() {
        return vaults.size();
    }

    /**
     * Writes the file when something changed and the debounce window has passed (or {@code force}).
     * The JSON snapshot is built here; the caller may hand {@link Snapshot#write()} to another thread.
     */
    Snapshot snapshotIfDue(boolean force) {
        if (!dirty) {
            return null;
        }
        long now = clock.getAsLong();
        if (!force && now - lastWriteMillis < WRITE_DEBOUNCE_MILLIS) {
            return null;
        }
        prune(now);
        dirty = false;
        lastWriteMillis = now;
        return new Snapshot(file, GSON.toJson(toJson()));
    }

    /** Synchronous variant for tests and shutdown paths. */
    void flushNow() {
        Snapshot snapshot = snapshotIfDue(true);
        if (snapshot != null) {
            snapshot.write();
        }
    }

    record Snapshot(Path target, String json) {
        boolean write() {
            try {
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(temp, json, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not save visited map rooms to {}: {}", target, e.toString());
                return false;
            }
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            JsonObject vaultObject = root.getAsJsonObject().getAsJsonObject("vaults");
            if (vaultObject == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> vault : vaultObject.entrySet()) {
                try {
                    UUID id = UUID.fromString(vault.getKey());
                    JsonObject data = vault.getValue().getAsJsonObject();
                    Entry entry = new Entry();
                    entry.updatedMillis = data.get("updated").getAsLong();
                    for (JsonElement room : data.getAsJsonArray("rooms")) {
                        entry.rooms.add(room.getAsLong());
                    }
                    vaults.put(id, entry);
                } catch (RuntimeException skipped) {
                    // one bad vault entry must not discard the rest
                }
            }
        } catch (IOException | RuntimeException e) {
            vaults.clear();
            LOGGER.warn("Ignoring unreadable visited map rooms file {}: {}", file, e.toString());
        }
    }

    private void prune(long now) {
        vaults.values().removeIf(entry -> now - entry.updatedMillis > MAX_AGE_MILLIS);
        if (vaults.size() > MAX_VAULTS) {
            List<Map.Entry<UUID, Entry>> newestFirst = new ArrayList<>(vaults.entrySet());
            newestFirst.sort((a, b) -> Long.compare(b.getValue().updatedMillis, a.getValue().updatedMillis));
            for (int i = MAX_VAULTS; i < newestFirst.size(); i++) {
                vaults.remove(newestFirst.get(i).getKey());
            }
        }
    }

    private JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        JsonObject vaultObject = new JsonObject();
        for (Map.Entry<UUID, Entry> vault : vaults.entrySet()) {
            JsonObject data = new JsonObject();
            data.addProperty("updated", vault.getValue().updatedMillis);
            JsonArray rooms = new JsonArray();
            vault.getValue().rooms.stream().sorted().forEach(rooms::add);
            data.add("rooms", rooms);
            vaultObject.add(vault.getKey().toString(), data);
        }
        root.add("vaults", vaultObject);
        return root;
    }

    static long pack(int regionX, int regionZ) {
        return ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);
    }
}
