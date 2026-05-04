---
name: blpc-overview
description: Detailed architecture reference for the BLPC project. Injected as shared knowledge into all QA team agents.
user-invocable: false
---

# BLPC Architecture Reference

Base package: `com.github.gtexpert.blpc`.

## Build System

RetroFuturaGradle (RFG) with GTNH Buildscripts. **Do not edit `build.gradle`** (auto-generated). Mod-specific config: `buildscript.properties`. Dependencies: `dependencies.gradle`. Debug flags: `debug_bqu`, `debug_jmap`, `debug_all` in `buildscript.properties`. Spotless enforced (formatting: `spotless.importorder` local + `spotless.eclipseformat.xml` via Blowdryer).

| Dependency | Role | Required? |
|---|---|---|
| ModularUI | GUI framework | Yes |
| BetterQuesting Unofficial | Party system backend (when present) | Optional (module) |
| JourneyMap API | Overlay integration | Optional |

## Java 17 Syntax (Mandatory)

Jabel (`enableModernJavaSyntax = true`) compiles Java 17 features to JVM 8 bytecode. **目的:** NPE削減（pattern matching で安全なキャスト）とコード量削減（switch expressions で冗長な break/cast を排除）。

| Feature | Requirement | Example |
|---|---|---|
| **Switch expressions** | Always use arrow form (`->`) instead of colon+break | `case X -> { ... }` or `var x = switch(v) { case A -> 1; };` |
| **Pattern matching instanceof** | Always use instead of separate cast | `if (obj instanceof MyClass mc)` not `if (obj instanceof MyClass) { MyClass mc = (MyClass) obj; }` |
| **`var`** | Use for local variables where type is obvious from context | `var entry : map.entrySet()`, `var list = new ArrayList<>(...)` |
| **Multi-label case** | Combine related cases | `case A, B, C -> { ... }` |

Do NOT use `var` for: primitives, ambiguous types (e.g. `Collections.emptyMap()`), or fields.

## Module System

Annotation-driven module framework (same pattern as GTMoreTools/GTWoodProcessing/GTBeesMatrix):

- **`api/modules/`** — `IModule`, `TModule` (annotation), `IModuleContainer`, `ModuleContainer`, `ModuleStage`, `IModuleManager`.
- **`module/`** — `ModuleManager` (ASM scanning, dependency resolution, config-driven enable/disable), `Modules` (container + module ID constants), `BaseModule`.
- **`core/CoreModule`** — `@TModule(coreModule=true)`. Registers network packets, ForgeChunkManager callback, and default `PartyProviderRegistry`.
- **`integration/IntegrationModule`** — Parent gate for all integration submodules.
- **`integration/IntegrationSubmodule`** — Abstract base for mod-specific integrations.

Modules are discovered at FML Construction via `@TModule` annotation scanning. The `modDependencies` field gates loading on `Loader.isModLoaded()`. Module enable/disable config: `config/blpc/modules.cfg`.

## Party Provider SPI

Party management is abstracted via `IPartyProvider`, allowing transparent switching between self-managed parties and BQu's party system:

- **`api/party/IPartyProvider`** — Full interface with query methods (`areInSameParty`, `getPartyName`, `getPartyMembers`, `getRole`) and mutation methods (`createParty`, `disbandParty`, `renameParty`, `invitePlayer`, `acceptInvite`, `kickOrLeave`, `changeRole`, `syncToAll`). Most mutation methods identify the party via the acting player's UUID. Exception: `acceptInvite(player, partyId)` requires an explicit partyId since it targets a different party.
- **`api/party/PartyProviderRegistry`** — Holds the active provider.
- **`common/party/DefaultPartyProvider`** — Self-managed implementation backed by `PartyManagerData`. Registered by `CoreModule`.
- **`integration/bqu/BQPartyProvider`** — BQu implementation that directly operates on BQu's `PartyManager`, `PartyInvitations`, and `NetPartySync`, with fallback to `DefaultPartyProvider` for players not in a BQu party. When BQu is present, this **replaces** the default provider — no data duplication.

**Design principle (Approach A):** When BQu is present, BLPC integrates INTO BQu's party system. BLPC's UI sends operations that `BQPartyProvider` translates into BQu API calls. BQu's quest sharing works unchanged.

## Naming Conventions

