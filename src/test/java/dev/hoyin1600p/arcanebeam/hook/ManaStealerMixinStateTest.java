package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerOwnership;
import dev.hoyin1600p.arcanebeam.mixin.manastealer.ManaStealerMixinPlugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The Mana Stealer mixin plugin runs while Mixin prepares every mod's configs. If a class it loads references a
 * Minecraft world or Vault class, the JVM may load that class early and other mods' mixins into it (for example
 * ModernFix into Entity) fail to start the game.
 */
class ManaStealerMixinStateTest {
    @Test
    void mixinSetupClassesReferenceNoGameOrVaultClasses() throws IOException {
        for (Class<?> type : new Class<?>[] {ManaStealerMixinPlugin.class, ManaStealerMixinState.class, ManaStealerOwnership.class}) {
            String constants = classFileText(type);
            assertFalse(constants.contains("net/minecraft/world/"), type.getSimpleName() + " references a Minecraft world class");
            assertFalse(constants.contains("net/minecraft/client/"), type.getSimpleName() + " references a Minecraft client class");
            assertFalse(constants.contains("iskallia/"), type.getSimpleName() + " references a Vault class");
            assertFalse(constants.contains("ManaStealerHooks"), type.getSimpleName() + " references the runtime hooks");
        }
    }

    private static String classFileText(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
