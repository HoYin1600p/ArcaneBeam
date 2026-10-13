package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.FadeInStyle;
import dev.hoyin1600p.arcanebeam.config.model.FadeOutStyle;
import dev.hoyin1600p.arcanebeam.sound.SoundBeamState;

import java.util.UUID;

public record ActiveBeam(UUID casterId, BeamKind kind, BeamSettings settings,
                         long firstSeenGameTime, long lastSeenGameTime) implements SoundBeamState {
    private static final float FADE_OUT_GRACE_TICKS = 2.0F;
    private static final int REMOTE_ACTIVITY_GRACE_TICKS = 4;

    public long expireAfterTicks() {
        return Math.max(Math.max(settings().lifetimeTicks, settings().fadeOutTicks), REMOTE_ACTIVITY_GRACE_TICKS);
    }

    public float alphaMultiplier(long gameTime, float partialTick) {
        float alpha = 1.0F;
        float age = Math.max(0.0F, gameTime - firstSeenGameTime + partialTick);
        float sinceLastSeen = fadeOutAge(gameTime, partialTick);

        if (fadeInStyle() == FadeInStyle.FADE && settings().fadeInTicks > 0) {
            alpha *= Math.min(1.0F, age / settings().fadeInTicks);
        }
        if (fadeOutStyle() == FadeOutStyle.FADE && settings().fadeOutTicks > 0 && sinceLastSeen > 0.0F) {
            alpha *= Math.max(0.0F, 1.0F - (sinceLastSeen / settings().fadeOutTicks));
        }
        return alpha;
    }

    public float beamRadiusMultiplier(long gameTime, float partialTick) {
        float radius = 1.0F;
        float age = Math.max(0.0F, gameTime - firstSeenGameTime + partialTick);
        float sinceLastSeen = fadeOutAge(gameTime, partialTick);

        if (fadeInStyle() == FadeInStyle.GROW && settings().fadeInTicks > 0) {
            radius *= Math.min(1.0F, age / settings().fadeInTicks);
        }
        if (fadeOutStyle() == FadeOutStyle.SHRINK && settings().fadeOutTicks > 0 && sinceLastSeen > 0.0F) {
            radius *= Math.max(0.0F, 1.0F - (sinceLastSeen / settings().fadeOutTicks));
        }
        return radius;
    }

    public float glowRadiusMultiplier(long gameTime, float partialTick) {
        return beamRadiusMultiplier(gameTime, partialTick);
    }

    private FadeInStyle fadeInStyle() {
        FadeInStyle style = FadeInStyle.fromId(settings().fadeInStyle);
        return style == null ? FadeInStyle.FADE : style;
    }

    private FadeOutStyle fadeOutStyle() {
        FadeOutStyle style = FadeOutStyle.fromId(settings().fadeOutStyle);
        return style == null ? FadeOutStyle.FADE : style;
    }

    private float fadeOutAge(long gameTime, float partialTick) {
        return Math.max(0.0F, gameTime - lastSeenGameTime - FADE_OUT_GRACE_TICKS + partialTick);
    }
}
