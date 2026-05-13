# Changelog

All notable changes to BetterLinkPartyClaim (BLPC) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [0.10.0]

> **Wire-protocol break.** The server-to-client notification packets were
> merged into one (see *Changed* below). Client and server must run the same
> version — a mixed pair will not communicate notifications correctly.

### Changed

- **S→C notification packets unified**: `MessageChunkTransitNotify`,
  `MessagePartyEventNotify`, and `MessageClaimFailed` are now a single
  discriminator-multiplexed packet, `MessageClientNotify` (`int kind` +
  per-kind payload). Wire IDs 6–8 collapsed into ID 6; new toast types are
  added by appending a `KIND_*`/`EVENT_*` constant instead of a new wire ID.
- **Live-update party UI**: the party panels (main menu, members, moderators,
  settings, create/join, transfer ownership) now stay open and refresh in
  place when server data changes, instead of closing on every sync. The view
  closes only on a structural change — the party disappearing, a permission
  boundary flipping, or ownership being lost.
- **Free-to-join / invite flow**:
  - Clicking a party in the create/join screen now opens the party menu
    directly once the join succeeds, instead of just dismissing the screen.
  - Full free-to-join parties are listed but shown grayed and non-clickable
    rather than hidden, so a previously-available party doesn't silently vanish.
- **Documentation**: `CLAUDE.md`, the architecture skill, and the code-review
  agent updated to describe the discriminator-multiplexed packets, the
  live-update widget patterns, and the shared `PartyWidgets` helpers.

### Fixed

- **Stale party data in open panels**: after a disband, ownership transfer, or
  kick performed by another player, an open party panel could keep showing the
  old state (and, for the main menu, leave a stale "open sub-panel" button
  behind). Panels now refresh or close correctly on every sync.
- **Stale values in the Settings panel**: every server sync replaces the
  cached party object, so the panel could display an out-of-date name, color,
  member count, or toggle state. All Settings widgets now read the live party.
- **Silent join failures**: trying to join a party that was disbanded, is no
  longer free-to-join, or whose invite expired now shows a "Could not join the
  party" toast instead of doing nothing visible. (Capacity failures already
  showed "Party is full".)
- **Self-notification toasts**: the player who joins a party no longer receives
  a "you joined" toast for themselves, and the player who disbands a party no
  longer receives a "party disbanded" toast.
- **Optimistic-UI desync on rejected actions**: when the server rejects a party
  action (e.g. a stale click), the rejecting player's client now receives a
  corrective sync so the UI no longer shows a change that didn't take effect.
- **Moderators panel after promotion**: a viewer who is promoted to OWNER while
  the panel is open now gets the role-cycle controls without having to reopen.
- **Memory leaks**: orphaned sub-panel handlers were accumulating each time the
  main menu rebuilt (they are now created once and reused), and empty
  invader-tracking sets were left behind on player logout.

[0.10.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.10.0

* * *

## [0.9.0]

### Changed

- **Network layer refactor**: split the network handlers along the side
  boundary. `IMessage` classes stay in `common/network/` and must not
  reference any `@SideOnly` types in their bytecode; client-bound (S→C)
  handlers now live in `client/network/` and are gated with
  `@SideOnly(Side.CLIENT)`. Server-side registration uses a no-op handler
  so the dedicated server never class-loads client code.
- **`MessagePartyAction` dispatcher extracted**: the 22 action arms moved
  from the inner `Handler` into `common/network/party/PartyActionDispatcher`,
  one private method per action. Wire-protocol IDs and `ACTION_*` constants
  are unchanged.
- **`ModNetwork` registration**: client-bound messages are listed once in
  `CLIENT_BOUND_MESSAGES` and installed via `ClientPacketHandlers.installAll()`
  on the client. Wire IDs (0–8) are preserved.

### Fixed

- **Dedicated-server crash on party creation**:
  `NoSuchMethodError: EnumDyeColor.func_193350_e()` — `EnumDyeColor.getColorValue()`
  is `@SideOnly(Side.CLIENT)` in vanilla 1.12.2 and is not present on the
  dedicated server. The default party color is now stored as the inlined
  RGB constant instead.

[0.9.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.9.0

* * *

## [0.8.0]

Initial release.

### Added

- **Chunk claiming**: claim, unclaim, and force-load chunks via a full-screen
  ModularUI map (`M` key) and a client-side minimap HUD (`N` key to toggle).
  Bulk operations are supported by drag selection, and dedicated buttons allow
  unclaiming or unloading every chunk owned by the player at once.
- **Server-authoritative party system**: parties are persisted under
  `world/betterlink/pc/parties/<id>.dat` with three roles (`OWNER`, `ADMIN`,
  `MEMBER`) and a configurable member cap.
- **Trust levels** (`NONE`, `ALLY`, `MEMBER`, `MODERATOR`, `OWNER`) per
  protected action: block edit, block interaction, attacking entities, and item
  use. A separate trust level controls fake-player automation mods such as
  BuildCraft and EnderIO.
- **Allies and enemies**: party-versus-party relations replace the previous
  per-player allowlists. Enemies are denied protection access regardless of
  trust level.
- **Explosion protection toggle** for claimed chunks (per party).
- **Free-to-join parties** with optional invitation flow, configurable
  description, color, and display name.
- **Modular party manager UI** with tabbed panels for party info, protection,
  allies, enemies, members, and invitations. Players and parties are filterable
  via a search field, and all rows expose tooltips for available actions.
- **Toast notifications** for party events (member joined/left, kicked,
  disbanded, ownership transferred, role changed, BQu link/unlink, party full)
  and a dedicated stream for claim-limit failures.
- **Transit notifications** announcing when a member returns home, an ally
  visits, or an enemy enters or leaves a claimed area.
- **BetterQuesting integration** (optional): an opt-in switch links a player's
  BLPC party to a BQu party. The active party provider is selected per player,
  so non-linked players never modify BQu state.
- **Chunk map rendering**: async terrain colorization with a `DynamicTexture`
  cache; player position, claim ownership, and party color overlays are drawn
  on top of the rendered map.
- **Chat commands** (Forge `CommandTreeBase` rooted at `/blpc`):
  - Public (level 0): `list`, `info <party>`, `me`, `here`, `claims`,
    `invites`, `accept <party>`, `decline <party>`, `leave`.
  - Operator only (level 3, under `/blpc admin`): `move-owner <party> <player>`,
    `kick <party> <player>`, `disband <party>`.
  - All commands provide tab completion for parties, members, and pending
    invitations as appropriate.
- **Localization**: full English (`en_us`) and Japanese (`ja_jp`) translations.
- **Persistence**: party data, BQu link state, and chunk claims are saved via
  `BLPCSaveHandler`; a one-time migration imports legacy data from the
  pre-FTB-Lib layout.

### Compatibility

- Minecraft 1.12.2, Forge.
- Required: ModularUI 3.1.5+.
- Optional: BetterQuesting (for party integration), JourneyMap (for the
  minimap integration hooks).

[0.8.0]: https://github.com/gtexpert/BetterLinkPartyClaim/releases/tag/v0.8.0
