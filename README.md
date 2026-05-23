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
- Supports separate beam and glow color sets for Arcane and Rail.
- Supports custom Arcane and Rail sound selections.
- Suppresses the stock Vault cast sounds for Arcane and Rail when custom ArcaneBeam sounds are selected.
- Suppresses the stray `the_vault:ability_on_cooldown` sound that can occur immediately after Arcane activates.

## Config Screen

Assign the keybind in:

`Options -> Controls -> Arcane Beam -> Open Arcane Beam Config`

The keybind is unbound by default.

Arcane and Rail each have their own tab in the config screen.

While the config screen is open, Arcane Beam renders a live world-space preview beam behind the translucent UI using the currently selected settings.

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
- `Rail Default`: Vault default Rail sound
- `Rail Option 1`: plays `rail_1.ogg`
- `Rail Option 2`: plays `rail_2.ogg`

When custom options are selected, Arcane Beam suppresses the stock Vault Arcane and Rail cast sounds so only the selected custom sound remains.

When `Default` is selected, Vault's normal Arcane or Rail cast sound is left alone.

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
- The beam endpoint follows the player's crosshair ray.
- The visual ray does not snap to entity feet or auto-aim to entity centers.
- The local player only claims Arcane/Rail ownership near the beam origin, which prevents nearby players from falsely triggering a local beam and local audio when their beam crosses your view.

## Config File

Settings are saved to:

`config/ArcaneBeam.json`

If the config schema changes enough to make older values unsafe, Arcane Beam may regenerate this file.

## Audio Assets

Current bundled sound files:

- `assets/arcanebeam/sounds/abilities/arcane_1.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_2_startup.ogg`
- `assets/arcanebeam/sounds/abilities/arcane_2_loop.ogg`
- `assets/arcanebeam/sounds/abilities/rail_1.ogg`
- `assets/arcanebeam/sounds/abilities/rail_2.ogg`

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
