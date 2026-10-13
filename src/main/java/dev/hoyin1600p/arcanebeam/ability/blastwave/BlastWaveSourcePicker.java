package dev.hoyin1600p.arcanebeam.ability.blastwave;

/** Pure selection helpers for attributing a blast wave packet to a player. */
final class BlastWaveSourcePicker {
    /** Squared-distance gap below which two candidates are considered indistinguishable. */
    static final double AMBIGUITY_EPSILON_SQR = 0.04D;

    private BlastWaveSourcePicker() {
    }

    /**
     * Returns the index of the closest candidate within {@code maxDistanceSqr}, or -1 when none
     * qualifies or the two closest candidates cannot be told apart (the caller then falls back to
     * the packet origin instead of guessing a caster).
     */
    static int pick(double[] distancesSqr, double maxDistanceSqr) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;
        for (int index = 0; index < distancesSqr.length; index++) {
            double distance = distancesSqr[index];
            if (!(distance <= maxDistanceSqr)) {
                continue;
            }
            if (distance < bestDistance) {
                secondDistance = bestDistance;
                bestDistance = distance;
                best = index;
            } else if (distance < secondDistance) {
                secondDistance = distance;
            }
        }
        if (best >= 0 && secondDistance - bestDistance < AMBIGUITY_EPSILON_SQR) {
            return -1;
        }
        return best;
    }

    static int clampCap(int cap, int size) {
        return Math.max(0, Math.min(cap, size));
    }
}
