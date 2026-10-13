package dev.hoyin1600p.arcanebeam.config;

import dev.hoyin1600p.arcanebeam.config.model.ArchonSettings;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.BlastWaveSettings;
import dev.hoyin1600p.arcanebeam.config.model.LightningStrikeSettings;
import dev.hoyin1600p.arcanebeam.config.model.SmiteSettings;
import dev.hoyin1600p.arcanebeam.config.model.StormArrowSettings;
import dev.hoyin1600p.arcanebeam.config.model.VaultAltarSettings;

/** Field-by-field copies of settings objects, used when a profile is created or defaults are restored. */
final class ConfigCopies {
    private ConfigCopies() {
    }

    static BeamSettings copyOf(BeamSettings source) {
        BeamSettings copy = new BeamSettings();
        copy.enabled = source.enabled;
        copy.color = source.color;
        copy.colors = source.colors == null ? null : source.colors.clone();
        copy.glowColor = source.glowColor;
        copy.glowColors = source.glowColors == null ? null : source.glowColors.clone();
        copy.radius = source.radius;
        copy.alpha = source.alpha;
        copy.intensity = source.intensity;
        copy.opacity = source.opacity;
        copy.glowRadius = source.glowRadius;
        copy.glowOpacity = source.glowOpacity;
        copy.colorShiftTicks = source.colorShiftTicks;
        copy.glowRotationRpm = source.glowRotationRpm;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.maxRange = source.maxRange;
        copy.sound = source.sound;
        copy.soundVolume = source.soundVolume;
        copy.fadeInStyle = source.fadeInStyle;
        copy.fadeInTicks = source.fadeInTicks;
        copy.fadeOutStyle = source.fadeOutStyle;
        copy.fadeOutTicks = source.fadeOutTicks;
        copy.startHand = source.startHand;
        copy.startOffsetX = source.startOffsetX;
        copy.startOffsetY = source.startOffsetY;
        copy.startOffsetZ = source.startOffsetZ;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.crosshairAim = source.crosshairAim;
        copy.resourcePackParticles = source.resourcePackParticles;
        copy.particleScale = source.particleScale;
        copy.orientParticles = source.orientParticles;
        copy.particleRotationDegrees = source.particleRotationDegrees;
        return copy;
    }

    static LightningStrikeSettings copyOf(LightningStrikeSettings source) {
        LightningStrikeSettings copy = new LightningStrikeSettings();
        copy.enabled = source.enabled;
        copy.startRadius = source.startRadius;
        copy.endRadius = source.endRadius;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.ringThickness = source.ringThickness;
        copy.ringSideCount = source.ringSideCount;
        copy.renderYOffset = source.renderYOffset;
        copy.ringColor = source.ringColor;
        copy.centerFlashColor = source.centerFlashColor;
        copy.alpha = source.alpha;
        copy.ringInteriorOpacity = source.ringInteriorOpacity;
        copy.sphereColor = source.sphereColor;
        copy.sphereRadius = source.sphereRadius;
        copy.sphereOpacity = source.sphereOpacity;
        copy.coneColor = source.coneColor;
        copy.coneHeight = source.coneHeight;
        copy.coneRadius = source.coneRadius;
        copy.coneOpacity = source.coneOpacity;
        copy.spotColor = source.spotColor;
        copy.spotCount = source.spotCount;
        copy.spotSize = source.spotSize;
        copy.spotOpacity = source.spotOpacity;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.shaderCompatibilityMigrated = source.shaderCompatibilityMigrated;
        copy.secondaryRippleCount = source.secondaryRippleCount;
        copy.secondaryRippleSize = source.secondaryRippleSize;
        copy.secondaryRippleDelayTicks = source.secondaryRippleDelayTicks;
        copy.soundMode = source.soundMode;
        copy.soundVolume = source.soundVolume;
        return copy;
    }

    static VaultAltarSettings copyOf(VaultAltarSettings source) {
        VaultAltarSettings copy = new VaultAltarSettings();
        copy.enabled = source.enabled;
        copy.cornerColors = source.cornerColors == null ? null : source.cornerColors.clone();
        copy.cornerRadius = source.cornerRadius;
        copy.cornerOpacity = source.cornerOpacity;
        copy.cornerVerticalTicks = source.cornerVerticalTicks;
        copy.cornerConvergeTicks = source.cornerConvergeTicks;
        copy.centerGrowTicks = source.centerGrowTicks;
        copy.cornerOriginHeight = source.cornerOriginHeight;
        copy.cornerOriginRadius = source.cornerOriginRadius;
        copy.cornerOriginMigrated = source.cornerOriginMigrated;
        copy.centerColors = source.centerColors == null ? null : source.centerColors.clone();
        copy.centerHeight = source.centerHeight;
        copy.centerFadeHeight = source.centerFadeHeight;
        copy.centerBottomRadius = source.centerBottomRadius;
        copy.centerTopRadius = source.centerTopRadius;
        copy.centerOpacity = source.centerOpacity;
        copy.centerGlowColors = source.centerGlowColors == null ? null : source.centerGlowColors.clone();
        copy.centerGlowHeight = source.centerGlowHeight;
        copy.centerGlowFadeHeight = source.centerGlowFadeHeight;
        copy.centerGlowBottomRadius = source.centerGlowBottomRadius;
        copy.centerGlowTopRadius = source.centerGlowTopRadius;
        copy.centerGlowOpacity = source.centerGlowOpacity;
        copy.centerGlowRotationRpm = source.centerGlowRotationRpm;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.originMarkersEnabled = source.originMarkersEnabled;
        copy.soundMode = source.soundMode;
        copy.soundVolume = source.soundVolume;
        return copy;
    }

