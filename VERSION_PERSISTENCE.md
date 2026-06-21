# ArcaneBeam Version Persistence

This file is an append-only release history for future Codex sessions.

Use it for:
- current published version context
- version-specific feature summaries
- concise change logs

Do not rewrite older version sections unless the recorded information is factually wrong.
When the user asks to append to this file, add a new subsection for the new version number and its change log.

## Current Version

- Current version: `0.1.10`
- Repo: `https://github.com/HoYin1600p/ArcaneBeam`
- Main technical reference: [PERSISTENCE.md](PERSISTENCE.md)

## Version History

### 0.1.10 - Menu Overhaul Worktree Build (2026-06-21)

#### Summary

This build keeps the existing `0.1.10` version and adds the Option B Arcane Starforge Hybrid config-menu foundation.
It is a UI/layout implementation pass, not a gameplay behavior change.

#### Changes

- Added production GUI atlas `assets/arcanebeam/textures/gui/config_atlas.png`
- Added `NineSliceRenderer` and `ArcaneBeamGuiTheme`
- Replaced the config screen's full-screen dim background with localized top-bar and bottom-drawer chrome
- Added `EXPANDED`, `COLLAPSED`, and `HIDDEN` drawer states
- Added `Collapse`, `Clean View`, and non-text-field `H` hide/restore behavior
- Added a compact clickable `Restore Menu (H)` handle while Clean View is active
- Moved existing config widgets into bottom-drawer/card coordinates while preserving existing config save/load handlers
- Added `ConfigTabId` and descriptor-driven tab layout with overflow arrows and mouse-wheel scrolling
- Replaced the always-visible palette with a color picker popover opened from swatches or hex fields
- Added themed rendering for buttons, sliders, and edit box frames
- Increased the expanded bottom drawer height and tightened dense-tab row spacing so lower input/button rows stay inside the drawer
- Kept native `EditBox` bordered text layout inside the themed frame so input text is vertically centered
- Suppresses vanilla, Forge, and Vault HUD overlays for the full duration of the config screen
- Keeps Clean View focused on hiding ArcaneBeam chrome for an unobstructed preview
- Restores the player's previous `options.hideGui` state when closing the config screen
- Reworked slider rendering so labels sit above the track/handle with compact labels on narrow controls
- Removed redundant Arcane/Rail transition tick labels that visually collided with buttons
- Shortened the color picker helper caption so it no longer clips
- Anchored dense-tab color labels above each swatch/hex group and removed redundant in-card row titles
- Iterated on Arcane/Rail color-row spacing during the card conversion
- Reduced the color picker inset frame width so it no longer protrudes past the popover's right edge
- Added titled control-card backgrounds without changing config behavior
- Added a selected-slot hex input inside the color picker popover
- Added lightweight sub-card backgrounds behind existing control groups while preserving widget positions and config behavior
- Added horizontal control-card overflow scrolling with viewport-aware widget and label visibility
- Converted Arcane/Rail controls and color rows into titled Phase 6 cards while preserving config behavior
- Converted Lightning Strike, Vault Altar, Storm Arrow, Smite, and Archon controls into titled Phase 6 cards
- Reduced the expanded drawer height and standardized the Colors card width across tabs
- Switched Arcane/Rail color rows to `Core 1-4` and `Glow 1-4` label-above-slot formatting
- Changed control-card scrolling to snap by actual card boundaries, render partial card chrome at scroll edges, and hide only clipped child controls
- Replaced local Sophisticated Core/Storage compile-only jar paths with CurseMaven compile-only coordinates for reproducible builds
- Updated README and project memory for the new config-menu behavior

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.10.jar`
- Version source: `gradle.properties -> mod_version=0.1.10`
- SHA256: `1147AF17FAFF4DD51BFE62960256B08A261C77989EA3598B8DC0318BC45890C8`

### 0.1.7

#### Summary

`0.1.7` adds a client-only Sophisticated Storage limited-barrel front-face interaction compatibility mixin.
Normal front-face limited-barrel interactions remain untouched so single-slot deposit, double-right-click deposit-all, dyes, packing tape, and upgrades keep Sophisticated Storage's original behavior.

#### Changes

- Added `SophisticatedStorageLimitedBarrelClientInteractionMixin`
- Registered the new mixin in the client mixin list
- Added the mixin to ArcaneBeam's optional Sophisticated Storage mixin gate
- Added the mixin to the existing `vaultadditions` conflict skip set
- Changed front-face GUI opening to Sneak + right-click with both hands empty
- Used Minecraft's secondary-use state so the behavior follows the user's configured Sneak keybind instead of hard-coding physical Shift
- Preserved regular front-face right-clicks, including double-right-click deposit-all
- Bumped the mod version to `0.1.7`
- Updated CurseForge release copy for `0.1.7`

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.7.jar`
- Version source: `gradle.properties -> mod_version=0.1.7`
- SHA256: `45B7158E34679DF55B89C32BA863D55BB144B44AD1E1BA01CD0D6CAEE555B29D`

### 0.1.6

#### Summary

`0.1.6` adds config-screen profiles for Arcane and Rail settings.
It also makes ArcaneBeam defer its Sophisticated Storage / Compressium compatibility mixins when the exact Forge mod id `vaultadditions` is present.

#### Changes

