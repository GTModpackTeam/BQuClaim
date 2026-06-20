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
- **`core/CoreModule`** — `@TModule(coreModule=true)`. Registers network packets, ForgeChunkManager callback, and `DefaultPartyProvider` via `PartyProviderRegistry.register(..., PRIORITY_DEFAULT)`.
- **`integration/IntegrationModule`** — Parent gate for all integration submodules.
- **`integration/IntegrationSubmodule`** — Abstract base for mod-specific integrations.

Modules are discovered at FML Construction via `@TModule` annotation scanning. The `modDependencies` field gates loading on `Loader.isModLoaded()`. Module enable/disable config: `config/blpc/modules.cfg`.

## Party Provider SPI

Party management is abstracted via `IPartyProvider`, allowing transparent switching between self-managed parties and BQu's party system:

- **`api/party/IPartyProvider`** — Full interface with query methods (`areInSameParty`, `getPartyName`, `getPartyMembers`, `getRole`; plus `default` query methods `findByName`, `allPartyNames`, `pendingInvitesFor`) and mutation methods (`createParty`, `disbandParty`, `renameParty`, `invitePlayer`, `acceptInvite`, `kickOrLeave`, `changeRole`, `syncToAll`). Most mutation methods identify the party via the acting player's UUID. Exception: `acceptInvite(player, partyId)` requires an explicit partyId since it targets a different party. Addons should query via `api/util/PartyQueryUtil` rather than the raw interface.
- **`api/party/PartyProviderRegistry`** — Priority-based registry for the active provider. Constants: `PRIORITY_LOW=-100`, `PRIORITY_DEFAULT=0`, `PRIORITY_HIGH=100`. Higher priority wins; equal priority logs a warning and accepts the new provider (last-write-wins at tie); lower priority is silently ignored. Use `register(provider, priority)`.
- **`common/party/DefaultPartyProvider`** — Self-managed implementation backed by `PartyManagerData`. Registered by `CoreModule` at `PRIORITY_DEFAULT`.
- **`integration/bqu/BQPartyProvider`** — BQu implementation that directly operates on BQu's `PartyManager`, `PartyInvitations`, and `NetPartySync`, with fallback to `DefaultPartyProvider` for players not in a BQu party. Registered by `BQuModule` at `PRIORITY_HIGH`, replacing the default provider when BQu is present — no data duplication.

**Design principle (Approach A):** When BQu is present, BLPC integrates INTO BQu's party system. BLPC's UI sends operations that `BQPartyProvider` translates into BQu API calls. BQu's quest sharing works unchanged.

## Naming Conventions

- **Panel IDs:** `blpc.map`, `blpc.party`, `blpc.map.dialog.confirm`, `blpc.party.dialog.invite`
- **Lang keys:** `blpc.map.*` for map screen, `blpc.party.*` for party screen
- **Mod ID constants:** `api/util/Mods.Names`

## Package Layout

**Start here:** `api/BLPCAPI` is the central access point and discoverability index (GregTech `GregTechAPI` analog) — one façade documenting every subsystem and addon extension point (`partyProvider()`, `moduleManager()`, `MODID`). Read it first.

