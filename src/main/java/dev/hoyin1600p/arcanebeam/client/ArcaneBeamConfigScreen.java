package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ArcaneBeamConfigScreen extends Screen {
    private static final int MIN_LAYOUT_WIDTH = 960;
    private static final int MIN_LAYOUT_HEIGHT = 720;
    private static final int TOP_BAR_Y = 18;
    private static final int TOP_BAR_HEIGHT = 48;
    private static final int TOP_BAR_WIDTH = 760;
    private static final int TOP_BAR_BRAND_WIDTH = 150;
    private static final int DRAWER_MARGIN = 24;
    private static final int DRAWER_EXPANDED_HEIGHT = 264;
    private static final int DRAWER_COLLAPSED_HEIGHT = 34;
    private static final int CLEAN_VIEW_HANDLE_WIDTH = 176;
    private static final int CLEAN_VIEW_HANDLE_HEIGHT = 28;
    private static final int PROFILE_PANEL_WIDTH = 152;
    private static final int PROFILE_ROW_HEIGHT = 18;
    private static final int COLOR_CARD_WIDTH = 414;
    private static final int PALETTE_WIDTH = 180;
    private static final int PALETTE_HEIGHT = 110;
    private static final int BRIGHTNESS_WIDTH = 14;
    private static final int SLOT_PREVIEW_WIDTH = 20;
    private static final int SLOT_HEX_WIDTH = 54;
    private static final int SLOT_GAP = 12;
    private static final int SLOT_INNER_GAP = 6;
    private static final int COLOR_ROW_GAP = 6;
    private static final int GLOW_ROW_GAP = 26;
    private static final int CONTROL_CARD_GAP = 14;
    private static final int BEAM_SHAPE_CARD_WIDTH = 224;
    private static final int BEAM_MOTION_CARD_WIDTH = 196;
    private static final int BEAM_ADVANCED_CARD_WIDTH = 184;
    private static final int BEAM_AUDIO_CARD_WIDTH = 208;
    private static final int BEAM_TRANSITION_CARD_WIDTH = 246;
    private static final int BEAM_ORIGIN_CARD_WIDTH = 304;
    private static final int DENSE_SETUP_CARD_WIDTH = 184;
    private static final int DENSE_NARROW_CARD_WIDTH = 190;
    private static final int DENSE_MEDIUM_CARD_WIDTH = 220;
    private static final int DENSE_WIDE_CARD_WIDTH = 252;
    private static final int DENSE_AUDIO_CARD_WIDTH = 196;

    private final List<EditBox> colorBoxes = new ArrayList<>();
    private final List<EditBox> glowColorBoxes = new ArrayList<>();
    private final List<EditBox> altarColorBoxes = new ArrayList<>();
    private final List<EditBox> stormArrowColorBoxes = new ArrayList<>();
    private final List<EditBox> originBoxes = new ArrayList<>();
    private final List<Button> originLabelButtons = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<ConfigTabId> tabButtonIds = new ArrayList<>();
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
    private SettingSlider altarCornerRadiusSlider;
    private SettingSlider altarCornerOpacitySlider;
    private SettingSlider altarCenterHeightSlider;
    private SettingSlider altarCenterFadeSlider;
    private SettingSlider altarCenterBottomRadiusSlider;
    private SettingSlider altarCenterTopRadiusSlider;
    private SettingSlider altarCenterOpacitySlider;
    private SettingSlider altarGlowHeightSlider;
    private SettingSlider altarGlowFadeSlider;
    private SettingSlider altarGlowBottomRadiusSlider;
    private SettingSlider altarGlowTopRadiusSlider;
    private SettingSlider altarGlowOpacitySlider;
    private SettingSlider altarGlowRotationSlider;
    private SettingSlider stormArrowCircleAlphaSlider;
    private SettingSlider stormArrowCircleThicknessSlider;
    private SettingSlider stormArrowBlasterAlphaSlider;
    private SettingSlider stormArrowBlasterWidthSlider;
    private SettingSlider stormArrowSegmentLengthSlider;
    private SettingSlider stormArrowSegmentGapSlider;
    private SettingSlider stormArrowImpactFlashSizeSlider;
    private Button soundButton;
    private Button handButton;
    private Button shaderCompatibilityButton;
    private Button fadeInModeButton;
    private Button fadeOutModeButton;
    private Button profileAddButton;
    private Button profileDropdownButton;
    private Button drawerToggleButton;
    private Button hideChromeButton;
    private Button doneButton;
    private Button tabScrollLeftButton;
    private Button tabScrollRightButton;
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
    private Button altarEnabledButton;
    private Button altarShaderCompatibilityButton;
    private Button altarFullbrightButton;
    private Button altarOriginMarkersButton;
    private Button altarSoundButton;
    private EditBox altarVerticalTicksBox;
    private EditBox altarConvergeTicksBox;
    private EditBox altarCenterGrowTicksBox;
    private EditBox altarOriginHeightBox;
    private EditBox altarOriginRadiusBox;
    private EditBox altarSoundVolumeBox;
    private Button stormArrowEnabledButton;
    private Button stormArrowTargetingCircleButton;
    private Button stormArrowActualRadiusButton;
    private Button stormArrowShaderCompatibilityButton;
    private Button stormArrowFullbrightButton;
    private Button stormArrowImpactFlashButton;
    private Button stormArrowSoundButton;
    private Button stormArrowProjectileSoundButton;
    private EditBox stormArrowLifetimeBox;
    private EditBox stormArrowOriginHeightBox;
    private EditBox archonMissileRadiusBox;
    private EditBox stormArrowSoundVolumeBox;
    private EditBox stormArrowAudioRangeBox;
    private EditBox pickerHexBox;
    private boolean profileDropdownOpen;
    private boolean railSelected;
    private boolean lightningSelected;
    private boolean vaultAltarSelected;
    private boolean stormArrowSelected;
    private boolean smiteSelected;
    private boolean archonSelected;
    private boolean draggingPalette;
    private boolean draggingBrightness;
    private boolean colorPickerOpen;
    private boolean refreshingPickerHexBox;
    private boolean gameHudSuppressed;
    private boolean previousHideGui;
    private int brightnessDragBaseColor;
    private int selectedSlot;
    private boolean glowColorsSelected;
    private int paletteX;
    private int paletteY;
    private int layoutWidth;
    private int layoutHeight;
    private float layoutScale = 1.0F;
    private DrawerState drawerState = DrawerState.EXPANDED;
    private DrawerState drawerStateBeforeCleanPreview = DrawerState.EXPANDED;
    private ConfigTabId activeTab = ConfigTabId.ARCANE;
    private int tabScrollOffset;
    private int controlScrollOffset;

    private static final TabDescriptor[] TABS = {
            new TabDescriptor(ConfigTabId.ARCANE, "Arcane", 58, ArcaneBeamGuiTheme.GOLD),
            new TabDescriptor(ConfigTabId.RAIL, "Rail", 50, ArcaneBeamGuiTheme.ORANGE),
            new TabDescriptor(ConfigTabId.LIGHTNING_STRIKE, "Lightning Strike", 104, ArcaneBeamGuiTheme.CYAN),
            new TabDescriptor(ConfigTabId.VAULT_ALTAR, "Vault Altar", 84, ArcaneBeamGuiTheme.GREEN),
            new TabDescriptor(ConfigTabId.STORM_ARROW, "Storm Arrow", 94, ArcaneBeamGuiTheme.BLUE),
            new TabDescriptor(ConfigTabId.SMITE, "Smite", 56, ArcaneBeamGuiTheme.RED),
            new TabDescriptor(ConfigTabId.ARCHON, "Archon", 70, ArcaneBeamGuiTheme.VIOLET)
    };

    private enum DrawerState {
        EXPANDED,
        COLLAPSED,
        HIDDEN
    }

    private enum ConfigTabId {
        ARCANE,
        RAIL,
        LIGHTNING_STRIKE,
        VAULT_ALTAR,
        STORM_ARROW,
        SMITE,
        ARCHON
    }

    private static final class TabDescriptor {
        private final ConfigTabId id;
        private final String label;
        private final int width;
        private final int accent;

        private TabDescriptor(ConfigTabId id, String label, int width, int accent) {
            this.id = id;
            this.label = label;
            this.width = width;
            this.accent = accent;
        }
    }

    public ArcaneBeamConfigScreen() {
        super(new TextComponent("Arcane Beam"));
    }

    public ArcaneBeamManager.BeamKind previewKind() {
        return railSelected ? ArcaneBeamManager.BeamKind.RAIL : ArcaneBeamManager.BeamKind.ARCANE;
    }

    private boolean stormLikeSelected() {
        return stormArrowSelected || smiteSelected || archonSelected;
    }

    public boolean lightningSelected() {
        return lightningSelected || vaultAltarSelected || stormLikeSelected();
    }

    @Override
    protected void init() {
        updateLayoutScale();
        suppressGameHud();
        colorBoxes.clear();
        glowColorBoxes.clear();
        altarColorBoxes.clear();
        stormArrowColorBoxes.clear();
        originBoxes.clear();
        originLabelButtons.clear();
        tabButtons.clear();
        tabButtonIds.clear();
        profileDropdownOpen = false;
        syncBooleansFromActiveTab();
        updatePalettePosition();

        for (TabDescriptor tab : TABS) {
            this.addTabButton(tab.id, new ThemeButton(0, 0, tab.width, 20, new TextComponent(tab.label), button -> selectTab(tab.id)));
        }
        tabScrollLeftButton = this.addRenderableWidget(new ThemeButton(0, 0, 20, 20, new TextComponent("<"), button -> {
            tabScrollOffset = Math.max(0, tabScrollOffset - 90);
            layoutTabs();
        }));
        tabScrollRightButton = this.addRenderableWidget(new ThemeButton(0, 0, 20, 20, new TextComponent(">"), button -> {
            tabScrollOffset = Math.min(maxTabScrollOffset(), tabScrollOffset + 90);
            layoutTabs();
        }));
        pickerHexBox = new EditBox(this.font, 0, 0, 78, 20, new TextComponent("Selected Color"));
        pickerHexBox.setMaxLength(7);
        pickerHexBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        pickerHexBox.setResponder(this::updatePickerHexFromText);
        this.addRenderableWidget(pickerHexBox);

        int profileX = profilePanelX();
        int profileY = profilePanelY();
        profileNameBox = new EditBox(this.font, profileX, profileY + 18, 84, 20, new TextComponent("Profile Name"));
        profileNameBox.setMaxLength(24);
        profileNameBox.setFilter(value -> value == null || !value.contains("\n") && !value.contains("\r") && !value.contains("\t"));
        this.addRenderableWidget(profileNameBox);
        profileAddButton = this.addRenderableWidget(new ThemeButton(profileX + 88, profileY + 18, 44, 20, new TextComponent("Add"), button -> addProfile()));
        profileDropdownButton = this.addRenderableWidget(new ThemeButton(profileX, profileY + 42, PROFILE_PANEL_WIDTH, 20, TextComponent.EMPTY, button -> profileDropdownOpen = !profileDropdownOpen));

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
        shaderCompatibilityButton = this.addRenderableWidget(new ThemeButton(sliderX, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleShaderCompatibility();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        handButton = this.addRenderableWidget(new ThemeButton(sliderX + 158, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleHand();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        int soundRowY = sliderY + 168;
        soundButton = this.addRenderableWidget(new ThemeButton(sliderX, soundRowY, 184, 20, TextComponent.EMPTY, button -> {
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
        fadeInModeButton = this.addRenderableWidget(new ThemeButton(sliderX, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
            cycleFadeInStyle();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        fadeInTicksBox = new EditBox(this.font, sliderX + 128, transitionRowY, 24, 20, new TextComponent("In Ticks"));
        fadeInTicksBox.setMaxLength(2);
        fadeInTicksBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        fadeInTicksBox.setResponder(this::updateFadeInTicksFromText);
        this.addRenderableWidget(fadeInTicksBox);
        fadeOutModeButton = this.addRenderableWidget(new ThemeButton(sliderX + 160, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
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
        addVaultAltarControls(sliderX, sliderY);
        addStormArrowControls(sliderX, sliderY);

        drawerToggleButton = this.addRenderableWidget(new ThemeButton(layoutWidth / 2 - 74, drawerY() + 10, 84, 20, TextComponent.EMPTY, button -> {
            drawerState = drawerState == DrawerState.EXPANDED ? DrawerState.COLLAPSED : DrawerState.EXPANDED;
            profileDropdownOpen = false;
            closeColorPicker();
            refreshControls();
        }));
        hideChromeButton = this.addRenderableWidget(new ThemeButton(layoutWidth / 2 + 18, drawerY() + 10, 74, 20, new TextComponent("Clean View"), button -> enterCleanPreviewMode()));
        doneButton = this.addRenderableWidget(new ThemeButton(layoutWidth / 2 + 100, drawerY() + 10, 74, 20, new TextComponent("Done"), button -> onClose()));
        styleEditBoxes();
        refreshControls();
    }

    private void addLightningControls(int x, int y) {
        int lightningColorY = beamRowY();
        lightningRingColorBox = addLightningColorBox(slotStartX(0), lightningColorY, "Ring Color", this::updateLightningRingColorFromText);
        lightningSphereColorBox = addLightningColorBox(slotStartX(1), lightningColorY, "Sphere Color", this::updateLightningSphereColorFromText);
        lightningConeColorBox = addLightningColorBox(slotStartX(2), lightningColorY, "Cone Color", this::updateLightningConeColorFromText);
        lightningSpotColorBox = addLightningColorBox(slotStartX(3), lightningColorY, "Spot Color", this::updateLightningSpotColorFromText);

        lightningEnabledButton = this.addRenderableWidget(new ThemeButton(x, y, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().enabled = !lightningSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        lightningShaderCompatibilityButton = this.addRenderableWidget(new ThemeButton(x + 158, y, 150, 20, TextComponent.EMPTY, button -> {
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

        lightningFullbrightButton = this.addRenderableWidget(new ThemeButton(x, y + 240, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().fullbright = !lightningSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        lightningSoundButton = this.addRenderableWidget(new ThemeButton(x, y + 268, 150, 20, TextComponent.EMPTY, button -> {
            cycleLightningSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));

        lightningLifetimeBox = addLightningNumberBox(x, y + 296, 54, 3, "Lifetime", "[0-9]{0,3}", this::updateLightningLifetimeFromText);
        lightningSideCountBox = addLightningNumberBox(x + 104, y + 296, 54, 2, "Sides", "[0-9]{0,2}", this::updateLightningSideCountFromText);
        lightningSecondaryCountBox = addLightningNumberBox(x + 208, y + 296, 54, 1, "Ripples", "[0-9]{0,1}", this::updateLightningSecondaryCountFromText);
        lightningSecondaryDelayBox = addLightningNumberBox(x, y + 324, 54, 2, "Delay", "[0-9]{0,2}", this::updateLightningSecondaryDelayFromText);
        lightningSoundVolumeBox = addLightningSoundVolumeBox(x + 208, y + 324);
    }

    private void addVaultAltarControls(int x, int y) {
        int colorY = beamRowY();
        addAltarColorBox(slotStartX(0), colorY, "Corner 1", 0);
        addAltarColorBox(slotStartX(1), colorY, "Corner 2", 1);
        addAltarColorBox(slotStartX(2), colorY, "Center 1", 2);
        addAltarColorBox(slotStartX(3), colorY, "Center 2", 3);
        addAltarColorBox(slotStartX(1), altarSecondColorRowY(), "Glow 1", 4);
        addAltarColorBox(slotStartX(2), altarSecondColorRowY(), "Glow 2", 5);

        int controlY = altarSecondColorRowY() + 52;
        altarEnabledButton = this.addRenderableWidget(new ThemeButton(x, controlY, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().enabled = !vaultAltarSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        altarShaderCompatibilityButton = this.addRenderableWidget(new ThemeButton(x + 158, controlY, 150, 20, TextComponent.EMPTY, button -> {
            ArcaneBeamConfig.ShaderCompatibility current = vaultAltarShaderCompatibility();
            vaultAltarSettings().shaderCompatibility = current == ArcaneBeamConfig.ShaderCompatibility.ON
                    ? ArcaneBeamConfig.ShaderCompatibility.OFF.id
                    : ArcaneBeamConfig.ShaderCompatibility.ON.id;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        altarCornerRadiusSlider = new SettingSlider(x, controlY + 24, 150, 20, "Corner Radius", 0.005D, 0.20D, () -> vaultAltarSettings().cornerRadius, value -> vaultAltarSettings().cornerRadius = (float) value);
        altarCornerOpacitySlider = new SettingSlider(x + 158, controlY + 24, 150, 20, "Corner Opacity", 0.0D, 1.0D, () -> vaultAltarSettings().cornerOpacity, value -> vaultAltarSettings().cornerOpacity = (float) value);
        altarCenterBottomRadiusSlider = new SettingSlider(x, controlY + 48, 150, 20, "Center Bottom", 0.005D, 0.50D, () -> vaultAltarSettings().centerBottomRadius, value -> vaultAltarSettings().centerBottomRadius = (float) value);
        altarCenterTopRadiusSlider = new SettingSlider(x + 158, controlY + 48, 150, 20, "Center Top", 0.005D, 0.50D, () -> vaultAltarSettings().centerTopRadius, value -> vaultAltarSettings().centerTopRadius = (float) value);
        altarCenterHeightSlider = new SettingSlider(x, controlY + 72, 150, 20, "Center Height", 0.1D, 3.0D, () -> vaultAltarSettings().centerHeight, value -> vaultAltarSettings().centerHeight = (float) value);
        altarCenterFadeSlider = new SettingSlider(x + 158, controlY + 72, 150, 20, "Center Fade", 0.05D, 3.0D, () -> vaultAltarSettings().centerFadeHeight, value -> vaultAltarSettings().centerFadeHeight = (float) value);
        altarCenterOpacitySlider = new SettingSlider(x, controlY + 96, 150, 20, "Center Opacity", 0.0D, 1.0D, () -> vaultAltarSettings().centerOpacity, value -> vaultAltarSettings().centerOpacity = (float) value);
        altarGlowOpacitySlider = new SettingSlider(x + 158, controlY + 96, 150, 20, "Glow Opacity", 0.0D, 1.0D, () -> vaultAltarSettings().centerGlowOpacity, value -> vaultAltarSettings().centerGlowOpacity = (float) value);
        altarGlowBottomRadiusSlider = new SettingSlider(x, controlY + 120, 150, 20, "Glow Bottom", 0.005D, 0.75D, () -> vaultAltarSettings().centerGlowBottomRadius, value -> vaultAltarSettings().centerGlowBottomRadius = (float) value);
        altarGlowTopRadiusSlider = new SettingSlider(x + 158, controlY + 120, 150, 20, "Glow Top", 0.005D, 0.75D, () -> vaultAltarSettings().centerGlowTopRadius, value -> vaultAltarSettings().centerGlowTopRadius = (float) value);
        altarGlowHeightSlider = new SettingSlider(x, controlY + 144, 150, 20, "Glow Height", 0.1D, 3.0D, () -> vaultAltarSettings().centerGlowHeight, value -> vaultAltarSettings().centerGlowHeight = (float) value);
        altarGlowFadeSlider = new SettingSlider(x + 158, controlY + 144, 150, 20, "Glow Fade", 0.05D, 3.0D, () -> vaultAltarSettings().centerGlowFadeHeight, value -> vaultAltarSettings().centerGlowFadeHeight = (float) value);
        altarGlowRotationSlider = new SettingSlider(x, controlY + 168, 308, 20, "Glow Rotation", 0.0D, 120.0D, () -> vaultAltarSettings().centerGlowRotationRpm, value -> vaultAltarSettings().centerGlowRotationRpm = (float) value);
        this.addRenderableWidget(altarCornerRadiusSlider);
        this.addRenderableWidget(altarCornerOpacitySlider);
        this.addRenderableWidget(altarCenterBottomRadiusSlider);
        this.addRenderableWidget(altarCenterTopRadiusSlider);
        this.addRenderableWidget(altarCenterHeightSlider);
        this.addRenderableWidget(altarCenterFadeSlider);
        this.addRenderableWidget(altarCenterOpacitySlider);
        this.addRenderableWidget(altarGlowOpacitySlider);
        this.addRenderableWidget(altarGlowBottomRadiusSlider);
        this.addRenderableWidget(altarGlowTopRadiusSlider);
        this.addRenderableWidget(altarGlowHeightSlider);
        this.addRenderableWidget(altarGlowFadeSlider);
        this.addRenderableWidget(altarGlowRotationSlider);

        altarFullbrightButton = this.addRenderableWidget(new ThemeButton(x, controlY + 196, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().fullbright = !vaultAltarSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        altarSoundButton = this.addRenderableWidget(new ThemeButton(x + 158, controlY + 196, 150, 20, TextComponent.EMPTY, button -> {
            cycleVaultAltarSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        altarOriginMarkersButton = this.addRenderableWidget(new ThemeButton(x, controlY + 280, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().originMarkersEnabled = !vaultAltarSettings().originMarkersEnabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        altarVerticalTicksBox = addAltarNumberBox(x, controlY + 224, 42, 2, "Vertical", "[0-9]{0,2}", this::updateAltarVerticalTicksFromText);
        altarConvergeTicksBox = addAltarNumberBox(x + 108, controlY + 224, 42, 3, "Converge", "[0-9]{0,3}", this::updateAltarConvergeTicksFromText);
        altarCenterGrowTicksBox = addAltarNumberBox(x + 222, controlY + 224, 42, 3, "Grow", "[0-9]{0,3}", this::updateAltarCenterGrowTicksFromText);
        altarOriginHeightBox = addAltarDecimalBox(x, controlY + 252, this::updateAltarOriginHeightFromText);
        altarOriginRadiusBox = addAltarDecimalBox(x + 158, controlY + 252, this::updateAltarOriginRadiusFromText);
        altarSoundVolumeBox = addAltarSoundVolumeBox(x + 208, controlY + 280);
    }

    private void addStormArrowControls(int x, int y) {
        int colorY = beamRowY();
        addStormArrowColorBox(slotStartX(0), colorY, "Circle", 0);
        addStormArrowColorBox(slotStartX(1), colorY, "Blaster", 1);
        addStormArrowColorBox(slotStartX(2), colorY, "Core", 2);
        addStormArrowColorBox(slotStartX(3), colorY, "Flash", 3);

        stormArrowEnabledButton = this.addRenderableWidget(new ThemeButton(x, y, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().enabled = !stormArrowSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowShaderCompatibilityButton = this.addRenderableWidget(new ThemeButton(x + 158, y, 150, 20, TextComponent.EMPTY, button -> {
            ArcaneBeamConfig.ShaderCompatibility current = stormArrowShaderCompatibility();
            stormArrowSettings().shaderCompatibility = current == ArcaneBeamConfig.ShaderCompatibility.ON
                    ? ArcaneBeamConfig.ShaderCompatibility.OFF.id
                    : ArcaneBeamConfig.ShaderCompatibility.ON.id;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowTargetingCircleButton = this.addRenderableWidget(new ThemeButton(x, y + 24, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().showTargetingCircle = !stormArrowSettings().showTargetingCircle;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowActualRadiusButton = this.addRenderableWidget(new ThemeButton(x + 158, y + 24, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().useActualRadius = !stormArrowSettings().useActualRadius;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowCircleAlphaSlider = new SettingSlider(x, y + 48, 150, 20, "Circle Alpha", 0.0D, 1.0D, () -> stormArrowSettings().circleAlpha, value -> stormArrowSettings().circleAlpha = (float) value);
        stormArrowCircleThicknessSlider = new SettingSlider(x + 158, y + 48, 150, 20, "Circle Width", 0.02D, 1.0D, () -> stormArrowSettings().circleThickness, value -> stormArrowSettings().circleThickness = (float) value);
        stormArrowBlasterAlphaSlider = new SettingSlider(x, y + 72, 150, 20, "Blaster Alpha", 0.0D, 1.0D, () -> stormArrowSettings().blasterAlpha, value -> stormArrowSettings().blasterAlpha = (float) value);
        stormArrowBlasterWidthSlider = new SettingSlider(x + 158, y + 72, 150, 20, "Blaster Width", 0.01D, 1.0D, () -> stormArrowSettings().blasterWidth, value -> stormArrowSettings().blasterWidth = (float) value);
        stormArrowSegmentLengthSlider = new SettingSlider(x, y + 96, 150, 20, "Bolt Length", 0.1D, 12.0D, () -> stormArrowSettings().segmentLength, value -> stormArrowSettings().segmentLength = (float) value);
        stormArrowSegmentGapSlider = new SettingSlider(x + 158, y + 96, 150, 20, "Segment Gap", 0.0D, 12.0D, () -> stormArrowSettings().segmentGap, value -> stormArrowSettings().segmentGap = (float) value);
        stormArrowImpactFlashSizeSlider = new SettingSlider(x, y + 120, 308, 20, "Impact Flash Size", 0.05D, 4.0D, () -> stormArrowSettings().impactFlashSize, value -> stormArrowSettings().impactFlashSize = (float) value);
        this.addRenderableWidget(stormArrowCircleAlphaSlider);
        this.addRenderableWidget(stormArrowCircleThicknessSlider);
        this.addRenderableWidget(stormArrowBlasterAlphaSlider);
        this.addRenderableWidget(stormArrowBlasterWidthSlider);
        this.addRenderableWidget(stormArrowSegmentLengthSlider);
        this.addRenderableWidget(stormArrowSegmentGapSlider);
        this.addRenderableWidget(stormArrowImpactFlashSizeSlider);

        stormArrowFullbrightButton = this.addRenderableWidget(new ThemeButton(x, y + 148, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().fullbright = !stormArrowSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowImpactFlashButton = this.addRenderableWidget(new ThemeButton(x + 158, y + 148, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().impactFlashEnabled = !stormArrowSettings().impactFlashEnabled;
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowSoundButton = this.addRenderableWidget(new ThemeButton(x, y + 172, 150, 20, TextComponent.EMPTY, button -> {
            cycleStormArrowSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowProjectileSoundButton = this.addRenderableWidget(new ThemeButton(x + 158, y + 172, 150, 20, TextComponent.EMPTY, button -> {
            cycleStormArrowProjectileSound();
            refreshControls();
            ArcaneBeamConfig.save();
        }));
        stormArrowLifetimeBox = addStormArrowNumberBox(x, y + 200, 54, 2, "Lifetime", "[0-9]{0,2}", this::updateStormArrowLifetimeFromText);
        stormArrowOriginHeightBox = addStormArrowNumberBox(x + 158, y + 200, 54, 4, "Height", "[0-9]{0,2}(\\.[0-9]?)?", this::updateStormArrowOriginHeightFromText);
        archonMissileRadiusBox = addStormArrowNumberBox(x + 158, y + 228, 54, 4, "Radius", "[0-9]{0,2}(\\.[0-9]?)?", this::updateArchonMissileRadiusFromText);
        stormArrowAudioRangeBox = addStormArrowAudioRangeBox(x, y + 228);
        stormArrowSoundVolumeBox = addStormArrowSoundVolumeBox(x + 208, y + 256);
    }

    private void syncBooleansFromActiveTab() {
        railSelected = activeTab == ConfigTabId.RAIL;
        lightningSelected = activeTab == ConfigTabId.LIGHTNING_STRIKE;
        vaultAltarSelected = activeTab == ConfigTabId.VAULT_ALTAR;
        stormArrowSelected = activeTab == ConfigTabId.STORM_ARROW;
        smiteSelected = activeTab == ConfigTabId.SMITE;
        archonSelected = activeTab == ConfigTabId.ARCHON;
    }

    private void selectTab(ConfigTabId tab) {
        activeTab = tab;
        syncBooleansFromActiveTab();
        controlScrollOffset = 0;
        selectedSlot = 0;
        glowColorsSelected = false;
        profileDropdownOpen = false;
        closeColorPicker();
        ensureActiveTabVisible();
        refreshControls();
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

    private Button addTabButton(ConfigTabId id, Button button) {
        tabButtons.add(button);
        tabButtonIds.add(id);
        return this.addRenderableWidget(button);
    }

    private int topBarY() {
        return TOP_BAR_Y;
    }

    private int topBarWidth() {
        return Math.min(TOP_BAR_WIDTH, layoutWidth - DRAWER_MARGIN * 2);
    }

    private int topBarX() {
        return layoutWidth / 2 - topBarWidth() / 2;
    }

    private int tabAreaX() {
        return topBarX() + TOP_BAR_BRAND_WIDTH;
    }

    private int tabAreaWidth() {
        return topBarWidth() - TOP_BAR_BRAND_WIDTH - 18;
    }

    private int drawerX() {
        return DRAWER_MARGIN;
    }

    private int drawerY() {
        return layoutHeight - drawerHeight() - DRAWER_MARGIN;
    }

    private int drawerWidth() {
        return layoutWidth - DRAWER_MARGIN * 2;
    }

    private int collapsedDrawerWidth() {
        return Math.min(620, layoutWidth - DRAWER_MARGIN * 2);
    }

    private int collapsedDrawerX() {
        return layoutWidth / 2 - collapsedDrawerWidth() / 2;
    }

    private int drawerHeight() {
        return drawerState == DrawerState.COLLAPSED ? DRAWER_COLLAPSED_HEIGHT : DRAWER_EXPANDED_HEIGHT;
    }

    private int drawerContentY() {
        return drawerY() + 66;
    }

    private int drawerContentHeight() {
        return drawerY() + drawerHeight() - drawerContentY() - 16;
    }

    private int profileCardX() {
        return drawerX() + 20;
    }

    private int profileCardY() {
        return drawerContentY();
    }

    private int profileCardWidth() {
        return PROFILE_PANEL_WIDTH + 24;
    }

    private int colorCardX() {
        return profileCardX() + profileCardWidth() + 16;
    }

    private int colorCardY() {
        return drawerContentY();
    }

    private int colorCardWidth() {
        return COLOR_CARD_WIDTH;
    }

    private int controlCardX() {
        return colorCardX() + colorCardWidth() + 16;
    }

    private int controlCardY() {
        return drawerContentY();
    }

    private int controlCardWidth() {
        return drawerX() + drawerWidth() - controlCardX() - 20;
    }

    private int controlViewportX() {
        return controlCardX() + 12;
    }

    private int controlViewportRight() {
        return controlCardX() + controlCardWidth() - 12;
    }

    private int controlViewportWidth() {
        return Math.max(0, controlViewportRight() - controlViewportX());
    }

    private int controlContentX() {
        return controlViewportX() + 8 - controlScrollOffset;
    }

    private int[] currentControlCardWidths() {
        if (lightningSelected) {
            return new int[]{
                    DENSE_SETUP_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_AUDIO_CARD_WIDTH
            };
        }
        if (vaultAltarSelected) {
            return new int[]{
                    DENSE_SETUP_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH,
                    DENSE_AUDIO_CARD_WIDTH
            };
        }
        if (archonSelected) {
            return new int[]{
                    DENSE_SETUP_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH,
                    DENSE_AUDIO_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH
            };
        }
        if (smiteSelected) {
            return new int[]{
                    DENSE_SETUP_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_NARROW_CARD_WIDTH
            };
        }
        if (stormArrowSelected) {
            return new int[]{
                    DENSE_SETUP_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_WIDE_CARD_WIDTH,
                    DENSE_MEDIUM_CARD_WIDTH,
                    DENSE_AUDIO_CARD_WIDTH
            };
        }
        return new int[]{
                BEAM_SHAPE_CARD_WIDTH,
                BEAM_MOTION_CARD_WIDTH,
                BEAM_ADVANCED_CARD_WIDTH,
                BEAM_AUDIO_CARD_WIDTH,
                BEAM_TRANSITION_CARD_WIDTH,
                BEAM_ORIGIN_CARD_WIDTH
        };
    }

    private int cardRowWidth(int... widths) {
        int total = 0;
        for (int width : widths) {
            total += width;
        }
        if (widths.length > 1) {
            total += CONTROL_CARD_GAP * (widths.length - 1);
        }
        return total;
    }

    private int beamShapeCardX() {
        return controlContentX();
    }

    private int beamMotionCardX() {
        return beamShapeCardX() + BEAM_SHAPE_CARD_WIDTH + CONTROL_CARD_GAP;
    }

    private int beamAdvancedCardX() {
        return beamMotionCardX() + BEAM_MOTION_CARD_WIDTH + CONTROL_CARD_GAP;
    }

    private int beamAudioCardX() {
        return beamAdvancedCardX() + BEAM_ADVANCED_CARD_WIDTH + CONTROL_CARD_GAP;
    }

    private int beamTransitionCardX() {
        return beamAudioCardX() + BEAM_AUDIO_CARD_WIDTH + CONTROL_CARD_GAP;
    }

    private int beamOriginCardX() {
        return beamTransitionCardX() + BEAM_TRANSITION_CARD_WIDTH + CONTROL_CARD_GAP;
    }

    private int controlCardInnerX(int cardX) {
        return cardX + 12;
    }

    private int controlCardInnerWidth(int cardWidth) {
        return Math.max(44, cardWidth - 24);
    }

    private int controlContentWidth() {
        return cardRowWidth(currentControlCardWidths());
    }

    private int maxControlScrollOffset() {
        return Math.max(0, controlContentWidth() + 8 - controlViewportWidth());
    }

    private void clampControlScrollOffset() {
        controlScrollOffset = clamp(controlScrollOffset, 0, maxControlScrollOffset());
    }

    private int nextControlScrollOffset(boolean forward) {
        List<Integer> stops = controlScrollStops();
        if (stops.isEmpty()) {
            return 0;
        }
        if (forward) {
            for (int stop : stops) {
                if (stop > controlScrollOffset + 2) {
                    return stop;
                }
            }
            return stops.get(stops.size() - 1);
        }
        for (int i = stops.size() - 1; i >= 0; i--) {
            int stop = stops.get(i);
            if (stop < controlScrollOffset - 2) {
                return stop;
            }
        }
        return 0;
    }

    private List<Integer> controlScrollStops() {
        int max = maxControlScrollOffset();
        List<Integer> stops = new ArrayList<>();
        stops.add(0);
        int offset = 0;
        int[] widths = currentControlCardWidths();
        for (int i = 0; i < widths.length - 1; i++) {
            offset += widths[i] + CONTROL_CARD_GAP;
            if (offset <= max) {
                stops.add(offset);
            }
        }
        if (max > 0 && stops.get(stops.size() - 1) != max) {
            stops.add(max);
        }
        return stops;
    }

    private boolean isInsideControlViewport(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, controlViewportX(), controlCardY(), controlViewportWidth(), drawerContentHeight());
    }

    private int profilePanelY() {
        return profileCardY() + 28;
    }

    private int beamControlsX() {
        return controlContentX();
    }

    private int denseControlsX() {
        return controlContentX();
    }

    private boolean denseTabSelected() {
        return lightningSelected || vaultAltarSelected || stormLikeSelected();
    }

    private int activeAccent() {
        return activeTabDescriptor().accent;
    }

    private String activeTabLabel() {
        return activeTabDescriptor().label;
    }

    private TabDescriptor activeTabDescriptor() {
        for (TabDescriptor tab : TABS) {
            if (tab.id == activeTab) {
                return tab;
            }
        }
        return TABS[0];
    }

    private void updatePalettePosition() {
        int pickerWidth = PALETTE_WIDTH + BRIGHTNESS_WIDTH + 34;
        paletteX = pickerX() + 12;
        paletteY = pickerY() + 24;
    }

    private void layoutWidgets() {
        clampControlScrollOffset();
        updatePalettePosition();
        layoutPickerWidgets();
        layoutTabs();
        layoutDrawerButtons();
        layoutProfileWidgets();
        layoutColorWidgets();
        layoutBeamWidgets();
        layoutLightningWidgets();
        layoutVaultAltarWidgets();
        layoutStormArrowWidgets();
    }

    private void layoutTabs() {
        tabScrollOffset = clamp(tabScrollOffset, 0, maxTabScrollOffset());
        int tabY = topBarY() + 14;
        int gap = 8;
        int x = tabAreaX() + overflowArrowSpace() - tabScrollOffset;
        int minX = tabAreaX() + overflowArrowSpace();
        int maxX = tabAreaX() + tabAreaWidth() - overflowArrowSpace();
        for (int i = 0; i < tabButtons.size() && i < TABS.length; i++) {
            Button button = tabButtons.get(i);
            TabDescriptor tab = TABS[i];
            moveWidget(button, x, tabY, tab.width);
            boolean visible = x >= minX && x + tab.width <= maxX;
            setVisible(button, visible);
            if (button instanceof ThemeButton themeButton) {
                themeButton.setSelected(tab.id == activeTab);
                themeButton.setAccent(tab.accent);
            }
            x += tab.width + gap;
        }

        boolean overflow = totalTabWidth() > tabAreaWidth();
        setVisible(tabScrollLeftButton, overflow && tabScrollOffset > 0);
        setVisible(tabScrollRightButton, overflow && tabScrollOffset < maxTabScrollOffset());
        moveWidget(tabScrollLeftButton, tabAreaX(), tabY, 20);
        moveWidget(tabScrollRightButton, tabAreaX() + tabAreaWidth() - 20, tabY, 20);
    }

    private int overflowArrowSpace() {
        return totalTabWidth() > tabAreaWidth() ? 28 : 0;
    }

    private int totalTabWidth() {
        int width = 0;
        for (int i = 0; i < TABS.length; i++) {
            width += TABS[i].width;
            if (i + 1 < TABS.length) {
                width += 8;
            }
        }
        return width;
    }

    private int maxTabScrollOffset() {
        return Math.max(0, totalTabWidth() - (tabAreaWidth() - overflowArrowSpace() * 2));
    }

    private void ensureActiveTabVisible() {
        int gap = 8;
        int x = 0;
        for (TabDescriptor tab : TABS) {
            int nextX = x + tab.width;
            if (tab.id == activeTab) {
                int visibleWidth = tabAreaWidth() - overflowArrowSpace() * 2;
                if (x - tabScrollOffset < 0) {
                    tabScrollOffset = x;
                } else if (nextX - tabScrollOffset > visibleWidth) {
                    tabScrollOffset = nextX - visibleWidth;
                }
                tabScrollOffset = clamp(tabScrollOffset, 0, maxTabScrollOffset());
                return;
            }
            x = nextX + gap;
        }
    }

    private void layoutDrawerButtons() {
        if (drawerToggleButton != null) {
            drawerToggleButton.setMessage(new TextComponent(drawerState == DrawerState.EXPANDED ? "Collapse" : "Expand"));
        }
        int y = drawerY() + (drawerState == DrawerState.COLLAPSED ? 7 : 13);
        if (drawerState == DrawerState.COLLAPSED) {
            int x = collapsedDrawerX();
            int right = x + collapsedDrawerWidth() - 12;
            moveWidget(drawerToggleButton, right - 250, y, 82);
            moveWidget(hideChromeButton, right - 158, y, 74);
            moveWidget(doneButton, right - 74, y, 62);
            return;
        }

        int right = drawerX() + drawerWidth() - 20;
        moveWidget(doneButton, right - 74, y, 74);
        moveWidget(hideChromeButton, right - 156, y, 74);
        moveWidget(drawerToggleButton, right - 248, y, 84);
    }

    private void layoutProfileWidgets() {
        int x = profilePanelX();
        int y = profilePanelY();
        moveWidget(profileNameBox, x, y + 18, 98);
        moveWidget(profileAddButton, x + 104, y + 18, 48);
        moveWidget(profileDropdownButton, x, y + 46, PROFILE_PANEL_WIDTH);
    }

    private void layoutColorWidgets() {
        for (int i = 0; i < colorBoxes.size(); i++) {
            moveColorBox(colorBoxes.get(i), i, beamRowY());
        }
        for (int i = 0; i < glowColorBoxes.size(); i++) {
            moveColorBox(glowColorBoxes.get(i), i, glowRowY());
        }
        moveColorBox(lightningRingColorBox, 0, beamRowY());
        moveColorBox(lightningSphereColorBox, 1, beamRowY());
        moveColorBox(lightningConeColorBox, 2, beamRowY());
        moveColorBox(lightningSpotColorBox, 3, beamRowY());

        for (int i = 0; i < altarColorBoxes.size(); i++) {
            int slot = i < 4 ? i : i - 3;
            int y = i < 4 ? beamRowY() : altarSecondColorRowY();
            moveColorBox(altarColorBoxes.get(i), slot, y);
        }
        for (int i = 0; i < stormArrowColorBoxes.size(); i++) {
            moveColorBox(stormArrowColorBoxes.get(i), i, beamRowY());
        }
    }

    private void layoutBeamWidgets() {
        int y = controlCardY() + 30;
        int shapeX = controlCardInnerX(beamShapeCardX());
        int shapeWidth = controlCardInnerWidth(BEAM_SHAPE_CARD_WIDTH);
        moveWidget(intensitySlider, shapeX, y, shapeWidth);
        moveWidget(opacitySlider, shapeX, y + 23, shapeWidth);
        moveWidget(glowRadiusSlider, shapeX, y + 46, shapeWidth);
        moveWidget(glowOpacitySlider, shapeX, y + 69, shapeWidth);

        int motionX = controlCardInnerX(beamMotionCardX());
        int motionWidth = controlCardInnerWidth(BEAM_MOTION_CARD_WIDTH);
        moveWidget(colorShiftSlider, motionX, y, motionWidth);
        moveWidget(glowRotationSlider, motionX, y + 23, motionWidth);

        int advancedX = controlCardInnerX(beamAdvancedCardX());
        int advancedWidth = controlCardInnerWidth(BEAM_ADVANCED_CARD_WIDTH);
        moveWidget(shaderCompatibilityButton, advancedX, y, advancedWidth);

        int audioX = controlCardInnerX(beamAudioCardX());
        int audioWidth = controlCardInnerWidth(BEAM_AUDIO_CARD_WIDTH);
        moveWidget(soundButton, audioX, y, audioWidth);
        moveWidget(soundVolumeBox, audioX + 50, y + 26, 50);

        int transitionX = controlCardInnerX(beamTransitionCardX());
        int transitionWidth = controlCardInnerWidth(BEAM_TRANSITION_CARD_WIDTH);
        moveWidget(fadeInModeButton, transitionX, y, transitionWidth - 46);
        moveWidget(fadeInTicksBox, transitionX + transitionWidth - 34, y, 34);
        moveWidget(fadeOutModeButton, transitionX, y + 26, transitionWidth - 46);
        moveWidget(fadeOutTicksBox, transitionX + transitionWidth - 34, y + 26, 34);

        int originX = controlCardInnerX(beamOriginCardX());
        int originWidth = controlCardInnerWidth(BEAM_ORIGIN_CARD_WIDTH);
        moveWidget(handButton, originX, y, originWidth);
        layoutOriginWidgets(originX, y + 28);
    }

    private void layoutLightningWidgets() {
        int y = controlCardY() + 30;
        int cardX = denseControlsX();

        int setupX = controlCardInnerX(cardX);
        int setupWidth = controlCardInnerWidth(DENSE_SETUP_CARD_WIDTH);
        moveWidget(lightningEnabledButton, setupX, y, setupWidth);
        moveWidget(lightningShaderCompatibilityButton, setupX, y + 23, setupWidth);
        moveWidget(lightningFullbrightButton, setupX, y + 46, setupWidth);
        moveWidget(lightningLifetimeBox, setupX + 58, y + 75, 54);
        cardX += DENSE_SETUP_CARD_WIDTH + CONTROL_CARD_GAP;

        int shockX = controlCardInnerX(cardX);
        int shockWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(lightningStartRadiusSlider, shockX, y, shockWidth);
        moveWidget(lightningEndRadiusSlider, shockX, y + 23, shockWidth);
        moveWidget(lightningThicknessSlider, shockX, y + 46, shockWidth);
        moveWidget(lightningSideCountBox, shockX + 58, y + 75, 44);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int opacityX = controlCardInnerX(cardX);
        int opacityWidth = controlCardInnerWidth(DENSE_NARROW_CARD_WIDTH);
        moveWidget(lightningAlphaSlider, opacityX, y, opacityWidth);
        moveWidget(lightningInteriorOpacitySlider, opacityX, y + 23, opacityWidth);
        cardX += DENSE_NARROW_CARD_WIDTH + CONTROL_CARD_GAP;

        int centerX = controlCardInnerX(cardX);
        int centerWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        int centerHalf = Math.max(88, (centerWidth - 10) / 2);
        moveWidget(lightningSphereRadiusSlider, centerX, y, centerHalf);
        moveWidget(lightningSphereOpacitySlider, centerX + centerHalf + 10, y, centerHalf);
        moveWidget(lightningConeHeightSlider, centerX, y + 23, centerHalf);
        moveWidget(lightningConeRadiusSlider, centerX + centerHalf + 10, y + 23, centerHalf);
        moveWidget(lightningConeOpacitySlider, centerX, y + 46, centerWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int spotsX = controlCardInnerX(cardX);
        int spotsWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        int spotsHalf = Math.max(80, (spotsWidth - 10) / 2);
        moveWidget(lightningSpotCountSlider, spotsX, y, spotsHalf);
        moveWidget(lightningSpotSizeSlider, spotsX + spotsHalf + 10, y, spotsHalf);
        moveWidget(lightningSpotOpacitySlider, spotsX, y + 23, spotsWidth);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int ripplesX = controlCardInnerX(cardX);
        int ripplesWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(lightningSecondarySizeSlider, ripplesX, y, ripplesWidth);
        moveWidget(lightningSecondaryCountBox, ripplesX + 58, y + 29, 54);
        moveWidget(lightningSecondaryDelayBox, ripplesX + 58, y + 55, 44);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int audioX = controlCardInnerX(cardX);
        int audioWidth = controlCardInnerWidth(DENSE_AUDIO_CARD_WIDTH);
        moveWidget(lightningSoundButton, audioX, y, audioWidth);
        moveWidget(lightningSoundVolumeBox, audioX + 58, y + 29, 50);
    }

    private void layoutVaultAltarWidgets() {
        int y = controlCardY() + 30;
        int cardX = denseControlsX();

        int setupX = controlCardInnerX(cardX);
        int setupWidth = controlCardInnerWidth(DENSE_SETUP_CARD_WIDTH);
        moveWidget(altarEnabledButton, setupX, y, setupWidth);
        moveWidget(altarShaderCompatibilityButton, setupX, y + 23, setupWidth);
        moveWidget(altarFullbrightButton, setupX, y + 46, setupWidth);
        cardX += DENSE_SETUP_CARD_WIDTH + CONTROL_CARD_GAP;

        int cornerX = controlCardInnerX(cardX);
        int cornerWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(altarCornerRadiusSlider, cornerX, y, cornerWidth);
        moveWidget(altarCornerOpacitySlider, cornerX, y + 23, cornerWidth);
        moveWidget(altarVerticalTicksBox, cornerX + 62, y + 52, 42);
        moveWidget(altarConvergeTicksBox, cornerX + 62, y + 78, 42);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int centerX = controlCardInnerX(cardX);
        int centerWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(altarCenterBottomRadiusSlider, centerX, y, centerWidth);
        moveWidget(altarCenterTopRadiusSlider, centerX, y + 23, centerWidth);
        moveWidget(altarCenterHeightSlider, centerX, y + 46, centerWidth);
        moveWidget(altarCenterFadeSlider, centerX, y + 69, centerWidth);
        moveWidget(altarCenterOpacitySlider, centerX, y + 92, centerWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int glowX = controlCardInnerX(cardX);
        int glowWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(altarGlowOpacitySlider, glowX, y, glowWidth);
        moveWidget(altarGlowBottomRadiusSlider, glowX, y + 23, glowWidth);
        moveWidget(altarGlowTopRadiusSlider, glowX, y + 46, glowWidth);
        moveWidget(altarGlowHeightSlider, glowX, y + 69, glowWidth);
        moveWidget(altarGlowFadeSlider, glowX, y + 92, glowWidth);
        moveWidget(altarGlowRotationSlider, glowX, y + 115, glowWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int originX = controlCardInnerX(cardX);
        int originWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(altarOriginHeightBox, originX + 58, y, 50);
        moveWidget(altarOriginRadiusBox, originX + 58, y + 26, 50);
        moveWidget(altarOriginMarkersButton, originX, y + 52, originWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int timingX = controlCardInnerX(cardX);
        moveWidget(altarCenterGrowTicksBox, timingX + 62, y, 42);
        cardX += DENSE_NARROW_CARD_WIDTH + CONTROL_CARD_GAP;

        int audioX = controlCardInnerX(cardX);
        int audioWidth = controlCardInnerWidth(DENSE_AUDIO_CARD_WIDTH);
        moveWidget(altarSoundButton, audioX, y, audioWidth);
        moveWidget(altarSoundVolumeBox, audioX + 58, y + 29, 50);
    }

    private void layoutStormArrowWidgets() {
        int y = controlCardY() + 30;
        if (archonSelected) {
            layoutArchonWidgets(y);
        } else if (smiteSelected) {
            layoutSmiteWidgets(y);
        } else {
            layoutStormArrowOnlyWidgets(y);
        }
    }

    private void layoutStormArrowOnlyWidgets(int y) {
        int cardX = denseControlsX();

        int setupX = controlCardInnerX(cardX);
        int setupWidth = controlCardInnerWidth(DENSE_SETUP_CARD_WIDTH);
        moveWidget(stormArrowEnabledButton, setupX, y, setupWidth);
        moveWidget(stormArrowShaderCompatibilityButton, setupX, y + 23, setupWidth);
        moveWidget(stormArrowFullbrightButton, setupX, y + 46, setupWidth);
        cardX += DENSE_SETUP_CARD_WIDTH + CONTROL_CARD_GAP;

        int targetX = controlCardInnerX(cardX);
        int targetWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowTargetingCircleButton, targetX, y, targetWidth);
        moveWidget(stormArrowActualRadiusButton, targetX, y + 23, targetWidth);
        moveWidget(stormArrowCircleAlphaSlider, targetX, y + 46, targetWidth);
        moveWidget(stormArrowCircleThicknessSlider, targetX, y + 69, targetWidth);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int projectileX = controlCardInnerX(cardX);
        int projectileWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowProjectileSoundButton, projectileX, y, projectileWidth);
        moveWidget(stormArrowAudioRangeBox, projectileX + 78, y + 29, 34);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int strikeX = controlCardInnerX(cardX);
        int strikeWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(stormArrowBlasterAlphaSlider, strikeX, y, strikeWidth);
        moveWidget(stormArrowBlasterWidthSlider, strikeX, y + 23, strikeWidth);
        moveWidget(stormArrowSegmentLengthSlider, strikeX, y + 46, strikeWidth);
        moveWidget(stormArrowSegmentGapSlider, strikeX, y + 69, strikeWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int impactX = controlCardInnerX(cardX);
        int impactWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowImpactFlashSizeSlider, impactX, y, impactWidth);
        moveWidget(stormArrowImpactFlashButton, impactX, y + 28, impactWidth);
        moveWidget(stormArrowLifetimeBox, impactX + 58, y + 58, 54);
        moveWidget(stormArrowOriginHeightBox, impactX + 58, y + 84, 54);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int audioX = controlCardInnerX(cardX);
        int audioWidth = controlCardInnerWidth(DENSE_AUDIO_CARD_WIDTH);
        moveWidget(stormArrowSoundButton, audioX, y, audioWidth);
        moveWidget(stormArrowSoundVolumeBox, audioX + 58, y + 29, 50);
    }

    private void layoutSmiteWidgets(int y) {
        int cardX = denseControlsX();

        int setupX = controlCardInnerX(cardX);
        int setupWidth = controlCardInnerWidth(DENSE_SETUP_CARD_WIDTH);
        moveWidget(stormArrowEnabledButton, setupX, y, setupWidth);
        moveWidget(stormArrowShaderCompatibilityButton, setupX, y + 23, setupWidth);
        moveWidget(stormArrowFullbrightButton, setupX, y + 46, setupWidth);
        cardX += DENSE_SETUP_CARD_WIDTH + CONTROL_CARD_GAP;

        int targetX = controlCardInnerX(cardX);
        int targetWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowTargetingCircleButton, targetX, y, targetWidth);
        moveWidget(stormArrowActualRadiusButton, targetX, y + 23, targetWidth);
        moveWidget(stormArrowCircleAlphaSlider, targetX, y + 46, targetWidth);
        moveWidget(stormArrowCircleThicknessSlider, targetX, y + 69, targetWidth);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int strikeX = controlCardInnerX(cardX);
        int strikeWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(stormArrowBlasterAlphaSlider, strikeX, y, strikeWidth);
        moveWidget(stormArrowBlasterWidthSlider, strikeX, y + 23, strikeWidth);
        moveWidget(stormArrowSegmentLengthSlider, strikeX, y + 46, strikeWidth);
        moveWidget(stormArrowSegmentGapSlider, strikeX, y + 69, strikeWidth);
        moveWidget(stormArrowSoundButton, strikeX, y + 92, strikeWidth);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int activationX = controlCardInnerX(cardX);
        int activationWidth = controlCardInnerWidth(DENSE_NARROW_CARD_WIDTH);
        moveWidget(stormArrowProjectileSoundButton, activationX, y, activationWidth);
        cardX += DENSE_NARROW_CARD_WIDTH + CONTROL_CARD_GAP;

        int impactX = controlCardInnerX(cardX);
        int impactWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowImpactFlashSizeSlider, impactX, y, impactWidth);
        moveWidget(stormArrowImpactFlashButton, impactX, y + 28, impactWidth);
        moveWidget(stormArrowLifetimeBox, impactX + 58, y + 58, 54);
        moveWidget(stormArrowOriginHeightBox, impactX + 58, y + 84, 54);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int rangeX = controlCardInnerX(cardX);
        moveWidget(stormArrowAudioRangeBox, rangeX + 78, y, 34);
        moveWidget(stormArrowSoundVolumeBox, rangeX + 58, y + 29, 50);
    }

    private void layoutArchonWidgets(int y) {
        int cardX = denseControlsX();

        int setupX = controlCardInnerX(cardX);
        int setupWidth = controlCardInnerWidth(DENSE_SETUP_CARD_WIDTH);
        moveWidget(stormArrowEnabledButton, setupX, y, setupWidth);
        moveWidget(stormArrowShaderCompatibilityButton, setupX, y + 23, setupWidth);
        moveWidget(stormArrowFullbrightButton, setupX, y + 46, setupWidth);
        cardX += DENSE_SETUP_CARD_WIDTH + CONTROL_CARD_GAP;

        int targetX = controlCardInnerX(cardX);
        int targetWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(stormArrowTargetingCircleButton, targetX, y, targetWidth);
        moveWidget(stormArrowActualRadiusButton, targetX, y + 23, targetWidth);
        moveWidget(stormArrowCircleAlphaSlider, targetX, y + 46, targetWidth);
        moveWidget(stormArrowCircleThicknessSlider, targetX, y + 69, targetWidth);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int missileX = controlCardInnerX(cardX);
        int missileWidth = controlCardInnerWidth(DENSE_MEDIUM_CARD_WIDTH);
        moveWidget(archonMissileRadiusBox, missileX + 58, y, 54);
        moveWidget(stormArrowOriginHeightBox, missileX + 58, y + 26, 54);
        moveWidget(stormArrowSegmentLengthSlider, missileX, y + 55, missileWidth);
        cardX += DENSE_MEDIUM_CARD_WIDTH + CONTROL_CARD_GAP;

        int impactX = controlCardInnerX(cardX);
        int impactWidth = controlCardInnerWidth(DENSE_WIDE_CARD_WIDTH);
        moveWidget(stormArrowBlasterAlphaSlider, impactX, y, impactWidth);
        moveWidget(stormArrowBlasterWidthSlider, impactX, y + 23, impactWidth);
        moveWidget(stormArrowImpactFlashSizeSlider, impactX, y + 46, impactWidth);
        moveWidget(stormArrowImpactFlashButton, impactX, y + 74, impactWidth);
        moveWidget(stormArrowLifetimeBox, impactX + 58, y + 104, 54);
        cardX += DENSE_WIDE_CARD_WIDTH + CONTROL_CARD_GAP;

        int activationX = controlCardInnerX(cardX);
        int activationWidth = controlCardInnerWidth(DENSE_NARROW_CARD_WIDTH);
        moveWidget(stormArrowProjectileSoundButton, activationX, y, activationWidth);
        cardX += DENSE_NARROW_CARD_WIDTH + CONTROL_CARD_GAP;

        int audioX = controlCardInnerX(cardX);
        int audioWidth = controlCardInnerWidth(DENSE_AUDIO_CARD_WIDTH);
        moveWidget(stormArrowSoundButton, audioX, y, audioWidth);
        moveWidget(stormArrowSoundVolumeBox, audioX + 58, y + 29, 50);
        cardX += DENSE_AUDIO_CARD_WIDTH + CONTROL_CARD_GAP;

        int rangeX = controlCardInnerX(cardX);
        moveWidget(stormArrowAudioRangeBox, rangeX + 78, y, 34);
    }

    private void layoutOriginWidgets(int x, int y) {
        for (int i = 0; i < originBoxes.size() && i < originLabelButtons.size(); i++) {
            int originX = x + i * 96;
            moveWidget(originLabelButtons.get(i), originX, y, 22);
            moveWidget(originBoxes.get(i), originX + 25, y, 64);
        }
    }

    private void moveColorBox(EditBox box, int slot, int y) {
        moveWidget(box, slotStartX(slot) + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, y, SLOT_HEX_WIDTH);
    }

    private void layoutPickerWidgets() {
        moveWidget(pickerHexBox, pickerX() + 74, pickerY() + pickerHeight() - 30, 78);
    }

    private int pickerWidth() {
        return PALETTE_WIDTH + BRIGHTNESS_WIDTH + 34;
    }

    private int pickerHeight() {
        return PALETTE_HEIGHT + 72;
    }

    private int pickerX() {
        int preferred = colorCardX() + colorCardWidth() / 2 - pickerWidth() / 2;
        return clamp(preferred, drawerX() + 18, layoutWidth - pickerWidth() - 18);
    }

    private int pickerY() {
        return Math.max(topBarY() + TOP_BAR_HEIGHT + 12, drawerY() - pickerHeight() - 10);
    }

    private boolean isInsidePicker(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, pickerX(), pickerY(), pickerWidth(), pickerHeight());
    }

    private void styleEditBoxes() {
        styleEditBox(profileNameBox);
        styleEditBox(soundVolumeBox);
        styleEditBox(fadeInTicksBox);
        styleEditBox(fadeOutTicksBox);
        styleEditBox(lightningLifetimeBox);
        styleEditBox(lightningSideCountBox);
        styleEditBox(lightningRingColorBox);
        styleEditBox(lightningSphereColorBox);
        styleEditBox(lightningConeColorBox);
        styleEditBox(lightningSpotColorBox);
        styleEditBox(lightningSecondaryCountBox);
        styleEditBox(lightningSecondaryDelayBox);
        styleEditBox(lightningSoundVolumeBox);
        styleEditBox(altarVerticalTicksBox);
        styleEditBox(altarConvergeTicksBox);
        styleEditBox(altarCenterGrowTicksBox);
        styleEditBox(altarOriginHeightBox);
        styleEditBox(altarOriginRadiusBox);
        styleEditBox(altarSoundVolumeBox);
        styleEditBox(stormArrowLifetimeBox);
        styleEditBox(stormArrowOriginHeightBox);
        styleEditBox(archonMissileRadiusBox);
        styleEditBox(stormArrowSoundVolumeBox);
        styleEditBox(stormArrowAudioRangeBox);
        styleEditBox(pickerHexBox);
        for (EditBox box : colorBoxes) {
            styleEditBox(box);
        }
        for (EditBox box : glowColorBoxes) {
            styleEditBox(box);
        }
        for (EditBox box : originBoxes) {
            styleEditBox(box);
        }
        for (EditBox box : altarColorBoxes) {
            styleEditBox(box);
        }
        for (EditBox box : stormArrowColorBoxes) {
            styleEditBox(box);
        }
    }

    private void styleEditBox(EditBox box) {
        if (box != null) {
            box.setBordered(true);
            box.setTextColor(ArcaneBeamGuiTheme.TEXT);
            box.setTextColorUneditable(ArcaneBeamGuiTheme.MUTED_TEXT);
        }
    }

    private static void moveWidget(AbstractWidget widget, int x, int y, int width) {
        if (widget != null) {
            widget.x = x;
            widget.y = y;
            widget.setWidth(width);
        }
    }

    private void addOriginBox(int x, int y, String label, int axis) {
        Button labelButton = this.addRenderableWidget(new ThemeButton(x, y, 22, 20, new TextComponent(label), button -> {
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

    private void addAltarColorBox(int x, int y, String label, int slot) {
        EditBox editBox = new EditBox(this.font, x + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(value -> updateAltarColorFromText(slot, value));
        altarColorBoxes.add(editBox);
        this.addRenderableWidget(editBox);
    }

    private void addStormArrowColorBox(int x, int y, String label, int slot) {
        EditBox editBox = new EditBox(this.font, x + SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(value -> updateStormArrowColorFromText(slot, value));
        stormArrowColorBoxes.add(editBox);
        this.addRenderableWidget(editBox);
    }

    private EditBox addLightningSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateLightningSoundVolumeFromText);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addAltarNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addAltarSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateAltarSoundVolumeFromText);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addAltarDecimalBox(int x, int y, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, 50, 20, new TextComponent("Altar Decimal"));
        editBox.setMaxLength(5);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}(\\.[0-9]{0,2})?"));
        editBox.setResponder(responder);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addStormArrowNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addStormArrowSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateStormArrowSoundVolumeFromText);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    private EditBox addStormArrowAudioRangeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 74, y, 34, 20, new TextComponent("Audio Range"));
        editBox.setMaxLength(2);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        editBox.setResponder(this::updateStormArrowAudioRangeFromText);
        this.addRenderableWidget(editBox);
        return editBox;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int layoutMouseX = toLayoutX(mouseX);
        int layoutMouseY = toLayoutY(mouseY);

        poseStack.pushPose();
        poseStack.scale(layoutScale, layoutScale, 1.0F);
        renderChrome(poseStack);
        if (drawerState == DrawerState.EXPANDED) {
            renderProfilePanel(poseStack);
            renderInputFrames(poseStack);
            if (stormLikeSelected()) {
                renderStormArrowColorPreviews(poseStack);
                renderStormArrowLabels(poseStack);
            } else if (vaultAltarSelected) {
                renderVaultAltarColorPreviews(poseStack);
                renderVaultAltarLabels(poseStack);
            } else if (lightningSelected) {
                renderLightningColorPreviews(poseStack);
                renderLightningLabels(poseStack);
            } else {
                renderInlinePreviews(poseStack);
            }
            if (!lightningSelected && !vaultAltarSelected && !stormLikeSelected() && soundVolumeBox != null) {
                drawControlLabel(poseStack, soundVolumeBox, "Volume", soundVolumeBox.x - this.font.width("Volume") - 8, soundVolumeBox.y + 6);
            }
            if (colorPickerOpen) {
                renderPalette(poseStack);
                renderBrightnessStrip(poseStack);
            }
        }
        super.render(poseStack, layoutMouseX, layoutMouseY, partialTick);
        if (drawerState == DrawerState.EXPANDED) {
            renderProfileDropdown(poseStack);
        }
        poseStack.popPose();
    }

    private void renderChrome(PoseStack poseStack) {
        if (drawerState == DrawerState.HIDDEN) {
            renderCleanViewHandle(poseStack);
            return;
        }

        renderTopBar(poseStack);
        if (drawerState == DrawerState.COLLAPSED) {
            renderCollapsedDrawer(poseStack);
            return;
        }

        int x = drawerX();
        int y = drawerY();
        int width = drawerWidth();
        int height = drawerHeight();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, x, y, width, height);
        fill(poseStack, x + 9, y + 9, x + width - 9, y + height - 9, ArcaneBeamGuiTheme.CARD_SHADE);
        fill(poseStack, x + 18, y + 38, x + width - 18, y + 39, ArcaneBeamGuiTheme.GOLD_LINE);

        drawString(poseStack, this.font, activeTabLabel() + " Tuning", x + 20, y + 15, ArcaneBeamGuiTheme.TEXT);
        drawString(poseStack, this.font, colorPickerOpen ? "Color picker is open above the drawer." : "Click a color swatch to open the picker. Use Collapse or Clean View for preview.", x + 20, y + 30, ArcaneBeamGuiTheme.MUTED_TEXT);

        renderCard(poseStack, profileCardX(), profileCardY(), profileCardWidth(), drawerContentHeight(), "Profiles");
        if (denseTabSelected()) {
            renderCard(poseStack, colorCardX(), colorCardY(), colorCardWidth(), drawerContentHeight(), "Colors");
            renderCard(poseStack, controlCardX(), controlCardY(), controlCardWidth(), drawerContentHeight(), "Effect Controls");
        } else {
            renderCard(poseStack, colorCardX(), colorCardY(), colorCardWidth(), drawerContentHeight(), "Colors");
            renderCard(poseStack, controlCardX(), controlCardY(), controlCardWidth(), drawerContentHeight(), "Beam Controls");
        }
        renderControlSubCards(poseStack);
        renderControlScrollHint(poseStack);
    }

    private void renderTopBar(PoseStack poseStack) {
        int width = topBarWidth();
        int x = topBarX();
        int y = topBarY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GLASS_PANEL, x, y, width, TOP_BAR_HEIGHT);
        fill(poseStack, x + 10, y + TOP_BAR_HEIGHT - 5, x + width - 10, y + TOP_BAR_HEIGHT - 4, (activeAccent() & 0x00FFFFFF) | 0x88000000);
        drawString(poseStack, this.font, "Arcane Beam", x + 18, y + 10, ArcaneBeamGuiTheme.TEXT);
        drawString(poseStack, this.font, activeTabLabel(), x + 18, y + 25, activeAccent());
        fill(poseStack, x + TOP_BAR_BRAND_WIDTH - 10, y + 8, x + TOP_BAR_BRAND_WIDTH - 9, y + TOP_BAR_HEIGHT - 9, ArcaneBeamGuiTheme.LINE);
    }

    private void renderCollapsedDrawer(PoseStack poseStack) {
        int width = collapsedDrawerWidth();
        int x = collapsedDrawerX();
        int y = drawerY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, x, y, width, DRAWER_COLLAPSED_HEIGHT);
        drawString(poseStack, this.font, "Controls collapsed - preview safe zone", x + 16, y + 13, ArcaneBeamGuiTheme.TEXT);
    }

    private int cleanViewHandleX() {
        return layoutWidth / 2 - CLEAN_VIEW_HANDLE_WIDTH / 2;
    }

    private int cleanViewHandleY() {
        return layoutHeight - DRAWER_MARGIN - CLEAN_VIEW_HANDLE_HEIGHT;
    }

    private void renderCleanViewHandle(PoseStack poseStack) {
        int x = cleanViewHandleX();
        int y = cleanViewHandleY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GLASS_PANEL, x, y, CLEAN_VIEW_HANDLE_WIDTH, CLEAN_VIEW_HANDLE_HEIGHT);
        fill(poseStack, x + 8, y + 8, x + CLEAN_VIEW_HANDLE_WIDTH - 8, y + CLEAN_VIEW_HANDLE_HEIGHT - 8, ArcaneBeamGuiTheme.CARD_SHADE);
        fill(poseStack, x + 12, y + CLEAN_VIEW_HANDLE_HEIGHT - 6, x + CLEAN_VIEW_HANDLE_WIDTH - 12, y + CLEAN_VIEW_HANDLE_HEIGHT - 5, (activeAccent() & 0x00FFFFFF) | 0xAA000000);
        drawCenteredString(poseStack, this.font, "Restore Menu (H)", x + CLEAN_VIEW_HANDLE_WIDTH / 2, y + 10, ArcaneBeamGuiTheme.TEXT);
    }

    private void renderCard(PoseStack poseStack, int x, int y, int width, int height, String label) {
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.IRON_PANEL, x, y, width, height);
        fill(poseStack, x + 7, y + 7, x + width - 7, y + height - 7, ArcaneBeamGuiTheme.SOFT_SHADE);
        drawString(poseStack, this.font, label, x + 10, y + 8, ArcaneBeamGuiTheme.MUTED_TEXT);
    }

    private void renderControlSubCards(PoseStack poseStack) {
        if (denseTabSelected()) {
            renderDenseControlSubCards(poseStack);
            return;
        }
        renderBeamControlSubCards(poseStack);
    }

    private void renderDenseControlSubCards(PoseStack poseStack) {
        if (lightningSelected) {
            renderDenseCardRow(poseStack,
                    new String[]{"Setup", "Shockwave", "Opacity", "Center Geometry", "Spots", "Ripples", "Audio"},
                    new int[]{DENSE_SETUP_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_AUDIO_CARD_WIDTH});
            return;
        }
        if (vaultAltarSelected) {
            renderDenseCardRow(poseStack,
                    new String[]{"Setup", "Corner Beam", "Center Beam", "Glow", "Origin", "Timing", "Audio"},
                    new int[]{DENSE_SETUP_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH, DENSE_AUDIO_CARD_WIDTH});
            return;
        }
        if (archonSelected) {
            renderDenseCardRow(poseStack,
                    new String[]{"Setup", "Targeting", "Missile", "Strike + Impact", "Activation", "Strike Audio", "Audio Range"},
                    new int[]{DENSE_SETUP_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH, DENSE_AUDIO_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH});
            return;
        }
        if (smiteSelected) {
            renderDenseCardRow(poseStack,
                    new String[]{"Setup", "Targeting", "Strike", "Activation", "Impact", "Audio Range"},
                    new int[]{DENSE_SETUP_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_NARROW_CARD_WIDTH});
            return;
        }
        if (stormArrowSelected) {
            renderDenseCardRow(poseStack,
                    new String[]{"Setup", "Targeting", "Projectile", "Strike", "Impact", "Audio"},
                    new int[]{DENSE_SETUP_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_WIDE_CARD_WIDTH, DENSE_MEDIUM_CARD_WIDTH, DENSE_AUDIO_CARD_WIDTH});
        }
    }

    private void renderDenseCardRow(PoseStack poseStack, String[] labels, int[] widths) {
        int cardX = denseControlsX();
        int cardY = controlCardY() + 12;
        int cardHeight = controlCardY() + drawerContentHeight() - cardY - 10;
        for (int i = 0; i < labels.length && i < widths.length; i++) {
            renderControlSubCard(poseStack, cardX, cardY, widths[i], cardHeight, labels[i]);
            cardX += widths[i] + CONTROL_CARD_GAP;
        }
    }

    private void renderBeamControlSubCards(PoseStack poseStack) {
        int y = controlCardY() + 30;
        int cardY = y - 18;
        int cardHeight = 112;
        renderControlSubCard(poseStack, beamShapeCardX(), cardY, BEAM_SHAPE_CARD_WIDTH, cardHeight, "Shape");
        renderControlSubCard(poseStack, beamMotionCardX(), cardY, BEAM_MOTION_CARD_WIDTH, cardHeight, "Motion");
        renderControlSubCard(poseStack, beamAdvancedCardX(), cardY, BEAM_ADVANCED_CARD_WIDTH, cardHeight, "Advanced");
        renderControlSubCard(poseStack, beamAudioCardX(), cardY, BEAM_AUDIO_CARD_WIDTH, cardHeight, "Audio");
        renderControlSubCard(poseStack, beamTransitionCardX(), cardY, BEAM_TRANSITION_CARD_WIDTH, cardHeight, "Transition");
        renderControlSubCard(poseStack, beamOriginCardX(), cardY, BEAM_ORIGIN_CARD_WIDTH, cardHeight, "Origin");
    }

    private void renderControlSubCard(PoseStack poseStack, int x, int y, int width, int height) {
        renderControlSubCard(poseStack, x, y, width, height, null);
    }

    private void renderControlSubCard(PoseStack poseStack, int x, int y, int width, int height, String label) {
        int left = Math.max(x, controlViewportX());
        int right = Math.min(x + width, controlViewportRight());
        int bottom = Math.min(y + height, controlCardY() + drawerContentHeight() - 8);
        if (right <= left + 12 || bottom <= y + 12) {
            return;
        }
        fill(poseStack, left, y, right, bottom, 0x2605080D);
        if (left == x) {
            fill(poseStack, left, y, left + 2, bottom, (activeAccent() & 0x00FFFFFF) | 0x66000000);
        }
        fill(poseStack, left + 4, y, right - 4, y + 1, 0x33294A5A);
        fill(poseStack, left + 4, bottom - 1, right - 4, bottom, 0x33294A5A);
        if (label != null) {
            int chipX = x + 8;
            int chipWidth = controlSectionChipWidth(Math.min(width - 16, 118), label);
            if (chipX >= controlViewportX() && chipX + chipWidth <= controlViewportRight()) {
                renderControlSectionChip(poseStack, chipX, y + 5, Math.min(width - 16, 118), label);
            }
        }
    }

    private void renderControlSectionChip(PoseStack poseStack, int x, int y, int width, String label) {
        int chipWidth = controlSectionChipWidth(width, label);
        fill(poseStack, x, y, x + chipWidth, y + 10, 0x8805080D);
        fill(poseStack, x, y + 9, x + chipWidth, y + 10, (activeAccent() & 0x00FFFFFF) | 0xAA000000);
        drawString(poseStack, this.font, label, x + 7, y + 1, ArcaneBeamGuiTheme.MUTED_TEXT);
    }

    private int controlSectionChipWidth(int width, String label) {
        return Math.min(width, Math.max(58, this.font.width(label) + 18));
    }

    private void renderControlScrollHint(PoseStack poseStack) {
        int max = maxControlScrollOffset();
        if (max <= 0) {
            return;
        }
        int x = controlViewportX();
        int right = controlViewportRight();
        int y = controlCardY() + drawerContentHeight() - 7;
        int trackWidth = Math.max(1, right - x);
        int thumbWidth = Math.max(28, trackWidth * controlViewportWidth() / Math.max(controlViewportWidth(), controlContentWidth() + 8));
        int thumbTravel = Math.max(1, trackWidth - thumbWidth);
        int thumbX = x + thumbTravel * controlScrollOffset / max;
        fill(poseStack, x, y, right, y + 2, 0x5505080D);
        fill(poseStack, thumbX, y - 1, thumbX + thumbWidth, y + 3, (activeAccent() & 0x00FFFFFF) | 0x99000000);
        if (controlScrollOffset > 0) {
            drawString(poseStack, this.font, "<", x - 7, y - 5, ArcaneBeamGuiTheme.MUTED_TEXT);
        }
        if (controlScrollOffset < max) {
            drawString(poseStack, this.font, ">", right + 3, y - 5, ArcaneBeamGuiTheme.MUTED_TEXT);
        }
    }

    private void renderInputFrames(PoseStack poseStack) {
        renderInputFrame(poseStack, profileNameBox);
        renderInputFrame(poseStack, soundVolumeBox);
        renderInputFrame(poseStack, fadeInTicksBox);
        renderInputFrame(poseStack, fadeOutTicksBox);
        renderInputFrame(poseStack, lightningLifetimeBox);
        renderInputFrame(poseStack, lightningSideCountBox);
        renderInputFrame(poseStack, lightningRingColorBox);
        renderInputFrame(poseStack, lightningSphereColorBox);
        renderInputFrame(poseStack, lightningConeColorBox);
        renderInputFrame(poseStack, lightningSpotColorBox);
        renderInputFrame(poseStack, lightningSecondaryCountBox);
        renderInputFrame(poseStack, lightningSecondaryDelayBox);
        renderInputFrame(poseStack, lightningSoundVolumeBox);
        renderInputFrame(poseStack, altarVerticalTicksBox);
        renderInputFrame(poseStack, altarConvergeTicksBox);
        renderInputFrame(poseStack, altarCenterGrowTicksBox);
        renderInputFrame(poseStack, altarOriginHeightBox);
        renderInputFrame(poseStack, altarOriginRadiusBox);
        renderInputFrame(poseStack, altarSoundVolumeBox);
        renderInputFrame(poseStack, stormArrowLifetimeBox);
        renderInputFrame(poseStack, stormArrowOriginHeightBox);
        renderInputFrame(poseStack, archonMissileRadiusBox);
        renderInputFrame(poseStack, stormArrowSoundVolumeBox);
        renderInputFrame(poseStack, stormArrowAudioRangeBox);
        for (EditBox box : colorBoxes) {
            renderInputFrame(poseStack, box);
        }
        for (EditBox box : glowColorBoxes) {
            renderInputFrame(poseStack, box);
        }
        for (EditBox box : originBoxes) {
            renderInputFrame(poseStack, box);
        }
        for (EditBox box : altarColorBoxes) {
            renderInputFrame(poseStack, box);
        }
        for (EditBox box : stormArrowColorBoxes) {
            renderInputFrame(poseStack, box);
        }
    }

    private void renderInputFrame(PoseStack poseStack, EditBox box) {
        if (box != null && box.visible) {
            NineSliceRenderer.draw(poseStack, NineSliceRenderer.INSET, box.x - 4, box.y - 4, box.getInnerWidth() + 16, box.getHeight() + 8);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (drawerState == DrawerState.HIDDEN) {
            if (button == 0 && isInside(layoutMouseX, layoutMouseY, cleanViewHandleX(), cleanViewHandleY(), CLEAN_VIEW_HANDLE_WIDTH, CLEAN_VIEW_HANDLE_HEIGHT)) {
                exitCleanPreviewMode();
                return true;
            }
            return super.mouseClicked(layoutMouseX, layoutMouseY, button);
        }
        if (drawerState != DrawerState.EXPANDED) {
            return super.mouseClicked(layoutMouseX, layoutMouseY, button);
        }
        if (button == 0 && handleProfileDropdownSelection(layoutMouseX, layoutMouseY)) {
            return true;
        }
        selectLightningColorBox(layoutMouseX, layoutMouseY);
        if (button == 0 && handlePreviewSelection(layoutMouseX, layoutMouseY)) {
            openColorPicker();
            return true;
        }
        if (button == 0 && colorPickerOpen && layoutMouseX >= paletteX && layoutMouseX < paletteX + PALETTE_WIDTH && layoutMouseY >= paletteY && layoutMouseY < paletteY + PALETTE_HEIGHT) {
            draggingPalette = true;
            updatePaletteSelection(layoutMouseX, layoutMouseY);
            return true;
        }

        int brightnessX = paletteX + PALETTE_WIDTH + 10;
        if (button == 0 && colorPickerOpen && layoutMouseX >= brightnessX && layoutMouseX < brightnessX + BRIGHTNESS_WIDTH && layoutMouseY >= paletteY && layoutMouseY < paletteY + PALETTE_HEIGHT) {
            draggingBrightness = true;
            brightnessDragBaseColor = selectedColor();
            updateBrightnessSelection(layoutMouseY);
            return true;
        }
        if (button == 0 && colorPickerOpen && !isInsidePicker(layoutMouseX, layoutMouseY)) {
            closeColorPicker();
        }
        return super.mouseClicked(layoutMouseX, layoutMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (drawerState != DrawerState.EXPANDED || !colorPickerOpen) {
            return super.mouseDragged(layoutMouseX, layoutMouseY, button, dragX / layoutScale, dragY / layoutScale);
        }
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
        if (isInside(layoutMouseX, layoutMouseY, tabAreaX(), topBarY(), tabAreaWidth(), TOP_BAR_HEIGHT) && maxTabScrollOffset() > 0) {
            tabScrollOffset = clamp(tabScrollOffset + (delta > 0.0D ? -60 : 60), 0, maxTabScrollOffset());
            layoutTabs();
            return true;
        }
        if (drawerState != DrawerState.EXPANDED) {
            return super.mouseScrolled(layoutMouseX, layoutMouseY, delta);
        }
        if (!lightningSelected && !vaultAltarSelected && !stormLikeSelected() && soundVolumeBox != null && soundVolumeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
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
        if (vaultAltarSelected && altarSoundVolumeBox != null && altarSoundVolumeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeAltarSoundVolume(delta > 0.0D ? step : -step);
            refreshAltarSoundVolumeBox();
            ArcaneBeamConfig.save();
            return true;
        }
        if (stormLikeSelected() && stormArrowSoundVolumeBox != null && stormArrowSoundVolumeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeStormArrowSoundVolume(delta > 0.0D ? step : -step);
            refreshStormArrowSoundVolumeBox();
            ArcaneBeamConfig.save();
            return true;
        }
        if (stormLikeSelected() && stormArrowAudioRangeBox != null && stormArrowAudioRangeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            nudgeStormArrowAudioRange(delta > 0.0D ? 1 : -1);
            refreshStormArrowAudioRangeBox();
            ArcaneBeamConfig.save();
            return true;
        }
        if (stormLikeSelected() && stormArrowLifetimeBox != null && stormArrowLifetimeBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            int step = hasShiftDown() ? 10 : 1;
            nudgeStormArrowLifetime(delta > 0.0D ? step : -step);
            refreshStormArrowControls();
            ArcaneBeamConfig.save();
            return true;
        }
        if (stormLikeSelected() && stormArrowOriginHeightBox != null && stormArrowOriginHeightBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 1.0D : 0.1D;
            nudgeStormArrowOriginHeight(delta > 0.0D ? step : -step);
            refreshStormArrowControls();
            ArcaneBeamConfig.save();
            return true;
        }
        if (archonSelected && archonMissileRadiusBox != null && archonMissileRadiusBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 1.0D : 0.1D;
            nudgeArchonMissileRadius(delta > 0.0D ? step : -step);
            refreshStormArrowControls();
            ArcaneBeamConfig.save();
            return true;
        }
        if (vaultAltarSelected && altarOriginHeightBox != null && altarOriginHeightBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeAltarOriginHeight(delta > 0.0D ? step : -step);
            refreshAltarOriginBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        if (vaultAltarSelected && altarOriginRadiusBox != null && altarOriginRadiusBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            double step = hasShiftDown() ? 0.10D : 0.01D;
            nudgeAltarOriginRadius(delta > 0.0D ? step : -step);
            refreshAltarOriginBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        if (!lightningSelected && !vaultAltarSelected && !stormLikeSelected() && fadeInTicksBox != null && fadeInTicksBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            nudgeFadeInTicks(delta > 0.0D ? 1 : -1);
            refreshFadeTickBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        if (!lightningSelected && !vaultAltarSelected && !stormLikeSelected() && fadeOutTicksBox != null && fadeOutTicksBox.isMouseOver(layoutMouseX, layoutMouseY)) {
            nudgeFadeOutTicks(delta > 0.0D ? 1 : -1);
            refreshFadeTickBoxes();
            ArcaneBeamConfig.save();
            return true;
        }
        for (int i = 0; !lightningSelected && !vaultAltarSelected && !stormLikeSelected() && i < originBoxes.size(); i++) {
            EditBox originBox = originBoxes.get(i);
            if (originBox.isMouseOver(layoutMouseX, layoutMouseY)) {
                double step = hasShiftDown() ? 0.10D : 0.01D;
                nudgeOrigin(i, delta > 0.0D ? step : -step);
                refreshOriginBoxes();
                ArcaneBeamConfig.save();
                return true;
            }
        }
        if (maxControlScrollOffset() > 0 && isInsideControlViewport(layoutMouseX, layoutMouseY)) {
            controlScrollOffset = nextControlScrollOffset(delta < 0.0D);
            layoutWidgets();
            updateWidgetVisibility();
            return true;
        }
        return super.mouseScrolled(layoutMouseX, layoutMouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && colorPickerOpen) {
            closeColorPicker();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_H && !textInputFocused()) {
            if (drawerState == DrawerState.HIDDEN) {
                exitCleanPreviewMode();
            } else {
                enterCleanPreviewMode();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        restoreGameHud();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static boolean shouldSuppressGameHud() {
        return Minecraft.getInstance().screen instanceof ArcaneBeamConfigScreen screen && screen.gameHudSuppressed;
    }

    private void enterCleanPreviewMode() {
        if (drawerState != DrawerState.HIDDEN) {
            drawerStateBeforeCleanPreview = drawerState;
        }
        drawerState = DrawerState.HIDDEN;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private void exitCleanPreviewMode() {
        drawerState = drawerStateBeforeCleanPreview == DrawerState.HIDDEN ? DrawerState.EXPANDED : drawerStateBeforeCleanPreview;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private void openColorPicker() {
        colorPickerOpen = true;
        refreshPickerHexBox();
        layoutPickerWidgets();
        updateWidgetVisibility();
    }

    private void closeColorPicker() {
        colorPickerOpen = false;
        if (pickerHexBox != null) {
            pickerHexBox.setFocus(false);
        }
        updateWidgetVisibility();
    }

    private void suppressGameHud() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!gameHudSuppressed) {
            previousHideGui = minecraft.options.hideGui;
            gameHudSuppressed = true;
        }
        minecraft.options.hideGui = true;
    }

    private void restoreGameHud() {
        if (gameHudSuppressed) {
            Minecraft.getInstance().options.hideGui = previousHideGui;
            gameHudSuppressed = false;
        }
    }


    private boolean textInputFocused() {
        if (isFocused(profileNameBox) || isFocused(soundVolumeBox) || isFocused(fadeInTicksBox) || isFocused(fadeOutTicksBox)) {
            return true;
        }
        if (isFocused(lightningLifetimeBox) || isFocused(lightningSideCountBox) || isFocused(lightningRingColorBox) || isFocused(lightningSphereColorBox) || isFocused(lightningConeColorBox) || isFocused(lightningSpotColorBox) || isFocused(lightningSecondaryCountBox) || isFocused(lightningSecondaryDelayBox) || isFocused(lightningSoundVolumeBox)) {
            return true;
        }
        if (isFocused(altarVerticalTicksBox) || isFocused(altarConvergeTicksBox) || isFocused(altarCenterGrowTicksBox) || isFocused(altarOriginHeightBox) || isFocused(altarOriginRadiusBox) || isFocused(altarSoundVolumeBox)) {
            return true;
        }
        if (isFocused(stormArrowLifetimeBox) || isFocused(stormArrowOriginHeightBox) || isFocused(archonMissileRadiusBox) || isFocused(stormArrowSoundVolumeBox) || isFocused(stormArrowAudioRangeBox)) {
            return true;
        }
        if (isFocused(pickerHexBox)) {
            return true;
        }
        for (EditBox box : colorBoxes) {
            if (isFocused(box)) {
                return true;
            }
        }
        for (EditBox box : glowColorBoxes) {
            if (isFocused(box)) {
                return true;
            }
        }
        for (EditBox box : originBoxes) {
            if (isFocused(box)) {
                return true;
            }
        }
        for (EditBox box : altarColorBoxes) {
            if (isFocused(box)) {
                return true;
            }
        }
        for (EditBox box : stormArrowColorBoxes) {
            if (isFocused(box)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFocused(EditBox box) {
        return box != null && box.isFocused();
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
        for (EditBox altarColorBox : altarColorBoxes) {
            altarColorBox.tick();
        }
        for (EditBox stormArrowColorBox : stormArrowColorBoxes) {
            stormArrowColorBox.tick();
        }
        tickBox(altarVerticalTicksBox);
        tickBox(altarConvergeTicksBox);
        tickBox(altarCenterGrowTicksBox);
        tickBox(altarOriginHeightBox);
        tickBox(altarOriginRadiusBox);
        tickBox(altarSoundVolumeBox);
        tickBox(stormArrowLifetimeBox);
        tickBox(stormArrowOriginHeightBox);
        tickBox(stormArrowSoundVolumeBox);
        tickBox(stormArrowAudioRangeBox);
        tickBox(pickerHexBox);
    }

    private static void tickBox(EditBox box) {
        if (box != null) {
            box.tick();
        }
    }

    private void renderPalette(PoseStack poseStack) {
        int pickerX = pickerX();
        int pickerY = pickerY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GLASS_PANEL, pickerX, pickerY, pickerWidth(), pickerHeight());
        fill(poseStack, pickerX + 8, pickerY + 8, pickerX + pickerWidth() - 8, pickerY + pickerHeight() - 8, 0xD0080C12);
        drawString(poseStack, this.font, "Color Picker", pickerX + 12, pickerY + 10, ArcaneBeamGuiTheme.TEXT);
        drawString(poseStack, this.font, "Drag to edit", pickerX + 92, pickerY + 10, ArcaneBeamGuiTheme.MUTED_TEXT);
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.INSET, paletteX - 7, paletteY - 7, PALETTE_WIDTH + 10 + BRIGHTNESS_WIDTH + 14, PALETTE_HEIGHT + 14);
        for (int x = 0; x < PALETTE_WIDTH; x++) {
            for (int y = 0; y < PALETTE_HEIGHT; y++) {
                fill(poseStack, paletteX + x, paletteY + y, paletteX + x + 1, paletteY + y + 1, 0xFF000000 | colorAt(x, y));
            }
        }
        fill(poseStack, paletteX - 1, paletteY - 1, paletteX + PALETTE_WIDTH + 1, paletteY, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, paletteX - 1, paletteY + PALETTE_HEIGHT, paletteX + PALETTE_WIDTH + 1, paletteY + PALETTE_HEIGHT + 1, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, paletteX - 1, paletteY, paletteX, paletteY + PALETTE_HEIGHT, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, paletteX + PALETTE_WIDTH, paletteY, paletteX + PALETTE_WIDTH + 1, paletteY + PALETTE_HEIGHT, ArcaneBeamGuiTheme.GOLD_LINE);
        renderPickerHexRow(poseStack, pickerX, pickerY);
    }

    private void renderPickerHexRow(PoseStack poseStack, int pickerX, int pickerY) {
        int hexY = pickerY + pickerHeight() - 30;
        drawString(poseStack, this.font, "Hex", pickerX + 12, hexY + 6, ArcaneBeamGuiTheme.MUTED_TEXT);
        renderPreviewBox(poseStack, pickerX + 44, hexY, selectedColor(), true);
        renderInputFrame(poseStack, pickerHexBox);
    }

    private void renderBrightnessStrip(PoseStack poseStack) {
        int x = paletteX + PALETTE_WIDTH + 10;
        int baseColor = draggingBrightness ? brightnessDragBaseColor : selectedColor();
        for (int y = 0; y < PALETTE_HEIGHT; y++) {
            int color = 0xFF000000 | applyBrightness(baseColor, y);
            fill(poseStack, x, paletteY + y, x + BRIGHTNESS_WIDTH, paletteY + y + 1, color);
        }

        fill(poseStack, x - 1, paletteY - 1, x + BRIGHTNESS_WIDTH + 1, paletteY, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, x - 1, paletteY + PALETTE_HEIGHT, x + BRIGHTNESS_WIDTH + 1, paletteY + PALETTE_HEIGHT + 1, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, x - 1, paletteY, x, paletteY + PALETTE_HEIGHT, ArcaneBeamGuiTheme.GOLD_LINE);
        fill(poseStack, x + BRIGHTNESS_WIDTH, paletteY, x + BRIGHTNESS_WIDTH + 1, paletteY + PALETTE_HEIGHT, ArcaneBeamGuiTheme.GOLD_LINE);

    }

    private void renderInlinePreviews(PoseStack poseStack) {
        int beamY = colorBoxes.isEmpty() ? beamRowY() : colorBoxes.get(0).y;
        int glowY = glowColorBoxes.isEmpty() ? glowRowY() : glowColorBoxes.get(0).y;
        for (int i = 0; i < 4; i++) {
            int previewX = slotStartX(i);
            renderColorSlotLabel(poseStack, previewX, beamY, "Core " + (i + 1));
            renderColorSlotLabel(poseStack, previewX, glowY, "Glow " + (i + 1));
            renderPreviewBox(poseStack, previewX, beamY, settings().colors[i], i == selectedSlot && !glowColorsSelected);
            renderPreviewBox(poseStack, previewX, glowY, settings().glowColors[i], i == selectedSlot && glowColorsSelected);
        }
    }

    private void renderLightningColorPreviews(PoseStack poseStack) {
        if (lightningRingColorBox == null || lightningSphereColorBox == null || lightningConeColorBox == null || lightningSpotColorBox == null) {
            return;
        }
        renderPreviewBox(poseStack, lightningRingColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningRingColorBox.y, lightningSettings().ringColor, selectedSlot == 0);
        renderPreviewBox(poseStack, lightningSphereColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningSphereColorBox.y, lightningSettings().sphereColor, selectedSlot == 1);
        renderPreviewBox(poseStack, lightningConeColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningConeColorBox.y, lightningSettings().coneColor, selectedSlot == 2);
        renderPreviewBox(poseStack, lightningSpotColorBox.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, lightningSpotColorBox.y, lightningSettings().spotColor, selectedSlot == 3);
        renderColorSlotLabel(poseStack, lightningRingColorBox, "Ring");
        renderColorSlotLabel(poseStack, lightningSphereColorBox, "Sphere");
        renderColorSlotLabel(poseStack, lightningConeColorBox, "Cone");
        renderColorSlotLabel(poseStack, lightningSpotColorBox, "Spots");
    }

    private void renderColorSlotLabel(PoseStack poseStack, EditBox box, String label) {
        if (box == null) {
            return;
        }
        renderColorSlotLabel(poseStack, box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, box.y, label);
    }

    private void renderColorSlotLabel(PoseStack poseStack, int groupX, int rowY, String label) {
        int groupWidth = SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP + SLOT_HEX_WIDTH;
        int textX = groupX + groupWidth / 2 - this.font.width(label) / 2;
        drawString(poseStack, this.font, label, textX, rowY - 12, ArcaneBeamGuiTheme.MUTED_TEXT);
    }

    private void renderVaultAltarColorPreviews(PoseStack poseStack) {
        if (altarColorBoxes.size() < 6) {
            return;
        }
        for (int i = 0; i < altarColorBoxes.size(); i++) {
            EditBox box = altarColorBoxes.get(i);
            renderPreviewBox(poseStack, box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, box.y, vaultAltarColor(i), selectedSlot == i);
            renderColorSlotLabel(poseStack, box, vaultAltarColorLabel(i));
        }
    }

    private void renderStormArrowColorPreviews(PoseStack poseStack) {
        if (stormArrowColorBoxes.size() < 4) {
            return;
        }
        for (int i = 0; i < stormArrowColorBoxes.size(); i++) {
            EditBox box = stormArrowColorBoxes.get(i);
            renderPreviewBox(poseStack, box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP, box.y, stormArrowColor(i), selectedSlot == i);
            renderColorSlotLabel(poseStack, box, stormArrowColorLabel(i));
        }
    }

    private String stormArrowColorLabel(int slot) {
        if (archonSelected) {
            return switch (slot) {
                case 0 -> "Circle";
                case 1 -> "Plume";
                case 2 -> "Core";
                case 3 -> "Flash";
                default -> "";
            };
        }
        return switch (slot) {
            case 0 -> "Circle";
            case 1 -> "Blaster";
            case 2 -> "Core";
            default -> "Flash";
        };
    }

    private String vaultAltarColorLabel(int slot) {
        return switch (slot) {
            case 0 -> "Corner 1";
            case 1 -> "Corner 2";
            case 2 -> "Center 1";
            case 3 -> "Center 2";
            case 4 -> "Glow 1";
            default -> "Glow 2";
        };
    }

    private void renderProfilePanel(PoseStack poseStack) {
        drawString(poseStack, this.font, profileLabel(), profilePanelX(), profilePanelY(), ArcaneBeamGuiTheme.MUTED_TEXT);
    }

    private void renderLightningLabels(PoseStack poseStack) {
        if (lightningLifetimeBox == null || lightningSoundVolumeBox == null) {
            return;
        }
        drawControlLabel(poseStack, lightningLifetimeBox, "Lifetime", lightningLifetimeBox.x - 50, lightningLifetimeBox.y + 6);
        drawControlLabel(poseStack, lightningSideCountBox, "Sides", lightningSideCountBox.x - 42, lightningSideCountBox.y + 6);
        drawControlLabel(poseStack, lightningSecondaryCountBox, "Ripples", lightningSecondaryCountBox.x - 48, lightningSecondaryCountBox.y + 6);
        drawControlLabel(poseStack, lightningSecondaryDelayBox, "Delay", lightningSecondaryDelayBox.x - 42, lightningSecondaryDelayBox.y + 6);
        drawControlLabel(poseStack, lightningSoundVolumeBox, "Volume", lightningSoundVolumeBox.x - 50, lightningSoundVolumeBox.y + 6);
    }

    private void renderVaultAltarLabels(PoseStack poseStack) {
        if (altarVerticalTicksBox == null || altarCenterGrowTicksBox == null || altarOriginHeightBox == null || altarOriginRadiusBox == null || altarSoundVolumeBox == null) {
            return;
        }
        drawControlLabel(poseStack, altarVerticalTicksBox, "Vertical", altarVerticalTicksBox.x - 54, altarVerticalTicksBox.y + 6);
        drawControlLabel(poseStack, altarConvergeTicksBox, "Converge", altarConvergeTicksBox.x - 58, altarConvergeTicksBox.y + 6);
        drawControlLabel(poseStack, altarCenterGrowTicksBox, "Grow", altarCenterGrowTicksBox.x - 36, altarCenterGrowTicksBox.y + 6);
        drawControlLabel(poseStack, altarOriginHeightBox, "Height", altarOriginHeightBox.x - 46, altarOriginHeightBox.y + 6);
        drawControlLabel(poseStack, altarOriginRadiusBox, "Radius", altarOriginRadiusBox.x - 48, altarOriginRadiusBox.y + 6);
        drawControlLabel(poseStack, altarSoundVolumeBox, "Volume", altarSoundVolumeBox.x - 50, altarSoundVolumeBox.y + 6);
    }

    private void renderStormArrowLabels(PoseStack poseStack) {
        if (stormArrowLifetimeBox == null || stormArrowOriginHeightBox == null || stormArrowSoundVolumeBox == null || stormArrowAudioRangeBox == null) {
            return;
        }
        drawControlLabel(poseStack, stormArrowLifetimeBox, "Lifetime", stormArrowLifetimeBox.x - 54, stormArrowLifetimeBox.y + 6);
        drawControlLabel(poseStack, stormArrowOriginHeightBox, "Height", stormArrowOriginHeightBox.x - 46, stormArrowOriginHeightBox.y + 6);
        if (archonSelected && archonMissileRadiusBox != null) {
            drawControlLabel(poseStack, archonMissileRadiusBox, "Radius", archonMissileRadiusBox.x - 46, archonMissileRadiusBox.y + 6);
        }
        drawControlLabel(poseStack, stormArrowAudioRangeBox, "Audio Range", stormArrowAudioRangeBox.x - this.font.width("Audio Range") - 8, stormArrowAudioRangeBox.y + 6);
        drawControlLabel(poseStack, stormArrowSoundVolumeBox, "Volume", stormArrowSoundVolumeBox.x - 50, stormArrowSoundVolumeBox.y + 6);
    }

    private void drawControlLabel(PoseStack poseStack, AbstractWidget widget, String label, int x, int y) {
        if (widget == null || !widget.visible) {
            return;
        }
        int width = this.font.width(label);
        if (x < controlViewportX() || x + width > controlViewportRight()) {
            return;
        }
        drawString(poseStack, this.font, label, x, y, 0xD8D8D8);
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
        if (isInside(mouseX, mouseY, profileX, profilePanelY() + 42, PROFILE_PANEL_WIDTH, 20)) {
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
        return profilePanelY() + 64;
    }

    private int profilePanelX() {
        return profileCardX() + 12;
    }

    private void addProfile() {
        if (archonSelected) {
            ArcaneBeamConfig.addArchonProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else if (smiteSelected) {
            ArcaneBeamConfig.addSmiteProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else if (stormArrowSelected) {
            ArcaneBeamConfig.addStormArrowProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else if (vaultAltarSelected) {
            ArcaneBeamConfig.addVaultAltarProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else if (lightningSelected) {
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
        NineSliceRenderer.draw(poseStack, selected ? NineSliceRenderer.BUTTON_ACTIVE : NineSliceRenderer.COLOR_SLOT, x - 2, y - 2, SLOT_PREVIEW_WIDTH + 4, 24);
        fill(poseStack, x + 2, y + 2, x + SLOT_PREVIEW_WIDTH - 2, y + 18, 0xFF000000 | color);
    }

    private boolean handlePreviewSelection(double mouseX, double mouseY) {
        if (stormLikeSelected()) {
            for (int i = 0; i < stormArrowColorBoxes.size(); i++) {
                EditBox box = stormArrowColorBoxes.get(i);
                int previewX = box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP;
                if (isInside(mouseX, mouseY, previewX, box.y, SLOT_PREVIEW_WIDTH, 20)) {
                    selectedSlot = i;
                    return true;
                }
            }
            return false;
        }
        if (vaultAltarSelected) {
            for (int i = 0; i < altarColorBoxes.size(); i++) {
                EditBox box = altarColorBoxes.get(i);
                int previewX = box.x - SLOT_PREVIEW_WIDTH - SLOT_INNER_GAP;
                if (isInside(mouseX, mouseY, previewX, box.y, SLOT_PREVIEW_WIDTH, 20)) {
                    selectedSlot = i;
                    return true;
                }
            }
            return false;
        }
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
        if (stormLikeSelected()) {
            for (int i = 0; i < stormArrowColorBoxes.size(); i++) {
                EditBox box = stormArrowColorBoxes.get(i);
                if (isInside(mouseX, mouseY, box.x, box.y, SLOT_HEX_WIDTH, 20)) {
                    selectedSlot = i;
                    openColorPicker();
                    return;
                }
            }
            return;
        }
        if (vaultAltarSelected) {
            for (int i = 0; i < altarColorBoxes.size(); i++) {
                EditBox box = altarColorBoxes.get(i);
                if (isInside(mouseX, mouseY, box.x, box.y, SLOT_HEX_WIDTH, 20)) {
                    selectedSlot = i;
                    openColorPicker();
                    return;
                }
            }
            return;
        }
        if (!lightningSelected) {
            for (int i = 0; i < colorBoxes.size(); i++) {
                EditBox box = colorBoxes.get(i);
                if (isInside(mouseX, mouseY, box.x, box.y, SLOT_HEX_WIDTH, 20)) {
                    selectedSlot = i;
                    glowColorsSelected = false;
                    openColorPicker();
                    return;
                }
            }
            for (int i = 0; i < glowColorBoxes.size(); i++) {
                EditBox box = glowColorBoxes.get(i);
                if (isInside(mouseX, mouseY, box.x, box.y, SLOT_HEX_WIDTH, 20)) {
                    selectedSlot = i;
                    glowColorsSelected = true;
                    openColorPicker();
                    return;
                }
            }
            return;
        }
        if (lightningRingColorBox == null || lightningSphereColorBox == null || lightningConeColorBox == null || lightningSpotColorBox == null) {
            return;
        }
        EditBox[] boxes = {lightningRingColorBox, lightningSphereColorBox, lightningConeColorBox, lightningSpotColorBox};
        for (int i = 0; i < boxes.length; i++) {
            if (isInside(mouseX, mouseY, boxes[i].x, boxes[i].y, SLOT_HEX_WIDTH, 20)) {
                selectedSlot = i;
                openColorPicker();
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
        int centered = colorCardX() + colorCardWidth() / 2 - rowWidth() / 2;
        int min = colorCardX() + 28;
        int max = colorCardX() + colorCardWidth() - rowWidth() - 18;
        return clamp(centered, min, max);
    }

    private int beamRowY() {
        return colorCardY() + 48;
    }

    private int glowRowY() {
        return beamRowY() + 40;
    }

    private int altarSecondColorRowY() {
        return beamRowY() + 52;
    }

    private static int rowWidth() {
        return slotWidth() * 4 + SLOT_GAP * 3;
    }

    private static int slotWidth() {
        return SLOT_PREVIEW_WIDTH + SLOT_INNER_GAP + SLOT_HEX_WIDTH;
    }

    private void refreshBoxes() {
        if (stormLikeSelected()) {
            for (int i = 0; i < stormArrowColorBoxes.size(); i++) {
                stormArrowColorBoxes.get(i).setValue(formatColor(stormArrowColor(i)));
            }
            refreshPickerHexBox();
            return;
        }
        if (vaultAltarSelected) {
            for (int i = 0; i < altarColorBoxes.size(); i++) {
                altarColorBoxes.get(i).setValue(formatColor(vaultAltarColor(i)));
            }
            refreshPickerHexBox();
            return;
        }
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
            refreshPickerHexBox();
            return;
        }
        for (int i = 0; i < colorBoxes.size(); i++) {
            colorBoxes.get(i).setValue(formatColor(settings().colors[i]));
        }
        for (int i = 0; i < glowColorBoxes.size(); i++) {
            glowColorBoxes.get(i).setValue(formatColor(settings().glowColors[i]));
        }
        refreshPickerHexBox();
    }

    private void refreshControls() {
        refreshBoxes();
        layoutWidgets();
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
            fitButton(shaderCompatibilityButton, 150);
            fitButton(handButton, 150);
            fitButton(soundButton, 184);
            fitButton(fadeInModeButton, 122);
            fitButton(fadeOutModeButton, 122);
            refreshOriginBoxes();
            refreshSoundVolumeBox();
            refreshFadeTickBoxes();
            refreshLightningControls();
            refreshVaultAltarControls();
            refreshStormArrowControls();
            layoutWidgets();
            updateWidgetVisibility();
        }
    }

    private void updateWidgetVisibility() {
        boolean chromeVisible = drawerState != DrawerState.HIDDEN;
        boolean drawerOpen = drawerState == DrawerState.EXPANDED;
        if (!drawerOpen) {
            profileDropdownOpen = false;
        }
        if (chromeVisible) {
            layoutTabs();
        } else {
            for (Button tabButton : tabButtons) {
                setVisible(tabButton, false);
            }
            setVisible(tabScrollLeftButton, false);
            setVisible(tabScrollRightButton, false);
        }
        setVisible(drawerToggleButton, chromeVisible);
        setVisible(hideChromeButton, chromeVisible);
        setVisible(doneButton, chromeVisible);
        setVisible(pickerHexBox, drawerOpen && colorPickerOpen);

        boolean beamVisible = drawerOpen && !lightningSelected && !vaultAltarSelected && !stormLikeSelected();
        boolean lightningVisible = drawerOpen && lightningSelected;
        boolean altarVisible = drawerOpen && vaultAltarSelected;
        boolean stormVisible = drawerOpen && stormLikeSelected();
        for (EditBox box : colorBoxes) {
            box.visible = beamVisible;
            box.active = beamVisible;
        }
        for (EditBox box : glowColorBoxes) {
            box.visible = beamVisible;
            box.active = beamVisible;
        }
        for (EditBox box : originBoxes) {
            setControlVisible(box, beamVisible);
        }
        for (Button button : originLabelButtons) {
            setControlVisible(button, beamVisible);
        }
        setVisible(profileNameBox, drawerOpen);
        setVisible(profileAddButton, drawerOpen);
        setVisible(profileDropdownButton, drawerOpen);
        setControlVisible(soundVolumeBox, beamVisible);
        setControlVisible(fadeInTicksBox, beamVisible);
        setControlVisible(fadeOutTicksBox, beamVisible);
        setControlVisible(intensitySlider, beamVisible);
        setControlVisible(opacitySlider, beamVisible);
        setControlVisible(glowRadiusSlider, beamVisible);
        setControlVisible(glowOpacitySlider, beamVisible);
        setControlVisible(colorShiftSlider, beamVisible);
        setControlVisible(glowRotationSlider, beamVisible);
        setControlVisible(shaderCompatibilityButton, beamVisible);
        setControlVisible(soundButton, beamVisible);
        setControlVisible(handButton, beamVisible);
        setControlVisible(fadeInModeButton, beamVisible);
        setControlVisible(fadeOutModeButton, beamVisible);

        setControlVisible(lightningEnabledButton, lightningVisible);
        setControlVisible(lightningShaderCompatibilityButton, lightningVisible);
        setControlVisible(lightningFullbrightButton, lightningVisible);
        setControlVisible(lightningSoundButton, lightningVisible);
        setControlVisible(lightningStartRadiusSlider, lightningVisible);
        setControlVisible(lightningEndRadiusSlider, lightningVisible);
        setControlVisible(lightningThicknessSlider, lightningVisible);
        setControlVisible(lightningAlphaSlider, lightningVisible);
        setControlVisible(lightningInteriorOpacitySlider, lightningVisible);
        setControlVisible(lightningSphereRadiusSlider, lightningVisible);
        setControlVisible(lightningSphereOpacitySlider, lightningVisible);
        setControlVisible(lightningConeHeightSlider, lightningVisible);
        setControlVisible(lightningConeRadiusSlider, lightningVisible);
        setControlVisible(lightningConeOpacitySlider, lightningVisible);
        setControlVisible(lightningSpotCountSlider, lightningVisible);
        setControlVisible(lightningSpotSizeSlider, lightningVisible);
        setControlVisible(lightningSpotOpacitySlider, lightningVisible);
        setControlVisible(lightningSecondarySizeSlider, lightningVisible);
        setControlVisible(lightningLifetimeBox, lightningVisible);
        setControlVisible(lightningSideCountBox, lightningVisible);
        setVisible(lightningRingColorBox, lightningVisible);
        setVisible(lightningSphereColorBox, lightningVisible);
        setVisible(lightningConeColorBox, lightningVisible);
        setVisible(lightningSpotColorBox, lightningVisible);
        setControlVisible(lightningSecondaryCountBox, lightningVisible);
        setControlVisible(lightningSecondaryDelayBox, lightningVisible);
        setControlVisible(lightningSoundVolumeBox, lightningVisible);

        for (EditBox box : altarColorBoxes) {
            box.visible = altarVisible;
            box.active = altarVisible;
        }
        setControlVisible(altarEnabledButton, altarVisible);
        setControlVisible(altarShaderCompatibilityButton, altarVisible);
        setControlVisible(altarFullbrightButton, altarVisible);
        setControlVisible(altarOriginMarkersButton, altarVisible);
        setControlVisible(altarSoundButton, altarVisible);
        setControlVisible(altarCornerRadiusSlider, altarVisible);
        setControlVisible(altarCornerOpacitySlider, altarVisible);
        setControlVisible(altarCenterHeightSlider, altarVisible);
        setControlVisible(altarCenterFadeSlider, altarVisible);
        setControlVisible(altarCenterBottomRadiusSlider, altarVisible);
        setControlVisible(altarCenterTopRadiusSlider, altarVisible);
        setControlVisible(altarCenterOpacitySlider, altarVisible);
        setControlVisible(altarGlowHeightSlider, altarVisible);
        setControlVisible(altarGlowFadeSlider, altarVisible);
        setControlVisible(altarGlowBottomRadiusSlider, altarVisible);
        setControlVisible(altarGlowTopRadiusSlider, altarVisible);
        setControlVisible(altarGlowOpacitySlider, altarVisible);
        setControlVisible(altarGlowRotationSlider, altarVisible);
        setControlVisible(altarVerticalTicksBox, altarVisible);
        setControlVisible(altarConvergeTicksBox, altarVisible);
        setControlVisible(altarCenterGrowTicksBox, altarVisible);
        setControlVisible(altarOriginHeightBox, altarVisible);
        setControlVisible(altarOriginRadiusBox, altarVisible);
        setControlVisible(altarSoundVolumeBox, altarVisible);

        for (EditBox box : stormArrowColorBoxes) {
            box.visible = stormVisible;
            box.active = stormVisible;
        }
        setControlVisible(stormArrowEnabledButton, stormVisible);
        setControlVisible(stormArrowTargetingCircleButton, stormVisible);
        setControlVisible(stormArrowActualRadiusButton, stormVisible);
        setControlVisible(stormArrowShaderCompatibilityButton, stormVisible);
        setControlVisible(stormArrowFullbrightButton, stormVisible);
        setControlVisible(stormArrowImpactFlashButton, stormVisible);
        setControlVisible(stormArrowSoundButton, stormVisible);
        setControlVisible(stormArrowProjectileSoundButton, stormVisible);
        setControlVisible(stormArrowCircleAlphaSlider, stormVisible);
        setControlVisible(stormArrowCircleThicknessSlider, stormVisible);
        setControlVisible(stormArrowBlasterAlphaSlider, stormVisible);
        setControlVisible(stormArrowBlasterWidthSlider, stormVisible);
        setControlVisible(stormArrowSegmentLengthSlider, stormVisible);
        setControlVisible(stormArrowSegmentGapSlider, stormVisible && !archonSelected);
        setControlVisible(stormArrowImpactFlashSizeSlider, stormVisible);
        setControlVisible(stormArrowLifetimeBox, stormVisible);
        setControlVisible(stormArrowOriginHeightBox, stormVisible);
        setControlVisible(archonMissileRadiusBox, drawerOpen && archonSelected);
        setControlVisible(stormArrowSoundVolumeBox, stormVisible);
        setControlVisible(stormArrowAudioRangeBox, stormVisible);
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible) {
        if (widget != null) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private void setControlVisible(AbstractWidget widget, boolean visible) {
        boolean finalVisible = visible && widgetInsideControlViewport(widget);
        setVisible(widget, finalVisible);
        if (!finalVisible && widget instanceof EditBox editBox) {
            editBox.setFocus(false);
        }
    }

    private boolean widgetInsideControlViewport(AbstractWidget widget) {
        return widget != null
                && widget.x >= controlViewportX()
                && widget.x + widget.getWidth() <= controlViewportRight();
    }

    private void fitButton(Button button, int maxWidth) {
        if (button != null) {
            button.setWidth(Math.min(maxWidth, Math.max(44, this.font.width(button.getMessage().getString()) + 18)));
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
        fitButton(lightningEnabledButton, 150);
        fitButton(lightningShaderCompatibilityButton, 150);
        fitButton(lightningFullbrightButton, 150);
        fitButton(lightningSoundButton, 150);
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

    private void refreshVaultAltarControls() {
        if (altarEnabledButton == null) {
            return;
        }
        ArcaneBeamConfig.VaultAltarSettings settings = vaultAltarSettings();
        altarEnabledButton.setMessage(new TextComponent("Replacement: " + (settings.enabled ? "On" : "Off")));
        altarShaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + vaultAltarShaderCompatibility().label));
        altarFullbrightButton.setMessage(new TextComponent("Fullbright: " + (settings.fullbright ? "On" : "Off")));
        altarOriginMarkersButton.setMessage(new TextComponent("Origin Markers: " + (settings.originMarkersEnabled ? "On" : "Off")));
        altarSoundButton.setMessage(new TextComponent("Sound: " + vaultAltarSoundMode().label));
        fitButton(altarEnabledButton, 150);
        fitButton(altarShaderCompatibilityButton, 150);
        fitButton(altarFullbrightButton, 150);
        fitButton(altarOriginMarkersButton, 150);
        fitButton(altarSoundButton, 150);
        altarCornerRadiusSlider.refresh();
        altarCornerOpacitySlider.refresh();
        altarCenterHeightSlider.refresh();
        altarCenterFadeSlider.refresh();
        altarCenterBottomRadiusSlider.refresh();
        altarCenterTopRadiusSlider.refresh();
        altarCenterOpacitySlider.refresh();
        altarGlowHeightSlider.refresh();
        altarGlowFadeSlider.refresh();
        altarGlowBottomRadiusSlider.refresh();
        altarGlowTopRadiusSlider.refresh();
        altarGlowOpacitySlider.refresh();
        altarGlowRotationSlider.refresh();
        altarVerticalTicksBox.setValue(Integer.toString(settings.cornerVerticalTicks));
        altarConvergeTicksBox.setValue(Integer.toString(settings.cornerConvergeTicks));
        altarCenterGrowTicksBox.setValue(Integer.toString(settings.centerGrowTicks));
        refreshAltarOriginBoxes();
        refreshAltarSoundVolumeBox();
    }

    private void refreshStormArrowControls() {
        if (stormArrowEnabledButton == null) {
            return;
        }
        ArcaneBeamConfig.StormArrowSettings settings = stormArrowSettings();
        stormArrowEnabledButton.setMessage(new TextComponent("Replacement: " + (settings.enabled ? "On" : "Off")));
        stormArrowTargetingCircleButton.setMessage(new TextComponent("Target Circle: " + (settings.showTargetingCircle ? "On" : "Off")));
        stormArrowActualRadiusButton.setMessage(new TextComponent("Actual Radius: " + (settings.useActualRadius ? "On" : "Off")));
        stormArrowShaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + stormArrowShaderCompatibility().label));
        stormArrowFullbrightButton.setMessage(new TextComponent("Fullbright: " + (settings.fullbright ? "On" : "Off")));
        stormArrowImpactFlashButton.setMessage(new TextComponent("Impact Flash: " + (settings.impactFlashEnabled ? "On" : "Off")));
        stormArrowBlasterAlphaSlider.setLabel(archonSelected ? "Missile Alpha" : "Blaster Alpha");
        stormArrowBlasterWidthSlider.setLabel(archonSelected ? "Missile Width" : "Blaster Width");
        stormArrowSegmentLengthSlider.setLabel(archonSelected ? "Plume Length" : "Bolt Length");
        stormArrowSoundButton.setMessage(new TextComponent("Strike: " + stormArrowSoundLabel()));
        stormArrowProjectileSoundButton.setMessage(new TextComponent((smiteSelected || archonSelected ? "Activation: " : "Projectile: ") + stormArrowProjectileSoundMode().label));
        fitButton(stormArrowEnabledButton, 150);
        fitButton(stormArrowTargetingCircleButton, 150);
        fitButton(stormArrowActualRadiusButton, 150);
        fitButton(stormArrowShaderCompatibilityButton, 150);
        fitButton(stormArrowFullbrightButton, 150);
        fitButton(stormArrowImpactFlashButton, 150);
        fitButton(stormArrowSoundButton, 150);
        fitButton(stormArrowProjectileSoundButton, 150);
        stormArrowCircleAlphaSlider.refresh();
        stormArrowCircleThicknessSlider.refresh();
        stormArrowBlasterAlphaSlider.refresh();
        stormArrowBlasterWidthSlider.refresh();
        stormArrowSegmentLengthSlider.refresh();
        stormArrowSegmentGapSlider.refresh();
        stormArrowImpactFlashSizeSlider.refresh();
        stormArrowLifetimeBox.setValue(Integer.toString(settings.lifetimeTicks));
        stormArrowOriginHeightBox.setValue(formatTenths(settings.originHeight));
        if (archonMissileRadiusBox != null) {
            archonMissileRadiusBox.setValue(formatTenths(archonSettings().missileOriginRadius));
        }
        refreshStormArrowSoundVolumeBox();
        refreshStormArrowAudioRangeBox();
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

    private void updatePickerHexFromText(String value) {
        if (refreshingPickerHexBox || value == null) {
            return;
        }

        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }

        try {
            setSelectedColor(Integer.parseInt(normalized, 16));
            refreshBoxes();
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void refreshPickerHexBox() {
        if (pickerHexBox == null) {
            return;
        }
        refreshingPickerHexBox = true;
        pickerHexBox.setValue(formatColor(selectedColor()));
        refreshingPickerHexBox = false;
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

    private void updateAltarColorFromText(int slot, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }
        try {
            setVaultAltarColor(slot, Integer.parseInt(normalized, 16));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowColorFromText(int slot, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }
        try {
            setStormArrowColor(slot, Integer.parseInt(normalized, 16));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarVerticalTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().cornerVerticalTicks = clamp(Integer.parseInt(value), 0, 60);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarConvergeTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().cornerConvergeTicks = clamp(Integer.parseInt(value), 1, 120);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarCenterGrowTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().centerGrowTicks = clamp(Integer.parseInt(value), 1, 160);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarOriginHeightFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().cornerOriginHeight = clampHundredths((float) roundHundredths(Double.parseDouble(value)), 0.0F, 15.0F);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarOriginRadiusFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().cornerOriginRadius = clampHundredths((float) roundHundredths(Double.parseDouble(value)), 0.0F, 15.0F);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowLifetimeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            stormArrowSettings().lifetimeTicks = clamp(Integer.parseInt(value), 1, 80);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowOriginHeightFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            stormArrowSettings().originHeight = clampTenths((float) roundTenths(Double.parseDouble(value)), 2.0F, 64.0F);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateArchonMissileRadiusFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            archonSettings().missileOriginRadius = clampTenths((float) roundTenths(Double.parseDouble(value)), 0.0F, 16.0F);
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            stormArrowSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowAudioRangeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            stormArrowSettings().audioRange = clampStormArrowAudioRange(Integer.parseInt(value));
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

    private void nudgeAltarSoundVolume(double amount) {
        vaultAltarSettings().soundVolume = clampSoundVolume((float) roundOffset(vaultAltarSettings().soundVolume + amount));
    }

    private void nudgeStormArrowSoundVolume(double amount) {
        stormArrowSettings().soundVolume = clampSoundVolume((float) roundOffset(stormArrowSettings().soundVolume + amount));
    }

    private void nudgeStormArrowAudioRange(int amount) {
        stormArrowSettings().audioRange = clampStormArrowAudioRange(stormArrowSettings().audioRange + amount);
    }

    private void nudgeStormArrowLifetime(int amount) {
        stormArrowSettings().lifetimeTicks = clamp(stormArrowSettings().lifetimeTicks + amount, 1, 80);
    }

    private void nudgeStormArrowOriginHeight(double amount) {
        stormArrowSettings().originHeight = clampTenths((float) roundTenths(stormArrowSettings().originHeight + amount), 2.0F, 64.0F);
    }

    private void nudgeArchonMissileRadius(double amount) {
        archonSettings().missileOriginRadius = clampTenths((float) roundTenths(archonSettings().missileOriginRadius + amount), 0.0F, 16.0F);
    }

    private void nudgeAltarOriginHeight(double amount) {
        vaultAltarSettings().cornerOriginHeight = clampHundredths((float) roundHundredths(vaultAltarSettings().cornerOriginHeight + amount), 0.0F, 15.0F);
    }

    private void nudgeAltarOriginRadius(double amount) {
        vaultAltarSettings().cornerOriginRadius = clampHundredths((float) roundHundredths(vaultAltarSettings().cornerOriginRadius + amount), 0.0F, 15.0F);
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

    private ArcaneBeamConfig.VaultAltarSettings vaultAltarSettings() {
        return ArcaneBeamConfig.INSTANCE.vaultAltar;
    }

    private ArcaneBeamConfig.StormArrowSettings stormArrowSettings() {
        if (archonSelected) {
            return ArcaneBeamConfig.INSTANCE.archon;
        }
        return smiteSelected ? ArcaneBeamConfig.INSTANCE.smite : ArcaneBeamConfig.INSTANCE.stormArrow;
    }

    private ArcaneBeamConfig.ArchonSettings archonSettings() {
        return ArcaneBeamConfig.INSTANCE.archon;
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

    private void refreshAltarSoundVolumeBox() {
        if (altarSoundVolumeBox != null) {
            altarSoundVolumeBox.setValue(formatOffset(vaultAltarSettings().soundVolume));
        }
    }

    private void refreshStormArrowSoundVolumeBox() {
        if (stormArrowSoundVolumeBox != null) {
            stormArrowSoundVolumeBox.setValue(formatOffset(stormArrowSettings().soundVolume));
        }
    }

    private void refreshStormArrowAudioRangeBox() {
        if (stormArrowAudioRangeBox != null) {
            stormArrowAudioRangeBox.setValue(Integer.toString(stormArrowSettings().audioRange));
        }
    }

    private void refreshAltarOriginBoxes() {
        if (altarOriginHeightBox != null) {
            altarOriginHeightBox.setValue(formatHundredths(vaultAltarSettings().cornerOriginHeight));
        }
        if (altarOriginRadiusBox != null) {
            altarOriginRadiusBox.setValue(formatHundredths(vaultAltarSettings().cornerOriginRadius));
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
        if (stormLikeSelected()) {
            return stormArrowColor(selectedSlot);
        }
        if (vaultAltarSelected) {
            return vaultAltarColor(selectedSlot);
        }
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
        if (stormLikeSelected()) {
            setStormArrowColor(selectedSlot, color);
            return;
        }
        if (vaultAltarSelected) {
            setVaultAltarColor(selectedSlot, color);
            return;
        }
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

    private int vaultAltarColor(int slot) {
        return switch (slot) {
            case 0 -> vaultAltarSettings().cornerColors[0];
            case 1 -> vaultAltarSettings().cornerColors[1];
            case 2 -> vaultAltarSettings().centerColors[0];
            case 3 -> vaultAltarSettings().centerColors[1];
            case 4 -> vaultAltarSettings().centerGlowColors[0];
            case 5 -> vaultAltarSettings().centerGlowColors[1];
            default -> vaultAltarSettings().cornerColors[0];
        };
    }

    private int stormArrowColor(int slot) {
        return switch (slot) {
            case 0 -> stormArrowSettings().circleColor;
            case 1 -> stormArrowSettings().blasterColor;
            case 2 -> stormArrowSettings().coreColor;
            case 3 -> stormArrowSettings().impactFlashColor;
            default -> stormArrowSettings().circleColor;
        };
    }

    private void setVaultAltarColor(int slot, int color) {
        switch (slot) {
            case 0 -> vaultAltarSettings().cornerColors[0] = color;
            case 1 -> vaultAltarSettings().cornerColors[1] = color;
            case 2 -> vaultAltarSettings().centerColors[0] = color;
            case 3 -> vaultAltarSettings().centerColors[1] = color;
            case 4 -> vaultAltarSettings().centerGlowColors[0] = color;
            case 5 -> vaultAltarSettings().centerGlowColors[1] = color;
            default -> {
            }
        }
    }

    private void setStormArrowColor(int slot, int color) {
        switch (slot) {
            case 0 -> stormArrowSettings().circleColor = color;
            case 1 -> stormArrowSettings().blasterColor = color;
            case 2 -> stormArrowSettings().coreColor = color;
            case 3 -> stormArrowSettings().impactFlashColor = color;
            default -> {
            }
        }
    }

    private void syncPrimaryColors() {
        settings().color = settings().colors[0];
        settings().glowColor = settings().glowColors[0];
    }

    private List<String> profileNames() {
        if (archonSelected) {
            return ArcaneBeamConfig.archonProfileNames();
        }
        if (smiteSelected) {
            return ArcaneBeamConfig.smiteProfileNames();
        }
        if (stormArrowSelected) {
            return ArcaneBeamConfig.stormArrowProfileNames();
        }
        if (vaultAltarSelected) {
            return ArcaneBeamConfig.vaultAltarProfileNames();
        }
        return lightningSelected ? ArcaneBeamConfig.lightningProfileNames() : ArcaneBeamConfig.profileNames(railSelected);
    }

    private String selectedProfileName() {
        if (archonSelected) {
            return ArcaneBeamConfig.selectedArchonProfileName();
        }
        if (smiteSelected) {
            return ArcaneBeamConfig.selectedSmiteProfileName();
        }
        if (stormArrowSelected) {
            return ArcaneBeamConfig.selectedStormArrowProfileName();
        }
        if (vaultAltarSelected) {
            return ArcaneBeamConfig.selectedVaultAltarProfileName();
        }
        return lightningSelected ? ArcaneBeamConfig.selectedLightningProfileName() : ArcaneBeamConfig.selectedProfileName(railSelected);
    }

    private void selectProfile(String profileName) {
        if (archonSelected) {
            ArcaneBeamConfig.selectArchonProfile(profileName);
        } else if (smiteSelected) {
            ArcaneBeamConfig.selectSmiteProfile(profileName);
        } else if (stormArrowSelected) {
            ArcaneBeamConfig.selectStormArrowProfile(profileName);
        } else if (vaultAltarSelected) {
            ArcaneBeamConfig.selectVaultAltarProfile(profileName);
        } else if (lightningSelected) {
            ArcaneBeamConfig.selectLightningProfile(profileName);
        } else {
            ArcaneBeamConfig.selectProfile(railSelected, profileName);
        }
    }

    private String profileLabel() {
        if (archonSelected) {
            return "Archon Profiles";
        }
        if (smiteSelected) {
            return "Smite Profiles";
        }
        if (stormArrowSelected) {
            return "Storm Arrow Profiles";
        }
        if (vaultAltarSelected) {
            return "Vault Altar Profiles";
        }
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

    private ArcaneBeamConfig.ShaderCompatibility vaultAltarShaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(vaultAltarSettings().shaderCompatibility);
        return compatibility == null ? ArcaneBeamConfig.ShaderCompatibility.OFF : compatibility;
    }

    private ArcaneBeamConfig.ShaderCompatibility stormArrowShaderCompatibility() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(stormArrowSettings().shaderCompatibility);
        return compatibility == null ? ArcaneBeamConfig.ShaderCompatibility.OFF : compatibility;
    }

    private ArcaneBeamConfig.LightningSoundMode lightningSoundMode() {
        ArcaneBeamConfig.LightningSoundMode mode = ArcaneBeamConfig.LightningSoundMode.fromId(lightningSettings().soundMode);
        return mode == null ? ArcaneBeamConfig.LightningSoundMode.DEFAULT : mode;
    }

    private ArcaneBeamConfig.VaultAltarSoundMode vaultAltarSoundMode() {
        ArcaneBeamConfig.VaultAltarSoundMode mode = ArcaneBeamConfig.VaultAltarSoundMode.fromId(vaultAltarSettings().soundMode);
        return mode == null ? ArcaneBeamConfig.VaultAltarSoundMode.DEFAULT : mode;
    }

    private ArcaneBeamConfig.StormArrowSoundMode stormArrowSoundMode() {
        ArcaneBeamConfig.StormArrowSoundMode mode = ArcaneBeamConfig.StormArrowSoundMode.fromId(stormArrowSettings().soundMode);
        return mode == null ? ArcaneBeamConfig.StormArrowSoundMode.DEFAULT : mode;
    }

    private String stormArrowSoundLabel() {
        ArcaneBeamConfig.StormArrowSoundMode mode = stormArrowSoundMode();
        if (archonSelected && mode == ArcaneBeamConfig.StormArrowSoundMode.BLASTER) {
            return "Whistling Bird";
        }
        return mode.label;
    }

    private ArcaneBeamConfig.StormArrowProjectileSoundMode stormArrowProjectileSoundMode() {
        ArcaneBeamConfig.StormArrowProjectileSoundMode mode = ArcaneBeamConfig.StormArrowProjectileSoundMode.fromId(stormArrowSettings().projectileSoundMode);
        return mode == null ? ArcaneBeamConfig.StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private void cycleLightningSound() {
        ArcaneBeamConfig.LightningSoundMode[] modes = ArcaneBeamConfig.LightningSoundMode.values();
        ArcaneBeamConfig.LightningSoundMode current = lightningSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        lightningSettings().soundMode = modes[next].id;
    }

    private void cycleVaultAltarSound() {
        ArcaneBeamConfig.VaultAltarSoundMode[] modes = ArcaneBeamConfig.VaultAltarSoundMode.values();
        ArcaneBeamConfig.VaultAltarSoundMode current = vaultAltarSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        vaultAltarSettings().soundMode = modes[next].id;
    }

    private void cycleStormArrowSound() {
        ArcaneBeamConfig.StormArrowSoundMode[] modes = ArcaneBeamConfig.StormArrowSoundMode.values();
        ArcaneBeamConfig.StormArrowSoundMode current = stormArrowSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        stormArrowSettings().soundMode = modes[next].id;
    }

    private void cycleStormArrowProjectileSound() {
        ArcaneBeamConfig.StormArrowProjectileSoundMode[] modes = ArcaneBeamConfig.StormArrowProjectileSoundMode.values();
        ArcaneBeamConfig.StormArrowProjectileSoundMode current = stormArrowProjectileSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        stormArrowSettings().projectileSoundMode = modes[next].id;
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

    private static String formatTenths(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static double roundTenths(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private static String formatHundredths(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static double roundHundredths(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static float clampTenths(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampHundredths(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampSoundVolume(float value) {
        return Math.max(0.0F, Math.min(2.0F, value));
    }

    private static int clampStormArrowAudioRange(int value) {
        return Math.max(16, Math.min(32, value));
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

    private static class ThemeButton extends Button {
        private boolean selected;
        private int accent = ArcaneBeamGuiTheme.GOLD;

        ThemeButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
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
            int color = this.active ? ArcaneBeamGuiTheme.TEXT : 0xFF687780;
            drawCenteredString(poseStack, Minecraft.getInstance().font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
        }
    }

    private static class SettingSlider extends AbstractSliderButton {
        private String label;
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

        void setLabel(String label) {
            this.label = label;
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

        @Override
        public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
            NineSliceRenderer.draw(poseStack, NineSliceRenderer.BUTTON, this.x, this.y, this.width, this.height);
            int trackY = this.y + this.height - 5;
            fill(poseStack, this.x + 7, trackY - 2, this.x + this.width - 7, trackY + 2, 0xAA05080D);
            int fillX = this.x + 7 + (int) ((this.width - 14) * this.value);
            fill(poseStack, this.x + 7, trackY - 2, fillX, trackY + 2, ArcaneBeamGuiTheme.GOLD_LINE);
            NineSliceRenderer.draw(poseStack, NineSliceRenderer.BUTTON_ACTIVE, fillX - 4, this.y + 8, 8, this.height - 12);
            Minecraft minecraft = Minecraft.getInstance();
            String text = fitText(minecraft.font, displayText(), this.width - 16);
            int textWidth = minecraft.font.width(text);
            int plateX = this.x + this.width / 2 - textWidth / 2 - 4;
            fill(poseStack, plateX, this.y + 2, plateX + textWidth + 8, this.y + 12, 0xC005080D);
            int color = this.active ? ArcaneBeamGuiTheme.TEXT : 0xFF687780;
            drawCenteredString(poseStack, minecraft.font, new TextComponent(text), this.x + this.width / 2, this.y + 3, color);
        }

        private String displayText() {
            String renderLabel = this.width < 180 ? compactLabel(label) : label;
            if ("Color Shift".equals(label)) {
                return renderLabel + ": " + String.format(Locale.ROOT, this.width < 180 ? "%.2fs" : "%.2fs/color", actualValue() / 20.0D);
            }
            if ("Glow Rotation".equals(label)) {
                return renderLabel + ": " + String.format(Locale.ROOT, "%.1f rpm", actualValue());
            }
            if ("Spot Count".equals(label)) {
                return renderLabel + ": " + Math.round(actualValue());
            }
            return renderLabel + ": " + String.format(Locale.ROOT, "%.2f", actualValue());
        }

        private static String compactLabel(String value) {
            return switch (value) {
                case "Start Radius" -> "Start R";
                case "End Radius" -> "End R";
                case "Edge Width" -> "Edge W";
                case "Interior Opacity" -> "Interior";
                case "Sphere Size" -> "Sphere";
                case "Sphere Opacity" -> "Sphere Op";
                case "Cone Height" -> "Cone H";
                case "Cone Width" -> "Cone W";
                case "Cone Opacity" -> "Cone Op";
                case "Spot Count" -> "Spots";
                case "Spot Size" -> "Spot Sz";
                case "Spot Opacity" -> "Spot Op";
                case "Secondary Size" -> "Ripple Sz";
                case "Corner Radius" -> "Corner R";
                case "Corner Opacity" -> "Corner Op";
                case "Center Bottom" -> "Ctr Bottom";
                case "Center Top" -> "Ctr Top";
                case "Center Height" -> "Ctr H";
                case "Center Fade" -> "Ctr Fade";
                case "Center Opacity" -> "Ctr Op";
                case "Glow Opacity" -> "Glow Op";
                case "Glow Bottom" -> "Glow Bot";
                case "Glow Height" -> "Glow H";
                case "Glow Rotation" -> "Glow Rot";
                case "Circle Alpha" -> "Circle A";
                case "Circle Width" -> "Circle W";
                case "Blaster Alpha" -> "Blaster A";
                case "Blaster Width" -> "Blaster W";
                case "Bolt Length" -> "Bolt Len";
                case "Segment Gap" -> "Seg Gap";
                case "Impact Flash Size" -> "Flash Size";
                case "Missile Alpha" -> "Missile A";
                case "Missile Width" -> "Missile W";
                case "Plume Length" -> "Plume Len";
                default -> value;
            };
        }

        private static String fitText(net.minecraft.client.gui.Font font, String value, int maxWidth) {
            if (font.width(value) <= maxWidth) {
                return value;
            }
            String suffix = "...";
            String clipped = value;
            while (!clipped.isEmpty() && font.width(clipped) + font.width(suffix) > maxWidth) {
                clipped = clipped.substring(0, clipped.length() - 1);
            }
            return clipped + suffix;
        }

        private double actualValue() {
            return min + (max - min) * this.value;
        }

        private static double normalize(double value, double min, double max) {
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
        }
    }
}
