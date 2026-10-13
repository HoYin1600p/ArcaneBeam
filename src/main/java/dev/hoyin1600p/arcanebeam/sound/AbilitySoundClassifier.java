package dev.hoyin1600p.arcanebeam.sound;

import dev.hoyin1600p.arcanebeam.mixin.accessor.AbstractSoundInstanceAccessor;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class AbilitySoundClassifier {
    private static final float ARCANE_PITCH = 2.0F;
    private static final float ARCANE_VOLUME = 0.5F;
    private static final float ARCANE_TOLERANCE = 0.05F;
    // ArcaneRailAbility plays block.beacon.deactivate at volume 0.5 / pitch 2.0 (same tuple as Arcane,
    // told apart by sound event). Arcane Prism plays 0.45 / 1.8, which must not match.
    private static final float RAIL_PITCH = 2.0F;
    private static final float RAIL_VOLUME = 0.5F;
    private static final float RAIL_TOLERANCE = 0.05F;

    private AbilitySoundClassifier() {
    }

    public static boolean isVaultArcaneSound(SoundInstance sound) {
        if (!(sound instanceof AbstractSoundInstanceAccessor rawSound)) {
            return false;
        }
        return matchesArcane(rawSound.arcanebeam$getRawPitch(), rawSound.arcanebeam$getRawVolume());
    }

    public static boolean isVaultRailSound(SoundInstance sound) {
        if (!(sound instanceof AbstractSoundInstanceAccessor rawSound)) {
            return false;
        }
        return matchesRail(rawSound.arcanebeam$getRawPitch(), rawSound.arcanebeam$getRawVolume());
    }

    static boolean matchesArcane(float rawPitch, float rawVolume) {
        return Math.abs(rawPitch - ARCANE_PITCH) <= ARCANE_TOLERANCE
                && Math.abs(rawVolume - ARCANE_VOLUME) <= ARCANE_TOLERANCE;
    }

    static boolean matchesRail(float rawPitch, float rawVolume) {
        return Math.abs(rawPitch - RAIL_PITCH) <= RAIL_TOLERANCE
                && Math.abs(rawVolume - RAIL_VOLUME) <= RAIL_TOLERANCE;
    }
}
