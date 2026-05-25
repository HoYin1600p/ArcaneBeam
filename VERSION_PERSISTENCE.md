# ArcaneBeam Version Persistence

This file is an append-only release history for future Codex sessions.

Use it for:
- current published version context
- version-specific feature summaries
- concise change logs

Do not rewrite older version sections unless the recorded information is factually wrong.
When the user asks to append to this file, add a new subsection for the new version number and its change log.

## Current Version

- Current version: `0.1.3`
- Repo: `https://github.com/HoYin1600p/ArcaneBeam`
- Main technical reference: [PERSISTENCE.md](PERSISTENCE.md)

## Version History

### 0.1.3

#### Summary

`0.1.3` focused on multiplayer trigger stability and local Arcane stop timing. Local Arcane now combines Vault key/ability activity with origin particle confirmation instead of relying only on nearby particles, while remote player beams still use particle detection.

#### Changes

- Improved local Arcane ownership so nearby players are less likely to cause local beam/audio false positives
- Improved multiplayer beam stability while strafing, running, and jumping
- Fixed local Arcane release timing so the beam and custom looped sound stop promptly when the cast key is released
- Preserved remote player beam rendering through particle detection
- Smoothed crouch/stand beam origin movement with a `5` tick pose-origin transition
- Improved color cycling continuity so the fourth-to-first color transition no longer jumps harshly
- Removed unused experimental trigger/mixin code from previous multiplayer attempts

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.3.jar`
- Version source: `gradle.properties -> mod_version=0.1.3`

### 0.1.2

#### Summary

`0.1.2` focused on beam shape and active-state stability. It fixed Arcane transition jitter introduced during the configurable fade/grow update and replaced the square prism beam with a rounder 8-sided tube.

#### Changes

- Fixed Arcane beam and glow jitter caused by fade-out/shrink-out starting too early during active refresh
- Added a fade-out grace window so active Arcane beams stay visually rigid between particle refreshes
- Replaced the square beam core geometry with an 8-sided tube
- Replaced the square glow shell geometry with an 8-sided tube
- Updated shader compatibility rendering to use the same 8-sided beam/glow geometry
- Verified the instance was running the correct rebuilt jar after the renderer changes

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.2.jar`
- Version source: `gradle.properties -> mod_version=0.1.2`

### 0.1.1

#### Summary

`0.1.1` was the first substantial post-release polish pass. It focused on sound controls, transition controls, ownership fixes for nearby-player edge cases, and config screen cleanup.

#### Changes

- Added per-ability sound volume controls
- Added configurable `Fade In` / `Grow In` transition mode per ability
- Added configurable `Fade Out` / `Shrink Out` transition mode per ability
- Added per-ability `Fade In Ticks` and `Fade Out Ticks` fields
- Added mouse wheel support for transition tick fields
- Added mouse wheel support for sound volume fields
- Added live preview support for the newer transition and origin settings
- Fixed Arcane option 2 startup sound so it stops immediately when Arcane ends
- Fixed false local beam/audio triggering from nearby players casting through the local player
- Restricted local ownership attribution to the beam origin area instead of the full ray
- Fixed default/custom sound suppression so Vault sounds only suppress when a custom ArcaneBeam sound is selected
- Fixed Rail `Default` sound fallback
- Refined config screen layout for:
  - color preview rows
  - hex box alignment
  - transition buttons
  - tick labels
- Updated README to document the newer UI and transition/audio behavior

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.1.jar`
- Version source: `gradle.properties -> mod_version=0.1.1`

### 0.1.0

#### Summary

Initial public release of Arcane Beam.

#### Changes

- Replaced Vault Arcane and Arcane Rail particle streams with rendered beams
- Added separate Arcane and Rail settings
- Added beam and glow color controls
- Added in-game color picker and live preview
- Added hand-based origin selection and XYZ origin offsets
- Added shader compatibility toggle
- Added custom Arcane and Rail sound selection
- Added suppression for default Vault cast sounds when custom ArcaneBeam sounds are selected
- Added client config persistence in `config/ArcaneBeam.json`
- Added README, MIT license, and initial persistence documentation
