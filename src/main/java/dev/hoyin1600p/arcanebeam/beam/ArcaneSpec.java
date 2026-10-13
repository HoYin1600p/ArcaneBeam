package dev.hoyin1600p.arcanebeam.beam;

/**
 * The specialization chosen inside Vault's "Arcane" ability group. The group id alone says nothing about which of
 * Base, Rail or Prism a quickselect press will cast, so the selection is read from the synced ability tree.
 */
enum ArcaneSpec {
    /** No synced selection yet (before the first ability sync, or after a disconnect). */
    UNKNOWN,
    BASE,
    RAIL,
    PRISM;

    static ArcaneSpec of(boolean rail, boolean prism, boolean base) {
        if (rail) {
            return RAIL;
        }
        if (prism) {
            return PRISM;
        }
        return base ? BASE : UNKNOWN;
    }

    /** Whether pressing the group's quickselect key arms the Arcane (held beam) replacement. */
    boolean armsArcaneOnQuickselect() {
        return this == BASE;
    }

    /** Whether pressing the group's quickselect key arms the Rail replacement. */
    boolean armsRailOnQuickselect() {
        return this == RAIL;
    }
}
