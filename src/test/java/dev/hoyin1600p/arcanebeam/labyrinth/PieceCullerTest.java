package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieceCullerTest {
    /** A straight line of {@code blocks} blocks along +x at height 0, from the origin. */
    private static LinePieces line(float blocks) {
        return LinePieces.fromPoints(new float[]{0, blocks}, new float[]{0, 0}, new float[]{0, 0}, 0, 0, 0);
    }

    @Test
    void linesAreCutIntoPiecesOfAtMostFourBlocks() {
        LinePieces pieces = line(10.0F);
        assertEquals(3, pieces.count);
        double total = 0;
        for (int i = 0; i < pieces.count; i++) {
            assertTrue(pieces.length(i) <= LinePieces.MAX_LENGTH + 1e-9);
            total += pieces.length(i);
            if (i > 0) {
                assertEquals(pieces.endX(i - 1), pieces.startX(i), 1e-9, "pieces join end to start");
            }
        }
        assertEquals(10.0, total, 1e-6);
        assertEquals(0.0F, pieces.startLength(0));
        assertEquals(10.0F / 3.0F, pieces.startLength(1), 1e-4F);
    }

    @Test
    void aShortLineIsOnePiece() {
        assertEquals(1, line(3.0F).count);
        assertEquals(1, line(4.0F).count);
        assertEquals(2, line(4.01F).count);
    }

    @Test
    void worldOffsetsAreApplied() {
        LinePieces pieces = LinePieces.fromPoints(new float[]{1, 2}, new float[]{1, 1}, new float[]{3, 3}, 100, 9, -50);
        assertEquals(101.0, pieces.startX(0), 1e-9);
        assertEquals(10.0, pieces.startY(0), 1e-9);
        assertEquals(-47.0, pieces.startZ(0), 1e-9);
    }

    @Test
    void checksAtMostTheBudgetPerCall() {
        LinePieces pieces = line(400.0F);
        PieceCuller culler = new PieceCuller(pieces.count);
        int[] calls = {0};
        for (int tick = 0; tick < 5; tick++) {
            int checks = culler.refresh(tick, pieces, 0, 1, 0, 1000, 10, 1000, i -> {
                calls[0]++;
                return true;
            });
            assertTrue(checks <= 10);
        }
        assertEquals(50, calls[0]);
    }

    @Test
    void everyPieceInRangeIsCheckedWithinOneLapOfBudgets() {
        LinePieces pieces = line(200.0F); // 50 pieces
        PieceCuller culler = new PieceCuller(pieces.count);
        for (int tick = 0; tick < 5; tick++) {
            culler.refresh(tick, pieces, 0, 1, 0, 1000, 10, 1000, i -> true);
        }
        assertEquals(0, culler.unknownCount());
        assertEquals(50, culler.visibleCount());
    }

    @Test
    void piecesOutOfRangeCostNothingAndAreNotVisible() {
        LinePieces pieces = line(400.0F);
        PieceCuller culler = new PieceCuller(pieces.count);
        int[] calls = {0};
        // Eye at x = 0, range 20: only the first pieces are in range.
        for (int tick = 0; tick < 20; tick++) {
            culler.refresh(tick, pieces, 0, 1, 0, 20, 24, 512, i -> {
                calls[0]++;
                return true;
            });
        }
        int inRange = 0;
        for (int i = 0; i < pieces.count; i++) {
            if (culler.visible(i)) {
                inRange++;
                assertTrue(pieces.midX(i) <= 24.0 + 1e-6);
            }
        }
        assertTrue(inRange >= 5 && inRange <= 7, "got " + inRange);
        assertTrue(calls[0] < 100, "no rays for the distant pieces: " + calls[0]);
    }

    @Test
    void hiddenPiecesStayHiddenUntilTheRecheckInterval() {
        LinePieces pieces = line(8.0F); // 2 pieces
        PieceCuller culler = new PieceCuller(pieces.count);
        boolean[] open = {false};
        culler.refresh(0, pieces, 0, 1, 0, 100, 24, 512, i -> open[0]);
        assertFalse(culler.visible(0));
        open[0] = true;
        culler.refresh(PieceCuller.RECHECK_TICKS - 1, pieces, 0, 1, 0, 100, 24, 512, i -> open[0]);
        assertFalse(culler.visible(0), "answers are cached");
        culler.refresh(PieceCuller.RECHECK_TICKS, pieces, 0, 1, 0, 100, 24, 512, i -> open[0]);
        assertTrue(culler.visible(0));
    }

    @Test
    void theScanLimitBoundsTheWorkPerCall() {
        LinePieces pieces = line(4000.0F); // 1000 pieces, all far away
        PieceCuller culler = new PieceCuller(pieces.count);
        int checks = culler.refresh(0, pieces, -5000, 1, 0, 10, 24, 100, i -> {
            throw new AssertionError("out of range pieces are never checked");
        });
        assertEquals(0, checks);
    }

    @Test
    void aRebuiltPieceSetKeepsTheAnswersOfPiecesThatDidNotMove() {
        LinePieces pieces = line(8.0F); // 2 pieces
        PieceCuller old = new PieceCuller(pieces.count);
        old.refresh(5, pieces, 0, 1, 0, 100, 24, 512, i -> i == 0);
        assertTrue(old.visible(0));
        assertFalse(old.visible(1));
        PieceCuller fresh = new PieceCuller(pieces.count);
        fresh.adopt(0, old, 0);
        fresh.adopt(1, old, 1);
        assertTrue(fresh.visible(0));
        assertFalse(fresh.visible(1));
        // The adopted check time still drives the re-check: nothing is re-asked before the interval ends.
        int[] asked = {0};
        fresh.refresh(5 + PieceCuller.RECHECK_TICKS - 1, pieces, 0, 1, 0, 100, 24, 512, i -> {
            asked[0]++;
            return true;
        });
        assertEquals(0, asked[0]);
    }

    @Test
    void resetForgetsEverything() {
        LinePieces pieces = line(8.0F);
        PieceCuller culler = new PieceCuller(pieces.count);
        culler.refresh(0, pieces, 0, 1, 0, 100, 24, 512, i -> true);
        assertEquals(2, culler.visibleCount());
        culler.reset();
        assertEquals(0, culler.visibleCount());
        assertEquals(2, culler.unknownCount());
    }
}
