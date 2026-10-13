package dev.hoyin1600p.arcanebeam.sound;

/** The part of an active local beam that the sound controller reads, so sound does not depend on the beam package. */
public interface SoundBeamState {
    long firstSeenGameTime();
}
