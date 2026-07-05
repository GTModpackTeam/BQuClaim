# Changelog

All notable changes to BetterLinkPartyClaim (BLPC) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [0.13.0]

### Added

- **Addons menu**
  - A new "Addons" entry in the party menu gathers the settings for optional mod integrations (BetterQuesting, JourneyMap) in one place.
  - The list is searchable, matching the Members, Moderators, and Transfer Ownership screens.
- **JourneyMap claim overlays**
  - With JourneyMap installed, claimed chunks are shown directly on JourneyMap's own map instead of a separate BLPC minimap.
  - The overlay on/off toggle lives in the new Addons menu, under JourneyMap.

### Changed

- **BQu settings moved to the Addons menu**
  - The BQu Link toggle and the "Open BQu Party Manager" button have moved out of the party Settings screen and into the new Addons menu, under BetterQuesting.

### Removed

- **Minimap HUD**
  - The always-on minimap (`N` key) has been removed.
  - The full-screen chunk map (`M` key) is unaffected; JourneyMap users get claim overlays on their own map instead (see Added, above).

[0.13.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.13.0

* * *

## [0.12.0]

### Changed

- **Cleaner party UI**
  - The chunk-map theme-switch button has been removed.
  - Buttons no longer change color when you hover over them.
- **Searchable Transfer Ownership screen**
  - The Transfer Ownership screen now has a search box, matching the Members and Moderators lists, and shows a message when there is no one to transfer to.
- **Tidier claim display on JourneyMap**
  - Adjacent chunks owned by the same player now show as a single outlined area with one label, instead of a separate border and name on every chunk.

### Fixed

- **Hard-to-read party menu text**
  - Button labels now use clear, high-contrast text against the menu buttons.
  - Role names (Owner, Admin) and ally/enemy names display in bright, readable colors instead of dark, muddy ones.

[0.12.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.12.0

* * *

## [0.11.0]

### Changed

- **BQu Link now syncs the full member list**
  - Turning BQu Link ON makes all BQu party members visible in BLPC automatically.
  - Per-player opt-in is no longer required.
- **BQu party auto-created on link**
  - If no BQu party exists when BQu Link is toggled ON, one is created from the BLPC party's name, members, and roles.
  - If a BQu party already exists, any missing BLPC members are added to it.
- **Party screen stays open after BQu Link toggle**
  - Switching BQu Link ON or OFF no longer closes the party menu — the panel refreshes in place.
- **Disband only affects the BLPC party**
  - Disbanding no longer touches the BQu party.
  - Manage the BQu party through BetterQuesting's own screen.

### Fixed

- **Disband not working after re-creating a party**
  - After disbanding and creating a new party, the Disband button would not show the confirmation dialog.
- **Crash on world entry**
  - Entering a world with certain mod combinations could cause a crash.
- **BQu party appearing without linking**
  - Creating a party in BQu would make it show up in BLPC's party list even when BQu Link was OFF.

[0.11.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.11.0

* * *

## [0.10.0]

> **Wire-protocol break.** Client and server must run the same version — a mixed pair will not communicate notifications correctly.

### Changed

- **Live-update party UI**
  - Party panels now stay open and refresh in place when data changes, instead of closing on every sync.
  - Panels only close when the party is gone, permissions change, or ownership is lost.
- **Free-to-join / invite flow**
  - Joining a party from the create/join screen now opens the party menu directly.
  - Full parties are shown grayed out instead of hidden.

### Fixed

- **Stale party data in open panels**
  - After a disband, ownership transfer, or kick by another player, open panels could keep showing outdated state.
  - Panels now refresh or close correctly.
- **Stale values in the Settings panel**
  - Name, color, member count, and toggle states could show outdated values after a server sync.
  - All settings now read live data.
- **Silent join failures**
  - Trying to join a disbanded, no-longer-free, or expired-invite party now shows a toast instead of doing nothing.
- **Self-notification toasts**
  - The player who joins or disbands a party no longer receives their own toast notification.
- **UI desync on rejected actions**
  - When the server rejects a party action, the client now receives a corrective sync so the UI matches the actual state.
- **Moderators panel after promotion**
  - A player promoted to OWNER while the panel is open now sees the role-cycle controls without reopening.
- **Memory leaks**
  - Sub-panel handlers were accumulating on each menu rebuild.
  - Empty tracking sets were left behind on player logout.

[0.10.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.10.0

* * *

## [0.9.0]

### Changed

- **Network layer split by side**
  - Server-side and client-side network handlers are now separated to prevent dedicated-server class-loading issues.

### Fixed

- **Dedicated-server crash on party creation**
  - Creating a party on a dedicated server no longer crashes due to a missing client-only color method.

[0.9.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.9.0

* * *

## [0.8.0]

Initial release.

### Added

- **Chunk claiming**
  - Claim, unclaim, and force-load chunks via a full-screen map (`M` key) and a minimap HUD (`N` key to toggle).
  - Supports drag selection and bulk unclaim/unload buttons.
- **Party system**
  - Server-authoritative parties with three roles (Owner, Admin, Member) and a configurable member cap.
  - Persisted per world.
- **Trust levels**
  - Per-action trust settings (block edit, block interaction, attacking entities, item use) with levels from None to Owner.
  - A separate setting controls fake-player automation mods.
- **Allies and enemies**
  - Party-versus-party relations.
  - Allies share protection access; enemies are denied regardless of trust level.
- **Explosion protection**
  - Per-party toggle for claimed chunks.
- **Free-to-join parties**
  - Optional open-join mode with invitation flow, description, color, and display name.
- **Party manager UI**
  - Tabbed panels for party info, protection, allies, enemies, members, and invitations.
  - Searchable player/party lists with tooltips.
- **Toast notifications**
  - Party events: join, leave, kick, disband, ownership transfer, role change, BQu link/unlink, party full.
  - Claim-limit failures.
- **Transit notifications**
  - Alerts when a member returns home, an ally visits, or an enemy enters/leaves claimed territory.
- **BetterQuesting integration** (optional)
  - Opt-in switch to link a BLPC party to a BQu party.
  - Non-linked players are unaffected.
- **Chunk map rendering**
  - Async terrain colorization with player position, claim ownership, and party color overlays.
- **Chat commands**
  - Public: `/blpc list`, `info`, `me`, `here`, `claims`, `invites`, `accept`, `decline`, `leave`.
  - Operator: `/blpc admin move-owner`, `kick`, `disband`.
  - All commands support tab completion.
- **Localization**
  - English and Japanese translations.

### Compatibility

- Minecraft 1.12.2, Forge.
- Required: ModularUI 3.1.5+.
- Optional: BetterQuesting (party integration), JourneyMap (minimap integration).

[0.8.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.8.0
