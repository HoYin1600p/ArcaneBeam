package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Lays a page out as stacked sections in a two-column grid (wide sections span both columns) and draws them.
 * <p>
 * A page builds its sections once per layout pass, then {@link #layout} assigns every widget a position for the
 * current scroll offset. Widgets are never created here; the screen owns them and only hands them in.
 */
final class SectionLayout {
    static final int SWATCH_WIDTH = 20;
    static final int HEX_WIDTH = 54;
    private static final int SWATCH_GAP = 6;
    private static final int COLOR_CELL_WIDTH = SWATCH_WIDTH + SWATCH_GAP + HEX_WIDTH;
    private static final int COLOR_LABEL_HEIGHT = 12;
    private static final int COLOR_ROW_HEIGHT = COLOR_LABEL_HEIGHT + 28;
    private static final int PAD = 7;
    private static final int TITLE_HEIGHT = 17;
    private static final int COLUMN_GAP = 10;
    private static final int SECTION_GAP = 8;
    private static final int BUTTON_ROW = 23;
    private static final int BOX_ROW = 27;
    private static final int LABEL_COLOR = 0xFFD8D8D8;

    /** One colour slot: a swatch that opens the picker and a hex box. */
    record ColorSlot(String label, EditBox box, int slot, boolean glow, IntSupplier color) {
    }

    private abstract static class Row {
        abstract int height(Font font, int innerWidth);

        abstract void place(SectionLayout out, Font font, int x, int y, int innerWidth);
    }

    /** Builder for one section; rows are stacked top to bottom. */
    final class Section {
        private final String title;
        private final boolean wide;
        private final List<Row> rows = new ArrayList<>();

        private Section(String title, boolean wide) {
            this.title = title;
            this.wide = wide;
        }

        /** A button, slider or other widget filling the full row. */
        Section widget(AbstractWidget widget) {
            rows.add(new Row() {
                @Override
                int height(Font font, int innerWidth) {
                    return BUTTON_ROW;
                }

                @Override
                void place(SectionLayout out, Font font, int x, int y, int innerWidth) {
                    out.put(widget, x, y, innerWidth);
                }
            });
            return this;
        }

        /** A text box with its caption on the left, right-aligned in the row. */
        Section labeled(String label, EditBox box, int boxWidth) {
            rows.add(new Row() {
                @Override
                int height(Font font, int innerWidth) {
                    return BOX_ROW;
                }

                @Override
                void place(SectionLayout out, Font font, int x, int y, int innerWidth) {
                    out.text(label, x + 2, y + 6, LABEL_COLOR);
                    out.put(box, x + innerWidth - boxWidth - 4, y, boxWidth);
                }
            });
            return this;
        }

        /** A button followed by a small text box and a caption, such as a fade style and its tick count. */
        Section buttonAndBox(AbstractWidget button, EditBox box, int boxWidth, String caption) {
            rows.add(new Row() {
                @Override
                int height(Font font, int innerWidth) {
                    return BOX_ROW;
                }

                @Override
                void place(SectionLayout out, Font font, int x, int y, int innerWidth) {
                    int captionWidth = caption.isEmpty() ? 0 : font.width(caption) + 4;
                    int buttonWidth = innerWidth - boxWidth - captionWidth - (caption.isEmpty() ? 6 : 10);
                    out.put(button, x, y, buttonWidth);
                    out.put(box, x + buttonWidth + 6, y, boxWidth);
                    if (!caption.isEmpty()) {
                        out.text(caption, x + buttonWidth + 6 + boxWidth + 5, y + 6, ArcaneBeamGuiTheme.MUTED_TEXT);
                    }
                }
            });
            return this;
        }

        /** Muted text wrapped to the section width. */
        Section hint(String text) {
            rows.add(new Row() {
                @Override
                int height(Font font, int innerWidth) {
                    return wrap(font, text, innerWidth).size() * (font.lineHeight + 1) + 3;
                }

                @Override
                void place(SectionLayout out, Font font, int x, int y, int innerWidth) {
                    int lineY = y;
                    for (FormattedCharSequence line : wrap(font, text, innerWidth)) {
                        out.lines.add(new TextLine(line, x, lineY, ArcaneBeamGuiTheme.MUTED_TEXT));
                        lineY += font.lineHeight + 1;
                    }
                }
            });
            return this;
        }

        /** A grid of colour slots, as many per row as fit. */
        Section colors(List<ColorSlot> slots) {
            rows.add(new Row() {
                private int perRow(int innerWidth) {
                    return Math.max(1, Math.min(4, (innerWidth + 8) / (COLOR_CELL_WIDTH + 8)));
                }

                @Override
                int height(Font font, int innerWidth) {
                    int lines = (slots.size() + perRow(innerWidth) - 1) / perRow(innerWidth);
                    return lines * COLOR_ROW_HEIGHT;
                }

                @Override
                void place(SectionLayout out, Font font, int x, int y, int innerWidth) {
                    int perRow = perRow(innerWidth);
                    int gap = perRow > 1 ? (innerWidth - perRow * COLOR_CELL_WIDTH) / (perRow - 1) : 0;
                    for (int i = 0; i < slots.size(); i++) {
                        ColorSlot slot = slots.get(i);
                        int cellX = x + (i % perRow) * (COLOR_CELL_WIDTH + gap);
                        int cellY = y + (i / perRow) * COLOR_ROW_HEIGHT;
                        out.text(slot.label(), cellX + COLOR_CELL_WIDTH / 2 - font.width(slot.label()) / 2, cellY, ArcaneBeamGuiTheme.MUTED_TEXT);
                        int controlY = cellY + COLOR_LABEL_HEIGHT;
                        out.put(slot.box(), cellX + SWATCH_WIDTH + SWATCH_GAP, controlY, HEX_WIDTH);
                        out.swatches.add(new Swatch(slot, cellX, controlY));
                    }
                }
            });
            return this;
        }
    }

    private record TextLine(FormattedCharSequence text, int x, int y, int color) {
    }

    private record Swatch(ColorSlot slot, int x, int y) {
    }

    private record PlacedSection(String title, int x, int y, int width, int height) {
    }

    private final List<Section> sections = new ArrayList<>();
    private final List<PlacedSection> placed = new ArrayList<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<TextLine> lines = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private int contentHeight;

    void clear() {
        sections.clear();
    }

    Section add(String title, boolean wide) {
        Section section = new Section(title, wide);
        sections.add(section);
        return section;
    }

    /** Total height of the laid-out content, independent of the scroll offset. */
    int contentHeight() {
        return contentHeight;
    }

    /** Every widget placed by the last {@link #layout} pass. */
    List<AbstractWidget> widgets() {
        return widgets;
    }

    /**
     * Assigns positions. {@code scroll} is subtracted from every y, so the same pass serves any scroll offset.
     */
    void layout(Font font, int x, int top, int width, int scroll) {
        placed.clear();
        widgets.clear();
        lines.clear();
        swatches.clear();
        int columnWidth = (width - COLUMN_GAP) / 2;
        int[] columnY = {0, 0};
        for (Section section : sections) {
            int sectionWidth = section.wide ? width : columnWidth;
            int innerWidth = sectionWidth - PAD * 2;
            int height = TITLE_HEIGHT + PAD - 3;
            for (Row row : section.rows) {
                height += row.height(font, innerWidth);
            }
            int sectionY;
            int sectionX;
            if (section.wide) {
                sectionY = Math.max(columnY[0], columnY[1]);
                sectionX = x;
                columnY[0] = sectionY + height + SECTION_GAP;
                columnY[1] = columnY[0];
            } else {
                int column = columnY[0] <= columnY[1] ? 0 : 1;
                sectionY = columnY[column];
                sectionX = x + column * (columnWidth + COLUMN_GAP);
                columnY[column] = sectionY + height + SECTION_GAP;
            }
            int absoluteY = top + sectionY - scroll;
            placed.add(new PlacedSection(section.title, sectionX, absoluteY, sectionWidth, height));
            int rowY = absoluteY + TITLE_HEIGHT;
            for (Row row : section.rows) {
                row.place(this, font, sectionX + PAD, rowY, innerWidth);
                rowY += row.height(font, innerWidth);
            }
        }
        contentHeight = Math.max(columnY[0], columnY[1]);
    }

    private void put(AbstractWidget widget, int x, int y, int width) {
        if (widget != null) {
            widget.x = x;
            widget.y = y;
            widget.setWidth(width);
            widgets.add(widget);
        }
    }

    private void text(String text, int x, int y, int color) {
        lines.add(new TextLine(new TextComponent(text).getVisualOrderText(), x, y, color));
    }

    private static List<FormattedCharSequence> wrap(Font font, String text, int width) {
        return font.split(FormattedText.of(text), width);
    }

    /** The colour slot whose swatch (or, with {@code includeBox}, hex box) contains the point, or null. */
    ColorSlot colorSlotAt(double mouseX, double mouseY, boolean includeBox) {
        for (Swatch swatch : swatches) {
            EditBox box = swatch.slot().box();
            if (!box.visible || !box.active) {
                continue;
            }
            if (inside(mouseX, mouseY, swatch.x(), swatch.y(), SWATCH_WIDTH, 20)) {
                return swatch.slot();
            }
            if (includeBox && inside(mouseX, mouseY, box.x, box.y, HEX_WIDTH, 20)) {
                return swatch.slot();
            }
        }
        return null;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    void render(PoseStack poseStack, Font font, int accent, int selectedSlot, boolean glowSelected) {
        for (PlacedSection section : placed) {
            int x = section.x();
            int y = section.y();
            GuiComponent.fill(poseStack, x, y, x + section.width(), y + section.height(), 0x2605080D);
            GuiComponent.fill(poseStack, x, y, x + 2, y + section.height(), (accent & 0x00FFFFFF) | 0x66000000);
            GuiComponent.drawString(poseStack, font, section.title().toUpperCase(Locale.ROOT), x + PAD, y + 5, accent);
            GuiComponent.fill(poseStack, x + PAD, y + TITLE_HEIGHT - 3, x + section.width() - PAD, y + TITLE_HEIGHT - 2, 0x33F1BF5B);
        }
        for (TextLine line : lines) {
            font.draw(poseStack, line.text(), (float) line.x(), (float) line.y(), line.color());
        }
        for (Swatch swatch : swatches) {
            if (swatch.slot().box().visible) {
                boolean selected = swatch.slot().slot() == selectedSlot && swatch.slot().glow() == glowSelected;
                drawSwatch(poseStack, swatch.x(), swatch.y(), swatch.slot().color().getAsInt(), selected);
            }
        }
    }

    static void drawSwatch(PoseStack poseStack, int x, int y, int color, boolean selected) {
        NineSliceRenderer.draw(poseStack, selected ? NineSliceRenderer.BUTTON_ACTIVE : NineSliceRenderer.COLOR_SLOT, x - 2, y - 2, SWATCH_WIDTH + 4, 24);
        GuiComponent.fill(poseStack, x + 2, y + 2, x + SWATCH_WIDTH - 2, y + 18, 0xFF000000 | color);
    }
}