- **Panel IDs:** `blpc.map`, `blpc.party`, `blpc.map.dialog.confirm`, `blpc.party.dialog.invite`
- **Lang keys:** `blpc.map.*` for map screen, `blpc.party.*` for party screen
- **Mod ID constants:** `api/util/Mods.Names`

## Package Layout

- **`common/party/`** — Party data: `Party`, `PartyRole`, `RelationType`, `PartyManagerData`, `DefaultPartyProvider`, `ClientPartyCache`.
- **`common/chunk/`** — Claim data: `ChunkManagerData`, `ClaimedChunkData`, `ClientCache`, `TicketManager`.
- **`common/network/`** — IMessage contracts only (no client-only references):
  - C→S: `MessageClaimChunk` (with inner `Handler`), `MessagePartyAction` (POJO; handler split out — see below).
  - S→C: `MessageSyncClaims`, `MessageSyncAllClaims`, `MessageSyncConfig`, `MessagePartySync`, `MessageChunkTransitNotify`, `MessagePartyEventNotify`, `MessageClaimFailed`. Each is a pure data container with getters; no inner `Handler`.
  - `ModNetwork` — channel registration (side-aware). `NoOpHandler` — server-side fallback so S→C discriminators stay valid for outbound sends. `PlayerLoginHandler` — login sync.
- **`common/network/party/`** — `PartyActionDispatcher` (server-side handler for `MessagePartyAction`; one private static method per action discriminator).
- **`client/network/`** — All S→C handlers (`@SideOnly(Side.CLIENT)`), one class per message: `SyncClaimsClientHandler`, `SyncAllClaimsClientHandler`, `SyncConfigClientHandler`, `PartySyncClientHandler`, `ChunkTransitNotifyClientHandler`, `PartyEventNotifyClientHandler`, `ClaimFailedClientHandler`. `ClientPacketHandlers` is a side-aware SPI installer (intentionally **not** `@SideOnly`) referenced by `ModNetwork`.
- **`client/gui/`** — ModularUI: `ChunkMapScreen`/`ChunkMapWidget`, party panels in `party/` subpackage, standalone widgets in `widget/` subpackage (`BLPCToast`), `MinimapHUD`, `KeyInputHandler` (keybind registration + input handling).
- **`client/map/`** — Async chunk rendering, texture caching, claim overlay.

## Network Layer Architecture

The network layer is split along the physical side boundary so that loading a class on the wrong side is impossible by construction:

| Package | Allowed types | Loaded on server? |
|---|---|---|
| `common/network/Message*` | IMessage POJOs only — no `@SideOnly` types in bytecode | Yes (both sides) |
| `common/network/*Handler` | Server-side IMessageHandler implementations | Yes (both sides) |
| `common/network/party/PartyActionDispatcher` | Server-side handler for the party god-message | Yes (both sides) |
| `client/network/*ClientHandler` | `@SideOnly(Side.CLIENT)` IMessageHandler implementations referencing `Minecraft`, `IToast`, `BLPCToast`, etc. | **Client only** |
| `client/network/ClientPacketHandlers` | Side-aware SPI installer; **not** `@SideOnly` | Yes (referenced from `ModNetwork`), but `installAll()` only executes on client |

**Why this matters:** `SimpleNetworkWrapper.registerMessage(handlerClass, ...)` calls `handlerClass.newInstance()`, which triggers JVM class verification. Verification loads every type referenced in the handler's method bodies (e.g. `BLPCToast` → `IToast`). If any of those types is `@SideOnly(CLIENT)`, the SideTransformer rejects them on a dedicated server and the mod crashes with `NoClassDefFoundError`. By keeping `client/network/*` out of the server's class-loading path entirely, the bug class is structurally eliminated.

`ClientPacketHandlers` uses class literals (`SomeHandler.class`) inside `installAll(channel, firstId)`. Class literals are resolved at execution time, not at verification time, so the server can safely reference `ClientPacketHandlers` itself without ever loading the handlers it points to.

### Wire protocol IDs (stable order)

