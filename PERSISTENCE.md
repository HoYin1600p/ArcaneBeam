# ArcaneBeam Persistence

This file is for future maintenance work. It is meant to give enough context to continue development without rereading the entire codebase first.

## Project Identity

- Repo: `https://github.com/HoYin1600p/ArcaneBeam`
- Mod name: `Arcane Beam`
- Current version:
  - `0.1.7`
- Target:
  - Minecraft `1.18.2`
  - Forge `40.x`
  - Java `17`
  - Vault Hunters / `the_vault`
- Current package root:
  - `dev.hoyin1600p.arcanebeam`

## User Conventions

- Use the user’s GitHub name `hoyin1600p`, not `ethan`, in code, metadata, and authorship.
- If `ArcaneBeam.json` becomes obsolete due to schema changes, it is acceptable to delete/regenerate it.
- Do not touch unrelated configs or unrelated user files.

## Purpose

Arcane Beam is a client-side replacement for Vault Hunters’ `Arcane` and `Arcane Rail` visuals and optional audio.

It does **not** modify Vault ability configs or gameplay logic. It only:

- suppresses Vault particle rendering for Arcane and Rail
- renders beams instead
- provides a config UI and JSON config
- optionally overrides cast sounds client-side

## High-Level Design

### Trigger Model

The mod uses different trigger paths for the local player and remote players.

Local player:

1. Vault spawns Arcane / Arcane Rail particles.
2. `ClientLevelMixin` intercepts particle creation and suppresses the original particles.
3. `ArcaneBeamManager` confirms local ownership only near the configured beam origin.
4. Local Arcane is also gated by Vault's client key state and ability activity messages.
5. This prevents nearby players' particles from making the local player emit a beam or play custom audio.
6. Local Arcane release timing is driven by key/activity state so the beam and looped sound stop promptly.

Remote players:

1. Their Arcane / Rail particles are still captured as the remote beam signal.
2. The mod matches those particles back to the closest likely caster near that caster's projected beam origin/path.
3. This keeps other players' Arcane/Rail beams visible without letting their particles control the local player's beam state.

### Visual Aim Model

The visible beam is **not** rendered by chaining every particle position.

Instead:

- particles are only used as an activation signal
- the beam start point is computed from the selected hand and offsets
- the beam aim uses the player/caster look direction
- the endpoint follows the crosshair ray
- the ray terminates on block collision or max range
- the rendered tube axis must stay locked to the resolved look direction; block collision is only allowed to shorten the beam, not rotate it

Important:

- the beam should **not** snap to entity feet or entity centers
- the beam should visually follow the crosshair
- the beam should **not** be pulled toward a cardinal world direction by vault theme blocks, bad collision endpoints, or particle layout
- current visual collision is block-based

## Core Files

### Mod Entry

- `src/main/java/dev/hoyin1600p/arcanebeam/ArcaneBeam.java`

Main mod bootstrap.

### Client Bootstrap

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamClient.java`

Initializes client systems, keybinds, config screen access, and sound ticking.

### Beam State / Detection

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamManager.java`

This is the central logic file for:

- particle capture
- active beam tracking
- local Arcane / Rail timing
- preview beam tracing
- recent ability timestamps used for sound suppression

Key internals:

- `ARCANE = the_vault:arcane`
- `ARCANE_RAIL = the_vault:arcane_rail`
- `ActiveBeam`
- `BeamTrace`
- `BeamKind`

It also exposes:

- `getLocalActiveBeam(...)`
- `shouldSuppressAbilityCooldownSound()`
- `shouldSuppressArcaneCastSound()`
- `shouldSuppressRailCastSound()`

Important active-state timing behavior:

- fade-out / shrink-out must not start the moment a beam misses a single partial-frame refresh
- there is currently a fade-out grace window in `ArcaneBeamManager`:
  - `FADE_OUT_GRACE_TICKS = 2.0F`
