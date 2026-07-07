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
| JourneyMap mod jar (`compileOnly`, not runtime-required) | Compile-time target for `WaypointStoreMixin`'s internal (non-API) class references | Optional |

## Java 25 Syntax (Mandatory)

Jabel (`enableModernJavaSyntax = true`) compiles Java 25 features to JVM 8 bytecode. **Purpose:** fewer NPEs (safe casts via pattern matching) and less code (switch expressions drop redundant `break`/casts).

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
  - **`getPartyId(UUID)`** and **`getEffectiveParty(UUID)`** — `default` methods returning `null`, added specifically so server-side authoritative code never reads `PartyManagerData` directly (which only reflects BLPC's own invite/accept/create flow — a BQu member added purely through BQu's own party UI has no record there). `getPartyId` returns just a stable storage key (used by `WaypointManagerData`, login sync); `getEffectiveParty` returns a fully-populated `Party` (members, trust levels, allies/enemies, limits) for **`ChunkProtectionHandler`** (trust checks), **`ClaimChunk.Handler`** (additive claim/force-load limits), **`ChunkTransitHandler`** (relation resolution for notifications/area effects), and the login/logout force-load re-sync in `PlayerLoginHandler`/`CoreEventHandler` — call `PartyProviderRegistry.get().getEffectiveParty(playerId)` from new code in these areas instead of `PartyManagerData.getInstance().getPartyByPlayer(playerId)`. `DefaultPartyProvider` just delegates to `PartyManagerData`; `BQuPartyProvider` builds a merged `Party` via its private `buildMergedParty(DBEntry<IParty>)` (live BQu membership + settings copied from whichever member has a BLPC-side record, preferring the owner's) — the same helper backs `serializeForClient()`'s per-party client-sync view, so both stay consistent. Party **settings mutations** (`PartyAction`'s `ACTION_SET_TRUST_LEVEL`, `ACTION_SET_COLOR`, ally/enemy, etc.) intentionally keep reading/writing `PartyManagerData` directly — those settings are BLPC-only concepts BQu has no equivalent for, and are exactly what a BQu-linked party's `Party` record exists to store.
  - **`isLinkedParty(UUID)`** — `default` method returning `false`, used for **routing** (which provider handles a player's mutating action) instead of the `PartyManagerData.bquLinkedPlayers` per-player flag. That flag is only ever set for the members present at the moment an OWNER runs `ACTION_TOGGLE_BQU_LINK`; a player who joins the same (already-linked) BQu party afterward — normally through BQu's own party screen — never gets it, so flag-based routing kept sending their actions to the self-managed provider, and `PartyAction`'s `getOrCreateSelfParty` would then silently create a disconnected personal party for them on any settings change. `BQuPartyProvider.isLinkedParty` instead checks the player's *current* BQu party for any member with the flag set — recognizing new joiners immediately. `PartyAction.Handler.dispatch()`, `WaypointAction.Handler`, and `BLPCCommandHelper.activeProviderFor` all call `provider.isLinkedParty(playerId)` rather than `PartyManagerData.isBQuLinked(playerId)` directly. `BQuPartyProvider.serializeForClient()`'s `bquLinked` NBT list is likewise built from live `bquMembers` (every member of every linked party), not forwarded from the stale flag set, so the client's link-state UI matches.
- **`api/party/PartyProviderRegistry`** — Priority-based registry for the active provider. Constants: `PRIORITY_LOW=-100`, `PRIORITY_DEFAULT=0`, `PRIORITY_HIGH=100`. Higher priority wins; equal priority logs a warning and accepts the new provider (last-write-wins at tie); lower priority is silently ignored. Use `register(provider, priority)`.
- **`common/party/DefaultPartyProvider`** — Self-managed implementation backed by `PartyManagerData`. Registered by `CoreModule` at `PRIORITY_DEFAULT`.
- **`integration/bqu/BQuPartyProvider`** — BQu implementation that directly operates on BQu's `PartyManager`, `PartyInvitations`, and `NetPartySync`, with fallback to `DefaultPartyProvider` for players not in a BQu party. Registered by `BQuModule` at `PRIORITY_HIGH`, replacing the default provider when BQu is present — no data duplication.

**Design principle (Approach A):** When BQu is present, BLPC integrates INTO BQu's party system. BLPC's UI sends operations that `BQuPartyProvider` translates into BQu API calls. BQu's quest sharing works unchanged.

## Naming Conventions

- **Panel IDs:** `blpc.map`, `blpc.party`, `blpc.map.dialog.confirm`, `blpc.party.dialog.invite`
- **Lang keys:** `blpc.map.*` for map screen, `blpc.party.*` for party screen
- **Mod ID constants:** `api/util/Mods.Names`

## Package Layout

**Start here:** `api/BLPCAPI` is the central access point and discoverability index (GregTech `GregTechAPI` analog) — one façade documenting every subsystem and addon extension point (`partyProvider()`, `moduleManager()`, `MODID`). Read it first.

- **`api/`** — Public, addon-facing surface. `BLPCAPI` (façade/index), `modules/` (module framework SPI), `party/` (party backend SPI + domain types — `IPartyProvider`, `PartyProviderRegistry` with priority registration, `unregister()`/`getRegisteredPriority()` for diagnostics/reset, `registerNativeScreenOpener`/`unregisterNativeScreenOpener`/`hasNativeScreen`; **domain types**: `Party`, `PartyRole`, `TrustLevel`, `TrustAction`, `RelationType`), `event/` (`ChunkModifiedEvent`; `PartyEvent` — Pre/Post lifecycle hierarchy: cancelable `Pre.Created`/`Pre.Disbanded` veto mutations before they occur; informational `Post.Created`/`Post.Disbanded`/`Post.MemberJoined`/`Post.MemberLeft`/`Post.RoleChanged` fire after success), `util/` (`Mods`, `ModUtility`, `PartyQueryUtil` — addon-safe query façade delegating to the active `IPartyProvider`; `EnumUtils.parseOrDefault(Class<E>, name, default)` — shared `valueOf`-or-fallback used by `TrustLevel.fromName`, `PartyRole.fromName`, `RelationType.fromName`; reach for this instead of writing another try/catch `valueOf`), `integration/` (`IntegrationPanelRegistry` — registry of per-mod settings panels for the Addons hub, mirroring the concrete `integration/` package below; see `client/gui/AddonsPanel` below).
- **`common/party/`** — Party infrastructure: `PartyManagerData`, `DefaultPartyProvider`, `ClientPartyCache`. Domain types (`Party`, `PartyRole`, `TrustLevel`, `TrustAction`, `RelationType`) live in `api/party/`.
- **`common/chunk/`** — Claim data: `ChunkManagerData` (per-player and per-party claim/force-load counts funnel through a private `countMatching(Predicate<ClaimedChunkData>)`; `ClaimChunk.Handler.isLimitReached(...)` mirrors this shape one level up, taking per-player vs. per-party count/max accessors as lambdas so `isClaimLimitReached`/`isForceLoadLimitReached` share one implementation), `ClaimedChunkData`, `ClientClaimCache` (client-side cache — named to mirror `common/party/ClientPartyCache`'s pattern), `TicketManager`.
- **`common/waypoint/`** — Party-shared JourneyMap waypoint data (server + client): `PartyWaypointData` (value type), `WaypointManagerData` (server-side singleton store, persisted by `BLPCSaveHandler`), `ClientWaypointCache` (client-side mirror + change listeners, same shape as `ClientPartyCache`). See "JourneyMap Waypoint Team Sync" below.
- **`common/network/`** — IMessage contracts only (no client-only references):
  - C→S: `ClaimChunk` (with inner `Handler`), `PartyAction` (with inner `Handler` — see below; same nested-handler convention as `ClaimChunk`, just larger), `WaypointAction` (with inner `Handler` — same convention, enforces party-OWNER-only mutation).
  - S→C: `SyncClaims`, `SyncAllClaims`, `SyncConfig`, `PartySync`, `ClientNotify`, `WaypointSync`, `SyncAllWaypoints`. Each is a pure data container with getters; no inner `Handler`. `ClientNotify` is a discriminator-multiplexed packet that carries every transient client toast (chunk transit, party event, claim limit) through a single wire ID — it does **not** hold `PartyAction`'s handler; that lives in `PartyAction.Handler`.
  - `NbtMessage` — abstract base for messages whose entire payload is one `NBTTagCompound` (`data` field + getter + `readTag`/`writeTag`). `PartySync`, `SyncAllClaims`, and `SyncAllWaypoints` extend it; future NBT-payload messages should too.
  - `ModNetwork` — channel registration (side-aware). `NoOpHandler` — server-side fallback so S→C discriminators stay valid for outbound sends. `PlayerLoginHandler` — login sync (claims, parties, and — since the waypoint feature — the full party waypoint snapshot via `SyncAllWaypoints`).
- **`client/network/`** — All S→C handlers (`@SideOnly(Side.CLIENT)`), one class per top-level wire packet: `SyncClaimsClientHandler`, `SyncAllClaimsClientHandler`, `SyncConfigClientHandler`, `PartySyncClientHandler`, `ClientNotifyClientHandler` (dispatches by `ClientNotify.getKind()` to the matching `BLPCToast` builder), `WaypointSyncClientHandler`, `SyncAllWaypointsClientHandler` (bulk-loads via `ClientWaypointCache.loadAll(...)`, not per-entry `update()` — see waypoint section below for why). Every one of them extends `MainThreadMessageHandler<REQ>`, whose `final onMessage` schedules `handleOnMainThread(msg)` onto `Minecraft.addScheduledTask` — implementations only override `handleOnMainThread`, never re-implement the scheduling hop. `ClientPacketHandlers` is a side-aware SPI installer (intentionally **not** `@SideOnly`) referenced by `ModNetwork`.
- **`client/gui/`** — ModularUI screens only. `Screens` = the single catalog of every GUI + its open/build entry points (`openMap()`, `partyMain(...)`; RecipeMaps analog); `BLPCGuiTextures` = shared reusable `IDrawable`s (`DIVIDER`, `MAP_BACKGROUND`, `MAP_BORDER`) + `ICON_*` constants that reuse ModularUI's built-in `GuiTextures` icon atlas (`CLOSE`/`REFRESH`/`REMOVE` — no custom art; chunk-map tool buttons use these). Drawables are shared instances (a `Rectangle` only reads its fields at draw time) — never inline `new Rectangle().color(...)` in screen code, add it here. `BLPCColors` = semantic party/map palette, `GuiColors` = fixed vanilla-context ARGB; `BLPCToast` = vanilla toast notification; `ProtectionStatusHud` = brief claimed-chunk indicator (see "Protection Status HUD" below); `ChunkMapScreen`/`ChunkMapWidget`; `PlayerFaceDrawable`; party panels in `party/` subpackage; reusable widgets in `party/widget/` (`ConfirmDialog`, `InputDialog`, `LiveSearchableList`). Map pixel math derives from `ChunkMapRenderer.CHUNK_BLOCKS` (16 blocks/chunk — the single source for the recurring `% 16` / `/ 16` calculations).
- **`client/gui/AddonsPanel`** — Addons hub (single class directly under `client/gui/`, not a subpackage — it's one small screen, not a feature area like `party/`). Searchable via `PartyWidgets.finalizeSearchableList`; lists the available entries from `api/integration/IntegrationPanelRegistry` (lives in `api/` so third-party integrations register without depending on `client.gui` internals; each integration module registers one entry from its client-side init via a lazy method reference, mirroring `PartyProviderRegistry.registerNativeScreenOpener` — no `@SideOnly` on the registry, client-only-ness lives in the lambdas); opened from `MainPanel` when `IntegrationPanelRegistry.hasAvailable()`. The per-mod panels live in their integration packages, named `<Mod>SettingsPanel` — not `<Mod>AddonPanel` — since they're just each mod's settings screen, not an "addon" concept in their own right (`integration/jmap/JMapSettingsPanel`, `integration/bqu/BQuSettingsPanel`). BQu's link/unlink toggle and native-manager shortcut live in `BQuSettingsPanel` (registered when `PartyProviderRegistry.hasNativeScreen()`), not `SettingsPanel`'s Party Info tab.
- **`client/input/`** — `KeyInputHandler` (keybind registration; routes key presses to `Screens`). Single keybind: open chunk map (`M`).
- **`client/map/`** — Async chunk rendering, texture caching, claim overlay.
- **`client/cache/`** — `ClientCacheKey` (derives a filesystem-safe identifier for the current connection — singleplayer save folder or multiplayer server IP) + `ClientCachePersistence` (debounced NBT snapshot of `ClientClaimCache`/`ClientPartyCache` to `<gameDir>/blpc/cache/<key>/{claims,parties}.dat`, so the map/party UI shows last-known state immediately after reconnecting instead of an empty screen). Registered/loaded from `CoreEventHandler.ClientHandler` on `ClientConnectedToServerEvent`/`ClientDisconnectionFromServerEvent` — both hop onto `Minecraft.addScheduledTask` first, since Forge posts those events from the Netty I/O thread and `ClientClaimCache`/`ClientPartyCache` are plain non-thread-safe collections. NBT snapshotting always happens on the main thread; only the actual file write is handed to a background executor.

## Network Layer Architecture

The network layer is split along the physical side boundary so that loading a class on the wrong side is impossible by construction:

| Package | Allowed types | Loaded on server? |
|---|---|---|
| `common/network/message/*` | IMessage POJOs only — no `@SideOnly` types in bytecode | Yes (both sides) |
| `common/network/*Handler` | Server-side IMessageHandler implementations | Yes (both sides) |
| `common/network/message/PartyAction.Handler` | Server-side handler for the party god-message | Yes (both sides) |
| `client/network/*ClientHandler` | `@SideOnly(Side.CLIENT)` IMessageHandler implementations referencing `Minecraft`, `IToast`, `BLPCToast`, etc. | **Client only** |
| `client/network/ClientPacketHandlers` | Side-aware SPI installer; **not** `@SideOnly` | Yes (referenced from `ModNetwork`), but `installAll()` only executes on client |

**Why this matters:** `SimpleNetworkWrapper.registerMessage(handlerClass, ...)` calls `handlerClass.newInstance()`, which triggers JVM class verification. Verification loads every type referenced in the handler's method bodies (e.g. `BLPCToast` → `IToast`). If any of those types is `@SideOnly(CLIENT)`, the SideTransformer rejects them on a dedicated server and the mod crashes with `NoClassDefFoundError`. By keeping `client/network/*` out of the server's class-loading path entirely, the bug class is structurally eliminated.

`ClientPacketHandlers` uses class literals (`SomeHandler.class`) inside `installAll(channel, firstId)`. Class literals are resolved at execution time, not at verification time, so the server can safely reference `ClientPacketHandlers` itself without ever loading the handlers it points to.

### Wire protocol IDs (stable order)

| ID | Direction | Message | Handler |
|---|---|---|---|
| 0 | C→S | `ClaimChunk` | `ClaimChunk.Handler` |
| 1 | C→S | `PartyAction` (multiplexed) | `PartyAction.Handler` |
| 2 | C→S | `WaypointAction` (multiplexed) | `WaypointAction.Handler` |
| 3 | S→C | `SyncClaims` | `SyncClaimsClientHandler` |
| 4 | S→C | `SyncAllClaims` | `SyncAllClaimsClientHandler` |
| 5 | S→C | `SyncConfig` | `SyncConfigClientHandler` |
| 6 | S→C | `PartySync` | `PartySyncClientHandler` |
| 7 | S→C | `ClientNotify` (multiplexed) | `ClientNotifyClientHandler` |
| 8 | S→C | `WaypointSync` | `WaypointSyncClientHandler` |
| 9 | S→C | `SyncAllWaypoints` | `SyncAllWaypointsClientHandler` |

### Discriminator-multiplexed packets (preferred for new operations)

Two packets carry their own internal discriminator so adding new operations
does not require a new top-level wire ID:

- **`PartyAction`** (C→S, ID 1) — `int action` + `String stringArg`. ~22 party operations.
- **`WaypointAction`** (C→S, ID 2) — `int action` (`ACTION_ADD_OR_UPDATE`/`ACTION_REMOVE`) + waypoint fields. See "JourneyMap Waypoint Team Sync" below.
- **`ClientNotify`** (S→C, ID 7) — `int kind` + per-kind payload. Three kinds today (`KIND_CHUNK_TRANSIT`, `KIND_PARTY_EVENT`, `KIND_CLAIM_FAILED`) covering every BLPC toast.

Append-only: existing constants are part of the on-wire format. Do not renumber.

### Adding a new network message

- **New action / notification** (preferred) — append a constant to `PartyAction` or `ClientNotify` and extend the corresponding `switch` (dispatcher / handler / `toBytes` / `fromBytes`). Neither `ModNetwork` nor `ClientPacketHandlers` changes.
- **New top-level packet** (only for genuinely new message families) —
  - **C→S** — Define IMessage in `common/network/`, write the server handler (inner class is fine), append `INSTANCE.registerMessage(...)` in `ModNetwork.init()` before the S→C block.
  - **S→C** — Define IMessage in `common/network/` with **no `@SideOnly` types** referenced (use getters, not lambdas that capture `Minecraft`). Create the client handler in `client/network/<MessageName>ClientHandler.java` with `@SideOnly(Side.CLIENT)`. Append the message class to `ModNetwork.CLIENT_BOUND_MESSAGES` **and** the handler/message pair to `ClientPacketHandlers.installAll()` in the **same order** so server-side NoOp registration and client-side real registration share the same discriminator.

### PartyAction action dispatch

`PartyAction` multiplexes ~22 party operations through an `int action` discriminator + `String stringArg`. The server-side `PartyAction.Handler` (nested in `PartyAction.java`, same convention as `ClaimChunk.Handler`) has one private static method per `ACTION_*` constant. Per-request state (player, args, providers, BQu link state, deferred notifications) lives in a private `ActionContext` holder passed to each method.

**Authorization invariant:** `playerBQuLinked` and `activeProvider` are re-derived from `IPartyProvider#isLinkedParty` on every request — never trusted from the client, and a *live* check against current party membership rather than a stale per-player flag (see `isLinkedParty` above). Mutating actions go through `getAdminParty()` / `getOrCreateSelfParty()` which enforce role checks server-side. Simple settings actions wrap the ADMIN+ gate via `onAdminParty(c, Predicate<Party>)` — return `false` from the predicate to fail the action. `disbandParty()` and `toggleBQuLink()` resolve the acting player's party/role via `c.provider.getEffectiveParty(...)` / `c.provider.getRole(...)` rather than a raw `PartyManagerData` lookup, for the same reason.

**Failure → rollback:** `dispatch()` calls `provider.syncToAll()` on success; on failure it sends `provider.syncToPlayer(actor)` (a single-player sync) so the actor's optimistic UI mutation is corrected (`TOGGLE_BQU_LINK` is the exception — it broadcasts on failure too, since provider state may have drifted). `joinFreeParty` / `acceptInvite` also push an `EVENT_PARTY_FULL` or `EVENT_JOIN_FAILED` toast on their respective failure paths so a click is never silent.

**Adding a new action:** append a new `ACTION_*` constant to `PartyAction` (do **not** renumber existing ones — wire-protocol stability), add a static factory method, add a `case` arm in `PartyAction.Handler.dispatch()`, and implement the corresponding private method.

### ClientNotify kind dispatch

`ClientNotify` multiplexes every transient client toast through an `int kind` discriminator. Top-level kinds carry their own payload fields; sub-discriminators (party event types, claim failure reasons) stay as strings for forward compatibility (newer clients/servers can ignore unknown sub-types without breaking the channel).

`ClientNotifyClientHandler` switches on `kind` and delegates to the matching `BLPCToast` builder configuration (`fromTransit` / `fromPartyEvent` / `fromClaimFailed`).

Party-event sub-types: `MEMBER_JOINED`, `MEMBER_LEFT`, `KICKED`, `DISBANDED`, `INVITE_RECEIVED`, `OWNER_TRANSFERRED`, `ROLE_CHANGED`, `BQU_LINKED`, `BQU_UNLINKED`, `PARTY_FULL`, `JOIN_FAILED`. The actor is excluded from their own "you joined" (`notifyPartyMembers(..., excludeId)`) and "you disbanded" toasts.

**Adding a new kind:** append `KIND_*` to `ClientNotify`, add a static factory (e.g. `claimFailed(...)`), extend the `toBytes` / `fromBytes` `switch` with the new field layout, and add a `case` arm in `ClientNotifyClientHandler.buildToast`. No `ModNetwork` change required. New party-event sub-types only need a new `EVENT_*` constant + a `case` in `BLPCToast.Builder.fromPartyEvent` + a lang key.

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
├── claims/
│   ├── global.dat      # claims belonging to players with no party
│   ├── 0.dat           # claims belonging to members of party 0
│   └── ...
└── waypoints/
    ├── 0.dat           # shared JourneyMap waypoints for party 0 (only written if non-empty)
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
| `blpc.party.settings` | `SettingsPanel.java` | Protection settings, ally/enemy management (Party Info, Protection, Allies, Enemies tabs) |
| `blpc.party.members` | `MembersPanel.java` | Member list |
| `blpc.party.moderators` | `ModeratorsPanel.java` | Moderator promote/demote |
| `blpc.party.addons` | `client/gui/AddonsPanel.java` | Addons hub — searchable list of available per-mod settings panels |
| `blpc.party.addons.journeymap` | `integration/jmap/JMapSettingsPanel.java` | JourneyMap claim-overlay toggle + team waypoint-sharing toggle |
| `blpc.party.addons.bqu` | `integration/bqu/BQuSettingsPanel.java` | BQu link/unlink toggle + native party manager shortcut |
| `blpc.party.dialog.disband` | MainPanel (inline `ConfirmDialog`) | Disband confirmation |
| `blpc.party.dialog.transfer` | `client/gui/party/TransferOwnerPanel.java` | Transfer ownership |
| `blpc.party.dialog.rename` | SettingsPanel (InputDialog) | Rename party |
| `blpc.party.dialog.description` | SettingsPanel (InputDialog) | Edit party description |

Invite is handled inline in `MembersPanel` (direct `PartyAction.invite()` call, no dialog). Ally/enemy management uses inline toggle buttons in SettingsPanel's trust party list (no separate dialog panels).

`MainPanel.build` is called either by `MainPanel.build(playerId)` (no auto-transition) or `MainPanel.build(playerId, IPanelHandler reopener)` — `ChunkMapScreen` passes its `partyHandler` so `CreatePanel`, after a successful create/join, can re-invoke the factory and pop straight into `MainPanel` instead of just closing. Full free-to-join parties show grayed and inert in `CreatePanel` (visible but not clickable — the server would reject anyway).

`MainPanel` pre-creates its 4 nav sub-panel handlers (Settings/Members/Moderators/Transfer) once per panel-open and reuses them across `rebuildMenu` calls — `IPanelHandler.simple` registers into `panel.clientSubPanels` with no removal API, so per-rebuild creation would leak. The handler closures re-read the party from cache by UUID (`PartyWidgets.livePartyRef`) so the sub-panel always opens against the current state.

## Color Conventions

**No ModularUI theme system.** BLPC ships a single **light** look; all colors are fixed Java values. There are two holders, split by surface:

- `client/gui/BLPCColors` — **semantic** party-panel + chunk-map colors. The `int` values are the **single source of truth** (`private static final`); consumers read them only through accessor methods so changing one value here propagates everywhere. Text/role: `text()` (`0xFF000000`), `buttonText()` (`0xFFFFFFFF`, white text on gray buttons) + `buttonTextShadow()` (`true`), `owner()` (`0xFFA66A00`), `admin()` (`0xFF1B7A1B`), `warning()` (`0xFFC00000`), `subtext()` (`0xFF555555`), `inactive()` (`0xFF888888`), `divider()` (`0x40000000`), plus `textShadow()` (`false`, panel-background titles). Map: `mapBackground()`, `mapBorder()`, `mapUnloaded()` (loading-tile fill), claim overlays `claimOwn()`/`claimParty()`/`claimOther()`/`claimHatching()`/`claimBorder()` (read by `ChunkMapRenderer`), and the `partyArgb(int rgb)` helper (opaque ARGB from a party's stored RGB — replaces the inlined `0xFF000000 | (rgb & 0xFFFFFF)`). Party panels, `ChunkMapScreen`, `ChunkMapWidget`, and `ChunkMapRenderer` all read these. `@SideOnly(CLIENT)`.
- `client/gui/GuiColors` — **fixed vanilla-context** ARGB constants, used where the surface is always MC's own dark background: tooltips, toasts (`BLPCToast`), chunk-map counters, and the chunk-map grid (`ChunkMapWidget`).

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
2. Client sends `PartyAction.toggleBQuLink()` to server.
3. Server verifies player is ADMIN+ and has a BQu party (for link). If rejected, `syncToAll()` is still called to roll back the optimistic update.
4. On success, updates `PartyManagerData.bquLinkedPlayers` and persists via `BLPCSaveHandler`.
5. `syncToAll()` broadcasts to all clients. Open panels stay mounted and rebuild their menus (live-update).

**Disband** (`PartyAction.disband()`):
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

Party panels receive real-time updates via `ClientPartyCache.loadFromNBT()` (triggered by `PartySync` from server). Listeners are fired **immediately** when new data arrives — no tick-based coalescing. **`loadFromNBT` replaces every `Party` instance** in the cache, so a captured `Party` reference goes stale at once — read fresh via `ClientPartyCache.getParty(partyId)` or `PartyWidgets.livePartyRef(partyId, fallback)`.

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
| `TransferOwnerPanel` | Party gone / no longer OWNER → `closeIfTopMost`; else rebuild member list |
| `SettingsPanel` | Party gone → `closeIfTopMost`; otherwise no rebuild — `livePartyRef` keeps values current |

**Panels without sync listeners**: inline `ConfirmDialog` / `InputDialog` instances.

## UI Reusable Templates

### Reusable widgets (`client/gui/party/widget/`)

- **`ConfirmDialog`** — Yes/No confirmation (`Dialog<Boolean>`). Default 220×70. Used by: `MainPanel` (disband), `ChunkMapScreen`.
- **`InputDialog`** — Text field + submit (`Dialog<Void>`). Default 220×70. Used by: `SettingsPanel` (rename, description).
- **`LiveSearchableList<T>`** — search box + list + parallel filter arrays; `buildContainer()` + `rebuild(Collection<T>)`. Used by: `MembersPanel`, `ModeratorsPanel`.

Dialogs use a consistent 220px width; custom sizing via `.size(w, h)`.

`TransferOwnerPanel` (`blpc.party.dialog.transfer`, `client/gui/party/TransferOwnerPanel.java`) — OWNER-only member picker; transfers ownership via `PartyAction.transferOwnership`. Lives alongside `MembersPanel`/`ModeratorsPanel` in `client/gui/party/`, not `widget/`, since it returns a full `ModularPanel` (not a `Dialog<T>`) and is wired as a normal nav sub-panel.

### `PartyMenuBuilder` (`client/gui/party/`)

Fluent builder for the party main menu. Accumulate entries, then `buildInto(ListWidget)`:
- `PartyMenuBuilder.of(panel, party, playerId)` — create with a `MenuContext` snapshot
- `.navHandler(langKey, IPanelHandler)` — nav entry that opens a **pre-created** handler (preferred when the menu is rebuilt across syncs — avoids `clientSubPanels` leak)
- `.nav(langKey, Function<Party, ModularPanel>)` — nav entry that builds a fresh sub-panel via the factory on each click (alternative for static or single-use panels; prefer `.navHandler` when the menu is rebuilt across syncs to avoid `clientSubPanels` leak)
- `.widget(IWidget)` — raw widget injection (toggle buttons, etc.)
- `.tooltip(langKey)` / `.visible(Predicate<MenuContext>)` — modifiers on the current entry; `.visible(...)` skips the entry when the predicate is false (used in place of `if` blocks for conditional widgets)
- `.buildInto(ListWidget)` — materializes all entries
- `MenuContext` exposes `canInvite()`, `isOwner()` (and package-private `party()` / `panel()`)

**Allies/Enemies Management**: handled directly in `SettingsPanel` via inline trust lists (no separate dialog panels).

### Shared Utilities

Color constants in `client/gui/GuiColors`: `WHITE`, `GOLD`, `GREEN`, `RED`, `GRAY`, `GRAY_LIGHT`, `HOVER`, `DIVIDER` — ARGB.

`client/gui/party/PartyWidgets`:
- **Size constants** — `STANDARD_W/H` (220×180), `LARGE_W/H` (260×220), `DIALOG_W/H` (220×70), `BTN_H` (18), `FACE_SIZE` (8), `TAB_H` (16)
- **Layout** — `addHeader(panel, titleKey | IKey)`, `addList(panel, list)`, `addTabs(panel, controller, labelKeys, pages)` / `buildInnerTabs(labelKeys, pages)`, `wrapWithSearchBox(list, widgets, searchNames)` / `finalizeSearchableList(list, widgets, searchNames, emptyKey)`, `emptyStateRow(langKey)`, `faceRow(uuid, IKey)`
- **Widgets** — `createPlayerRow(uuid, label, color)`, `dialogButton(IKey label, IPanelHandler)`, `createEnterSubmitTextField(onSubmit)`
- **Data/format** — `getDisplayName(UUID)`, `getRoleColor(PartyRole)`, `formatMemberLabel(name, role)`
- **Live-update plumbing** — `addSyncRefreshListener(panel, onSync)`, `closeIfTopMost(panel)`, `livePartyRef(partyId, fallback) → Supplier<Party>`, `sendAndApply(IMessage, partyId, Consumer<Party>) → boolean`, `setLocalBQuLinked(boolean)`, `clearLocalPartyData()`
- **Member lists** — `collectSortedMembers(party, excludeUuid)` builds a `List<MemberEntry>` sorted by `byRoleThenName()`, optionally skipping one UUID (self, in transfer/kick pickers). Shared by `MembersPanel`, `ModeratorsPanel`, `TransferOwnerPanel` — don't hand-roll the collect-and-sort loop in a new panel.

## Commands

`/blpc` root tree (`BLPCCommand extends CommandTreeBase`, permission level 0) registered by `CoreModule.serverStarting()`.

**Player subcommands** (`common/command/`, all extend `PlayerCommand` — the shared base that fixes `getRequiredPermissionLevel() = 0` and `checkPermission(...) = true` so individual commands don't repeat that boilerplate):

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

**Admin subcommands** (`common/command/admin/AdminCommand`, permission level 3; `MoveOwnerCommand`/`KickCommand`/`DisbandCommand` extend `AdminSubCommand`, the shared base that fixes `getRequiredPermissionLevel() = 3`):

| Subcommand | Purpose |
|---|---|
| `admin move-owner <party> <player>` | Transfer party ownership |
| `admin kick <party> <player>` | Force-kick a player from a party |
| `admin disband <party>` | Force-disband a party |

Query helpers shared by all commands: `api/util/PartyQueryUtil` (`findByName`, `allPartyNames`, `pendingInvitesFor`, `resolveName`). The internal helper `common/command/BLPCCommandHelper` adds `activeProviderFor` (BQu routing logic), `requirePartyByName(name)` (like `findByName` but throws the standard "Party not found" `CommandException` instead of returning null — use this instead of a manual null-check in every command), and `resolveOwnerName(server, party)` (owner display name, or `"-"` when ownerless), then delegates the rest to `PartyQueryUtil`.

## Mixins

Uses MixinBooter (`ILateMixinLoader`) for conditional late-stage injection:

- **`BLPCMixinLoader`** — Loads mixin configs conditionally based on mod presence.
- **`NetPartyActionMixin`** — Injects into BQu's `NetPartyAction.deleteParty()` to auto-unlink all affected players from BQu in BLPC's `PartyManagerData`. Prevents orphaned BQu links.
- **`mixins/journeymap/WaypointStoreMixin`** (client-only) — Injects into JourneyMap's internal (non-API) `journeymap.client.waypoint.WaypointStore` to detect local waypoint add/edit/remove, since the public JourneyMap API has no change-notification hook for this. `@Inject(method = "save", at = @At("RETURN"))` and `@Inject(method = "remove", at = @At("HEAD"))` forward to `JMapWaypointOutgoing`. Deliberately does **not** hook `WaypointStore`'s add path (startup load would look identical to a real add and cause spurious network traffic). Because this reaches into JourneyMap's non-API internals, it's inherently more fragile across JourneyMap versions than the rest of the (API-based) `integration/jmap` code — see "JourneyMap Waypoint Team Sync" below.

Configs: `src/main/resources/mixins.blpc.betterquesting.json`, `src/main/resources/mixins.blpc.journeymap.json` (`client: ["WaypointStoreMixin"]`, no `server` mixins — JourneyMap itself is client-only). `dependencies.gradle` adds `compileOnly rfg.deobf(...)` for JourneyMap's mod jar (not just the API) so the Mixin's target classes resolve at compile time.

## JourneyMap Waypoint Team Sync

Party-owned JourneyMap waypoints are mirrored to every online party member's local map, so a party sees one shared set of markers (e.g. base, farm, portal) instead of each member maintaining their own. Gated by `JMapClientConfig.isWaypointSharingEnabled()` (per-client toggle in `JMapSettingsPanel`) and, structurally, by whether the Mixin config loaded at all (`Mods.Names.JOURNEY_MAP` present).

**Permission model:** only the party **OWNER** may add/edit/remove shared waypoints; regular members are view-only. This is enforced authoritatively server-side in `WaypointAction.Handler` — a non-owner's action is rejected and the server sends back the pre-existing server-side state for that waypoint (or a `WaypointSync.remove` if it didn't exist) so the sender's local JourneyMap store snaps back to the authoritative state instead of silently keeping the rejected local edit. `JMapWaypointOutgoing.isPartyOwner()` mirrors this client-side purely to avoid pointless traffic/rollback flicker for non-owners — it is not itself a security boundary.

**Outgoing flow (owner's client → server):**
1. `WaypointStoreMixin` detects a local `save`/`remove` on JourneyMap's internal `WaypointStore` and forwards to `JMapWaypointOutgoing`.
2. `JMapWaypointOutgoing` filters out: remote-echoed changes (`applyingRemoteChange` flag, set while `JMapWaypointSyncHandler` is writing incoming data — prevents feedback loops), `Waypoint.Type.Death` waypoints, non-owners, and sharing-disabled clients.
3. **Save/remove debounce**: JourneyMap's waypoint editor always does `remove(original)` then `save(edited)` even for a pure edit of an existing waypoint. A detected remove is held in `pendingRemoveId` until end-of-tick (`TickEvent.ClientTickEvent`, static-registered on the class) rather than sent immediately; if a `save` for the same id arrives first, the pending remove is cleared and only the update is sent. Without this, every edit would emit a spurious delete-then-recreate on every other member's map.
4. Sends `WaypointAction.addOrUpdate(...)` / `.remove(...)` (C→S, ID 2) to the server.

**Server (`WaypointAction.Handler`):** resolves the acting player's party via `IPartyProvider.getPartyId(UUID)` (see below), validates (`waypointId`/`name` length caps, `MAX_WAYPOINTS_PER_PARTY = 200`), authorizes (OWNER-only, with rollback on rejection as described above), applies the change to `WaypointManagerData`, persists via `BLPCSaveHandler`, and broadcasts a `WaypointSync` diff (S→C, ID 8) to every **other** online party member (the actor already has the change applied locally).

**Incoming flow (other members / full login sync):** `WaypointSyncClientHandler` (single-waypoint diff) and `SyncAllWaypointsClientHandler` (full snapshot, sent on login via `PlayerLoginHandler`) write into `ClientWaypointCache`, whose change listener (`JMapWaypointSyncHandler`) rebuilds the local JourneyMap `WaypointStore` entries under `applyingRemoteChange = true` so the mirrored writes don't re-trigger `WaypointStoreMixin`. `SyncAllWaypointsClientHandler` uses `ClientWaypointCache.loadAll(...)` (replace-all + fire listeners once) rather than looping `update()` per waypoint — the latter would re-run the full JourneyMap mirror rebuild once per waypoint on login, an O(n²) cost for a party with many waypoints.

**Deterministic IDs:** a shared waypoint's key is JourneyMap's own `Waypoint.getId()`, which for a waypoint built via `journeymap.client.api.display.Waypoint(BLPC_MODID, waypointId, ...)` always resolves to `"blpc:" + waypointId` (from JourneyMap's `Waypoint.getGuid()` = `origin + ":" + displayId`). `JMapWaypointSyncHandler.applyToJourneyMap()` matches on `Tags.MODID.equals(wp.getOrigin())` to find/clean up only BLPC-mirrored entries, without needing a separate id-mapping table.

**`IPartyProvider.getPartyId(UUID)`:** a `default` method returning `null`, added specifically so waypoint code (and any future per-party server storage) can resolve a stable party identifier without depending on a `Party` object existing. `DefaultPartyProvider` derives it from its own `Party.getPartyId()`; `BQuPartyProvider` derives it from BQu's own integer party id via `Party.uuidFromIntId(...)` so it's identical for every member even if no BLPC-side `Party` record has ever been created for that BQu party (a real bug found in earlier iterations — resolving the acting player's `Party` object directly could diverge between members before the BQu link created BLPC-side shadow records).

**Persistence:** `common/waypoint/WaypointManagerData` (server-side singleton, `Map<UUID partyId, Map<String waypointId, PartyWaypointData>>`, `getWaypoints`/`getAllForSave` return unmodifiable views) is saved/loaded by `BLPCSaveHandler` under `world/betterlink/pc/waypoints/<partyId>.dat`, one file per party with any waypoints (mirrors the `parties/`/`claims/` layout). `WaypointManagerData.removeParty(partyId)` is called from `PartyAction`'s disband path so a disbanded party's waypoints don't linger.

**Key classes:** `common/waypoint/PartyWaypointData` (value type), `WaypointManagerData` (server store), `ClientWaypointCache` (client mirror + change listeners); `common/network/message/WaypointAction` (C→S, with nested `Handler`), `WaypointSync` (S→C diff), `SyncAllWaypoints` (S→C snapshot, extends `NbtMessage`); `integration/jmap/JMapWaypointOutgoing` (local-change detector), `JMapWaypointSyncHandler` (remote-change applier); `mixins/journeymap/WaypointStoreMixin`.

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

**Party required to claim:** `ClaimChunk.Handler.isPartyMissing` rejects a brand-new claim (both `MODE_CLAIM` and the fresh-claim branch of `MODE_TOGGLE_FORCE`) unless `PartyProviderRegistry.get().getPartyId(playerId) != null` — chunk protection is a party-sharing feature, not a solo-player one, so a player must first create/join a party (or, in singleplayer, rely on `ModConfig.party.autoCreatePartySingleplayer`). Rejection sends `ClientNotify.claimFailed(REASON_NO_PARTY, 0, 0)` → `blpc.toast.no_party`. Already-claimed chunks are unaffected (unclaim/toggle-force on an *existing* claim never re-checks this).

**Party** (`ModConfig.party`)

| Option | Type | Default | Description |
|---|---|---|---|
| `autoCreatePartySingleplayer` | boolean | true | Auto-create party in singleplayer |

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

**Fair Play** (`ModConfig.fairPlay`) — client-visible gameplay toggles, aimed at PvP servers that want to dial back or fully disable BLPC's chunk-transit side effects.

| Option | Type | Default | Description |
|---|---|---|---|
| `enableAreaEffects` | boolean | true | Apply potion effects for area control (weakness/mining fatigue to enemies, resistance/strength to defenders) |
| `enableTransitNotify` | boolean | true | Send toast notifications on claimed-chunk entry/exit |
| `showProtectionStatusHud` | boolean | true | Show `ProtectionStatusHud`'s on-screen indicator while standing in a claimed chunk |

### Internal defaults (`ModConfig.Defaults` inner class — not in cfg)

| Constant | Value | Description |
|---|---|---|
| `enableProtection` | true | Master protection toggle |
| `protectMobGriefing` | true | Prevent mob griefing in claims |
| `protectFireSpread` | true | Prevent fire spread in claims |
| `protectFluidFlow` | true | Prevent fluid flow into claims |
| `transitToastDuration` | 3000 | Toast display duration (ms) |
| `enemyWeaknessAmplifier` | 0 | Weakness amplifier (0 = level I) |
| `enemyMiningFatigue` | true | Mining fatigue for enemies |
| `defenderResistanceAmplifier` | 0 | Resistance amplifier (0 = level I) |

## Chunk Transit System

Players receive **toast notifications** when entering/leaving claimed chunks, and **potion effects** are applied based on relationship.

### Classes

- **`api/party/RelationType`** — Enum: `MEMBER`, `ALLY`, `ENEMY`, `NONE`.
- **`core/ChunkTransitHandler`** — `PlayerTickEvent.END` listener. Detects chunk boundary crossings (overworld only), sends notifications via `ClientNotify.chunkTransit(...)`, and applies area effects.
- **`common/network/message/ClientNotify`** — multiplexed S→C packet for every client toast. `KIND_CHUNK_TRANSIT` carries player name + relation (`name()` string for forward compatibility) + entered flag. `KIND_PARTY_EVENT` carries event type string (join, leave, kick, disband, invite, transfer, role change, BQu link/unlink) + player name + extra info. `KIND_CLAIM_FAILED` carries reason + current/max counts. Handler: `client/network/ClientNotifyClientHandler`.
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

### Protection Status HUD

`client/gui/ProtectionStatusHud` — `RenderGameOverlayEvent.Post` listener, gated by `ModConfig.fairPlay.showProtectionStatusHud`. Purely client-side: resolves relation from `ClientClaimCache`/`ClientPartyCache` data already synced to the client (its own `resolveRelation` mirrors `ChunkTransitHandler`'s server-side version but starts from a `ClaimedChunkData` instead of a `Party`, and short-circuits to `MEMBER` when the local player is the claim's direct owner). On entering a new claimed chunk, shows `blpc.hud.protected_area` centered just above the food/stamina bar (`BOTTOM_MARGIN = 50`, matching vanilla's `height - 39` bar position) for 5 seconds (`DISPLAY_TICKS = 100`), colored via `GuiColors` by relation (`GREEN` member, `GOLD` ally, `RED` enemy, `GRAY` none). Re-arms only on a chunk-coordinate change, not every frame.



## Localization

Lang files in `src/main/resources/assets/blpc/lang/`: `en_us.lang` and `ja_jp.lang`. Both cover keybindings, commands, map UI, party UI, roles, trust actions/levels, protection settings, allies/enemies, tooltips, search, transit notifications (`blpc.transit.*`), party event/claim failure notifications (`blpc.toast.*`), addon panels (`blpc.addons.*` — including `blpc.addons.journeymap.waypoints_on`/`waypoints_off`/`waypoints_tooltip` for the team waypoint-sharing toggle), the Fair Play config category (`config.blpc.fair_play`), and the Protection Status HUD (`blpc.hud.protected_area`).

## Adding a New Integration Module

1. Create `integration/<modid>/` package.
2. Create a module class extending `IntegrationSubmodule` with `@TModule(modDependencies=Mods.Names.THE_MOD)`.
3. Add module ID constant to `Modules.java`.
4. Add mod ID to `Mods` enum and `Mods.Names`.
5. (Optional, for a settings UI) Add a `<Mod>SettingsPanel` (`@SideOnly(CLIENT)`) in the integration package and register it from the module's client-guarded `init` via `api/integration/IntegrationPanelRegistry.register(labelKey, tooltipKey, available, <Mod>SettingsPanel::build)`. Use a lazy method reference so the client-only panel is never loaded on a dedicated server. Add `blpc.addons.<mod>*` lang keys to both lang files. It then appears automatically under the party menu's Addons hub.
