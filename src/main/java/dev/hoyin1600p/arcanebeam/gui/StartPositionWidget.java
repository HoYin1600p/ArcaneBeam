package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.config.model.StartHand;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.Locale;
import java.util.function.Consumer;

import static dev.hoyin1600p.arcanebeam.util.IntMath.clamp;

/**
 * The small draggable window shown while the settings panel is minimized on a page with an editable origin. It nudges
 * the same three offsets as the page's own X/Y/Z fields, so the beam or missile preview behind it moves live.
 * <p>
 * The window owns its child widgets; the screen registers them (see the constructor) so they take part in normal
 * event routing, and draws {@link #render} before them. Position and step size are remembered for the session in
 * static fields; they are not written to the config file.
 */
final class StartPositionWidget {
    static final int WIDTH = 280;
    static final int HEIGHT = 190;
    private static final int TITLE_HEIGHT = 24;
    private static final int PAD_SIZE = 96;
    private static final double PAD_SENSITIVITY = 0.01D;
    private static final double[] STEPS = {0.01D, 0.05D, 0.25D};
    private static final String[] STEP_LABELS = {"0.01", "0.05", "0.25"};
    private static final char[] AXIS_NAMES = {'X', 'Y', 'Z'};

    // Session memory only: survives re-opening the screen, not a game restart.
    private static int savedX = Integer.MIN_VALUE;
    private static int savedY = Integer.MIN_VALUE;
    private static int savedStep = 1;

    /** The offsets the window edits. The screen implements it over whichever settings object the page uses. */
    interface Target {
        /** Page name for the title, for example "Arcane". */
        String pageName();

        boolean hasHand();

        StartHand hand();

        void applyHand(StartHand hand);

        double get(int axis);

        /** Sets an offset from a number; the target rounds and clamps it like the page's own fields do. */
        void set(int axis, double value);

        /** Applies typed text with the same parsing as the page's own field. */
        void setFromText(int axis, String text);

        /** Largest absolute offset the target accepts. */
        double limit();

        /** Puts X, Y and Z back to the bundled defaults for the page. */
        void reset();
    }

    private final Font font;
    private final Runnable onChanged;
    private final ThemeButton panelButton;
    private final ThemeButton offhandButton;
    private final ThemeButton mainHandButton;
    private final ThemeButton[] minusButtons = new ThemeButton[3];
    private final ThemeButton[] plusButtons = new ThemeButton[3];
    private final EditBox[] boxes = new EditBox[3];
    private final ThemeButton[] stepButtons = new ThemeButton[3];
    private final ThemeButton resetButton;
    private Target target;
    private boolean open;
    private boolean refreshing;
    private int x = savedX;
    private int y = savedY;
    private boolean draggingWindow;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean draggingPad;
    private int padAnchorX;
    private int padAnchorY;
    private double padAnchorValueX;
    private double padAnchorValueY;