- this exists because Arcane particle capture cadence is not guaranteed to refresh perfectly every tick
- removing or reducing that grace window too aggressively can reintroduce visible beam jitter
- local Arcane release should still override the grace behavior and start the configured fade/shrink-out promptly
- crouch/stand beam-origin smoothing currently uses a `5` tick pose-origin transition; it smooths only the pose/eye-height contribution so jumping and normal movement stay responsive

### Renderer

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamRenderer.java`

Responsible for the beam core and glow rendering.

Important renderer behavior:

- beam core and glow shell are currently rendered as `8`-sided tubes, not square prisms
- supports separate core and glow color sets
- supports configurable opacity/intensity/glow radius/glow opacity
- supports glow rotation
- has shader compatibility behavior
- skips glow rendering entirely if effective glow opacity is `0`

This renderer was built with direct reference to the `VaultLootBeams` rendering approach and texture usage.

### Config Model

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamConfig.java`

Stores and serializes config state.

Config path:

- `config/ArcaneBeam.json`

Contains separate settings for:

- `arcane`
- `rail`

### Config UI

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamConfigScreen.java`

Provides the in-game editor.

Important UI behavior:

- Arcane and Rail have separate tabs
- beam and glow rows each have 4 slots
- click preview square to select editable slot
- palette supports click+drag live updates
- brightness strip supports click+drag live updates
- center of brightness strip is neutral
- above center blends toward white
- below center blends toward black
- live preview beam is shown behind the translucent UI
- hand selection shares a row with shader compatibility
- XYZ boxes support mouse wheel:
  - `0.01` per wheel tick
  - `0.10` with `Shift`

### Sound Controller

- `src/main/java/dev/hoyin1600p/arcanebeam/client/ArcaneBeamSoundController.java`

Handles custom sound playback.

Important: the final working implementation does **not** rely on normal registered `SoundEvent` playback for Arcane/Rail overrides.

Instead it uses file-backed client sound instances from Arcane Beam’s own assets.

That decision was made because the standard sound registration path repeatedly failed in this environment.

### Mixins

- `src/main/java/dev/hoyin1600p/arcanebeam/mixin/AbilityActivityMessageMixin.java`
- `src/main/java/dev/hoyin1600p/arcanebeam/mixin/ClientLevelMixin.java`
- `src/main/java/dev/hoyin1600p/arcanebeam/mixin/SoundManagerMixin.java`

`AbilityActivityMessageMixin`:

- observes Vault `AbilityActivityMessage` client handling
- passes ability id and active/deactivate flag names into `ArcaneBeamManager.observeAbilityActivity(...)`
- this is part of the local Arcane release-timing fix

`ClientLevelMixin`:

- intercepts particle spawn
- suppresses Vault Arcane / Rail particles

`SoundManagerMixin`:

- suppresses stray `the_vault:ability_on_cooldown`
- suppresses Vault’s default Arcane/Rail cast sounds during local custom playback windows

## Sound System Notes

### Final Working Sound Strategy

Custom Arcane/Rail sounds are currently played via direct file-backed client sound instances.

Why:

- standard registered custom sound event playback had repeated `Missing sound for event` and resource resolution failures
- direct file-backed playback is what finally worked reliably

### Sound Files

Stored in:

- `src/main/resources/assets/arcanebeam/sounds/abilities/`

Files:

- `arcane_1.ogg`
- `arcane_2_startup.ogg`
- `arcane_2_loop.ogg`
- `rail_1.ogg`
- `rail_2.ogg`

Resource-pack extension slots use the same namespace/path and are intentionally not bundled as real sounds:

- `arcane_resourcepack_1.ogg`
- `arcane_resourcepack_2.ogg`
- `rail_resourcepack_1.ogg`
- `rail_resourcepack_2.ogg`

Resource packs should place those files under `assets/arcanebeam/sounds/abilities/`.
No `sounds.json` entry is required for these slots because ArcaneBeam plays them through direct file-backed sound instances.

### Current Mappings

Arcane:

- `default` -> Vault default sound
- `option_1` -> `arcane_1.ogg` loop while active
- `option_2` -> `arcane_2_startup.ogg` then `arcane_2_loop.ogg`
- `resourcepack_1` -> `arcane_resourcepack_1.ogg` loop while active
- `resourcepack_2` -> `arcane_resourcepack_2.ogg` loop while active

Rail:

- `default` -> Vault default sound
- `option_1` -> `rail_1.ogg`
- `option_2` -> `rail_2.ogg`
- `resourcepack_1` -> `rail_resourcepack_1.ogg`
- `resourcepack_2` -> `rail_resourcepack_2.ogg`

### Arcane Option 2 Timing

User-measured timing:

- `arcane_2_startup.ogg` = `2.433s`
- `arcane_2_loop.ogg` = `1.077s`

Startup-to-loop handoff is currently hardcoded at:

- `49` ticks

If the audio file changes, this is the first number to revisit.

### Vault Default Sound Suppression

Vault Arcane/Rail do **not** use Vault-namespaced cast sound events here. They use vanilla `SoundEvents`.

The default sounds currently suppressed are:

- Arcane -> `minecraft:block.fire.extinguish`
- Rail -> `minecraft:block.beacon.deactivate`

These are suppressed only in a narrow local timing window tied to ArcaneBeam’s beam detection so the filter is not global.

The cooldown noise also suppressed:

- `the_vault:ability_on_cooldown`

## Config Surface

Each ability currently supports:

- 4 beam colors
- 4 glow colors
- intensity
- opacity
- glow radius
- glow opacity
- glow rotation
- color shift time
- shader compatibility toggle
- sound choice
- sound volume
- fade in style
- fade in ticks
- fade out style
- fade out ticks
- start hand
- start offset X/Y/Z
- max range

### Interpretation

- `intensity` -> beam thickness
- `opacity` -> core beam transparency
- `glow radius` -> visual outer shell size
- `glow opacity` -> outer shell alpha
- `glow rotation` -> rpm around beam axis
- `color shift time` -> seconds per color
- `sound volume` -> custom sound playback volume
- `fade in style` -> `fade` or `grow`
- `fade out style` -> `fade` or `shrink`
- `fade in/out ticks` -> per-ability transition duration in ticks
- `start hand` -> `main` or `offhand`
- `X/Y/Z` -> lateral / vertical / forward origin offsets

## Known Behavior Decisions

### Beam Origin

The beam origin is configurable per ability and may be:

- `Main Hand`
- `Offhand`

It uses handedness-aware logic, not hardcoded left/right.

### Preview

While the config screen is open:

- a preview beam renders in the world
- no sound is played
- preview reflects current settings live

### Rail Fade

Rail is intentionally not just static full-alpha for its whole lifetime.

It:

- fades in quickly
- reaches full brightness
- fades out over the latter half of its lifetime

### Arcane Active Stability

Arcane now uses configurable transition styles, but while it is actively being maintained it should remain visually rigid.

Important:

- active Arcane should not wobble, breathe, or skew between particle refreshes
- if that returns, inspect `fadeOutAge(...)` and `FADE_OUT_GRACE_TICKS` in `ArcaneBeamManager`
- this was the source of the major post-`0.1.1` jitter bug

### Glow Zero Rule

If glow opacity is `0`, the glow pass should not render at all.

This exists specifically because some shader paths ignored alpha and still showed the glow shell.

## Shader History / Decisions

This area took several iterations.

### Problem

The initial lit/beacon-style path caused beam opacity to vary depending on sun direction and view direction.

### Intermediate Fix

Moved to an unlit path to stabilize opacity.

### New Problem

That unlit path looked poor under shaders.

### Current Compromise

There is now a shader compatibility mode and a separate glow opacity control.

The current renderer behavior was tuned specifically to:

- keep beam opacity stable
- avoid large shader bloom artifacts
- preserve configurable color behavior
- preserve the rounder 8-sided tube shape in both normal and shader-compatible render paths

If future shader issues appear, inspect:

- render types used in `ArcaneBeamRenderer`
- which pass is lit vs unlit
- whether the problem is core beam or glow shell

## UI History / Decisions

The config screen went through several layout fixes.

Current important design choices:

- no old “color select” buttons
- preview square itself is the selector
- beam and glow rows are separate
- `Beam` and `Glow` labels sit to the left of their rows
- slots are spaced by computed width, not fixed cramped spacing
- `CURSEFORGE_RELEASES.md` is the public release-copy file
- `VERSION_PERSISTENCE.md` is internal version memory, not release-page copy

Do not reintroduce the old selector button layout.

## README / Credits State

README has already been updated to reflect the current feature set.

Credits currently included for:

- `JustAHuman`
- `VaultLootBeams`
- `shiroroku (elise)`
- `amo`
- upstream `Loot Beams: Relooted!`
- upstream credited creators `shiroroku` and `AmoAsterVT`

If the credits section changes, keep the attribution lineage intact.

## Git / Release State

GitHub repo:

- `https://github.com/HoYin1600p/ArcaneBeam`