    static StormArrowSettings copyOf(StormArrowSettings source) {
        StormArrowSettings copy = new StormArrowSettings();
        copy.enabled = source.enabled;
        copy.showTargetingCircle = source.showTargetingCircle;
        copy.useActualRadius = source.useActualRadius;
        copy.circleColor = source.circleColor;
        copy.circleAlpha = source.circleAlpha;
        copy.circleThickness = source.circleThickness;
        copy.blasterColor = source.blasterColor;
        copy.coreColor = source.coreColor;
        copy.blasterAlpha = source.blasterAlpha;
        copy.blasterWidth = source.blasterWidth;
        copy.segmentLength = source.segmentLength;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.originHeight = source.originHeight;
        copy.impactFlashEnabled = source.impactFlashEnabled;
        copy.impactFlashColor = source.impactFlashColor;
        copy.impactFlashSize = source.impactFlashSize;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.soundMode = source.soundMode;
        copy.projectileSoundMode = source.projectileSoundMode;
        copy.soundVolume = source.soundVolume;
        copy.audioRange = source.audioRange;
        return copy;
    }

    static SmiteSettings copyOf(SmiteSettings source) {
        SmiteSettings copy = new SmiteSettings();
        copy.enabled = source.enabled;
        copy.showTargetingCircle = source.showTargetingCircle;
        copy.useActualRadius = source.useActualRadius;
        copy.circleColor = source.circleColor;
        copy.circleAlpha = source.circleAlpha;
        copy.circleThickness = source.circleThickness;
        copy.blasterColor = source.blasterColor;
        copy.coreColor = source.coreColor;
        copy.blasterAlpha = source.blasterAlpha;
        copy.blasterWidth = source.blasterWidth;
        copy.segmentLength = source.segmentLength;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.originHeight = source.originHeight;
        copy.impactFlashEnabled = source.impactFlashEnabled;
        copy.impactFlashColor = source.impactFlashColor;
        copy.impactFlashSize = source.impactFlashSize;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.soundMode = source.soundMode;
        copy.projectileSoundMode = source.projectileSoundMode;
        copy.soundVolume = source.soundVolume;
        copy.audioRange = source.audioRange;
        return copy;
    }

    static ArchonSettings copyOf(ArchonSettings source) {
        ArchonSettings copy = new ArchonSettings();
        copy.enabled = source.enabled;
        copy.showTargetingCircle = source.showTargetingCircle;
        copy.useActualRadius = source.useActualRadius;
        copy.circleColor = source.circleColor;
        copy.circleAlpha = source.circleAlpha;
        copy.circleThickness = source.circleThickness;
        copy.blasterColor = source.blasterColor;
        copy.coreColor = source.coreColor;
        copy.blasterAlpha = source.blasterAlpha;
        copy.blasterWidth = source.blasterWidth;
        copy.segmentLength = source.segmentLength;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.originHeight = source.originHeight;
        copy.missileOriginRadius = source.missileOriginRadius;
        copy.impactFlashEnabled = source.impactFlashEnabled;
        copy.impactFlashColor = source.impactFlashColor;
        copy.impactFlashSize = source.impactFlashSize;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.soundMode = source.soundMode;
        copy.projectileSoundMode = source.projectileSoundMode;
        copy.soundVolume = source.soundVolume;
        copy.audioRange = source.audioRange;
        return copy;
    }

    static BlastWaveSettings copyOf(BlastWaveSettings source) {
        BlastWaveSettings copy = new BlastWaveSettings();
        copy.enabled = source.enabled;
        copy.showTargetingCircle = source.showTargetingCircle;
        copy.useActualRadius = source.useActualRadius;
        copy.circleColor = source.circleColor;
        copy.circleAlpha = source.circleAlpha;
        copy.circleThickness = source.circleThickness;
        copy.blasterColor = source.blasterColor;
        copy.coreColor = source.coreColor;
        copy.blasterAlpha = source.blasterAlpha;
        copy.blasterWidth = source.blasterWidth;
        copy.segmentLength = source.segmentLength;
        copy.lifetimeTicks = source.lifetimeTicks;
        copy.originHeight = source.originHeight;
        copy.originOffsetX = source.originOffsetX;
        copy.originOffsetY = source.originOffsetY;
        copy.originOffsetZ = source.originOffsetZ;
        copy.lateralSpread = source.lateralSpread;
        copy.pulseCircleColor = source.pulseCircleColor;
        copy.missileScale = source.missileScale;
        copy.maxMissiles = source.maxMissiles;
        copy.maxSoundLayers = source.maxSoundLayers;
        copy.soundStaggerTicks = source.soundStaggerTicks;
        copy.pitchVariance = source.pitchVariance;
        copy.impactFlashEnabled = source.impactFlashEnabled;
        copy.impactFlashColor = source.impactFlashColor;
        copy.impactFlashSize = source.impactFlashSize;
        copy.fullbright = source.fullbright;
        copy.shaderCompatibility = source.shaderCompatibility;
        copy.soundMode = source.soundMode;
        copy.projectileSoundMode = source.projectileSoundMode;
        copy.soundVolume = source.soundVolume;
        copy.audioRange = source.audioRange;
        return copy;
    }
}
