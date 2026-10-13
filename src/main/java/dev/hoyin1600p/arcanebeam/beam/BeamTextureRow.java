package dev.hoyin1600p.arcanebeam.beam;

/**
 * The texture row the Arcane and Rail beam tubes sample along their whole length.
 *
 * <p>{@code loot_beam.png} is 16 rows tall: rows 0 to 2 are fully transparent, rows 3 to 13 ramp the alpha up, and
 * rows 14 and 15 are fully opaque. The tubes used to map that column from tip (v = 0) to origin (v = 1), which
 * stretched the ramp over however long the beam happened to be: a point a few blocks out was nearly opaque on a
 * full-range beam and nearly invisible on a beam that stopped at a wall, so the beam brightened and dimmed as the
 * hit distance changed. Every vertex now samples the middle of the last opaque row instead, so the texture adds no
 * alpha of its own and the beam is exactly the configured opacity at any length. The texture's sideways shading
 * (its u direction) is kept.
 */
final class BeamTextureRow {
    static final int TEXTURE_ROWS = 16;
    static final int OPAQUE_ROW = 15;
    /** The v coordinate of the beam tip (the far end, hit or max range). */
    static final float TIP_V = (OPAQUE_ROW + 0.5F) / TEXTURE_ROWS;
    /** The v coordinate of the beam origin (the hand). */
    static final float ORIGIN_V = TIP_V;

    private BeamTextureRow() {
    }
}
