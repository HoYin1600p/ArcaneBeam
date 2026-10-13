package dev.hoyin1600p.arcanebeam.ability.manastealer;

public final class ManaStealerDrainStreamPolicy {
    public static final int ALL = 0;
    public static final int DECREASED = 1;
    public static final int MINIMAL = 2;

    private ManaStealerDrainStreamPolicy() {
    }

    public static boolean insideSphere(
            double playerX,
            double playerY,
            double playerZ,
            double centerX,
            double centerY,
            double centerZ,
            double radius
    ) {
        if (!(radius > 0.0D)) {
            return false;
        }
        double deltaX = playerX - centerX;
        double deltaY = playerY - centerY;
        double deltaZ = playerZ - centerZ;
        return Math.fma(deltaX, deltaX, Math.fma(deltaY, deltaY, deltaZ * deltaZ)) <= radius * radius;
    }

    public static int visibleOrbCount(
            double distance,
            double densityPerBlock,
            int minimumOrbs,
            int maximumOrbs,
            int quality
    ) {
        if (!(distance > 0.05D) || !(densityPerBlock > 0.0D) || maximumOrbs <= 0) {
            return 0;
        }
        int boundedMinimum = Math.max(0, Math.min(minimumOrbs, maximumOrbs));
        int allCount = Math.max(
                boundedMinimum,
                Math.min(maximumOrbs, (int) Math.ceil(distance * densityPerBlock) + boundedMinimum)
        );
        double multiplier = switch (quality) {
            case DECREASED -> 0.65D;
            case MINIMAL -> 0.25D;
            default -> 1.0D;
        };
        return Math.max(1, (int) Math.round(allCount * multiplier));
    }

    /** Maximum simultaneous drain streams so that streams times per-stream orbs stays within the budget. */
    public static int maxStreams(int globalOrbBudget, int maximumOrbsPerStream) {
        return Math.max(1, Math.max(0, globalOrbBudget) / Math.max(1, maximumOrbsPerStream));
    }

    public static double progress(double distanceTravelled, int slot, int slotCount) {
        if (slotCount <= 0) {
            return 0.0D;
        }
        double unwrapped = distanceTravelled + (double) slot / (double) slotCount;
        return unwrapped - Math.floor(unwrapped);
    }
}
