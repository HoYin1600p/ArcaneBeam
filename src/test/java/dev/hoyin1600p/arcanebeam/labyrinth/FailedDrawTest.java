package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FailedDrawTest {
    @Test
    void endsBothBatchesWhenTheTypesDiffer() {
        List<String> ended = new ArrayList<>();
        FailedDraw.closeBatches(ended::add, "lines", "route");
        assertEquals(List.of("lines", "route"), ended);
    }

    @Test
    void endsASharedBatchOnce() {
        List<String> ended = new ArrayList<>();
        String shared = "lit";
        FailedDraw.closeBatches(ended::add, shared, shared);
        assertEquals(List.of("lit"), ended);
    }

    @Test
    void aFailureWhileClosingIsSwallowed() {
        assertDoesNotThrow(() -> FailedDraw.closeBatches(type -> {
            throw new IllegalStateException("Not building!");
        }, "lines", "route"));
    }
}
