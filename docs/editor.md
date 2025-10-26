# Menu Editor

The Menu Editor lets you create, organize, and modify your radial menus.

Access it from the **Main Menu** or bind the **Open Editor** key in Controls.

---

## Layout Overview

The editor consists of:
- **Left Panel:** action list (root or current category)
- **Right Panel:** item properties
- **Top Buttons:** Add, Remove, Import, Export
- **Bottom Bar:** navigation breadcrumbs

---

## Adding New Items

Click **Add** and choose one of the following:

### 1. Key Action
Triggers a key mapping (e.g., “Jump,” “Inventory,” or a mod key).

- **Target Key:** choose from all registered key mappings.
- **Delivery Mode:** how the key is simulated (see below).

### 2. Command Action
Executes one or more commands.
- **Command Lines:** each line is a command (e.g., `/home`).
- **Delay:** optional delay (ticks) between commands.

### 3. Bundle
Creates a nested category that can hold other actions.

---

## Delivery Modes

| Mode | Description |
|------|-------------|
| **AUTO** | Automatically picks the most reliable input method. |
| **INPUT** | Direct input simulation (for keybinds). |
| **TICK** | Runs the action during the next game tick (for commands). |

---

## Notes and Icons

- **Note:** Optional tooltip text. Appears in-game when hovering.
- **Icon:** Choose from Minecraft item textures or custom identifiers.

---

## Drag & Drop

- Drag items to reorder them.
- Drop onto a bundle to move the item inside.
- Drag to the breadcrumb bar to move back to a parent category.

---

## Import / Export

Use the **Import/Export** buttons to share or back up your entire menu layout.

- Exports produce a JSON or text representation of your menu.
- Imports replace the current menu layout with the new one.
