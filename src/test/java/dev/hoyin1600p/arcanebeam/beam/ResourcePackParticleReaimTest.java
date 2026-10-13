package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePackParticleReaimTest {
    private static final Vec3 SERVER_START = new Vec3(0.0D, 1.12D, 0.0D);
    private static final Vec3 LOOK = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 HAND = new Vec3(0.18D, 1.17D, 0.38D);
    private static final Vec3 HIT = new Vec3(10.0D, 1.12D, 0.0D);

    @Test
    void particleAtTheServerOriginMovesToTheHand() {
        assertClose(HAND, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, SERVER_START));
    }

    @Test
    void particleAtTheHitStaysAtTheHit() {
        assertClose(HIT, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, HIT));
    }

    @Test
    void midpointMapsToTheMidpointOfTheNewLine() {
        Vec3 expected = HAND.add(HIT.subtract(HAND).scale(0.5D));
        assertClose(expected, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(5.0D, 1.12D, 0.0D)));
    }

    @Test
    void sidewaysScatterIsKept() {
        Vec3 scattered = new Vec3(5.0D, 1.32D, -0.1D);
        Vec3 onLine = ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(5.0D, 1.12D, 0.0D));
        assertClose(onLine.add(0.0D, 0.2D, -0.1D), ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, scattered));
    }

    @Test
    void particleBeyondTheLocalHitIsClampedToTheHit() {
        // The client hit stops at x=10 but the server beam went on to x=60.
        assertClose(HIT, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(60.0D, 1.12D, 0.0D)));
    }

    @Test
    void particleBehindTheServerOriginIsClampedToTheHand() {
        assertClose(HAND, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(-3.0D, 1.12D, 0.0D)));
    }

    @Test
    void scatterBeyondTheLateralLimitIsProjectedOntoTheBeam() {
        Vec3 far = new Vec3(5.0D, 1.12D + 4.0D, 0.0D);
        Vec3 onLine = ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(5.0D, 1.12D, 0.0D));
        assertClose(onLine, ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, far));
    }

    @Test
    void reaimedParticlesNeverPassTheLocalHitAlongTheBeam() {
        for (double along = -5.0D; along <= 200.0D; along += 3.7D) {
            Vec3 result = ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, HIT, new Vec3(along, 1.12D, 0.1D));
            org.junit.jupiter.api.Assertions.assertTrue(result.x <= HIT.x + 1.0E-9D, "x=" + result.x);
            org.junit.jupiter.api.Assertions.assertTrue(result.x >= HAND.x - 1.0E-9D, "x=" + result.x);
        }
    }

    @Test
    void noUsableHitShiftsByTheOriginOffset() {
        Vec3 particle = new Vec3(0.2D, 1.12D, 0.0D);
        assertClose(particle.add(HAND.subtract(SERVER_START)), ArcaneParticleRouter.reaim(SERVER_START, LOOK, HAND, null, particle));
    }

    private static void assertClose(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1.0E-9D);
        assertEquals(expected.y, actual.y, 1.0E-9D);
        assertEquals(expected.z, actual.z, 1.0E-9D);
    }
}
