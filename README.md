<p align="center"><img src="https://github.com/GTModpackTeam/BetterLinkPartyClaim/blob/main/src/main/resources/assets/blpc/logo.png" alt="Logo" width="128" height="128"></p>
<h1 align="center">BetterLinkPartyClaim</h1>
<h1 align="center">
    <a href="https://www.curseforge.com/minecraft/mc-mods/better-link-party-claim"><img src="https://img.shields.io/badge/Available%20for-MC%201.12.2%20-informational?style=for-the-badge" alt="Supported Versions"></a>
    <a href="https://github.com/GTModpackTeam/BetterLinkPartyClaim/blob/main/LICENSE"><img src="https://img.shields.io/github/license/GTModpackTeam/BetterLinkPartyClaim?style=for-the-badge" alt="License"></a>
    <a href="https://discord.gg/xBwHpZyZdW"><img src="https://img.shields.io/discord/945647524855812176?color=5464ec&label=Discord&style=for-the-badge" alt="Discord"></a>
    <br>
    <a href="https://www.curseforge.com/minecraft/mc-mods/better-link-party-claim"><img src="https://cf.way2muchnoise.eu/1530505.svg?badge_style=for_the_badge" alt="CurseForge"></a>
    <a href="https://modrinth.com/mod/better-link-party-claim"><img src="https://img.shields.io/modrinth/dt/better-link-party-claim?logo=modrinth&label=&suffix=%20&style=for-the-badge&color=2d2d2d&labelColor=5ca424&logoColor=1c1c1c" alt="Modrinth"></a>
    <a href="https://github.com/GTModpackTeam/BetterLinkPartyClaim/releases"><img src="https://img.shields.io/github/downloads/GTModpackTeam/BetterLinkPartyClaim/total?sort=semver&logo=github&label=&style=for-the-badge&color=2d2d2d&labelColor=545454&logoColor=FFFFFF" alt="GitHub"></a>
</h1>

## Info
1. BetterLinkPartyClaim(BLPC) is a chunk claiming mod with its own party system, with optional integration into Better Questing Unofficial's parties. Players can claim chunks, share access with party members, and optionally force-load claimed chunks.
2. Includes a full-screen claim map (ModularUI) with async chunk rendering and texture caching, plus an Addons menu for optional mod integrations.
3. **Check with [Curseforge](https://www.curseforge.com/minecraft/mc-mods/better-link-party-claim) or [Modrinth](https://modrinth.com/mod/better-link-party-claim) to see what changes have been made!!**

## Features

### Chunk Claiming
- Claim and unclaim chunks via an in-game map UI
- Force-load claimed chunks (respecting per-player limits)
- Bulk operations by dragging across chunks

### Party Integration
- Server-authoritative parties with Owner/Admin/Member roles, allies, and enemies
- Allies are visualized on the claim map
- Role-based tab UI for invited/joined party members
- Optional link to a Better Questing Unofficial party, managed from the Addons menu

### Map
- Full-screen chunk map (default keybind: `M`)
- Async chunk map rendering with texture caching for performance

### Addons Menu (optional integrations)
- A searchable "Addons" screen in the party menu gathers settings for optional mod integrations in one place
- **BetterQuesting** — link/unlink your BLPC party to a BQu party, with a shortcut to BQu's own party manager
- **JourneyMap** — shows claimed chunks as overlays directly on JourneyMap's map, merging adjacent same-owner chunks into a single labeled area

## For Developers

BLPC exposes an addon-facing API (custom party backends, party/claim lifecycle events,
Addons-menu settings panels, and more) — see [`DEVELOPER.md`](DEVELOPER.md).

## Credits

- Built on [Better Questing Unofficial](https://www.curseforge.com/minecraft/mc-mods/better-questing-unofficial) party system
- Uses [ModularUI](https://github.com/CleanroomMC/ModularUI) for in-game UI rendering
