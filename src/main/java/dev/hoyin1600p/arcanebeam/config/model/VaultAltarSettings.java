package dev.hoyin1600p.arcanebeam.config.model;

public final class VaultAltarSettings {
    public boolean enabled = true;
    public int[] cornerColors = new int[]{0xFF0D0D, 0xFAA40D};
    public float cornerRadius = 0.035F;
    public float cornerOpacity = 0.91549295F;
    public int cornerVerticalTicks = 20;
    public int cornerConvergeTicks = 40;
    public int centerGrowTicks = 15;
    public float cornerOriginHeight = 3.0F;
    public float cornerOriginRadius = 2.0F;
    public boolean cornerOriginMigrated = true;
    public int[] centerColors = new int[]{0x0DFAEE, 0x0DAAFF};
    public float centerHeight = 1.6316901F;
    public float centerFadeHeight = 1.171831F;
    public float centerBottomRadius = 0.06F;
    public float centerTopRadius = 0.04F;
    public float centerOpacity = 0.80F;
    public int[] centerGlowColors = new int[]{0xBBFD0D, 0x0DFFA2};
    public float centerGlowHeight = 1.0802817F;
    public float centerGlowFadeHeight = 0.94330984F;
    public float centerGlowBottomRadius = 0.14140844F;
    public float centerGlowTopRadius = 0.09419014F;
    public float centerGlowOpacity = 0.25F;
    public float centerGlowRotationRpm = 35.6F;
    public boolean fullbright = true;
    public String shaderCompatibility = ShaderCompatibility.ON.id;
    public boolean originMarkersEnabled = false;
    public String soundMode = VaultAltarSoundMode.ALTAR_1.id;
    public float soundVolume = 1.24F;
}
