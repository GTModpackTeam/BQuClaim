# BetterLinkPartyClaim — Developer API

BLPC exposes an addon-facing API for third-party mods that want to plug into its party
system, react to claim/party events, or add their own settings screen. The table below
shows current coverage:

| Area                          | API package                    | Status    |
|--------------------------------|--------------------------------|-----------|
| Custom party backend           | `api.party`                     | Available |
| Query party data                | `api.util` (`PartyQueryUtil`)   | Available |
| Party lifecycle events          | `api.event` (`PartyEvent`)      | Available |
| Claim lifecycle events          | `api.event` (`ChunkModifiedEvent`) | Available |
| Addons hub settings panel       | `api.integration`               | Available |
| Module framework                | `api.modules`                   | Available |

Start at [`api/BLPCAPI.java`](src/main/java/com/github/gtexpert/blpc/api/BLPCAPI.java) —
it's the central façade and package index (a `GregTechAPI`-style entry point) that links
every subsystem described below.

Registry-based registration calls (`PartyProviderRegistry`, `IntegrationPanelRegistry`)
should happen from your mod's `preInit`/`init`, guarded by whichever FML phase the
registry expects (noted per-section below) — see [Module Framework](#module-framework)
if your integration is itself driven by BLPC's module system.

---

## Party Backend SPI

### PartyProviderRegistry

Replace BLPC's party backend entirely — e.g. to back parties with your own guild/clan
system instead of BLPC's self-managed storage or BetterQuesting.

```java
// preInit, or wherever your mod wires up its own systems
PartyProviderRegistry.register(new MyPartyProvider(), PartyProviderRegistry.PRIORITY_HIGH);
```

- `PRIORITY_LOW` (-100) / `PRIORITY_DEFAULT` (0) / `PRIORITY_HIGH` (100) — a
  higher-priority registration wins; a lower one is silently ignored. BLPC's own
  `DefaultPartyProvider` registers at `PRIORITY_DEFAULT`, and `BQuPartyProvider`
  (BetterQuesting integration) at `PRIORITY_HIGH` when BetterQuesting is present — use
  `PRIORITY_HIGH` to take over from BQu, or `PRIORITY_LOW` to act as a fallback only.
- `unregister()` reverts to the internal no-op provider; `getRegisteredPriority()`
  returns the active registration's priority (`Integer.MIN_VALUE` if none).
- `registerNativeScreenOpener(Runnable)` / `unregisterNativeScreenOpener()` /
  `hasNativeScreen()` — optionally expose a shortcut to your own native party-management
  screen from BLPC's UI (this is how the BetterQuesting integration opens BQu's own party
  manager).

Implement `IPartyProvider`:

```java
public class MyPartyProvider implements IPartyProvider {
    @Override
    public boolean areInSameParty(UUID playerA, UUID playerB) { ... }

    @Override
    public String getPartyName(UUID playerUUID) { ... }

    // ... createParty, disbandParty, invitePlayer, kickOrLeave, changeRole, etc.

    @Override
    public void syncToAll() { ... }

    @Override
    public NBTTagCompound serializeForClient() { ... }
}
```

All mutation methods identify the acting party via the player's UUID — no explicit
party-ID parameter is needed except `acceptInvite(player, partyId)`, which targets a
*different* party than the one the player currently belongs to (or none).

### Party domain types

`api.party` also carries the shared domain model, usable regardless of which provider is
active:

- **`Party`** — members (`Map<UUID, PartyRole>`), trust settings, allies/enemies,
  invites, free-to-join/description/color/max-members.
- **`PartyRole`** — `MEMBER < ADMIN < OWNER`, with `canInvite()`, `canKick(target)`,
  `canDisband()`, `toTrustLevel()`.
- **`TrustLevel`** — `NONE < ALLY < MEMBER < MODERATOR < OWNER`, checked via
  `isAtLeast(required)`.
- **`TrustAction`** — the four protection actions (`BLOCK_EDIT`, `BLOCK_INTERACT`,
  `ATTACK_ENTITY`, `USE_ITEM`), each with a per-party configurable minimum `TrustLevel`.
