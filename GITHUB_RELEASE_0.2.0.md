# ArcaneBeam 0.2.0 - Major Visual Update

ArcaneBeam `0.2.0` is the largest update to the mod so far. It expands the client-side visual overhaul with a new Blast Wave replacement, a redesigned live config editor, more reliable Arcane and Rail handling, optional remote-player particle profiles, smoother animation, and built-in update notifications.

ArcaneBeam changes presentation only. Vault Hunters damage, targeting, radius, cooldowns, mana costs, and server behavior remain unchanged.

## Highlights

### Blast Wave

- Capped volleys of curved Whistling Bird-style missiles launch from the player toward affected targets.
- Missile count, scale, path, origin, lifetime, colors, opacity, impacts, and sounds are configurable.
- A smooth player-following circle can display the ability radius and pulse when the damage wave activates.
- The config screen includes a live missile-origin preview.

### Config Editor

- New themed ability tabs and horizontally scrolling control cards.
- Live world-space previews, color editing, independent profiles, Collapse, and Clean View.
- Mouse-wheel value controls no longer accidentally scroll the page beneath them.
- Arcane and Rail now have independent replacement toggles matching the other ability tabs.

### Arcane and Rail

- Arcane activation now follows Vault Hunters' synchronized ability state instead of continuous key polling.
- Particle and default-sound suppression is more reliable under multiplayer latency.
- Optional Crosshair Aim points the beam at the block or entity under the crosshair.
- Rail Crosshair Aim is corrected and enabled in its default profile.
- Beam color transitions and glow rotation are smooth again.

### Player Resource-Pack Profiles

- Optional UUID/name assignments can select a resource-pack beam or particle profile for nearby players.
- Profiles support rendered beams, one custom particle texture, or randomized particle texture lists.
- Unassigned players and uncertain particle ownership retain normal Vault Hunters particles.
- No personal assignments or player-specific texture packs are included.

See [README.md](README.md#remote-player-beam-profiles) for the assignment and resource-pack format.

### Maintenance

- Added optional client-only update notifications that link only to the official CurseForge project.
- Smoothed player-following circles for Blast Wave, Smite, and Storm Arrow.
- Improved config input handling and custom sound cleanup.
- Moved Sophisticated Storage compatibility fixes into a separate standalone mod.

## Requirements

- Minecraft `1.18.2`
- Forge `40.x`
- Vault Hunters 3 / `the_vault`
- Client installation only

Other players and the multiplayer server do not need ArcaneBeam installed.

## Download

Use the attached `ArcaneBeam-1.18.2-0.2.0.jar` or install ArcaneBeam from the [official CurseForge page](https://www.curseforge.com/minecraft/mc-mods/arcanebeam).

SHA-256:

`5E1A7CA906FB6DB0F081449AE449424E32151EB004B694BE7970A8A812157D13`