- **`api/`** — Public, addon-facing surface. `BLPCAPI` (façade/index), `modules/` (module framework SPI), `party/` (party backend SPI + domain types — `IPartyProvider`, `PartyProviderRegistry` with priority registration; **domain types**: `Party`, `PartyRole`, `TrustLevel`, `TrustAction`, `RelationType`), `event/` (`ChunkModifiedEvent`; `PartyEvent` — Pre/Post lifecycle hierarchy: cancelable `Pre.Created`/`Pre.Disbanded` veto mutations before they occur; informational `Post.Created`/`Post.Disbanded`/`Post.MemberJoined`/`Post.MemberLeft`/`Post.RoleChanged` fire after success), `util/` (`Mods`, `ModUtility`, `PartyQueryUtil` — addon-safe query façade delegating to the active `IPartyProvider`).
- **`common/party/`** — Party infrastructure: `PartyManagerData`, `DefaultPartyProvider`, `ClientPartyCache`. Domain types (`Party`, `PartyRole`, `TrustLevel`, `TrustAction`, `RelationType`) live in `api/party/`.
- **`common/chunk/`** — Claim data: `ChunkManagerData`, `ClaimedChunkData`, `ClientCache`, `TicketManager`.
- **`common/network/`** — IMessage contracts only (no client-only references):
  - C→S: `MessageClaimChunk` (with inner `Handler`), `MessagePartyAction` (POJO; handler split out — see below).
  - S→C: `MessageSyncClaims`, `MessageSyncAllClaims`, `MessageSyncConfig`, `MessagePartySync`, `MessageClientNotify`. Each is a pure data container with getters; no inner `Handler`. `MessageClientNotify` is a discriminator-multiplexed packet that carries every transient client toast (chunk transit, party event, claim limit) through a single wire ID.
  - `NbtMessage` — abstract base for messages whose entire payload is one `NBTTagCompound` (`data` field + getter + `readTag`/`writeTag`). `MessagePartySync` and `MessageSyncAllClaims` extend it; future NBT-payload messages should too.
  - `ModNetwork` — channel registration (side-aware). `NoOpHandler` — server-side fallback so S→C discriminators stay valid for outbound sends. `PlayerLoginHandler` — login sync.
- **`common/network/party/`** — `PartyActionDispatcher` (server-side handler for `MessagePartyAction`; one private static method per action discriminator; the `onAdminParty(c, Predicate<Party>)` helper wraps the ADMIN+ auth gate shared by ~8 simple settings actions).
- **`client/network/`** — All S→C handlers (`@SideOnly(Side.CLIENT)`), one class per top-level wire packet: `SyncClaimsClientHandler`, `SyncAllClaimsClientHandler`, `SyncConfigClientHandler`, `PartySyncClientHandler`, `ClientNotifyClientHandler` (dispatches by `MessageClientNotify.getKind()` to the matching `BLPCToast` builder). `ClientPacketHandlers` is a side-aware SPI installer (intentionally **not** `@SideOnly`) referenced by `ModNetwork`.
- **`client/gui/`** — ModularUI screens only. `Screens` = the single catalog of every GUI + its open/build entry points (`openMap()`, `partyMain(...)`; RecipeMaps analog); `BLPCGuiTextures` = shared reusable `IDrawable`s (`DIVIDER`, `MAP_BACKGROUND`, `MAP_BORDER`) + `ICON_*` constants that reuse ModularUI's built-in `GuiTextures` icon atlas (`CLOSE`/`REFRESH`/`REMOVE` — no custom art; chunk-map tool buttons use these). Drawables are shared instances (a `Rectangle` only reads its fields at draw time) — never inline `new Rectangle().color(...)` in screen code, add it here. `BLPCColors` = semantic party/map palette, `GuiColors` = fixed vanilla-context ARGB; `BLPCToast` = vanilla toast notification; `ChunkMapScreen`/`ChunkMapWidget`; `PlayerFaceDrawable`; party panels in `party/` subpackage; reusable widgets in `party/widget/` (`ConfirmDialog`, `InputDialog`, `LiveSearchableList`). Map pixel math derives from `ChunkMapRenderer.CHUNK_BLOCKS` (16 blocks/chunk — the single source for the recurring `% 16` / `/ 16` calculations).
- **`client/hud/`** — `MinimapHUD` (in-game `RenderGameOverlayEvent` overlay; not a ModularUI screen).
- **`client/input/`** — `KeyInputHandler` (keybind registration; routes key presses to `Screens`).
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
| 1 | C→S | `MessagePartyAction` (multiplexed) | `PartyActionDispatcher` |
| 2 | S→C | `MessageSyncClaims` | `SyncClaimsClientHandler` |
| 3 | S→C | `MessageSyncAllClaims` | `SyncAllClaimsClientHandler` |
| 4 | S→C | `MessageSyncConfig` | `SyncConfigClientHandler` |
| 5 | S→C | `MessagePartySync` | `PartySyncClientHandler` |
| 6 | S→C | `MessageClientNotify` (multiplexed) | `ClientNotifyClientHandler` |

