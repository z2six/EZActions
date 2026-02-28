# Import Export

EZ Actions import/export works through your clipboard.

## Export

In Menu Editor, click `Export`.

Result:

- Current full root tree is serialized to JSON.
- JSON is copied to clipboard.

## Import

In Menu Editor, click `Import`.

Result:

- Clipboard JSON is parsed and validated.
- On success, imported entries are added/replaced per import path.

## Common Error Messages

- Clipboard is empty
- Clipboard is not JSON
- Root JSON is not array
- Entry is not object / invalid

## Practical Workflow

1. Export current menu to a text file as backup.
2. Test edits in JSON.
3. Import.
4. If needed, rollback by importing previous backup.

## JSON Shape

Top-level supports array of menu items (or single item in some API paths).

Each menu item must be either:

- an **action** item with `action` object
- a **bundle** item with `children` array

### Minimal Action Example

```json
{
  "id": "act_123",
  "title": "Inventory",
  "icon": "minecraft:chest",
  "action": {
    "type": "KEY",
    "name": "key.inventory",
    "toggle": false,
    "mode": "AUTO"
  }
}
```

### Minimal Bundle Example

```json
{
  "id": "bundle_abc",
  "title": "Utilities",
  "icon": "minecraft:shulker_box",
  "hideFromMainRadial": false,
  "bundleKeybindEnabled": true,
  "locked": false,
  "children": []
}
```

???+ info "Deep dive: schema details"
    - `title` and `note` accept plain string or text component JSON.
    - `locked` is optional; defaults false.
    - `action.type` currently supports `KEY`, `COMMAND`, `ITEM_EQUIP`.
    - `KEY` fields: `name`, `toggle`, `mode`.
    - `COMMAND` fields: `command`, `delayTicks`, `cycleCommands`.
    - `ITEM_EQUIP` fields: `slots` map with stored item snapshots.