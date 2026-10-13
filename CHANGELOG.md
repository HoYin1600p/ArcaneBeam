# Changelog

## 0.2.0 - 2026-10-12

Every change since 0.1.14, the last release on CurseForge. ArcaneBeam stays client-side only and changes presentation only: Vault Hunters damage, targeting, cooldowns, mana costs and server behavior are unchanged.

### New

- **Blast Wave replacement**: Smite: Blast Wave pulses become a capped volley (default 12) of small Whistling Bird-style missiles that launch from you and descend onto affected targets. Damage stays instant. Configurable count, scale, lifetime, apex height, spread, start position, colours, opacity, impact effects and profiles, an optional player-following radius ring with a hit pulse colour, and separate activation and strike sounds (Default, built-in, `Resourcepack1`, `Resourcepack2`). Player-following circles for Storm Arrow and Smite also move more smoothly.
- **Resource-pack beam profiles for other players** (optional): assign a nearby player, by UUID with an optional name fallback, in `config/arcanebeam/player_beams/assignments.json` to a profile supplied by an enabled resource pack at `assets/<namespace>/arcanebeam/beam_profiles/<profile>.json`. A profile can give that player's Arcane or Rail a rendered beam, one particle texture or a random list of textures. Unassigned players, missing profiles and any uncertain cast keep Vault's own particles; a remote cast is only claimed when its cast sound was heard at that player. Press `F3+T` to reload. Nothing personal is bundled.
- **Arcane `Visuals: Resource Pack` mode**: keeps Vault's own Arcane particles so a resource pack's retexture shows on your own Arcane, re-aimed from your configured beam origin to the crosshair, with Particle Size, Face Beam and Particle Rotation. The default stays `Mod Beam`. Rail has no such mode, and other players and Arcane Prism are not affected.
- **Arcane and Rail**: each has its own on/off switch, the beam follows Vault's synchronized ability state, Vault's own particles and (with a custom sound) cast sound are hidden reliably, and Quick-select Arcane follows the selected specialization.
- **New settings screen**: a docked panel with a page list grouped as Beams, Abilities and Vault, scrolling titled sections, a left or right dock, a minimize tab, and a Start Position window with step buttons and a drag pad for the Arcane, Rail and Blast Wave origin (typed offsets limited to 64). Mouse-wheel nudging works on every number field. The Mods list Config button opens the screen. New pages: Blast Wave, Globe Map, Mana Trap, Labyrinth and Update Notices.
- **Labyrinth Breadcrumbs** (off by default, `Vault > Labyrinth` page): inside the underground labyrinth of the Living, Gilded and Ornate village challenge rooms and the Wolds labyrinth room, one line leads from you to the exit. The maze is scanned live from the blocks you see, so secret passages and opened walls are followed. Needs a render distance of 4 chunks or more. Show it always or while a key is held (`Show labyrinth route`, Tab by default). Also drawn under shader packs.
- **Mana Stealer visuals**: bounded, quality-scaled pale-blue and navy orbs travel inward through the trap's full radius, and a player-to-trap stream is drawn while an eligible player is inside the drain radius. The flat sigil and old dust can be hidden. Own hot-reloadable config `config/arcanebeam-mana-stealer-client.toml`, the Mana Trap settings page, and `/arcanebeam mana_stealer on|off`, `sigil`, `stream` and `preview <ticks> [<x> <y> <z>]` commands.
- **Dungeon globe map fade**: rooms revealed by globes, objectives or commands, and rooms a teammate visited, are drawn fainter on the Vault minimap and map screen until you visit them, with separate Revealed and Teammate opacity settings. On by default.
- **Update notices**: a startup check of the public update list shows a main-menu row and an in-world reminder linking only to the CurseForge page. Critical updates only by default; change it on the Update Notices page or with `/arcanebeam updates critical|all|on|off|status`. Never downloads anything and does nothing on a multiplayer server.
- **Sound**: bundled ability sounds have subtitles and play positioned in mono, so they fade with distance. Resource-pack sound slots are unchanged and also available for Blast Wave.

### Fixes

All of these were present in 0.1.14.

- A Vault update that renames a class, field or method ArcaneBeam hooks could stop the game from starting. The main mixin config is now optional, the three Vault members it read directly are read by reflection, and a startup check names anything that no longer matches.
- Each feature is isolated: an exception in an event handler or mixin hook is logged once and switches off only that feature for the session.
- Arcane and Rail beams no longer brighten and dim as the hit distance changes; every beam shows exactly the configured opacity, with shader compatibility on or off.
- Arcane and Rail colour blending and glow rotation are smooth again (a steady client clock replaces game ticks).
- Smite bolts are identified from the synced ability colours and your active effect, so a recoloured Archon or Thunderstorm is no longer drawn as Smite.
- Storm Arrow leaves the Blizzard arrow and storm as Vault draws them, hides Vault's storm rain with the cloud, and plays strike sounds even when the bolt is off screen.
- Lightning Strike hides only its own bolts, briefly and near its own impact.
- Turning a replacement off takes effect even while another effect is active, and a missing resource-pack sound file keeps Vault's default sound instead of silence.
- The Arcane and Rail beam origin no longer jumps sideways when looking almost straight up or down.
- Rendering no longer jitters far from the world origin.
- Scrolling over a number field no longer scrolls the settings screen.
- A locked or non-UTF-8 config file is no longer overwritten with defaults. Wrong-typed hand edits are read field by field, zero opacities, black colours and zero offsets are kept, and saves are atomic, run in the background, retry on a read-only folder and flush on shutdown.
- The 32 sound warnings on every resource reload are gone, and a resource-pack sound longer than about 6 seconds plays to the end.

### Changes

- Requires **Forge 40.1.67 or newer** and **Vault Hunters 3.21.6 or newer** (tested with Wolds 3.21.6 and Remastered 20.0.3).
- Other players' Arcane and Rail are no longer replaced with your own beam; without a player assignment they keep Vault's particles and sound. Arcane Prism keeps Vault's own visuals.
- The bundled default profiles are all named `Default`: a fresh Arcane `Default` uses the cyan look 0.1.14 shipped as `Test`, Lightning Strike `Default` uses the old `Main` look, and Rail `Default` has Crosshair Aim on. Existing configs keep their own profiles.
- The unused Storm Arrow `segmentGap` setting and the `Phaser` sound choice were removed; existing files load normally.
- The unused `arcanebeam:` sound events and `sounds.json` were removed; sound files still play by path.
- Mod metadata names HoYin1600p as author and jar vendor, adds an issue tracker and update list address, credits Masuary for the config menu overhaul, and the jar is built reproducibly.
- `config/ArcaneBeam.json` from 0.1.14 loads without losing values; new sections are added with defaults on the next save.
