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
1. BetterLinkPartyClaim(BLPC) is a chunk claiming mod integrated with the Better Questing Unofficial party system. Players can claim chunks, share access with party members, and optionally force-load claimed chunks.
2. Includes a full-screen claim map (ModularUI) and a small minimap HUD with async chunk rendering and texture caching.
3. **Check with [Curseforge](https://www.curseforge.com/minecraft/mc-mods/better-link-party-claim) or [Modrinth](https://modrinth.com/mod/better-link-party-claim) to see what changes have been made!!**

## Features

### Chunk Claiming
- Claim and unclaim chunks via an in-game map UI
- Force-load claimed chunks (respecting per-player limits)
- Bulk operations by dragging across chunks

### Party Integration
- Members of the same Better Questing Unofficial party are treated as allies
- Allies are visualized on the claim map
- Role-based tab UI for invited/joined party members

### Map & HUD
- Full-screen chunk map (default keybind: `M`)
- Client-side minimap HUD showing nearby chunks and claims (default keybind: `N`)
- Async chunk map rendering with texture caching for performance

### JourneyMap Integration (optional)
- Optional integration with JourneyMap when present

## Credits

- Built on [Better Questing Unofficial](https://www.curseforge.com/minecraft/mc-mods/better-questing-unofficial) party system
- Uses [ModularUI](https://github.com/CleanroomMC/ModularUI) for in-game UI rendering
