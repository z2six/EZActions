package com.z2six.ezactions.api;

import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.data.click.IClickAction;

public interface EzActionsApi {

    /** Opens the Menu Editor UI (same screen you already have). */
    void openEditor(Screen parent);

    // ---- Batch 1 (kept) ----

    String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action);

    String addBundle(String parentIdOrNull, String title, String noteOrNull);

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
}
