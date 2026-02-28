package org.z2six.ezactions.api;

import org.jetbrains.annotations.Nullable;

/** Read-only snapshot of a menu item for API consumers. */
public final class ApiMenuItem {
    private final String id;
    private final String title;
    private final boolean category;
    private final String typeLabel;    // action type name or "BUNDLE"
    private final @Nullable String note;
    private final @Nullable String iconKind;
    private final @Nullable String iconId;
    private final boolean hideFromMainRadial;
    private final boolean bundleKeybindEnabled;
    private final boolean locked;
    private final @Nullable String actionType;     // "KEY" / "COMMAND" for actions, null for bundles
    private final @Nullable String actionJson;     // full action payload as JSON
    private final @Nullable String keyMappingName; // key action only
    private final boolean keyToggle;               // key action only
    private final @Nullable String keyMode;        // key action only (AUTO/INPUT/TICK)
    private final @Nullable String commandRaw;     // command action only
    private final int commandDelayTicks;           // command action only
    private final boolean commandCycleCommands;    // command action only
    private final @Nullable String itemEquipSlotsJson; // item-equip action only

    public ApiMenuItem(String id, String title, boolean category, String typeLabel, @Nullable String note,
                       @Nullable String iconKind, @Nullable String iconId,
                       boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked,
                       @Nullable String actionType,
                       @Nullable String actionJson,
                       @Nullable String keyMappingName, boolean keyToggle, @Nullable String keyMode,
                       @Nullable String commandRaw, int commandDelayTicks, boolean commandCycleCommands,
                       @Nullable String itemEquipSlotsJson) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.typeLabel = typeLabel;
        this.note = note;
        this.iconKind = iconKind;
        this.iconId = iconId;
        this.hideFromMainRadial = hideFromMainRadial;
        this.bundleKeybindEnabled = bundleKeybindEnabled;
        this.locked = locked;
        this.actionType = actionType;
        this.actionJson = actionJson;
        this.keyMappingName = keyMappingName;
        this.keyToggle = keyToggle;
        this.keyMode = keyMode;
        this.commandRaw = commandRaw;
        this.commandDelayTicks = commandDelayTicks;
        this.commandCycleCommands = commandCycleCommands;
        this.itemEquipSlotsJson = itemEquipSlotsJson;
    }

    public String id() { return id; }
    public String title() { return title; }
    public boolean isCategory() { return category; }
    public String typeLabel() { return typeLabel; }
    public @Nullable String note() { return note; }
    public @Nullable String iconKind() { return iconKind; }
    public @Nullable String iconId() { return iconId; }
    public boolean hideFromMainRadial() { return hideFromMainRadial; }
    public boolean bundleKeybindEnabled() { return bundleKeybindEnabled; }
    public boolean locked() { return locked; }
    public @Nullable String actionType() { return actionType; }
    public @Nullable String actionJson() { return actionJson; }
    public @Nullable String keyMappingName() { return keyMappingName; }
    public boolean keyToggle() { return keyToggle; }
    public @Nullable String keyMode() { return keyMode; }
    public @Nullable String commandRaw() { return commandRaw; }
    public int commandDelayTicks() { return commandDelayTicks; }
    public boolean commandCycleCommands() { return commandCycleCommands; }
    public @Nullable String itemEquipSlotsJson() { return itemEquipSlotsJson; }
}
