# Main Menu Editor GUI

The Menu Editor is your control center for building the radial.

Open it with the `Open editor` keybind.

## Layout

- **Left panel:** create/edit/remove actions and bundles.
- **Right panel:** current page list (root or current bundle).
- **Top-left filter:** filters by title, note, and action type text.
- **Bottom-left:** import/export, config, close.

## Add Buttons

- `Add Key Action`
- `Add Command`
- `Add Item Equip`
- `Add Bundle`

## List Interaction

### Mouse

- **LMB on item:** select.
- **LMB drag item:** reorder inside current page.
- **LMB drag item onto bundle row:** move item into that bundle.
- **LMB drag item onto back rows:** move item to parent/root.
- **RMB on bundle row:** open that bundle.

### Keyboard

- `Ctrl + F` focuses the filter box.
- `Enter` edits selected row.
- `Delete` or `Backspace` removes selected row.
- `Up Arrow` moves selected item up.
- `Down Arrow` moves selected item down.

!!! tip
    Keyboard move up/down is disabled while filter text is active, to avoid ambiguous reorders.

## Row Types You Will See

- Normal item rows (actions or bundles)
- Breadcrumb row (`root/.../bundle`)
- `Back to root`
- `Back to <parent>`

## Locked Entries

Locked entries are protected from in-game delete operations.

- You cannot remove them with GUI delete.
- You cannot remove them with API remove calls that honor lock.
- They can still be removed by manually editing `config/ezactions/menu.json`.

## Tips for Fast Editing

- Use short titles for cleaner radial labels.
- Put shared utility actions in bundles (for less root clutter).
- Use the filter box as a quick type-search when your menu gets huge.

???+ info "Deep dive: drag and drop behavior"
    - Reorder uses insertion logic (not simple swap).
    - Dropping into a bundle appends to that bundle's child list.
    - Dropping to parent/root keeps your current editor view instead of auto-jumping.
    - All successful move/remove operations persist immediately to disk.