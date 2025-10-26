# EZActions API Reference

The **EZActions API** allows other mods, tools, or scripts to interact with EZActions' menu system.  
It exposes ways to **read, import, export, and trigger actions** programmatically, as well as to understand the JSON format used for menu data.

---

## 1. Overview

EZActions is built on a **stable public API layer** found under the package:

`com.z2six.ezactions.api`

This API can be used to:

- Detect if EZActions is loaded.
- Retrieve and interact with the current menu.
- Import or export complete radial layouts as JSON.
- Define custom actions (key, command, or bundles).
- Hook into delivery logic (how actions are triggered).
- Extend compatibility for your own mods.

All of this is designed to be **cross-loader compatible** — the same calls work for both **Forge (1.20.1)** and **NeoForge (1.21.1)**.

---

## 2. API Access

To access the API, depend on EZActions in your mod and import the entry class:

```java
import com.z2six.ezactions.api.EzActions;
```

You can then retrieve the API instance or call its static helpers:

```java
EzActionsApi api = EzActions.get();  // or EzActions.API if exposed statically

api.importMenu(jsonText);
api.exportMenu(); // returns JSON string
```

> **Note:** The actual `EzActionsApi` interface and `EzActionsApiImpl` class are automatically loaded via Java’s ServiceLoader, so no explicit registration is required.

---

## 3. Detecting EZActions

You can check if EZActions is loaded before calling its API:

```java
if (Services.PLATFORM.isModLoaded("ezactions")) {
    // Safe to use EzActions API
}
```

On both loaders, this check returns `true` if the mod is active.

---

## 4. Menu Data Format (JSON)

EZActions menus are serialized into a structured JSON tree.  
Each node represents either a **menu item** (action) or a **bundle** (category).

### Example

```json
[
  {
    "type": "bundle",
    "title": "Combat",
    "children": [
      {
        "type": "key",
        "title": "Attack",
        "key": "key.attack",
        "icon": "minecraft:iron_sword"
      },
      {
        "type": "key",
        "title": "Block",
        "key": "key.use",
        "icon": "minecraft:shield"
      }
    ]
  },
  {
    "type": "command",
    "title": "Go Home",
    "command": "/home",
    "icon": "minecraft:ender_pearl"
  }
]
```

---

### Field Reference

| Field | Type | Description |
|--------|------|-------------|
| **type** | String | One of `"key"`, `"command"`, or `"bundle"`. |
| **title** | String | Text displayed in the radial segment. |
| **note** | String | Optional tooltip displayed when hovering. |
| **icon** | String | Optional icon resource (e.g. `"minecraft:iron_sword"`). |
| **key** | String | Minecraft key mapping ID (for key actions). |
| **command** | String or Array | Command(s) to run (without `/`). |
| **delay** | Integer | Optional delay between command lines (in ticks). |
| **children** | Array | Nested items (only for type `"bundle"`). |
| **delivery** | String | `"AUTO"`, `"INPUT"`, or `"TICK"`. Determines how the action executes. |

---

### Example with Notes and Delivery

```json
[
  {
    "type": "command",
    "title": "Feed Me",
    "command": ["/effect give @p saturation 1 10", "/say Yum!"],
    "delay": 10,
    "icon": "minecraft:apple",
    "note": "Gives instant saturation.",
    "delivery": "TICK"
  },
  {
    "type": "key",
    "title": "Toggle Zoom",
    "key": "key.zoom",
    "icon": "minecraft:spyglass",
    "delivery": "INPUT"
  }
]
```

---

## 5. Action Types

### a) Key Action

Triggers an existing key mapping in the client (e.g., “attack,” “jump,” or any mod key).

```json
{
  "type": "key",
  "title": "Attack",
  "key": "key.attack",
  "delivery": "INPUT"
}
```

- Simulates a key press for one tick.
- Works for both vanilla and mod keybinds.
- Best for quick, repeatable inputs.

---

### b) Command Action

Executes one or more chat commands as the player.

