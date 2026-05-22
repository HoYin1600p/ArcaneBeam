package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArcaneBeamConfigScreen extends Screen {
    private static final int PALETTE_WIDTH = 180;
    private static final int PALETTE_HEIGHT = 110;
    private static final int BRIGHTNESS_WIDTH = 14;
    private static final int SLOT_PREVIEW_WIDTH = 20;
    private static final int SLOT_HEX_WIDTH = 54;
    private static final int SLOT_GAP = 12;
    private static final int SLOT_INNER_GAP = 6;
    private static final int ROW_LABEL_GAP = 8;

    private final List<EditBox> colorBoxes = new ArrayList<>();
    private final List<EditBox> glowColorBoxes = new ArrayList<>();
    private final List<EditBox> originBoxes = new ArrayList<>();
    private SettingSlider intensitySlider;
    private SettingSlider opacitySlider;
    private SettingSlider glowRadiusSlider;
    private SettingSlider glowOpacitySlider;
    private SettingSlider colorShiftSlider;
    private SettingSlider glowRotationSlider;
    private Button soundButton;
    private Button handButton;
    private Button shaderCompatibilityButton;
    private boolean railSelected;
    private boolean draggingPalette;
    private boolean draggingBrightness;
    private int brightnessDragBaseColor;
    private int selectedSlot;
    private boolean glowColorsSelected;
    private int paletteX;
    private int paletteY;

    public ArcaneBeamConfigScreen() {
        super(new TextComponent("Arcane Beam"));
    }

    public ArcaneBeamManager.BeamKind previewKind() {
        return railSelected ? ArcaneBeamManager.BeamKind.RAIL : ArcaneBeamManager.BeamKind.ARCANE;
    }

    @Override
    protected void init() {
        colorBoxes.clear();
        glowColorBoxes.clear();
        originBoxes.clear();
        paletteX = this.width / 2 - PALETTE_WIDTH / 2;
        paletteY = 72;

        this.addRenderableWidget(new Button(this.width / 2 - 92, 36, 90, 20, new TextComponent("Arcane"), button -> {
            railSelected = false;
            selectedSlot = 0;
            glowColorsSelected = false;
            refreshControls();
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 2, 36, 90, 20, new TextComponent("Rail"), button -> {
            railSelected = true;
            selectedSlot = 0;
            glowColorsSelected = false;
            refreshControls();
        }));

        int boxY = paletteY + PALETTE_HEIGHT + 24;
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            int slotX = slotStartX(i);
            EditBox editBox = new EditBox(this.font, slotX + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, boxY, SLOT_HEX_WIDTH, 20, new TextComponent("Color " + (i + 1)));
            editBox.setMaxLength(7);
            editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
            editBox.setResponder(value -> updateColorFromText(slot, value));
            colorBoxes.add(editBox);
            this.addRenderableWidget(editBox);
        }

        int glowBoxY = boxY + 30;
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            int slotX = slotStartX(i);
            EditBox editBox = new EditBox(this.font, slotX + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, glowBoxY, SLOT_HEX_WIDTH, 20, new TextComponent("Glow " + (i + 1)));
            editBox.setMaxLength(7);
            editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
            editBox.setResponder(value -> updateGlowColorFromText(slot, value));
            glowColorBoxes.add(editBox);
            this.addRenderableWidget(editBox);
        }

        int sliderX = this.width / 2 - 154;
        int sliderY = glowBoxY + 38;
        intensitySlider = new SettingSlider(sliderX, sliderY, 308, 20, "Intensity", 0.02D, 0.25D, () -> settings().intensity, value -> {
            settings().intensity = (float) value;
            settings().radius = (float) value;
        });
        opacitySlider = new SettingSlider(sliderX, sliderY + 24, 308, 20, "Opacity", 0.05D, 1.0D, () -> settings().opacity, value -> {
            settings().opacity = (float) value;
            settings().alpha = (float) value;
        });
        glowRadiusSlider = new SettingSlider(sliderX, sliderY + 48, 308, 20, "Glow Radius", 0.02D, 0.6D, () -> settings().glowRadius, value -> settings().glowRadius = (float) value);
        glowOpacitySlider = new SettingSlider(sliderX, sliderY + 72, 308, 20, "Glow Opacity", 0.0D, 1.0D, () -> settings().glowOpacity, value -> settings().glowOpacity = (float) value);
        colorShiftSlider = new SettingSlider(sliderX, sliderY + 96, 308, 20, "Color Shift", 2.0D, 60.0D, () -> settings().colorShiftTicks, value -> settings().colorShiftTicks = (float) value);
        glowRotationSlider = new SettingSlider(sliderX, sliderY + 120, 308, 20, "Glow Rotation", 0.0D, 60.0D, () -> settings().glowRotationRpm, value -> settings().glowRotationRpm = (float) value);
        this.addRenderableWidget(intensitySlider);
        this.addRenderableWidget(opacitySlider);
        this.addRenderableWidget(glowRadiusSlider);
        this.addRenderableWidget(glowOpacitySlider);
        this.addRenderableWidget(colorShiftSlider);
        this.addRenderableWidget(glowRotationSlider);
        shaderCompatibilityButton = this.addRenderableWidget(new Button(sliderX, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleShaderCompatibility();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        handButton = this.addRenderableWidget(new Button(sliderX + 158, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleHand();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        soundButton = this.addRenderableWidget(new Button(sliderX, sliderY + 168, 308, 20, TextComponent.EMPTY, button -> {
            cycleSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));

        int originY = sliderY + 192;
        addOriginBox(sliderX, originY, "X", 0);
        addOriginBox(sliderX + 104, originY, "Y", 1);
        addOriginBox(sliderX + 208, originY, "Z", 2);

        this.addRenderableWidget(new Button(this.width / 2 - 45, this.height - 32, 90, 20, new TextComponent("Done"), button -> onClose()));
        refreshControls();
    }

    private void addOriginBox(int x, int y, String label, int axis) {
        this.addRenderableWidget(new Button(x, y, 22, 20, new TextComponent(label), button -> {
        }));
        EditBox editBox = new EditBox(this.font, x + 25, y, 70, 20, new TextComponent(label + " Offset"));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,2}(\\.[0-9]{0,2})?"));
        editBox.setResponder(value -> updateOriginFromText(axis, value));
        originBoxes.add(editBox);
        this.addRenderableWidget(editBox);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, railSelected ? "Rail colors" : "Arcane colors", this.width / 2, 60, 0xD8D8D8);
        renderPalette(poseStack);
        renderBrightnessStrip(poseStack);
        renderInlinePreviews(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePreviewSelection(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && mouseX >= paletteX && mouseX < paletteX + PALETTE_WIDTH && mouseY >= paletteY && mouseY < paletteY + PALETTE_HEIGHT) {
            draggingPalette = true;
            updatePaletteSelection(mouseX, mouseY);
            return true;
        }

        int brightnessX = paletteX + PALETTE_WIDTH + 10;
        if (button == 0 && mouseX >= brightnessX && mouseX < brightnessX + BRIGHTNESS_WIDTH && mouseY >= paletteY && mouseY < paletteY + PALETTE_HEIGHT) {
            draggingBrightness = true;
            brightnessDragBaseColor = activeColors()[selectedSlot];
            updateBrightnessSelection(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingPalette) {
            updatePaletteSelection(mouseX, mouseY);
            return true;
        }
        if (button == 0 && draggingBrightness) {
            updateBrightnessSelection(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingPalette || draggingBrightness)) {
            draggingPalette = false;
            draggingBrightness = false;
            ArcaneBeamConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (int i = 0; i < originBoxes.size(); i++) {
            EditBox originBox = originBoxes.get(i);
            if (originBox.isMouseOver(mouseX, mouseY)) {
                double step = hasShiftDown() ? 0.10D : 0.01D;
                nudgeOrigin(i, delta > 0.0D ? step : -step);
                refreshOriginBoxes();
                ArcaneBeamConfig.save();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        for (EditBox colorBox : colorBoxes) {
            colorBox.tick();
        }
        for (EditBox glowColorBox : glowColorBoxes) {
            glowColorBox.tick();
        }
        for (EditBox originBox : originBoxes) {
            originBox.tick();
        }
    }

    private void renderPalette(PoseStack poseStack) {
        for (int x = 0; x < PALETTE_WIDTH; x++) {
            for (int y = 0; y < PALETTE_HEIGHT; y++) {
                fill(poseStack, paletteX + x, paletteY + y, paletteX + x + 1, paletteY + y + 1, 0xFF000000 | colorAt(x, y));
            }
        }
        fill(poseStack, paletteX - 1, paletteY - 1, paletteX + PALETTE_WIDTH + 1, paletteY, 0xFFFFFFFF);
        fill(poseStack, paletteX - 1, paletteY + PALETTE_HEIGHT, paletteX + PALETTE_WIDTH + 1, paletteY + PALETTE_HEIGHT + 1, 0xFFFFFFFF);
        fill(poseStack, paletteX - 1, paletteY, paletteX, paletteY + PALETTE_HEIGHT, 0xFFFFFFFF);
        fill(poseStack, paletteX + PALETTE_WIDTH, paletteY, paletteX + PALETTE_WIDTH + 1, paletteY + PALETTE_HEIGHT, 0xFFFFFFFF);
    }

    private void renderBrightnessStrip(PoseStack poseStack) {
        int x = paletteX + PALETTE_WIDTH + 10;
        int baseColor = draggingBrightness ? brightnessDragBaseColor : activeColors()[selectedSlot];
        for (int y = 0; y < PALETTE_HEIGHT; y++) {
            int color = 0xFF000000 | applyBrightness(baseColor, y);
            fill(poseStack, x, paletteY + y, x + BRIGHTNESS_WIDTH, paletteY + y + 1, color);
        }

        fill(poseStack, x - 1, paletteY - 1, x + BRIGHTNESS_WIDTH + 1, paletteY, 0xFFFFFFFF);
        fill(poseStack, x - 1, paletteY + PALETTE_HEIGHT, x + BRIGHTNESS_WIDTH + 1, paletteY + PALETTE_HEIGHT + 1, 0xFFFFFFFF);
        fill(poseStack, x - 1, paletteY, x, paletteY + PALETTE_HEIGHT, 0xFFFFFFFF);
        fill(poseStack, x + BRIGHTNESS_WIDTH, paletteY, x + BRIGHTNESS_WIDTH + 1, paletteY + PALETTE_HEIGHT, 0xFFFFFFFF);

    }

    private void renderInlinePreviews(PoseStack poseStack) {
        int beamY = paletteY + PALETTE_HEIGHT + 24;
        int glowY = beamY + 30;
        int labelX = rowStartX() - ROW_LABEL_GAP - Math.max(this.font.width("Beam"), this.font.width("Glow"));
        drawString(poseStack, this.font, "Beam", labelX, beamY + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Glow", labelX, glowY + 6, 0xD8D8D8);
        for (int i = 0; i < 4; i++) {
            int previewX = slotStartX(i);
            renderPreviewBox(poseStack, previewX, beamY, settings().colors[i], i == selectedSlot && !glowColorsSelected);
            renderPreviewBox(poseStack, previewX, glowY, settings().glowColors[i], i == selectedSlot && glowColorsSelected);
        }
    }

    private void renderPreviewBox(PoseStack poseStack, int x, int y, int color, boolean selected) {
        int border = selected ? 0xFFFFFFFF : 0xFF707070;
        fill(poseStack, x, y, x + SLOT_PREVIEW_WIDTH, y + 20, border);
        fill(poseStack, x + 1, y + 1, x + SLOT_PREVIEW_WIDTH - 1, y + 19, 0xFF000000 | color);
    }

    private boolean handlePreviewSelection(double mouseX, double mouseY) {
        int beamY = paletteY + PALETTE_HEIGHT + 24;
        int glowY = beamY + 30;
        for (int i = 0; i < 4; i++) {
            int previewX = slotStartX(i);
            if (isInside(mouseX, mouseY, previewX, beamY, SLOT_PREVIEW_WIDTH, 20)) {
                selectedSlot = i;
                glowColorsSelected = false;
                return true;
            }
            if (isInside(mouseX, mouseY, previewX, glowY, SLOT_PREVIEW_WIDTH, 20)) {
                selectedSlot = i;
                glowColorsSelected = true;
                return true;
            }
        }
        return false;
    }

    private int colorAt(int x, int y) {
        float hue = x / (float) Math.max(1, PALETTE_WIDTH - 1);
        float brightness = 1.0F - (y / (float) Math.max(1, PALETTE_HEIGHT - 1));
        return java.awt.Color.HSBtoRGB(hue, 0.95F, Math.max(0.05F, brightness)) & 0xFFFFFF;
    }

    private void updatePaletteSelection(double mouseX, double mouseY) {
        int x = clamp((int) (mouseX - paletteX), 0, PALETTE_WIDTH - 1);
        int y = clamp((int) (mouseY - paletteY), 0, PALETTE_HEIGHT - 1);
        activeColors()[selectedSlot] = colorAt(x, y);
        syncPrimaryColors();
        refreshBoxes();
    }

    private void updateBrightnessSelection(double mouseY) {
        int y = clamp((int) (mouseY - paletteY), 0, PALETTE_HEIGHT - 1);
        activeColors()[selectedSlot] = applyBrightness(brightnessDragBaseColor, y);
        syncPrimaryColors();
        refreshBoxes();
    }

    private int applyBrightness(int color, int y) {
        float center = (PALETTE_HEIGHT - 1) / 2.0F;
        float amount = (center - y) / center;
        amount = Math.max(-1.0F, Math.min(1.0F, amount));

        int target = amount >= 0.0F ? 0xFFFFFF : 0x000000;
        return lerpColor(color, target, Math.abs(amount));
    }

    private static int lerpColor(int first, int second, float progress) {
        int r1 = (first >> 16) & 0xFF;
        int g1 = (first >> 8) & 0xFF;
        int b1 = first & 0xFF;
        int r2 = (second >> 16) & 0xFF;
        int g2 = (second >> 8) & 0xFF;
        int b2 = second & 0xFF;
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int slotStartX(int slot) {
        return rowStartX() + slot * (slotWidth() + SLOT_GAP);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int rowStartX() {
        return this.width / 2 - rowWidth() / 2;
    }

    private static int rowWidth() {
        return slotWidth() * 4 + SLOT_GAP * 3;
    }

    private static int slotWidth() {
        return SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP + SLOT_HEX_WIDTH;
    }

    private void refreshBoxes() {
        for (int i = 0; i < colorBoxes.size(); i++) {
            colorBoxes.get(i).setValue(formatColor(settings().colors[i]));
        }
        for (int i = 0; i < glowColorBoxes.size(); i++) {
            glowColorBoxes.get(i).setValue(formatColor(settings().glowColors[i]));
        }
    }

    private void refreshControls() {
        refreshBoxes();
        if (intensitySlider != null) {
            intensitySlider.refresh();
            opacitySlider.refresh();
            glowRadiusSlider.refresh();
            glowOpacitySlider.refresh();
            colorShiftSlider.refresh();
            glowRotationSlider.refresh();
            shaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + shaderCompatibility().label));
            soundButton.setMessage(new TextComponent("Sound: " + soundChoice().label));
            handButton.setMessage(new TextComponent("Start: " + startHand().label));
            refreshOriginBoxes();
        }
    }

    private void cycleShaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility current = shaderCompatibility();
        ArcaneBeamConfig.INSTANCE.shaderCompatibility = current == ArcaneBeamConfig.ShaderCompatibility.ON
                ? ArcaneBeamConfig.ShaderCompatibility.OFF.id
                : ArcaneBeamConfig.ShaderCompatibility.ON.id;
    }

    private void cycleSound() {
        ArcaneBeamConfig.SoundChoice[] choices = ArcaneBeamConfig.SoundChoice.values();
        ArcaneBeamConfig.SoundChoice current = soundChoice();
        int next = (current.ordinal() + 1) % choices.length;
        settings().sound = choices[next].id;
    }

    private void cycleHand() {
        ArcaneBeamConfig.StartHand current = startHand();
        settings().startHand = current == ArcaneBeamConfig.StartHand.OFFHAND ? ArcaneBeamConfig.StartHand.MAIN_HAND.id : ArcaneBeamConfig.StartHand.OFFHAND.id;
    }

    private void updateColorFromText(int slot, String value) {
        if (value == null) {
            return;
        }

        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }

        try {
            settings().colors[slot] = Integer.parseInt(normalized, 16);
            syncPrimaryColors();
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateGlowColorFromText(int slot, String value) {
        if (value == null) {
            return;
        }

        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }

        try {
            settings().glowColors[slot] = Integer.parseInt(normalized, 16);
            syncPrimaryColors();
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void refreshOriginBoxes() {
        if (originBoxes.size() < 3) {
            return;
        }
        originBoxes.get(0).setValue(formatOffset(settings().startOffsetX));
        originBoxes.get(1).setValue(formatOffset(settings().startOffsetY));
        originBoxes.get(2).setValue(formatOffset(settings().startOffsetZ));
    }

    private void updateOriginFromText(int axis, String value) {
        if (value == null || value.isEmpty() || "-".equals(value) || ".".equals(value) || "-.".equals(value)) {
            return;
        }

        try {
            double parsed = Math.round(Double.parseDouble(value) * 100.0D) / 100.0D;
            if (axis == 0) {
                settings().startOffsetX = parsed;
            } else if (axis == 1) {
                settings().startOffsetY = parsed;
            } else {
                settings().startOffsetZ = parsed;
            }
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void nudgeOrigin(int axis, double amount) {
        if (axis == 0) {
            settings().startOffsetX = roundOffset(settings().startOffsetX + amount);
        } else if (axis == 1) {
            settings().startOffsetY = roundOffset(settings().startOffsetY + amount);
        } else {
            settings().startOffsetZ = roundOffset(settings().startOffsetZ + amount);
        }
    }

    private ArcaneBeamConfig.BeamSettings settings() {
        return railSelected ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
    }

    private int[] activeColors() {
        return glowColorsSelected ? settings().glowColors : settings().colors;
    }

    private void syncPrimaryColors() {
        settings().color = settings().colors[0];
        settings().glowColor = settings().glowColors[0];
    }

    private ArcaneBeamConfig.SoundChoice soundChoice() {
        ArcaneBeamConfig.SoundChoice choice = ArcaneBeamConfig.SoundChoice.fromId(settings().sound);
        return choice == null ? ArcaneBeamConfig.SoundChoice.DEFAULT : choice;
    }

    private ArcaneBeamConfig.StartHand startHand() {
        ArcaneBeamConfig.StartHand hand = ArcaneBeamConfig.StartHand.fromId(settings().startHand);
        return hand == null ? ArcaneBeamConfig.StartHand.OFFHAND : hand;
    }

    private ArcaneBeamConfig.ShaderCompatibility shaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(ArcaneBeamConfig.INSTANCE.shaderCompatibility);
        return compatibility == null ? ArcaneBeamConfig.ShaderCompatibility.OFF : compatibility;
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private static String formatOffset(double offset) {
        return String.format(Locale.ROOT, "%.2f", offset);
    }

    private static double roundOffset(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private interface DoubleGetter {
        double get();
    }

    private interface DoubleSetter {
        void set(double value);
    }

    private static class SettingSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleGetter getter;
        private final DoubleSetter setter;

        SettingSlider(int x, int y, int width, int height, String label, double min, double max, DoubleGetter getter, DoubleSetter setter) {
            super(x, y, width, height, TextComponent.EMPTY, normalize(getter.get(), min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        void refresh() {
            this.value = normalize(getter.get(), min, max);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if ("Color Shift".equals(label)) {
                setMessage(new TextComponent(label + ": " + String.format(Locale.ROOT, "%.2fs/color", actualValue() / 20.0D)));
            } else if ("Glow Rotation".equals(label)) {
                setMessage(new TextComponent(label + ": " + String.format(Locale.ROOT, "%.1f rpm", actualValue())));
            } else {
                setMessage(new TextComponent(label + ": " + String.format(Locale.ROOT, "%.2f", actualValue())));
            }
        }

        @Override
        protected void applyValue() {
            setter.set(actualValue());
            ArcaneBeamConfig.save();
            updateMessage();
        }

        private double actualValue() {
            return min + (max - min) * this.value;
        }

        private static double normalize(double value, double min, double max) {
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
        }
    }
}
