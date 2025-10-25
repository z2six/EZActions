package org.z2six.ezactions.api;

import org.jetbrains.annotations.Nullable;

/** Read-only snapshot of a menu item for API consumers. */
public final class ApiMenuItem {
    private final String id;
    private final String title;
    private final boolean category;
    private final String typeLabel;    // "KEY", "COMMAND", or "BUNDLE"
    private final @Nullable String note;
    private final @Nullable String icon; // serialized icon spec (implementation-defined, opaque string)

    public ApiMenuItem(String id, String title, boolean category, String typeLabel, @Nullable String note, @Nullable String icon) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.typeLabel = typeLabel;
        this.note = note;
        this.icon = icon;
    }

    public String id() { return id; }
    public String title() { return title; }
    public boolean isCategory() { return category; }
    public String typeLabel() { return typeLabel; }
    public @Nullable String note() { return note; }
    public @Nullable String icon() { return icon; }
}