Published branch:

- `main`

Repo was initialized locally, then merged with an existing remote placeholder repo that only contained a `LICENSE`.

## Local Environment Paths Used During Development

Vault jar reference:

- `C:\Users\Ethan\AppData\Roaming\PrismLauncher\instances\vaultcrafters-bootstrap-1.0.0\.minecraft\mods\the_vault-1.18.2-3.21.5-remastered.jar`

Previous ArcaneBeam instance mod path:

- `C:\Users\Ethan\AppData\Roaming\PrismLauncher\instances\vaultcrafters-bootstrap-1.0.0\.minecraft\mods\ArcaneBeam-1.18.2-0.1.4.jar`

Vault ability config reference files used for context only:

- `C:\Users\Ethan\AppData\Roaming\PrismLauncher\instances\vaultcrafters-bootstrap-1.0.0\.minecraft\config\the_vault\abilities.json`
- `C:\Users\Ethan\AppData\Roaming\PrismLauncher\instances\vaultcrafters-bootstrap-1.0.0\.minecraft\config\the_vault\abilities_descriptions.json`

VaultLootBeams reference repo:

- `E:\Git Repo's\VaultLootBeams`

## What Not To Re-Debug First

If you return later, do **not** start by re-debugging these unless symptoms specifically point there:

- particle suppression logic
- basic beam visibility
- offhand/main-hand selection logic
- sound asset path registration through normal `sounds.json` events