- Added per-category Arcane and Rail profiles in the config screen
- Added left-side profile controls for creating and selecting profiles
- Moved the profile controls closer to the rest of the config UI after initial layout testing
- Saved profile data in `config/ArcaneBeam.json` under `arcaneProfiles` and `railProfiles`
- Migrated existing Arcane and Rail settings into `Default` profiles when the new config format is first loaded
- Moved shader compatibility into the active profile so every editable config-screen setting is profile-specific
- Added an exact `vaultadditions` loading-list check that skips only ArcaneBeam's Sophisticated Storage compatibility mixins
- Kept the guard fail-open: if the Forge loading mod list cannot be queried, ArcaneBeam applies its optional mixins as before
- Lowered ArcaneBeam's Sophisticated Storage compatibility mixin priority to `900`, below Mixin's default `1000`
- Bumped the mod version to `0.1.6`
- Updated CurseForge release copy for `0.1.6`

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.6.jar`
- Version source: `gradle.properties -> mod_version=0.1.6`
- SHA256: `B9A7CFE55FF2CAE64886EB7D7562DF8225A3A3F58570DD9017980214E67E60C1`

### 0.1.5

#### Summary

`0.1.5` packages the Sophisticated Storage / Compressium display compatibility work, config-screen fit scaling, and resource-pack sound slot integration as a patch release.

#### Changes

- Fixed Compressium compressed blocks not displaying correctly on Sophisticated Storage barrel display faces
- Added optional client-side mixins for Sophisticated Storage's dynamic display renderer and baked limited-barrel model path
- Centralized Compressium tier texture lookup, fixed-display sizing, and barrel-face offset logic
- Preserved optional compatibility behavior so Arcane Beam can still load without Sophisticated Storage or Compressium installed
- Added config-screen fit scaling so the Arcane/Rail editor remains visible at larger GUI scales and smaller resolutions
- Added `Resourcepack1` and `Resourcepack2` sound slots for both Arcane and Rail
- Added direct resource-pack sound paths for `arcane_resourcepack_1.ogg`, `arcane_resourcepack_2.ogg`, `rail_resourcepack_1.ogg`, and `rail_resourcepack_2.ogg`
- Added an example resource pack zip with placeholder sound files for the new slots
- Updated CurseForge release copy for `0.1.5`

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.5.jar`
- Version source: `gradle.properties -> mod_version=0.1.5`
- SHA256: `09B4B67C7150137BAE3AE3A3ADAC20FF794E0F235640E45E60E04358DED7B85F`
- Example resource pack: `ArcaneBeam-Example-Resourcepack-Sounds.zip`
- Example resource pack SHA256: `DDB9D3DE4818822E3493A7041A961F40D3CC426C8D7DC43F8EA1BE568BEABE6D`

### 0.1.4

#### Summary

`0.1.4` focused on hardening the beam's directional render path against vault themes whose particles or collision endpoints could make the rendered tube snap toward a cardinal world direction.
It also includes a small client-side Sophisticated Storage compatibility mixin for Compressium compressed block display rendering.

#### Changes

- Fixed beams being pulled straight north in some vault themes
- Added an explicit normalized direction to `BeamTrace`
- Updated `ArcaneBeamRenderer` to rotate the beam from the resolved look direction instead of recalculating direction from `end - start`
- Projected collision endpoints onto the aim vector so block collision can shorten the beam but cannot rotate the visual axis
- Cached each caster's last valid look vector so brief invalid look-vector frames do not snap to a fixed world direction
- Added an optional Sophisticated Storage display-item mixin that manually renders Compressium block display faces from their base particle sprite plus the matching Compressium outline layer
- Added a second optional Sophisticated Storage baked-barrel-model mixin for limited barrels, which can bake item-display quads instead of using the dynamic display renderer
- Corrected Compressium barrel display sizing by applying a vanilla-style fixed block display scale
- Restored the baked Compressium overlay path that made the outline visible on limited barrels
- Merged Compressium layer PNG alpha into subdivided base-sprite quads for limited barrels, preserving the intended translucent-black outline effect without a second z-fighting overlay surface
- Pushed Compressium barrel display faces outward by a quarter pixel so they sit closer to normal block display items without protruding too far
- Cleaned up the Compressium compatibility code by centralizing shared tier/texture/offset logic and removing the failed limited-barrel dynamic overlay experiment
- Moved the shared Compressium helper out of the configured mixin package to avoid Mixin `IllegalClassLoadError` during client startup
- Updated optional mixin detection to use Mixin's bytecode provider so the Sophisticated Storage compatibility mixin is not silently skipped during early loading
- Added local compile-only Sophisticated Core/Storage jars so the optional mixin target can be validated during development without bundling those mods
- Updated CurseForge release copy for `0.1.4`

#### Release Notes

- Artifact: `build/libs/ArcaneBeam-1.18.2-0.1.4.jar`
- Version source: `gradle.properties -> mod_version=0.1.4`
- SHA256: `76A6B0C215A2EC49371211C4ADC7C9140672D32D3D38E35704BCC38A5B0AA3CC`
- Matching jar deployed to the Prism instance mod folder

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
- SHA256: `690DE5ACCB6913016C2583FAEC5EDD6EE08D9BBB7ED2B95469CD2F692A25A5A9`
- Matching jar deployed to the Prism instance mod folder
- Residual note: crouch/stand smoothing is still slightly jumpy at `5` ticks, but the user accepted leaving it for now

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
