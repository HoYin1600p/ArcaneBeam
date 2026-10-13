package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerVisualConfig;
import dev.hoyin1600p.arcanebeam.beam.BeamKind;
import dev.hoyin1600p.arcanebeam.beam.PreviewScreenState;
import dev.hoyin1600p.arcanebeam.beam.ResourcePackParticleStyle;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.config.model.ArchonSettings;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.BlastWaveSettings;
import dev.hoyin1600p.arcanebeam.config.model.DungeonGlobeMapSettings;
import dev.hoyin1600p.arcanebeam.config.model.FadeInStyle;
import dev.hoyin1600p.arcanebeam.config.model.FadeOutStyle;
import dev.hoyin1600p.arcanebeam.config.model.LightningSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.LightningStrikeSettings;
import dev.hoyin1600p.arcanebeam.config.model.ShaderCompatibility;
import dev.hoyin1600p.arcanebeam.config.model.SoundChoice;
import dev.hoyin1600p.arcanebeam.config.model.StartHand;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowProjectileSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSoundMode;
import dev.hoyin1600p.arcanebeam.config.model.UiSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSoundMode;
import dev.hoyin1600p.arcanebeam.update.UpdateNoticeFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static dev.hoyin1600p.arcanebeam.gui.SettingSlider.GLOBE_REVEALED_LABEL;
import static dev.hoyin1600p.arcanebeam.gui.SettingSlider.GLOBE_TEAMMATE_LABEL;
import static dev.hoyin1600p.arcanebeam.util.IntMath.clamp;

/**
 * The in-game settings screen: a full-height panel docked on the left with a page list and one scrolling page of
 * settings sections, leaving the rest of the screen clear for the live preview. The panel can be minimized to a
 * thin tab, and then offers a small Start Position window for the pages that have an editable origin.
 * <p>
 * Coordinates are in a virtual layout of at least {@value #MIN_LAYOUT_WIDTH} by {@value #MIN_LAYOUT_HEIGHT} that is
 * scaled down uniformly on small windows; every widget lives in that virtual space.
 */
public class ArcaneBeamConfigScreen extends Screen implements PreviewScreenState {
    private static final int MIN_LAYOUT_WIDTH = 960;
    private static final int MIN_LAYOUT_HEIGHT = 720;
    private static final int PANEL_WIDTH = 600;
    private static final int HEADER_HEIGHT = 62;
    private static final int DETAILS_LEFT = ConfigNav.WIDTH + 14;
    private static final int DETAILS_RIGHT_MARGIN = 18;
    private static final int DETAILS_BOTTOM_MARGIN = 12;
    private static final int NAV_TOP = 44;
    private static final int WHEEL_SCROLL_STEP = 32;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int MINIMIZE_TAB_WIDTH = 24;
    private static final int MINIMIZE_TAB_HEIGHT = 56;
    private static final int RESTORE_TAB_WIDTH = 18;
    private static final int RESTORE_TAB_HEIGHT = 100;
    private static final int CLEAN_VIEW_HANDLE_WIDTH = 176;
    private static final int CLEAN_VIEW_HANDLE_HEIGHT = 28;
    private static final int CLEAN_VIEW_HANDLE_MARGIN = 24;
    private static final int PROFILE_BUTTON_WIDTH = 126;
    private static final int SWAP_BUTTON_WIDTH = MINIMIZE_TAB_WIDTH;
    private static final int SWAP_BUTTON_HEIGHT = 34;
    private static final int PROFILE_ROW_HEIGHT = 18;
    private static final int PALETTE_WIDTH = 180;
    private static final int PALETTE_HEIGHT = 110;
    private static final int BRIGHTNESS_WIDTH = 14;
    private static final int SLOT_PREVIEW_WIDTH = SectionLayout.SWATCH_WIDTH;
    private static final int SLOT_HEX_WIDTH = SectionLayout.HEX_WIDTH;
    private static final int BOX_WIDTH = 54;


    private final List<EditBox> colorBoxes = new ArrayList<>();
    private final List<EditBox> glowColorBoxes = new ArrayList<>();
    private final List<EditBox> altarColorBoxes = new ArrayList<>();
    private final List<EditBox> stormArrowColorBoxes = new ArrayList<>();
    private final List<EditBox> manaTrapColorBoxes = new ArrayList<>();
    private final List<EditBox> originBoxes = new ArrayList<>();
    private final List<WheelScrollTarget> wheelScrollTargets = new ArrayList<>();
    private EditBox profileNameBox;
    private boolean updatingManaTrapBoxes;
    private String pendingProfileName = "";
    private int pendingProfileCursor;
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
    private SettingSlider dungeonGlobeRoomOpacitySlider;
    private SettingSlider dungeonGlobeSharedOpacitySlider;
    private SettingSlider manaTrapParticleCountSlider;
    private SettingSlider manaBeamParticleCountSlider;
    private SettingSlider stormArrowCircleAlphaSlider;
    private SettingSlider stormArrowCircleThicknessSlider;
    private SettingSlider stormArrowBlasterAlphaSlider;
    private SettingSlider stormArrowBlasterWidthSlider;
    private SettingSlider stormArrowSegmentLengthSlider;
    private SettingSlider stormArrowImpactFlashSizeSlider;
    private SettingSlider blastWaveLateralSpreadSlider;
    private SettingSlider blastWaveMissileScaleSlider;
    private SettingSlider blastWavePitchVarianceSlider;
    private Button soundButton;
    private Button beamEnabledButton;
    private Button handButton;
    private Button shaderCompatibilityButton;
    private Button crosshairAimButton;
    private Button particleStyleButton;
    private Button particleFacingButton;
    private SettingSlider particleScaleSlider;
    private SettingSlider particleRotationSlider;
    private Button fadeInModeButton;
    private Button fadeOutModeButton;
    private Button profileAddButton;
    private Button profileDropdownButton;
    private Button doneButton;
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
    private Button dungeonGlobeMapEnabledButton;
    private Button updateChecksButton;
    private Button updateTypesButton;
    private Button manaTrapEnabledButton;
    private Button manaTrapGroundSigilButton;
    private Button manaTrapDrainStreamButton;
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
    private EditBox blastWaveOriginXBox;
    private EditBox blastWaveOriginYBox;
    private EditBox blastWaveOriginZBox;
    private EditBox blastWaveMaxMissilesBox;
    private EditBox blastWaveSoundLayersBox;
    private EditBox blastWaveSoundStaggerBox;
    private EditBox stormArrowSoundVolumeBox;
    private EditBox stormArrowAudioRangeBox;
    private EditBox pickerHexBox;

    private final List<EditBox> editBoxes = new ArrayList<>();
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final Map<AbstractWidget, BooleanSupplier> activeRules = new HashMap<>();
    private final SectionLayout sections = new SectionLayout();
    private ConfigNav nav;
    private StartPositionWidget startPosition;
    private Button cleanViewButton;
    private Button adjustOriginButton;
    private boolean profileDropdownOpen;
    private boolean railSelected;
    private boolean lightningSelected;
    private boolean vaultAltarSelected;
    private boolean globeMapSelected;
    private boolean manaTrapSelected;
    private boolean labyrinthSelected;
    private LabyrinthPanel labyrinth;
    private boolean updateNoticesSelected;
    private boolean stormArrowSelected;
    private boolean smiteSelected;
    private boolean archonSelected;
    private boolean blastWaveSelected;
    private boolean draggingPalette;
    private boolean draggingBrightness;
    private boolean draggingScrollbar;
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
    private PanelState panelState = PanelState.OPEN;
    private PanelState panelStateBeforeClean = PanelState.OPEN;
    private ConfigPage activePage = ConfigPage.ARCANE;
    private int detailsScroll;

    private enum PanelState {
        /** The panel is open. */
        OPEN,
        /** Only the thin "Settings" tab (and the Start Position window, where the page has one) is shown. */
        MINIMIZED,
        /** Everything is hidden except the handle that brings the panel back. */
        CLEAN
    }

    /** The screen to return to on close, or null to return to the game (keybind). */
    private final Screen parent;

    public ArcaneBeamConfigScreen() {
        this(null);
    }

