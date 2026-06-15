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
import java.util.function.Consumer;

public class ArcaneBeamConfigScreen extends Screen {
    private static final int MIN_LAYOUT_WIDTH = 560;
    private static final int MIN_LAYOUT_HEIGHT = 680;
    private static final int PROFILE_PANEL_Y = 68;
    private static final int PROFILE_PANEL_WIDTH = 132;
    private static final int PROFILE_PANEL_GAP = 20;
    private static final int PROFILE_ROW_HEIGHT = 18;
    private static final int PALETTE_WIDTH = 180;
    private static final int PALETTE_HEIGHT = 110;
    private static final int BRIGHTNESS_WIDTH = 14;
    private static final int SLOT_PREVIEW_WIDTH = 20;
    private static final int SLOT_HEX_WIDTH = 54;
    private static final int SLOT_GAP = 12;
    private static final int SLOT_INNER_GAP = 6;
    private static final int ROW_LABEL_GAP = 8;
    private static final int COLOR_ROW_GAP = 6;
    private static final int GLOW_ROW_GAP = 26;

    private final List<EditBox> colorBoxes = new ArrayList<>();
    private final List<EditBox> glowColorBoxes = new ArrayList<>();
    private final List<EditBox> originBoxes = new ArrayList<>();
    private final List<Button> originLabelButtons = new ArrayList<>();
    private EditBox profileNameBox;
    private EditBox soundVolumeBox;
    private EditBox fadeInTicksBox;
    private EditBox fadeOutTicksBox;
    private SettingSlider intensitySlider;
    private SettingSlider opacitySlider;
    private SettingSlider glowRadiusSlider;
    private SettingSlider glowOpacitySlider;
    private SettingSlider colorShiftSlider;
    private SettingSlider glowRotationSlider;
    private SettingSlider lightningStartRadiusSlider;
    private SettingSlider lightningEndRadiusSlider;
    private SettingSlider lightningThicknessSlider;
    private SettingSlider lightningAlphaSlider;
    private SettingSlider lightningInteriorOpacitySlider;
    private SettingSlider lightningSphereRadiusSlider;
    private SettingSlider lightningSphereOpacitySlider;
    private SettingSlider lightningConeHeightSlider;
    private SettingSlider lightningConeRadiusSlider;
    private SettingSlider lightningConeOpacitySlider;
    private SettingSlider lightningSpotCountSlider;
    private SettingSlider lightningSpotSizeSlider;
    private SettingSlider lightningSpotOpacitySlider;
    private SettingSlider lightningSecondarySizeSlider;
    private Button soundButton;
    private Button handButton;
    private Button shaderCompatibilityButton;
    private Button fadeInModeButton;
    private Button fadeOutModeButton;
    private Button profileAddButton;
    private Button profileDropdownButton;
    private Button lightningEnabledButton;
    private Button lightningShaderCompatibilityButton;
    private Button lightningFullbrightButton;
    private Button lightningSoundButton;
    private EditBox lightningLifetimeBox;
    private EditBox lightningSideCountBox;
    private EditBox lightningRingColorBox;
    private EditBox lightningSphereColorBox;
    private EditBox lightningConeColorBox;
    private EditBox lightningSpotColorBox;
    private EditBox lightningSecondaryCountBox;
    private EditBox lightningSecondaryDelayBox;
    private EditBox lightningSoundVolumeBox;
    private boolean profileDropdownOpen;
    private boolean railSelected;
    private boolean lightningSelected;
    private boolean draggingPalette;
    private boolean draggingBrightness;
    private int brightnessDragBaseColor;
    private int selectedSlot;
    private boolean glowColorsSelected;
    private int paletteX;
    private int paletteY;
    private int layoutWidth;
    private int layoutHeight;
    private float layoutScale = 1.0F;

    public ArcaneBeamConfigScreen() {
        super(new TextComponent("Arcane Beam"));
    }

    public ArcaneBeamManager.BeamKind previewKind() {
        return railSelected ? ArcaneBeamManager.BeamKind.RAIL : ArcaneBeamManager.BeamKind.ARCANE;
    }

    public boolean lightningSelected() {
        return lightningSelected;
    }

