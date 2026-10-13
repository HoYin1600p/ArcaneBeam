package dev.hoyin1600p.arcanebeam.ability;

/** Pure helpers for deciding whether a sound or bolt belongs to one specific player. */
public final class OwnershipMath {
    private OwnershipMath() {
    }

    /**
     * True when the candidate at {@code index} is within {@code maxDistanceSqr} of the source and no other
     * candidate is as close (closer, or within {@code ambiguityMarginSqr} of it), so the source cannot be
     * mistaken for another player's.
     */
    public static boolean isUniqueNearest(double[] distancesSqr, int index, double maxDistanceSqr, double ambiguityMarginSqr) {
        if (index < 0 || index >= distancesSqr.length) {
            return false;
        }
        double own = distancesSqr[index];
        if (!(own <= maxDistanceSqr)) {
            return false;
        }
        for (int other = 0; other < distancesSqr.length; other++) {
            if (other != index && distancesSqr[other] - own < ambiguityMarginSqr) {
                return false;
            }
        }
        return true;
    }

    /** Horizontal-plus-vertical box test used for "within casting reach of the caster". */
    public static boolean withinReach(double dx, double dy, double dz, double horizontalReach, double verticalReach) {
        return dx * dx + dz * dz <= horizontalReach * horizontalReach && Math.abs(dy) <= verticalReach;
    }
}
