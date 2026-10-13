# ArcaneBeam

Arcane Beam is a client-side visual and audio overhaul for Vault Hunters abilities, Vault Altar effects, the Mana Stealer chest trap and the Vault map on Minecraft 1.18.2 Forge.

Arcane Beam is made by HoYin1600p.

Instead of rendering long streams of particles, Arcane Beam draws clean configurable beams that follow the player's crosshair, stop on collision, and preserve the original behavior of both abilities. Arcane remains a held beam, and Rail remains a short single-shot beam. Every replacement changes presentation only: Vault Hunters damage, targeting, cooldowns, mana costs and server behavior are unchanged.

## Features

### Ability replacements

- Arcane and Arcane Rail: particle lines replaced with rendered beams, each with its own on/off switch
- Arcane `Visuals: Resource Pack` mode: keeps Vault's own Arcane particles so a resource-pack retexture shows on your own Arcane, aimed from your configured origin to the crosshair, with particle size, face-beam and rotation options
- Lightning Strike: a seismic-charge projectile and shockwave
- Storm Arrow: a rolling target designator and orbital-style energy strikes
- Smite: orbital-style strike beams with a targeting circle
- Archon: curved Whistling Bird-style missiles
- Smite: Blast Wave: capped volleys of small Whistling Bird-style missiles with a smooth player-following radius ring
- Vault Altar imbuement: four converging corner beams and a rising center beam
- Mana Stealer trap: bounded pale-blue/navy orbs across its spherical radius and a player-to-trap mana stream while an eligible player is inside the drain radius, with a separate hot-reloadable config and `/arcanebeam mana_stealer` preview/toggle commands

### Other players

- Other players keep Vault's normal Arcane and Rail particles by default, and Arcane Prism keeps Vault's own visuals
- Optional resource-pack beam profiles: assign a nearby player a rendered beam, a particle texture or a random list of textures supplied by a resource pack (`config/arcanebeam/player_beams/assignments.json`, reload with `F3+T`). Nothing personal is bundled

### Beam settings

- Four beam colors and four glow colors per ability
- In-game color picker with live preview
- Intensity, opacity, glow radius, glow opacity, glow rotation, and color shift controls
- Configurable fade/grow-in and fade/shrink-out transitions and timing per ability
- Main hand or offhand origin selection with fine XYZ origin offsets
- Crosshair Aim per beam profile
- Shader compatibility toggle
- Profiles for every ability

### Sound

- Custom sound selection and per-ability volume for every replacement
- Arcane option support for startup + loop sound behavior
- Suppresses default Vault cast sounds when custom ArcaneBeam sounds are selected
- Bundled sounds have subtitles and fade with distance
- `Resourcepack1` / `Resourcepack2` sound slots for your own `.ogg` files

### Settings screen

- Docked panel with a page list (Beams, Abilities, Vault), scrolling sections, a left or right dock and a minimize tab
- Live preview behind the translucent screen, with Clean View (`H`)
- On-screen Start Position window for the Arcane, Rail and Blast Wave origin
- Mouse wheel adjustment on every number field
- Opens from its keybind (unbound by default) or the Mods list Config button
- Client-side config saved to `config/ArcaneBeam.json`

### Vault helpers

- Labyrinth Breadcrumbs (off by default): inside the village challenge-room labyrinths (Living, Gilded, Ornate) and the Wolds labyrinth room, a single line leads from you to the exit
- Dungeon globe map fade: rooms revealed by globes or teammates are drawn fainter on the Vault map until you visit them
- Optional update notices that link only to this page, limited to critical updates by default

## Requirements

This mod is intended for Vault Hunters 3rd Edition and requires:

- Minecraft `1.18.2`
- Forge `40.1.67` or newer
- Java `17`
- `The Vault` (Vault Hunters `3.21.6` or newer; tested with Wolds `3.21.6` and Remastered `20.0.3`)
- Client installation only; the server and other players do not need Arcane Beam

