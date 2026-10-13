package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.function.Consumer;

/** Cleanup after a labyrinth draw threw, kept free of render classes so it can be unit tested. */
final class FailedDraw {
    private FailedDraw() {
    }

    /**
     * Ends both batches after a failed draw (ending a batch that is not open does nothing), so the shared buffer source
     * is not left mid-batch for vanilla. A second failure here is dropped so the guard reports the first one.
     */
    static <T> void closeBatches(Consumer<T> endBatch, T lineType, T routeType) {
        try {
            endBatch.accept(lineType);
            if (routeType != lineType) {
                endBatch.accept(routeType);
            }
        } catch (RuntimeException ignored) {
            // The first exception is the one worth reporting.
        }
    }
}
