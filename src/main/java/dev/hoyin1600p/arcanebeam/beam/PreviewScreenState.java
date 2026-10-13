package dev.hoyin1600p.arcanebeam.beam;

/**
 * What the beam and ability previews need to know about the open settings screen, so they do not depend on the
 * screen class itself.
 */
public interface PreviewScreenState {
    BeamKind previewKind();

    boolean blastWaveSelected();

    boolean lightningSelected();
}
