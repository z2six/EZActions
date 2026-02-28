# API

This page documents the public API in `org.z2six.ezactions.api` for EZ Actions **2.0.0.0**.

Audience:

- mod developers
- modpack developers
- advanced automation users

## What The API Can Do

Short answer: everything users can do in the GUI, plus extra runtime control.

- Read menu tree
- Add/update/remove/reorder actions and bundles
- Mark entries as locked
- Import/export/validate menu JSON
- Open editor/config/radial screens
- Open radial directly at a bundle
- Open temporary runtime radials without persisting
- Trigger key input and command sequencing
- Subscribe to simple API events

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Core Concepts

### MenuPath

`MenuPath` addresses bundles by **bundle title chain** from root.

```java
MenuPath root = MenuPath.root();
MenuPath p = MenuPath.root().child("Utilities").child("Combat");
```

Notes:

- path matching is case-sensitive and title-literal.
- empty path = root list.

### Locked Entries

`locked=true` means protected from in-game delete flows and lock-aware API removals.

Locked entries can still be removed by manual JSON edits.

### Persistence Model

Most mutating API calls persist immediately.

You generally do not need an extra save call.

### Threading

API callbacks/events are designed for client thread usage.

## Action Types

### Key Action

```java
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.helper.InputInjector;

ClickActionKey keyAction = new ClickActionKey(
    "key.inventory",                    // mapping id or label
    false,                              // toggle
    InputInjector.DeliveryMode.AUTO     // AUTO/INPUT/TICK
);
```

### Command Action

```java
import org.z2six.ezactions.data.click.ClickActionCommand;

ClickActionCommand cmd = new ClickActionCommand(
    "/time set day\n/time set night", // multi-line
    10,                                 // delay ticks between lines
    true                                // cycleCommands
);
```

### Item Equip Action

```java
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.data.click.ClickActionItemEquip;

HolderLookup.Provider regs = Minecraft.getInstance().player.level().registryAccess();
ItemStack stack = Minecraft.getInstance().player.getMainHandItem();

ClickActionItemEquip.StoredItem stored =
    ClickActionItemEquip.StoredItem.fromStack(stack, regs);

ClickActionItemEquip eq = new ClickActionItemEquip(
    java.util.Map.of(ClickActionItemEquip.TargetSlot.MAINHAND, stored)
);
```

## Icons

```java
import org.z2six.ezactions.data.icon.IconSpec;

IconSpec itemIcon = IconSpec.item("minecraft:ender_pearl");
IconSpec customIcon = IconSpec.custom("custom:my_icon");
```

## Top-Level EzActionsApi

| Method | Purpose |
|---|---|
| `openEditor(Screen)` | Open in-game editor screen |
| `openConfig(Screen)` | Open config screen |
| `openRadial()` | Open root radial |
| `openRadialAtBundle(String)` | Open radial at bundle id |
| `openTemporaryRadial(String, DynamicRadialStyle)` | Open one-off runtime radial from JSON |
| `addAction(...)` | Legacy direct add action API |
| `addBundle(...)` | Legacy direct add bundle API |
| `removeItem(String)` | Legacy remove by id |
| `moveWithin(String,int,int)` | Legacy move in parent/root |
| `persist()` | Force persist |
| `importFromClipboard()` | GUI-style clipboard import |
| `exportToClipboard()` | GUI-style clipboard export |
| `menuRead()` | Read-only surface |
| `menuWrite()` | Mutating surface |
| `importExport()` | JSON import/export surface |
| `inputOps()` | Input + command helper surface |
| `editorOps()` | UI/runtime helper surface |
| `events()` | Event hooks |

## MenuRead

Interface: `MenuRead`

- `list(MenuPath path)`
- `findById(String id)`
- `currentPath()`
- `existsPath(MenuPath path)`

### ApiMenuItem Snapshot Fields

Basics:

- `id`, `title`, `note`
- `isCategory`, `typeLabel`
- `iconKind`, `iconId`
- `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`

Action details:

- `actionType`
- `actionJson`
- key action: `keyMappingName`, `keyToggle`, `keyMode`
- command action: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
- item equip action: `itemEquipSlotsJson`

## MenuWrite

Interface: `MenuWrite`

### Create

- `addAction(path, title, note, action, locked)`
- `addAction(path, title, note, icon, action, locked)`
- `addBundle(path, title, note, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `addBundle(path, title, note, icon, hideFromMainRadial, bundleKeybindEnabled, locked)`

### Move

- `moveWithin(path, fromIndex, toIndex)`
- `moveTo(itemId, targetBundle)`

### Remove

- `removeFirst(path, predicate)`
- `removeById(id)`

### Update

- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, action)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`

### Structural Helpers

- `ensureBundles(path)` creates missing bundle chain by title.
- `upsertFromJson(path, jsonObjectOrArray)` add/replace items from JSON snippet.

### Example: Create A Locked Utility Bundle

