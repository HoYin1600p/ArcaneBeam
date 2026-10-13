package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static dev.hoyin1600p.arcanebeam.util.IntMath.clamp;

/**
 * The left column of the settings panel: the title and the grouped page list. The list scrolls vertically when the
 * entries do not fit, which only happens on very small screens.
 */
final class ConfigNav {
    static final int WIDTH = 170;
    private static final int PAD = 14;
    private static final int HEADING_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_PITCH = 24;
    private static final int GROUP_GAP = 6;

    private final Map<ConfigPage, ThemeButton> buttons = new EnumMap<>(ConfigPage.class);
    private final List<Heading> headings = new ArrayList<>();
    private int scroll;
    private int top;
    private int bottom;
    private int contentHeight;

    private record Heading(String text, int y) {
    }

    ConfigNav(UnaryOperator<ThemeButton> register, Consumer<ConfigPage> onSelect) {
        for (ConfigPage page : ConfigPage.values()) {
            ThemeButton button = new ThemeButton(0, 0, WIDTH - PAD * 2, BUTTON_HEIGHT, new TextComponent(page.label), b -> onSelect.accept(page));
            button.setAccent(page.accent);
            buttons.put(page, register.apply(button));
        }
    }

    /** Positions every entry for the current scroll offset; entries outside {@code [top, bottom]} are hidden. */
    void layout(int originX, int top, int bottom, ConfigPage active, boolean visible) {
        this.top = top;
        this.bottom = bottom;
        headings.clear();
        int y = top;
        ConfigPage.Group group = null;
        for (ConfigPage page : ConfigPage.values()) {
            if (page.group != group) {
                group = page.group;
                if (y > top) {
                    y += GROUP_GAP;
                }
                headings.add(new Heading(group.label.toUpperCase(java.util.Locale.ROOT), y));
                y += HEADING_HEIGHT;
            }
            ThemeButton button = buttons.get(page);
            button.x = originX + PAD;
            button.y = y - scroll;
            button.setSelected(page == active);
            boolean inside = button.y >= top && button.y + BUTTON_HEIGHT <= bottom;
            button.visible = visible && inside;
            button.active = visible && inside;
            y += BUTTON_PITCH;
        }
        contentHeight = y - top;
        int max = maxScroll();
        if (scroll > max) {
            scroll = max;
            layout(originX, top, bottom, active, visible);
        }
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (bottom - top));
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < 0 || mouseX >= WIDTH || mouseY < top || mouseY >= bottom || maxScroll() <= 0) {
            return false;
        }
        scroll = clamp(scroll + (delta > 0.0D ? -BUTTON_PITCH : BUTTON_PITCH), 0, maxScroll());
        return true;
    }

    void resetScroll() {
        scroll = 0;
    }

    int scroll() {
        return scroll;
    }

    void render(PoseStack poseStack, Font font) {
        GuiComponent.drawString(poseStack, font, "Arcane Beam", PAD, 16, ArcaneBeamGuiTheme.TEXT);
        GuiComponent.fill(poseStack, PAD, 32, WIDTH - PAD, 33, ArcaneBeamGuiTheme.GOLD_LINE);
        for (Heading heading : headings) {
            int y = heading.y() - scroll;
            if (y >= top && y + HEADING_HEIGHT <= bottom) {
                GuiComponent.drawString(poseStack, font, heading.text(), PAD, y + 4, ArcaneBeamGuiTheme.MUTED_TEXT);
            }
        }
    }
}
