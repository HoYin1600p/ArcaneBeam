package dev.hoyin1600p.arcanebeam.config.model;

public final class BeamSettings {
    public boolean enabled = true;
    public int color = 0x8F35FF;
    public int[] colors = new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF};
    public int glowColor = 0x8F35FF;
    public int[] glowColors = new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF};
    public float radius = 0.08F;
    public float alpha = 0.65F;
    public float intensity = 0.08F;
    public float opacity = 0.65F;
    public float glowRadius = 0.14F;
    public float glowOpacity = 0.20F;
    public float colorShiftTicks = 8.0F;
    public float glowRotationRpm = 0.0F;
    public int lifetimeTicks = 3;
    public double maxRange = 16.0D;
    public String sound = SoundChoice.DEFAULT.id;
    public float soundVolume = 1.00F;
    public String fadeInStyle = FadeInStyle.FADE.id;
    public int fadeInTicks = -1;
    public String fadeOutStyle = FadeOutStyle.SHRINK.id;
    public int fadeOutTicks = -1;
    public String startHand = StartHand.OFFHAND.id;
    public double startOffsetX = 0.38D;
    public double startOffsetY = -0.45D;
    public double startOffsetZ = 0.18D;
    public String shaderCompatibility;
    public boolean crosshairAim = false;
    /** Arcane only: show Vault's own particles (and any resource pack retexture) instead of the mod beam. */
    public boolean resourcePackParticles = false;
    /** Resource pack mode: multiplier on Vault's particle size. */
    public float particleScale = 1.0F;
    /** Resource pack mode: turn each sprite so its top points along the beam on screen. */
    public boolean orientParticles = true;
    /** Resource pack mode: extra roll for sprites whose art does not point up (degrees, clockwise). */
    public float particleRotationDegrees = 0.0F;

    public BeamSettings() {
    }

    public BeamSettings(int color, int[] colors, float intensity, float opacity, float glowRadius, int lifetimeTicks, double maxRange) {
        this.color = color;
        this.colors = colors;
        this.glowColor = color;
        this.glowColors = colors.clone();
        this.radius = intensity;
        this.alpha = opacity;
        this.intensity = intensity;
        this.opacity = opacity;
        this.glowRadius = glowRadius;
        this.glowOpacity = 0.20F;
        this.lifetimeTicks = lifetimeTicks;
        this.maxRange = maxRange;
        this.shaderCompatibility = ShaderCompatibility.OFF.id;
    }
    public static BeamSettings defaultArcane() {
        return new BeamSettings(0x8F35FF, new int[]{0x8F35FF, 0xB369FF, 0x5C7CFF, 0xFFFFFF}, 0.08F, 0.65F, 0.14F, 3, 16.0D);
    }

    public static BeamSettings defaultRail() {
        BeamSettings settings = new BeamSettings(0x00FF44, new int[]{0x00FF44, 0x7CFF5C, 0x00FFC8, 0xFFFFFF}, 0.11F, 0.95F, 0.18F, 8, 128.0D);
        settings.crosshairAim = true;
        return settings;
    }
}
