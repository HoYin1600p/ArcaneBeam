package dev.hoyin1600p.arcanebeam.config.model;

public final class BlastWaveSettings extends StormArrowSettings {
    public float originOffsetX = 0.32F;
    public float originOffsetY = -0.28F;
    public float originOffsetZ = 0.18F;
    public float lateralSpread = 1.0F;
    public int pulseCircleColor = 0x50D5FF;
    public float missileScale = 1.0F;
    public int maxMissiles = 12;
    public int maxSoundLayers = 4;
    public int soundStaggerTicks = 1;
    public float pitchVariance = 0.04F;

    public BlastWaveSettings() {
        showTargetingCircle = true;
        useActualRadius = true;
        circleColor = 0xD9F3FF;
        circleAlpha = 0.42F;
        circleThickness = 0.08F;
        blasterColor = 0x50D5FF;
        coreColor = 0xFFFFFF;
        blasterAlpha = 0.90F;
        blasterWidth = 0.10F;
        segmentLength = 2.8F;
        lifetimeTicks = 7;
        originHeight = 5.0F;
        impactFlashColor = 0xBFEFFF;
        impactFlashSize = 0.36F;
        soundMode = StormArrowSoundMode.BLASTER.id;
        projectileSoundMode = StormArrowProjectileSoundMode.OPTION_1.id;
        soundVolume = 1.0F;
        audioRange = 32;
    }
}
