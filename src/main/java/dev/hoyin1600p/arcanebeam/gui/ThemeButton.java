package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** The themed nine-slice button used throughout the settings screen. */
final class ThemeButton extends Button {
    private static final int ARROW_DEPTH = 4;

    private boolean selected;
    private ArrowIcon.Direction arrow;
    private int accent = ArcaneBeamGuiTheme.GOLD;

    ThemeButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress);
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Draws a small arrow beside the label: before it when pointing left, after it when pointing right. Null removes it.
     */
    void setArrow(ArrowIcon.Direction arrow) {
        this.arrow = arrow;
    }

    void setAccent(int accent) {
        this.accent = accent;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        NineSliceRenderer.Region region = selected ? NineSliceRenderer.BUTTON_ACTIVE : NineSliceRenderer.BUTTON;
        NineSliceRenderer.draw(poseStack, region, this.x, this.y, this.width, this.height);
        int shade = selected ? 0x661E1206 : this.isHoveredOrFocused() ? 0x5522333A : 0x4410141A;
        fill(poseStack, this.x + 3, this.y + 3, this.x + this.width - 3, this.y + this.height - 3, shade);
        if (selected || this.isHoveredOrFocused()) {
            fill(poseStack, this.x + 6, this.y + this.height - 4, this.x + this.width - 6, this.y + this.height - 3, (accent & 0x00FFFFFF) | 0xAA000000);
        }
        int color = this.active ? (selected ? accent : ArcaneBeamGuiTheme.TEXT) : 0xFF687780;
        Font font = Minecraft.getInstance().font;
        int arrowSpace = arrow == null ? 0 : ARROW_DEPTH + 4;
        String text = SettingSlider.fitText(font, this.getMessage().getString(), this.width - 8 - arrowSpace);
        int textWidth = font.width(text);
        int startX = this.x + (this.width - textWidth - arrowSpace) / 2;
        int textX = arrow == ArrowIcon.Direction.LEFT ? startX + arrowSpace : startX;
        font.draw(poseStack, text, (float) textX, (float) (this.y + (this.height - 8) / 2), color);
        if (arrow != null) {
            int arrowX = arrow == ArrowIcon.Direction.LEFT ? startX + ARROW_DEPTH / 2 : startX + textWidth + 4 + ARROW_DEPTH / 2;
            ArrowIcon.draw(poseStack, arrowX, this.y + this.height / 2, ARROW_DEPTH, arrow, this.active ? ArcaneBeamGuiTheme.GOLD : 0xFF687780);
        }
    }
}