    StartPositionWidget(Font font, Consumer<AbstractWidget> register, Runnable onRestorePanel, Runnable onChanged) {
        this.font = font;
        this.onChanged = onChanged;
        panelButton = new ThemeButton(0, 0, 62, 16, new TextComponent("Panel"), b -> onRestorePanel.run());
        panelButton.setArrow(ArrowIcon.Direction.LEFT);
        offhandButton = new ThemeButton(0, 0, 70, 20, new TextComponent(StartHand.OFFHAND.label), b -> chooseHand(StartHand.OFFHAND));
        mainHandButton = new ThemeButton(0, 0, 70, 20, new TextComponent(StartHand.MAIN_HAND.label), b -> chooseHand(StartHand.MAIN_HAND));
        register.accept(panelButton);
        register.accept(offhandButton);
        register.accept(mainHandButton);
        for (int axis = 0; axis < 3; axis++) {
            final int a = axis;
            minusButtons[axis] = new ThemeButton(0, 0, 22, 20, new TextComponent("-"), b -> nudge(a, -STEPS[savedStep]));
            plusButtons[axis] = new ThemeButton(0, 0, 22, 20, new TextComponent("+"), b -> nudge(a, STEPS[savedStep]));
            EditBox box = new EditBox(font, 0, 0, 52, 20, new TextComponent(AXIS_NAMES[axis] + " Offset"));
            box.setMaxLength(7);
            box.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,2}(\\.[0-9]{0,2})?"));
            box.setResponder(value -> {
                if (!refreshing && target != null) {
                    target.setFromText(a, value);
                }
            });
            box.setBordered(true);
            box.setTextColor(ArcaneBeamGuiTheme.TEXT);
            boxes[axis] = box;
            register.accept(minusButtons[axis]);
            register.accept(box);
            register.accept(plusButtons[axis]);
        }
        for (int i = 0; i < 3; i++) {
            final int index = i;
            stepButtons[i] = new ThemeButton(0, 0, 44, 20, new TextComponent(STEP_LABELS[i]), b -> {
                savedStep = index;
                updateButtons();
            });
            register.accept(stepButtons[i]);
        }
        resetButton = new ThemeButton(0, 0, 60, 20, new TextComponent("Reset"), b -> {
            if (target != null) {
                target.reset();
                refresh();
                onChanged.run();
            }
        });
        register.accept(resetButton);
        setChildrenVisible(false);
    }

    /** The panel's side decides which way the Panel button's arrow points: toward the panel. */
    void setPanelOnRight(boolean panelOnRight) {
        panelButton.setArrow(panelOnRight ? ArrowIcon.Direction.RIGHT : ArrowIcon.Direction.LEFT);
    }

    boolean isOpen() {
        return open;
    }

    /** Shows the window for {@code newTarget}. */
    void open(Target newTarget, int layoutWidth, int layoutHeight) {
        target = newTarget;
        open = true;
        refresh();
        layout(layoutWidth, layoutHeight);
        setChildrenVisible(true);
        updateButtons();
    }

    void close() {
        open = false;
        draggingWindow = false;
        draggingPad = false;
        for (EditBox box : boxes) {
            box.setFocus(false);
        }
        setChildrenVisible(false);
    }

    EditBox[] boxes() {
        return boxes;
    }

    /** Keeps the window on screen and moves the child widgets with it. */
    void layout(int layoutWidth, int layoutHeight) {
        if (x == Integer.MIN_VALUE) {
            x = layoutWidth / 2 - WIDTH / 2;
            y = 70;
        }
        x = clamp(x, 0, Math.max(0, layoutWidth - WIDTH));
        y = clamp(y, 0, Math.max(0, layoutHeight - HEIGHT));
        savedX = x;
        savedY = y;
        panelButton.x = x + WIDTH - 62 - 6;
        panelButton.y = y + 4;
        offhandButton.x = x + 10;
        offhandButton.y = y + 30;
        mainHandButton.x = x + 84;
        mainHandButton.y = y + 30;
        for (int axis = 0; axis < 3; axis++) {
            int rowY = y + 62 + axis * 26;
            minusButtons[axis].x = x + 130;
            minusButtons[axis].y = rowY;
            boxes[axis].x = x + 160;
            boxes[axis].y = rowY;
            plusButtons[axis].x = x + 220;
            plusButtons[axis].y = rowY;
        }
        for (int i = 0; i < 3; i++) {
            stepButtons[i].x = x + 40 + i * 48;
            stepButtons[i].y = y + 162;
        }
        resetButton.x = x + WIDTH - 10 - 60;
        resetButton.y = y + 162;
    }

    /** Re-reads the offsets from the target into the boxes. Typing in a box never calls this. */
    void refresh() {
        if (target == null) {
            return;
        }
        refreshing = true;
        try {
            for (int axis = 0; axis < 3; axis++) {
                boxes[axis].setValue(String.format(Locale.ROOT, "%.2f", target.get(axis)));
            }
        } finally {
            refreshing = false;
        }
        updateButtons();
    }

    private void updateButtons() {
        if (target == null) {
            return;
        }
        boolean hand = target.hasHand();
        offhandButton.visible = open && hand;
        offhandButton.active = open && hand;
        mainHandButton.visible = open && hand;
        mainHandButton.active = open && hand;
        offhandButton.setSelected(hand && target.hand() == StartHand.OFFHAND);
        mainHandButton.setSelected(hand && target.hand() == StartHand.MAIN_HAND);
        for (int i = 0; i < 3; i++) {
            stepButtons[i].setSelected(i == savedStep);
        }
    }

    private void setChildrenVisible(boolean visible) {
        panelButton.visible = visible;
        panelButton.active = visible;
        resetButton.visible = visible;
        resetButton.active = visible;
        for (int i = 0; i < 3; i++) {
            minusButtons[i].visible = visible;
            minusButtons[i].active = visible;
            plusButtons[i].visible = visible;
            plusButtons[i].active = visible;
            boxes[i].visible = visible;
            boxes[i].active = visible;
            stepButtons[i].visible = visible;
            stepButtons[i].active = visible;
        }
        if (!visible) {
            offhandButton.visible = false;
            offhandButton.active = false;
            mainHandButton.visible = false;
            mainHandButton.active = false;
        }
    }

    private void chooseHand(StartHand hand) {
        if (target != null) {
            target.applyHand(hand);
            updateButtons();
            onChanged.run();
        }
    }

    private void nudge(int axis, double amount) {
        if (target != null) {
            target.set(axis, target.get(axis) + amount);
            refresh();
            onChanged.run();
        }
    }

    void tick() {
        for (EditBox box : boxes) {
            box.tick();
        }
    }

    boolean contains(double mouseX, double mouseY) {
        return open && mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    private int padX() {
        return x + 10;
    }

    private int padY() {
        return y + 58;
    }

    void render(PoseStack poseStack) {
        if (!open || target == null) {
            return;
        }
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, x, y, WIDTH, HEIGHT);
        GuiComponent.fill(poseStack, x + 8, y + 8, x + WIDTH - 8, y + HEIGHT - 8, 0xE0080C12);
        GuiComponent.fill(poseStack, x + 8, y + TITLE_HEIGHT, x + WIDTH - 8, y + TITLE_HEIGHT + 1, ArcaneBeamGuiTheme.GOLD_LINE);
        drawGrip(poseStack, x + 12, y + 8);
        GuiComponent.drawString(poseStack, font, "Start Position \u00B7 " + target.pageName(), x + 24, y + 9, ArcaneBeamGuiTheme.TEXT);
        if (!target.hasHand()) {
            GuiComponent.drawString(poseStack, font, "Missile origin offsets", x + 12, y + 36, ArcaneBeamGuiTheme.MUTED_TEXT);
        }
        renderPad(poseStack);
        for (int axis = 0; axis < 3; axis++) {
            GuiComponent.drawString(poseStack, font, String.valueOf(AXIS_NAMES[axis]), x + 114, y + 68 + axis * 26, ArcaneBeamGuiTheme.GOLD);
            EditBox box = boxes[axis];
            NineSliceRenderer.draw(poseStack, NineSliceRenderer.INSET, box.x - 4, box.y - 4, box.getInnerWidth() + 16, box.getHeight() + 8);
        }
        GuiComponent.drawString(poseStack, font, "Step", x + 10, y + 168, ArcaneBeamGuiTheme.MUTED_TEXT);
    }

    /** A 2x3 grid of dots marking the title bar as draggable. */
    private static void drawGrip(PoseStack poseStack, int gx, int gy) {
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                GuiComponent.fill(poseStack, gx + column * 4, gy + row * 4, gx + column * 4 + 2, gy + row * 4 + 2, ArcaneBeamGuiTheme.GOLD);
            }
        }
    }

    private void renderPad(PoseStack poseStack) {
        int px = padX();
        int py = padY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.INSET, px - 3, py - 3, PAD_SIZE + 6, PAD_SIZE + 6);
        GuiComponent.fill(poseStack, px, py, px + PAD_SIZE, py + PAD_SIZE, 0xAA05080D);
        for (int i = 1; i < 4; i++) {
            int offset = i * PAD_SIZE / 4;
            GuiComponent.fill(poseStack, px + offset, py, px + offset + 1, py + PAD_SIZE, 0x2266D8F0);
            GuiComponent.fill(poseStack, px, py + offset, px + PAD_SIZE, py + offset + 1, 0x2266D8F0);
        }
        double half = PAD_SIZE / 2.0D;
        int dotX = (int) Math.round(px + half + Math.max(-1.0D, Math.min(1.0D, target.get(0))) * half);
        int dotY = (int) Math.round(py + half - Math.max(-1.0D, Math.min(1.0D, target.get(1))) * half);
        GuiComponent.fill(poseStack, dotX - 3, dotY - 3, dotX + 4, dotY + 4, 0xFF000000);
        GuiComponent.fill(poseStack, dotX - 2, dotY - 2, dotX + 3, dotY + 3, ArcaneBeamGuiTheme.GOLD);
    }

    /**
     * Handles presses on the title bar (drag the window) and the pad (nudge X and Y). Presses on child widgets are
     * left for normal routing, so this returns false for them.
     */
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        boolean onPanelButton = mouseX >= panelButton.x && mouseX < panelButton.x + panelButton.getWidth()
                && mouseY >= panelButton.y && mouseY < panelButton.y + panelButton.getHeight();
        if (mouseY < y + TITLE_HEIGHT && !onPanelButton) {
            draggingWindow = true;
            dragOffsetX = (int) mouseX - x;
            dragOffsetY = (int) mouseY - y;
            return true;
        }
        if (mouseX >= padX() && mouseX < padX() + PAD_SIZE && mouseY >= padY() && mouseY < padY() + PAD_SIZE) {
            draggingPad = true;
            padAnchorX = (int) mouseX;
            padAnchorY = (int) mouseY;
            padAnchorValueX = target.get(0);
            padAnchorValueY = target.get(1);
            return true;
        }
        return false;
    }

    boolean mouseDragged(double mouseX, double mouseY, int layoutWidth, int layoutHeight) {
        if (draggingWindow) {
            x = (int) mouseX - dragOffsetX;
            y = (int) mouseY - dragOffsetY;
            layout(layoutWidth, layoutHeight);
            return true;
        }
        if (draggingPad && target != null) {
            double dx = (mouseX - padAnchorX) * PAD_SENSITIVITY;
            double dy = -(mouseY - padAnchorY) * PAD_SENSITIVITY;
            target.set(0, padAnchorValueX + dx);
            target.set(1, padAnchorValueY + dy);
            refresh();
            onChanged.run();
            return true;
        }
        return false;
    }

    boolean mouseReleased() {
        boolean was = draggingWindow || draggingPad;
        draggingWindow = false;
        draggingPad = false;
        return was;
    }

    /** Mouse wheel over one of the three boxes: 0.01 per notch, 0.1 with Shift. Returns true if it was over a box. */
    boolean mouseScrolled(double mouseX, double mouseY, double delta, boolean coarse) {
        if (!open || target == null) {
            return false;
        }
        for (int axis = 0; axis < 3; axis++) {
            EditBox box = boxes[axis];
            if (mouseX >= box.x - 4 && mouseX < box.x + box.getWidth() + 4 && mouseY >= box.y - 4 && mouseY < box.y + box.getHeight() + 4) {
                double step = coarse ? 0.10D : 0.01D;
                nudge(axis, delta > 0.0D ? step : -step);
                return true;
            }
        }
        return false;
    }
}
