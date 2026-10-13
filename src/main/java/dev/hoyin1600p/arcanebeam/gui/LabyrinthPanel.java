package dev.hoyin1600p.arcanebeam.gui;

import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * The "Labyrinth" page of the settings screen: its widgets, number parsing and colour slots. The screen owns the
 * layout, the colour picker and the mouse-wheel dispatch; this class only creates and updates the controls.
 */
final class LabyrinthPanel {
    /** Mouse-wheel step on a number field: {@code coarse} is the Shift step. */
    @FunctionalInterface
    interface Wheel {
        void scroll(boolean increase, boolean coarse);
    }

    static final int ROUTE_COLOR_SLOT = 0;
    private static final String HEX_PATTERN = "#?[0-9a-fA-F]{0,6}";
    private static final String DECIMAL_PATTERN = "[0-9]{0,1}(\\.[0-9]{0,2})?";

    static final String HINT = "Off by default. Inside the Living, Gilded and Ornate village labyrinths and the Wolds labyrinth room it "
            + "scans the maze around you and draws one line from you to the exit, down the middle of the halls and stairs. It "
            + "appears once you are inside the maze (not in the exit passage) and needs a render distance of 4 chunks or more "
            + "so the whole room is loaded.";
    static final String KEY_HINT = "Hold key mode uses the \"Show labyrinth route\" key (Tab by default; change it under Controls).";

    private final Font font;
    private final UnaryOperator<AbstractWidget> addContent;
    private final Runnable refreshScreen;

    ThemeButton enabledButton;
    ThemeButton visibilityButton;
    ThemeButton fullbrightButton;
    ThemeButton chevronsButton;
    final List<EditBox> colorBoxes = new ArrayList<>();
    EditBox opacityBox;
    EditBox widthBox;
    EditBox thicknessBox;
    EditBox elevationBox;
    EditBox distanceBox;
    private boolean updating;

    LabyrinthPanel(Font font, UnaryOperator<AbstractWidget> addContent, Runnable refreshScreen) {
        this.font = font;
        this.addContent = addContent;
        this.refreshScreen = refreshScreen;
    }

    private static LabyrinthSettings settings() {
        return ArcaneBeamConfig.INSTANCE.labyrinth;
    }

    /** Creates every widget; {@code wheel} registers a number box for mouse-wheel nudging. */
    void create(BiConsumer<EditBox, Wheel> wheel) {
        colorBoxes.clear();
        enabledButton = button(() -> settings().enabled = !settings().enabled);
        visibilityButton = button(() -> settings().visibility = settings().visibilityMode().next().id);
        fullbrightButton = button(() -> settings().fullbright = !settings().fullbright);
        chevronsButton = button(() -> settings().chevrons = !settings().chevrons);
        colorBoxes.add(colorBox("Route Color", ROUTE_COLOR_SLOT));
        opacityBox = decimalBox("Opacity", this::setOpacity);
        widthBox = decimalBox("Width", this::setWidth);
        thicknessBox = decimalBox("Thickness", this::setThickness);
        elevationBox = decimalBox("Elevation", this::setElevation);
        distanceBox = integerBox("Max Distance");
        wheel.accept(opacityBox, (up, coarse) -> {
            setOpacity(settings().opacity + step(up, coarse));
            refresh();
        });
        wheel.accept(widthBox, (up, coarse) -> {
            setWidth(settings().width + step(up, coarse));
            refresh();
        });
        wheel.accept(thicknessBox, (up, coarse) -> {
            setThickness(settings().thickness + step(up, coarse));
            refresh();
        });
        wheel.accept(elevationBox, (up, coarse) -> {
            setElevation(settings().elevation + step(up, coarse));
            refresh();
        });
        wheel.accept(distanceBox, (up, coarse) -> {
            setDistance(settings().maxDistance + (up ? 1 : -1) * (coarse ? 8 : 1));
            refresh();
        });
    }

    /** The colour slot for the picker: the route. */
    List<SectionLayout.ColorSlot> colorSlots() {
        List<SectionLayout.ColorSlot> slots = new ArrayList<>();
        slots.add(new SectionLayout.ColorSlot("Route Color", colorBoxes.get(ROUTE_COLOR_SLOT), ROUTE_COLOR_SLOT, false, () -> color(ROUTE_COLOR_SLOT)));
        return slots;
    }

    void layout(SectionLayout sections, int boxWidth) {
        sections.add("Setup", false).widget(enabledButton).widget(visibilityButton).widget(chevronsButton)
                .hint("One line from you to the exit, down the middle of the halls.");
        sections.add("Look", false).widget(fullbrightButton).labeled("Opacity", opacityBox, boxWidth).labeled("Width", widthBox, boxWidth)
                .labeled("Thickness", thicknessBox, boxWidth).labeled("Elevation", elevationBox, boxWidth)
                .labeled("Max Distance", distanceBox, boxWidth);
        sections.add("Colors", true).colors(colorSlots());
        sections.add("About", true).hint(HINT).hint(KEY_HINT);
    }