The basic systems already work. The custom sound path intentionally bypasses the standard event route.
- the current 8-sided tube renderer also works and should be treated as the baseline shape unless the user explicitly wants another geometry

## Current Working State After 0.1.7

Last verified build:

- Version: `0.1.7`
- Built jar: `build/libs/ArcaneBeam-1.18.2-0.1.7.jar`
- Built jar SHA256: `45B7158E34679DF55B89C32BA863D55BB144B44AD1E1BA01CD0D6CAEE555B29D`
- Instance jar was not updated during this build/version-bump pass.

Sophisticated Storage limited-barrel client interaction added in `0.1.7`:

- `SophisticatedStorageLimitedBarrelClientInteractionMixin` targets `net.minecraft.client.multiplayer.MultiPlayerGameMode`
- The mixin is client-only and registered in the `client` array of `arcanebeam.mixins.json`
- It rewrites the client `BlockHitResult` for limited-barrel front-face interactions only when the player is using Minecraft's secondary-use/sneak state and both hands are empty
- It uses `LocalPlayer.isSecondaryUseActive()`, so it follows the user's configured Sneak keybind and is not hard-coded to physical Shift
- Regular front-face right-clicks remain untouched, preserving Sophisticated Storage's native slot deposit, double-right-click deposit-all, dyes, packing tape, and upgrades
- The rewritten direction is `UP` for horizontally facing barrels and `NORTH` for vertically facing barrels, matching a non-front face so Sophisticated Storage falls through to its normal GUI-open path
- `ArcaneBeamMixinPlugin` treats the mixin as optional and skips it when the exact Forge mod id `vaultadditions` is installed
- The git-ignored export package for sharing this mixin is `build/exports/sophisticatedstorage-limited-barrel-client-mixin-20260608.zip`

