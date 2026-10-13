package dev.hoyin1600p.arcanebeam.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcaneBeamIdsTest {
    @Test
    void identifierConstantsKeepTheirWireValues() {
        assertEquals("Default", ArcaneBeamIds.DEFAULT_PROFILE);
        assertEquals("Arcane", ArcaneBeamIds.ARCANE_ABILITY_ID);
        assertEquals("the_vault", ArcaneBeamIds.VAULT_NAMESPACE);
        assertEquals("minecraft", ArcaneBeamIds.MINECRAFT_NAMESPACE);
        assertEquals("textures/entity/white.png", ArcaneBeamIds.WHITE_TEXTURE_PATH);
        assertEquals("arcane_beam_", ArcaneBeamIds.RENDER_TYPE_PREFIX);
    }
}
