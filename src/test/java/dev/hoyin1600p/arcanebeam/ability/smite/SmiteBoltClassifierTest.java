package dev.hoyin1600p.arcanebeam.ability.smite;

import dev.hoyin1600p.arcanebeam.ability.smite.SmiteBoltClassifier.BoltClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmiteBoltClassifierTest {
    // The two test packs: Smite_Base 16776959, Smite_Archon and Smite_Thunderstorm 14282239.
    private static final int PACK_SMITE = 16776959;
    private static final int PACK_SHARED = 14282239;
    private static final int DEFAULT = SmiteBoltClassifier.VAULT_DEFAULT_COLOR;

    private static SmiteColorTable packTable() {
        SmiteColorTable table = new SmiteColorTable();
        table.add(SmiteVariant.SMITE, PACK_SMITE);
        table.add(SmiteVariant.ARCHON, PACK_SHARED);
        table.add(SmiteVariant.THUNDERSTORM, PACK_SHARED);
        return table;
    }

    private static SmiteColorTable jarDefaultTable() {
        SmiteColorTable table = new SmiteColorTable();
        for (SmiteVariant variant : SmiteVariant.values()) {
            table.add(variant, DEFAULT);
        }
        return table;
    }

    @Test
    void recolouredSmiteStillMatchesSmiteWithoutAnyLocalEvidence() {
        assertEquals(BoltClass.SMITE, SmiteBoltClassifier.classify(PACK_SMITE, packTable(), null, false));
    }

    @Test
    void sharedArchonThunderstormColourIsResolvedByTheLocalEffect() {
        assertEquals(BoltClass.ARCHON, SmiteBoltClassifier.classify(PACK_SHARED, packTable(), SmiteVariant.ARCHON, true));
        assertEquals(BoltClass.THUNDERSTORM, SmiteBoltClassifier.classify(PACK_SHARED, packTable(), SmiteVariant.THUNDERSTORM, true));
    }

    @Test
    void sharedColourWithoutEvidenceIsLeftToVaultNeverDrawnAsArchon() {
        assertEquals(BoltClass.UNKNOWN, SmiteBoltClassifier.classify(PACK_SHARED, packTable(), null, false));
        assertEquals(BoltClass.UNKNOWN, SmiteBoltClassifier.classify(PACK_SHARED, packTable(), SmiteVariant.ARCHON, false));
        assertEquals(BoltClass.UNKNOWN, SmiteBoltClassifier.classify(PACK_SHARED, packTable(), SmiteVariant.SMITE, true));
    }

    @Test
    void jarDefaultColoursUseTheLocalEffectInsteadOfSilentlyDoingNothing() {
        assertEquals(BoltClass.SMITE, SmiteBoltClassifier.classify(DEFAULT, jarDefaultTable(), SmiteVariant.SMITE, true));
        assertEquals(BoltClass.ARCHON, SmiteBoltClassifier.classify(DEFAULT, jarDefaultTable(), SmiteVariant.ARCHON, true));
    }

    @Test
    void defaultColourWithoutLocalEvidenceIsLightningStrike() {
        assertEquals(BoltClass.LIGHTNING_DEFAULT, SmiteBoltClassifier.classify(DEFAULT, jarDefaultTable(), null, false));
        assertEquals(BoltClass.LIGHTNING_DEFAULT, SmiteBoltClassifier.classify(DEFAULT, packTable(), SmiteVariant.SMITE, true));
    }

    @Test
    void unconfiguredColourIsForeign() {
        assertEquals(BoltClass.UNKNOWN, SmiteBoltClassifier.classify(12345, packTable(), SmiteVariant.SMITE, true));
    }

    @Test
    void withoutSyncedColoursTheOriginalColourOnlyBehaviourApplies() {
        SmiteColorTable none = new SmiteColorTable();
        assertEquals(BoltClass.SMITE, SmiteBoltClassifier.classify(PACK_SMITE, none, null, false));
        assertEquals(BoltClass.ARCHON, SmiteBoltClassifier.classify(PACK_SHARED, none, null, false));
        assertEquals(BoltClass.LIGHTNING_DEFAULT, SmiteBoltClassifier.classify(DEFAULT, none, null, false));
        assertEquals(BoltClass.THUNDERSTORM, SmiteBoltClassifier.classify(1, none, SmiteVariant.THUNDERSTORM, true));
    }

    @Test
    void effectPathsMapToVariants() {
        assertEquals(SmiteVariant.SMITE, SmiteVariant.fromEffectPath("smite"));
        assertEquals(SmiteVariant.ARCHON, SmiteVariant.fromEffectPath("smite_archon"));
        assertEquals(SmiteVariant.THUNDERSTORM, SmiteVariant.fromEffectPath("smite_thunderstorm"));
        assertEquals(null, SmiteVariant.fromEffectPath("smite_blast_wave"));
        assertEquals(null, SmiteVariant.fromEffectPath(null));
    }
}
