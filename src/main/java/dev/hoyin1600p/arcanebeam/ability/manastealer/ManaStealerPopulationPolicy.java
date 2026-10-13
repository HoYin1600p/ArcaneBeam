package dev.hoyin1600p.arcanebeam.ability.manastealer;

public final class ManaStealerPopulationPolicy {
    public static final int ALL = 0;
    public static final int DECREASED = 1;
    public static final int MINIMAL = 2;
    /** Ceiling on live Mana Stealer orb particles across every trap. */
    public static final int GLOBAL_ORB_BUDGET = 1024;

    private ManaStealerPopulationPolicy() {
    }

    public static int target(int quality, int all, int decreased, int minimal) {
        return switch (quality) {
            case DECREASED -> Math.max(0, decreased);
            case MINIMAL -> Math.max(0, minimal);
            default -> Math.max(0, all);
        };
    }

    /**
     * Per-source target after splitting the global budget evenly between active sources. A source
     * never receives less than one orb unless the requested target is zero.
     */
    public static int fairTarget(int requestedTarget, int activeSources, int globalBudget) {
        if (requestedTarget <= 0) {
            return 0;
        }
        int sources = Math.max(1, activeSources);
        int share = Math.max(1, Math.max(0, globalBudget) / sources);
        return Math.min(requestedTarget, share);
    }

    /** Orbs that may still be spawned without exceeding the global budget. */
    public static int remainingBudget(int totalLive, int globalBudget) {
        return Math.max(0, globalBudget - Math.max(0, totalLive));
    }
}