### Discriminator-multiplexed packets (preferred for new operations)

Two packets carry their own internal discriminator so adding new operations
does not require a new top-level wire ID:

- **`MessagePartyAction`** (C→S, ID 1) — `int action` + `String stringArg`. ~22 party operations.
- **`MessageClientNotify`** (S→C, ID 6) — `int kind` + per-kind payload. Three kinds today (`KIND_CHUNK_TRANSIT`, `KIND_PARTY_EVENT`, `KIND_CLAIM_FAILED`) covering every BLPC toast.

Append-only: existing constants are part of the on-wire format. Do not renumber.

### Adding a new network message

- **New action / notification** (preferred) — append a constant to `MessagePartyAction` or `MessageClientNotify` and extend the corresponding `switch` (dispatcher / handler / `toBytes` / `fromBytes`). Neither `ModNetwork` nor `ClientPacketHandlers` changes.
- **New top-level packet** (only for genuinely new message families) —
  - **C→S** — Define IMessage in `common/network/`, write the server handler (inner class is fine), append `INSTANCE.registerMessage(...)` in `ModNetwork.init()` before the S→C block.
  - **S→C** — Define IMessage in `common/network/` with **no `@SideOnly` types** referenced (use getters, not lambdas that capture `Minecraft`). Create the client handler in `client/network/<MessageName>ClientHandler.java` with `@SideOnly(Side.CLIENT)`. Append the message class to `ModNetwork.CLIENT_BOUND_MESSAGES` **and** the handler/message pair to `ClientPacketHandlers.installAll()` in the **same order** so server-side NoOp registration and client-side real registration share the same discriminator.

### MessagePartyAction action dispatch

`MessagePartyAction` multiplexes ~22 party operations through an `int action` discriminator + `String stringArg`. The server-side `PartyActionDispatcher` has one private static method per `ACTION_*` constant. Per-request state (player, args, providers, BQu link state, deferred notifications) lives in a private `ActionContext` holder passed to each method.

**Authorization invariant:** `playerBQuLinked` and `activeProvider` are re-derived from `PartyManagerData.isBQuLinked` on every request — never trusted from the client. Mutating actions go through `getAdminParty()` / `getOrCreateSelfParty()` which enforce role checks server-side. Simple settings actions wrap the ADMIN+ gate via `onAdminParty(c, Predicate<Party>)` — return `false` from the predicate to fail the action.

**Failure → rollback:** `dispatch()` calls `provider.syncToAll()` on success; on failure it sends `provider.syncToPlayer(actor)` (a single-player sync) so the actor's optimistic UI mutation is corrected (`TOGGLE_BQU_LINK` is the exception — it broadcasts on failure too, since provider state may have drifted). `joinFreeParty` / `acceptInvite` also push an `EVENT_PARTY_FULL` or `EVENT_JOIN_FAILED` toast on their respective failure paths so a click is never silent.

**Adding a new action:** append a new `ACTION_*` constant to `MessagePartyAction` (do **not** renumber existing ones — wire-protocol stability), add a static factory method, add a `case` arm in `PartyActionDispatcher.dispatch()`, and implement the corresponding private method.

### MessageClientNotify kind dispatch

`MessageClientNotify` multiplexes every transient client toast through an `int kind` discriminator. Top-level kinds carry their own payload fields; sub-discriminators (party event types, claim failure reasons) stay as strings for forward compatibility (newer clients/servers can ignore unknown sub-types without breaking the channel).

