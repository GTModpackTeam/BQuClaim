# AGENTS.md

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

Enforced conventions (Java 25 syntax, imports, GUI colors/entry points, network wire protocol, side boundary, key bindings, logging, integration rules) live in `.claude/rules/*.md` — read those, not this file, for anything build-breaking or merge-blocking. One environment note not covered there: **local builds need JDK 25** — spotless' googlejavaformat can't parse switch expressions on an older daemon JVM. If the Gradle daemon is an older Java, run with `-Dorg.gradle.java.home=<jdk25>` (e.g. `/usr/lib/jvm/zulu-25`); compilation uses the Java 25 toolchain regardless.

## Architecture

**Entry point for discovery:** `api/BLPCAPI` is the central façade and index (GregTech `GregTechAPI` analog) — read it first; it documents every subsystem and addon extension point. Public addon surface lives under `api/` (`modules/`, `party/`, `event/`, `util/`).

See `.claude/skills/blpc-overview/SKILL.md` for full reference (package layout, conventions, data schemas, UI patterns, config, etc.).
