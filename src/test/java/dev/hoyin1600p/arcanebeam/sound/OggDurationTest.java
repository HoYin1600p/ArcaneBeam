package dev.hoyin1600p.arcanebeam.sound;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OggDurationTest {
    private static double secondsOf(String file) throws IOException {
        try (InputStream stream = OggDurationTest.class.getResourceAsStream("/assets/arcanebeam/sounds/abilities/" + file)) {
            assertTrue(stream != null, file);
            return OggDuration.seconds(stream.readAllBytes());
        }
    }

    @Test
    void readsTheLengthOfShippedSoundsWithinAFewMilliseconds() throws IOException {
        // Lengths measured with ffprobe.
        assertEquals(3.4885D, secondsOf("arcane_1.ogg"), 0.01D);
        assertEquals(7.6165D, secondsOf("vault_altar_beam.ogg"), 0.01D);
        assertEquals(4.8635D, secondsOf("lightning_seismic_charge_impact.ogg"), 0.01D);
        assertEquals(1.1206D, secondsOf("archon_strike.ogg"), 0.01D);
    }

    @Test
    void garbageAndTruncatedDataAreRejected() {
        assertEquals(-1.0D, OggDuration.seconds(null), 0.0D);
        assertEquals(-1.0D, OggDuration.seconds(new byte[0]), 0.0D);
        assertEquals(-1.0D, OggDuration.seconds("not an ogg file at all, just text".getBytes()), 0.0D);
    }

    @Test
    void lifetimeCoversTheRealLengthAndFallsBackWhenUnknown() {
        assertEquals(SoundLifetime.UNKNOWN_LENGTH_TICKS, SoundLifetime.ticksFor(-1.0D));
        assertEquals(SoundLifetime.UNKNOWN_LENGTH_TICKS, SoundLifetime.ticksFor(Double.NaN));
        assertEquals(100 + SoundLifetime.GRACE_TICKS, SoundLifetime.ticksFor(5.0D));
        assertEquals(SoundLifetime.MAX_TICKS, SoundLifetime.ticksFor(10_000.0D));
    }

    @Test
    void subtitleKeysGroupBuiltInsAndResourcePackSlots() {
        assertEquals("subtitles.arcanebeam.smite_strike", SoundSubtitles.keyFor("smite_1"));
        assertEquals("subtitles.arcanebeam.smite_strike", SoundSubtitles.keyFor("abilities/smite_resourcepack_2"));
        assertEquals("subtitles.arcanebeam.smite_activation", SoundSubtitles.keyFor("smite_activation_resourcepack_1"));
        assertEquals("subtitles.arcanebeam.archon_activation", SoundSubtitles.keyFor("archon_activation"));
        assertEquals("subtitles.arcanebeam.storm_arrow_projectile", SoundSubtitles.keyFor("storm_arrow_projectile_resourcepack_1"));
        assertEquals("subtitles.arcanebeam.storm_arrow_strike", SoundSubtitles.keyFor("storm_arrow_1"));
        assertEquals("subtitles.arcanebeam.lightning_impact", SoundSubtitles.keyFor("lightning_resourcepack_2_impact"));
        assertEquals("subtitles.arcanebeam.lightning_cast", SoundSubtitles.keyFor("lightning_seismic_charge_cast"));
        assertEquals("subtitles.arcanebeam.altar", SoundSubtitles.keyFor("vault_altar_resourcepack_1"));
        assertEquals("subtitles.arcanebeam.arcane", SoundSubtitles.keyFor("abilities/arcane_2_loop"));
        assertEquals("subtitles.arcanebeam.rail", SoundSubtitles.keyFor("abilities/rail_1"));
    }
}
