package dev.hoyin1600p.arcanebeam.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitedRoomStoreTest {
    private static final long DAY = 24L * 60L * 60L * 1000L;

    @TempDir
    Path dir;

    private final AtomicLong now = new AtomicLong(1_000_000_000_000L);

    private VisitedRoomStore store() {
        return new VisitedRoomStore(dir.resolve("sub").resolve("visited.json"), now::get);
    }

    private static UUID id(int n) {
        return new UUID(0L, n);
    }

    @Test
    void roomsAreScopedToTheirVaultAndNegativeCoordinatesWork() {
        VisitedRoomStore store = store();
        store.record(id(1), 3, -2);
        assertTrue(store.contains(id(1), 3, -2));
        assertFalse(store.contains(id(1), -2, 3));
        assertFalse(store.contains(id(2), 3, -2));
    }

    @Test
    void visitsWithoutVaultIdAreIgnored() {
        VisitedRoomStore store = store();
        store.record(null, 1, 1);
        assertFalse(store.contains(null, 1, 1));
        assertFalse(store.isDirty());
    }

    @Test
    void roundTripKeepsVisitsAcrossInstances() {
        VisitedRoomStore first = store();
        first.record(id(1), 5, 6);
        first.record(id(1), -7, 8);
        first.record(id(2), 0, 0);
        first.flushNow();

        VisitedRoomStore second = store();
        assertTrue(second.contains(id(1), 5, 6));
        assertTrue(second.contains(id(1), -7, 8));
        assertTrue(second.contains(id(2), 0, 0));
        assertFalse(second.contains(id(2), 5, 6));
        assertEquals(2, second.vaultCount());
    }

    @Test
    void writesAreDebouncedUntilTheWindowPasses() {
        VisitedRoomStore store = store();
        store.record(id(1), 1, 1);
        assertNotNull(store.snapshotIfDue(false)); // first write is due immediately
        store.record(id(1), 2, 2);
        assertNull(store.snapshotIfDue(false));
        now.addAndGet(VisitedRoomStore.WRITE_DEBOUNCE_MILLIS - 1);
        assertNull(store.snapshotIfDue(false));
        now.addAndGet(1);
        assertNotNull(store.snapshotIfDue(false));
        assertNull(store.snapshotIfDue(false)); // nothing changed since
        store.record(id(1), 3, 3);
        assertNotNull(store.snapshotIfDue(true)); // force ignores the window
    }

    @Test
    void repeatedVisitToTheSameRoomDoesNotMarkDirty() {
        VisitedRoomStore store = store();
        store.record(id(1), 1, 1);
        store.flushNow();
        store.record(id(1), 1, 1);
        assertFalse(store.isDirty());
    }

    @Test
    void corruptFileStartsEmptyAndIsReplacedOnNextWrite() throws IOException {
        Path file = dir.resolve("sub").resolve("visited.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ this is not json");
        VisitedRoomStore store = store();
        assertFalse(store.contains(id(1), 1, 1));
        store.record(id(1), 1, 1);
        store.flushNow();
        assertTrue(store().contains(id(1), 1, 1));
    }

    @Test
    void wrongShapeAndEmptyFilesStartEmpty() throws IOException {
        Path file = dir.resolve("sub").resolve("visited.json");
        Files.createDirectories(file.getParent());
        String[] contents = {"", "[]", "{\"vaults\": 5}", "{\"vaults\": {\"not-a-uuid\": {}}}", "null"};
        for (String content : contents) {
            Files.writeString(file, content);
            assertFalse(store().contains(id(1), 1, 1), content);
        }
    }

    @Test
    void oneBadVaultEntryDoesNotDiscardTheOthers() throws IOException {
        Path file = dir.resolve("sub").resolve("visited.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"version\":1,\"vaults\":{\"" + id(1) + "\":{\"updated\":1,\"rooms\":\"oops\"},\""
                + id(2) + "\":{\"updated\":" + now.get() + ",\"rooms\":[" + VisitedRoomStore.pack(4, 4) + "]}}}");
        VisitedRoomStore store = store();
        assertTrue(store.contains(id(2), 4, 4));
        assertFalse(store.contains(id(1), 4, 4));
    }

    @Test
    void vaultsOlderThanAWeekArePrunedOnWrite() {
        VisitedRoomStore old = store();
        old.record(id(1), 1, 1);
        old.flushNow();
        now.addAndGet(8 * DAY);
        VisitedRoomStore fresh = store();
        fresh.record(id(2), 2, 2);
        fresh.flushNow();
        VisitedRoomStore reloaded = store();
        assertFalse(reloaded.contains(id(1), 1, 1));
        assertTrue(reloaded.contains(id(2), 2, 2));
    }

    @Test
    void onlyTheMostRecentVaultsAreKept() {
        VisitedRoomStore store = store();
        int total = VisitedRoomStore.MAX_VAULTS + 5;
        for (int i = 0; i < total; i++) {
            now.addAndGet(1000);
            store.record(id(i), i, i);
        }
        store.flushNow();
        VisitedRoomStore reloaded = store();
        assertFalse(reloaded.contains(id(0), 0, 0));
        assertFalse(reloaded.contains(id(4), 4, 4));
        assertTrue(reloaded.contains(id(5), 5, 5));
        assertTrue(reloaded.contains(id(total - 1), total - 1, total - 1));
        assertEquals(VisitedRoomStore.MAX_VAULTS, reloaded.vaultCount());
    }

    @Test
    void unwritableTargetDoesNotThrow() throws IOException {
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "a file where a directory is needed");
        VisitedRoomStore store = new VisitedRoomStore(blocker.resolve("visited.json"), now::get);
        store.record(id(1), 1, 1);
        store.flushNow(); // logs a warning, must not throw
        assertTrue(store.contains(id(1), 1, 1));
    }
}
