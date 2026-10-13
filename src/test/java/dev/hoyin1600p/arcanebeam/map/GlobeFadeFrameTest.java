package dev.hoyin1600p.arcanebeam.map;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobeFadeFrameTest {
    private static final class FakeGl implements GlobeFadeFrame.Gl {
        final List<String> calls = new ArrayList<>();
        float[] color = {0.9F, 0.8F, 0.7F, 0.5F};

        @Override
        public void captureAndEnableAlphaBlend(float[] out) {
            calls.add("capture");
            System.arraycopy(color, 0, out, 0, 4);
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            calls.add("color");
            color = new float[] {r, g, b, a};
        }

        @Override
        public void restoreBlend() {
            calls.add("restore");
        }
    }

    @Test
    void nothingTouchesRenderStateUntilBegun() {
        FakeGl gl = new FakeGl();
        GlobeFadeFrame frame = new GlobeFadeFrame(gl);
        frame.setFactor(0.1F);
        frame.end();
        assertTrue(gl.calls.isEmpty());
        assertFalse(frame.isActive());
    }

    @Test
    void roomAlphaIsRelativeToTheCapturedMapAlpha() {
        FakeGl gl = new FakeGl();
        GlobeFadeFrame frame = new GlobeFadeFrame(gl);
        frame.begin();
        frame.setFactor(0.4F);
        assertEquals(0.5F * 0.4F, gl.color[3], 1e-6F);
        assertEquals(0.9F, gl.color[0]);
        frame.setFactor(1.0F);
        assertEquals(0.5F, gl.color[3], 1e-6F);
    }

    @Test
    void endRestoresColourAndBlendExactlyOnce() {
        FakeGl gl = new FakeGl();
        GlobeFadeFrame frame = new GlobeFadeFrame(gl);
        frame.begin();
        frame.setFactor(0.15F);
        frame.end();
        assertEquals(0.5F, gl.color[3], 1e-6F);
        assertEquals(List.of("capture", "color", "color", "restore"), gl.calls);
        frame.end(); // the always-run frame cleanup after the normal end
        assertEquals(4, gl.calls.size());
    }

    @Test
    void cleanupAfterAMissedEndStillRestores() {
        FakeGl gl = new FakeGl();
        GlobeFadeFrame frame = new GlobeFadeFrame(gl);
        frame.begin();
        frame.setFactor(0.15F);
        // the getTunnels hook never ran; the RETURN cleanup calls end()
        frame.end();
        assertEquals(0.5F, gl.color[3], 1e-6F);
        assertEquals("restore", gl.calls.get(gl.calls.size() - 1));
        assertFalse(frame.isActive());
    }

    @Test
    void beginningTwiceRestoresTheFirstPassBeforeCapturing() {
        FakeGl gl = new FakeGl();
        GlobeFadeFrame frame = new GlobeFadeFrame(gl);
        frame.begin();
        frame.setFactor(0.1F);
        frame.begin();
        assertEquals(0.5F, gl.color[3], 1e-6F, "second capture must see the restored colour, not the faded one");
    }
}
