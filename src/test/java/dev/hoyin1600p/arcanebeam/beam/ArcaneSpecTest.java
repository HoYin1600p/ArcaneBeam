package dev.hoyin1600p.arcanebeam.beam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneSpecTest {
    @Test
    void quickselectArmsOnlyTheSelectedSpecialization() {
        assertTrue(ArcaneSpec.BASE.armsArcaneOnQuickselect());
        assertFalse(ArcaneSpec.BASE.armsRailOnQuickselect());
        assertTrue(ArcaneSpec.RAIL.armsRailOnQuickselect());
        assertFalse(ArcaneSpec.RAIL.armsArcaneOnQuickselect());
    }

    @Test
    void prismAndUnsyncedSelectionsArmNothing() {
        for (ArcaneSpec spec : new ArcaneSpec[]{ArcaneSpec.PRISM, ArcaneSpec.UNKNOWN}) {
            assertFalse(spec.armsArcaneOnQuickselect());
            assertFalse(spec.armsRailOnQuickselect());
        }
    }

    @Test
    void specializationIsReadFromTheAbilityClassesFound() {
        assertEquals(ArcaneSpec.RAIL, ArcaneSpec.of(true, false, false));
        assertEquals(ArcaneSpec.PRISM, ArcaneSpec.of(false, true, false));
        assertEquals(ArcaneSpec.BASE, ArcaneSpec.of(false, false, true));
        assertEquals(ArcaneSpec.UNKNOWN, ArcaneSpec.of(false, false, false));
    }
}