| ID | Direction | Message | Handler |
|---|---|---|---|
| 0 | C→S | `MessageClaimChunk` | `MessageClaimChunk.Handler` |
| 1 | C→S | `MessagePartyAction` | `PartyActionDispatcher` |
| 2 | S→C | `MessageSyncClaims` | `SyncClaimsClientHandler` |
| 3 | S→C | `MessageSyncAllClaims` | `SyncAllClaimsClientHandler` |
| 4 | S→C | `MessageSyncConfig` | `SyncConfigClientHandler` |
| 5 | S→C | `MessagePartySync` | `PartySyncClientHandler` |
| 6 | S→C | `MessageChunkTransitNotify` | `ChunkTransitNotifyClientHandler` |
| 7 | S→C | `MessagePartyEventNotify` | `PartyEventNotifyClientHandler` |
| 8 | S→C | `MessageClaimFailed` | `ClaimFailedClientHandler` |

### Adding a new network message

- **C→S** — Define IMessage in `common/network/`, write the server handler (inner class is fine), append `INSTANCE.registerMessage(...)` in `ModNetwork.init()` before the S→C block.
- **S→C** — Define IMessage in `common/network/` with **no `@SideOnly` types** referenced (use getters, not lambdas that capture `Minecraft`). Create the client handler in `client/network/<MessageName>ClientHandler.java` with `@SideOnly(Side.CLIENT)`. Append the message class to `ModNetwork.CLIENT_BOUND_MESSAGES` **and** the handler/message pair to `ClientPacketHandlers.installAll()` in the **same order** so server-side NoOp registration and client-side real registration share the same discriminator.

### MessagePartyAction action dispatch

`MessagePartyAction` multiplexes ~22 party operations through an `int action` discriminator + `String stringArg`. The server-side `PartyActionDispatcher` has one private static method per `ACTION_*` constant. Per-request state (player, args, providers, BQu link state, deferred notifications) lives in a private `ActionContext` holder passed to each method.

**Authorization invariant:** `playerBQuLinked` and `activeProvider` are re-derived from `PartyManagerData.isBQuLinked` on every request — never trusted from the client. Mutating actions go through `getAdminParty()` / `getOrCreateSelfParty()` which enforce role checks server-side.

**Adding a new action:** append a new `ACTION_*` constant to `MessagePartyAction` (do **not** renumber existing ones — wire-protocol stability), add a static factory method, add a `case` arm in `PartyActionDispatcher.dispatch()`, and implement the corresponding private method.

## Data Persistence

BLPC uses **file-based persistence** (FTB Lib style) instead of `WorldSavedData`. All data is managed by `BLPCSaveHandler.INSTANCE` and stored under `world/betterlink/pc/`:

```
world/betterlink/pc/
├── config.dat          # bquLinkedPlayers set (+ legacy migrated flag)
├── backup/
│   ├── parties/        # most recent backup of parties/
│   └── claims/         # most recent backup of claims/
├── parties/
│   ├── 0.dat           # one compressed NBT file per party (keyed by partyId)
│   └── ...
└── claims/
    ├── global.dat      # claims belonging to players with no party
    ├── 0.dat           # claims belonging to members of party 0
    └── ...
```

`BLPCSaveHandler.loadAll(server)` is called by `CoreModule.serverStarting()` (FMLServerStartingEvent). `saveAll()` is called by both `CoreEventHandler.onWorldSave()` (WorldEvent.Save) and `CoreModule.serverStopping()` (FMLServerStoppingEvent). Neither `ChunkManagerData` nor `PartyManagerData` is a `WorldSavedData` subclass — they are plain singletons reset via their `reset()` static methods. `BLPCSaveHandler` uses atomic write (`writeCompressedAtomic`) and backup-swap (`backupAndSwap`) for crash-safe persistence.

Claims: `ClaimedChunkData` includes `partyName` resolved server-side via `PartyProviderRegistry`. NBT key `"party"` for party name.

Parties (self-managed mode only): `PartyManagerData`. Not used for storage when BQu is the active backend.

## Trust Level System

Trust levels control who can interact with claimed chunks. Each party configures the minimum trust level required per action.

**TrustLevel enum** (ascending privilege):

| Value | Description |
|---|---|
| `NONE` | Outsiders with no relationship to the party |
| `ALLY` | Explicitly added to the party's ally list |
| `MEMBER` | Regular party member |
| `MODERATOR` | Maps from `PartyRole.ADMIN` |
| `OWNER` | Party creator / current owner |

**TrustAction enum** (configurable per-party):