```java
var write = EzActions.get().menuWrite();

MenuPath root = MenuPath.root();
String bundleId = write.addBundle(
    root,
    "Utilities",
    "Pack-defined utilities",
    IconSpec.item("minecraft:shulker_box"),
    false,   // hideFromMainRadial
    true,    // bundleKeybindEnabled
    true     // locked
).orElseThrow();

write.addAction(
    root.child("Utilities"),
    "Open Inventory",
    "Quick inventory",
    IconSpec.item("minecraft:chest"),
    new ClickActionKey("key.inventory", false, InputInjector.DeliveryMode.AUTO),
    true
);
```

## ImportExport

Interface: `ImportExport`

- `exportAllJson()`
- `exportBundleJson(path)`
- `importInto(path, json)`
- `replaceAll(json)`
- `validate(json)`

### Validation Rules (high level)

- root must be object or array
- each item must contain exactly one of:
  - `action`
  - `children`
- action object must include valid `type`
- optional booleans (`hideFromMainRadial`, `bundleKeybindEnabled`, `locked`) must be booleans when present

## InputOps

Interface: `InputOps`

- `deliver(mappingNameOrLabel, toggle, mode)`
- `enqueueCommands(commands, perLineDelayTicks)`

Example:

```java
var input = EzActions.get().inputOps();

input.deliver("key.inventory", false, InputOps.Mode.AUTO);
input.enqueueCommands(new String[]{"/time set day", "/weather clear"}, 10);
```

## EditorOps

Interface: `EditorOps`

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, styleOrNull)`

## Dynamic Temporary Radial Style

Class: `DynamicRadialStyle`

All fields are optional nullable overrides.

Colors:

- `ringColor`
- `hoverColor`
- `borderColor`
- `textColor`

Animation:

- `animationsEnabled`
- `animOpenClose`
- `animHover`
- `openCloseMs`
- `hoverGrowPct`
- `openStyle` (`WIPE|FADE|NONE`)
- `openDirection` (`CW|CCW`)
- `hoverStyle` (`FILL_SCALE|FILL_ONLY|SCALE_ONLY|NONE`)

Design:

- `deadzone`
- `baseOuterRadius`
- `ringThickness`
- `scaleStartThreshold`
- `scalePerItem`
- `sliceGapDeg`
- `designStyle` (`SOLID|SEGMENTED|OUTLINE|GLASS`)

### Example: Temporary Runtime Radial

```java
String json = """
[
  {
    "id": "tmp_inv",
    "title": "Inventory",
    "icon": "minecraft:chest",
    "action": { "type": "KEY", "name": "key.inventory", "toggle": false, "mode": "AUTO" }
  },
  {
    "id": "tmp_day",
    "title": "Day",
    "icon": "minecraft:sunflower",
    "action": { "type": "COMMAND", "command": "/time set day", "delayTicks": 0, "cycleCommands": false }
  }
]
""";

DynamicRadialStyle style = new DynamicRadialStyle(
    0xAA000000, 0xFFF20044, 0x66FFFFFF, 0xFFFFFFFF,
    true, true, true,
    125, 0.05,
    "WIPE", "CW", "FILL_SCALE",
    18, 72, 28,
    8, 6, 0,
    "SOLID"
);

EzActions.get().editorOps().openTemporaryRadial(json, style);
```

## Events

Class: `ApiEvents`

- `onMenuChanged(Consumer<MenuChanged>)`
- `onImported(Consumer<ImportEvent>)`

Payloads:

- `MenuChanged.path`, `MenuChanged.reason`
- `ImportEvent.target`, `ImportEvent.json`, `ImportEvent.count`

Example:

```java
var events = EzActions.get().events();

events.onMenuChanged(evt -> {
    System.out.println("Menu changed: " + evt.reason + " at " + evt.path);
});

events.onImported(evt -> {
    System.out.println("Imported " + evt.count + " entries into " + evt.target);
});
```

## JSON Item Schema (API + Import/Export)

```json
{
  "id": "string",
  "title": "string or text component",
  "note": "string or text component",
  "icon": "minecraft:item_id",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": false,
  "locked": false,
  "action": {
    "type": "KEY | COMMAND | ITEM_EQUIP"
  },
  "children": []
}
```

Rules:

- use `action` for action items
- use `children` for bundles
- do not include both in the same object

## KubeJS-Style Interop Pattern

Exact syntax depends on your KubeJS setup, but flow is usually:

1. load Java API class
2. get singleton via `EzActions.get()`
3. call `menuWrite()` / `editorOps()` methods

Pseudo-flow:

```js
// pseudo example, adapt to your exact loader bridge
const EzActions = Java.loadClass('org.z2six.ezactions.api.EzActions')
const api = EzActions.get()

// use api.menuWrite(), api.importExport(), api.editorOps(), etc.
```

## API Notes For Pack Authors

- If your API writes enable bundle keybinds, users still need restart for keybind registration.
- EZ Actions now notifies users in chat when restart is required for new bundle keybinds.
- Keep stable ids/titles if you plan to patch menus over time.

???+ warning "Compatibility note"
    API signatures can change in future versions. This page matches 2.0.0.0 behavior.