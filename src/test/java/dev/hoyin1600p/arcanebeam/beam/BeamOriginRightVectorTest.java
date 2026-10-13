package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeamOriginRightVectorTest {
    private static Vec3 look(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    @Test
    void matchesUpCrossLookWhenNotVertical() {
        for (float yaw = -180.0F; yaw <= 180.0F; yaw += 17.0F) {
            for (float pitch = -80.0F; pitch <= 80.0F; pitch += 20.0F) {
                Vec3 expected = new Vec3(0.0D, 1.0D, 0.0D).cross(look(yaw, pitch)).normalize();
                Vec3 actual = BeamTracer.horizontalLeftFromYaw(yaw);
                assertEquals(expected.x, actual.x, 1.0E-6D);
                assertEquals(expected.y, actual.y, 1.0E-6D);
                assertEquals(expected.z, actual.z, 1.0E-6D);
            }
        }
    }

    @Test
    void isAlwaysAHorizontalUnitVectorEvenForVerticalAim() {
        assertEquals(1.0D, BeamTracer.horizontalLeftFromYaw(123.0F).length(), 1.0E-9D);
        assertEquals(0.0D, BeamTracer.horizontalLeftFromYaw(123.0F).y, 0.0D);
    }
}