| Action | NBT Key | Forge Events |
|---|---|---|
| `BLOCK_EDIT` | `blockEdit` | `BreakEvent`, `EntityPlaceEvent`, `FarmlandTrampleEvent` |
| `BLOCK_INTERACT` | `blockInteract` | `RightClickBlock`, `EntityInteract`, `EntityInteractSpecific` |
| `ATTACK_ENTITY` | `attackEntity` | `AttackEntityEvent` |
| `USE_ITEM` | `useItem` | `RightClickItem` |

The Settings panel cycles each action through `NONE -> ALLY -> MEMBER`. Additional per-party settings: FakePlayer trust level (same cycle), explosion protection (boolean toggle), free-to-join (boolean toggle).

## Party UI Panels

| Panel ID | File | Purpose |
|---|---|---|
| `blpc.party` | `MainPanel.java` | Party menu (uses `PartyMenuBuilder` for fluent menu composition) |
| `blpc.party.create` | `CreatePanel.java` | Create party |
| `blpc.party.settings` | `SettingsPanel.java` | Protection settings, ally/enemy management |
| `blpc.party.members` | `MembersPanel.java` | Member list |
| `blpc.party.moderators` | `ModeratorsPanel.java` | Moderator promote/demote |
| `blpc.party.dialog.disband` | MainPanel (inline `ConfirmDialog`) | Disband confirmation |
| `blpc.party.dialog.transfer` | `TransferOwnerDialog.java` | Transfer ownership |
| `blpc.party.dialog.rename` | SettingsPanel (InputDialog) | Rename party |
| `blpc.party.dialog.description` | SettingsPanel (InputDialog) | Edit party description |

Invite is handled inline in `MembersPanel` (direct `MessagePartyAction.invite()` call, no dialog). Ally/enemy management uses inline toggle buttons in SettingsPanel's trust party list (no separate dialog panels).

## Color Conventions

All GUI colors are defined as ARGB constants in `client/gui/GuiColors`:

| Constant | Value | Matches | Usage |
|---|---|---|---|
| `WHITE` | `0xFFFFFFFF` | `TextFormatting.WHITE` (§f) | Titles, default text |
| `GOLD` | `0xFFFFAA00` | `TextFormatting.GOLD` (§6) | OWNER role, section headers |
| `GREEN` | `0xFF55FF55` | `TextFormatting.GREEN` (§a) | ADMIN role, active items |
| `RED` | `0xFFFF5555` | `TextFormatting.RED` (§c) | Warnings, limit exceeded |
| `GRAY` | `0xFFAAAAAA` | `TextFormatting.GRAY` (§7) | Sub-text, messages |
| `GRAY_LIGHT` | `0xFFCCCCCC` | — | Inactive items, non-members |

`GuiColors` is at the `client/gui` package level — shared by all GUI components (party panels, chunk map, minimap). Party-specific role color logic is in `PartyWidgets.getRoleColor(PartyRole)`.

For Minecraft formatting codes in tooltip strings, use `TextFormatting` enum constants (e.g. `TextFormatting.GREEN + "text"`) instead of raw `\u00a7X` escape sequences.

## ModLog Categories

| Category | Logger | Purpose |
|---|---|---|
| `ModLog.ROOT` | `blpc` | General |
| `ModLog.IO` | `blpc/IO` | File I/O |
| `ModLog.PARTY` | `blpc/Party` | Party operations |
| `ModLog.MODULE` | `blpc/Module` | Module system |
| `ModLog.SYNC` | `blpc/Sync` | Client sync |
| `ModLog.BQU` | `blpc/BQu` | BQu integration |
| `ModLog.MIGRATION` | `blpc/Migration` | Data migration |
| `ModLog.UI` | `blpc/UI` | Panel navigation |
| `ModLog.PROTECTION` | `blpc/Protection` | Chunk protection |

## BQu Link/Unlink/Disband Flow

**Link/Unlink** — toggled via `ToggleButton` in `MainPanel` with `BoolValue.Dynamic`:
1. Client calls `PartyWidgets.setLocalBQuLinked()` for optimistic UI update + `fireSyncListeners()` for instant MainPanel rebuild.
2. Client sends `MessagePartyAction.toggleBQuLink()` to server.
3. Server verifies player is ADMIN+ and has a BQu party (for link). If rejected, `syncToAll()` is still called to roll back the optimistic update.
4. On success, updates `PartyManagerData.bquLinkedPlayers` and persists via `BLPCSaveHandler`.
5. `syncToAll()` broadcasts to all clients. Open panels close via `addSyncCloseListener`.

