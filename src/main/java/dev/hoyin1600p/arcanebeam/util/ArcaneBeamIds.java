package dev.hoyin1600p.arcanebeam.util;

/** Identifier strings shared across features. The values are wire and config identifiers and must not change. */
public final class ArcaneBeamIds {
    /** Name of the built-in profile every ability profile map starts with. */
    public static final String DEFAULT_PROFILE = "Default";
    /** Vault Hunters ability id for the base Arcane ability. */
    public static final String ARCANE_ABILITY_ID = "Arcane";
    /** Resource namespace of The Vault. */
    public static final String VAULT_NAMESPACE = "the_vault";
    /** Resource namespace of vanilla Minecraft. */
    public static final String MINECRAFT_NAMESPACE = "minecraft";
    /** Path of the plain white texture the beam renderers tint. */
    public static final String WHITE_TEXTURE_PATH = "textures/entity/white.png";
    /** Prefix for the names of the custom render types. */
    public static final String RENDER_TYPE_PREFIX = "arcane_beam_";

    private ArcaneBeamIds() {
    }
}
