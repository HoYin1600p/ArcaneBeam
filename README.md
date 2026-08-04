# Arcane Beam

Arcane Beam is a client-side Forge mod for Vault Hunters on Minecraft 1.18.2. It replaces the dense particle stream used by `Arcane` and `Arcane Rail` with configurable rendered beams, custom client audio, and an in-game editor.

## Requirements

- Minecraft `1.18.2`
- Forge `40.x`
- Java `17`
- Vault Hunters / `the_vault`

## What It Does

- Suppresses Vault Hunters `Arcane` and `Arcane Rail` particles.
- Renders a beam from the selected hand origin toward the player's crosshair.
- Stops the beam at block collision or configured max range.
- Preserves Arcane's held-beam behavior.
- Preserves Rail's short single-shot behavior.
- Keeps local Arcane tracking tied to the local cast key/activity state so nearby players cannot easily trigger a beam or sound from your character.
- Keeps other players' Arcane and Rail casts visible as beams through remote particle detection.
- Supports separate beam and glow color sets for Arcane and Rail.
- Supports custom Arcane and Rail sound selections.
- Suppresses the stock Vault cast sounds for Arcane and Rail when custom ArcaneBeam sounds are selected.
- Suppresses the stray `the_vault:ability_on_cooldown` sound that can occur immediately after Arcane activates.

## Config Screen

Assign the keybind in:

`Options -> Controls -> Arcane Beam -> Open Arcane Beam Config`

The keybind is unbound by default.

Arcane, Rail, Lightning Strike, Vault Altar, Storm Arrow, Smite, and Archon each have their own tab in the config screen.

While the config screen is open, Arcane Beam renders a live world-space preview beam behind the translucent UI using the currently selected settings.
The config screen suppresses vanilla, Forge, and Vault HUD overlays while it is open. Use `Collapse` to reduce the controls to a handle, or `Clean View` / `H` to hide ArcaneBeam chrome for an unobstructed preview. Clean View leaves a small `Restore Menu (H)` handle on screen so the menu can be reopened without closing the screen.
Click a color swatch or hex field to open the color picker popover above the drawer. The popover includes its own hex field for exact color edits.

### Profiles

Each tab has an independent profile list.

Profile controls are in the left side of the bottom drawer:

- Type a profile name and click `Add` to copy the current tab's settings into a new profile.
- Use the profile dropdown to switch the current tab to a saved profile.
- Profiles save independently per tab.
- Existing configs are migrated into `Default` profiles the first time the new format is loaded.

Profiles are saved in `config/ArcaneBeam.json`.
Every setting editable in the config screen is saved per profile.

## Per-Ability Options

Every option below exists separately for `Arcane` and `Rail`.

### Beam Colors

Each ability has four beam color slots.

- Click a color preview square to select a slot.
- Drag inside the color palette for live hue/saturation updates.
- Drag inside the brightness strip for live brightness updates.
- Edit the hex box directly for exact colors.

Color cycling blends smoothly through all four configured beam colors.

### Glow Colors

Each ability also has four independent glow color slots.

These control the outer glow shell separately from the beam core. They use the same editor flow as the beam color slots.

### Brightness Strip

The vertical brightness strip behaves as follows:

- center: neutral
- above center: blends toward white
- below center: blends toward black

### Intensity

Controls beam thickness.

### Opacity

Controls core beam transparency.

### Glow Radius

Controls the size of the outer visual glow shell.

This is a rendered effect, not true dynamic world lighting.

### Glow Opacity

Controls the transparency of the outer glow separately from the beam core.

If glow opacity is set to `0`, the glow pass is skipped entirely.

### Glow Rotation

Controls how fast the glow shell rotates around the beam axis.

Range:

- `0 rpm`
- `60 rpm`

### Color Shift Time

Controls how long each color is held before shifting toward the next color.

Range:

- fastest: `0.10s per color`
- slowest: `3.00s per color`

### Shader Compatibility

