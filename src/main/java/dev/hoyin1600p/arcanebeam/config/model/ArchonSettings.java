package dev.hoyin1600p.arcanebeam.config.model;

public final class ArchonSettings extends StormArrowSettings {
    public float missileOriginRadius = 2.0F;

    public ArchonSettings() {
        showTargetingCircle = true;
        useActualRadius = false;
        circleColor = 0xD9F3FF;
        circleAlpha = 0.42F;
        circleThickness = 0.08F;
        blasterColor = 0x50D5FF;
        coreColor = 0xFFFFFF;
        blasterAlpha = 0.90F;
        blasterWidth = 0.10F;
        segmentLength = 2.8F;
        lifetimeTicks = 5;
        originHeight = 8.0F;
        missileOriginRadius = 2.0F;
        impactFlashColor = 0xBFEFFF;
        impactFlashSize = 0.36F;
        soundMode = StormArrowSoundMode.BLASTER.id;
        projectileSoundMode = StormArrowProjectileSoundMode.OPTION_1.id;
        soundVolume = 1.0F;
        audioRange = 32;
    }
}
