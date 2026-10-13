package dev.hoyin1600p.arcanebeam.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.TextComponent;

import java.util.Locale;

final class SettingSlider extends AbstractSliderButton {
    static final String GLOBE_REVEALED_LABEL = "Revealed Opacity";
    static final String GLOBE_TEAMMATE_LABEL = "Teammate Opacity";

    interface Getter {
        double get();
    }

    interface Setter {
        void set(double value);
    }

    private static boolean isGlobeOpacityLabel(String label) {
        return GLOBE_REVEALED_LABEL.equals(label) || GLOBE_TEAMMATE_LABEL.equals(label);
    }

    private String label;
    private final double min;
    private final double max;
    private final Getter getter;
    private final Setter setter;

    SettingSlider(int x, int y, int width, int height, String label, double min, double max, Getter getter, Setter setter) {
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
        } else if ("Particle Rotation".equals(label)) {
            setMessage(new TextComponent(label + ": " + Math.round(actualValue()) + "°"));
        } else if ("Spot Count".equals(label) || "Trap Particle Count".equals(label) || "Beam Particle Count".equals(label)) {
            setMessage(new TextComponent(label + ": " + Math.round(actualValue())));
        } else if (isGlobeOpacityLabel(label)) {
            setMessage(new TextComponent(label + ": " + Math.round(actualValue()) + "%"));
        } else {
            setMessage(new TextComponent(label + ": " + String.format(Locale.ROOT, "%.2f", actualValue())));
        }
    }

    @Override
    protected void applyValue() {
        setter.set(actualValue());
        ArcaneBeamConfig.requestSave();
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
        if ("Particle Rotation".equals(label)) {
            return renderLabel + ": " + Math.round(actualValue()) + "°";
        }
        if ("Spot Count".equals(label) || "Trap Particle Count".equals(label) || "Beam Particle Count".equals(label)) {
            return renderLabel + ": " + Math.round(actualValue());
        }
        if (isGlobeOpacityLabel(label)) {
            return renderLabel + ": " + Math.round(actualValue()) + "%";
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
            case GLOBE_REVEALED_LABEL -> "Revealed Op";
            case GLOBE_TEAMMATE_LABEL -> "Teammate Op";
            default -> value;
        };
    }

    static String fitText(Font font, String value, int maxWidth) {
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
