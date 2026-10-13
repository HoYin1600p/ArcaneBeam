package dev.hoyin1600p.arcanebeam.labyrinth;

/**
 * Everything computed from one scan of a labyrinth: the walkable cells, the exit and the route field. Pure data, built
 * off the client thread and then only read.
 */
public final class LabyrinthModel {
    public final LabyrinthVariant variant;
    public final WalkGraph graph;
    public final EntranceLocator.Result exit;
    /** Cells of the exit passage (shaft or stair up to the surface); no route is shown from them. */
    private final boolean[] passage;
    public final RouteField field;
    /** Milliseconds the pure part of the build took. */
    public final long buildMillis;

    private LabyrinthModel(LabyrinthVariant variant, WalkGraph graph, EntranceLocator.Result exit, boolean[] passage, RouteField field,
            long buildMillis) {
        this.variant = variant;
        this.graph = graph;
        this.exit = exit;
        this.passage = passage;
        this.field = field;
        this.buildMillis = buildMillis;
    }

    /**
     * Builds the model from sampled blocks indexed {@code (y * 47 + z) * 47 + x} with {@code variant.scanHeight()}
     * layers.
     */
    public static LabyrinthModel build(int[] codes, LabyrinthVariant variant, int unknownBlocks) {
        long start = System.nanoTime();
        WalkGraph graph = WalkScanner.build(codes, RoomTransform.ROOM, variant.scanHeight(), unknownBlocks, variant.topFeetY);
        EntranceLocator.Result exit = EntranceLocator.locate(variant, graph);
        boolean[] passage = new boolean[graph.count];
        if (exit.method() == EntranceLocator.Method.TRANSFORM) {
            // The route ends at the mouth of the one wide passage on the labyrinth floor, not up inside it, and nothing
            // is shown while the player is in that passage.
            FloorLayers layers = FloorLayers.compute(graph);
            exit = exit.withMouth(ExitMouth.mouth(graph, layers, exit.cells()));
            passage = ExitMouth.passage(graph, layers, exit.shaftTop(), exit.cells());
        }
        RouteField field = RouteField.build(graph, exit.cells(), passage);
        return new LabyrinthModel(variant, graph, exit, passage, field, (System.nanoTime() - start) / 1_000_000L);
    }

    /** True for a cell of the exit passage. */
    public boolean inPassage(int cell) {
        return cell >= 0 && cell < passage.length && passage[cell];
    }
}
