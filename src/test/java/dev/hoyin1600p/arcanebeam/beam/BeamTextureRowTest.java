package dev.hoyin1600p.arcanebeam.beam;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The beam's brightness must not depend on its length: the tube vertices store a fixed texture v at the tip and at
 * the origin, so the texture alpha at a point is the GPU's interpolation between the two, sampled with nearest
 * filtering. These tests sample the real texture the way the GPU does.
 */
class BeamTextureRowTest {
    private static final String TEXTURE = "/assets/arcanebeam/textures/entity/loot_beam.png";
    private static final float[] BEAM_LENGTHS = {0.15F, 0.5F, 1.0F, 2.75F, 4.0F, 7.3F, 16.0F, 32.0F, 64.0F, 128.0F};
    private static BufferedImage texture;

    @BeforeAll
    static void loadTexture() throws IOException {
        try (InputStream stream = BeamTextureRowTest.class.getResourceAsStream(TEXTURE)) {
            assertNotNull(stream, TEXTURE);
            texture = ImageIO.read(stream);
        }
        assertEquals(BeamTextureRow.TEXTURE_ROWS, texture.getHeight());
    }

    @Test
    void tipAndOriginSampleTheSameRow() {
        assertEquals(BeamTextureRow.TIP_V, BeamTextureRow.ORIGIN_V);
    }

    @Test
    void textureAlphaIsFullyOpaqueEverywhereAlongBeamsOfAnyLength() {
        for (float length : BEAM_LENGTHS) {
            for (float distance = 0.0F; distance <= length; distance += length / 64.0F) {
                float fromOrigin = distance / length;
                // QUADS interpolate v linearly between the tip vertex and the origin vertex.
                float v = BeamTextureRow.ORIGIN_V + (BeamTextureRow.TIP_V - BeamTextureRow.ORIGIN_V) * fromOrigin;
                for (int column = 0; column < texture.getWidth(); column++) {
                    assertEquals(255, alphaAt(column, v), "length " + length + " distance " + distance + " column " + column);
                }
            }
        }
    }

    @Test
    void alphaAtAFixedDistanceIsTheSameWhetherTheBeamHitsCloseOrRunsToMaxRange() {
        float[] distances = {0.5F, 1.0F, 2.0F, 3.0F};
        for (float distance : distances) {
            int shortHit = alphaAlong(distance, 4.0F);
            int maxRange = alphaAlong(distance, 16.0F);
            assertEquals(maxRange, shortHit, "distance " + distance);
        }
    }

    @Test
    void theOldTipToOriginMappingDependedOnLength() {
        // Documents the bug this fixes: v from 0 at the tip to 1 at the origin stretched the alpha ramp over the beam.
        float distance = 3.0F;
        int shortHit = oldAlphaAlong(distance, 4.0F);
        int maxRange = oldAlphaAlong(distance, 16.0F);
        assertTrue(maxRange - shortHit > 150, "old mapping: short hit " + shortHit + ", max range " + maxRange);
    }

    @Test
    void sampledRowStaysInsideTheOpaqueRowWithFloatError() {
        int row = (int) Math.floor(BeamTextureRow.TIP_V * BeamTextureRow.TEXTURE_ROWS);
        assertEquals(BeamTextureRow.OPAQUE_ROW, row);
        float centre = BeamTextureRow.TIP_V * BeamTextureRow.TEXTURE_ROWS - row;
        assertTrue(centre > 0.25F && centre < 0.75F, "v should sit near the middle of the row, not on an edge");
    }

    private static int alphaAlong(float distanceFromOrigin, float length) {
        float fromOrigin = distanceFromOrigin / length;
        float v = BeamTextureRow.ORIGIN_V + (BeamTextureRow.TIP_V - BeamTextureRow.ORIGIN_V) * fromOrigin;
        return alphaAt(texture.getWidth() / 2, v);
    }

    private static int oldAlphaAlong(float distanceFromOrigin, float length) {
        float fromOrigin = distanceFromOrigin / length;
        float v = 1.0F + (0.0F - 1.0F) * fromOrigin;
        return alphaAt(texture.getWidth() / 2, v);
    }

    /** Nearest-filter sample, as the beam render types use (blur off, no mipmaps). */
    private static int alphaAt(int column, float v) {
        int row = Math.min(texture.getHeight() - 1, Math.max(0, (int) Math.floor(v * texture.getHeight())));
        return (texture.getRGB(column, row) >>> 24) & 0xFF;
    }
}
