# EZ Actions Wiki

EZ Actions is a client-side Minecraft mod that gives you a fast radial menu for actions you use all the time.

Think of it as your "combat + utility quick wheel": one hold key, one flick, done.

!!! warning "Version scope"
    This wiki is written for **EZ Actions 2.0.0.0**.

    If you are using a newer version, some features and UI details may have changed.

???+ info "TLDR"
    - Build your own radial menu with **Key Actions**, **Command Actions**, **Item Equip Actions**, and **Bundles**.
    - Style it with color/design/animation configs.
    - Import/export menu JSON for sharing and backups.
    - Mod developers can control everything through the API (including temporary runtime radials).

## What EZ Actions Can Do

- Trigger vanilla or modded keybinds.
- Run single or multi-line commands.
- Equip recorded gear sets using exact item matching (NBT included).
- Organize actions into nested bundles.
- Hide bundles from root while keeping them accessible via bundle keybind.
- Add custom icons from `config/ezactions/icons`.
- Build/edit menus in-game with drag/drop and keyboard shortcuts.
- Let other mods drive EZ Actions through the public API.

## Who This Wiki Is For

- Players who want a clear setup guide without reading source code.
- Power users who want advanced behavior details.
- Modpack makers and mod developers who want complete API docs.

You will see expandable "deep dive" sections in most pages. Skip them if you just want the practical flow.

## Quick Start

1. Set keybinds for:
   - `Open radial menu`
   - `Open editor`
2. Open the editor and add your first action.
3. Hold your radial key in-game and release over a slice to execute.
4. Tune visuals in the Config screen.

## Navigation

Use the left nav for full docs:

- Main Menu Editor GUI
- Key Action
- Command Action
- Item Equip Action
- Bundles
- Import Export
- Configuration
- API

??? "Technical note"
    EZ Actions is fully client-side. It does not require server installation.

    Actions still depend on what the server allows (for example command permissions).