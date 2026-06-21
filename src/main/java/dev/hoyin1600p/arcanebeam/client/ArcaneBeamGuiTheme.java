package dev.hoyin1600p.arcanebeam.client;

final class ArcaneBeamGuiTheme {
    static final int TEXT = 0xFFEAF6F8;
    static final int MUTED_TEXT = 0xFF9FB0B6;
    static final int GOLD = 0xFFF1BF5B;
    static final int CYAN = 0xFF66D8F0;
    static final int BLUE = 0xFF7BA6F7;
    static final int VIOLET = 0xFFB38BFF;
    static final int GREEN = 0xFF78E3B2;
    static final int ORANGE = 0xFFFF9E64;
    static final int RED = 0xFFFF6E6E;
    static final int CARD_SHADE = 0xA4060B12;
    static final int SOFT_SHADE = 0x5A000000;
    static final int LINE = 0x8866D8F0;
    static final int GOLD_LINE = 0xAAF1BF5B;

    private ArcaneBeamGuiTheme() {
    }

    static int accent(boolean railSelected, boolean lightningSelected, boolean vaultAltarSelected, boolean stormArrowSelected, boolean smiteSelected, boolean archonSelected) {
        if (archonSelected) {
            return VIOLET;
        }
        if (smiteSelected) {
            return RED;
        }
        if (stormArrowSelected) {
            return BLUE;
        }
        if (vaultAltarSelected) {
            return GREEN;
        }
        if (lightningSelected) {
            return CYAN;
        }
        return railSelected ? ORANGE : GOLD;
    }
}
