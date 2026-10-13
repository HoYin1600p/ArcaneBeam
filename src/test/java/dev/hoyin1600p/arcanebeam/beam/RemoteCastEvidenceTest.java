package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteCastEvidenceTest {
    private final RemoteCastEvidence evidence = new RemoteCastEvidence();
    private static final Vec3 PLAYER = new Vec3(10.0D, 64.0D, 10.0D);

    @Test
    void aSoundAtThePlayerAuthorisesTheMatchingKind() {
        evidence.record(BeamKind.ARCANE, new Vec3(10.5D, 65.0D, 10.0D), 100L);
        assertTrue(evidence.hasEvidence(BeamKind.ARCANE, PLAYER, 110L));
        assertFalse(evidence.hasEvidence(BeamKind.RAIL, PLAYER, 110L));
    }

    @Test
    void noSoundMeansNoEvidenceSoPrismIsNeverClaimed() {
        assertFalse(evidence.hasEvidence(BeamKind.ARCANE, PLAYER, 100L));
        assertFalse(evidence.hasEvidence(BeamKind.RAIL, PLAYER, 100L));
    }

    @Test
    void aSoundAtAnotherPlayerDoesNotAuthorise() {
        evidence.record(BeamKind.RAIL, new Vec3(20.0D, 64.0D, 10.0D), 100L);
        assertFalse(evidence.hasEvidence(BeamKind.RAIL, PLAYER, 101L));
    }

    @Test
    void evidenceExpires() {
        evidence.record(BeamKind.RAIL, PLAYER, 100L);
        assertTrue(evidence.hasEvidence(BeamKind.RAIL, PLAYER, 100L + RemoteCastEvidence.RAIL_EVIDENCE_TICKS));
        assertFalse(evidence.hasEvidence(BeamKind.RAIL, PLAYER, 101L + RemoteCastEvidence.RAIL_EVIDENCE_TICKS));
        evidence.record(BeamKind.ARCANE, PLAYER, 200L);
        assertFalse(evidence.hasEvidence(BeamKind.ARCANE, PLAYER, 201L + RemoteCastEvidence.ARCANE_EVIDENCE_TICKS));
    }

    @Test
    void aClockResetDropsOldEvidence() {
        evidence.record(BeamKind.ARCANE, PLAYER, 500L);
        assertFalse(evidence.hasEvidence(BeamKind.ARCANE, PLAYER, 10L));
    }
}
