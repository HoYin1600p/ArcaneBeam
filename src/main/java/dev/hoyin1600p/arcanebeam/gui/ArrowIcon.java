package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

/**
 * Small solid arrows built from filled one-pixel columns, because the game font draws the arrow characters as
 * barely visible dots. All sizes are in layout pixels.
 */
final class ArrowIcon {
    enum Direction {
        LEFT,
        RIGHT
    }

    private ArrowIcon() {
    }

    /** Width of a triangle of the given depth. */
    static int width(int depth) {
        return depth;
    }

    /** A solid triangle {@code depth} wide and {@code 2 * depth - 1} tall, centred on ({@code cx}, {@code cy}). */
    static void draw(PoseStack poseStack, int cx, int cy, int depth, Direction direction, int color) {
        int left = cx - depth / 2;
        for (int column = 0; column < depth; column++) {
            // Column 0 is the tip; each column away from it grows by one pixel above and below.
            int half = column;
            int x = direction == Direction.LEFT ? left + column : left + depth - 1 - column;
            GuiComponent.fill(poseStack, x, cy - half, x + 1, cy + half + 1, color);
        }
    }

    /**
     * A swap symbol: two arrows stacked one above the other, the upper pointing right and the lower pointing left,
     * each a 3-pixel shaft with a 5-deep head. The icon is 15 wide and 19 tall, centred on ({@code cx}, {@code cy}).
     */
    static void drawSwap(PoseStack poseStack, int cx, int cy, int color) {
        int halfWidth = 7;
        int depth = 5;
        int upperY = cy - 5;
        int lowerY = cy + 5;
        // Upper arrow: shaft from the left edge up to the head, tip on the right edge.
        GuiComponent.fill(poseStack, cx - halfWidth, upperY - 1, cx + halfWidth - depth + 1, upperY + 2, color);
        arrowHead(poseStack, cx + halfWidth, upperY, depth, Direction.RIGHT, color);
        // Lower arrow: the mirror image.
        GuiComponent.fill(poseStack, cx - halfWidth + depth, lowerY - 1, cx + halfWidth + 1, lowerY + 2, color);
        arrowHead(poseStack, cx - halfWidth, lowerY, depth, Direction.LEFT, color);
    }

    /** A solid head whose tip sits exactly at ({@code tipX}, {@code cy}), one pixel taller per column away from it. */
    private static void arrowHead(PoseStack poseStack, int tipX, int cy, int depth, Direction direction, int color) {
        for (int column = 0; column < depth; column++) {
            int x = direction == Direction.RIGHT ? tipX - column : tipX + column;
            GuiComponent.fill(poseStack, x, cy - column, x + 1, cy + column + 1, color);
        }
    }
}
