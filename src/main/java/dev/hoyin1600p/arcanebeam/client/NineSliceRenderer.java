package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.GuiUtils;

final class NineSliceRenderer {
    static final ResourceLocation ATLAS = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/gui/config_atlas.png");

    static final Region GLASS_PANEL = new Region(0, 0, 32, 32, 8);
    static final Region IRON_PANEL = new Region(32, 0, 32, 32, 8);
    static final Region GOLD_DRAWER = new Region(64, 0, 32, 32, 8);
    static final Region BUTTON = new Region(96, 0, 24, 24, 6);
    static final Region BUTTON_ACTIVE = new Region(120, 0, 24, 24, 6);
    static final Region COLOR_SLOT = new Region(144, 0, 20, 20, 4);
    static final Region INSET = new Region(164, 0, 24, 24, 6);

    private NineSliceRenderer() {
    }

    static void draw(PoseStack poseStack, Region region, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int safeWidth = Math.max(width, region.border * 2);
        int safeHeight = Math.max(height, region.border * 2);
        GuiUtils.drawContinuousTexturedBox(
                poseStack,
                ATLAS,
                x,
                y,
                region.u,
                region.v,
                safeWidth,
                safeHeight,
                region.width,
                region.height,
                region.border,
                0.0F
        );
    }

    static final class Region {
        private final int u;
        private final int v;
        private final int width;
        private final int height;
        private final int border;

        private Region(int u, int v, int width, int height, int border) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.border = border;
        }
    }
}
