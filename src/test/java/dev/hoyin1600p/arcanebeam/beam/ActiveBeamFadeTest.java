package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A held beam keeps its configured opacity: the fade multipliers depend only on its start and last-seen ticks. */
class ActiveBeamFadeTest {
    private static final UUID CASTER = new UUID(1L, 2L);

    private static BeamSettings ownerLikeSettings() {
        BeamSettings settings = new BeamSettings();
        settings.opacity = 0.7348837F;
        settings.fadeInStyle = "fade";
        settings.fadeInTicks = 2;
        settings.fadeOutStyle = "shrink";
        settings.fadeOutTicks = 6;
        return settings;
    }

    @Test
    void heldBeamStaysAtFullMultiplierAfterFadeIn() {
        BeamSettings settings = ownerLikeSettings();
        long firstSeen = 1000L;
        for (long tick = firstSeen + 2; tick < firstSeen + 200; tick++) {
            // Refreshed every tick, as the local cast is while the ability is held.
            ActiveBeam beam = new ActiveBeam(CASTER, BeamKind.ARCANE, settings, firstSeen, tick);
            for (float partial = 0.0F; partial < 1.0F; partial += 0.25F) {
                assertEquals(1.0F, beam.alphaMultiplier(tick, partial), "tick " + tick + " partial " + partial);
                assertEquals(1.0F, beam.beamRadiusMultiplier(tick, partial), "tick " + tick + " partial " + partial);
            }
        }
    }

    @Test
    void fadeInRampsOnceFromTheFirstSeenTick() {
        BeamSettings settings = ownerLikeSettings();
        ActiveBeam beam = new ActiveBeam(CASTER, BeamKind.ARCANE, settings, 1000L, 1000L);
        assertEquals(0.0F, beam.alphaMultiplier(1000L, 0.0F));
        assertEquals(0.5F, beam.alphaMultiplier(1000L, 1.0F));
        assertEquals(1.0F, new ActiveBeam(CASTER, BeamKind.ARCANE, settings, 1000L, 1002L).alphaMultiplier(1002L, 0.0F));
    }
}