- **`RelationType`** — `MEMBER` / `ALLY` / `ENEMY` / `NONE`, relative to a chunk-owning
  party.

Each enum has a `fromName(String)` parser (backed by `EnumUtils.parseOrDefault`, see
[Utility helpers](#utility-helpers)) that falls back to a safe default instead of
throwing on an unrecognized name — useful when reading data from another mod's wire
format or config.

---

## Querying Party Data

### PartyQueryUtil

Read party data without depending on internal packages or the raw `IPartyProvider`:

```java
Party party = PartyQueryUtil.findByName("MyCrew");
List<String> allNames = PartyQueryUtil.allPartyNames();
List<Party> invites = PartyQueryUtil.pendingInvitesFor(playerUUID);

// Multi-source name resolution for chat/log output: online player → cached party
// name → global UsernameCache → UUID prefix.
String displayName = PartyQueryUtil.resolveName(server, party, uuid);
```

Safe to call from the server thread after world load; calling before
`FMLServerStartedEvent` or from the client thread returns empty/null results.

---

## Party Lifecycle Events

### PartyEvent

Subscribe on `MinecraftForge.EVENT_BUS`. `Pre` variants are `@Cancelable` (veto the
mutation before it happens); `Post` variants are informational and fire only after a
successful mutation.

```java
@SubscribeEvent
public void onPartyCreated(PartyEvent.Post.Created e) {
    LOGGER.info("Party '{}' created by {}", e.getPartyName(), e.getOwnerUUID());
}

@SubscribeEvent
public void onBeforeDisband(PartyEvent.Pre.Disbanded e) {
    if (isProtected(e.getPartyId())) e.setCanceled(true);
}
```

Event tree:

```
PartyEvent
├── Pre  (cancelable, fired before mutation)
│   ├── Pre.Created   – party about to be created (no partyId yet)
│   └── Pre.Disbanded – party about to be disbanded
└── Post (informational, fired after successful mutation)
    ├── Post.Created
    ├── Post.Disbanded
    ├── Post.MemberJoined
    ├── Post.MemberLeft     – wasKicked() distinguishes kick vs. voluntary leave
    └── Post.RoleChanged
```

`Pre.Created#getPartyId()` is `null` (the party doesn't exist yet); every other event
carries a non-null id.

---

## Claim Lifecycle Events

### ChunkModifiedEvent

Same `Pre`/`Post` shape as `PartyEvent`, fired around chunk claim/unclaim/force-load
changes:

```java
@SubscribeEvent
public void onBeforeClaim(ChunkModifiedEvent.Pre.Claim e) {
    if (isInProtectedRegion(e.getChunkX(), e.getChunkZ())) e.setCanceled(true);
}

@SubscribeEvent
public void onClaimed(ChunkModifiedEvent.Post.Claim e) {
    LOGGER.info("Chunk ({}, {}) claimed by {}", e.getChunkX(), e.getChunkZ(), e.getOwnerUUID());
}
```

`Pre.Claim` / `Pre.Unclaim` / `Pre.ForceLoad` / `Pre.Unforce` are `@Cancelable`;
`Post.Claim` / `Post.Unclaim` / `Post.ForceLoad` / `Post.Unforce` fire after success.

---

## Addons Hub — Settings Panels

### IntegrationPanelRegistry

Add your own entry to the party menu's **Addons** hub — the searchable list of per-mod
settings panels (BetterQuesting, JourneyMap today). Requires a
[ModularUI](https://github.com/CleanroomMC/ModularUI) dependency, since the factory
builds a `ModularPanel`.

```java
// From a client-guarded init block — lazy method reference so the panel class
// is never loaded on a dedicated server.
if (event.getSide().isClient()) {
    IntegrationPanelRegistry.register(
        "mymod.addons.label",      // button label lang key
        "mymod.addons.tooltip",    // tooltip lang key, or null for no tooltip
        () -> true,                // availability predicate — hide the entry when false
        MySettingsPanel::build);   // UUID playerId -> ModularPanel
}
```

The entry appears automatically under the party menu's Addons entry once `available`
returns `true` — no changes to BLPC's own UI code are needed. See
`integration/bqu/BQuSettingsPanel.java` in BLPC's own source for a complete reference
implementation of a panel-backed entry.

If your integration doesn't need its own `ModularPanel` — e.g. it just opens another
mod's native settings screen — register an action-only entry instead with
`registerAction(...)`:

```java
IntegrationPanelRegistry.registerAction(
    "mymod.addons.label",
    null,                       // tooltip lang key, or null
    () -> true,                 // availability predicate
    MySettingsOpener::open);    // Runnable — no UUID/ModularPanel involved
```

`AddonsPanel` distinguishes the two via `Entry.hasPanel()`: panel entries open a
sub-panel through `IPanelHandler`, action entries just invoke `Runnable.run()` on click.
See `integration/jmap/JMapSettingsPanel.java`, which uses `registerAction(...)` to jump
straight to JourneyMap's own Addon Options screen.

---

## Module Framework

### @TModule / IModule

BLPC's own optional integrations (BetterQuesting, JourneyMap) are built on this
framework, and third-party mods can use it too for conditional, dependency-aware
feature loading — though a plain `@Mod` with its own lifecycle works just as well if you
don't need BLPC's config-gated enable/disable.

```java
@TModule(
    moduleID = "my_integration",
    containerID = "mymod",
    modDependencies = "othermod",   // skipped entirely if "othermod" isn't loaded
    name = "My Mod Integration",
    description = "Adds X when Other Mod is present.")
public class MyIntegrationModule implements IModule {

    @Override
    public void preInit(FMLPreInitializationEvent event) { ... }

    @Override
    public void init(FMLInitializationEvent event) { ... }

    @NotNull
    @Override
    public Logger getLogger() {
        return MyMod.LOGGER;
    }
}
```

Discovered automatically via ASM scanning at FML Construction — no manual registration
call. Lifecycle hooks run in FML-stage order (`construction` → `registerPackets` →
`preInit` → `init` → `postInit` → `loadComplete` → server start/stop hooks), each with a
no-op default so a module only overrides what it needs. `getDependencyUids()` can
require sibling modules by `ResourceLocation`; `modDependencies()` requires Forge mods by
ID. Users can still disable any module via `config/<modid>/modules.cfg` unless
`coreModule() = true`.

**Important:** if you use your own `containerID` (e.g. `containerID = "mymod"` instead
of `"blpc"`), that container **must** include exactly one module with
`coreModule = true` — otherwise `ModuleManager` throws an `IllegalStateException` at
FML Construction and the game will not start. If you only have one module, mark it as
the core module. If you register under `containerID = "blpc"` (BLPC's own container),
BLPC's own `CoreModule` already satisfies this requirement.

`@TModule(coreModule = true)` also means the module cannot be disabled via
`modules.cfg`, so use it deliberately, not by default.

---

## Utility helpers

- **`ModUtility.id(path)`** / **`ModUtility.blpcId(path)`** — build a
  `ResourceLocation` namespaced under BLPC's mod ID.
- **`Mods`** — lazy-cached `Loader.isModLoaded()` checks for BLPC's own soft
  dependencies (`BetterQuesting`, `ModularUI`, `JourneyMap`); `Mods.Names` holds the raw
  mod-ID strings. `Mods.Names.JOURNEY_MAP` presence alone isn't a version guarantee —
  BLPC's JourneyMap integration targets the v2 API and requires JourneyMap **v6+**
  (`@Mod`'s `after:journeymap@[1.12.2-6.0.0-beta.2,)` constraint); an older JourneyMap
  install loads as absent from the integration's perspective.
- **`EnumUtils.parseOrDefault(Class<E>, name, default)`** — `Enum.valueOf` that falls
  back to a default instead of throwing on an unrecognized or `null` name. Used
  throughout `api.party`'s `fromName` parsers; reach for it instead of writing another
  try/catch `valueOf`.