## Current Working State After 0.1.6

Last verified build:

- Version: `0.1.6`
- Built jar: `build/libs/ArcaneBeam-1.18.2-0.1.6.jar`
- Built jar SHA256: `B9A7CFE55FF2CAE64886EB7D7562DF8225A3A3F58570DD9017980214E67E60C1`
- Example resource pack: `ArcaneBeam-Example-Resourcepack-Sounds.zip`
- Example resource pack SHA256: `DDB9D3DE4818822E3493A7041A961F40D3CC426C8D7DC43F8EA1BE568BEABE6D`
- Instance jar was not updated during this build/version-bump pass.

User-confirmed before closing:

- local Arcane release timing is good
- crouch/stand smoothing now works
- smoothing is set to `5` ticks
- the crouch/stand transition is still a little jumpy, but close enough to leave for now

Direction hardening applied after the initial `0.1.3` build:

- `BeamTrace` now carries an explicit normalized direction
- `ArcaneBeamRenderer` rotates the tube from that direction instead of deriving direction from `end - start`
- `ArcaneBeamManager.directionalRenderEnd(...)` projects the collision/crosshair endpoint onto the look vector, so collision can shorten the beam but cannot pull its visual axis north or any other cardinal direction
- `ArcaneBeamManager` caches the last valid look vector per caster so a transient bad look vector does not fall back to a fixed world direction

Sophisticated Storage / Compressium compatibility added in the same deployed `0.1.5` artifact:

- `SophisticatedStorageDisplayItemRendererMixin` targets `net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer`
- `SophisticatedStorageBarrelBakedModelBaseMixin` targets `net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelBakedModelBase`
- `CompressiumDisplayCompat` centralizes Compressium namespace detection, tier texture lookup, and fixed-display offset/scale constants shared by both mixins
- `CompressiumDisplayCompat` must stay outside `dev.hoyin1600p.arcanebeam.mixin`; putting normal helper classes inside the configured mixin package causes Mixin `IllegalClassLoadError` when those helpers are referenced directly
- it only changes `compressium` namespace block items
- it cancels Sophisticated Storage's private `renderSingleItem(...)` for Compressium block items and manually draws the exposed display face as two textured quads
- it also intercepts the baked barrel model display-item path used by limited barrels and injects a merged Compressium face before Sophisticated's normal display-item transformations move it onto the barrel face
- Compressium display fallback applies a `0.5` fixed-display scale because Compressium's Forge multi-layer parent does not reliably expose the normal vanilla block item fixed transform
- the base quad uses the Compressium item model's particle sprite, which is the original block texture
- the dynamic renderer overlay uses `compressium:block/layer_1` through `compressium:block/layer_9`, parsed from the item registry name's final tier suffix
- in the baked limited-barrel model path, the Compressium layer PNG alpha is merged into subdivided base-sprite quads as per-pixel darkening; this preserves the intended translucent-black visual effect without a second surface that can z-fight
- Compressium barrel display faces are pushed outward by `1/64` block to align their depth with normal non-Compressium block display items
- this bypasses both the normal item renderer path and the block renderer path, because Compressium's Forge multi-layer model can fail to emit visible display geometry in Sophisticated barrel display rendering
- it logs each Compressium item variant once when the fallback handles it, so future runtime debugging can tell whether the Sophisticated Storage hook is being reached
- it still returns a stable full-cube display offset for Compressium block items when Sophisticated Storage asks for one
- `ArcaneBeamMixinPlugin` uses Mixin's bytecode provider to detect the Sophisticated Storage target class and logs whether the optional mixin is applied or skipped
- `build.gradle` uses local `compileOnly` Sophisticated Core/Storage jars from the Prism instance; these are compile-time validation inputs only and are not bundled

Config profiles added in `0.1.6`:

