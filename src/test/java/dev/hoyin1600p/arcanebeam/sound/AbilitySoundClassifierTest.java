package dev.hoyin1600p.arcanebeam.sound;

import dev.hoyin1600p.arcanebeam.mixin.accessor.AbstractSoundInstanceAccessor;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilitySoundClassifierTest {
    @Test
    void arcanePreservesToleranceWindow() {
        assertTrue(AbilitySoundClassifier.matchesArcane(1.951F, 0.451F));
        assertTrue(AbilitySoundClassifier.matchesArcane(2.049F, 0.549F));
        assertFalse(AbilitySoundClassifier.matchesArcane(1.949F, 0.5F));
        assertFalse(AbilitySoundClassifier.matchesArcane(2.051F, 0.5F));
        assertFalse(AbilitySoundClassifier.matchesArcane(2.0F, 0.449F));
        assertFalse(AbilitySoundClassifier.matchesArcane(2.0F, 0.551F));
    }

    @Test
    void railMatchesExactVaultTupleWithSmallTolerance() {
        assertTrue(AbilitySoundClassifier.matchesRail(2.0F, 0.5F));
        assertTrue(AbilitySoundClassifier.matchesRail(1.951F, 0.451F));
        assertTrue(AbilitySoundClassifier.matchesRail(2.049F, 0.549F));
        assertFalse(AbilitySoundClassifier.matchesRail(1.949F, 0.5F));
        assertFalse(AbilitySoundClassifier.matchesRail(2.0F, 0.551F));
    }

    @Test
    void arcanePrismSoundIsNeitherArcaneNorRail() {
        assertFalse(AbilitySoundClassifier.matchesRail(1.8F, 0.45F));
        assertFalse(AbilitySoundClassifier.matchesArcane(1.8F, 0.45F));
    }

    @Test
    void otherVaultTuplesAreNotMatched() {
        assertFalse(AbilitySoundClassifier.matchesRail(1.0F, 1.0F));
        assertFalse(AbilitySoundClassifier.matchesRail(1.65F, 0.7F));
        assertFalse(AbilitySoundClassifier.matchesArcane(1.8F, 0.25F));
    }

    @Test
    void classificationReadsOnlyRawAccessorValues() {
        SoundInstance arcane = rawSound(2.0F, 0.5F);
        SoundInstance rail = rawSound(2.0F, 0.5F);

        assertTrue(AbilitySoundClassifier.isVaultArcaneSound(arcane));
        assertTrue(AbilitySoundClassifier.isVaultRailSound(rail));
    }

    @Test
    void unknownSoundImplementationFailsOpenWithoutCallingGetters() {
        SoundInstance unknown = (SoundInstance) Proxy.newProxyInstance(
                SoundInstance.class.getClassLoader(),
                new Class<?>[]{SoundInstance.class},
                (proxy, method, args) -> {
                    throw new AssertionError("Unexpected SoundInstance call: " + method.getName());
                });

        assertFalse(AbilitySoundClassifier.isVaultArcaneSound(unknown));
        assertFalse(AbilitySoundClassifier.isVaultRailSound(unknown));
    }

    private static SoundInstance rawSound(float rawPitch, float rawVolume) {
        return (SoundInstance) Proxy.newProxyInstance(
                SoundInstance.class.getClassLoader(),
                new Class<?>[]{SoundInstance.class, AbstractSoundInstanceAccessor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "arcanebeam$getRawPitch" -> rawPitch;
                    case "arcanebeam$getRawVolume" -> rawVolume;
                    default -> throw new AssertionError("Resolved sound method was called: " + method.getName());
                });
    }
}
