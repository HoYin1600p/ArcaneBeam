# ArcaneBeam CurseForge Release Copy

This file is for paste-ready CurseForge release text.

Rules for future updates:
- Add a new section for each new version.
- Write for the CurseForge release page first.
- Do not include repo URLs, build paths, Gradle metadata, hashes, or internal engineering notes in the release copy.
- Keep the user-facing release text clean and ready to paste.
- If Codex needs internal notes for a release, put them in a clearly separate `Internal Notes` subsection.

## 0.1.4

### Release Copy

```markdown
## 0.1.4

- Fixed beams being pulled straight north in some vault themes
- Hardened beam direction rendering so theme particles and collision endpoints cannot rotate the rendered beam away from the caster's aim
- Kept block collision behavior intact: beams can still stop early on blocks, but collision can only shorten the beam instead of changing its direction
- Added fallback handling for brief invalid look-vector frames so beams keep their last valid aim instead of snapping to a world direction
- Added a client-side compatibility fix for Compressium compressed blocks on Sophisticated Storage barrel display faces
```

## 0.1.3

### Release Copy

```markdown
## 0.1.3

- Improved Arcane beam stability in multiplayer while moving, strafing, and jumping
- Improved local Arcane activation tracking so nearby players are less likely to trigger a beam or sound from your character
- Fixed Arcane release timing so the local beam and custom looped sound stop much more quickly when the cast key is released
- Kept remote player beam rendering through particle detection while separating it from local-player ownership
- Smoothed the beam origin when entering or leaving sneak so the start point transitions instead of snapping vertically
- Improved beam color cycling so transitions blend more smoothly through all configured colors
- Cleaned up unused experimental trigger code from earlier multiplayer tests
```

## 0.1.2

### Release Copy

```markdown
## 0.1.2

- Fixed Arcane beam and glow jitter caused by fade-out/shrink-out starting too early during active refresh
- Added a fade-out grace window so active Arcane beams stay visually rigid between particle refreshes
- Replaced the square beam core geometry with an 8-sided tube for a rounder look
- Replaced the square glow shell geometry with an 8-sided tube
- Updated shader compatibility rendering to use the same 8-sided beam and glow geometry
```

## 0.1.1

### Release Copy

```markdown
## 0.1.1

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
- Refined config screen layout for color rows, hex box alignment, transition buttons, and tick labels
```

## 0.1.0

### Release Copy

```markdown
## 0.1.0

Initial public release.

- Replaced Vault Arcane and Arcane Rail particle streams with rendered beams
- Added separate Arcane and Rail settings
- Added beam and glow color controls
- Added in-game color picker and live preview
- Added hand-based origin selection and XYZ origin offsets
- Added shader compatibility toggle
- Added custom Arcane and Rail sound selection
- Added suppression for default Vault cast sounds when custom ArcaneBeam sounds are selected
- Added client config persistence in `config/ArcaneBeam.json`
```