**Disband** (`MessagePartyAction.disband()`):
1. Server verifies player is OWNER (checks both BLPC and BQu roles when BQu-linked).
2. Releases all chunk claims, removes party from `PartyManagerData`, clears BQu link flags.
3. Persists and syncs.
4. Client calls `PartyWidgets.clearLocalPartyData()` + `displayGuiScreen(null)` to close entire GUI immediately.

## MUI Widget Patterns

| Widget | Usage | Notes |
|---|---|---|
| `CycleButtonWidget` + `IntValue.Dynamic` + `IKey.dynamic()` | Multi-state settings (trust levels) | `stateCount()` sets number of states; overlay label updates dynamically |
| `ToggleButton` + `BoolValue.Dynamic` | Boolean settings (explosions, free-to-join) | `overlay(false, ...)` / `overlay(true, ...)` for state-dependent labels |
| `ListWidget` | Scrollable lists (members, allies, enemies) | `children(iterable, mapper)` for data-driven population. Children should use `.widthRel(1f).height(h)` or `.height(h)` only — avoid fixed-pixel `.size(w, h)` as the ListWidget's `.left(n).right(n)` auto-stretches children. |
| `Dialog<T>` | Modal confirmations (disband, map bulk actions) | `closeWith(result)` triggers the result consumer and closes; extends `ModularPanel` |
| `Flow.col()` / `Flow.row()` | Automatic vertical/horizontal layout | `childPadding(n)` for spacing; eliminates manual `y += ROW_H` positioning |

For ModularUI API details, consult the ModularUI source code at `/mnt/data/git/ModularUI`. Text input fields use `setMaxLength(32)` for user-facing name inputs (party name, player name).

## Client-Side Sync Pattern

Party panels receive real-time updates via `ClientPartyCache.loadFromNBT()` (triggered by `MessagePartySync` from server). Listeners are fired **immediately** when new data arrives — no tick-based coalescing.

`ClientPartyCache.fireSyncListeners()` can also be called directly for optimistic UI updates (e.g., after `PartyWidgets.setLocalBQuLinked()` or `clearLocalPartyData()`).

**Registration pattern** — use `PartyWidgets.addSyncCloseListener(panel)`:

```java
// Registers a sync listener that closes the panel when server data changes.
// The listener is automatically removed when the panel closes.
PartyWidgets.addSyncCloseListener(panel);
```

Panels are not reopened automatically to avoid MUI handler conflicts. The user reopens via the P button, which always creates a fresh handler.

**Panels with sync listeners:**

| Panel | Helper | Behavior on sync |
|---|---|---|
| `MainPanel` | `addSyncCloseListener` | Close panel |
| `SettingsPanel` | (no sync listener) | Stateful tabbed UI — no auto-close |
| `MembersPanel` | `addSyncCloseListener` | Close panel |
| `ModeratorsPanel` | `addSyncCloseListener` | Close panel |
| `CreatePanel` | `addSyncCloseListener` | Close panel |
| `TransferOwnerDialog` | `addSyncCloseListener` | Close dialog |

**Panels without sync listeners**: `SettingsPanel` (uses `IPanelHandler` for dialogs), inline `ConfirmDialog`/`InputDialog` instances

## UI Reusable Templates

### Dialog Templates (`client/gui/party/widget/`)

- **`ConfirmDialog`** — Yes/No confirmation dialog (`Dialog<Boolean>`). Default size: `PartyWidgets.DIALOG_W x DIALOG_H` (220x70). Used by: MainPanel (disband), ChunkMapScreen.
- **`InputDialog`** — Text field + submit dialog (`Dialog<Void>`). Default size: 220x70. Used by: SettingsPanel (invite, rename, description).

All dialog templates use a consistent width of 220px. Custom sizing available via `.size(w, h)`.

### Panel Infrastructure (`client/gui/party/`)

- **`PartyWidgets`** — Central utility class consolidating size constants, layout helpers, and shared utilities:
  - Size constants: `STANDARD_W/H` (220x180), `LARGE_W/H` (260x220), `DIALOG_W/H` (220x70), `BTN_H` (18), `FACE_SIZE` (8).
  - `addHeader(panel, titleKey)` — centered title (WHITE, shadow) + close button
  - `addHeader(panel, IKey)` — IKey variant for dynamic titles
  - `addList(panel, list)` — positions list widget (top=22, padded)
