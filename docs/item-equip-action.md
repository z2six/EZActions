# Item Equip Action

Item Equip lets you save a gear snapshot and re-equip matching items from your inventory.

## Target Slots

You can assign any of these:

- Mainhand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

If a slot is left empty in the editor, EZ Actions skips it during execution.

## How To Create One

1. Click `Add Item Equip`.
2. Set title/note/icon.
3. Drag items from source grid into target slots.
4. Save.

### Source Grid Includes

- current offhand item
- equipped armor
- main inventory
- hotbar

## During Execution

When you trigger the action from radial:

1. EZ Actions checks each configured target slot.
2. If target already matches recorded item, it skips.
3. If not, it finds best matching source stack.
4. It swaps items into target slot.

It processes slot-by-slot and allows partial success.

## Important Matching Rules

- Matching is based on full stack snapshot signature (NBT and metadata), ignoring count.
- If multiple matching stacks exist, it picks the one with highest count.

## Mainhand Rule

`Mainhand` means your currently selected hotbar slot at trigger time.

## Behavior Under Pressure

- If you trigger a second Item Equip action while one is running, old one is canceled and replaced.
- Movement and gameplay inputs remain active while execution runs in background ticks.

## Quick Controls In Editor

- LMB drag from source to target slot: assign item.
- RMB on target slot: clear assignment.
- `Refresh Items`: rebuild source list from current player inventory state.

???+ info "Deep dive: slot execution order"
    Current execution order is:

    1. Helmet
    2. Chestplate
    3. Leggings
    4. Boots
    5. Offhand
    6. Mainhand

    Empty targets and missing source matches are skipped, not treated as hard failure.