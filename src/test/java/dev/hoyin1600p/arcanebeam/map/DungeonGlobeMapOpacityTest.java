package dev.hoyin1600p.arcanebeam.map;

import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapOpacity.Factors;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapOpacity.RevealKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonGlobeMapOpacityTest {
    private static final Factors FACTORS = new Factors(0.15F, 0.40F);

    @Test
    void disabledLeavesRoomsOpaque() {
        assertEquals(1.0F, DungeonGlobeMapOpacity.resolveFactor(false, 15));
        assertFalse(DungeonGlobeMapOpacity.resolveFactors(false, 15, 40).anyFade());
    }

    @Test
    void enabledUsesConfiguredPercentages() {
        Factors factors = DungeonGlobeMapOpacity.resolveFactors(true, 15, 40);
        assertEquals(0.15F, factors.revealed());
        assertEquals(0.40F, factors.shared());
        assertTrue(factors.anyFade());
    }

    @Test
    void configuredPercentagesAreClamped() {
        assertEquals(0.0F, DungeonGlobeMapOpacity.resolveFactor(true, -1));
        assertEquals(1.0F, DungeonGlobeMapOpacity.resolveFactor(true, 101));
        assertFalse(DungeonGlobeMapOpacity.resolveFactors(true, 100, 250).anyFade());
    }

    @Test
    void contextRuleFadesOnlySystemAndShared() {
        assertEquals(RevealKind.SYSTEM, DungeonGlobeMapOpacity.classify("SYSTEM"));
        assertEquals(RevealKind.SHARED, DungeonGlobeMapOpacity.classify("SHARED"));
        assertEquals(RevealKind.NONE, DungeonGlobeMapOpacity.classify("PLAYER_DISCOVERED"));
        assertEquals(RevealKind.NONE, DungeonGlobeMapOpacity.classify("INSCRIPTION"));
    }

    @Test
    void missingAndUnknownContextsDoNotFade() {
        assertEquals(RevealKind.NONE, DungeonGlobeMapOpacity.classify(null));
        assertEquals(RevealKind.NONE, DungeonGlobeMapOpacity.classify("SOME_FUTURE_CONTEXT"));
        assertEquals(RevealKind.NONE, DungeonGlobeMapOpacity.classify("system"));
    }

    @Test
    void eachKindUsesItsOwnOpacity() {
        assertEquals(0.15F, DungeonGlobeMapOpacity.roomFactor(RevealKind.SYSTEM, false, FACTORS));
        assertEquals(0.40F, DungeonGlobeMapOpacity.roomFactor(RevealKind.SHARED, false, FACTORS));
        assertEquals(1.0F, DungeonGlobeMapOpacity.roomFactor(RevealKind.NONE, false, FACTORS));
    }

    @Test
    void visitedRoomsAreAlwaysSolid() {
        assertEquals(1.0F, DungeonGlobeMapOpacity.roomFactor(RevealKind.SYSTEM, true, FACTORS));
        assertEquals(1.0F, DungeonGlobeMapOpacity.roomFactor(RevealKind.SHARED, true, FACTORS));
    }

    @Test
    void fadeIsOnlyNeededWhenSomethingWillBeDrawnFaded() {
        assertTrue(DungeonGlobeMapOpacity.fades(RevealKind.SYSTEM, false, FACTORS));
        assertTrue(DungeonGlobeMapOpacity.fades(RevealKind.SHARED, false, FACTORS));
        assertFalse(DungeonGlobeMapOpacity.fades(RevealKind.SYSTEM, true, FACTORS));
        assertFalse(DungeonGlobeMapOpacity.fades(RevealKind.NONE, false, FACTORS));
        // a 100% slider means that kind is drawn natively even when unvisited
        Factors teammatesSolid = new Factors(0.15F, 1.0F);
        assertFalse(DungeonGlobeMapOpacity.fades(RevealKind.SHARED, false, teammatesSolid));
        assertTrue(DungeonGlobeMapOpacity.fades(RevealKind.SYSTEM, false, teammatesSolid));
    }
}
