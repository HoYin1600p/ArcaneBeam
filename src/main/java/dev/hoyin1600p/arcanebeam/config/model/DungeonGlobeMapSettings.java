package dev.hoyin1600p.arcanebeam.config.model;

public final class DungeonGlobeMapSettings {
    public boolean enabled = true;
    /** Opacity of rooms revealed by globes, objectives or commands, relative to Vault's own map opacity. */
    public int opacityPercent = 15;
    /** Opacity of rooms revealed because a teammate visited them, relative to Vault's own map opacity. */
    public int sharedOpacityPercent = 40;
}