- **`PartyMenuBuilder`** — Fluent builder for composing the party main menu. GregTech-style two-phase pattern:
  - `PartyMenuBuilder.of(panel, party, playerId)` creates context
  - `.nav(langKey, PanelClass::build)` — navigation entry with `Function<Party, ModularPanel>` factory (method references)
  - `.widget(widget)` — raw widget injection (toggle buttons, etc.)
  - `.tooltip(langKey)` / `.visible(predicate)` — modifiers on the current entry
  - `.buildInto(listWidget)` — materializes all entries into a `ListWidget`
  - Inner class `MenuContext` provides convenience predicates: `canInvite()`, `isOwner()`, `bquAvailable()`

**Allies/Enemies Management**: Handled directly in SettingsPanel via inline ListWidget. Uses party selection dialogs for adding allies/enemies.

### Shared Utilities

Shared color constants in `client/gui/GuiColors`:
- `WHITE`, `GOLD`, `GREEN`, `RED`, `GRAY`, `GRAY_LIGHT` — ARGB constants matching Minecraft TextFormatting palette

Shared utilities in `client/gui/party/PartyWidgets` (also consolidates former `PanelSizes` and `PanelBuilder`):
- Size constants: `STANDARD_W/H`, `LARGE_W/H`, `DIALOG_W/H`, `BTN_H`, `FACE_SIZE`
- `addHeader(panel, titleKey)` / `addHeader(panel, IKey)` — centered title + close button
- `addList(panel, list)` — positions list widget (top=22, padded)
- `getDisplayName(UUID)` — resolve player UUID to display name
- `getRoleColor(PartyRole)` — ARGB color for party role (OWNER=gold, ADMIN=green, default=white)
- `addSyncCloseListener(ModularPanel panel)` — register sync listener that closes panel on data change (auto-removed on close)
- `setLocalBQuLinked(boolean linked)` — optimistic BQu link flag update for current player
- `clearLocalPartyData()` — optimistic cache clear after disband
- `createActionButton(IKey label, String actionName, Runnable action)` — button with click handler
- `createPlayerRow(UUID uuid, String label, int color)` — standard player-row button with face icon
- `createEnterSubmitTextField(Runnable onSubmit)` — text field that submits on Enter key press
- `formatMemberLabel(String name, PartyRole role)` — format "Name [Role]" label
- `wrapWithSearchBox(ListWidget, List<IWidget>, List<String>)` — search-filterable list wrapper

## Commands

`/blpc move-owner <partyId> <newOwner>` — Op-only (permission level 3) command to transfer party ownership. Registered in `CoreModule` via `FMLServerStartingEvent`. Lang key: `command.blpc.move_owner.success`.

## Mixins

Uses MixinBooter (`ILateMixinLoader`) for conditional late-stage injection:

- **`BLPCMixinLoader`** — Loads mixin configs conditionally based on mod presence.
- **`PartyManagerMixin`** — Injects into BQu's `NetPartyAction.deleteParty()` to auto-unlink all affected players from BQu in BLPC's `PartyManagerData`. Prevents orphaned BQu links.

Config: `src/main/resources/mixins.blpc.betterquesting.json`.

## Server Configuration (ModConfig)

Forge `@Config` at `common/ModConfig.java`. Auto-syncs when changed in-game.

### Configurable (exposed in cfg file)

Uses nested subcategories via `@Config.LangKey` (`config.blpc.<category>`). Access pattern: `ModConfig.claims.maxClaimsPerPlayer`.

**Claims** (`ModConfig.claims`)

| Option | Type | Default | Description |
|---|---|---|---|
| `maxClaimsPerPlayer` | int (0–10000) | 1000 | Max chunks claimable per player |
| `maxForceLoadsPerPlayer` | int (0–10000) | 64 | Max force-loaded chunks per player |
| `additiveLimits` | boolean | true | Party claim limit = sum of each member's individual limit |
| `allowOfflineChunkLoading` | boolean | true | Keep force-loaded chunks active when all party members are offline |

**Party** (`ModConfig.party`)

| Option | Type | Default | Description |
|---|---|---|---|
| `autoCreatePartySingleplayer` | boolean | false | Auto-create party in singleplayer |

**Server Party** (`ModConfig.serverParty`)