```json
{
  "type": "command",
  "title": "Heal",
  "command": ["/effect give @p instant_health 1"],
  "delivery": "TICK"
}
```

- Can contain multiple lines; each line executes in order.
- Optional `"delay"` defines the tick gap between commands.
- Commands are client-side simulated chat sends (no server permission bypass).

---

### c) Bundle (Category)

A container that holds other actions.  
Bundles appear as sub-menus in the radial.

```json
{
  "type": "bundle",
  "title": "Utility",
  "children": [
    { "type": "command", "title": "Home", "command": "/home" },
    { "type": "command", "title": "Spawn", "command": "/spawn" }
  ]
}
```

- Right-click or hover-release behavior opens bundles.
- A “Back” entry appears automatically in nested pages.

---

## 6. Delivery Modes

Delivery modes determine *how* EZActions executes a key or command.  
They are available via both the JSON and the API.

| Mode | Description | Suitable For |
|------|--------------|--------------|
| **AUTO** | Automatically selects between INPUT or TICK based on action type. | General use |
| **INPUT** | Simulates a physical key press on the client input system. | Keybinds or local actions |
| **TICK** | Queues the action to run in the next client tick. | Commands and macros |

Example:

```json
{
  "type": "key",
  "title": "Jump",
  "key": "key.jump",
  "delivery": "AUTO"
}
```

---

## 7. Importing and Exporting Menus

You can import or export complete menu structures via the API:

```java
EzActionsApi api = EzActions.get();

// Export
String json = api.exportMenu();

// Import
api.importMenu(json);
```

Menus can also be exported/imported through the in-game **Menu Editor**, using the same JSON structure described above.

---

## 8. Integration Examples

### Example 1: Detect and Register

```java
if (Services.PLATFORM.isModLoaded("ezactions")) {
    EzActionsApi api = EzActions.get();
    api.importMenu(customMenuJson);
}
```

### Example 2: Add an Action Programmatically

```java
EzActionsApi api = EzActions.get();

MenuItem newItem = MenuItem.key("key.attack")
    .withTitle("Quick Attack")
    .withIcon("minecraft:iron_sword")
    .withDelivery(DeliveryMode.INPUT);

api.addToRoot(newItem);
```

*(The builder-style API mirrors the JSON schema; methods and naming may vary slightly per version.)*

---

## 9. Runtime Environment

The API is safe to use:
- On **client side only**.
- When EZActions is active.
- During or after the mod loading phase (post-init).

Attempting to call it on the server side will result in a no-op or a safe warning.

---

## 10. Versioning and Stability

| Key | Description |
|------|-------------|
| **API Version** | Accessible through `EzActions.API_VERSION` |
| **Compatibility** | Stable across minor versions (e.g., 1.21.x → 1.21.y) |
| **Breaking Changes** | Only introduced on major API version changes |
| **Cross-Loader** | Identical behavior on Forge and NeoForge |

Example:

```java
String version = EzActions.API_VERSION;  // e.g. "1.0.0"
```

---

## 11. Extending EZActions

If you develop your own mod that adds new types of actions:
- You can serialize them into EZActions JSON by using custom `type` identifiers.
- Unknown `type` values are ignored gracefully to prevent crashes.
- Consider prefixing your types, e.g., `"myaddon:custom_action"`.

---

## 12. Future API Plans

Planned extensions include:
- Cross-loader Fabric/Quilt compatibility.
- Extended input simulation modes (e.g., hold/release durations).
- Event hooks for pre/post action execution.
- Radial style customization via JSON or API.
- Menu sync and sharing between players.

---

## 13. Summary

- The **EZActions API** lets you integrate your mod’s features with the radial system.
- Menu data is stored as structured JSON and can be imported/exported freely.
- All calls are **client-safe** and **cross-compatible** across loaders.
- Delivery modes define how actions execute for best reliability.

For examples, updates, and schema references, visit the official repository documentation.

[← Back to Home](index.md)