Shader packs are supported: keep `Shader compatibility` on (the default) for Arcane and Rail, and the labyrinth route switches to the beacon beam program automatically when a shader pack is active.

## Links

- [Example Resource Pack](https://github.com/HoYin1600p/ArcaneBeam/releases/download/mod/ArcaneBeam-Example-Resourcepack-Sounds.zip)
- [Source, full release notes and issue tracker](https://github.com/HoYin1600p/ArcaneBeam)

## 0.2.0

### Release Copy

```markdown
## ArcaneBeam 0.2.0

A big update since 0.1.14: a new Blast Wave replacement, resource-pack beam profiles for other players, a Resource Pack visuals mode for Arcane, a rebuilt settings screen, Labyrinth Breadcrumbs, Mana Stealer trap visuals, a globe map fade, update notices and many fixes. ArcaneBeam stays client-side only and changes presentation only: Vault Hunters damage, targeting, cooldowns, mana costs and server behavior are unchanged.

Full release notes: https://github.com/HoYin1600p/ArcaneBeam/releases/tag/v0.2.0

### New

- **Blast Wave replacement:** Smite: Blast Wave pulses become capped volleys of small Whistling Bird-style missiles, with a smooth player-following radius ring, its own sounds and profiles.
- **Beam profiles for other players (optional):** give a nearby player their own rendered beam, particle texture or random texture list from a resource pack, set in `config/arcanebeam/player_beams/assignments.json`. Nobody is assigned by default.
- **Arcane `Visuals: Resource Pack` mode:** keeps Vault's own Arcane particles so a resource-pack retexture shows on your own Arcane, aimed from your configured origin to the crosshair.
- **New settings screen:** a docked panel with a page list, scrolling sections, left or right dock, a minimize tab and an on-screen Start Position window. The Mods list Config button opens it.
- **Labyrinth Breadcrumbs** (off by default, `Vault > Labyrinth` page): inside the village challenge-room labyrinths (Living, Gilded, Ornate) and the Wolds labyrinth room, one line leads from you to the exit.
- **Mana Stealer trap visuals:** pale-blue and navy orbs through the trap's radius and a player-to-trap stream, with their own config and `/arcanebeam mana_stealer` commands.
- **Dungeon globe map fade:** rooms revealed by globes or teammates are drawn fainter until you visit them.
- **Update notices:** a startup check that links only to this CurseForge page; critical updates only by default.
- Arcane and Rail each have their own on/off switch; bundled sounds have subtitles and fade with distance.

### Fixes (bugs present in 0.1.14)

- A Vault update that renames something ArcaneBeam hooks no longer stops the game from starting; an error in one feature now switches off only that feature.
- Arcane and Rail beams show exactly the configured opacity at any length, and their color blending and glow rotation are smooth again.
- Smite, Storm Arrow, Lightning Strike and Archon replacements only touch their own bolts and effects; turning a replacement off always takes effect.
- The beam origin no longer jumps when looking straight up or down, and rendering no longer jitters far from the world origin.
- Config files that are locked or hand-edited are no longer reset to defaults; saves are safer.
- The sound warnings on every resource reload are gone, and long resource-pack sounds play to the end.

### Changes

- Requires Forge 40.1.67 or newer and Vault Hunters 3.21.6 or newer.
- Other players' Arcane and Rail are no longer replaced with your beam unless you assign them a profile, and Arcane Prism keeps Vault's own visuals.
- Bundled default profiles are all named `Default`; existing configs load without losing values.

0.1.14 has no update checking, so it will not tell you about this release in game; from 0.2.0 on, ArcaneBeam can tell you about the next one.
```

## 0.1.14

Shout out to Masuary for the new menu.

### Release Copy

```markdown
## 0.1.14

This update focuses on Arcane Beam's beam configuration and keeps the mod centered on Vault Hunters ability visual replacements. Gameplay behavior, damage, cooldowns, targeting, mana costs, and server logic are unchanged.

### Beam Configuration

- Added a new Crosshair Aim toggle for Arcane/Rail beam rendering
- When Crosshair Aim is enabled, the rendered beam points directly at the block or entity under the crosshair instead of staying locked to the player's look vector
- This helps the beam visually line up with close targets, far targets, blocks, and entities even when the beam is cut off at different distances
- Crosshair Aim is optional and defaults to off, preserving the previous beam behavior unless enabled
- Crosshair Aim is saved per beam profile

### Config Menu

- Updated the Arcane Beam config menu to the newer tabbed layout
- Improved spacing and organization for the existing ability replacement settings
- Kept existing profiles, color controls, shader compatibility options, sound options, and resource pack sound slots intact

### Mod Cleanup

- Sophisticated Storage compatibility fixes have been moved out of Arcane Beam and into a standalone mod

### Notes

- This is still a client-side visual/audio replacement mod
- Existing Arcane, Rail, Lightning Strike, Vault Altar, Storm Arrow, Smite, and Archon gameplay behavior is unchanged
```

## 0.1.11

### Release Copy

```markdown
## 0.1.11

This update improves Arcane Beam's custom sound handling and reduces the chance of Minecraft audio cutting out during longer play sessions.

### Audio Stability

- Improved cleanup for Arcane Beam custom sounds when leaving a world, disconnecting, or when Minecraft stops active sounds
- Changed Arcane Beam's bundled ability sounds to avoid unnecessary streamed audio playback
- Changed Arcane Beam's runtime custom sounds to use the lighter non-streamed sound path by default
- Improved cleanup for active Arcane, Lightning Strike, Rail, Vault Altar, Storm Arrow, Smite, and Archon replacement sounds
- Kept all existing sound options, resource pack sound slots, visual effects, and ability behavior unchanged

This is a maintenance release focused on client audio stability. Gameplay behavior, damage, cooldowns, targeting, mana costs, and server logic are unchanged.
```

## 0.1.10

### Release Copy

```markdown
## 0.1.10

This update adds several new client-side visual and sound replacements for Vault Hunters abilities and altar imbuement effects. All gameplay behavior, damage, targeting, cooldowns, mana costs, and server logic are unchanged.

### Vault Altar Imbuement

Adds a configurable Vault Altar imbuement replacement.

When an altar imbuement starts, the default particle column is replaced with four small corner beams that converge on the altar, followed by a center beam and outer glow rising from the altar surface.

Config options include:
- Enable/disable replacement
- Profiles
- Shader Compatibility
- Fullbright
- Corner beam colors
- Center beam colors
- Center glow colors
- Beam opacity, size, taper, height, and fade
- Corner beam origin height and radius for aligning the beams with ceiling builds
- Optional origin marker X renders for easier setup
- Timing controls for the corner hold, convergence, and center growth
- Sound mode and volume

Sound options:
- Default
- Altar 1
- Resourcepack1
- Resourcepack2

Resource pack sound paths:
- `assets/arcanebeam/sounds/abilities/vault_altar_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/vault_altar_resourcepack_2.ogg`

### Storm Arrow

Adds a configurable Storm Arrow replacement.

The fired arrow is replaced with a small rolling target designator sphere, and successful Storm Arrow strikes are replaced with fast orbital-style energy bolts from above. Impacts include a larger rendered spark plume without using a heavy particle system.

Config options include:
- Enable/disable replacement
- Profiles
- Shader Compatibility
- Fullbright
- Targeting circle
- Optional actual-radius display
- Circle color, opacity, and width
- Bolt color, core color, width, length, lifetime, and origin height
- Impact flash color and size
- Audio range
- Sound volume

Sound options include separate selections for:
- Strike sound
- Projectile/designator launch sound

Resource pack sound paths:
- `assets/arcanebeam/sounds/abilities/storm_arrow_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/storm_arrow_resourcepack_2.ogg`
- `assets/arcanebeam/sounds/abilities/storm_arrow_projectile_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/storm_arrow_projectile_resourcepack_2.ogg`

### Smite

Adds a configurable Smite replacement.

Smite strikes are replaced with the same style of fast orbital energy bolt used by Storm Arrow, including configurable impact flashes and spark plumes. Smite does not use a projectile replacement, but it does support a separate activation sound.

Config options include:
- Enable/disable replacement
- Profiles
- Shader Compatibility
- Fullbright
- Targeting circle
- Optional actual-radius display
- Circle color, opacity, and width
- Bolt color, core color, width, length, lifetime, and origin height
- Impact flash color and size
- Audio range
- Sound volume

Sound options include separate selections for:
- Strike sound
- Activation sound

Resource pack sound paths:
- `assets/arcanebeam/sounds/abilities/smite_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/smite_resourcepack_2.ogg`
- `assets/arcanebeam/sounds/abilities/smite_activation_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/smite_activation_resourcepack_2.ogg`

### Archon

Adds a configurable Archon replacement.

Archon is treated separately from Smite and uses a small homing missile-style visual inspired by Whistling Birds. Missiles spawn from a configurable radius around the player, curve toward their target, and use a configurable plume color, core color, impact flash, and short lifetime.

Config options include:
- Enable/disable replacement
- Profiles
- Shader Compatibility
- Fullbright
- Targeting circle
- Optional actual-radius display
- Circle color, opacity, and width
- Missile color/plume color, core color, width, length, lifetime, height, and origin radius
- Impact flash color and size
- Audio range
- Sound volume

Sound options include separate selections for:
- Strike sound, including the bundled Whistling Bird option
- Activation sound

Resource pack sound paths:
- `assets/arcanebeam/sounds/abilities/archon_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/archon_resourcepack_2.ogg`
- `assets/arcanebeam/sounds/abilities/archon_activation_resourcepack_1.ogg`
- `assets/arcanebeam/sounds/abilities/archon_activation_resourcepack_2.ogg`

### Resource Pack Example

The example Arcane Beam resource pack zip has been updated with the new Storm Arrow, Smite, Archon, and Vault Altar sound replacement paths.

No custom `sounds.json` is needed for these Arcane Beam resource pack slots. Replace the placeholder `.ogg` files with your own Ogg Vorbis files, enable the resource pack, then select Resourcepack1 or Resourcepack2 in the relevant Arcane Beam config tab.
```

## 0.1.9

### Release Copy

```markdown
## 0.1.9

- Added a configurable Vault Altar imbuement visual replacement
- Replaced the altar activation particle column with four corner beams that converge on the altar center
- Added a configurable center beam and center glow that grow upward after the corner beams converge
- Added Vault Altar profile support in the config screen, matching the Arcane, Rail, and Lightning Strike tabs
- Added Vault Altar color controls for corner beams, center beam, and center glow
- Added Vault Altar shader compatibility support
- Added configurable corner beam origin height and radius so ceiling-mounted emitters can be aligned in-world
- Added configurable timing for the corner hold, convergence, and center beam growth phases
- Added small rendered contact sparks at the corner beam impact points without using particle spam
- Added Vault Altar sound modes for Default, Altar 1, Resource Pack 1, and Resource Pack 2
- Added Vault Altar replacement sound volume control
- Included the bundled Altar 1 imbuement sound
- Updated the example resource pack zip with Vault Altar sound replacement paths
- Suppressed the default altar activation and completion sounds when a custom Vault Altar sound is selected
- Set the default Vault Altar settings to the tested in-game profile values
- Improved the Vault Altar beam path so the beam contact points trace the altar surface during convergence
```

## 0.1.8

### Release Copy

```markdown
## 0.1.8

- Added a configurable Lightning Strike visual replacement inspired by a seismic charge shockwave
- Added a new Lightning Strike tab to the config screen with profiles, shader compatibility, color controls, visual tuning, and sound controls
- Added a custom Lightning Strike projectile render styled like a small seismic charge
- Added configurable Lightning Strike shockwave elements, including the expanding ring, center sphere, upper/lower cones, star-like spots, ripple settings, opacity, size, color, and lifetime
- Added Lightning Strike sound modes for Default, Seismic Charge, Resource Pack 1, and Resource Pack 2
- Added separate Lightning Strike cast and impact sound replacement support, with volume control
- Included bundled Seismic Charge cast and impact sounds
- Set the default Lightning Strike settings to the tested `Main` profile values
- Improved Lightning Strike shader compatibility for both the shockwave and projectile render
- Improved Lightning Strike impact reliability by using the Vault impact sound as a fallback trigger when the client projectile hit hook misses
- Fixed Lightning Strike replacement effects so projectile expiration and non-target impacts do not trigger the shockwave or impact sound
- Fixed Lightning Strike color picker layout and color update behavior
- Improved Arcane activation and release handling so the custom beam and sound track the key press more reliably
- Hardened the optional client-side Sophisticated Storage limited-barrel front-face interaction helper
```

## 0.1.7

### Release Copy

```markdown
## 0.1.7

- Added a client-side Sophisticated Storage limited-barrel interaction fix
- Preserved normal front-face behavior for item deposit, double-right-click deposit-all, dyes, packing tape, and upgrades
- Added Sneak + right-click empty-hand opening from the limited-barrel front face, matching the user's configured Sneak keybind
- Skipped the new Sophisticated Storage interaction mixin when Vault Additions is installed, matching Arcane Beam's existing Vault Additions compatibility guard
```

## 0.1.6

### Release Copy

```markdown
## 0.1.6

- Added Arcane and Rail profile support in the config screen
- Added per-category profile creation and selection, with profiles saved in `config/ArcaneBeam.json`
- Migrated existing Arcane and Rail settings into `Default` profiles on first launch with the new config format
- Safely skipped Arcane Beam's Sophisticated Storage / Compressium mixins when Vault Additions is installed
```

## 0.1.5

### Release Copy

```markdown
## 0.1.5

- Fixed Compressium compressed blocks not displaying correctly on Sophisticated Storage barrel display faces
- Added optional client-side compatibility handling for Sophisticated Storage display rendering paths
- Improved Compressium block display sizing and face alignment so barrel displays match normal block display behavior more closely
- Kept the compatibility path optional, so Arcane Beam can still load without Sophisticated Storage or Compressium installed
- Improved the Arcane/Rail config screen so it scales down to fit larger GUI scales and smaller screens
- Added two resource-pack sound slots for Arcane and two resource-pack sound slots for Rail
- Included an example resource pack zip showing the sound file paths for the new slots
```

## 0.1.4

### Release Copy

```markdown
## 0.1.4

- Fixed beams being pulled straight north in some vault themes
- Hardened beam direction rendering so theme particles and collision endpoints cannot rotate the rendered beam away from the caster's aim
- Kept block collision behavior intact: beams can still stop early on blocks, but collision can only shorten the beam instead of changing its direction
- Added fallback handling for brief invalid look-vector frames so beams keep their last valid aim instead of snapping to a world direction
- Added a client-side compatibility fix for Compressium compressed blocks on Sophisticated Storage barrel display faces
- Improved the Arcane/Rail config screen so it scales down to fit larger GUI scales and smaller screens
- Added two resource-pack sound slots for Arcane and two resource-pack sound slots for Rail
- Included an example resource pack zip showing the sound file paths for the new slots
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

## Credits

Arcane Beam's renderer was built with direct reference to VaultLootBeams by JustAHuman, with additional credit to that project's listed contributors `shiroroku (elise)` and `amo`, and to the upstream Loot Beams renderer lineage.
