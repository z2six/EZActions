package org.z2six.ezactions.api.internal;

import org.jetbrains.annotations.Nullable;
import org.z2six.ezactions.api.ApiMenuItem;
import org.z2six.ezactions.data.click.ClickActionCommand;
import org.z2six.ezactions.data.click.ClickActionItemEquip;
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;

/** Internal mapper from runtime menu model to API snapshots. */
final class ApiMapper {
    private ApiMapper() {}

    static ApiMenuItem toApi(MenuItem mi) {
        String typeLabel = "ACTION";
        String actionType = null;
        String actionJson = null;
        String keyMappingName = null;
        boolean keyToggle = false;
        String keyMode = null;
        String commandRaw = null;
        int commandDelayTicks = 0;
        boolean commandCycleCommands = false;
        String itemEquipSlotsJson = null;

        if (mi != null && mi.isCategory()) {
            typeLabel = "BUNDLE";
        } else if (mi != null) {
            IClickAction action = mi.action();
            if (action != null) {
                actionType = action.getType().name();
                typeLabel = actionType;
                try { actionJson = action.serialize().toString(); } catch (Throwable ignored) {}
                if (action instanceof ClickActionKey kk) {
                    keyMappingName = kk.mappingName();
                    keyToggle = kk.toggle();
                    keyMode = kk.mode() == null ? null : kk.mode().name();
                } else if (action instanceof ClickActionCommand cc) {
                    commandRaw = cc.getCommand();
                    commandDelayTicks = cc.getDelayTicks();
                    commandCycleCommands = cc.isCycleCommands();
                } else if (action instanceof ClickActionItemEquip eq) {
                    try {
                        itemEquipSlotsJson = eq.serialize().getAsJsonObject("slots").toString();
                    } catch (Throwable ignored) {}
                }
            }
        }

        IconSpec icon = mi == null ? null : mi.icon();
        String iconKind = icon == null ? null : icon.kind().name();
        String iconId = icon == null ? null : icon.id();

        return new ApiMenuItem(
                safe(mi == null ? null : mi.id()),
                safe(mi == null ? null : mi.title()),
                mi != null && mi.isCategory(),
                typeLabel,
                nullable(mi == null ? null : mi.note()),
                iconKind,
                iconId,
                mi != null && mi.hideFromMainRadial(),
                mi != null && mi.bundleKeybindEnabled(),
                mi != null && mi.locked(),
                actionType,
                actionJson,
                keyMappingName,
                keyToggle,
                keyMode,
                commandRaw,
                commandDelayTicks,
                commandCycleCommands,
                itemEquipSlotsJson
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static @Nullable String nullable(String s) { return s; }
}
