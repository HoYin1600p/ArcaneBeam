package dev.hoyin1600p.arcanebeam.ability.manastealer;

import static dev.hoyin1600p.arcanebeam.util.Hashing.mix64;
import static dev.hoyin1600p.arcanebeam.util.Hashing.unit;

public final class ManaStealerOrbKinematics {

    private ManaStealerOrbKinematics() {
    }

    public static Sample sample(long seed, double minimumSpeed, double maximumSpeed) {
        long first = mix64(seed);
        long second = mix64(first);
        long third = mix64(second);

        double vertical = Math.fma(unit(first), 2.0D, -1.0D);
        double angle = unit(second) * Math.PI * 2.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        double speed = Math.fma(unit(third), maximumSpeed - minimumSpeed, minimumSpeed);
        return new Sample(
                horizontal * Math.cos(angle),
                vertical,
                horizontal * Math.sin(angle),
                speed
        );
    }

    public static int lifetimeTicks(double radius, double speedPerTick) {
        if (!(radius > 0.0D) || !(speedPerTick > 0.0D)) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(radius / speedPerTick));
    }

    public static float scale(float progress) {
        float bounded = Math.max(0.0F, Math.min(1.0F, progress));
        return Math.fma(-2.375F, bounded, 2.5F);
    }

    public record Sample(double x, double y, double z, double speedPerTick) {
    }
}
