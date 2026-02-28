package org.z2six.ezactions.api;

import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.api.events.ApiEvents;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;

public interface EzActionsApi {

    /** Opens the Menu Editor UI (same screen you already have). */
    void openEditor(Screen parent);

    /** Opens the Config UI. */
    void openConfig(Screen parent);

    /** Opens the radial at root, same behavior as the hold hotkey open action. */
    void openRadial();

    /** Opens the radial at the specified bundle id (falls back to root when not found). */
    void openRadialAtBundle(String bundleId);

    /** Opens a temporary radial defined by JSON (same schema as menu JSON root array/item). */
    boolean openTemporaryRadial(String jsonItemOrArray, DynamicRadialStyle styleOrNull);

    // ---- Batch 1 (kept) ----

    default String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action) {
        return addAction(parentIdOrNull, title, noteOrNull, action, null, false);
    }

    default String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action, boolean locked) {
        return addAction(parentIdOrNull, title, noteOrNull, action, null, locked);
    }

    String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action, IconSpec iconOrNull, boolean locked);

    default String addBundle(String parentIdOrNull, String title, String noteOrNull) {
        return addBundle(parentIdOrNull, title, noteOrNull, null, false, false, false);
    }

    default String addBundle(String parentIdOrNull, String title, String noteOrNull,
                             boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked) {
        return addBundle(parentIdOrNull, title, noteOrNull, null, hideFromMainRadial, bundleKeybindEnabled, locked);
    }

    String addBundle(String parentIdOrNull, String title, String noteOrNull, IconSpec iconOrNull,
                     boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked);

    boolean removeItem(String id);

    boolean moveWithin(String parentIdOrNull, int fromIndex, int toIndex);

    void persist();

    int importFromClipboard();

    boolean exportToClipboard();

    // ---- Batch 2 accessors ----

    /** Read-only API surface. */
    MenuRead menuRead();

    /** Mutating API surface (persisted immediately). */
    MenuWrite menuWrite();

    /** JSON import/export helpers (full tree or slices). */
    ImportExport importExport();

    /** Input and command delivery operations. */
    InputOps inputOps();

    /** Editor + runtime UI operations. */
    EditorOps editorOps();

    /** API-level event hooks. */
    ApiEvents events();
}
