package dev.hoyin1600p.arcanebeam.gui;

/** One page of the settings screen, shown as an entry in the left navigation list. */
enum ConfigPage {
    ARCANE("Arcane", Group.BEAMS, ArcaneBeamGuiTheme.GOLD, true),
    RAIL("Rail", Group.BEAMS, ArcaneBeamGuiTheme.ORANGE, true),
    LIGHTNING_STRIKE("Lightning Strike", Group.ABILITIES, ArcaneBeamGuiTheme.CYAN, true),
    STORM_ARROW("Storm Arrow", Group.ABILITIES, ArcaneBeamGuiTheme.BLUE, true),
    SMITE("Smite", Group.ABILITIES, ArcaneBeamGuiTheme.RED, true),
    ARCHON("Archon", Group.ABILITIES, ArcaneBeamGuiTheme.VIOLET, true),
    BLAST_WAVE("Blast Wave", Group.ABILITIES, ArcaneBeamGuiTheme.CYAN, true),
    VAULT_ALTAR("Altar Beam", Group.VAULT, ArcaneBeamGuiTheme.GREEN, true),
    GLOBE_MAP("Globe Map", Group.VAULT, ArcaneBeamGuiTheme.GREEN, false),
    MANA_TRAP("Mana Trap", Group.VAULT, ArcaneBeamGuiTheme.GREEN, false),
    LABYRINTH("Labyrinth", Group.VAULT, ArcaneBeamGuiTheme.CYAN, false),
    UPDATE_NOTICES("Update Notices", Group.VAULT, ArcaneBeamGuiTheme.GREEN, false);

    enum Group {
        BEAMS("Beams"),
        ABILITIES("Abilities"),
        VAULT("Vault");

        final String label;

        Group(String label) {
            this.label = label;
        }
    }

    final String label;
    final Group group;
    final int accent;
    /** Whether the page edits a named profile, so the header shows the profile selector. */
    final boolean hasProfiles;

    ConfigPage(String label, Group group, int accent, boolean hasProfiles) {
        this.label = label;
        this.group = group;
        this.accent = accent;
        this.hasProfiles = hasProfiles;
    }

    String breadcrumb() {
        return group.label + " › " + label;
    }

    /** Pages whose origin can be adjusted on screen with the Start Position widget. */
    boolean hasOriginWidget() {
        return this == ARCANE || this == RAIL || this == BLAST_WAVE;
    }
}
