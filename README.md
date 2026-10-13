# Arcane Beam

ArcaneBeam is a client-side visual and audio overhaul for Vault Hunters 3 on Minecraft Forge 1.18.2. It replaces selected abilities, Vault Altar effects, and the Mana Stealer chest-trap presentation with configurable custom renders and sounds while leaving damage, targeting, cooldowns, mana costs, and server behavior unchanged.

## Requirements

- Minecraft `1.18.2`
- Forge `40.1.67` or newer (first 1.18.2 build with `RenderLevelStageEvent`)
- Java `17`
- Vault Hunters / `the_vault` `3.21.6` or newer (tested with Wolds `3.21.6` and Remastered `20.0.3`)

## What It Does

- Replaces selected Vault Hunters particles and projectiles with lightweight custom rendering.
- Provides independent profiles, colors, dimensions, opacity, timing, sound, and shader-compatible settings for each supported effect.
- Includes a translucent in-world config editor with live previews and precise mouse-wheel controls.
- Supports bundled sounds and user-supplied resource-pack sound slots.
- Supports optional resource-pack profiles for assigned remote players while retaining default Vault particles for everyone else.
- Includes optional client-only update notifications that link to the official CurseForge page.
- Requires no server installation and never changes ability gameplay.

## Supported Replacements