`ClientNotifyClientHandler` switches on `kind` and delegates to the matching `BLPCToast` builder configuration (`fromTransit` / `fromPartyEvent` / `fromClaimFailed`).

Party-event sub-types: `MEMBER_JOINED`, `MEMBER_LEFT`, `KICKED`, `DISBANDED`, `INVITE_RECEIVED`, `OWNER_TRANSFERRED`, `ROLE_CHANGED`, `BQU_LINKED`, `BQU_UNLINKED`, `PARTY_FULL`, `JOIN_FAILED`. The actor is excluded from their own "you joined" (`notifyPartyMembers(..., excludeId)`) and "you disbanded" toasts.

**Adding a new kind:** append `KIND_*` to `MessageClientNotify`, add a static factory (e.g. `claimFailed(...)`), extend the `toBytes` / `fromBytes` `switch` with the new field layout, and add a `case` arm in `ClientNotifyClientHandler.buildToast`. No `ModNetwork` change required. New party-event sub-types only need a new `EVENT_*` constant + a `case` in `BLPCToast.Builder.fromPartyEvent` + a lang key.

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
| `blpc.party.create` | `CreatePanel.java` | Create-or-join (when no party): name input + pending-invite / free-to-join list |
| `blpc.party.settings` | `SettingsPanel.java` | Protection settings, ally/enemy management |
| `blpc.party.members` | `MembersPanel.java` | Member list |
| `blpc.party.moderators` | `ModeratorsPanel.java` | Moderator promote/demote |
| `blpc.party.dialog.disband` | MainPanel (inline `ConfirmDialog`) | Disband confirmation |
| `blpc.party.dialog.transfer` | `TransferOwnerDialog.java` | Transfer ownership |
| `blpc.party.dialog.rename` | SettingsPanel (InputDialog) | Rename party |
| `blpc.party.dialog.description` | SettingsPanel (InputDialog) | Edit party description |

Invite is handled inline in `MembersPanel` (direct `MessagePartyAction.invite()` call, no dialog). Ally/enemy management uses inline toggle buttons in SettingsPanel's trust party list (no separate dialog panels).

`MainPanel.build` is called either by `MainPanel.build(playerId)` (no auto-transition) or `MainPanel.build(playerId, IPanelHandler reopener)` — `ChunkMapScreen` passes its `partyHandler` so `CreatePanel`, after a successful create/join, can re-invoke the factory and pop straight into `MainPanel` instead of just closing. Full free-to-join parties show grayed and inert in `CreatePanel` (visible but not clickable — the server would reject anyway).

`MainPanel` pre-creates its 4 nav sub-panel handlers (Settings/Members/Moderators/Transfer) once per panel-open and reuses them across `rebuildMenu` calls — `IPanelHandler.simple` registers into `panel.clientSubPanels` with no removal API, so per-rebuild creation would leak. The handler closures re-read the party from cache by UUID (`PartyWidgets.livePartyRef`) so the sub-panel always opens against the current state.

## Color Conventions

**No ModularUI theme system.** BLPC ships a single **light** look; all colors are fixed Java values. There are two holders, split by surface:

- `client/gui/BLPCColors` — **semantic** party-panel + chunk-map colors. The `int` values are the **single source of truth** (`private static final`); consumers read them only through accessor methods so changing one value here propagates everywhere. Text/role: `text()` (`0xFF000000`), `buttonText()` (`0xFFFFFFFF`, white text on gray buttons) + `buttonTextShadow()` (`true`), `owner()` (`0xFFA66A00`), `admin()` (`0xFF1B7A1B`), `warning()` (`0xFFC00000`), `subtext()` (`0xFF555555`), `inactive()` (`0xFF888888`), `divider()` (`0x40000000`), plus `textShadow()` (`false`, panel-background titles). Map/HUD: `mapBackground()`, `mapBorder()`, `minimapBackground()`, `mapUnloaded()` (loading-tile fill), claim overlays `claimOwn()`/`claimParty()`/`claimOther()`/`claimHatching()`/`claimBorder()` (read by `ChunkMapRenderer`), and the `partyArgb(int rgb)` helper (opaque ARGB from a party's stored RGB — replaces the inlined `0xFF000000 | (rgb & 0xFFFFFF)`). Party panels, `ChunkMapScreen`, `ChunkMapWidget`, `ChunkMapRenderer`, and `MinimapHUD` all read these. `@SideOnly(CLIENT)`.
- `client/gui/GuiColors` — **fixed vanilla-context** ARGB constants, used where the surface is always MC's own dark background: tooltips, toasts (`BLPCToast`), chunk-map counters, and the chunk-map grid (`MinimapHUD`, `ChunkMapWidget`).

| `GuiColors` constant | Value | Matches | Usage |
|---|---|---|---|
| `WHITE` | `0xFFFFFFFF` | `TextFormatting.WHITE` (§f) | Map counters, toast default, map border |
| `GOLD` | `0xFFFFAA00` | `TextFormatting.GOLD` (§6) | Ally/invite toasts |
| `GREEN` | `0xFF55FF55` | `TextFormatting.GREEN` (§a) | Member/join toasts |
| `RED` | `0xFFFF5555` | `TextFormatting.RED` (§c) | Enemy/fail toasts, counter over-limit |
| `GRAY` | `0xFFAAAAAA` | `TextFormatting.GRAY` (§7) | Toast sub-text, tooltips |
| `DIVIDER` | `0x30FFFFFF` | — | Chunk-map grid lines |

Party text colors route through `BLPCColors` (black on the light panels) so they read against ModularUI's default button. Buttons use ModularUI's default theme — no per-widget background override. **Never inline `0x…` color literals** in widget code; the only exception is dynamic per-party `getColor()` ARGB composition (`ChunkMapWidget`, `SettingsPanel` ColorPicker). Party-specific role color logic is in `PartyWidgets.getRoleColor(PartyRole)`. Color changes are visual — verify with `runClient`.

For Minecraft formatting codes in tooltip strings, use `TextFormatting` enum constants (e.g. `TextFormatting.GREEN + "text"`) instead of raw `§X` escape sequences.

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
5. `syncToAll()` broadcasts to all clients. Open panels stay mounted and rebuild their menus (live-update).

**Disband** (`MessagePartyAction.disband()`):
1. Server verifies player is OWNER (checks both BLPC and BQu roles when BQu-linked).
2. Releases all chunk claims, removes party from `PartyManagerData`, clears BQu link flags.
3. Persists and syncs. The actor is excluded from the `DISBANDED` toast (they initiated it).
4. Client (in the disband `ConfirmDialog`) calls `panel.closeIfOpen()` (cascades to sub-panels) + `PartyWidgets.clearLocalPartyData()`. `MainPanel`'s sync listener also closes on `getPartyByPlayer == null` for the other party members.

## MUI Widget Patterns

| Widget | Usage | Notes |
|---|---|---|
| `CycleButtonWidget` + `IntValue.Dynamic` + `IKey.dynamic()` | Multi-state settings (trust levels), role MEMBER↔ADMIN cycle | `length()` sets number of states; `stateChild(i, ...)` per state; overlay/labels update dynamically |
| `ToggleButton` + `BoolValue.Dynamic` | Boolean settings (explosions, free-to-join, BQu link) | `overlay(false, ...)` / `overlay(true, ...)` for state-dependent labels |
| `ListWidget` + `LiveSearchableList` | Scrollable lists (members, invites, roles, allies, enemies) | For live-update panels use `LiveSearchableList<T>` (search box + parallel `rows`/`searchNames` arrays + `rebuild(Collection<T>)`). Row widgets use `.widthRel(1f).height(h)` — avoid fixed `.size(w, h)`. |
| `Dialog<T>` | Modal confirmations (disband, map bulk actions) | `closeWith(result)` triggers the result consumer and closes; extends `ModularPanel` |
| `Flow.col()` / `Flow.row()` | Automatic vertical/horizontal layout | `childPadding(n)` for spacing; `PartyWidgets.faceRow(uuid, label)` for the recurring face-icon + label row |
| `IKey.dynamic` / `*Value.Dynamic` / `setEnabledIf(w -> ...)` | Per-frame reactive state | Refresh visible values/visibility without rebuilding the widget tree — preferred over re-creating widgets |

For ModularUI API details, consult the ModularUI source code at `/mnt/data/git/ModularUI`. Text input fields use `setMaxLength(32)` for user-facing name inputs (party name, player name).

**`PartyWidgets` is the single styling/factory source for party UI** — change it once, every panel follows. Don't hand-build a row button or hard-code its geometry; route through the helpers:
- **Dimensions** (constants): `BTN_H`, `TAB_H`, `FACE_SIZE`, `ROW_INDENT` (left text indent), `INPUT_H`, `SUBMIT_BTN_W`, `CONFIRM_BTN_W`/`CONFIRM_BTN_H`, `CONTENT_TOP`. Never inline the magic numbers.
- **Labels**: `buttonLabel(key)` (white+shadow), `buttonLabelLeft(key)` (+ left align), `rowLabel(key, color)` (keep a role color, add shadow+align). Never repeat the `.color().shadow().alignment()` triple inline.
- **Widgets/layout**: `dialogButton`, `toggleButton`, `createPlayerRow`, `faceRow`, `divider()`, `dialogHeader(titleKey, messageKey)`, `addHeader`, `addTabs`, `addList` / `fillBelowHeader`, `newPageList`.
- **Shared logic**: `MemberEntry` (row data for member/player lists), `byRoleThenName()` (sort), `formatCycleOptionLine(prefix, name, selected)`, `underlineKey`/`defaultTooltip` (tooltip lines). These replaced the per-panel copies in `MembersPanel`/`ModeratorsPanel`/`SettingsPanel`.

## Client-Side Sync Pattern

Party panels receive real-time updates via `ClientPartyCache.loadFromNBT()` (triggered by `MessagePartySync` from server). Listeners are fired **immediately** when new data arrives — no tick-based coalescing. **`loadFromNBT` replaces every `Party` instance** in the cache, so a captured `Party` reference goes stale at once — read fresh via `ClientPartyCache.getParty(partyId)` or `PartyWidgets.livePartyRef(partyId, fallback)`.

`ClientPartyCache.fireSyncListeners()` can also be called directly for optimistic UI updates (e.g., after `PartyWidgets.setLocalBQuLinked()`, `clearLocalPartyData()`, or `PartyWidgets.sendAndApply(...)`).

**Live-update is the default** (Clayium-style). Panels stay mounted across server syncs; their dynamic regions (member lists, invite lists, role buttons) rebuild in place. Use `PartyWidgets.addSyncRefreshListener(panel, onSync)`:

```java
PartyWidgets.addSyncRefreshListener(panel, () -> {
    Party fresh = ClientPartyCache.getParty(partyId);
    if (fresh == null /* or other structural change */) {
        PartyWidgets.closeIfTopMost(panel);   // structural change → close (closeIfOpen() if party-gone affects parents too)
        return;
    }
    liveList.rebuild(collectRows(fresh));      // data change → repopulate
});
```

The callback runs on the next client tick (deferred via `addScheduledTask`) to avoid mutating the widget tree from inside a click handler that just optimistically called `fireSyncListeners()`. The listener is auto-removed on panel close.

**`LiveSearchableList<T>`** (`client/gui/party/widget/`) wraps the search-box + list + parallel `rows`/`searchNames` + filter pattern. `buildContainer()` returns the search-box-over-list `Flow`; `rebuild(Collection<T>)` repopulates rows in place (the search box widget itself survives, preserving the active filter). Constructor: `(rowFactory, nameExtractor, emptyStateKey)` — empty key may be `null`.

**Per-frame value bindings** (`IKey.dynamic`, `BoolValue.Dynamic`, `IntValue.Dynamic`) refresh visible state without any rebuild — use them for titles, toggle states, role colors, etc. `setEnabledIf(w -> ...)` toggles widget visibility per-frame (e.g. `MainPanel`'s disband button on ownership change). `SettingsPanel` relies entirely on these (read through `PartyWidgets.livePartyRef`) — its sync listener only closes when the party disappears.

**Optimistic mutation helper:** `PartyWidgets.sendAndApply(IMessage action, UUID partyId, Consumer<Party> optimistic)` sends the action, applies the mutation to the live cache instance, fires sync listeners, returns `true` — use directly as an `onMousePressed` body.

**Panels with sync listeners (live-update via `addSyncRefreshListener`):**

| Panel | Behavior on sync |
|---|---|
| `MainPanel` | Party gone → `panel.closeIfOpen()` (cascades to sub-panels); else `rebuildMenu` |
| `MembersPanel` | Party gone / not a member / manage-permission flipped → `closeIfTopMost`; else rebuild member + invite `LiveSearchableList`s |
| `ModeratorsPanel` | Party gone / not a member → `closeIfTopMost`; else refresh `isOwner` ref + rebuild row list |
| `CreatePanel` | Now in a party → `transitionToMain` (close + reopener.openPanel); else rebuild invite/free-to-join list |
| `TransferOwnerDialog` | Party gone / no longer OWNER → `closeIfTopMost`; else rebuild member list |
| `SettingsPanel` | Party gone → `closeIfTopMost`; otherwise no rebuild — `livePartyRef` keeps values current |

**Panels without sync listeners**: inline `ConfirmDialog` / `InputDialog` instances.

## UI Reusable Templates

### Reusable widgets (`client/gui/party/widget/`)

- **`ConfirmDialog`** — Yes/No confirmation (`Dialog<Boolean>`). Default 220×70. Used by: `MainPanel` (disband), `ChunkMapScreen`.
- **`InputDialog`** — Text field + submit (`Dialog<Void>`). Default 220×70. Used by: `SettingsPanel` (rename, description).
- **`LiveSearchableList<T>`** — search box + list + parallel filter arrays; `buildContainer()` + `rebuild(Collection<T>)`. Used by: `MembersPanel`, `ModeratorsPanel`.

Dialogs use a consistent 220px width; custom sizing via `.size(w, h)`.

### `PartyMenuBuilder` (`client/gui/party/`)

Fluent builder for the party main menu. Accumulate entries, then `buildInto(ListWidget)`:
- `PartyMenuBuilder.of(panel, party, playerId)` — create with a `MenuContext` snapshot
- `.navHandler(langKey, IPanelHandler)` — nav entry that opens a **pre-created** handler (preferred when the menu is rebuilt across syncs — avoids `clientSubPanels` leak)
- `.nav(langKey, Function<Party, ModularPanel>)` — nav entry that builds a fresh sub-panel via the factory on each click (alternative for static or single-use panels; prefer `.navHandler` when the menu is rebuilt across syncs to avoid `clientSubPanels` leak)
- `.widget(IWidget)` — raw widget injection (toggle buttons, etc.)
- `.tooltip(langKey)` / `.visible(Predicate<MenuContext>)` — modifiers on the current entry; `.visible(...)` skips the entry when the predicate is false (used in place of `if` blocks for conditional widgets)
- `.buildInto(ListWidget)` — materializes all entries
- `MenuContext` exposes `canInvite()`, `isOwner()`, `bquAvailable()` (and package-private `party()` / `panel()`)

**Allies/Enemies Management**: handled directly in `SettingsPanel` via inline trust lists (no separate dialog panels).

### Shared Utilities

Color constants in `client/gui/GuiColors`: `WHITE`, `GOLD`, `GREEN`, `RED`, `GRAY`, `GRAY_LIGHT`, `HOVER`, `DIVIDER` — ARGB.

`client/gui/party/PartyWidgets`:
- **Size constants** — `STANDARD_W/H` (220×180), `LARGE_W/H` (260×220), `DIALOG_W/H` (220×70), `BTN_H` (18), `FACE_SIZE` (8), `TAB_H` (16)
- **Layout** — `addHeader(panel, titleKey | IKey)`, `addList(panel, list)`, `addTabs(panel, controller, labelKeys, pages)` / `buildInnerTabs(labelKeys, pages)`, `wrapWithSearchBox(list, widgets, searchNames)` / `finalizeSearchableList(list, widgets, searchNames, emptyKey)`, `emptyStateRow(langKey)`, `faceRow(uuid, IKey)`
- **Widgets** — `createPlayerRow(uuid, label, color)`, `dialogButton(IKey label, IPanelHandler)`, `createEnterSubmitTextField(onSubmit)`
- **Data/format** — `getDisplayName(UUID)`, `getRoleColor(PartyRole)`, `formatMemberLabel(name, role)`
- **Live-update plumbing** — `addSyncRefreshListener(panel, onSync)`, `closeIfTopMost(panel)`, `livePartyRef(partyId, fallback) → Supplier<Party>`, `sendAndApply(IMessage, partyId, Consumer<Party>) → boolean`, `setLocalBQuLinked(boolean)`, `clearLocalPartyData()`

## Commands

`/blpc` root tree (`BLPCCommand extends CommandTreeBase`, permission level 0) registered by `CoreModule.serverStarting()`.

**Player subcommands** (`common/command/`):

| Subcommand | Purpose |
|---|---|
| `list` | List all parties |
| `info <party>` | Show party details |
| `me` | Show your own party info |
| `here` | Show claim owner of current chunk |
| `claims` | Show your claim count |
| `invites` | List pending invites |
| `accept <party>` | Accept a party invite |
| `decline <party>` | Decline a party invite |
| `leave` | Leave your current party |
| `admin` | Admin subcommand tree (see below) |

**Admin subcommands** (`common/command/admin/AdminCommand`, permission level 3):

| Subcommand | Purpose |
|---|---|
| `admin move-owner <party> <player>` | Transfer party ownership |
| `admin kick <party> <player>` | Force-kick a player from a party |
| `admin disband <party>` | Force-disband a party |

Query helpers shared by all commands: `api/util/PartyQueryUtil` (`findByName`, `allPartyNames`, `pendingInvitesFor`, `resolveName`). The internal helper `common/command/BLPCCommandHelper` adds `activeProviderFor` (BQu routing logic) and delegates all queries to `PartyQueryUtil`.

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

- **`api/party/RelationType`** — Enum: `MEMBER`, `ALLY`, `ENEMY`, `NONE`.
- **`core/ChunkTransitHandler`** — `PlayerTickEvent.END` listener. Detects chunk boundary crossings (overworld only), sends notifications via `MessageClientNotify.chunkTransit(...)`, and applies area effects.
- **`common/network/MessageClientNotify`** — multiplexed S→C packet for every client toast. `KIND_CHUNK_TRANSIT` carries player name + relation (`name()` string for forward compatibility) + entered flag. `KIND_PARTY_EVENT` carries event type string (join, leave, kick, disband, invite, transfer, role change, BQu link/unlink) + player name + extra info. `KIND_CLAIM_FAILED` carries reason + current/max counts. Handler: `client/network/ClientNotifyClientHandler`.
- **`client/gui/widget/BLPCToast`** — `IToast` implementation with Builder pattern. Factory methods: `fromTransit()` (chunk entry/exit), `fromPartyEvent()` (party events), `fromClaimFailed()` (claim limit errors). Only loaded on the physical client — never reachable from server-side bytecode.

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