Enables a shader-safe glow rendering mode.

Use this when shader packs make the normal glow render look too heavy or incorrect.

### Sound

Each ability supports:

- `Default`
- `Option 1`
- `Option 2`
- `Resourcepack1`
- `Resourcepack2`

Each ability also has a `Volume` field.

Volume controls:

- direct text editing
- mouse wheel over the box: `0.01`
- `Shift + mouse wheel`: `0.10`
- range: `0.00` to `2.00`

Current behavior:

- `Arcane Default`: Vault default Arcane sound
- `Arcane Option 1`: loops `arcane_1.ogg` while Arcane is active
- `Arcane Option 2`: plays `arcane_2_startup.ogg`, then loops `arcane_2_loop.ogg` while Arcane is active
- `Arcane Resourcepack1`: loops `arcane_resourcepack_1.ogg` while Arcane is active
- `Arcane Resourcepack2`: loops `arcane_resourcepack_2.ogg` while Arcane is active
- `Rail Default`: Vault default Rail sound
- `Rail Option 1`: plays `rail_1.ogg`
- `Rail Option 2`: plays `rail_2.ogg`
- `Rail Resourcepack1`: plays `rail_resourcepack_1.ogg`
- `Rail Resourcepack2`: plays `rail_resourcepack_2.ogg`

When custom options are selected, Arcane Beam suppresses the stock Vault Arcane and Rail cast sounds so only the selected custom sound remains.

When `Default` is selected, Vault's normal Arcane or Rail cast sound is left alone.

### Resource Pack Sound Slots

The `Resourcepack1` and `Resourcepack2` sound choices are intentionally left for resource packs.

Resource packs can provide replacement `.ogg` files at:

- `assets/arcanebeam/sounds/abilities/arcane_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_resourcepack_2.ogg`
- `assets/arcanebeam/sounds/abilities/rail_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/rail_resourcepack_2.ogg`

No `sounds.json` entry is required for these slots. Arcane Beam plays the files directly by resource path.

### Fade / Grow In

Each ability can choose how the beam appears when it starts:

- `Fade In`
- `Grow In`

`Fade In` ramps opacity from `0` to the configured opacity.

`Grow In` ramps beam and glow radius from `0` to the configured size.

### Fade / Shrink Out

Each ability can choose how the beam ends:

- `Fade Out`
- `Shrink Out`

`Fade Out` reduces opacity to `0`.

`Shrink Out` reduces beam and glow radius to `0`.

### Transition Ticks

Each ability has independent `Fade In Ticks` and `Fade Out Ticks` fields.

Controls:

- direct text editing
- mouse wheel over the box: `1`
- range: `0` to `99`

`0` is valid and means no transition time.

### Start Hand

Controls which hand the beam originates from:

- `Offhand`
- `Main Hand`

This respects the player's handedness setting.

### Start Offset

Each ability has editable `X`, `Y`, and `Z` origin offsets.

- `X`: lateral offset from the selected hand side
- `Y`: vertical offset
- `Z`: forward offset along look direction

Precision:

- direct text editing
- mouse wheel over the box: `0.01`
- `Shift + mouse wheel`: `0.10`

Defaults:

- `X = 0.38`
- `Y = -0.45`
- `Z = 0.18`

### Max Range

The beam respects the configured maximum range for each ability and will also terminate early on block collision.

Default behavior in this mod:

- `Arcane`: short range
- `Rail`: long range

## Render Behavior

- Arcane uses sustained rendering while the ability is active.
- Rail fades in quickly, reaches full brightness, then fades out over its short lifetime.
- Arcane and Rail both support configurable fade/grow-in and fade/shrink-out transitions.
- Arcane starts and stops from Vault's authoritative ability synchronization; ArcaneBeam no longer polls Vault's keybindings to maintain the beam.
- The beam endpoint follows the player's crosshair ray.
- The visual ray does not snap to entity feet or auto-aim to entity centers.
- The local player's replacement is driven by Vault ability and successful-cast signals rather than particle presence.
- Remote players keep Vault's default particles unless they have an explicit player beam assignment.
- Ambiguous remote particle ownership always falls back to Vault's default particles.
- The beam core and glow shell render as 8-sided tubes for a rounder shape.
- The start point has a short crouch/stand smoothing pass to reduce vertical snapping when entering or leaving sneak.