    @Override
    protected void init() {
        updateLayoutScale();
        colorBoxes.clear();
        glowColorBoxes.clear();
        originBoxes.clear();
        originLabelButtons.clear();
        profileDropdownOpen = false;
        paletteX = layoutWidth / 2 - PALETTE_WIDTH / 2;
        paletteY = 72;

        this.addRenderableWidget(new Button(layoutWidth / 2 - 139, 36, 90, 20, new TextComponent("Arcane"), button -> {
            railSelected = false;
            lightningSelected = false;
            selectedSlot = 0;
            glowColorsSelected = false;
            profileDropdownOpen = false;
            refreshControls();
        }));
        this.addRenderableWidget(new Button(layoutWidth / 2 - 45, 36, 90, 20, new TextComponent("Rail"), button -> {
            railSelected = true;
            lightningSelected = false;
            selectedSlot = 0;
            glowColorsSelected = false;
            profileDropdownOpen = false;
            refreshControls();
        }));
        this.addRenderableWidget(new Button(layoutWidth / 2 + 49, 36, 112, 20, new TextComponent("Lightning Strike"), button -> {
            railSelected = false;
            lightningSelected = true;
            selectedSlot = 0;
            glowColorsSelected = false;
            profileDropdownOpen = false;
            refreshControls();
        }));

        int profileX = profilePanelX();
        profileNameBox = new EditBox(this.font, profileX, PROFILE_PANEL_Y + 18, 84, 20, new TextComponent("Profile Name"));
        profileNameBox.setMaxLength(24);
        profileNameBox.setFilter(value -> value == null || !value.contains("\n") && !value.contains("\r") && !value.contains("\t"));
        this.addRenderableWidget(profileNameBox);
        profileAddButton = this.addRenderableWidget(new Button(profileX + 88, PROFILE_PANEL_Y + 18, 44, 20, new TextComponent("Add"), button -> addProfile()));
        profileDropdownButton = this.addRenderableWidget(new Button(profileX, PROFILE_PANEL_Y + 42, PROFILE_PANEL_WIDTH, 20, TextComponent.EMPTY, button -> profileDropdownOpen = !profileDropdownOpen));

        int boxY = beamRowY();
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

        int glowBoxY = glowRowY();
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

        int sliderX = layoutWidth / 2 - 154;
        int sliderY = glowBoxY + 32;
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
        int soundRowY = sliderY + 168;
        soundButton = this.addRenderableWidget(new Button(sliderX, soundRowY, 184, 20, TextComponent.EMPTY, button -> {
            cycleSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        soundVolumeBox = new EditBox(this.font, sliderX + 248, soundRowY, 60, 20, new TextComponent("Sound Volume"));
        soundVolumeBox.setMaxLength(4);
        soundVolumeBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        soundVolumeBox.setResponder(this::updateSoundVolumeFromText);
        this.addRenderableWidget(soundVolumeBox);

        int transitionRowY = soundRowY + 24;
        fadeInModeButton = this.addRenderableWidget(new Button(sliderX, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
            cycleFadeInStyle();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        fadeInTicksBox = new EditBox(this.font, sliderX + 128, transitionRowY, 24, 20, new TextComponent("In Ticks"));
        fadeInTicksBox.setMaxLength(2);
        fadeInTicksBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        fadeInTicksBox.setResponder(this::updateFadeInTicksFromText);
        this.addRenderableWidget(fadeInTicksBox);
        fadeOutModeButton = this.addRenderableWidget(new Button(sliderX + 160, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
            cycleFadeOutStyle();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        fadeOutTicksBox = new EditBox(this.font, sliderX + 284, transitionRowY, 24, 20, new TextComponent("Out Ticks"));
        fadeOutTicksBox.setMaxLength(2);
        fadeOutTicksBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        fadeOutTicksBox.setResponder(this::updateFadeOutTicksFromText);
        this.addRenderableWidget(fadeOutTicksBox);

        int originY = transitionRowY + 24;
        addOriginBox(sliderX, originY, "X", 0);
        addOriginBox(sliderX + 104, originY, "Y", 1);
        addOriginBox(sliderX + 208, originY, "Z", 2);

        addLightningControls(sliderX, sliderY);

        this.addRenderableWidget(new Button(layoutWidth / 2 - 45, layoutHeight - 32, 90, 20, new TextComponent("Done"), button -> onClose()));
        refreshControls();
    }

    private void addLightningControls(int x, int y) {
        int lightningColorY = beamRowY();
        lightningRingColorBox = addLightningColorBox(slotStartX(0), lightningColorY, "Ring Color", this::updateLightningRingColorFromText);
        lightningSphereColorBox = addLightningColorBox(slotStartX(1), lightningColorY, "Sphere Color", this::updateLightningSphereColorFromText);
        lightningConeColorBox = addLightningColorBox(slotStartX(2), lightningColorY, "Cone Color", this::updateLightningConeColorFromText);
        lightningSpotColorBox = addLightningColorBox(slotStartX(3), lightningColorY, "Spot Color", this::updateLightningSpotColorFromText);

        lightningEnabledButton = this.addRenderableWidget(new Button(x, y, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().enabled = !lightningSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        lightningShaderCompatibilityButton = this.addRenderableWidget(new Button(x + 158, y, 150, 20, TextComponent.EMPTY, button -> {
            ArcaneBeamConfig.ShaderCompatibility current = lightningShaderCompatibility();
            lightningSettings().shaderCompatibility = current == ArcaneBeamConfig.ShaderCompatibility.ON
                    ? ArcaneBeamConfig.ShaderCompatibility.OFF.id
                    : ArcaneBeamConfig.ShaderCompatibility.ON.id;
            lightningSettings().shaderCompatibilityMigrated = true;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        lightningStartRadiusSlider = new SettingSlider(x, y + 24, 308, 20, "Start Radius", 0.1D, 12.0D, () -> lightningSettings().startRadius, value -> {
            lightningSettings().startRadius = (float) value;
            lightningSettings().endRadius = Math.max(lightningSettings().endRadius, lightningSettings().startRadius);
        });
        lightningEndRadiusSlider = new SettingSlider(x, y + 48, 308, 20, "End Radius", 0.5D, 24.0D, () -> lightningSettings().endRadius, value -> lightningSettings().endRadius = Math.max((float) value, lightningSettings().startRadius));
        lightningThicknessSlider = new SettingSlider(x, y + 72, 150, 20, "Edge Width", 0.02D, 2.0D, () -> lightningSettings().ringThickness, value -> lightningSettings().ringThickness = (float) value);
        lightningAlphaSlider = new SettingSlider(x + 158, y + 72, 150, 20, "Alpha", 0.0D, 1.0D, () -> lightningSettings().alpha, value -> lightningSettings().alpha = (float) value);
        lightningInteriorOpacitySlider = new SettingSlider(x, y + 96, 308, 20, "Interior Opacity", 0.0D, 1.0D, () -> lightningSettings().ringInteriorOpacity, value -> lightningSettings().ringInteriorOpacity = (float) value);
        lightningSphereRadiusSlider = new SettingSlider(x, y + 120, 150, 20, "Sphere Size", 0.02D, 2.0D, () -> lightningSettings().sphereRadius, value -> lightningSettings().sphereRadius = (float) value);
        lightningSphereOpacitySlider = new SettingSlider(x + 158, y + 120, 150, 20, "Sphere Opacity", 0.0D, 1.0D, () -> lightningSettings().sphereOpacity, value -> lightningSettings().sphereOpacity = (float) value);
        lightningConeHeightSlider = new SettingSlider(x, y + 144, 150, 20, "Cone Height", 0.05D, 6.0D, () -> lightningSettings().coneHeight, value -> lightningSettings().coneHeight = (float) value);
        lightningConeRadiusSlider = new SettingSlider(x + 158, y + 144, 150, 20, "Cone Width", 0.02D, 4.0D, () -> lightningSettings().coneRadius, value -> lightningSettings().coneRadius = (float) value);
        lightningConeOpacitySlider = new SettingSlider(x, y + 168, 150, 20, "Cone Opacity", 0.0D, 1.0D, () -> lightningSettings().coneOpacity, value -> lightningSettings().coneOpacity = (float) value);
        lightningSpotCountSlider = new SettingSlider(x + 158, y + 168, 150, 20, "Spot Count", 0.0D, 128.0D, () -> lightningSettings().spotCount, value -> lightningSettings().spotCount = clamp((int) Math.round(value), 0, 128));
        lightningSpotSizeSlider = new SettingSlider(x, y + 192, 150, 20, "Spot Size", 0.02D, 1.5D, () -> lightningSettings().spotSize, value -> lightningSettings().spotSize = (float) value);
        lightningSpotOpacitySlider = new SettingSlider(x + 158, y + 192, 150, 20, "Spot Opacity", 0.0D, 1.0D, () -> lightningSettings().spotOpacity, value -> lightningSettings().spotOpacity = (float) value);
        lightningSecondarySizeSlider = new SettingSlider(x, y + 216, 308, 20, "Secondary Size", 0.1D, 1.5D, () -> lightningSettings().secondaryRippleSize, value -> lightningSettings().secondaryRippleSize = (float) value);
        this.addRenderableWidget(lightningStartRadiusSlider);
        this.addRenderableWidget(lightningEndRadiusSlider);
        this.addRenderableWidget(lightningThicknessSlider);
        this.addRenderableWidget(lightningAlphaSlider);
        this.addRenderableWidget(lightningInteriorOpacitySlider);
        this.addRenderableWidget(lightningSphereRadiusSlider);
        this.addRenderableWidget(lightningSphereOpacitySlider);
        this.addRenderableWidget(lightningConeHeightSlider);
        this.addRenderableWidget(lightningConeRadiusSlider);
        this.addRenderableWidget(lightningConeOpacitySlider);
        this.addRenderableWidget(lightningSpotCountSlider);
        this.addRenderableWidget(lightningSpotSizeSlider);
        this.addRenderableWidget(lightningSpotOpacitySlider);
        this.addRenderableWidget(lightningSecondarySizeSlider);

        lightningFullbrightButton = this.addRenderableWidget(new Button(x, y + 240, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().fullbright = !lightningSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        lightningSoundButton = this.addRenderableWidget(new Button(x + 158, y + 240, 150, 20, TextComponent.EMPTY, button -> {
            cycleLightningSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));

        lightningLifetimeBox = addLightningNumberBox(x, y + 268, 54, 3, "Lifetime", "[0-9]{0,3}", this::updateLightningLifetimeFromText);
        lightningSideCountBox = addLightningNumberBox(x + 104, y + 268, 54, 2, "Sides", "[0-9]{0,2}", this::updateLightningSideCountFromText);
        lightningSecondaryCountBox = addLightningNumberBox(x + 208, y + 268, 54, 1, "Ripples", "[0-9]{0,1}", this::updateLightningSecondaryCountFromText);
        lightningSecondaryDelayBox = addLightningNumberBox(x, y + 296, 54, 2, "Delay", "[0-9]{0,2}", this::updateLightningSecondaryDelayFromText);
        lightningSoundVolumeBox = addLightningSoundVolumeBox(x + 208, y + 296);
    }

    private void updateLayoutScale() {
        float widthScale = this.width / (float) MIN_LAYOUT_WIDTH;
        float heightScale = this.height / (float) MIN_LAYOUT_HEIGHT;
        layoutScale = Math.min(1.0F, Math.min(widthScale, heightScale));
        if (layoutScale <= 0.0F) {
            layoutScale = 1.0F;
        }
        layoutWidth = Math.max(MIN_LAYOUT_WIDTH, (int) Math.ceil(this.width / layoutScale));
        layoutHeight = Math.max(MIN_LAYOUT_HEIGHT, (int) Math.ceil(this.height / layoutScale));
    }

    private void addOriginBox(int x, int y, String label, int axis) {
        Button labelButton = this.addRenderableWidget(new Button(x, y, 22, 20, new TextComponent(label), button -> {
        }));
        originLabelButtons.add(labelButton);
        EditBox editBox = new EditBox(this.font, x + 25, y, 70, 20, new TextComponent(label + " Offset"));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,2}(\\.[0-9]{0,2})?"));
        editBox.setResponder(value -> updateOriginFromText(axis, value));
        originBoxes.add(editBox);
        this.addRenderableWidget(editBox);
    }

    private EditBox addLightningNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 50, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addLightningColorBox(int x, int y, String label, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(responder);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addLightningSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateLightningSoundVolumeFromText);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        int layoutMouseX = toLayoutX(mouseX);
        int layoutMouseY = toLayoutY(mouseY);

        poseStack.pushPose();
        poseStack.scale(layoutScale, layoutScale, 1.0F);
        drawCenteredString(poseStack, this.font, this.title, layoutWidth / 2, 14, 0xFFFFFF);
        renderProfilePanel(poseStack);
        renderPalette(poseStack);
        renderBrightnessStrip(poseStack);
        if (lightningSelected) {
            drawCenteredString(poseStack, this.font, "Lightning Strike colors", layoutWidth / 2, 60, 0xD8D8D8);
            renderLightningColorPreviews(poseStack);
            renderLightningLabels(poseStack);
        } else {
            drawCenteredString(poseStack, this.font, railSelected ? "Rail colors" : "Arcane colors", layoutWidth / 2, 60, 0xD8D8D8);
            renderInlinePreviews(poseStack);
        }
        if (!lightningSelected && soundVolumeBox != null) {
            drawString(poseStack, this.font, "Volume", soundVolumeBox.x - this.font.width("Volume") - 8, soundVolumeBox.y + 6, 0xD8D8D8);
        }
        if (!lightningSelected && fadeInTicksBox != null) {
            drawString(poseStack, this.font, "Ticks", fadeInTicksBox.x - this.font.width("Ticks") - 10, fadeInTicksBox.y + 6, 0xD8D8D8);
        }
        if (!lightningSelected && fadeOutTicksBox != null) {
            drawString(poseStack, this.font, "Ticks", fadeOutTicksBox.x - this.font.width("Ticks") - 6, fadeOutTicksBox.y + 6, 0xD8D8D8);
        }
        super.render(poseStack, layoutMouseX, layoutMouseY, partialTick);
        renderProfileDropdown(poseStack);
        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (button == 0 && handleProfileDropdownSelection(layoutMouseX, layoutMouseY)) {
            return true;
        }
        selectLightningColorBox(layoutMouseX, layoutMouseY);
        if (button == 0 && handlePreviewSelection(layoutMouseX, layoutMouseY)) {
            return true;
        }
        if (button == 0 && layoutMouseX >= paletteX && layoutMouseX < paletteX + PALETTE_WIDTH && layoutMouseY >= paletteY && layoutMouseY < paletteY + PALETTE_HEIGHT) {
            draggingPalette = true;
            updatePaletteSelection(layoutMouseX, layoutMouseY);
            return true;
        }

        int brightnessX = paletteX + PALETTE_WIDTH + 10;
        if (button == 0 && layoutMouseX >= brightnessX && layoutMouseX < brightnessX + BRIGHTNESS_WIDTH && layoutMouseY >= paletteY && layoutMouseY < paletteY + PALETTE_HEIGHT) {
            draggingBrightness = true;
            brightnessDragBaseColor = selectedColor();
            updateBrightnessSelection(layoutMouseY);
            return true;
        }
        return super.mouseClicked(layoutMouseX, layoutMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (button == 0 && draggingPalette) {
            updatePaletteSelection(layoutMouseX, layoutMouseY);
            return true;
        }
        if (button == 0 && draggingBrightness) {
            updateBrightnessSelection(layoutMouseY);
            return true;
        }
        return super.mouseDragged(layoutMouseX, layoutMouseY, button, dragX / layoutScale, dragY / layoutScale);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingPalette || draggingBrightness)) {
            draggingPalette = false;
            draggingBrightness = false;
            ArcaneBeamConfig.save();
            return true;
        }
        return super.mouseReleased(toLayoutX(mouseX), toLayoutY(mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (!lightningSelected && soundVolumeBox != null && soundVolumeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeSoundVolume(delta > 0.0D ? step : -step);
            refreshSoundVolumeBox();
            ArcaneBeamConfig.save();
            return true;
        }
        if (lightningSelected && lightningSoundVolumeBox != null && lightningSoundVolumeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeLightningSoundVolume(delta > 0.0D ? step : -step);
            refreshLightningSoundVolumeBox();
            ArcaneBeamConfig.save();
            return true;
        }
        if (!lightningSelected && fadeInTicksBox != null && fadeInTicksBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            nudgeFadeInTicks(delta > 0.0D ? 1 : -1);
            refreshFadeTickBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        if (!lightningSelected && fadeOutTicksBox != null && fadeOutTicksBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            nudgeFadeOutTicks(delta > 0.0D ? 1 : -1);
            refreshFadeTickBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        for (int i = 0; !lightningSelected && i < originBoxes.size(); i++) {
            EditBox originBox = originBoxes.get(i);
            if (originBox.isMouseOver(layoutMouseX, layoutMouseY)) {
                double step = hasShiftDown() ? 0.10D : 0.01D;
                nudgeOrigin(i, delta > 0.0D ? step : -step);
                refreshOriginBoxes();
                ArcaneBeamConfig.save();
                return true;
            }
        }
        return super.mouseScrolled(layoutMouseX, layoutMouseY, delta);
    }

    private int toLayoutX(double mouseX) {
        return (int) Math.floor(mouseX / layoutScale);
    }

    private int toLayoutY(double mouseY) {
        return (int) Math.floor(mouseY / layoutScale);
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
        if (profileNameBox != null) {
            profileNameBox.tick();
        }
        if (soundVolumeBox != null) {
            soundVolumeBox.tick();
        }
        if (fadeInTicksBox != null) {
            fadeInTicksBox.tick();
        }
        if (fadeOutTicksBox != null) {
            fadeOutTicksBox.tick();
        }
        tickBox(lightningLifetimeBox);
        tickBox(lightningSideCountBox);
        tickBox(lightningRingColorBox);
        tickBox(lightningSphereColorBox);
        tickBox(lightningConeColorBox);
        tickBox(lightningSpotColorBox);
        tickBox(lightningSecondaryCountBox);
        tickBox(lightningSecondaryDelayBox);
        tickBox(lightningSoundVolumeBox);
    }

    private static void tickBox(EditBox box) {
        if (box != null) {
            box.tick();
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
        int baseColor = draggingBrightness ? brightnessDragBaseColor : selectedColor();
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
        int beamY = colorBoxes.isEmpty() ? beamRowY() : colorBoxes.get(0).y;
        int glowY = glowColorBoxes.isEmpty() ? glowRowY() : glowColorBoxes.get(0).y;
        int labelX = rowStartX() - ROW_LABEL_GAP - Math.max(this.font.width("Beam"), this.font.width("Glow"));
        drawString(poseStack, this.font, "Beam", labelX, beamY + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Glow", labelX, glowY + 6, 0xD8D8D8);
        for (int i = 0; i < 4; i++) {
            int previewX = slotStartX(i);
            renderPreviewBox(poseStack, previewX, beamY, settings().colors[i], i == selectedSlot && !glowColorsSelected);
            renderPreviewBox(poseStack, previewX, glowY, settings().glowColors[i], i == selectedSlot && glowColorsSelected);
        }
    }

    private void renderLightningColorPreviews(PoseStack poseStack) {
        if (lightningRingColorBox == null || lightningSphereColorBox == null || lightningConeColorBox == null || lightningSpotColorBox == null) {
            return;
        }
        int labelX = rowStartX() - ROW_LABEL_GAP - this.font.width("Shockwave");
        drawString(poseStack, this.font, "Shockwave", labelX, lightningRingColorBox.y + 6, 0xD8D8D8);
        renderPreviewBox(poseStack, lightningRingColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningRingColorBox.y, lightningSettings().ringColor, selectedSlot == 0);
        renderPreviewBox(poseStack, lightningSphereColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningSphereColorBox.y, lightningSettings().sphereColor, selectedSlot == 1);
        renderPreviewBox(poseStack, lightningConeColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningConeColorBox.y, lightningSettings().coneColor, selectedSlot == 2);
        renderPreviewBox(poseStack, lightningSpotColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningSpotColorBox.y, lightningSettings().spotColor, selectedSlot == 3);
        renderLightningColorLabel(poseStack, lightningRingColorBox, "Ring");
        renderLightningColorLabel(poseStack, lightningSphereColorBox, "Sphere");
        renderLightningColorLabel(poseStack, lightningConeColorBox, "Cone");
        renderLightningColorLabel(poseStack, lightningSpotColorBox, "Spots");
    }

    private void renderLightningColorLabel(PoseStack poseStack, EditBox box, String label) {
        int groupX = box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP;
        int groupWidth = SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP + SLOT_HEX_WIDTH;
        int textX = groupX + groupWidth / 2 - this.font.width(label) / 2;
        drawString(poseStack, this.font, label, textX, box.y + 23, 0xD8D8D8);
    }

    private void renderProfilePanel(PoseStack poseStack) {
        drawString(poseStack, this.font, profileLabel(), profilePanelX(), PROFILE_PANEL_Y, 0xD8D8D8);
    }

    private void renderLightningLabels(PoseStack poseStack) {
        if (lightningLifetimeBox == null || lightningSoundVolumeBox == null) {
            return;
        }
        drawString(poseStack, this.font, "Lifetime", lightningLifetimeBox.x - 50, lightningLifetimeBox.y + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Sides", lightningSideCountBox.x - 42, lightningSideCountBox.y + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Ripples", lightningSecondaryCountBox.x - 48, lightningSecondaryCountBox.y + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Delay", lightningSecondaryDelayBox.x - 42, lightningSecondaryDelayBox.y + 6, 0xD8D8D8);
        drawString(poseStack, this.font, "Volume", lightningSoundVolumeBox.x - 50, lightningSoundVolumeBox.y + 6, 0xD8D8D8);
    }

    private void renderProfileDropdown(PoseStack poseStack) {
        if (!profileDropdownOpen) {
            return;
        }
        List<String> profiles = profileNames();
        int x = profilePanelX();
        int y = profileDropdownY();
        int height = profiles.size() * PROFILE_ROW_HEIGHT;
        fill(poseStack, x - 1, y - 1, x + PROFILE_PANEL_WIDTH + 1, y + height + 1, 0xFFFFFFFF);
        fill(poseStack, x, y, x + PROFILE_PANEL_WIDTH, y + height, 0xEE101018);

        String selected = selectedProfileName();
        for (int i = 0; i < profiles.size(); i++) {
            int rowY = y + i * PROFILE_ROW_HEIGHT;
            String profile = profiles.get(i);
            int rowColor = profile.equals(selected) ? 0xFF304060 : 0xFF181820;
            fill(poseStack, x, rowY, x + PROFILE_PANEL_WIDTH, rowY + PROFILE_ROW_HEIGHT, rowColor);
            drawString(poseStack, this.font, ellipsize(profile, PROFILE_PANEL_WIDTH - 8), x + 4, rowY + 5, 0xFFFFFF);
        }
    }

    private boolean handleProfileDropdownSelection(double mouseX, double mouseY) {
        if (!profileDropdownOpen) {
            return false;
        }

        int profileX = profilePanelX();
        if (isInside(mouseX, mouseY, profileX, PROFILE_PANEL_Y + 42, PROFILE_PANEL_WIDTH, 20)) {
            profileDropdownOpen = false;
            return true;
        }

        int x = profileX;
        int y = profileDropdownY();
        List<String> profiles = profileNames();
        int height = profiles.size() * PROFILE_ROW_HEIGHT;
        if (!isInside(mouseX, mouseY, x, y, PROFILE_PANEL_WIDTH, height)) {
            profileDropdownOpen = false;
            return false;
        }

        int index = (int) ((mouseY - y) / PROFILE_ROW_HEIGHT);
        if (index >= 0 && index < profiles.size()) {
            selectProfile(profiles.get(index));
            selectedSlot = 0;
            glowColorsSelected = false;
            profileDropdownOpen = false;
            refreshControls();
            return true;
        }
        return false;
    }

    private int profileDropdownY() {
        return PROFILE_PANEL_Y + 64;
    }

    private int profilePanelX() {
        return Math.max(12, paletteX - PROFILE_PANEL_WIDTH - PROFILE_PANEL_GAP);
    }

    private void addProfile() {
        if (lightningSelected) {
            ArcaneBeamConfig.addLightningProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else {
            ArcaneBeamConfig.addProfile(railSelected, profileNameBox == null ? "" : profileNameBox.getValue());
        }
        if (profileNameBox != null) {
            profileNameBox.setValue("");
        }
        selectedSlot = 0;
        glowColorsSelected = false;
        profileDropdownOpen = false;
        refreshControls();
    }

    private String ellipsize(String value, int maxWidth) {
        if (this.font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        String clipped = value;
        while (!clipped.isEmpty() && this.font.width(clipped) + suffixWidth > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped + suffix;
    }

    private void renderPreviewBox(PoseStack poseStack, int x, int y, int color, boolean selected) {
        int border = selected ? 0xFFFFFFFF : 0xFF707070;
        fill(poseStack, x, y, x + SLOT_PREVIEW_WIDTH, y + 20, border);
        fill(poseStack, x + 1, y + 1, x + SLOT_PREVIEW_WIDTH - 1, y + 19, 0xFF000000 | color);
    }

    private boolean handlePreviewSelection(double mouseX, double mouseY) {
        if (lightningSelected) {
            if (lightningRingColorBox == null || lightningSphereColorBox == null || lightningConeColorBox == null || lightningSpotColorBox == null) {
                return false;
            }
            EditBox[] boxes = {lightningRingColorBox, lightningSphereColorBox, lightningConeColorBox, lightningSpotColorBox};
            for (int i = 0; i < boxes.length; i++) {
                int previewX = boxes[i].x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP;
                if (isInside(mouseX, mouseY, previewX, boxes[i].y, SLOT_PREVIEW_WIDTH, 20)) {
                    selectedSlot = i;
                    return true;
                }
            }
            return false;
        }
        for (int i = 0; i < 4; i++) {
            int beamY = colorBoxes.get(i).y;
            int glowY = glowColorBoxes.get(i).y;
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

    private void selectLightningColorBox(double mouseX, double mouseY) {
        if (!lightningSelected || lightningRingColorBox == null || lightningSphereColorBox == null || lightningConeColorBox == null || lightningSpotColorBox == null) {
            return;
        }
        EditBox[] boxes = {lightningRingColorBox, lightningSphereColorBox, lightningConeColorBox, lightningSpotColorBox};
        for (int i = 0; i < boxes.length; i++) {
            if (isInside(mouseX, mouseY, boxes[i].x, boxes[i].y, SLOT_HEX_WIDTH, 20)) {
                selectedSlot = i;
                return;
            }
        }
    }

    private int colorAt(int x, int y) {
        float hue = x / (float) Math.max(1, PALETTE_WIDTH - 1);
        float brightness = 1.0F - (y / (float) Math.max(1, PALETTE_HEIGHT - 1));
        return java.awt.Color.HSBtoRGB(hue, 0.95F, Math.max(0.05F, brightness)) & 0xFFFFFF;
    }

    private void updatePaletteSelection(double mouseX, double mouseY) {
        int x = clamp((int) (mouseX - paletteX), 0, PALETTE_WIDTH - 1);
        int y = clamp((int) (mouseY - paletteY), 0, PALETTE_HEIGHT - 1);
        setSelectedColor(colorAt(x, y));
        refreshBoxes();
    }

    private void updateBrightnessSelection(double mouseY) {
        int y = clamp((int) (mouseY - paletteY), 0, PALETTE_HEIGHT - 1);
        setSelectedColor(applyBrightness(brightnessDragBaseColor, y));
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
        return layoutWidth / 2 - rowWidth() / 2;
    }

    private int beamRowY() {
        return paletteY + PALETTE_HEIGHT + COLOR_ROW_GAP;
    }

    private int glowRowY() {
        return beamRowY() + GLOW_ROW_GAP;
    }

    private static int rowWidth() {
        return slotWidth() * 4 + SLOT_GAP * 3;
    }

    private static int slotWidth() {
        return SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP + SLOT_HEX_WIDTH;
    }

    private void refreshBoxes() {
        if (lightningSelected) {
            if (lightningRingColorBox != null) {
                lightningRingColorBox.setValue(formatColor(lightningSettings().ringColor));
            }
            if (lightningSphereColorBox != null) {
                lightningSphereColorBox.setValue(formatColor(lightningSettings().sphereColor));
            }
            if (lightningConeColorBox != null) {
                lightningConeColorBox.setValue(formatColor(lightningSettings().coneColor));
            }
            if (lightningSpotColorBox != null) {
                lightningSpotColorBox.setValue(formatColor(lightningSettings().spotColor));
            }
            return;
        }
        for (int i = 0; i < colorBoxes.size(); i++) {
            colorBoxes.get(i).setValue(formatColor(settings().colors[i]));
        }
        for (int i = 0; i < glowColorBoxes.size(); i++) {
            glowColorBoxes.get(i).setValue(formatColor(settings().glowColors[i]));
        }
    }

    private void refreshControls() {
        refreshBoxes();
        updateWidgetVisibility();
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
            fadeInModeButton.setMessage(new TextComponent(fadeInStyle().label));
            fadeOutModeButton.setMessage(new TextComponent(fadeOutStyle().label));
            profileDropdownButton.setMessage(new TextComponent("Profile: " + ellipsize(selectedProfileName(), 74)));
            refreshOriginBoxes();
            refreshSoundVolumeBox();
            refreshFadeTickBoxes();
            refreshLightningControls();
        }
    }

    private void updateWidgetVisibility() {
        boolean beamVisible = !lightningSelected;
        for (EditBox box : colorBoxes) {
            box.visible = beamVisible;
        }
        for (EditBox box : glowColorBoxes) {
            box.visible = beamVisible;
        }
        for (EditBox box : originBoxes) {
            box.visible = beamVisible;
        }
        for (Button button : originLabelButtons) {
            setVisible(button, beamVisible);
        }
        setVisible(profileNameBox, true);
        setVisible(profileAddButton, true);
        setVisible(profileDropdownButton, true);
        setVisible(soundVolumeBox, beamVisible);
        setVisible(fadeInTicksBox, beamVisible);
        setVisible(fadeOutTicksBox, beamVisible);
        setVisible(intensitySlider, beamVisible);
        setVisible(opacitySlider, beamVisible);
        setVisible(glowRadiusSlider, beamVisible);
        setVisible(glowOpacitySlider, beamVisible);
        setVisible(colorShiftSlider, beamVisible);
        setVisible(glowRotationSlider, beamVisible);
        setVisible(shaderCompatibilityButton, beamVisible);
        setVisible(soundButton, beamVisible);
        setVisible(handButton, beamVisible);
        setVisible(fadeInModeButton, beamVisible);
        setVisible(fadeOutModeButton, beamVisible);

        setVisible(lightningEnabledButton, lightningSelected);
        setVisible(lightningShaderCompatibilityButton, lightningSelected);
        setVisible(lightningFullbrightButton, lightningSelected);
        setVisible(lightningSoundButton, lightningSelected);
        setVisible(lightningStartRadiusSlider, lightningSelected);
        setVisible(lightningEndRadiusSlider, lightningSelected);
        setVisible(lightningThicknessSlider, lightningSelected);
        setVisible(lightningAlphaSlider, lightningSelected);
        setVisible(lightningInteriorOpacitySlider, lightningSelected);
        setVisible(lightningSphereRadiusSlider, lightningSelected);
        setVisible(lightningSphereOpacitySlider, lightningSelected);
        setVisible(lightningConeHeightSlider, lightningSelected);
        setVisible(lightningConeRadiusSlider, lightningSelected);
        setVisible(lightningConeOpacitySlider, lightningSelected);
        setVisible(lightningSpotCountSlider, lightningSelected);
        setVisible(lightningSpotSizeSlider, lightningSelected);
        setVisible(lightningSpotOpacitySlider, lightningSelected);
        setVisible(lightningSecondarySizeSlider, lightningSelected);
        setVisible(lightningLifetimeBox, lightningSelected);
        setVisible(lightningSideCountBox, lightningSelected);
        setVisible(lightningRingColorBox, lightningSelected);
        setVisible(lightningSphereColorBox, lightningSelected);
        setVisible(lightningConeColorBox, lightningSelected);
        setVisible(lightningSpotColorBox, lightningSelected);
        setVisible(lightningSecondaryCountBox, lightningSelected);
        setVisible(lightningSecondaryDelayBox, lightningSelected);
        setVisible(lightningSoundVolumeBox, lightningSelected);
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible) {
        if (widget != null) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private void refreshLightningControls() {
        if (lightningEnabledButton == null) {
            return;
        }
        ArcaneBeamConfig.LightningStrikeSettings settings = lightningSettings();
        lightningEnabledButton.setMessage(new TextComponent("Replacement: " + (settings.enabled ? "On" : "Off")));
        lightningShaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + lightningShaderCompatibility().label));
        lightningFullbrightButton.setMessage(new TextComponent("Fullbright: " + (settings.fullbright ? "On" : "Off")));
        lightningSoundButton.setMessage(new TextComponent("Sound: " + lightningSoundMode().label));
        lightningStartRadiusSlider.refresh();
        lightningEndRadiusSlider.refresh();
        lightningThicknessSlider.refresh();
        lightningAlphaSlider.refresh();
        lightningInteriorOpacitySlider.refresh();
        lightningSphereRadiusSlider.refresh();
        lightningSphereOpacitySlider.refresh();
        lightningConeHeightSlider.refresh();
        lightningConeRadiusSlider.refresh();
        lightningConeOpacitySlider.refresh();
        lightningSpotCountSlider.refresh();
        lightningSpotSizeSlider.refresh();
        lightningSpotOpacitySlider.refresh();
        lightningSecondarySizeSlider.refresh();
        lightningLifetimeBox.setValue(Integer.toString(settings.lifetimeTicks));
        lightningSideCountBox.setValue(Integer.toString(settings.ringSideCount));
        lightningRingColorBox.setValue(formatColor(settings.ringColor));
        lightningSphereColorBox.setValue(formatColor(settings.sphereColor));
        lightningConeColorBox.setValue(formatColor(settings.coneColor));
        lightningSpotColorBox.setValue(formatColor(settings.spotColor));
        lightningSecondaryCountBox.setValue(Integer.toString(settings.secondaryRippleCount));
        lightningSecondaryDelayBox.setValue(Integer.toString(settings.secondaryRippleDelayTicks));
        refreshLightningSoundVolumeBox();
    }

    private void cycleShaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility current = shaderCompatibility();
        settings().shaderCompatibility = current == ArcaneBeamConfig.ShaderCompatibility.ON
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

    private void cycleFadeInStyle() {
        ArcaneBeamConfig.FadeInStyle current = fadeInStyle();
        settings().fadeInStyle = current == ArcaneBeamConfig.FadeInStyle.FADE ? ArcaneBeamConfig.FadeInStyle.GROW.id : ArcaneBeamConfig.FadeInStyle.FADE.id;
    }

    private void cycleFadeOutStyle() {
        ArcaneBeamConfig.FadeOutStyle current = fadeOutStyle();
        settings().fadeOutStyle = current == ArcaneBeamConfig.FadeOutStyle.FADE ? ArcaneBeamConfig.FadeOutStyle.SHRINK.id : ArcaneBeamConfig.FadeOutStyle.FADE.id;
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

    private void updateSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }

        try {
            settings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFadeInTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            settings().fadeInTicks = clampTicks(Integer.parseInt(value));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFadeOutTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            settings().fadeOutTicks = clampTicks(Integer.parseInt(value));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningLifetimeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().lifetimeTicks = clamp(Integer.parseInt(value), 1, 200);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSideCountFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().ringSideCount = clamp(Integer.parseInt(value), 8, 96);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSecondaryCountFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().secondaryRippleCount = clamp(Integer.parseInt(value), 0, 4);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSecondaryDelayFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().secondaryRippleDelayTicks = clamp(Integer.parseInt(value), 0, 40);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }

        try {
            lightningSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningRingColorFromText(String value) {
        updateLightningColor(value, 0);
    }

    private void updateLightningSphereColorFromText(String value) {
        updateLightningColor(value, 1);
    }

    private void updateLightningConeColorFromText(String value) {
        updateLightningColor(value, 2);
    }

    private void updateLightningSpotColorFromText(String value) {
        updateLightningColor(value, 3);
    }

    private void updateLightningColor(String value, int slot) {
        if (value == null) {
            return;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }
        try {
            int color = Integer.parseInt(normalized, 16);
            switch (slot) {
                case 0 -> lightningSettings().ringColor = color;
                case 1 -> {
                    lightningSettings().sphereColor = color;
                    lightningSettings().centerFlashColor = color;
                }
                case 2 -> lightningSettings().coneColor = color;
                case 3 -> lightningSettings().spotColor = color;
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

    private void nudgeSoundVolume(double amount) {
        settings().soundVolume = clampSoundVolume((float) roundOffset(settings().soundVolume + amount));
    }

    private void nudgeLightningSoundVolume(double amount) {
        lightningSettings().soundVolume = clampSoundVolume((float) roundOffset(lightningSettings().soundVolume + amount));
    }

    private void nudgeFadeInTicks(int amount) {
        settings().fadeInTicks = clampTicks(settings().fadeInTicks + amount);
    }

    private void nudgeFadeOutTicks(int amount) {
        settings().fadeOutTicks = clampTicks(settings().fadeOutTicks + amount);
    }

    private ArcaneBeamConfig.BeamSettings settings() {
        return railSelected ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
    }

    private ArcaneBeamConfig.LightningStrikeSettings lightningSettings() {
        return ArcaneBeamConfig.INSTANCE.lightningStrike;
    }

    private void refreshSoundVolumeBox() {
        if (soundVolumeBox != null) {
            soundVolumeBox.setValue(formatOffset(settings().soundVolume));
        }
    }

    private void refreshLightningSoundVolumeBox() {
        if (lightningSoundVolumeBox != null) {
            lightningSoundVolumeBox.setValue(formatOffset(lightningSettings().soundVolume));
        }
    }

    private void refreshFadeTickBoxes() {
        if (fadeInTicksBox != null) {
            fadeInTicksBox.setValue(Integer.toString(settings().fadeInTicks));
        }
        if (fadeOutTicksBox != null) {
            fadeOutTicksBox.setValue(Integer.toString(settings().fadeOutTicks));
        }
    }

    private int[] activeColors() {
        return glowColorsSelected ? settings().glowColors : settings().colors;
    }

    private int selectedColor() {
        if (lightningSelected) {
            return switch (selectedSlot) {
                case 1 -> lightningSettings().sphereColor;
                case 2 -> lightningSettings().coneColor;
                case 3 -> lightningSettings().spotColor;
                default -> lightningSettings().ringColor;
            };
        }
        return activeColors()[selectedSlot];
    }

    private void setSelectedColor(int color) {
        if (lightningSelected) {
            switch (selectedSlot) {
                case 1 -> {
                    lightningSettings().sphereColor = color;
                    lightningSettings().centerFlashColor = color;
                }
                case 2 -> lightningSettings().coneColor = color;
                case 3 -> lightningSettings().spotColor = color;
                default -> lightningSettings().ringColor = color;
            }
            return;
        }
        activeColors()[selectedSlot] = color;
        syncPrimaryColors();
    }

    private void syncPrimaryColors() {
        settings().color = settings().colors[0];
        settings().glowColor = settings().glowColors[0];
    }

    private List<String> profileNames() {
        return lightningSelected ? ArcaneBeamConfig.lightningProfileNames() : ArcaneBeamConfig.profileNames(railSelected);
    }

    private String selectedProfileName() {
        return lightningSelected ? ArcaneBeamConfig.selectedLightningProfileName() : ArcaneBeamConfig.selectedProfileName(railSelected);
    }

    private void selectProfile(String profileName) {
        if (lightningSelected) {
            ArcaneBeamConfig.selectLightningProfile(profileName);
        } else {
            ArcaneBeamConfig.selectProfile(railSelected, profileName);
        }
    }

    private String profileLabel() {
        if (lightningSelected) {
            return "Lightning Profiles";
        }
        return railSelected ? "Rail Profiles" : "Arcane Profiles";
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
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(settings().shaderCompatibility);
        return compatibility == null ? ArcaneBeamConfig.ShaderCompatibility.OFF : compatibility;
    }

    private ArcaneBeamConfig.ShaderCompatibility lightningShaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(lightningSettings().shaderCompatibility);
        return compatibility == null ? ArcaneBeamConfig.ShaderCompatibility.OFF : compatibility;
    }

    private ArcaneBeamConfig.LightningSoundMode lightningSoundMode() {
        ArcaneBeamConfig.LightningSoundMode mode = ArcaneBeamConfig.LightningSoundMode.fromId(lightningSettings().soundMode);
        return mode == null ? ArcaneBeamConfig.LightningSoundMode.DEFAULT : mode;
    }

    private void cycleLightningSound() {
        ArcaneBeamConfig.LightningSoundMode[] modes = ArcaneBeamConfig.LightningSoundMode.values();
        ArcaneBeamConfig.LightningSoundMode current = lightningSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        lightningSettings().soundMode = modes[next].id;
    }

    private ArcaneBeamConfig.FadeInStyle fadeInStyle() {
        ArcaneBeamConfig.FadeInStyle style = ArcaneBeamConfig.FadeInStyle.fromId(settings().fadeInStyle);
        return style == null ? ArcaneBeamConfig.FadeInStyle.FADE : style;
    }

    private ArcaneBeamConfig.FadeOutStyle fadeOutStyle() {
        ArcaneBeamConfig.FadeOutStyle style = ArcaneBeamConfig.FadeOutStyle.fromId(settings().fadeOutStyle);
        return style == null ? ArcaneBeamConfig.FadeOutStyle.SHRINK : style;
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

    private static float clampSoundVolume(float value) {
        return Math.max(0.0F, Math.min(2.0F, value));
    }

    private static int clampTicks(int value) {
        return Math.max(0, Math.min(99, value));
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
            } else if ("Spot Count".equals(label)) {
                setMessage(new TextComponent(label + ": " + Math.round(actualValue())));
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
