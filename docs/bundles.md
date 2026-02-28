# Bundles

Bundles are folders (categories) inside the radial tree.

Use them to group actions by context: combat, building, utility, roleplay, etc.

## Bundle Fields

- **Title** (also used as internal id)
- **Note** (optional)
- **Icon**
- **Hide from main radial**
- **Enable keybind**

## Bundle Keybinds

If `Enable keybind` is on, EZ Actions registers a dedicated keybind for that bundle.

!!! warning "Restart required"
    Bundle keybind registration is applied on next client restart.

    EZ Actions shows a client message when restart is needed (including API-created bundles).

## Hide From Main Radial

If enabled:

- Bundle is hidden on root radial page.
- Bundle still exists in menu model.
- Bundle can still be opened via API or bundle keybind.

Good for "advanced pages" you do not want cluttering root.

## Nested Bundles

Bundles can contain:

- key actions
- command actions
- item equip actions
- more bundles

## Best Practices

- Keep root small and high-priority.
- Put low-frequency actions in deeper bundles.
- Give bundles clear icons and short names.

## Locked Bundles

A bundle can be marked `locked` (usually via API or JSON).

- In-game delete paths will not remove it.
- Manual JSON edits can still remove it.

???+ info "Deep dive: identity and uniqueness"
    Bundle title is used as bundle id in editor workflows.

    Duplicate bundle names can cause ambiguity, so the editor blocks duplicate bundle title/id saves.