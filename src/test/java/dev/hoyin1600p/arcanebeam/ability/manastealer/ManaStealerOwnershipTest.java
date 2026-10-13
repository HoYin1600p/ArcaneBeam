package dev.hoyin1600p.arcanebeam.ability.manastealer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import org.junit.jupiter.api.Test;

class ManaStealerOwnershipTest {
    @Test
    void arcaneBeamOwnsWhenLegacyVroModuleIsAbsent() {
        assertTrue(ManaStealerOwnership.owns(false));
    }

    @Test
    void arcaneBeamYieldsWhileLegacyVroModuleIsPresent() {
        assertFalse(ManaStealerOwnership.owns(true));
    }

    @Test
    void exactLegacyClassResourceControlsDetection() throws Exception {
        URL marker = new URL("file:/legacy-vro-module.class");
        ClassLoader withLegacyModule = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return ManaStealerOwnership.LEGACY_VRO_MODULE_RESOURCE.equals(name) ? marker : null;
            }
        };
        ClassLoader withoutLegacyModule = new ClassLoader(null) {
        };

        assertTrue(ManaStealerOwnership.detectLegacyVroModule(withLegacyModule));
        assertFalse(ManaStealerOwnership.detectLegacyVroModule(withoutLegacyModule));
        assertFalse(ManaStealerOwnership.detectLegacyVroModule(null));
    }
}