## Config File

Settings are saved to:

`config/ArcaneBeam.json`

If the config schema changes enough to make older values unsafe, Arcane Beam may regenerate this file.

## Remote Player Beam Profiles

Optional remote-player assignments are stored in:

`config/arcanebeam/player_beams/assignments.json`

ArcaneBeam creates that folder, an empty assignment file, and a local instruction file automatically. UUID matching takes priority over the optional name fallback.

```json
{
  "players": [
    {
      "uuid": "00000000-0000-0000-0000-000000000000",
      "name": "PlayerName",
      "profile": "example_pack:blue_arcane"
    }
  ]
}
```

The referenced profile must be supplied by an enabled Minecraft resource pack at:

`assets/<namespace>/arcanebeam/beam_profiles/<profile>.json`

Example profile:

```json
{
  "arcane": {
    "colors": [3516415, 7397375, 11393254, 16777215],
    "glowColors": [3516415, 7397375, 11393254, 16777215],
    "intensity": 0.08,
    "opacity": 0.75,
    "glowRadius": 0.14,
    "glowOpacity": 0.2,
    "colorShiftTicks": 8,
    "lifetimeTicks": 3,
    "maxRange": 16,
    "shaderCompatibility": "on"
  },
  "rail": {
    "colors": [65416, 8191836, 65500, 16777215],
    "glowColors": [65416, 8191836, 65500, 16777215],
    "intensity": 0.11,
    "opacity": 0.95,
    "glowRadius": 0.18,
    "glowOpacity": 0.2,
    "lifetimeTicks": 8,
    "maxRange": 128,
    "shaderCompatibility": "on"
  }
}
```

A profile can instead preserve the player's particle-style beam while selecting a unique texture:

```json
{
  "arcaneParticle": {
    "texture": "example_pack:textures/particle/custom_arcane.png",
    "lifetimeTicks": 5,
    "size": 0.4,
    "alpha": 1.0,
    "fullbright": true
  }
}
```

`railParticle` provides the same option for Rail. The resource pack is enabled globally, but ArcaneBeam selects its named profile only for assigned players. Press `F3+T` after changing assignments or profile JSON. Missing profiles, unassigned players, and uncertain ownership retain the original Vault particles.

## Audio Assets

Current bundled sound files:

- `assets/arcanebeam/sounds/abilities/arcane_1.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_2_startup.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_2_loop.ogg`
- `assets/arcanebeam/sounds/abilities/rail_1.ogg`
- `assets/arcanebeam/sounds/abilities/rail_2.ogg`

Resource-pack extension sound file names:

- `assets/arcanebeam/sounds/abilities/arcane_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_resourcepack_2.ogg`
- `assets/arcanebeam/sounds/abilities/rail_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/rail_resourcepack_2.ogg`

## Building

```powershell
.\gradlew.bat build
```

Output jar:

`build/libs/ArcaneBeam-1.18.2-<version>.jar`

## Credits

Arcane Beam’s beam renderer and overall rendering direction were built with direct reference to `VaultLootBeams`.

Credit to:

- `JustAHuman` for `VaultLootBeams`
- the `VaultLootBeams` credited contributors listed in that project: `shiroroku (elise)` and `amo`
- the upstream `Loot Beams: Relooted!` project and its credited creators `shiroroku` and `AmoAsterVT`

Arcane Beam is not a drop-in copy of those mods, but it intentionally reuses and adapts ideas from that renderer lineage for Vault Hunters spell beams.

## License

MIT. See [LICENSE](LICENSE).
