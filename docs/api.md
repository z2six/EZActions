# EZActions API Reference

The public API package is:

`org.z2six.ezactions.api`

This document reflects the current NeoForge branch implementation.

## Access

```java
import org.z2six.ezactions.api.EzActions;
import org.z2six.ezactions.api.EzActionsApi;

EzActionsApi api = EzActions.get();
```

## Top-level API

`EzActionsApi` exposes:

- UI/runtime
  - `openEditor(Screen parent)`
  - `openConfig(Screen parent)`
  - `openRadial()`
  - `openRadialAtBundle(String bundleId)`
  - `openTemporaryRadial(String jsonItemOrArray, DynamicRadialStyle styleOrNull)`
- Legacy mutators
  - `addAction(..., IClickAction action, IconSpec iconOrNull, boolean locked)`
  - `addBundle(..., IconSpec iconOrNull, hideFromMainRadial, bundleKeybindEnabled, boolean locked)`
  - `removeItem(...)`
  - `moveWithin(...)`
  - `persist()`
  - `importFromClipboard()`
  - `exportToClipboard()`
- Structured surfaces
  - `menuRead()`
  - `menuWrite()`
  - `importExport()`
  - `inputOps()`
  - `editorOps()`
  - `events()`

## MenuRead

- `list(MenuPath)`
- `findById(String)`
- `currentPath()`
- `existsPath(MenuPath)`

`ApiMenuItem` includes:

- item basics: `id`, `title`, `note`, `isCategory`, `typeLabel`
- icon: `iconKind`, `iconId`
- bundle/item flags: `hideFromMainRadial`, `bundleKeybindEnabled`, `locked`
- action details:
  - `actionType`
  - `actionJson` (full serialized action payload)
  - key actions: `keyMappingName`, `keyToggle`, `keyMode`
  - command actions: `commandRaw`, `commandDelayTicks`, `commandCycleCommands`
  - item equip actions: `itemEquipSlotsJson`

## MenuWrite

- `addAction(MenuPath, title, note, IClickAction, locked)`
- `addAction(MenuPath, title, note, IconSpec, IClickAction, locked)`
- `addBundle(MenuPath, title, note, IconSpec, hideFromMainRadial, bundleKeybindEnabled, locked)`
- `moveWithin(MenuPath, from, to)`
- `moveTo(itemId, targetBundle)`
- `removeFirst(MenuPath, Predicate<ApiMenuItem>)`
- `removeById(id)`
- `updateMeta(id, titleOrNull, noteOrNull, iconOrNull)`
- `replaceAction(id, IClickAction)`
- `setBundleFlags(id, hideFromMainRadial, bundleKeybindEnabled)`
- `setLocked(id, locked)`
- `ensureBundles(MenuPath)`
- `upsertFromJson(MenuPath, jsonObjectOrArray)`

All mutators persist immediately.

Lock behavior:
- locked entries cannot be removed via `removeById`, `removeFirst`, or in-game editor delete.
- they can still be removed by manually editing JSON.

## ImportExport

- `exportAllJson()`
- `exportBundleJson(MenuPath)`
- `importInto(MenuPath, jsonObjectOrArray)`
- `replaceAll(jsonObjectOrArray)`
- `validate(json)`

The JSON schema used by API import/export is the same shape as EZActions live menu JSON (`menu.json` / clipboard import-export).

## InputOps

- `deliver(mappingNameOrLabel, toggle, mode)` where mode is `AUTO`, `INPUT`, `TICK`
- `enqueueCommands(commands, perLineDelayTicks)`

## EditorOps

- `openEditor()`
- `openConfig()`
- `openRadial()`
- `openRadialAtBundle(bundleId)`
- `openTemporaryRadial(jsonItemOrArray, DynamicRadialStyle)`

`openTemporaryRadial`:
- accepts the same item schema used by menu JSON (single object or array).
- does not persist anything to disk.
- supports optional per-open style overrides:
  - colors: `ringColor`, `hoverColor`, `borderColor`, `textColor`
  - animations: toggles, `openCloseMs`, `hoverGrowPct`, `openStyle`, `openDirection`, `hoverStyle`
  - design/layout: `designStyle`, `sliceGapDeg`, and deadzone/radius/thickness scaling knobs

Bundle keybind behavior:
- when an API write introduces keybind-enabled bundles that are not registered this session,
  EZActions shows a client chat message that a restart is required.

## Events

`ApiEvents` provides simple client-thread callbacks:

- `onMenuChanged(...)`
- `onImported(...)`

These are fired by API mutating/import operations.