    int color(int slot) {
        return settings().routeColor;
    }

    void setColor(int slot, int color) {
        settings().routeColor = color & 0xFFFFFF;
    }

    /** Re-reads every value into its widget. */
    void refresh() {
        if (enabledButton == null) {
            return;
        }
        LabyrinthSettings s = settings();
        enabledButton.setMessage(new TextComponent("Labyrinth Breadcrumbs: " + (s.enabled ? "On" : "Off")));
        visibilityButton.setMessage(new TextComponent("Show Route: " + s.visibilityMode().label));
        fullbrightButton.setMessage(new TextComponent("Fullbright Route: " + (s.fullbright ? "On" : "Off")));
        chevronsButton.setMessage(new TextComponent("Flow Chevrons: " + (s.chevrons ? "On" : "Off")));
        updating = true;
        try {
            for (int slot = 0; slot < colorBoxes.size(); slot++) {
                String text = String.format(Locale.ROOT, "#%06X", color(slot) & 0xFFFFFF);
                if (!text.equals(colorBoxes.get(slot).getValue())) {
                    colorBoxes.get(slot).setValue(text);
                }
            }
            opacityBox.setValue(hundredths(s.opacity));
            widthBox.setValue(hundredths(s.width));
            thicknessBox.setValue(hundredths(s.thickness));
            elevationBox.setValue(hundredths(s.elevation));
            distanceBox.setValue(Integer.toString(s.maxDistance));
        } finally {
            updating = false;
        }
    }

    // ---- widget factories ------------------------------------------------------------------------------------------

    private ThemeButton button(Runnable toggle) {
        ThemeButton button = new ThemeButton(0, 0, 220, 20, TextComponent.EMPTY, pressed -> {
            toggle.run();
            ArcaneBeamConfig.requestSave();
            refreshScreen.run();
        });
        addContent.apply(button);
        return button;
    }

    private EditBox colorBox(String label, int slot) {
        EditBox box = new EditBox(font, 0, 0, SectionLayout.HEX_WIDTH, 20, new TextComponent(label));
        box.setMaxLength(7);
        box.setFilter(value -> value.isEmpty() || value.matches(HEX_PATTERN));
        box.setResponder(value -> {
            if (updating || value == null) {
                return;
            }
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() != 6) {
                return;
            }
            try {
                setColor(slot, Integer.parseInt(hex, 16));
                ArcaneBeamConfig.requestSave();
            } catch (NumberFormatException ignored) {
            }
        });
        addContent.apply(box);
        return box;
    }

    private EditBox decimalBox(String label, java.util.function.DoubleConsumer setter) {
        EditBox box = new EditBox(font, 0, 0, 54, 20, new TextComponent(label));
        box.setMaxLength(5);
        box.setFilter(value -> value.isEmpty() || value.matches(DECIMAL_PATTERN));
        box.setResponder(value -> {
            if (updating || value == null || value.isEmpty() || ".".equals(value)) {
                return;
            }
            try {
                setter.accept(Double.parseDouble(value));
                ArcaneBeamConfig.requestSave();
            } catch (NumberFormatException ignored) {
            }
        });
        addContent.apply(box);
        return box;
    }

    private EditBox integerBox(String label) {
        EditBox box = new EditBox(font, 0, 0, 54, 20, new TextComponent(label));
        box.setMaxLength(3);
        box.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}"));
        box.setResponder(value -> {
            if (updating || value == null || value.isEmpty()) {
                return;
            }
            try {
                setDistance(Integer.parseInt(value));
                ArcaneBeamConfig.requestSave();
            } catch (NumberFormatException ignored) {
            }
        });
        addContent.apply(box);
        return box;
    }

    // ---- value setters (clamped to the same bounds the config applies on load) --------------------------------------

    private static double step(boolean up, boolean coarse) {
        double step = coarse ? 0.10 : 0.01;
        return up ? step : -step;
    }

    private static double clamp(double value, float min, float max) {
        return Math.round(Math.max(min, Math.min(max, value)) * 100.0) / 100.0;
    }

    private void setOpacity(double value) {
        settings().opacity = (float) clamp(value, LabyrinthSettings.MIN_OPACITY, LabyrinthSettings.MAX_OPACITY);
    }

    private void setWidth(double value) {
        settings().width = (float) clamp(value, LabyrinthSettings.MIN_WIDTH, LabyrinthSettings.MAX_WIDTH);
    }

    private void setThickness(double value) {
        settings().thickness = (float) clamp(value, LabyrinthSettings.MIN_THICKNESS, LabyrinthSettings.MAX_THICKNESS);
    }

    private void setElevation(double value) {
        settings().elevation = (float) clamp(value, LabyrinthSettings.MIN_ELEVATION, LabyrinthSettings.MAX_ELEVATION);
    }

    private void setDistance(int value) {
        settings().maxDistance = Math.max(LabyrinthSettings.MIN_DISTANCE, Math.min(LabyrinthSettings.MAX_DISTANCE, value));
    }

    private static String hundredths(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
