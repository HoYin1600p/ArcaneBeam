package dev.hoyin1600p.arcanebeam.labyrinth;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the exit of a scanned labyrinth without knowing how the room was mirrored and turned.
 * <p>
 * Each variant has one exit at a known template position. The 8 orientations map that position to up to 8 world
 * columns; the one that holds a walkable cell at the top level of the labyrinth (for the ladder variant, a
 * climbable one) is the real exit. When no orientation matches, for example because the pack changed the room, the
 * walkable cells at the top of the labyrinth are used instead.
 */
public final class EntranceLocator {
    /** How the exit cells were found. */
    public enum Method {
        /** A template position transformed by one of the 8 orientations matched the scan. */
        TRANSFORM,
        /** The room tunnel mouths (all orientations give the same cells). */
        GATES,
        /** The walkable cells at the top of the labyrinth. */
        TOP_CELLS,
        NONE
    }

    /**
     * The exit cells (cell ids of the graph; possibly several) and the method that found them. {@code cells} is where
     * the route ends; {@code shaftTop} is where the exit passage meets the surface (the same cells until
     * {@link ExitMouth} has moved the route end down to the mouth of the passage).
     */
    public record Result(int[] cells, Method method, RoomTransform transform, int[] shaftTop) {
        public Result(int[] cells, Method method, RoomTransform transform) {
            this(cells, method, transform, cells);
        }

        public boolean found() {
            return cells.length > 0;
        }

        /** The same result with the route end moved to {@code mouth}. */
        public Result withMouth(int[] mouth) {
            return new Result(mouth, method, transform, shaftTop);
        }
    }

    private EntranceLocator() {
    }

    public static Result locate(LabyrinthVariant variant, WalkGraph graph) {
        if (variant.entranceKind == LabyrinthVariant.EntranceKind.GATES) {
            return gates(variant, graph);
        }
        int[][] template = variant.entranceCells();
        int bestScore = 0;
        List<Candidate> best = new ArrayList<>();
        for (RoomTransform transform : RoomTransform.all()) {
            for (int[] anchor : template) {
                int[] column = transform.apply(anchor[0], anchor[2], graph.size);
                int cell = cellNear(graph, column[0], column[1], anchor[1]);
                int score = score(variant, graph, cell);
                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                }
                if (score == bestScore && score > 0 && !contains(best, cell)) {
                    best.add(new Candidate(cell, transform));
                }
            }
        }
        // A real exit holds the top level of the labyrinth, which no other column does.
        if (bestScore >= 3 && best.size() == 1) {
            Candidate only = best.get(0);
            return new Result(new int[]{only.cell}, Method.TRANSFORM, only.transform);
        }
        return topCells(variant, graph);
    }

    private static Result gates(LabyrinthVariant variant, WalkGraph graph) {
        List<Integer> cells = new ArrayList<>();
        for (int[] gate : variant.entranceCells()) {
            int cell = cellNear(graph, gate[0], gate[2], gate[1]);
            if (cell >= 0 && !cells.contains(cell)) {
                cells.add(cell);
            }
        }
        return cells.isEmpty() ? new Result(new int[0], Method.NONE, null) : new Result(toArray(cells), Method.GATES, null);
    }

    /** The walkable cells whose feet are at the top level of the labyrinth. */
    static Result topCells(LabyrinthVariant variant, WalkGraph graph) {
        List<Integer> cells = new ArrayList<>();
        for (int c = 0; c < graph.count; c++) {
            if (graph.feetY(c) >= variant.topFeetY - 1) {
                cells.add(c);
            }
        }
        return cells.isEmpty() ? new Result(new int[0], Method.NONE, null) : new Result(toArray(cells), Method.TOP_CELLS, null);
    }

    private static int score(LabyrinthVariant variant, WalkGraph graph, int cell) {
        if (cell < 0) {
            return 0;
        }
        int score = 1;
        if (graph.feetY(cell) >= variant.topFeetY - 1) {
            score += 2;
        }
        if (variant.entranceKind == LabyrinthVariant.EntranceKind.LADDER && graph.climbable(cell)) {
            score += 1;
        }
        return score;
    }

    /** The cell in column (x, z) whose feet are within one block of {@code feetY}, preferring the closest, or -1. */
    private static int cellNear(WalkGraph graph, int x, int z, int feetY) {
        return graph.findCell(x, z, feetY * 16, 16);
    }

    private static boolean contains(List<Candidate> list, int cell) {
        for (Candidate candidate : list) {
            if (candidate.cell == cell) {
                return true;
            }
        }
        return false;
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private record Candidate(int cell, RoomTransform transform) {
    }
}