- **Arcane:** a sustained hand-origin beam that tracks the crosshair and stops at block collision or configured range. The `Visuals: Resource Pack` mode instead keeps Vault's own Arcane particles, so a resource pack's retexture shows, and re-aims them from the configured origin to the crosshair (your own Arcane only; Arcane Prism keeps Vault's visuals).
- **Arcane Rail:** a short, long-range beam with independent visuals, transitions, sound, and crosshair aiming. Arcane and Rail each have their own on/off switch.
- **Lightning Strike:** a compact seismic-charge projectile followed by an expanding shockwave, center flash, cones, ripples, and configurable impact audio.
- **Vault Altar:** four converging corner beams, contact sparks, a rising center beam and glow, and replacement imbuement audio.
- **Storm Arrow:** a rolling target-designator projectile followed by orbital-style energy strikes and impact sparks.
- **Smite:** configurable orbital-style strike beams, impact effects, targeting circle, and activation/strike audio.
- **Archon:** short curved Whistling Bird-style missiles with configurable plume, impact, targeting, and sound settings.
- **Smite: Blast Wave:** capped volleys of curved missiles launched from the player toward affected targets, with a smooth player-following radius indicator.
- **Mana Stealer:** pale-blue and navy composite orbs move inward through the trap's full spherical radius, with a substantial player-to-trap stream while an eligible player is inside it.
- **Dungeon globe map fade:** rooms revealed by globes, objectives or commands, and rooms a teammate visited, are drawn fainter on the Vault minimap and map screen until you visit them (Globe Map page; on by default).
- **Labyrinth Breadcrumbs (off by default):** inside the underground labyrinth of the Living, Gilded and Ornate village challenge rooms and the Wolds labyrinth room, draws one line from you to the exit: straight across rooms and down the middle of halls and stairways, ending on the floor where the exit stair or ladder begins. It appears once you are inside the maze, not while you are in the exit passage. It scans the maze from the blocks you see, so it follows secret passages and mined or opened walls, and it needs a render distance of 4 chunks or more so the whole room is loaded. The route can be shown always or only while a key is held (`Show labyrinth route`, Tab by default).

## Config Screen

Assign the keybind in:

`Options -> Controls -> Arcane Beam -> Open Arcane Beam Config`

The keybind is unbound by default.

The config screen is a docked panel with a page list on one side, grouped as Beams (Arcane, Rail), Abilities (Lightning Strike, Storm Arrow, Smite, Archon, Blast Wave) and Vault (Altar Beam, Globe Map, Mana Trap, Labyrinth, Update Notices). The panel can be docked on the left or the right and swapped with the arrow button, and it can be minimized to a thin `Settings` tab. Pages that have a start position (Arcane, Rail and Blast Wave) offer an `Adjust on screen` button that opens a Start Position window.

While the config screen is open, Arcane Beam renders a live world-space preview beam behind the translucent UI using the currently selected settings.
The config screen suppresses vanilla, Forge, and Vault HUD overlays while it is open. Use `Clean View` / `H` to hide ArcaneBeam chrome for an unobstructed preview. Clean View leaves a small `Restore Menu (H)` handle on screen so the menu can be reopened without closing the screen.
Click a color swatch or hex field to open the color picker popover. The popover includes its own hex field for exact color edits.

### Profiles

Each tab has an independent profile list.

Profile controls are in the page header:

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

**Edit profiles, not the top-level sections.** The top-level ability sections (`arcane`, `rail`, `lightningStrike`, `vaultAltar`, `stormArrow`, `smite`, `archon`, `blastWave`) are only a mirror of the selected profile. The real settings live in `<ability>Profiles.<profile name>` (for example `smiteProfiles.Default`); on load the mirror is replaced by the selected profile and rewritten on the next save, so a hand edit made only in a top-level section is discarded. The log names such a section when it differs from its profile.

Hand edits are read leniently, one value at a time. Colours may be a number (including values above 2147483647 such as `4294967295`), `"#RRGGBB"`, `"#AARRGGBB"` or `"0xRRGGBB"`; booleans may be `true`/`false`, `"true"`/`"false"` or `1`/`0`; numbers may be written as strings. A value that still cannot be read falls back to the default of that one field (named in the log), and the rest of the profile loads. The file is copied to `ArcaneBeam.json.corrupt-<time>` only when its text is not valid JSON or a whole profile cannot be read. A file that is locked by another program at launch is not treated as damaged: the game runs on defaults for that session and does not overwrite your file.

`particleScale` is limited to 0.1 to 8 and `particleRotationDegrees` is wrapped into 0 to 360 when the file is loaded. Settings are written on a background thread; if the config directory is read-only, one warning is logged and the save is retried with a growing delay.

Mana Stealer has an intentionally separate hot-reloadable client config:

`config/arcanebeam-mana-stealer-client.toml`

Its visual replacement, legacy ground sigil, and player drain stream can be toggled immediately with:

- `/arcanebeam mana_stealer on|off`
- `/arcanebeam mana_stealer sigil on|off`
- `/arcanebeam mana_stealer stream on|off`

The complete client-only effect can be rehearsed without creating a trap or draining mana:

- `/arcanebeam mana_stealer preview <ticks>` (1 to 12000 ticks, which is 10 minutes)
- `/arcanebeam mana_stealer preview <ticks> <x> <y> <z>`
- `/arcanebeam mana_stealer preview stop`

See [Mana Stealer visuals](docs/MANA_STEALER_VISUALS.md) for behavior, tuning, ownership, and validation details.

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

Use `"textures": ["namespace:textures/particle/first.png", "namespace:textures/particle/second.png"]` instead of `texture` when a profile should randomly choose among several sprites for each particle.

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

## Update Notifications

Arcane Beam checks its [raw GitHub update manifest](https://raw.githubusercontent.com/HoYin1600p/ArcaneBeam/main/update.json) asynchronously and displays an update row on the main menu when a newer version is available. A clickable in-world reminder links only to the official [ArcaneBeam CurseForge page](https://www.curseforge.com/minecraft/mc-mods/arcanebeam).

Update checks are client-only, enabled by default, and show only critical updates by default. Network failures, malformed manifests, and invalid update-type settings fail closed. The notifier never downloads or installs files and does not notify a multiplayer server.

The setting is saved in `config/ArcaneBeam.json`:

```json
"updates": {
  "checkForUpdates": true,
  "updateTypes": "CRITICAL"
}
```

It can be changed immediately without restarting:

- `/arcanebeam updates`
- `/arcanebeam updates status`
- `/arcanebeam updates on`
- `/arcanebeam updates off`
- `/arcanebeam updates critical`
- `/arcanebeam updates all`

`CRITICAL` permits only manifest entries marked `[CRITICAL]`. `ALL` permits both normal and critical updates. Changing this setting immediately reapplies the filter to the already-fetched result without another network request. Normal in-world reminders are eligible every ten qualifying client launches. A critical reminder is eligible every five qualifying launches. Each JVM can advance the counter once and display at most one reminder, so reconnects, dimensions, and later server joins do not create additional reminders.

## Building

```powershell
.\gradlew.bat build
```

Output jar:

`build/libs/ArcaneBeam-1.18.2-<version>.jar`

### Vault reference jar

The build compiles against a Vault Hunters jar. Put `the_vault-1.18.2-<vault_hunters_local_version>.jar` (the version in `gradle.properties`) in `reference/`, which is not tracked by git. If it is missing, Gradle falls back to the CurseForge file named by `vault_hunters_version`, which may be a different Vault build, and prints a warning that names the source it used. Add `-PrequireVaultReference` to make a missing reference jar fail the build, for example before a release.

### Robustness

Every event handler and mixin entry point is isolated per feature: if one throws (for example after a Vault update changes something this mod reads), the error is logged once with its stack trace and only that feature is switched off for the session. Mixins are not required, so a renamed Vault target is skipped instead of stopping the game from starting, and a startup check names the Vault members that no longer match.

## Credits

Arcane Beam’s beam renderer and overall rendering direction were built with direct reference to `VaultLootBeams`.

Credit to:

- `JustAHuman` for `VaultLootBeams`
- the `VaultLootBeams` credited contributors listed in that project: `shiroroku (elise)` and `amo`
- the upstream `Loot Beams: Relooted!` project and its credited creators `shiroroku` and `AmoAsterVT`

Arcane Beam is not a drop-in copy of those mods, but it intentionally reuses and adapts ideas from that renderer lineage for Vault Hunters spell beams.

## License

MIT. See [LICENSE](LICENSE).
