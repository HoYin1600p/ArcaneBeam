package dev.hoyin1600p.arcanebeam.labyrinth;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** The key that shows the labyrinth route while it is held, when the visibility setting is "Hold key". */
public final class LabyrinthKeys {
    /** Rebindable in Controls; Tab by default. */
    public static final KeyMapping SHOW_LINES = new KeyMapping(
            "key.arcanebeam.labyrinth_lines",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            "key.categories.arcanebeam"
    );

    private LabyrinthKeys() {
    }

    static boolean held() {
        return SHOW_LINES.isDown();
    }
}
