# Using EZActions

EZActions introduces a radial menu that opens on a configurable key (default: backtick `).

---

## Opening the Radial

- **Open:** Hold the *Open EZActions* key (default: backtick `).
- **Navigate:** Move your mouse toward the desired item.
- **Trigger:** Release the key while hovering an item.
- **Cancel:** Move to the center or release outside the wheel.
- **Back:** If you’re inside a bundle (category), a “Back” item appears at the edge of the menu.

---

## Basic Controls

| Action | Default Key | Description |
|--------|--------------|-------------|
| Open EZActions | ` | Opens the radial menu |
| Select item | Mouse movement | Move toward the item |
| Trigger action | Release ` | Executes the highlighted action |
| Open Editor | Unbound | Opens the Menu Editor GUI |
| Back to root | Right-click | Returns to main radial page |

---

## Types of Actions

1. **Key Action:** Triggers any keybind (vanilla or modded).
2. **Command Action:** Runs one or more console commands in sequence.
3. **Bundle (Category):** Groups actions into sub-menus.

---

## Notes and Tooltips

Each action can include an optional *note* visible when hovering over it.  
For example:
> `/give @p minecraft:apple`  
> *Note: Quick food for testing.*

A small bookmark icon will appear if an action includes a note.

---

## Bundles (Nested Menus)

Bundles act as categories inside your radial.  
Right-click a bundle to enter it; a “Back” item appears automatically.

This allows complex setups such as:
Root Menu
├── Combat
│ ├── Sword Attack
│ ├── Bow Zoom
│ └── Shield Block
├── Utility
│ ├── Torch
│ └── Water Bucket
└── Commands
├── /home
└── /spawn


---

## Tips

- Use bundles to group similar functions (combat, building, etc.).
- Keep labels short — long names wrap in the radial.
- You can import/export full menus as text using the Menu Editor.
- 