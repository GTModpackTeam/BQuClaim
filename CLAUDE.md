# CLAUDE.md

BLPC — Minecraft 1.12.2 Forge mod. Chunk claiming with party-based sharing. Optional BetterQuesting integration.

## Build

RetroFuturaGradle (RFG v2) + GTNH Buildscripts. **Do not edit `build.gradle`** (auto-generated). Config: `buildscript.properties`.

```bash
./gradlew build              # Full build (includes spotlessCheck)
./gradlew runClient          # Launch Minecraft client with the mod
./gradlew runServer          # Launch Minecraft server with the mod
./gradlew spotlessApply      # Auto-format code (run before committing)
./gradlew spotlessCheck      # Check formatting without fixing
./gradlew test               # Run JUnit 5 tests
```

## Key Rules

- **Java 25 syntax mandatory** (Jabel → JVM 8): switch expressions (`->`), pattern matching `instanceof`, `var` for obvious types. Details in `.claude/skills/blpc-overview/SKILL.md`.
- **Local builds need JDK 25**: spotless' googlejavaformat can't parse switch expressions on an older daemon JVM. If the Gradle daemon is an older Java, run with `-Dorg.gradle.java.home=<jdk25>` (e.g. `/usr/lib/jvm/zulu-25`). Compilation uses the Java 25 toolchain regardless.
- **Imports**: Always use `import` statements, not FQCN. Spotless enforces ordering.
- **GUI entry points**: open screens through `client/gui/Screens` (the single catalog — `openMap()`, `openPartyDirect()`, `partyMain(...)`), never `ClientGUI.open(new …)` ad-hoc. Reuse shared drawables from `client/gui/BLPCGuiTextures` (incl. `ICON_*` from ModularUI's `GuiTextures` atlas) instead of inlining drawables.
- **GUI colors**: No ModularUI theme system — BLPC ships a single **light** look with colors defined directly in Java. `client/gui/BLPCColors` holds the **semantic** party/map colors (`text()`, `owner()`, `admin()`, `warning()`, `subtext()`, `inactive()`, `divider()`, `mapBackground()`, `mapBorder()`, `textShadow()`) as fixed constants. `client/gui/GuiColors` holds **fixed vanilla-context** colors (`WHITE`/`GOLD`/`GREEN`/`RED`/`GRAY` for toasts, map counters, tooltips, map grid). Use these holders — never inline `0x…` literals (the only exceptions are dynamic per-party `getColor()` ARGB composition). Buttons use ModularUI's default theme; black party text reads against it. Visual changes need `runClient` to verify.
- **Network messages**: Wire protocol IDs are stable. C→S party operations multiplex through `PartyAction` (discriminator: `int action`). S→C client toasts/notifications multiplex through `ClientNotify` (discriminator: `int kind`). To add a new operation/notification, append a new `ACTION_*` / `KIND_*` constant — do **not** add a new top-level wire ID. Top-level IDs are only added for genuinely new message families (new sync stream, new C→S request shape) and must be appended to **both** `ModNetwork.CLIENT_BOUND_MESSAGES` (server-side NoOp registration) **and** `ClientPacketHandlers.installAll()` (client-side handler registration) in the **same order** — never insert into existing positions.
- **Side boundary**: Server-side handlers live in `common/network/`; client-side (S→C) handlers live in `client/network/` with `@SideOnly(Side.CLIENT)`. IMessage classes stay in `common/network/` and must not reference any `@SideOnly` types in their own bytecode.
- **JourneyMap integration**: requires JourneyMap v6+ (API v2). Use `journeymap.api.v2.*` packages, `@JourneyMapPlugin` (not `@ClientPlugin`), and subscribe to events via `ClientEventRegistry`/`FullscreenEventRegistry`/`CommonEventRegistry` (not `api.subscribe()`). See `.claude/skills/blpc-overview/SKILL.md` for details.
- **Key bindings**: category `key.categories.blpc`, two binds registered in `KeyInputHandler.init()` (called from `init`, FMLInitializationEvent) — `key.blpc.open_map` (M) and `key.blpc.open_party` (P), both `KeyConflictContext.IN_GAME`.

## Architecture

**Entry point for discovery:** `api/BLPCAPI` is the central façade and index (GregTech `GregTechAPI` analog) — read it first; it documents every subsystem and addon extension point. Public addon surface lives under `api/` (`modules/`, `party/`, `event/`, `util/`).

See `.claude/skills/blpc-overview/SKILL.md` for full reference (package layout, conventions, data schemas, UI patterns, config, etc.).
