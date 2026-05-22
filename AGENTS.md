# EZActions — Agent Guide

## Project Overview

EZActions is a Minecraft mod (1.21.1) for **NeoForge** and **Fabric** that provides a radial menu for keybinds, multi-command execution, item-equip loadouts, and more. Java 21.

## Quick Start

```bash
# Build all modules
./gradlew build

# Run NeoForge client
./gradlew :neoforge:runClient

# Run Fabric client
./gradlew :fabric:runClient

# Generate data (NeoForge only)
./gradlew :neoforge:runData
```

## Project Structure

| Module | Purpose |
|--------|---------|
| `common/` | Shared code: constants, services SPI, mixin config |
| `fabric/` | Fabric loader entrypoint, `fabric.mod.json` |
| `neoforge/` | **Main module** — all gameplay code, GUI, config, API |
| `forge/` | Legacy, **not included** in `settings.gradle` (inactive) |
| `buildSrc/` | Custom Gradle plugins: `multiloader-common`, `multiloader-loader` |
| `docs/` | MKDocs wiki (i18n, Material theme) |

## Architecture Rules

1. **`common/`** — Vanilla-only imports. No loader-specific APIs (no NeoForge events, no Fabric API). Use `ServiceLoader` SPI for platform abstractions.
2. **`neoforge/`** — All gameplay code lives here (GUI, handlers, config, API, mixins). The Fabric module delegates to NeoForge's mixin classes via shared mixin config.
3. **Platform abstraction** — `IPlatformHelper` (in `common`) → `FabricPlatformHelper` / `NeoForgePlatformHelper` via `META-INF/services/`.
4. **All config** uses NeoForge's `ModConfig.Type.CLIENT` with TOML files:
   - `anim-client.toml`, `general-client.toml`, `design-client.toml`
5. **Mixins** — Configs in `common/` (`ezactions.mixins.json`), `neoforge/` (`ezactions.neoforge.mixins.json`), `fabric/` (`ezactions.fabric.mixins.json`).

## Key Packages (neoforge module)

| Package | Responsibility |
|---------|---------------|
| `api/` | Public API (`EzActionsApi`, events, import/export) |
| `config/` | TOML config specs |
| `data/menu/` | Menu model (`RadialMenu`, `MenuItem`) |
| `data/click/` | Click action types (command, key, item-equip) |
| `gui/` | Radial screen, drawing, math, animations |
| `handler/` | `KeyboardHandler` — hold-to-open radial logic |
| `helper/` | Input injection, item equip executor, task queue |
| `util/` | Keybinds, clipboard IO, command sequencer, bundle hotkeys, pinyin search |
| `init/` | Client overlay registration |

## Key Design Decisions

- **Hold-to-open** radial (not toggle). Release executes the hovered action.
- **Movement passthrough** while radial is open (configurable).
- **Per-bundle hotkeys** supported alongside the main radial hotkey.
- **No blur** on radial screen (uses `NoMenuBlurScreen` interface + mixin).
- **Animations** configurable (open/close wipe, hover grow, styles).
- **Icons** — Custom icon manager with `ensureFolderReady()`.
- **Config** is NeoForge `ModConfig.Type.CLIENT` registered programmatically (not via annotations).

## API Package

`org.z2six.ezactions.api` provides a stable API for modpack makers and other mods to:
- Add/remove menu items programmatically
- Open the radial at specific bundles
- Open temporary radials from JSON
- Import/export configurations

## Build System

- Gradle with `buildSrc/` convention plugins.
- Version properties in `gradle.properties` (single source of truth).
- `fabric-loom` 1.8-SNAPSHOT for Fabric, `net.neoforged.moddev` 2.0.49-beta for NeoForge.
- Parchment mappings (1.21 / 2024.11.10).
- Prefer `./gradlew` wrapper over direct Gradle installs.

## Documentation

- Wiki: https://z2six.github.io/EZActions/
- Full docs in `docs/` as Markdown (MKDocs Material theme + i18n).
- Key doc pages: `menu-editor.md`, `key-action.md`, `command-action.md`, `bundles.md`, `configuration.md`, `api.md`.

## Potential Pitfalls

- The **Forge module is inactive** — it's NOT in `settings.gradle`. Do not modify it unless explicitly asked.
- `RadialMenu.TemporaryStyle` is a public inner class used by the API for temporary radials.
- `KeyboardHandler.suppressReopenUntilReleased()` must be called when executing actions on release to prevent re-trigger.
- Config specs use `ModConfig.Type.CLIENT` — do not switch to SERVER or COMMON.
- `Config.java` in neoforge is a minimal scaffold; actual config lives in the `config/` subpackage with NeoForge specs.
