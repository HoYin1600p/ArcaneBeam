package dev.hoyin1600p.arcanebeam.ability.blastwave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlastWaveSourcePickerTest {
    @Test
    void picksClosestCandidateWithinRange() {
        assertEquals(1, BlastWaveSourcePicker.pick(new double[]{4.0D, 0.5D, 8.0D}, 9.0D));
    }

    @Test
    void ignoresCandidatesBeyondRange() {
        assertEquals(-1, BlastWaveSourcePicker.pick(new double[]{9.5D, 20.0D}, 9.0D));
        assertEquals(-1, BlastWaveSourcePicker.pick(new double[0], 9.0D));
    }

    @Test
    void ambiguousCandidatesFallBackToNoSource() {
        assertEquals(-1, BlastWaveSourcePicker.pick(new double[]{1.00D, 1.01D}, 9.0D));
        assertEquals(-1, BlastWaveSourcePicker.pick(new double[]{2.0D, 2.0D}, 9.0D));
    }

    @Test
    void clearlyCloserCandidateWinsOverFarOne() {
        assertEquals(0, BlastWaveSourcePicker.pick(new double[]{0.1D, 3.0D}, 9.0D));
    }

    @Test
    void nanDistancesAreIgnored() {
        assertEquals(1, BlastWaveSourcePicker.pick(new double[]{Double.NaN, 1.0D}, 9.0D));
    }

    @Test
    void capIsClamped() {
        assertEquals(0, BlastWaveSourcePicker.clampCap(-3, 5));
        assertEquals(5, BlastWaveSourcePicker.clampCap(10, 5));
        assertEquals(2, BlastWaveSourcePicker.clampCap(2, 5));
    }
}
