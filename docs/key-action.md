# Key Action

Use a Key Action to fire an existing keybind (vanilla or modded) from the radial.

## Fields

- **Title**: what you see in editor/radial label.
- **Note**: optional helper text.
- **Mapping Name**: key mapping id or label.
- **Delivery**: `AUTO`, `INPUT`, or `TICK`.
- **Toggle**: toggle key state instead of tap.
- **Icon**: click icon box to open Icon Picker.

## Recommended Setup

1. Click `Pick from Keybinds`.
2. Select the binding from the picker (safer than typing by hand).
3. Keep delivery on `AUTO` unless you have a specific reason.
4. Save.

## Delivery Modes

| Mode | What it does | When to use |
|---|---|---|
| `AUTO` | Picks best path automatically | Default for almost everyone |
| `INPUT` | Injects key press/release through input pipeline | If a binding is not responding in `AUTO` |
| `TICK` | Sets key down/up via key state updates | Useful fallback for hard-to-inject keys |

## Toggle

- `OFF`: one tap per radial use.
- `ON`: flips key down/up each use.

Useful for actions like sprint/sneak style toggles, depending on how the target keybind behaves.

## Common Issues

- **Nothing happens:** check the mapping id or pick from keybind list again.
- **Wrong keybind triggers:** your typed name matched another mapping label; use picker.
- **Works in singleplayer but not server:** target action may be server-restricted.

???+ info "Deep dive: how key delivery works"
    Internally, EZ Actions resolves mapping names by:

    1. exact translation key match
    2. exact localized label match
    3. fallback contains match

    For modifier-required keys (Ctrl/Shift/Alt), it uses extra injection logic. `AUTO` can switch strategy depending on whether a key is bound, has a scancode, or requires modifiers.