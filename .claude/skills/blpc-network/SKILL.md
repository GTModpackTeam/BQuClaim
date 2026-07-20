---
name: blpc-network
description: Network layer reference for BLPC — wire protocol, side boundary, PartyAction dispatch, ClientNotify.
user-invocable: false
---

# BLPC Network Layer

The network layer is split along the physical side boundary so that loading a class on the wrong side is impossible by construction:

| Package | Allowed types | Loaded on server? |
|---|---|---|
| `common/network/message/*` | IMessage POJOs only — no `@SideOnly` types in bytecode | Yes (both sides) |
| `common/network/*Handler` | Server-side IMessageHandler implementations | Yes (both sides) |
| `common/network/message/PartyAction.Handler` | Server-side handler for the party god-message | Yes (both sides) |
| `client/network/*ClientHandler` | `@SideOnly(Side.CLIENT)` IMessageHandler implementations | **Client only** |
| `client/network/ClientPacketHandlers` | Side-aware SPI installer; **not** `@SideOnly` | Yes (referenced from `ModNetwork`), but `installAll()` only executes on client |

**Why this matters:** `SimpleNetworkWrapper.registerMessage(handlerClass, ...)` triggers JVM class verification, which loads every type referenced in the handler's method bodies. If any is `@SideOnly(CLIENT)`, the SideTransformer rejects them on a dedicated server → `NoClassDefFoundError`. Keeping `client/network/*` out of the server's class-loading path eliminates this structurally.

## Package Contents

- **`common/network/`** — IMessage contracts only (no client-only references):
  - C→S: `ClaimChunk` (with inner `Handler`), `PartyAction` (with inner `Handler`), `WaypointAction` (with inner `Handler` — OWNER-only mutation).
  - S→C: `SyncClaims`, `SyncAllClaims`, `SyncConfig`, `PartySync`, `ClientNotify`, `WaypointSync`, `SyncAllWaypoints`. Pure data containers; no inner `Handler`.
  - `NbtMessage` — abstract base for messages with NBT payload. `PartySync`, `SyncAllClaims`, `SyncAllWaypoints` extend it.
  - `ModNetwork` — channel registration (side-aware). `NoOpHandler` — server-side fallback. `PlayerLoginHandler` — login sync.
- **`client/network/`** — All S→C handlers (`@SideOnly(Side.CLIENT)`), one per wire packet. Every handler extends `MainThreadMessageHandler<REQ>` (schedules `handleOnMainThread` onto `Minecraft.addScheduledTask`). `ClientPacketHandlers` is the side-aware SPI installer.

## Wire Protocol IDs (stable order)

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

## Discriminator-Multiplexed Packets

- **`PartyAction`** (C→S, ID 1) — `int action` + `String stringArg`. ~22 party operations.
- **`WaypointAction`** (C→S, ID 2) — `int action` (`ACTION_ADD_OR_UPDATE`/`ACTION_REMOVE`) + waypoint fields.
- **`ClientNotify`** (S→C, ID 7) — `int kind` + per-kind payload. Three kinds: `KIND_CHUNK_TRANSIT`, `KIND_PARTY_EVENT`, `KIND_CLAIM_FAILED`.

Append-only: existing constants are part of the on-wire format. Do not renumber.

## Adding a New Network Message

- **New action / notification** (preferred) — append a constant to `PartyAction` or `ClientNotify` and extend the `switch`. No `ModNetwork`/`ClientPacketHandlers` change.
- **New top-level packet** (rare) —
  - **C→S** — IMessage + handler in `common/network/`, append to `ModNetwork.init()`.
  - **S→C** — IMessage in `common/network/` (no `@SideOnly` refs). Handler in `client/network/` with `@SideOnly(Side.CLIENT)`. Append to BOTH `ModNetwork.CLIENT_BOUND_MESSAGES` AND `ClientPacketHandlers.installAll()` in same order.

## PartyAction Dispatch

`PartyAction.Handler` (nested in `PartyAction.java`) has one private static method per `ACTION_*` constant. Per-request state lives in `ActionContext`.

**Authorization:** `playerBQuLinked` and `activeProvider` re-derived from `IPartyProvider#isLinkedParty` on every request — never trusted from client. Mutating actions go through `getAdminParty()` / `getOrCreateSelfParty()` for role checks.

**Failure → rollback:** `dispatch()` calls `syncToAll()` on success; `syncToPlayer(actor)` on failure. `joinFreeParty` / `acceptInvite` push `EVENT_PARTY_FULL` / `EVENT_JOIN_FAILED` toasts on failure.

## ClientNotify Dispatch

`ClientNotifyClientHandler` switches on `kind` → `BLPCToast` builder (`fromTransit` / `fromPartyEvent` / `fromClaimFailed`).

Party-event sub-types: `MEMBER_JOINED`, `MEMBER_LEFT`, `KICKED`, `DISBANDED`, `INVITE_RECEIVED`, `OWNER_TRANSFERRED`, `ROLE_CHANGED`, `BQU_LINKED`, `BQU_UNLINKED`, `PARTY_FULL`, `JOIN_FAILED`.