| Option | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | false | Automatically create a shared party on server start |
| `name` | String | "Server" | Name for the auto-created server party |
| `freeToJoin` | boolean | true | Enable free-to-join on the server party |
| `owner` | String | "" | Player name who owns the server party; empty = server-owned |
| `moderators` | String[] | [] | Player names to assign as moderators (ADMIN role) |

**Data** (`ModConfig.data`)

| Option | Type | Default | Description |
|---|---|---|---|
| `mergeOfflineOnlineData` | boolean | true | Merge offline/online chunk data  |

### Internal defaults (`ModConfig.Defaults` inner class — not in cfg)

| Constant | Value | Description |
|---|---|---|
| `showMinimap` | true | Minimap HUD default visibility (toggled at runtime via keybind) |
| `enableProtection` | true | Master protection toggle |
| `protectMobGriefing` | true | Prevent mob griefing in claims |
| `protectFireSpread` | true | Prevent fire spread in claims |
| `protectFluidFlow` | true | Prevent fluid flow into claims |
| `enableTransitNotify` | true | Toast notifications for chunk entry/exit |
| `transitToastDuration` | 3000 | Toast display duration (ms) |
| `enableAreaEffects` | true | Potion effects for enemies/defenders |
| `enemyWeaknessAmplifier` | 0 | Weakness amplifier (0 = level I) |
| `enemyMiningFatigue` | true | Mining fatigue for enemies |
| `defenderResistanceAmplifier` | 0 | Resistance amplifier (0 = level I) |

## Chunk Transit System

Players receive **toast notifications** when entering/leaving claimed chunks, and **potion effects** are applied based on relationship.

### Classes

- **`common/party/RelationType`** — Enum: `MEMBER`, `ALLY`, `ENEMY`, `NONE`.
- **`core/ChunkTransitHandler`** — `PlayerTickEvent.END` listener. Detects chunk boundary crossings (overworld only), sends notifications via `MessageChunkTransitNotify`, and applies area effects.
- **`common/network/MessageChunkTransitNotify`** — S→C packet. Serializes relation as `name()` string (not ordinal) for forward compatibility. Handler: `client/network/ChunkTransitNotifyClientHandler`.
- **`client/gui/widget/BLPCToast`** — `IToast` implementation with Builder pattern. Factory methods: `fromTransit()` (chunk entry/exit), `fromPartyEvent()` (party events), `fromClaimFailed()` (claim limit errors). Only loaded on the physical client — never reachable from server-side bytecode.
- **`common/network/MessagePartyEventNotify`** — S→C packet for party events (join, leave, kick, disband, invite, transfer, role change, BQu link/unlink). Handler: `client/network/PartyEventNotifyClientHandler`.
- **`common/network/MessageClaimFailed`** — S→C packet for claim/force-load limit errors. Handler: `client/network/ClaimFailedClientHandler`.

### Notification Messages

| Relation | Enter | Leave |
|----------|-------|-------|
| MEMBER | `blpc.transit.member.enter` — "%s returned home" | `blpc.transit.member.leave` — "%s went exploring" |
| ALLY | `blpc.transit.ally.enter` — "%s came to visit" | `blpc.transit.ally.leave` — "%s went home" |
| ENEMY | `blpc.transit.enemy.enter` — "Invaded by %s" | `blpc.transit.enemy.leave` — "%s fled" |

Notifications are sent to all online party members of the claim owner. Enemies also receive their own notification.

### Area Effects

Applied every 20 ticks while player is in a claimed chunk:

- **Enemy debuff**: Weakness + optional Mining Fatigue. Removed immediately on leaving.
- **Defender buff**: Resistance + Strength. Only active while enemies are invading the party's territory. Expires naturally when all enemies leave.

`activeInvasions` map tracks which parties have enemy invaders. Cleaned up on player logout and enemy departure.

## Localization

Lang files in `src/main/resources/assets/blpc/lang/`: `en_us.lang` and `ja_jp.lang`. Both cover keybindings, commands, map UI, party UI, roles, trust actions/levels, protection settings, allies/enemies, tooltips, search, transit notifications (`blpc.transit.*`), and party event/claim failure notifications (`blpc.toast.*`).

## Adding a New Integration Module

1. Create `integration/<modid>/` package.
2. Create a module class extending `IntegrationSubmodule` with `@TModule(modDependencies=Mods.Names.THE_MOD)`.
3. Add module ID constant to `Modules.java`.
4. Add mod ID to `Mods` enum and `Mods.Names`.