- Arcane and Rail each maintain independent profile maps in `config/ArcaneBeam.json`
- profile sections are stored as `arcaneProfiles` and `railProfiles`
- selected profile names are stored as `selectedArcaneProfile` and `selectedRailProfile`
- old configs without profile maps migrate the current `arcane` and `rail` settings into `Default` profiles
- profile edits save live because `ArcaneBeamConfig.INSTANCE.arcane` and `.rail` are references to the selected profile entries
- adding a profile copies the current category settings, creates a unique name if needed, and selects the new profile
- the profile panel is dynamically positioned just left of the centered palette using `profilePanelX()`
- shader compatibility is now stored on `BeamSettings` so it is profile-specific like the rest of the config-screen settings
- `ArcaneBeamConfig.INSTANCE.shaderCompatibility` remains as a legacy migration field for older JSON files

Vault Additions coexistence guard added in `0.1.6`:

- `ArcaneBeamMixinPlugin` checks the exact Forge mod id `vaultadditions`
- if `vaultadditions` is present, ArcaneBeam skips only `SophisticatedStorageDisplayItemRendererMixin` and `SophisticatedStorageBarrelBakedModelBaseMixin`
- if `vaultadditions` is absent, ArcaneBeam applies those mixins exactly as before when Sophisticated Storage targets are present
- if Forge's loading mod list cannot be queried, the guard fails open and ArcaneBeam applies those mixins as before
- both Sophisticated Storage compatibility mixins use priority `900`, so a default-priority `1000` mixin from another mod takes precedence if both are ever active

Config screen fit-scaling added in the same deployed `0.1.5` artifact:

- `ArcaneBeamConfigScreen` now computes a virtual layout size and scale factor from the current screen dimensions
- the screen background still fills the real viewport, while UI widgets render and receive mouse input through the virtual layout transform
- this is intended to keep the full Arcane/Rail config UI visible at larger Minecraft GUI scales and smaller monitor resolutions

Resource-pack sound slots added in the same deployed `0.1.5` artifact:

- `ArcaneBeamConfig.SoundChoice` now includes `Resourcepack1` and `Resourcepack2`
- Arcane resource-pack slots loop `assets/arcanebeam/sounds/abilities/arcane_resourcepack_1.ogg` and `arcane_resourcepack_2.ogg`
- Rail resource-pack slots play `assets/arcanebeam/sounds/abilities/rail_resourcepack_1.ogg` and `rail_resourcepack_2.ogg`
- the example pack `ArcaneBeam-Example-Resourcepack-Sounds.zip` includes `pack.mcmeta`, a README, and zero-byte placeholder `.ogg` files at all four paths
- the resource-pack slots still use direct file-backed playback; do not switch this to registered `sounds.json` events unless the direct playback strategy is intentionally replaced

Do not restart the crouch smoothing investigation unless the user asks. If revisiting it, start from:

- `ArcaneBeamManager.visualBeamStart(...)`
- `ArcaneBeamManager.smoothPoseStart(...)`
- `ORIGIN_VERTICAL_SMOOTHING_TICKS = 5.0D`
- `poseContributionY(...)`

The important design constraint is that crouch/stand should smooth, but jumping and normal movement should remain responsive.

## Best Next Debug Entry Points

If something breaks in the future, start here:

1. `ArcaneBeamManager`
   - activation timing
   - beam lifetime
   - preview state
   - sound suppression windows
2. `ArcaneBeamRenderer`
   - shader issues
   - color issues
   - opacity issues
   - glow/core render behavior
3. `ArcaneBeamConfigScreen`
   - layout
   - input behavior
   - slider behavior
4. `ArcaneBeamSoundController`
   - wrong sound choice
   - startup/loop handoff
   - missing playback
5. `SoundManagerMixin`
   - default Vault sound leaks

## Recommended Maintenance Approach

Before making future changes:

1. Read this file.
2. Read:
   - `ArcaneBeamManager.java`
   - `ArcaneBeamRenderer.java`
   - `ArcaneBeamConfigScreen.java`
   - `ArcaneBeamSoundController.java`
3. Build with:
   - `.\gradlew.bat build`
4. If deploying to the user’s Prism instance, verify the copied jar hash matches the built jar hash.
