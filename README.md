# EZActions

A fast, flexible radial action menu for Minecraft. Open a wheel of actions, then trigger keybinds, run commands, or enter bundles — all without leaving the game.

- **Hold-to-open radial:** default hotkey is the backtick (`) key.
- **No-conflict movement (optional):** keep moving while the menu is open.
- **Drag-and-drop editor:** reorder items, nest bundles, and manage icons.
- **Per-action delivery modes:** input injection strategies that work with stubborn keybinds and mods.
- **Import/Export:** share or back up your menu as text.

> Supports NeoForge on modern Minecraft. See **Installation** for exact versions.

---

## Quick Start

1. **Install** EZActions (see **Installation**).
2. Launch Minecraft and join a world.
3. Hold the **Open EZActions** key (default: backtick `) to open the radial.
4. Move the mouse to an item and release the key to trigger it.

Open the **Menu Editor** from the main screen (or via the optional “Open Editor” keybind) to add key actions, commands, and bundles.

---

## Features

- **Key Actions:** trigger a specific Key Mapping (e.g., Inventory, Zoom, Mod shortcuts).
- **Command Actions:** send one or multiple commands with per-line delay.
- **Bundles (Categories):** group actions into nested pages to keep complex setups clean.
- **Icons:** choose from items or other visuals for each action.
- **Notes:** add an optional tooltip per action.
- **Movement passthrough:** continue walking/jumping while the radial is open (toggle in config).
- **Import/Export:** copy your entire radial layout to/from the clipboard.

---

## Installation

- **Loader:** Forge & NeoForge
- **Minecraft:** 1.20.1, 1.21.1
- **Client-only:** the mod is designed for client use.

1. Download the appropriate **.jar** for your Minecraft + NeoForge version.
2. Place it in your `mods/` folder.
3. Start the game.

> Detailed, version-specific steps and troubleshooting: see the online docs:  
> **Installation:** `docs/installation.md`

---

## Using the Radial

- **Open:** hold the **Open EZActions** key (default: backtick `).
- **Navigate:** move the mouse to highlight an item.
- **Trigger:** release the key while an item is highlighted.
- **Bundles:** right-click an item labeled “(RMB to open)” to enter a nested page.
- **Back:** use the on-screen back rows to return to the parent/root.

> Full guide with tips, screenshots, and behavior details:  
> **Usage:** `docs/usage.md`

---

## Menu Editor

Open **Menu Editor** from the main screen (or bind an “Open Editor” key in controls).

- **Add Key Action:** map to a Key Mapping (search by category, readable labels).
- **Add Command:** send one or multiple commands (with delays).
- **Add Bundle:** create a category and drag items into it.
- **Drag & Drop:** reorder items; drop onto a bundle to move inside.
- **Import/Export:** move layouts via clipboard.

> Editor walkthrough and drag-and-drop behavior:  
> **Editor:** `docs/editor.md`

---

## Configuration

All options are available via **Config** from the Menu Editor:

- **General:** movement passthrough, visible lines in command editor.
- **Animations:** enable/disable, hover grow %, open/close timing.
- **Design:** radial size, ring thickness, scaling thresholds, colors (with picker).

> Every option explained with defaults and ranges:  
> **Configuration:** `docs/configuration.md`

---

## Keybinds

- **Open EZActions:** default **backtick (`)**
- **Open Editor:** **unbound** by default (bind it in Controls)

> Keybinding tips (e.g., reserved keys, conflicts):  
> **Keybinds:** `docs/keybinds.md`

---

## API

EZActions exposes a simple, versioned surface for two purposes:

1. **Import/Export Format** — share or back up your radial layouts.
2. **Action Delivery Modes** — control how actions are executed, useful for advanced setups or integrations.

Read the full API specification (schemas, examples, and stability notes):  
**API Reference:** `docs/api.md`

> Highlights:
> - **Menu JSON:** items, bundles, titles, notes, icons, and action payloads (`key`, `command`).
> - **Delivery Modes:** `AUTO`, `INPUT`, `TICK` — how EZActions triggers a key/command.
> - **Compatibility & Limits:** how the game’s input system affects reliability and what EZActions does about it (temporary bindings, modifier synthesis, etc.).

---

## Troubleshooting

- **Action doesn’t fire:** try `INPUT` mode or rebind the target action to a key with a valid scancode.
- **Movement stops in the radial:** enable “Move While Radial Open” in **General** config.
- **Nested bundles confusion:** use the breadcrumb rows and “Back to root/parent” actions.

See the detailed guide: **FAQ:** `docs/faq.md`

---

## Contributing

Bug reports, feature requests, and PRs are welcome.

- **How to build, branch, and submit PRs:** `docs/contributing.md`
- **Changelog:** `docs/changelog.md`

---

## License

This project is open source. See the license file and the docs page:
- `LICENSE` in the repo root
- `docs/license.md`

---

## Credits

EZActions by the project authors and contributors.  
Built for (Neo)Forge with care for reliability, crash-safety, and clean UX.