    /** Opened from the Mods list Config button: closing returns to {@code parent}. */
    public ArcaneBeamConfigScreen(Screen parent) {
        super(new TextComponent("Arcane Beam"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public BeamKind previewKind() {
        return railSelected ? BeamKind.RAIL : BeamKind.ARCANE;
    }

    private boolean stormLikeSelected() {
        return stormArrowSelected || smiteSelected || archonSelected || blastWaveSelected;
    }

    @Override
    public boolean blastWaveSelected() {
        return blastWaveSelected;
    }

    /** True on every page that is not Arcane or Rail, so the beam preview stays off for them. */
    @Override
    public boolean lightningSelected() {
        return !(activePage == ConfigPage.ARCANE || activePage == ConfigPage.RAIL);
    }

    private <T extends AbstractWidget> T addContent(T widget) {
        this.addWidget(widget);
        contentWidgets.add(widget);
        if (widget instanceof EditBox box) {
            editBoxes.add(box);
        }
        return widget;
    }

    private ThemeButton addHeaderButton(String label, Button.OnPress onPress) {
        return this.addRenderableWidget(new ThemeButton(0, 0, 60, 20, new TextComponent(label), onPress));
    }

    @Override
    protected void init() {
        if (profileNameBox != null) {
            pendingProfileName = profileNameBox.getValue();
            pendingProfileCursor = profileNameBox.getCursorPosition();
        }
        updateLayoutScale();
        suppressGameHud();
        colorBoxes.clear();
        glowColorBoxes.clear();
        altarColorBoxes.clear();
        stormArrowColorBoxes.clear();
        manaTrapColorBoxes.clear();
        originBoxes.clear();
        wheelScrollTargets.clear();
        editBoxes.clear();
        contentWidgets.clear();
        activeRules.clear();
        profileDropdownOpen = false;
        syncBooleansFromPage();

        nav = new ConfigNav(this::addRenderableWidget, this::selectPage);
        pickerHexBox = new EditBox(this.font, 0, 0, 78, 20, new TextComponent("Selected Color"));
        pickerHexBox.setMaxLength(7);
        pickerHexBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        pickerHexBox.setResponder(this::updatePickerHexFromText);
        this.addRenderableWidget(pickerHexBox);
        editBoxes.add(pickerHexBox);

        profileNameBox = new EditBox(this.font, 0, 0, 100, 20, new TextComponent("Profile Name"));
        profileNameBox.setMaxLength(24);
        profileNameBox.setFilter(value -> value == null || !value.contains("\n") && !value.contains("\r") && !value.contains("\t"));
        if (!pendingProfileName.isEmpty()) {
            profileNameBox.setValue(pendingProfileName);
            profileNameBox.moveCursorTo(Math.min(pendingProfileCursor, pendingProfileName.length()));
        }
        this.addRenderableWidget(profileNameBox);
        editBoxes.add(profileNameBox);
        profileAddButton = addHeaderButton("Add", button -> addProfile());
        profileDropdownButton = addHeaderButton("", button -> profileDropdownOpen = !profileDropdownOpen);
        cleanViewButton = addHeaderButton("Clean View", button -> enterCleanPreviewMode());
        doneButton = addHeaderButton("Done", button -> onClose());

        for (int i = 0; i < 4; i++) {
            final int slot = i;
            EditBox editBox = new EditBox(this.font, 0, 0, SLOT_HEX_WIDTH, 20, new TextComponent("Color " + (i + 1)));
            editBox.setMaxLength(7);
            editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
            editBox.setResponder(value -> updateColorFromText(slot, value));
            colorBoxes.add(editBox);
            addContent(editBox);
        }
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            EditBox editBox = new EditBox(this.font, 0, 0, SLOT_HEX_WIDTH, 20, new TextComponent("Glow " + (i + 1)));
            editBox.setMaxLength(7);
            editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
            editBox.setResponder(value -> updateGlowColorFromText(slot, value));
            glowColorBoxes.add(editBox);
            addContent(editBox);
        }

        // Positions are assigned by the section layout; only the widgets' sizes and behaviour are set here.
        int sliderX = 0;
        int sliderY = 0;
        int glowBoxY = 0;
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
        addContent(intensitySlider);
        addContent(opacitySlider);
        addContent(glowRadiusSlider);
        addContent(glowOpacitySlider);
        addContent(colorShiftSlider);
        addContent(glowRotationSlider);
        beamEnabledButton = addContent(new ThemeButton(sliderX, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            settings().enabled = !settings().enabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        shaderCompatibilityButton = addContent(new ThemeButton(sliderX, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleShaderCompatibility();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        crosshairAimButton = addContent(new ThemeButton(sliderX, sliderY + 168, 150, 20, TextComponent.EMPTY, button -> {
            cycleCrosshairAim();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        particleStyleButton = addContent(new ThemeButton(sliderX, sliderY + 192, 150, 20, TextComponent.EMPTY, button -> {
            settings().resourcePackParticles = !settings().resourcePackParticles;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        particleFacingButton = addContent(new ThemeButton(sliderX, sliderY + 216, 150, 20, TextComponent.EMPTY, button -> {
            settings().orientParticles = !settings().orientParticles;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        particleScaleSlider = addContent(new SettingSlider(sliderX, sliderY + 240, 308, 20, "Particle Size",
                ResourcePackParticleStyle.MIN_SCALE, 4.0D, () -> settings().particleScale, value -> settings().particleScale = (float) value));
        particleRotationSlider = addContent(new SettingSlider(sliderX, sliderY + 264, 308, 20, "Particle Rotation",
                0.0D, 360.0D, () -> settings().particleRotationDegrees, value -> settings().particleRotationDegrees = (float) value));
        handButton = addContent(new ThemeButton(sliderX + 158, sliderY + 144, 150, 20, TextComponent.EMPTY, button -> {
            cycleHand();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        int soundRowY = sliderY + 168;
        soundButton = addContent(new ThemeButton(sliderX, soundRowY, 184, 20, TextComponent.EMPTY, button -> {
            cycleSound();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        soundVolumeBox = new EditBox(this.font, sliderX + 248, soundRowY, 60, 20, new TextComponent("Sound Volume"));
        soundVolumeBox.setMaxLength(4);
        soundVolumeBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        soundVolumeBox.setResponder(this::updateSoundVolumeFromText);
        addContent(soundVolumeBox);

        int transitionRowY = soundRowY + 24;
        fadeInModeButton = addContent(new ThemeButton(sliderX, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
            cycleFadeInStyle();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        fadeInTicksBox = new EditBox(this.font, sliderX + 128, transitionRowY, 24, 20, new TextComponent("In Ticks"));
        fadeInTicksBox.setMaxLength(2);
        fadeInTicksBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        fadeInTicksBox.setResponder(this::updateFadeInTicksFromText);
        addContent(fadeInTicksBox);
        fadeOutModeButton = addContent(new ThemeButton(sliderX + 160, transitionRowY, 122, 20, TextComponent.EMPTY, button -> {
            cycleFadeOutStyle();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        fadeOutTicksBox = new EditBox(this.font, sliderX + 284, transitionRowY, 24, 20, new TextComponent("Out Ticks"));
        fadeOutTicksBox.setMaxLength(2);
        fadeOutTicksBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        fadeOutTicksBox.setResponder(this::updateFadeOutTicksFromText);
        addContent(fadeOutTicksBox);

        int originY = transitionRowY + 24;
        addOriginBox(sliderX, originY, "X", 0);
        addOriginBox(sliderX + 104, originY, "Y", 1);
        addOriginBox(sliderX + 208, originY, "Z", 2);
        adjustOriginButton = addContent(new ThemeButton(0, 0, 150, 20, new TextComponent("Adjust on screen"), button -> minimizePanel()));

        addLightningControls(sliderX, sliderY);
        addVaultAltarControls(sliderX, sliderY);
        addVaultControls(sliderX, sliderY);
        labyrinth = new LabyrinthPanel(this.font, widget -> addContent(widget), this::refreshControls);
        labyrinth.create((box, wheel) -> registerWheelScrollTarget(box, wheel::scroll));
        addStormArrowControls(sliderX, sliderY);
        configureWheelScrollTargets();
        configureActiveRules();

        startPosition = new StartPositionWidget(this.font, widget -> this.addRenderableWidget(widget), this::restorePanel,
                ArcaneBeamConfig::requestSave);
        styleEditBoxes();
        refreshControls();
    }

    /** Widgets that are greyed out (rather than hidden) while a related option is off. */
    private void configureActiveRules() {
        activeRules.put(particleRotationSlider, () -> settings().orientParticles);
        activeRules.put(dungeonGlobeRoomOpacitySlider, () -> dungeonGlobeMapSettings().enabled);
        activeRules.put(dungeonGlobeSharedOpacitySlider, () -> dungeonGlobeMapSettings().enabled);
        activeRules.put(updateTypesButton, ArcaneBeamConfig::updateChecksEnabled);
        activeRules.put(manaTrapParticleCountSlider, ManaStealerVisualConfig::configuredEnabled);
        activeRules.put(manaBeamParticleCountSlider, ManaStealerVisualConfig::configuredEnabled);
        for (EditBox box : manaTrapColorBoxes) {
            activeRules.put(box, ManaStealerVisualConfig::configuredEnabled);
        }
    }


    private void addLightningControls(int x, int y) {
        int lightningColorY = 0;
        lightningRingColorBox = addLightningColorBox(0, lightningColorY, "Ring Color", this::updateLightningRingColorFromText);
        lightningSphereColorBox = addLightningColorBox(0, lightningColorY, "Sphere Color", this::updateLightningSphereColorFromText);
        lightningConeColorBox = addLightningColorBox(0, lightningColorY, "Cone Color", this::updateLightningConeColorFromText);
        lightningSpotColorBox = addLightningColorBox(0, lightningColorY, "Spot Color", this::updateLightningSpotColorFromText);

        lightningEnabledButton = addContent(new ThemeButton(x, y, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().enabled = !lightningSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        lightningShaderCompatibilityButton = addContent(new ThemeButton(x + 158, y, 150, 20, TextComponent.EMPTY, button -> {
            ShaderCompatibility current = lightningShaderCompatibility();
            lightningSettings().shaderCompatibility = current == ShaderCompatibility.ON
                    ? ShaderCompatibility.OFF.id
                    : ShaderCompatibility.ON.id;
            lightningSettings().shaderCompatibilityMigrated = true;
            refreshControls();
            ArcaneBeamConfig.requestSave();
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
        addContent(lightningStartRadiusSlider);
        addContent(lightningEndRadiusSlider);
        addContent(lightningThicknessSlider);
        addContent(lightningAlphaSlider);
        addContent(lightningInteriorOpacitySlider);
        addContent(lightningSphereRadiusSlider);
        addContent(lightningSphereOpacitySlider);
        addContent(lightningConeHeightSlider);
        addContent(lightningConeRadiusSlider);
        addContent(lightningConeOpacitySlider);
        addContent(lightningSpotCountSlider);
        addContent(lightningSpotSizeSlider);
        addContent(lightningSpotOpacitySlider);
        addContent(lightningSecondarySizeSlider);

        lightningFullbrightButton = addContent(new ThemeButton(x, y + 240, 150, 20, TextComponent.EMPTY, button -> {
            lightningSettings().fullbright = !lightningSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        lightningSoundButton = addContent(new ThemeButton(x, y + 268, 150, 20, TextComponent.EMPTY, button -> {
            cycleLightningSound();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));

        lightningLifetimeBox = addLightningNumberBox(x, y + 296, 54, 3, "Lifetime", "[0-9]{0,3}", this::updateLightningLifetimeFromText);
        lightningSideCountBox = addLightningNumberBox(x + 104, y + 296, 54, 2, "Sides", "[0-9]{0,2}", this::updateLightningSideCountFromText);
        lightningSecondaryCountBox = addLightningNumberBox(x + 208, y + 296, 54, 1, "Ripples", "[0-9]{0,1}", this::updateLightningSecondaryCountFromText);
        lightningSecondaryDelayBox = addLightningNumberBox(x, y + 324, 54, 2, "Delay", "[0-9]{0,2}", this::updateLightningSecondaryDelayFromText);
        lightningSoundVolumeBox = addLightningSoundVolumeBox(x + 208, y + 324);
    }

    private void addVaultAltarControls(int x, int y) {
        int colorY = 0;
        addAltarColorBox(0, colorY, "Corner 1", 0);
        addAltarColorBox(0, colorY, "Corner 2", 1);
        addAltarColorBox(0, colorY, "Center 1", 2);
        addAltarColorBox(0, colorY, "Center 2", 3);
        addAltarColorBox(0, 0, "Glow 1", 4);
        addAltarColorBox(0, 0, "Glow 2", 5);

        int controlY = 0 + 52;
        altarEnabledButton = addContent(new ThemeButton(x, controlY, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().enabled = !vaultAltarSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        altarShaderCompatibilityButton = addContent(new ThemeButton(x + 158, controlY, 150, 20, TextComponent.EMPTY, button -> {
            ShaderCompatibility current = vaultAltarShaderCompatibility();
            vaultAltarSettings().shaderCompatibility = current == ShaderCompatibility.ON
                    ? ShaderCompatibility.OFF.id
                    : ShaderCompatibility.ON.id;
            refreshControls();
            ArcaneBeamConfig.requestSave();
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
        addContent(altarCornerRadiusSlider);
        addContent(altarCornerOpacitySlider);
        addContent(altarCenterBottomRadiusSlider);
        addContent(altarCenterTopRadiusSlider);
        addContent(altarCenterHeightSlider);
        addContent(altarCenterFadeSlider);
        addContent(altarCenterOpacitySlider);
        addContent(altarGlowOpacitySlider);
        addContent(altarGlowBottomRadiusSlider);
        addContent(altarGlowTopRadiusSlider);
        addContent(altarGlowHeightSlider);
        addContent(altarGlowFadeSlider);
        addContent(altarGlowRotationSlider);

        altarFullbrightButton = addContent(new ThemeButton(x, controlY + 196, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().fullbright = !vaultAltarSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        altarSoundButton = addContent(new ThemeButton(x + 158, controlY + 196, 150, 20, TextComponent.EMPTY, button -> {
            cycleVaultAltarSound();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        altarOriginMarkersButton = addContent(new ThemeButton(x, controlY + 280, 150, 20, TextComponent.EMPTY, button -> {
            vaultAltarSettings().originMarkersEnabled = !vaultAltarSettings().originMarkersEnabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        altarVerticalTicksBox = addAltarNumberBox(x, controlY + 224, 42, 2, "Vertical", "[0-9]{0,2}", this::updateAltarVerticalTicksFromText);
        altarConvergeTicksBox = addAltarNumberBox(x + 108, controlY + 224, 42, 3, "Converge", "[0-9]{0,3}", this::updateAltarConvergeTicksFromText);
        altarCenterGrowTicksBox = addAltarNumberBox(x + 222, controlY + 224, 42, 3, "Grow", "[0-9]{0,3}", this::updateAltarCenterGrowTicksFromText);
        altarOriginHeightBox = addAltarDecimalBox(x, controlY + 252, this::updateAltarOriginHeightFromText);
        altarOriginRadiusBox = addAltarDecimalBox(x + 158, controlY + 252, this::updateAltarOriginRadiusFromText);
        altarSoundVolumeBox = addAltarSoundVolumeBox(x + 208, controlY + 280);
    }

    private void addVaultControls(int x, int y) {
        dungeonGlobeMapEnabledButton = addContent(new ThemeButton(x, y, 220, 20, TextComponent.EMPTY, button -> {
            dungeonGlobeMapSettings().enabled = !dungeonGlobeMapSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        dungeonGlobeRoomOpacitySlider = new SettingSlider(x, y + 28, 220, 20, GLOBE_REVEALED_LABEL, 0.0D, 100.0D,
                () -> dungeonGlobeMapSettings().opacityPercent,
                value -> dungeonGlobeMapSettings().opacityPercent = (int) Math.round(value));
        addContent(dungeonGlobeRoomOpacitySlider);
        dungeonGlobeSharedOpacitySlider = new SettingSlider(x, y + 56, 220, 20, GLOBE_TEAMMATE_LABEL, 0.0D, 100.0D,
                () -> dungeonGlobeMapSettings().sharedOpacityPercent,
                value -> dungeonGlobeMapSettings().sharedOpacityPercent = (int) Math.round(value));
        addContent(dungeonGlobeSharedOpacitySlider);

        updateChecksButton = addContent(new ThemeButton(x, y, 220, 20, TextComponent.EMPTY, button -> {
            ArcaneBeamConfig.setUpdateChecksEnabled(!ArcaneBeamConfig.updateChecksEnabled());
            refreshControls();
        }));
        updateTypesButton = addContent(new ThemeButton(x, y + 28, 220, 20, TextComponent.EMPTY, button -> {
            ArcaneBeamConfig.setUpdateNoticeFilter(ArcaneBeamConfig.updateNoticeFilter() == UpdateNoticeFilter.CRITICAL
                    ? UpdateNoticeFilter.ALL
                    : UpdateNoticeFilter.CRITICAL);
            refreshControls();
        }));

        manaTrapEnabledButton = addContent(new ThemeButton(x, y, 220, 20, TextComponent.EMPTY, button -> {
            ManaStealerVisualConfig.setEnabled(!ManaStealerVisualConfig.configuredEnabled());
            refreshControls();
        }));
        manaTrapGroundSigilButton = addContent(new ThemeButton(x, y + 28, 220, 20, TextComponent.EMPTY, button -> {
            ManaStealerVisualConfig.setReplaceGroundSigil(!ManaStealerVisualConfig.replaceGroundSigil());
            refreshControls();
        }));
        manaTrapDrainStreamButton = addContent(new ThemeButton(x, y + 56, 220, 20, TextComponent.EMPTY, button -> {
            ManaStealerVisualConfig.setDrainStreamEnabled(!ManaStealerVisualConfig.drainStreamEnabled());
            refreshControls();
        }));
        manaTrapParticleCountSlider = new SettingSlider(x, y + 84, 220, 20, "Trap Particle Count", 0.0D, 512.0D,
                ManaStealerVisualConfig::trapParticleCount,
                value -> ManaStealerVisualConfig.setTrapParticleCount((int) Math.round(value)));
        manaBeamParticleCountSlider = new SettingSlider(x, y + 112, 220, 20, "Beam Particle Count", 1.0D, 256.0D,
                ManaStealerVisualConfig::beamParticleCount,
                value -> ManaStealerVisualConfig.setBeamParticleCount((int) Math.round(value)));
        addContent(manaTrapParticleCountSlider);
        addContent(manaBeamParticleCountSlider);
        addManaTrapColorBox(x, y, "Trap Halo", 0);
        addManaTrapColorBox(x, y, "Trap Core", 1);
        addManaTrapColorBox(x, y, "Beam Halo", 2);
        addManaTrapColorBox(x, y, "Beam Core", 3);
    }

    private void addStormArrowControls(int x, int y) {
        int colorY = 0;
        addStormArrowColorBox(0, colorY, "Circle", 0);
        addStormArrowColorBox(0, colorY, "Blaster", 1);
        addStormArrowColorBox(0, colorY, "Core", 2);
        addStormArrowColorBox(0, colorY, "Flash", 3);
        addStormArrowColorBox(0, 0, "Pulse Circle", 4);

        stormArrowEnabledButton = addContent(new ThemeButton(x, y, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().enabled = !stormArrowSettings().enabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowShaderCompatibilityButton = addContent(new ThemeButton(x + 158, y, 150, 20, TextComponent.EMPTY, button -> {
            ShaderCompatibility current = stormArrowShaderCompatibility();
            stormArrowSettings().shaderCompatibility = current == ShaderCompatibility.ON
                    ? ShaderCompatibility.OFF.id
                    : ShaderCompatibility.ON.id;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowTargetingCircleButton = addContent(new ThemeButton(x, y + 24, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().showTargetingCircle = !stormArrowSettings().showTargetingCircle;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowActualRadiusButton = addContent(new ThemeButton(x + 158, y + 24, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().useActualRadius = !stormArrowSettings().useActualRadius;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowCircleAlphaSlider = new SettingSlider(x, y + 48, 150, 20, "Circle Alpha", 0.0D, 1.0D, () -> stormArrowSettings().circleAlpha, value -> stormArrowSettings().circleAlpha = (float) value);
        stormArrowCircleThicknessSlider = new SettingSlider(x + 158, y + 48, 150, 20, "Circle Width", 0.02D, 1.0D, () -> stormArrowSettings().circleThickness, value -> stormArrowSettings().circleThickness = (float) value);
        stormArrowBlasterAlphaSlider = new SettingSlider(x, y + 72, 150, 20, "Blaster Alpha", 0.0D, 1.0D, () -> stormArrowSettings().blasterAlpha, value -> stormArrowSettings().blasterAlpha = (float) value);
        stormArrowBlasterWidthSlider = new SettingSlider(x + 158, y + 72, 150, 20, "Blaster Width", 0.01D, 1.0D, () -> stormArrowSettings().blasterWidth, value -> stormArrowSettings().blasterWidth = (float) value);
        stormArrowSegmentLengthSlider = new SettingSlider(x, y + 96, 150, 20, "Bolt Length", 0.1D, 12.0D, () -> stormArrowSettings().segmentLength, value -> stormArrowSettings().segmentLength = (float) value);
        stormArrowImpactFlashSizeSlider = new SettingSlider(x, y + 120, 308, 20, "Impact Flash Size", 0.05D, 4.0D, () -> stormArrowSettings().impactFlashSize, value -> stormArrowSettings().impactFlashSize = (float) value);
        addContent(stormArrowCircleAlphaSlider);
        addContent(stormArrowCircleThicknessSlider);
        addContent(stormArrowBlasterAlphaSlider);
        addContent(stormArrowBlasterWidthSlider);
        addContent(stormArrowSegmentLengthSlider);
        addContent(stormArrowImpactFlashSizeSlider);

        stormArrowFullbrightButton = addContent(new ThemeButton(x, y + 148, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().fullbright = !stormArrowSettings().fullbright;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowImpactFlashButton = addContent(new ThemeButton(x + 158, y + 148, 150, 20, TextComponent.EMPTY, button -> {
            stormArrowSettings().impactFlashEnabled = !stormArrowSettings().impactFlashEnabled;
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowSoundButton = addContent(new ThemeButton(x, y + 172, 150, 20, TextComponent.EMPTY, button -> {
            cycleStormArrowSound();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowProjectileSoundButton = addContent(new ThemeButton(x + 158, y + 172, 150, 20, TextComponent.EMPTY, button -> {
            cycleStormArrowProjectileSound();
            refreshControls();
            ArcaneBeamConfig.requestSave();
        }));
        stormArrowLifetimeBox = addStormArrowNumberBox(x, y + 200, 54, 2, "Lifetime", "[0-9]{0,2}", this::updateStormArrowLifetimeFromText);
        stormArrowOriginHeightBox = addStormArrowNumberBox(x + 158, y + 200, 54, 4, "Height", "[0-9]{0,2}(\\.[0-9]?)?", this::updateStormArrowOriginHeightFromText);
        archonMissileRadiusBox = addStormArrowNumberBox(x + 158, y + 228, 54, 4, "Radius", "[0-9]{0,2}(\\.[0-9]?)?", this::updateArchonMissileRadiusFromText);
        stormArrowAudioRangeBox = addStormArrowAudioRangeBox(x, y + 228);
        stormArrowSoundVolumeBox = addStormArrowSoundVolumeBox(x + 208, y + 256);
        blastWaveOriginXBox = addBlastWaveDecimalBox(x, y + 284, "Origin X", this::updateBlastWaveOriginXFromText);
        blastWaveOriginYBox = addBlastWaveDecimalBox(x, y + 308, "Origin Y", this::updateBlastWaveOriginYFromText);
        blastWaveOriginZBox = addBlastWaveDecimalBox(x, y + 332, "Origin Z", this::updateBlastWaveOriginZFromText);
        blastWaveMaxMissilesBox = addStormArrowNumberBox(x, y + 356, 42, 2, "Missiles", "[0-9]{0,2}", this::updateBlastWaveMaxMissilesFromText);
        blastWaveSoundLayersBox = addStormArrowNumberBox(x, y + 380, 42, 2, "Layers", "[0-9]{0,2}", this::updateBlastWaveSoundLayersFromText);
        blastWaveSoundStaggerBox = addStormArrowNumberBox(x, y + 404, 42, 1, "Stagger", "[0-9]?", this::updateBlastWaveSoundStaggerFromText);
        blastWaveLateralSpreadSlider = new SettingSlider(x, y + 428, 180, 20, "Side Veer", 0.0D, 4.0D, () -> blastWaveSettings().lateralSpread, value -> blastWaveSettings().lateralSpread = (float) value);
        blastWaveMissileScaleSlider = new SettingSlider(x, y + 452, 180, 20, "Missile Scale", 0.25D, 3.0D, () -> blastWaveSettings().missileScale, value -> blastWaveSettings().missileScale = (float) value);
        blastWavePitchVarianceSlider = new SettingSlider(x, y + 476, 180, 20, "Pitch Variation", 0.0D, 0.20D, () -> blastWaveSettings().pitchVariance, value -> blastWaveSettings().pitchVariance = (float) value);
        addContent(blastWaveLateralSpreadSlider);
        addContent(blastWaveMissileScaleSlider);
        addContent(blastWavePitchVarianceSlider);
    }



    private void syncBooleansFromPage() {
        railSelected = activePage == ConfigPage.RAIL;
        lightningSelected = activePage == ConfigPage.LIGHTNING_STRIKE;
        vaultAltarSelected = activePage == ConfigPage.VAULT_ALTAR;
        globeMapSelected = activePage == ConfigPage.GLOBE_MAP;
        manaTrapSelected = activePage == ConfigPage.MANA_TRAP;
        labyrinthSelected = activePage == ConfigPage.LABYRINTH;
        updateNoticesSelected = activePage == ConfigPage.UPDATE_NOTICES;
        stormArrowSelected = activePage == ConfigPage.STORM_ARROW;
        smiteSelected = activePage == ConfigPage.SMITE;
        archonSelected = activePage == ConfigPage.ARCHON;
        blastWaveSelected = activePage == ConfigPage.BLAST_WAVE;
    }

    private void selectPage(ConfigPage page) {
        activePage = page;
        syncBooleansFromPage();
        detailsScroll = 0;
        selectedSlot = 0;
        glowColorsSelected = false;
        profileDropdownOpen = false;
        closeColorPicker();
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

    // ---- geometry -------------------------------------------------------------------------------------------

    private boolean panelOnRight() {
        return ArcaneBeamConfig.INSTANCE.ui != null && ArcaneBeamConfig.INSTANCE.ui.panelOnRight();
    }

    /** Left edge of the panel: 0 when docked left, flush with the right screen edge otherwise. */
    private int panelX() {
        return panelOnRight() ? layoutWidth - PANEL_WIDTH : 0;
    }

    private int detailsX() {
        return panelX() + DETAILS_LEFT;
    }

    /** X of the minimize tab and the swap button: they hang off the panel's inner edge. */
    private int innerTabX() {
        return panelOnRight() ? panelX() - MINIMIZE_TAB_WIDTH : PANEL_WIDTH;
    }

    private int swapButtonY() {
        return minimizeTabY() - SWAP_BUTTON_HEIGHT - 4;
    }

    private int restoreTabX() {
        return panelOnRight() ? layoutWidth - RESTORE_TAB_WIDTH : 0;
    }

    /** The direction the panel collapses to: toward its screen edge. */
    private ArrowIcon.Direction collapseDirection() {
        return panelOnRight() ? ArrowIcon.Direction.RIGHT : ArrowIcon.Direction.LEFT;
    }

    private int detailsWidth() {
        return PANEL_WIDTH - DETAILS_LEFT - DETAILS_RIGHT_MARGIN;
    }

    private int viewTop() {
        return HEADER_HEIGHT + 6;
    }

    private int viewBottom() {
        return layoutHeight - DETAILS_BOTTOM_MARGIN;
    }

    private int viewHeight() {
        return viewBottom() - viewTop();
    }

    private int maxDetailsScroll() {
        return Math.max(0, sections.contentHeight() - viewHeight());
    }

    private int scrollbarX() {
        return panelX() + PANEL_WIDTH - 12;
    }

    private int scrollbarThumbHeight() {
        int content = Math.max(1, sections.contentHeight());
        return clamp(viewHeight() * viewHeight() / content, 24, viewHeight());
    }

    private int minimizeTabY() {
        return layoutHeight / 2 - MINIMIZE_TAB_HEIGHT / 2;
    }

    private int restoreTabY() {
        return layoutHeight / 2 - RESTORE_TAB_HEIGHT / 2;
    }

    private int cleanViewHandleX() {
        return layoutWidth / 2 - CLEAN_VIEW_HANDLE_WIDTH / 2;
    }

    private int cleanViewHandleY() {
        return layoutHeight - CLEAN_VIEW_HANDLE_MARGIN - CLEAN_VIEW_HANDLE_HEIGHT;
    }

    private int pickerWidth() {
        return PALETTE_WIDTH + BRIGHTNESS_WIDTH + 34;
    }

    private int pickerHeight() {
        return PALETTE_HEIGHT + 72;
    }

    /** The colour picker opens in the preview area just right of the panel, near the top. */
    private int pickerX() {
        // The picker opens on the panel's inner side, in the preview area.
        int preferred = panelOnRight() ? panelX() - MINIMIZE_TAB_WIDTH - 12 - pickerWidth() : PANEL_WIDTH + MINIMIZE_TAB_WIDTH + 12;
        return clamp(preferred, 0, Math.max(0, layoutWidth - pickerWidth() - 8));
    }

    private int pickerY() {
        return 20;
    }

    private void updatePalettePosition() {
        paletteX = pickerX() + 12;
        paletteY = pickerY() + 24;
    }

    private void layoutPickerWidgets() {
        moveWidget(pickerHexBox, pickerX() + 74, pickerY() + pickerHeight() - 30, 78);
    }

    private boolean isInsidePicker(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, pickerX(), pickerY(), pickerWidth(), pickerHeight());
    }

    private static void moveWidget(AbstractWidget widget, int x, int y, int width) {
        if (widget != null) {
            widget.x = x;
            widget.y = y;
            widget.setWidth(width);
        }
    }

    // ---- page layout ----------------------------------------------------------------------------------------

    /** Re-assigns every widget's position for the current page, section grid and scroll offset. */
    private void layoutWidgets() {
        buildPage();
        sections.layout(this.font, detailsX(), viewTop(), detailsWidth(), detailsScroll);
        int clamped = clamp(detailsScroll, 0, maxDetailsScroll());
        if (clamped != detailsScroll) {
            detailsScroll = clamped;
            sections.layout(this.font, detailsX(), viewTop(), detailsWidth(), detailsScroll);
        }
        updatePalettePosition();
        layoutPickerWidgets();
        // "Adjust on screen" points away from the edge the panel is docked to.
        ((ThemeButton) adjustOriginButton).setArrow(panelOnRight() ? ArrowIcon.Direction.LEFT : ArrowIcon.Direction.RIGHT);
    }

    private void scrollDetailsTo(int offset) {
        int clamped = clamp(offset, 0, maxDetailsScroll());
        if (clamped != detailsScroll) {
            detailsScroll = clamped;
            sections.layout(this.font, detailsX(), viewTop(), detailsWidth(), detailsScroll);
            updateWidgetVisibility();
        }
    }

    private void buildPage() {
        sections.clear();
        switch (activePage) {
            case ARCANE, RAIL -> buildBeamPage();
            case LIGHTNING_STRIKE -> buildLightningPage();
            case VAULT_ALTAR -> buildVaultAltarPage();
            case STORM_ARROW -> buildStormArrowPage();
            case SMITE -> buildSmitePage();
            case ARCHON -> buildArchonPage();
            case BLAST_WAVE -> buildBlastWavePage();
            case GLOBE_MAP -> buildGlobeMapPage();
            case MANA_TRAP -> buildManaTrapPage();
            case LABYRINTH -> labyrinth.layout(sections, BOX_WIDTH);
            case UPDATE_NOTICES -> buildUpdateNoticesPage();
        }
    }

    private SectionLayout.ColorSlot colorSlot(String label, EditBox box, int slot, boolean glow, java.util.function.IntSupplier color) {
        return new SectionLayout.ColorSlot(label, box, slot, glow, color);
    }

    private List<SectionLayout.ColorSlot> beamColorSlots() {
        List<SectionLayout.ColorSlot> slots = new ArrayList<>();
        for (int i = 0; i < colorBoxes.size(); i++) {
            final int slot = i;
            slots.add(colorSlot("Core " + (i + 1), colorBoxes.get(i), i, false, () -> settings().colors[slot]));
        }
        for (int i = 0; i < glowColorBoxes.size(); i++) {
            final int slot = i;
            slots.add(colorSlot("Glow " + (i + 1), glowColorBoxes.get(i), i, true, () -> settings().glowColors[slot]));
        }
        return slots;
    }

    private List<SectionLayout.ColorSlot> stormColorSlots() {
        List<SectionLayout.ColorSlot> slots = new ArrayList<>();
        int count = blastWaveSelected ? 5 : 4;
        for (int i = 0; i < count && i < stormArrowColorBoxes.size(); i++) {
            final int slot = i;
            slots.add(colorSlot(stormArrowColorLabel(i), stormArrowColorBoxes.get(i), i, false, () -> stormArrowColor(slot)));
        }
        return slots;
    }

    private void buildBeamPage() {
        SectionLayout.Section setup = sections.add("Setup", false).widget(beamEnabledButton);
        if (!railSelected) {
            // Resource pack visuals are an Arcane-only option; Rail always uses the mod beam.
            setup.widget(particleStyleButton);
        }
        setup.widget(shaderCompatibilityButton).widget(crosshairAimButton);
        if (resourcePackParticlesShown()) {
            sections.add("Particles", false).widget(particleScaleSlider).widget(particleFacingButton).widget(particleRotationSlider);
        } else {
            sections.add("Shape", false).widget(intensitySlider).widget(opacitySlider).widget(glowRadiusSlider).widget(glowOpacitySlider);
        }
        sections.add("Colors", true).colors(beamColorSlots());
        sections.add("Motion", false).widget(colorShiftSlider).widget(glowRotationSlider);
        sections.add("Origin", false).widget(handButton)
                .labeled("X", originBoxes.get(0), BOX_WIDTH)
                .labeled("Y", originBoxes.get(1), BOX_WIDTH)
                .labeled("Z", originBoxes.get(2), BOX_WIDTH)
                .widget(adjustOriginButton);
        sections.add("Transitions (ticks)", false)
                .buttonAndBox(fadeInModeButton, fadeInTicksBox, 36, "")
                .buttonAndBox(fadeOutModeButton, fadeOutTicksBox, 36, "");
        sections.add("Sound", false).widget(soundButton).labeled("Volume", soundVolumeBox, BOX_WIDTH);
    }

    private void buildLightningPage() {
        sections.add("Setup", false).widget(lightningEnabledButton).widget(lightningShaderCompatibilityButton)
                .widget(lightningFullbrightButton).labeled("Lifetime", lightningLifetimeBox, BOX_WIDTH);
        sections.add("Colors", true).colors(List.of(
                colorSlot("Ring", lightningRingColorBox, 0, false, () -> lightningSettings().ringColor),
                colorSlot("Sphere", lightningSphereColorBox, 1, false, () -> lightningSettings().sphereColor),
                colorSlot("Cone", lightningConeColorBox, 2, false, () -> lightningSettings().coneColor),
                colorSlot("Spots", lightningSpotColorBox, 3, false, () -> lightningSettings().spotColor)));
        sections.add("Shockwave", false).widget(lightningStartRadiusSlider).widget(lightningEndRadiusSlider)
                .widget(lightningThicknessSlider).labeled("Sides", lightningSideCountBox, BOX_WIDTH);
        sections.add("Opacity", false).widget(lightningAlphaSlider).widget(lightningInteriorOpacitySlider);
        sections.add("Center Geometry", false).widget(lightningSphereRadiusSlider).widget(lightningSphereOpacitySlider)
                .widget(lightningConeHeightSlider).widget(lightningConeRadiusSlider).widget(lightningConeOpacitySlider);
        sections.add("Spots", false).widget(lightningSpotCountSlider).widget(lightningSpotSizeSlider).widget(lightningSpotOpacitySlider);
        sections.add("Ripples", false).widget(lightningSecondarySizeSlider)
                .labeled("Ripples", lightningSecondaryCountBox, BOX_WIDTH).labeled("Delay", lightningSecondaryDelayBox, BOX_WIDTH);
        sections.add("Sound", false).widget(lightningSoundButton).labeled("Volume", lightningSoundVolumeBox, BOX_WIDTH);
    }

    private void buildVaultAltarPage() {
        sections.add("Setup", false).widget(altarEnabledButton).widget(altarShaderCompatibilityButton).widget(altarFullbrightButton);
        List<SectionLayout.ColorSlot> slots = new ArrayList<>();
        for (int i = 0; i < altarColorBoxes.size(); i++) {
            final int slot = i;
            slots.add(colorSlot(vaultAltarColorLabel(i), altarColorBoxes.get(i), i, false, () -> vaultAltarColor(slot)));
        }
        sections.add("Colors", true).colors(slots);
        sections.add("Corner Beam", false).widget(altarCornerRadiusSlider).widget(altarCornerOpacitySlider);
        sections.add("Center Beam", false).widget(altarCenterBottomRadiusSlider).widget(altarCenterTopRadiusSlider)
                .widget(altarCenterHeightSlider).widget(altarCenterFadeSlider).widget(altarCenterOpacitySlider);
        sections.add("Glow", false).widget(altarGlowOpacitySlider).widget(altarGlowBottomRadiusSlider).widget(altarGlowTopRadiusSlider)
                .widget(altarGlowHeightSlider).widget(altarGlowFadeSlider).widget(altarGlowRotationSlider);
        sections.add("Origin", false).labeled("Height", altarOriginHeightBox, BOX_WIDTH).labeled("Radius", altarOriginRadiusBox, BOX_WIDTH)
                .widget(altarOriginMarkersButton);
        sections.add("Timing", false).labeled("Vertical", altarVerticalTicksBox, BOX_WIDTH)
                .labeled("Converge", altarConvergeTicksBox, BOX_WIDTH).labeled("Grow", altarCenterGrowTicksBox, BOX_WIDTH);
        sections.add("Sound", false).widget(altarSoundButton).labeled("Volume", altarSoundVolumeBox, BOX_WIDTH);
    }

    private SectionLayout.Section stormSetupSection() {
        return sections.add("Setup", false).widget(stormArrowEnabledButton).widget(stormArrowShaderCompatibilityButton).widget(stormArrowFullbrightButton);
    }

    private void stormTargetingSection() {
        sections.add("Targeting", false).widget(stormArrowTargetingCircleButton).widget(stormArrowActualRadiusButton)
                .widget(stormArrowCircleAlphaSlider).widget(stormArrowCircleThicknessSlider);
    }

    private void buildStormArrowPage() {
        stormSetupSection();
        sections.add("Colors", true).colors(stormColorSlots());
        stormTargetingSection();
        sections.add("Projectile", false).widget(stormArrowProjectileSoundButton).labeled("Audio Range", stormArrowAudioRangeBox, BOX_WIDTH);
        sections.add("Strike", false).widget(stormArrowBlasterAlphaSlider).widget(stormArrowBlasterWidthSlider).widget(stormArrowSegmentLengthSlider);
        sections.add("Impact", false).widget(stormArrowImpactFlashSizeSlider).widget(stormArrowImpactFlashButton)
                .labeled("Lifetime", stormArrowLifetimeBox, BOX_WIDTH).labeled("Height", stormArrowOriginHeightBox, BOX_WIDTH);
        sections.add("Sound", false).widget(stormArrowSoundButton).labeled("Volume", stormArrowSoundVolumeBox, BOX_WIDTH);
    }

    private void buildSmitePage() {
        stormSetupSection();
        sections.add("Colors", true).colors(stormColorSlots());
        stormTargetingSection();
        sections.add("Strike", false).widget(stormArrowBlasterAlphaSlider).widget(stormArrowBlasterWidthSlider)
                .widget(stormArrowSegmentLengthSlider).widget(stormArrowSoundButton);
        sections.add("Activation", false).widget(stormArrowProjectileSoundButton);
        sections.add("Impact", false).widget(stormArrowImpactFlashSizeSlider).widget(stormArrowImpactFlashButton)
                .labeled("Lifetime", stormArrowLifetimeBox, BOX_WIDTH).labeled("Height", stormArrowOriginHeightBox, BOX_WIDTH);
        sections.add("Audio", false).labeled("Audio Range", stormArrowAudioRangeBox, BOX_WIDTH).labeled("Volume", stormArrowSoundVolumeBox, BOX_WIDTH);
    }

    private void buildArchonPage() {
        stormSetupSection();
        sections.add("Colors", true).colors(stormColorSlots());
        stormTargetingSection();
        sections.add("Missile", false).labeled("Radius", archonMissileRadiusBox, BOX_WIDTH).labeled("Height", stormArrowOriginHeightBox, BOX_WIDTH)
                .widget(stormArrowSegmentLengthSlider);
        sections.add("Strike + Impact", false).widget(stormArrowBlasterAlphaSlider).widget(stormArrowBlasterWidthSlider)
                .widget(stormArrowImpactFlashSizeSlider).widget(stormArrowImpactFlashButton).labeled("Lifetime", stormArrowLifetimeBox, BOX_WIDTH);
        sections.add("Activation", false).widget(stormArrowProjectileSoundButton);
        sections.add("Sound", false).widget(stormArrowSoundButton).labeled("Volume", stormArrowSoundVolumeBox, BOX_WIDTH)
                .labeled("Audio Range", stormArrowAudioRangeBox, BOX_WIDTH);
    }

    private void buildBlastWavePage() {
        stormSetupSection();
        sections.add("Colors", true).colors(stormColorSlots());
        stormTargetingSection();
        sections.add("Missile Origin", false)
                .labeled("Origin X", blastWaveOriginXBox, BOX_WIDTH)
                .labeled("Origin Y", blastWaveOriginYBox, BOX_WIDTH)
                .labeled("Origin Z", blastWaveOriginZBox, BOX_WIDTH)
                .widget(adjustOriginButton);
        sections.add("Flight", false).labeled("Apex", stormArrowOriginHeightBox, BOX_WIDTH).labeled("Lifetime", stormArrowLifetimeBox, BOX_WIDTH)
                .labeled("Missiles", blastWaveMaxMissilesBox, BOX_WIDTH).widget(blastWaveLateralSpreadSlider);
        sections.add("Missile + Impact", false).widget(stormArrowBlasterAlphaSlider).widget(stormArrowBlasterWidthSlider)
                .widget(stormArrowSegmentLengthSlider).widget(blastWaveMissileScaleSlider).widget(stormArrowImpactFlashSizeSlider)
                .widget(stormArrowImpactFlashButton);
        sections.add("Activation", false).widget(stormArrowProjectileSoundButton);
        sections.add("Sound", false).widget(stormArrowSoundButton).labeled("Volume", stormArrowSoundVolumeBox, BOX_WIDTH)
                .labeled("Audio Range", stormArrowAudioRangeBox, BOX_WIDTH);
        sections.add("Sound Layering", false).labeled("Layers", blastWaveSoundLayersBox, BOX_WIDTH)
                .labeled("Stagger", blastWaveSoundStaggerBox, BOX_WIDTH).widget(blastWavePitchVarianceSlider);
    }

    private void buildGlobeMapPage() {
        sections.add("Dungeon Globe Map", true).widget(dungeonGlobeMapEnabledButton).widget(dungeonGlobeRoomOpacitySlider)
                .widget(dungeonGlobeSharedOpacitySlider)
                .hint("Percent of Vault's map opacity; Off uses Vault's normal.")
                .hint("Only globe, objective and teammate reveals fade, never inscriptions or rooms you enter.");
    }

    private void buildManaTrapPage() {
        sections.add("Setup", false).widget(manaTrapEnabledButton).widget(manaTrapGroundSigilButton).widget(manaTrapDrainStreamButton);
        sections.add("Particles", false).widget(manaTrapParticleCountSlider).widget(manaBeamParticleCountSlider)
                .hint("Trap count scales with Minecraft's particle quality setting.");
        List<SectionLayout.ColorSlot> slots = new ArrayList<>();
        for (int i = 0; i < manaTrapColorBoxes.size(); i++) {
            final int slot = i;
            slots.add(colorSlot(manaTrapColorLabel(i), manaTrapColorBoxes.get(i), i, false, () -> manaTrapColor(slot)));
        }
        sections.add("Colors", true).colors(slots);
    }

    private void buildUpdateNoticesPage() {
        sections.add("Update Notices", true).widget(updateChecksButton).widget(updateTypesButton)
                .hint("Critical only shows releases marked critical.")
                .hint("Changes apply immediately; nothing is downloaded.");
    }

    // ---- visibility -----------------------------------------------------------------------------------------

    private void layoutHeader(boolean open) {
        int right = panelX() + PANEL_WIDTH - DETAILS_RIGHT_MARGIN;
        moveWidget(doneButton, right - 46, 8, 46);
        moveWidget(cleanViewButton, right - 46 - 6 - 70, 8, 70);
        moveWidget(profileDropdownButton, right - 46 - 6 - 70 - 6 - PROFILE_BUTTON_WIDTH, 8, PROFILE_BUTTON_WIDTH);
        moveWidget(profileAddButton, right - 44, 34, 44);
        moveWidget(profileNameBox, right - 44 - 12 - 106, 34, 106);
        boolean profiles = open && activePage.hasProfiles;
        setVisible(doneButton, open);
        setVisible(cleanViewButton, open);
        setVisible(profileDropdownButton, profiles);
        setVisible(profileAddButton, profiles);
        setVisible(profileNameBox, profiles);
    }

    private void updateWidgetVisibility() {
        boolean open = panelState == PanelState.OPEN;
        if (!open) {
            profileDropdownOpen = false;
        }
        nav.layout(panelX(), NAV_TOP, layoutHeight - 12, activePage, open);
        layoutHeader(open);
        setVisible(pickerHexBox, open && colorPickerOpen);
        List<AbstractWidget> placed = open ? sections.widgets() : List.of();
        for (AbstractWidget widget : contentWidgets) {
            if (!placed.contains(widget)) {
                setVisible(widget, false);
            }
        }
        for (AbstractWidget widget : placed) {
            applyViewport(widget);
        }
        updateStartPosition();
    }

    /**
     * Shows a widget when any part of it is inside the scrolling viewport and lets it take input only when it is
     * fully inside, so a half-clipped widget can never be clicked through the header or below the panel.
     */
    private void applyViewport(AbstractWidget widget) {
        int margin = widget instanceof EditBox ? 4 : 0;
        int top = widget.y - margin;
        int bottom = widget.y + widget.getHeight() + margin;
        boolean fully = top >= viewTop() && bottom <= viewBottom();
        boolean overlaps = bottom > viewTop() && top < viewBottom();
        BooleanSupplier rule = activeRules.get(widget);
        boolean ruleOk = rule == null || rule.getAsBoolean();
        widget.visible = overlaps;
        widget.active = fully && ruleOk;
        if (widget instanceof EditBox box) {
            if (rule != null) {
                box.setEditable(ruleOk);
            }
            if (!fully || !ruleOk) {
                box.setFocus(false);
            }
        }
    }

    private static void setVisible(AbstractWidget widget, boolean visible) {
        if (widget != null) {
            widget.visible = visible;
            widget.active = visible;
            if (!visible && widget instanceof EditBox box) {
                box.setFocus(false);
            }
        }
    }

    private void updateStartPosition() {
        boolean show = panelState == PanelState.MINIMIZED && activePage.hasOriginWidget();
        startPosition.setPanelOnRight(panelOnRight());
        if (show) {
            if (startPosition.isOpen()) {
                startPosition.layout(layoutWidth, layoutHeight);
            } else {
                startPosition.open(originTarget(), layoutWidth, layoutHeight);
            }
        } else if (startPosition.isOpen()) {
            startPosition.close();
        }
    }

    private void minimizePanel() {
        panelState = PanelState.MINIMIZED;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    /** Moves the panel to the other screen edge and remembers the choice in the config. */
    private void swapPanelSide() {
        if (ArcaneBeamConfig.INSTANCE.ui == null) {
            ArcaneBeamConfig.INSTANCE.ui = new UiSettings();
        }
        ArcaneBeamConfig.INSTANCE.ui.panelSide = panelOnRight() ? UiSettings.LEFT : UiSettings.RIGHT;
        ArcaneBeamConfig.requestSave();
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private void restorePanel() {
        panelState = PanelState.OPEN;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private void enterCleanPreviewMode() {
        if (panelState != PanelState.CLEAN) {
            panelStateBeforeClean = panelState;
        }
        panelState = PanelState.CLEAN;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private void exitCleanPreviewMode() {
        panelState = panelStateBeforeClean == PanelState.CLEAN ? PanelState.OPEN : panelStateBeforeClean;
        profileDropdownOpen = false;
        closeColorPicker();
        refreshControls();
    }

    private StartPositionWidget.Target originTarget() {
        if (blastWaveSelected) {
            return new StartPositionWidget.Target() {
                @Override
                public String pageName() {
                    return ConfigPage.BLAST_WAVE.label;
                }

                @Override
                public boolean hasHand() {
                    return false;
                }

                @Override
                public StartHand hand() {
                    return null;
                }

                @Override
                public void applyHand(StartHand hand) {
                }

                @Override
                public double get(int axis) {
                    BlastWaveSettings s = blastWaveSettings();
                    return axis == 0 ? s.originOffsetX : axis == 1 ? s.originOffsetY : s.originOffsetZ;
                }

                @Override
                public void set(int axis, double value) {
                    float clamped = clampHundredths((float) roundHundredths(value), -4.0F, 4.0F);
                    BlastWaveSettings s = blastWaveSettings();
                    if (axis == 0) {
                        s.originOffsetX = clamped;
                    } else if (axis == 1) {
                        s.originOffsetY = clamped;
                    } else {
                        s.originOffsetZ = clamped;
                    }
                }

                @Override
                public void setFromText(int axis, String text) {
                    updateBlastWaveOriginFromText(text, axis);
                }

                @Override
                public double limit() {
                    return 4.0D;
                }

                @Override
                public void reset() {
                    BlastWaveSettings defaults = ArcaneBeamConfig.bundledBlastWaveDefaults();
                    BlastWaveSettings s = blastWaveSettings();
                    s.originOffsetX = defaults.originOffsetX;
                    s.originOffsetY = defaults.originOffsetY;
                    s.originOffsetZ = defaults.originOffsetZ;
                }
            };
        }
        final boolean rail = railSelected;
        final String page = activePage.label;
        return new StartPositionWidget.Target() {
            @Override
            public String pageName() {
                return page;
            }

            @Override
            public boolean hasHand() {
                return true;
            }

            @Override
            public StartHand hand() {
                return startHand();
            }

            @Override
            public void applyHand(StartHand hand) {
                settings().startHand = hand.id;
            }

            @Override
            public double get(int axis) {
                BeamSettings s = settings();
                return axis == 0 ? s.startOffsetX : axis == 1 ? s.startOffsetY : s.startOffsetZ;
            }

            @Override
            public void set(int axis, double value) {
                double clamped = roundOffset(Math.max(-limit(), Math.min(limit(), value)));
                BeamSettings s = settings();
                if (axis == 0) {
                    s.startOffsetX = clamped;
                } else if (axis == 1) {
                    s.startOffsetY = clamped;
                } else {
                    s.startOffsetZ = clamped;
                }
            }

            @Override
            public void setFromText(int axis, String text) {
                updateOriginFromText(axis, text);
            }

            @Override
            public double limit() {
                return 64.0D;
            }

            @Override
            public void reset() {
                BeamSettings defaults = ArcaneBeamConfig.bundledBeamDefaults(rail);
                BeamSettings s = settings();
                s.startOffsetX = defaults.startOffsetX;
                s.startOffsetY = defaults.startOffsetY;
                s.startOffsetZ = defaults.startOffsetZ;
            }
        };
    }

    private void styleEditBoxes() {
        for (EditBox box : editBoxes) {
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


    private void addOriginBox(int x, int y, String label, int axis) {
        EditBox editBox = new EditBox(this.font, x + 25, y, 70, 20, new TextComponent(label + " Offset"));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,2}(\\.[0-9]{0,2})?"));
        editBox.setResponder(value -> updateOriginFromText(axis, value));
        originBoxes.add(editBox);
        addContent(editBox);
    }

    private EditBox addLightningNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 50, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private EditBox addLightningColorBox(int x, int y, String label, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, 0, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private void addAltarColorBox(int x, int y, String label, int slot) {
        EditBox editBox = new EditBox(this.font, 0, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(value -> updateAltarColorFromText(slot, value));
        altarColorBoxes.add(editBox);
        addContent(editBox);
    }

    private void addStormArrowColorBox(int x, int y, String label, int slot) {
        EditBox editBox = new EditBox(this.font, 0, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(value -> updateStormArrowColorFromText(slot, value));
        stormArrowColorBoxes.add(editBox);
        addContent(editBox);
    }

    private void addManaTrapColorBox(int x, int y, String label, int slot) {
        EditBox editBox = new EditBox(this.font, 0, y, SLOT_HEX_WIDTH, 20, new TextComponent(label));
        editBox.setMaxLength(7);
        editBox.setFilter(value -> value.isEmpty() || value.matches("#?[0-9a-fA-F]{0,6}"));
        editBox.setResponder(value -> updateManaTrapColorFromText(slot, value));
        manaTrapColorBoxes.add(editBox);
        addContent(editBox);
    }

    private EditBox addLightningSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateLightningSoundVolumeFromText);
        addContent(editBox);
        return editBox;
    }

    private EditBox addAltarNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private EditBox addAltarSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateAltarSoundVolumeFromText);
        addContent(editBox);
        return editBox;
    }

    private EditBox addAltarDecimalBox(int x, int y, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, 50, 20, new TextComponent("Altar Decimal"));
        editBox.setMaxLength(5);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}(\\.[0-9]{0,2})?"));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private EditBox addStormArrowNumberBox(int x, int y, int width, int maxLength, String label, String pattern, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, width, 20, new TextComponent(label));
        editBox.setMaxLength(maxLength);
        editBox.setFilter(value -> value.isEmpty() || value.matches(pattern));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private EditBox addBlastWaveDecimalBox(int x, int y, String label, Consumer<String> responder) {
        EditBox editBox = new EditBox(this.font, x + 58, y, 54, 20, new TextComponent(label));
        editBox.setMaxLength(6);
        editBox.setFilter(value -> value.isEmpty() || value.matches("-?[0-9]{0,1}(\\.[0-9]{0,2})?"));
        editBox.setResponder(responder);
        addContent(editBox);
        return editBox;
    }

    private EditBox addStormArrowSoundVolumeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 50, y, 50, 20, new TextComponent("Sound Volume"));
        editBox.setMaxLength(4);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,1}(\\.[0-9]{0,2})?") || value.matches("2(\\.[0]{0,2})?"));
        editBox.setResponder(this::updateStormArrowSoundVolumeFromText);
        addContent(editBox);
        return editBox;
    }

    private EditBox addStormArrowAudioRangeBox(int x, int y) {
        EditBox editBox = new EditBox(this.font, x + 74, y, 34, 20, new TextComponent("Audio Range"));
        editBox.setMaxLength(2);
        editBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,2}"));
        editBox.setResponder(this::updateStormArrowAudioRangeFromText);
        addContent(editBox);
        return editBox;
    }



    // ---- rendering ------------------------------------------------------------------------------------------

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        int layoutMouseX = toLayoutX(mouseX);
        int layoutMouseY = toLayoutY(mouseY);

        // The HUD pass is cancelled while this screen is open, which skips its closing colour reset.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.pushPose();
        poseStack.scale(layoutScale, layoutScale, 1.0F);
        switch (panelState) {
            case OPEN -> {
                renderPanel(poseStack);
                renderDetails(poseStack, layoutMouseX, layoutMouseY, partialTick);
                renderMinimizeTab(poseStack);
                renderInputFrame(poseStack, profileNameBox);
                if (colorPickerOpen) {
                    renderPalette(poseStack);
                    renderBrightnessStrip(poseStack);
                }
            }
            case MINIMIZED -> {
                renderRestoreTab(poseStack);
                startPosition.render(poseStack);
            }
            case CLEAN -> renderCleanViewHandle(poseStack);
        }
        super.render(poseStack, layoutMouseX, layoutMouseY, partialTick);
        if (panelState == PanelState.OPEN) {
            renderProfileDropdown(poseStack);
            if (isInside(layoutMouseX, layoutMouseY, innerTabX(), swapButtonY(), SWAP_BUTTON_WIDTH, SWAP_BUTTON_HEIGHT)) {
                renderTooltip(poseStack, new TextComponent("Move panel to the other side"), layoutMouseX, layoutMouseY);
            }
        }
        poseStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private void renderPanel(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(panelX(), 0.0D, 0.0D);
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, 0, 0, PANEL_WIDTH, layoutHeight);
        fill(poseStack, 9, 9, PANEL_WIDTH - 9, layoutHeight - 9, ArcaneBeamGuiTheme.CARD_SHADE);
        fill(poseStack, 9, 9, ConfigNav.WIDTH, layoutHeight - 9, 0x66000000);
        fill(poseStack, ConfigNav.WIDTH, 9, ConfigNav.WIDTH + 1, layoutHeight - 9, ArcaneBeamGuiTheme.GOLD_LINE);
        nav.render(poseStack, this.font);

        int accent = activePage.accent;
        drawString(poseStack, this.font, activePage.label, DETAILS_LEFT, 12, ArcaneBeamGuiTheme.TEXT);
        drawString(poseStack, this.font, activePage.breadcrumb(), DETAILS_LEFT, 24, accent);
        String help = colorPickerOpen ? "Color picker is open beside the panel."
                : activePage.hasProfiles ? "Click a color swatch to open the picker." : "Client-side Vault controls. Changes apply immediately.";
        drawString(poseStack, this.font, help, DETAILS_LEFT, 40, ArcaneBeamGuiTheme.MUTED_TEXT);
        fill(poseStack, DETAILS_LEFT, HEADER_HEIGHT - 3, PANEL_WIDTH - DETAILS_RIGHT_MARGIN, HEADER_HEIGHT - 2, ArcaneBeamGuiTheme.GOLD_LINE);
        poseStack.popPose();
    }

    /** Sections, their labels and every page widget, clipped to the scrolling viewport. */
    private void renderDetails(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        beginClip(detailsX() - 6, viewTop(), detailsWidth() + 12, viewHeight());
        sections.render(poseStack, this.font, activePage.accent, selectedSlot, glowColorsSelected);
        for (AbstractWidget widget : sections.widgets()) {
            if (!widget.visible) {
                continue;
            }
            if (widget instanceof EditBox box) {
                renderInputFrame(poseStack, box);
            }
            widget.render(poseStack, mouseX, mouseY, partialTick);
        }
        endClip();
        renderScrollbar(poseStack);
    }

    /** Clips drawing to a rectangle given in virtual layout coordinates. */
    private void beginClip(int x, int y, int width, int height) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale() * layoutScale;
        int left = (int) Math.floor(x * scale);
        int right = (int) Math.ceil((x + width) * scale);
        int bottom = (int) Math.ceil((y + height) * scale);
        int top = (int) Math.floor(y * scale);
        RenderSystem.enableScissor(left, window.getHeight() - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    private void endClip() {
        RenderSystem.disableScissor();
    }

    private void renderScrollbar(PoseStack poseStack) {
        int max = maxDetailsScroll();
        if (max <= 0) {
            return;
        }
        int x = scrollbarX();
        int thumb = scrollbarThumbHeight();
        int thumbY = viewTop() + (viewHeight() - thumb) * detailsScroll / max;
        fill(poseStack, x, viewTop(), x + SCROLLBAR_WIDTH, viewBottom(), 0x5505080D);
        fill(poseStack, x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, (activePage.accent & 0x00FFFFFF) | (draggingScrollbar ? 0xFF000000 : 0xAA000000));
    }

    private void renderMinimizeTab(PoseStack poseStack) {
        int x = innerTabX();
        int y = minimizeTabY();
        drawEdgeTab(poseStack, x, y, MINIMIZE_TAB_WIDTH, MINIMIZE_TAB_HEIGHT, panelOnRight());
        ArrowIcon.draw(poseStack, x + MINIMIZE_TAB_WIDTH / 2, y + MINIMIZE_TAB_HEIGHT / 2, 5, collapseDirection(), ArcaneBeamGuiTheme.GOLD);
        int swapY = swapButtonY();
        drawEdgeTab(poseStack, x, swapY, SWAP_BUTTON_WIDTH, SWAP_BUTTON_HEIGHT, panelOnRight());
        ArrowIcon.drawSwap(poseStack, x + SWAP_BUTTON_WIDTH / 2, swapY + SWAP_BUTTON_HEIGHT / 2, ArcaneBeamGuiTheme.GOLD);
    }

    /** A small gold-framed tab; its frame extends a few pixels under the panel edge it hangs from. */
    private void drawEdgeTab(PoseStack poseStack, int x, int y, int width, int height, boolean panelOnRightSide) {
        int overlap = 4;
        int drawX = panelOnRightSide ? x : x - overlap;
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, drawX, y, width + overlap, height);
        fill(poseStack, x + 2, y + 5, x + width - 2, y + height - 5, 0xE0080C12);
    }

    private void renderRestoreTab(PoseStack poseStack) {
        int x = restoreTabX();
        int y = restoreTabY();
        boolean right = panelOnRight();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GOLD_DRAWER, right ? x : x - 4, y, RESTORE_TAB_WIDTH + 4, RESTORE_TAB_HEIGHT);
        fill(poseStack, x + 2, y + 6, x + RESTORE_TAB_WIDTH - 2, y + RESTORE_TAB_HEIGHT - 6, 0xE0080C12);
        poseStack.pushPose();
        poseStack.translate(x + RESTORE_TAB_WIDTH / 2.0D + 4.0D, y + 12.0D, 0.0D);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
        this.font.draw(poseStack, "Settings", 0.0F, 0.0F, ArcaneBeamGuiTheme.GOLD);
        poseStack.popPose();
        // The arrow points toward the screen, away from the edge the tab sits on.
        ArrowIcon.draw(poseStack, x + RESTORE_TAB_WIDTH / 2, y + RESTORE_TAB_HEIGHT - 16, 4,
                right ? ArrowIcon.Direction.LEFT : ArrowIcon.Direction.RIGHT, ArcaneBeamGuiTheme.GOLD);
    }

    private void renderCleanViewHandle(PoseStack poseStack) {
        int x = cleanViewHandleX();
        int y = cleanViewHandleY();
        NineSliceRenderer.draw(poseStack, NineSliceRenderer.GLASS_PANEL, x, y, CLEAN_VIEW_HANDLE_WIDTH, CLEAN_VIEW_HANDLE_HEIGHT);
        fill(poseStack, x + 8, y + 8, x + CLEAN_VIEW_HANDLE_WIDTH - 8, y + CLEAN_VIEW_HANDLE_HEIGHT - 8, ArcaneBeamGuiTheme.CARD_SHADE);
        fill(poseStack, x + 12, y + CLEAN_VIEW_HANDLE_HEIGHT - 6, x + CLEAN_VIEW_HANDLE_WIDTH - 12, y + CLEAN_VIEW_HANDLE_HEIGHT - 5, (activePage.accent & 0x00FFFFFF) | 0xAA000000);
        drawCenteredString(poseStack, this.font, "Restore Menu (H)", x + CLEAN_VIEW_HANDLE_WIDTH / 2, y + 10, ArcaneBeamGuiTheme.TEXT);
    }

    private void renderInputFrame(PoseStack poseStack, EditBox box) {
        if (box != null && box.visible) {
            NineSliceRenderer.draw(poseStack, NineSliceRenderer.INSET, box.x - 4, box.y - 4, box.getInnerWidth() + 16, box.getHeight() + 8);
        }
    }

    // ---- mouse and keyboard ---------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = toLayoutX(mouseX);
        double ly = toLayoutY(mouseY);
        if (panelState == PanelState.CLEAN) {
            if (button == 0 && isInside(lx, ly, cleanViewHandleX(), cleanViewHandleY(), CLEAN_VIEW_HANDLE_WIDTH, CLEAN_VIEW_HANDLE_HEIGHT)) {
                exitCleanPreviewMode();
                return true;
            }
            return super.mouseClicked(lx, ly, button);
        }
        if (panelState == PanelState.MINIMIZED) {
            if (button == 0 && isInside(lx, ly, restoreTabX(), restoreTabY(), RESTORE_TAB_WIDTH, RESTORE_TAB_HEIGHT)) {
                restorePanel();
                return true;
            }
            if (startPosition.mouseClicked(lx, ly, button)) {
                return true;
            }
            return super.mouseClicked(lx, ly, button) || startPosition.contains(lx, ly);
        }
        if (button == 0 && isInside(lx, ly, innerTabX(), minimizeTabY(), MINIMIZE_TAB_WIDTH, MINIMIZE_TAB_HEIGHT)) {
            minimizePanel();
            return true;
        }
        if (button == 0 && isInside(lx, ly, innerTabX(), swapButtonY(), SWAP_BUTTON_WIDTH, SWAP_BUTTON_HEIGHT)) {
            swapPanelSide();
            return true;
        }
        if (button == 0 && handleProfileDropdownSelection(lx, ly)) {
            return true;
        }
        if (button == 0 && maxDetailsScroll() > 0 && isInside(lx, ly, scrollbarX() - 4, viewTop(), SCROLLBAR_WIDTH + 8, viewHeight())) {
            draggingScrollbar = true;
            scrollToMouse(ly);
            return true;
        }
        if (button == 0) {
            SectionLayout.ColorSlot swatch = sections.colorSlotAt(lx, ly, false);
            if (swatch != null) {
                selectColorSlot(swatch);
                openColorPicker();
                return true;
            }
            SectionLayout.ColorSlot hexBox = sections.colorSlotAt(lx, ly, true);
            if (hexBox != null) {
                selectColorSlot(hexBox);
                openColorPicker();
            }
        }
        if (button == 0 && colorPickerOpen && lx >= paletteX && lx < paletteX + PALETTE_WIDTH && ly >= paletteY && ly < paletteY + PALETTE_HEIGHT) {
            draggingPalette = true;
            updatePaletteSelection(lx, ly);
            return true;
        }
        int brightnessX = paletteX + PALETTE_WIDTH + 10;
        if (button == 0 && colorPickerOpen && lx >= brightnessX && lx < brightnessX + BRIGHTNESS_WIDTH && ly >= paletteY && ly < paletteY + PALETTE_HEIGHT) {
            draggingBrightness = true;
            brightnessDragBaseColor = selectedColor();
            updateBrightnessSelection(ly);
            return true;
        }
        if (button == 0 && colorPickerOpen && !isInsidePicker(lx, ly)) {
            closeColorPicker();
        }
        return super.mouseClicked(lx, ly, button);
    }

    private void selectColorSlot(SectionLayout.ColorSlot slot) {
        selectedSlot = slot.slot();
        glowColorsSelected = slot.glow();
    }

    private void scrollToMouse(double layoutY) {
        int max = maxDetailsScroll();
        int travel = Math.max(1, viewHeight() - scrollbarThumbHeight());
        double ratio = (layoutY - viewTop() - scrollbarThumbHeight() / 2.0D) / travel;
        scrollDetailsTo((int) Math.round(Math.max(0.0D, Math.min(1.0D, ratio)) * max));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double lx = toLayoutX(mouseX);
        double ly = toLayoutY(mouseY);
        if (button == 0 && draggingScrollbar) {
            scrollToMouse(ly);
            return true;
        }
        if (button == 0 && startPosition.mouseDragged(lx, ly, layoutWidth, layoutHeight)) {
            return true;
        }
        if (panelState == PanelState.OPEN && colorPickerOpen) {
            if (button == 0 && draggingPalette) {
                updatePaletteSelection(lx, ly);
                return true;
            }
            if (button == 0 && draggingBrightness) {
                updateBrightnessSelection(ly);
                return true;
            }
        }
        return super.mouseDragged(lx, ly, button, dragX / layoutScale, dragY / layoutScale);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        if (button == 0 && startPosition.mouseReleased()) {
            return true;
        }
        if (button == 0 && (draggingPalette || draggingBrightness)) {
            draggingPalette = false;
            draggingBrightness = false;
            ArcaneBeamConfig.requestSave();
            return true;
        }
        return super.mouseReleased(toLayoutX(mouseX), toLayoutY(mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double lx = toLayoutX(mouseX);
        double ly = toLayoutY(mouseY);
        if (panelState == PanelState.MINIMIZED) {
            if (startPosition.mouseScrolled(lx, ly, delta, hasShiftDown())) {
                ArcaneBeamConfig.requestSave();
                return true;
            }
            return super.mouseScrolled(lx, ly, delta);
        }
        if (panelState != PanelState.OPEN) {
            return super.mouseScrolled(lx, ly, delta);
        }
        // A number field under the pointer always takes the wheel, so it can never scroll the page.
        if (dispatchWheelScrollTarget(lx, ly, delta)) {
            return true;
        }
        if (nav.mouseScrolled(lx - panelX(), ly, delta)) {
            updateWidgetVisibility();
            return true;
        }
        if (isInside(lx, ly, panelX() + ConfigNav.WIDTH, viewTop(), PANEL_WIDTH - ConfigNav.WIDTH, viewHeight()) && maxDetailsScroll() > 0) {
            scrollDetailsTo(detailsScroll + (delta > 0.0D ? -WHEEL_SCROLL_STEP : WHEEL_SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(lx, ly, delta);
    }

    private void registerWheelScrollTarget(EditBox box, WheelScrollHandler handler) {
        if (box != null) {
            wheelScrollTargets.add(new WheelScrollTarget(box, handler));
        }
    }

    private boolean dispatchWheelScrollTarget(double mouseX, double mouseY, double delta) {
        boolean increase = delta > 0.0D;
        boolean coarse = hasShiftDown();
        for (WheelScrollTarget target : wheelScrollTargets) {
            EditBox box = target.box();
            if (!box.visible || !box.active || !isInsideInputFrame(box, mouseX, mouseY)) {
                continue;
            }
            target.handler().scroll(increase, coarse);
            ArcaneBeamConfig.requestSave();
            return true;
        }
        return false;
    }

    private static boolean isInsideInputFrame(EditBox box, double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, box.x - 4, box.y - 4, box.getWidth() + 8, box.getHeight() + 8);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && colorPickerOpen) {
            closeColorPicker();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_H && !textInputFocused()) {
            if (panelState == PanelState.CLEAN) {
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
        ArcaneBeamConfig.flushPendingSave();
        ManaStealerVisualConfig.flushPendingSave();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static boolean shouldSuppressGameHud() {
        return Minecraft.getInstance().screen instanceof ArcaneBeamConfigScreen screen && screen.gameHudSuppressed;
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
        if (nav != null) {
            updateWidgetVisibility();
        }
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
        for (EditBox box : editBoxes) {
            if (box.isFocused()) {
                return true;
            }
        }
        for (EditBox box : startPosition.boxes()) {
            if (box.isFocused()) {
                return true;
            }
        }
        return false;
    }

    private int toLayoutX(double mouseX) {
        return (int) Math.floor(mouseX / layoutScale);
    }

    private int toLayoutY(double mouseY) {
        return (int) Math.floor(mouseY / layoutScale);
    }

    @Override
    public void tick() {
        for (EditBox box : editBoxes) {
            box.tick();
        }
        if (startPosition != null) {
            startPosition.tick();
        }
    }

    // ---- profile dropdown -----------------------------------------------------------------------------------

    private int dropdownX() {
        return profileDropdownButton.x;
    }

    private int dropdownY() {
        return profileDropdownButton.y + profileDropdownButton.getHeight() + 2;
    }

    private int dropdownWidth() {
        return profileDropdownButton.getWidth();
    }

    private void renderProfileDropdown(PoseStack poseStack) {
        if (!profileDropdownOpen) {
            return;
        }
        List<String> profiles = profileNames();
        int x = dropdownX();
        int y = dropdownY();
        int width = dropdownWidth();
        int height = profiles.size() * PROFILE_ROW_HEIGHT;
        fill(poseStack, x - 1, y - 1, x + width + 1, y + height + 1, 0xFFFFFFFF);
        fill(poseStack, x, y, x + width, y + height, 0xEE101018);

        String selected = selectedProfileName();
        for (int i = 0; i < profiles.size(); i++) {
            int rowY = y + i * PROFILE_ROW_HEIGHT;
            String profile = profiles.get(i);
            int rowColor = profile.equals(selected) ? 0xFF304060 : 0xFF181820;
            fill(poseStack, x, rowY, x + width, rowY + PROFILE_ROW_HEIGHT, rowColor);
            drawString(poseStack, this.font, ellipsize(profile, width - 8), x + 4, rowY + 5, 0xFFFFFF);
        }
    }

    private boolean handleProfileDropdownSelection(double mouseX, double mouseY) {
        if (!profileDropdownOpen) {
            return false;
        }

        if (isInside(mouseX, mouseY, dropdownX(), profileDropdownButton.y, dropdownWidth(), profileDropdownButton.getHeight())) {
            profileDropdownOpen = false;
            return true;
        }

        int x = dropdownX();
        int y = dropdownY();
        List<String> profiles = profileNames();
        int height = profiles.size() * PROFILE_ROW_HEIGHT;
        if (!isInside(mouseX, mouseY, x, y, dropdownWidth(), height)) {
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



    // ---- mouse wheel on number fields -----------------------------------------------------------------------

    @FunctionalInterface
    private interface WheelScrollHandler {
        void scroll(boolean increase, boolean coarse);
    }

    private record WheelScrollTarget(EditBox box, WheelScrollHandler handler) {
    }

    /**
     * Wheel steps: decimal fields change by 0.01 per notch (0.1 with Shift); tenths fields by 0.1 (1.0 with Shift);
     * integer fields by 1 (a larger step with Shift only where the field has a wide range).
     */
    private void configureWheelScrollTargets() {
        registerWheelScrollTarget(soundVolumeBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeSoundVolume(increase ? step : -step);
            refreshSoundVolumeBox();
        });
        registerWheelScrollTarget(lightningSoundVolumeBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeLightningSoundVolume(increase ? step : -step);
            refreshLightningSoundVolumeBox();
        });
        registerWheelScrollTarget(altarSoundVolumeBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeAltarSoundVolume(increase ? step : -step);
            refreshAltarSoundVolumeBox();
        });
        registerWheelScrollTarget(stormArrowSoundVolumeBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeStormArrowSoundVolume(increase ? step : -step);
            refreshStormArrowSoundVolumeBox();
        });
        registerWheelScrollTarget(stormArrowAudioRangeBox, (increase, coarse) -> {
            nudgeStormArrowAudioRange(increase ? 1 : -1);
            refreshStormArrowAudioRangeBox();
        });
        registerWheelScrollTarget(stormArrowLifetimeBox, (increase, coarse) -> {
            int step = coarse ? 10 : 1;
            nudgeStormArrowLifetime(increase ? step : -step);
            refreshStormArrowControls();
        });
        registerWheelScrollTarget(stormArrowOriginHeightBox, (increase, coarse) -> {
            double step = coarse ? 1.0D : 0.1D;
            nudgeStormArrowOriginHeight(increase ? step : -step);
            refreshStormArrowControls();
        });
        registerWheelScrollTarget(archonMissileRadiusBox, (increase, coarse) -> {
            double step = coarse ? 1.0D : 0.1D;
            nudgeArchonMissileRadius(increase ? step : -step);
            refreshStormArrowControls();
        });
        registerWheelScrollTarget(blastWaveOriginXBox, (increase, coarse) -> nudgeBlastWaveOrigin(0, increase, coarse));
        registerWheelScrollTarget(blastWaveOriginYBox, (increase, coarse) -> nudgeBlastWaveOrigin(1, increase, coarse));
        registerWheelScrollTarget(blastWaveOriginZBox, (increase, coarse) -> nudgeBlastWaveOrigin(2, increase, coarse));
        registerWheelScrollTarget(blastWaveMaxMissilesBox, (increase, coarse) -> {
            blastWaveSettings().maxMissiles = clamp(blastWaveSettings().maxMissiles + (increase ? 1 : -1), 1, 64);
            refreshBlastWaveBoxes();
        });
        registerWheelScrollTarget(blastWaveSoundLayersBox, (increase, coarse) -> {
            blastWaveSettings().maxSoundLayers = clamp(blastWaveSettings().maxSoundLayers + (increase ? 1 : -1), 1, 12);
            refreshBlastWaveBoxes();
        });
        registerWheelScrollTarget(blastWaveSoundStaggerBox, (increase, coarse) -> {
            blastWaveSettings().soundStaggerTicks = clamp(blastWaveSettings().soundStaggerTicks + (increase ? 1 : -1), 0, 4);
            refreshBlastWaveBoxes();
        });
        registerWheelScrollTarget(altarOriginHeightBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeAltarOriginHeight(increase ? step : -step);
            refreshAltarOriginBoxes();
        });
        registerWheelScrollTarget(altarOriginRadiusBox, (increase, coarse) -> {
            double step = coarse ? 0.10D : 0.01D;
            nudgeAltarOriginRadius(increase ? step : -step);
            refreshAltarOriginBoxes();
        });
        registerWheelScrollTarget(fadeInTicksBox, (increase, coarse) -> {
            nudgeFadeInTicks(increase ? 1 : -1);
            refreshFadeTickBoxes();
        });
        registerWheelScrollTarget(fadeOutTicksBox, (increase, coarse) -> {
            nudgeFadeOutTicks(increase ? 1 : -1);
            refreshFadeTickBoxes();
        });
        for (int axis = 0; axis < originBoxes.size(); axis++) {
            final int originAxis = axis;
            registerWheelScrollTarget(originBoxes.get(axis), (increase, coarse) -> {
                double step = coarse ? 0.10D : 0.01D;
                nudgeOrigin(originAxis, increase ? step : -step);
                refreshOriginBoxes();
            });
        }
        // Integer fields that had no wheel support before the redesign.
        registerWheelScrollTarget(lightningLifetimeBox, (increase, coarse) -> {
            LightningStrikeSettings s = lightningSettings();
            s.lifetimeTicks = clamp(s.lifetimeTicks + (increase ? 1 : -1) * (coarse ? 10 : 1), 1, 200);
            lightningLifetimeBox.setValue(Integer.toString(s.lifetimeTicks));
        });
        registerWheelScrollTarget(lightningSideCountBox, (increase, coarse) -> {
            LightningStrikeSettings s = lightningSettings();
            s.ringSideCount = clamp(s.ringSideCount + (increase ? 1 : -1) * (coarse ? 8 : 1), 8, 96);
            lightningSideCountBox.setValue(Integer.toString(s.ringSideCount));
        });
        registerWheelScrollTarget(lightningSecondaryCountBox, (increase, coarse) -> {
            LightningStrikeSettings s = lightningSettings();
            s.secondaryRippleCount = clamp(s.secondaryRippleCount + (increase ? 1 : -1), 0, 4);
            lightningSecondaryCountBox.setValue(Integer.toString(s.secondaryRippleCount));
        });
        registerWheelScrollTarget(lightningSecondaryDelayBox, (increase, coarse) -> {
            LightningStrikeSettings s = lightningSettings();
            s.secondaryRippleDelayTicks = clamp(s.secondaryRippleDelayTicks + (increase ? 1 : -1) * (coarse ? 5 : 1), 0, 40);
            lightningSecondaryDelayBox.setValue(Integer.toString(s.secondaryRippleDelayTicks));
        });
        registerWheelScrollTarget(altarVerticalTicksBox, (increase, coarse) -> {
            VaultAltarSettings s = vaultAltarSettings();
            s.cornerVerticalTicks = clamp(s.cornerVerticalTicks + (increase ? 1 : -1) * (coarse ? 5 : 1), 0, 60);
            altarVerticalTicksBox.setValue(Integer.toString(s.cornerVerticalTicks));
        });
        registerWheelScrollTarget(altarConvergeTicksBox, (increase, coarse) -> {
            VaultAltarSettings s = vaultAltarSettings();
            s.cornerConvergeTicks = clamp(s.cornerConvergeTicks + (increase ? 1 : -1) * (coarse ? 10 : 1), 1, 120);
            altarConvergeTicksBox.setValue(Integer.toString(s.cornerConvergeTicks));
        });
        registerWheelScrollTarget(altarCenterGrowTicksBox, (increase, coarse) -> {
            VaultAltarSettings s = vaultAltarSettings();
            s.centerGrowTicks = clamp(s.centerGrowTicks + (increase ? 1 : -1) * (coarse ? 10 : 1), 1, 160);
            altarCenterGrowTicksBox.setValue(Integer.toString(s.centerGrowTicks));
        });
    }

    // ---- refreshing -----------------------------------------------------------------------------------------

    /** Re-reads every value into its widget, then re-lays the page out and updates what is visible. */
    private void refreshControls() {
        refreshBoxes();
        if (intensitySlider != null) {
            intensitySlider.refresh();
            opacitySlider.refresh();
            glowRadiusSlider.refresh();
            glowOpacitySlider.refresh();
            colorShiftSlider.refresh();
            glowRotationSlider.refresh();
            beamEnabledButton.setMessage(new TextComponent("Replacement: " + (settings().enabled ? "On" : "Off")));
            shaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + shaderCompatibility().label));
            crosshairAimButton.setMessage(new TextComponent("Crosshair Aim: " + (settings().crosshairAim ? "On" : "Off")));
            particleStyleButton.setMessage(new TextComponent("Visuals: " + (settings().resourcePackParticles ? "Resource Pack" : "Mod Beam")));
            particleFacingButton.setMessage(new TextComponent("Face Beam: " + (settings().orientParticles ? "On" : "Off")));
            particleScaleSlider.refresh();
            particleRotationSlider.refresh();
            soundButton.setMessage(new TextComponent("Sound: " + soundChoice().label));
            handButton.setMessage(new TextComponent("Start: " + startHand().label));
            fadeInModeButton.setMessage(new TextComponent("Fade In: " + shortStyleName(fadeInStyle().label)));
            fadeOutModeButton.setMessage(new TextComponent("Fade Out: " + shortStyleName(fadeOutStyle().label)));
            profileDropdownButton.setMessage(new TextComponent("Profile: " + ellipsize(selectedProfileName(), PROFILE_BUTTON_WIDTH - 52)));
            refreshOriginBoxes();
            refreshSoundVolumeBox();
            refreshFadeTickBoxes();
            refreshLightningControls();
            refreshVaultAltarControls();
            refreshVaultControls();
            labyrinth.refresh();
            refreshStormArrowControls();
        }
        layoutWidgets();
        updateWidgetVisibility();
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
        SectionLayout.drawSwatch(poseStack, pickerX + 44, hexY, selectedColor(), true);
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


    private String manaTrapColorLabel(int slot) {
        return switch (slot) {
            case 0 -> "Trap Halo";
            case 1 -> "Trap Core";
            case 2 -> "Beam Halo";
            default -> "Beam Core";
        };
    }

    private String stormArrowColorLabel(int slot) {
        if (archonSelected || blastWaveSelected) {
            return switch (slot) {
                case 0 -> "Circle";
                case 1 -> "Plume";
                case 2 -> "Core";
                case 3 -> "Flash";
                case 4 -> blastWaveSelected ? "Pulse Circle" : "";
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


    private void addProfile() {
        if (blastWaveSelected) {
            ArcaneBeamConfig.addBlastWaveProfile(profileNameBox == null ? "" : profileNameBox.getValue());
        } else if (archonSelected) {
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


    private int colorAt(int x, int y) {
        float hue = x / (float) Math.max(1, PALETTE_WIDTH - 1);
        float brightness = 1.0F - (y / (float) Math.max(1, PALETTE_HEIGHT - 1));
        return Color.HSBtoRGB(hue, 0.95F, Math.max(0.05F, brightness)) & 0xFFFFFF;
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

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }


    private void refreshBoxes() {
        if (globeMapSelected || updateNoticesSelected) {
            return;
        }
        if (labyrinthSelected) {
            labyrinth.refresh();
            refreshPickerHexBox();
            return;
        }
        if (manaTrapSelected) {
            for (int i = 0; i < manaTrapColorBoxes.size(); i++) {
                String formatted = formatColor(manaTrapColor(i));
                EditBox manaBox = manaTrapColorBoxes.get(i);
                if (!formatted.equals(manaBox.getValue())) {
                    updatingManaTrapBoxes = true;
                    try {
                        manaBox.setValue(formatted);
                    } finally {
                        updatingManaTrapBoxes = false;
                    }
                }
            }
            refreshPickerHexBox();
            return;
        }
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


    private void refreshLightningControls() {
        if (lightningEnabledButton == null) {
            return;
        }
        LightningStrikeSettings settings = lightningSettings();
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

    private void refreshVaultAltarControls() {
        if (altarEnabledButton == null) {
            return;
        }
        VaultAltarSettings settings = vaultAltarSettings();
        altarEnabledButton.setMessage(new TextComponent("Replacement: " + (settings.enabled ? "On" : "Off")));
        altarShaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + vaultAltarShaderCompatibility().label));
        altarFullbrightButton.setMessage(new TextComponent("Fullbright: " + (settings.fullbright ? "On" : "Off")));
        altarOriginMarkersButton.setMessage(new TextComponent("Origin Markers: " + (settings.originMarkersEnabled ? "On" : "Off")));
        altarSoundButton.setMessage(new TextComponent("Sound: " + vaultAltarSoundMode().label));
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

    private void refreshVaultControls() {
        if (dungeonGlobeMapEnabledButton == null || manaTrapEnabledButton == null) {
            return;
        }
        dungeonGlobeMapEnabledButton.setMessage(new TextComponent("Custom Globe Opacity: " + (dungeonGlobeMapSettings().enabled ? "On" : "Off")));
        dungeonGlobeRoomOpacitySlider.refresh();
        dungeonGlobeSharedOpacitySlider.refresh();
        updateChecksButton.setMessage(new TextComponent("Update Checks: " + (ArcaneBeamConfig.updateChecksEnabled() ? "On" : "Off")));
        updateTypesButton.setMessage(new TextComponent("Update Types: "
                + (ArcaneBeamConfig.updateNoticeFilter() == UpdateNoticeFilter.ALL ? "All Updates" : "Critical Only")));
        manaTrapEnabledButton.setMessage(new TextComponent("Mana Trap Visuals: " + (ManaStealerVisualConfig.configuredEnabled() ? "On" : "Off")));
        manaTrapGroundSigilButton.setMessage(new TextComponent("Legacy Ground Sigil: " + (ManaStealerVisualConfig.replaceGroundSigil() ? "Hidden" : "Shown")));
        manaTrapDrainStreamButton.setMessage(new TextComponent("Player Drain Stream: " + (ManaStealerVisualConfig.drainStreamEnabled() ? "On" : "Off")));
        manaTrapParticleCountSlider.refresh();
        manaBeamParticleCountSlider.refresh();
    }

    private void refreshStormArrowControls() {
        if (stormArrowEnabledButton == null) {
            return;
        }
        StormArrowSettings settings = stormArrowSettings();
        stormArrowEnabledButton.setMessage(new TextComponent("Replacement: " + (settings.enabled ? "On" : "Off")));
        stormArrowTargetingCircleButton.setMessage(new TextComponent("Target Circle: " + (settings.showTargetingCircle ? "On" : "Off")));
        stormArrowActualRadiusButton.setMessage(new TextComponent("Actual Radius: " + (settings.useActualRadius ? "On" : "Off")));
        stormArrowShaderCompatibilityButton.setMessage(new TextComponent("Shader Compatibility: " + stormArrowShaderCompatibility().label));
        stormArrowFullbrightButton.setMessage(new TextComponent("Fullbright: " + (settings.fullbright ? "On" : "Off")));
        stormArrowImpactFlashButton.setMessage(new TextComponent("Impact Flash: " + (settings.impactFlashEnabled ? "On" : "Off")));
        boolean missile = archonSelected || blastWaveSelected;
        stormArrowBlasterAlphaSlider.setLabel(missile ? "Missile Alpha" : "Blaster Alpha");
        stormArrowBlasterWidthSlider.setLabel(missile ? "Missile Width" : "Blaster Width");
        stormArrowSegmentLengthSlider.setLabel(missile ? "Plume Length" : "Bolt Length");
        stormArrowSoundButton.setMessage(new TextComponent("Strike: " + stormArrowSoundLabel()));
        stormArrowProjectileSoundButton.setMessage(new TextComponent((smiteSelected || archonSelected || blastWaveSelected ? "Activation: " : "Projectile: ") + stormArrowProjectileSoundMode().label));
        stormArrowCircleAlphaSlider.refresh();
        stormArrowCircleThicknessSlider.refresh();
        stormArrowBlasterAlphaSlider.refresh();
        stormArrowBlasterWidthSlider.refresh();
        stormArrowSegmentLengthSlider.refresh();
        stormArrowImpactFlashSizeSlider.refresh();
        stormArrowLifetimeBox.setValue(Integer.toString(settings.lifetimeTicks));
        stormArrowOriginHeightBox.setValue(formatTenths(settings.originHeight));
        if (archonMissileRadiusBox != null) {
            archonMissileRadiusBox.setValue(formatTenths(archonSettings().missileOriginRadius));
        }
        refreshBlastWaveBoxes();
        if (blastWaveLateralSpreadSlider != null) {
            blastWaveLateralSpreadSlider.refresh();
            blastWaveMissileScaleSlider.refresh();
            blastWavePitchVarianceSlider.refresh();
        }
        refreshStormArrowSoundVolumeBox();
        refreshStormArrowAudioRangeBox();
    }


    private void cycleShaderCompatibility() {
        ShaderCompatibility current = shaderCompatibility();
        settings().shaderCompatibility = current == ShaderCompatibility.ON
                ? ShaderCompatibility.OFF.id
                : ShaderCompatibility.ON.id;
    }

    private boolean resourcePackParticlesShown() {
        return !railSelected && settings() != null && settings().resourcePackParticles;
    }

    private void cycleCrosshairAim() {
        settings().crosshairAim = !settings().crosshairAim;
    }

    private void cycleSound() {
        SoundChoice[] choices = SoundChoice.values();
        SoundChoice current = soundChoice();
        int next = (current.ordinal() + 1) % choices.length;
        settings().sound = choices[next].id;
    }

    private void cycleHand() {
        StartHand current = startHand();
        settings().startHand = current == StartHand.OFFHAND ? StartHand.MAIN_HAND.id : StartHand.OFFHAND.id;
    }

    private void cycleFadeInStyle() {
        FadeInStyle current = fadeInStyle();
        settings().fadeInStyle = current == FadeInStyle.FADE ? FadeInStyle.GROW.id : FadeInStyle.FADE.id;
    }

    private void cycleFadeOutStyle() {
        FadeOutStyle current = fadeOutStyle();
        settings().fadeOutStyle = current == FadeOutStyle.FADE ? FadeOutStyle.SHRINK.id : FadeOutStyle.FADE.id;
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
            ArcaneBeamConfig.requestSave();
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
            ArcaneBeamConfig.requestSave();
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
            ArcaneBeamConfig.requestSave();
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
            // The box accepts up to +-99.99; the Start Position window, the wheel and the config loader all stop at 64.
            double parsed = Math.round(Math.max(-64.0D, Math.min(64.0D, Double.parseDouble(value))) * 100.0D) / 100.0D;
            if (axis == 0) {
                settings().startOffsetX = parsed;
            } else if (axis == 1) {
                settings().startOffsetY = parsed;
            } else {
                settings().startOffsetZ = parsed;
            }
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }

        try {
            settings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFadeInTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            settings().fadeInTicks = clampTicks(Integer.parseInt(value));
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFadeOutTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            settings().fadeOutTicks = clampTicks(Integer.parseInt(value));
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningLifetimeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().lifetimeTicks = clamp(Integer.parseInt(value), 1, 200);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSideCountFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().ringSideCount = clamp(Integer.parseInt(value), 8, 96);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSecondaryCountFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().secondaryRippleCount = clamp(Integer.parseInt(value), 0, 4);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSecondaryDelayFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            lightningSettings().secondaryRippleDelayTicks = clamp(Integer.parseInt(value), 0, 40);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateLightningSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }

        try {
            lightningSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.requestSave();
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
            ArcaneBeamConfig.requestSave();
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
            ArcaneBeamConfig.requestSave();
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
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateManaTrapColorFromText(int slot, String value) {
        if (value == null || updatingManaTrapBoxes) {
            return;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            return;
        }
        try {
            setManaTrapColor(slot, Integer.parseInt(normalized, 16));
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarVerticalTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().cornerVerticalTicks = clamp(Integer.parseInt(value), 0, 60);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarConvergeTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().cornerConvergeTicks = clamp(Integer.parseInt(value), 1, 120);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarCenterGrowTicksFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            vaultAltarSettings().centerGrowTicks = clamp(Integer.parseInt(value), 1, 160);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarOriginHeightFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().cornerOriginHeight = clampHundredths((float) roundHundredths(Double.parseDouble(value)), 0.0F, 15.0F);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarOriginRadiusFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().cornerOriginRadius = clampHundredths((float) roundHundredths(Double.parseDouble(value)), 0.0F, 15.0F);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateAltarSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            vaultAltarSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowLifetimeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            stormArrowSettings().lifetimeTicks = clamp(Integer.parseInt(value), 1, blastWaveSelected ? 20 : 80);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowOriginHeightFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            stormArrowSettings().originHeight = clampTenths((float) roundTenths(Double.parseDouble(value)), blastWaveSelected ? 0.5F : 2.0F, blastWaveSelected ? 16.0F : 64.0F);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateArchonMissileRadiusFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            archonSettings().missileOriginRadius = clampTenths((float) roundTenths(Double.parseDouble(value)), 0.0F, 16.0F);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateBlastWaveOriginXFromText(String value) {
        updateBlastWaveOriginFromText(value, 0);
    }

    private void updateBlastWaveOriginYFromText(String value) {
        updateBlastWaveOriginFromText(value, 1);
    }

    private void updateBlastWaveOriginZFromText(String value) {
        updateBlastWaveOriginFromText(value, 2);
    }

    private void updateBlastWaveOriginFromText(String value, int axis) {
        if (value == null || value.isEmpty() || "-".equals(value) || ".".equals(value) || "-.".equals(value)) {
            return;
        }
        try {
            float parsed = (float) Math.max(-4.0D, Math.min(4.0D, roundOffset(Double.parseDouble(value))));
            if (axis == 0) {
                blastWaveSettings().originOffsetX = parsed;
            } else if (axis == 1) {
                blastWaveSettings().originOffsetY = parsed;
            } else {
                blastWaveSettings().originOffsetZ = parsed;
            }
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateBlastWaveMaxMissilesFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            blastWaveSettings().maxMissiles = clamp(Integer.parseInt(value), 1, 64);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateBlastWaveSoundLayersFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            blastWaveSettings().maxSoundLayers = clamp(Integer.parseInt(value), 1, 12);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateBlastWaveSoundStaggerFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            blastWaveSettings().soundStaggerTicks = clamp(Integer.parseInt(value), 0, 4);
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowSoundVolumeFromText(String value) {
        if (value == null || value.isEmpty() || ".".equals(value)) {
            return;
        }
        try {
            stormArrowSettings().soundVolume = clampSoundVolume((float) roundOffset(Double.parseDouble(value)));
            ArcaneBeamConfig.requestSave();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateStormArrowAudioRangeFromText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            stormArrowSettings().audioRange = clampStormArrowAudioRange(Integer.parseInt(value));
            ArcaneBeamConfig.requestSave();
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
        stormArrowSettings().lifetimeTicks = clamp(stormArrowSettings().lifetimeTicks + amount, 1, blastWaveSelected ? 20 : 80);
    }

    private void nudgeStormArrowOriginHeight(double amount) {
        stormArrowSettings().originHeight = clampTenths((float) roundTenths(stormArrowSettings().originHeight + amount), blastWaveSelected ? 0.5F : 2.0F, blastWaveSelected ? 16.0F : 64.0F);
    }

    private void nudgeArchonMissileRadius(double amount) {
        archonSettings().missileOriginRadius = clampTenths((float) roundTenths(archonSettings().missileOriginRadius + amount), 0.0F, 16.0F);
    }

    private void nudgeBlastWaveOrigin(int axis, boolean increase, boolean coarse) {
        float amount = (float) ((coarse ? 0.10D : 0.01D) * (increase ? 1.0D : -1.0D));
        if (axis == 0) {
            blastWaveSettings().originOffsetX = clampHundredths((float) roundHundredths(blastWaveSettings().originOffsetX + amount), -4.0F, 4.0F);
        } else if (axis == 1) {
            blastWaveSettings().originOffsetY = clampHundredths((float) roundHundredths(blastWaveSettings().originOffsetY + amount), -4.0F, 4.0F);
        } else {
            blastWaveSettings().originOffsetZ = clampHundredths((float) roundHundredths(blastWaveSettings().originOffsetZ + amount), -4.0F, 4.0F);
        }
        refreshBlastWaveBoxes();
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

    private BeamSettings settings() {
        return railSelected ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
    }

    private LightningStrikeSettings lightningSettings() {
        return ArcaneBeamConfig.INSTANCE.lightningStrike;
    }

    private VaultAltarSettings vaultAltarSettings() {
        return ArcaneBeamConfig.INSTANCE.vaultAltar;
    }

    private static boolean isGlobeOpacityLabel(String label) {
        return GLOBE_REVEALED_LABEL.equals(label) || GLOBE_TEAMMATE_LABEL.equals(label);
    }

    private DungeonGlobeMapSettings dungeonGlobeMapSettings() {
        return ArcaneBeamConfig.INSTANCE.dungeonGlobeMap;
    }

    private StormArrowSettings stormArrowSettings() {
        if (blastWaveSelected) {
            return ArcaneBeamConfig.INSTANCE.blastWave;
        }
        if (archonSelected) {
            return ArcaneBeamConfig.INSTANCE.archon;
        }
        return smiteSelected ? ArcaneBeamConfig.INSTANCE.smite : ArcaneBeamConfig.INSTANCE.stormArrow;
    }

    private ArchonSettings archonSettings() {
        return ArcaneBeamConfig.INSTANCE.archon;
    }

    private BlastWaveSettings blastWaveSettings() {
        return ArcaneBeamConfig.INSTANCE.blastWave;
    }

    private void refreshBlastWaveBoxes() {
        if (blastWaveOriginXBox != null) {
            blastWaveOriginXBox.setValue(formatHundredths(blastWaveSettings().originOffsetX));
            blastWaveOriginYBox.setValue(formatHundredths(blastWaveSettings().originOffsetY));
            blastWaveOriginZBox.setValue(formatHundredths(blastWaveSettings().originOffsetZ));
            blastWaveMaxMissilesBox.setValue(Integer.toString(blastWaveSettings().maxMissiles));
            blastWaveSoundLayersBox.setValue(Integer.toString(blastWaveSettings().maxSoundLayers));
            blastWaveSoundStaggerBox.setValue(Integer.toString(blastWaveSettings().soundStaggerTicks));
        }
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
        if (labyrinthSelected) {
            return labyrinth.color(selectedSlot);
        }
        if (manaTrapSelected) {
            return manaTrapColor(selectedSlot);
        }
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
        if (labyrinthSelected) {
            labyrinth.setColor(selectedSlot, color);
            return;
        }
        if (manaTrapSelected) {
            setManaTrapColor(selectedSlot, color);
            return;
        }
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

    private int manaTrapColor(int slot) {
        return switch (slot) {
            case 0 -> ManaStealerVisualConfig.trapHaloColor();
            case 1 -> ManaStealerVisualConfig.trapCoreColor();
            case 2 -> ManaStealerVisualConfig.beamHaloColor();
            default -> ManaStealerVisualConfig.beamCoreColor();
        };
    }

    private int stormArrowColor(int slot) {
        return switch (slot) {
            case 0 -> stormArrowSettings().circleColor;
            case 1 -> stormArrowSettings().blasterColor;
            case 2 -> stormArrowSettings().coreColor;
            case 3 -> stormArrowSettings().impactFlashColor;
            case 4 -> blastWaveSelected ? blastWaveSettings().pulseCircleColor : stormArrowSettings().circleColor;
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
            case 4 -> {
                if (blastWaveSelected) {
                    blastWaveSettings().pulseCircleColor = color;
                }
            }
            default -> {
            }
        }
    }

    private void setManaTrapColor(int slot, int color) {
        switch (slot) {
            case 0 -> ManaStealerVisualConfig.setTrapHaloColor(color);
            case 1 -> ManaStealerVisualConfig.setTrapCoreColor(color);
            case 2 -> ManaStealerVisualConfig.setBeamHaloColor(color);
            case 3 -> ManaStealerVisualConfig.setBeamCoreColor(color);
            default -> {
            }
        }
    }

    private void syncPrimaryColors() {
        settings().color = settings().colors[0];
        settings().glowColor = settings().glowColors[0];
    }

    private List<String> profileNames() {
        if (blastWaveSelected) {
            return ArcaneBeamConfig.blastWaveProfileNames();
        }
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
        if (blastWaveSelected) {
            return ArcaneBeamConfig.selectedBlastWaveProfileName();
        }
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
        if (blastWaveSelected) {
            ArcaneBeamConfig.selectBlastWaveProfile(profileName);
        } else if (archonSelected) {
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
        if (blastWaveSelected) {
            return "Blast Wave Profiles";
        }
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

    private SoundChoice soundChoice() {
        SoundChoice choice = SoundChoice.fromId(settings().sound);
        return choice == null ? SoundChoice.DEFAULT : choice;
    }

    private StartHand startHand() {
        StartHand hand = StartHand.fromId(settings().startHand);
        return hand == null ? StartHand.OFFHAND : hand;
    }

    private ShaderCompatibility shaderCompatibility() {
        ShaderCompatibility compatibility = ShaderCompatibility.fromId(settings().shaderCompatibility);
        return compatibility == null ? ShaderCompatibility.OFF : compatibility;
    }

    private ShaderCompatibility lightningShaderCompatibility() {
        ShaderCompatibility compatibility = ShaderCompatibility.fromId(lightningSettings().shaderCompatibility);
        return compatibility == null ? ShaderCompatibility.OFF : compatibility;
    }

    private ShaderCompatibility vaultAltarShaderCompatibility() {
        ShaderCompatibility compatibility = ShaderCompatibility.fromId(vaultAltarSettings().shaderCompatibility);
        return compatibility == null ? ShaderCompatibility.OFF : compatibility;
    }

    private ShaderCompatibility stormArrowShaderCompatibility() {
        ShaderCompatibility compatibility = ShaderCompatibility.fromId(stormArrowSettings().shaderCompatibility);
        return compatibility == null ? ShaderCompatibility.OFF : compatibility;
    }

    private LightningSoundMode lightningSoundMode() {
        LightningSoundMode mode = LightningSoundMode.fromId(lightningSettings().soundMode);
        return mode == null ? LightningSoundMode.DEFAULT : mode;
    }

    private VaultAltarSoundMode vaultAltarSoundMode() {
        VaultAltarSoundMode mode = VaultAltarSoundMode.fromId(vaultAltarSettings().soundMode);
        return mode == null ? VaultAltarSoundMode.DEFAULT : mode;
    }

    private StormArrowSoundMode stormArrowSoundMode() {
        StormArrowSoundMode mode = StormArrowSoundMode.fromId(stormArrowSettings().soundMode);
        return mode == null ? StormArrowSoundMode.DEFAULT : mode;
    }

    private String stormArrowSoundLabel() {
        StormArrowSoundMode mode = stormArrowSoundMode();
        if ((archonSelected || blastWaveSelected) && mode == StormArrowSoundMode.BLASTER) {
            return "Whistling Bird";
        }
        return mode.label;
    }

    private StormArrowProjectileSoundMode stormArrowProjectileSoundMode() {
        StormArrowProjectileSoundMode mode = StormArrowProjectileSoundMode.fromId(stormArrowSettings().projectileSoundMode);
        return mode == null ? StormArrowProjectileSoundMode.DEFAULT : mode;
    }

    private void cycleLightningSound() {
        LightningSoundMode[] modes = LightningSoundMode.values();
        LightningSoundMode current = lightningSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        lightningSettings().soundMode = modes[next].id;
    }

    private void cycleVaultAltarSound() {
        VaultAltarSoundMode[] modes = VaultAltarSoundMode.values();
        VaultAltarSoundMode current = vaultAltarSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        vaultAltarSettings().soundMode = modes[next].id;
    }

    private void cycleStormArrowSound() {
        StormArrowSoundMode[] modes = StormArrowSoundMode.values();
        StormArrowSoundMode current = stormArrowSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        stormArrowSettings().soundMode = modes[next].id;
    }

    private void cycleStormArrowProjectileSound() {
        StormArrowProjectileSoundMode[] modes = StormArrowProjectileSoundMode.values();
        StormArrowProjectileSoundMode current = stormArrowProjectileSoundMode();
        int next = (current.ordinal() + 1) % modes.length;
        stormArrowSettings().projectileSoundMode = modes[next].id;
    }

    private FadeInStyle fadeInStyle() {
        FadeInStyle style = FadeInStyle.fromId(settings().fadeInStyle);
        return style == null ? FadeInStyle.FADE : style;
    }

    private FadeOutStyle fadeOutStyle() {
        FadeOutStyle style = FadeOutStyle.fromId(settings().fadeOutStyle);
        return style == null ? FadeOutStyle.SHRINK : style;
    }

    /** "Fade In" becomes "Fade", "Shrink Out" becomes "Shrink": the button already names the direction. */
    private static String shortStyleName(String label) {
        if (label.endsWith(" In")) {
            return label.substring(0, label.length() - 3);
        }
        return label.endsWith(" Out") ? label.substring(0, label.length() - 4) : label;
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

}
