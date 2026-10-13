package dev.hoyin1600p.arcanebeam.ability.manastealer;

/** Allocation-free camera-facing quad coordinates used only by the Mana Stealer orb. */
final class ManaStealerBillboardGeometry {
    private static final ThreadLocal<ManaStealerBillboardGeometry> LOCAL =
            ThreadLocal.withInitial(ManaStealerBillboardGeometry::new);

    private final float[] positions = new float[12];

    private ManaStealerBillboardGeometry() {
    }

    static ManaStealerBillboardGeometry compute(
            float leftX, float leftY, float leftZ,
            float upX, float upY, float upZ,
            float size,
            float positionX, float positionY, float positionZ
    ) {
        ManaStealerBillboardGeometry geometry = LOCAL.get();
        float[] value = geometry.positions;
        value[0] = Math.fma(leftX + upX, -size, positionX);
        value[1] = Math.fma(leftY + upY, -size, positionY);
        value[2] = Math.fma(leftZ + upZ, -size, positionZ);
        value[3] = Math.fma(-leftX + upX, size, positionX);
        value[4] = Math.fma(-leftY + upY, size, positionY);
        value[5] = Math.fma(-leftZ + upZ, size, positionZ);
        value[6] = Math.fma(leftX + upX, size, positionX);
        value[7] = Math.fma(leftY + upY, size, positionY);
        value[8] = Math.fma(leftZ + upZ, size, positionZ);
        value[9] = Math.fma(-leftX + upX, -size, positionX);
        value[10] = Math.fma(-leftY + upY, -size, positionY);
        value[11] = Math.fma(-leftZ + upZ, -size, positionZ);
        return geometry;
    }

    float x0() { return positions[0]; }
    float y0() { return positions[1]; }
    float z0() { return positions[2]; }
    float x1() { return positions[3]; }
    float y1() { return positions[4]; }
    float z1() { return positions[5]; }
    float x2() { return positions[6]; }
    float y2() { return positions[7]; }
    float z2() { return positions[8]; }
    float x3() { return positions[9]; }
    float y3() { return positions[10]; }
    float z3() { return positions[11]; }
}
