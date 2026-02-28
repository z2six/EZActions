package org.z2six.ezactions.api.events;

import org.z2six.ezactions.api.MenuPath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lightweight event hooks for API consumers (no external bus dependency).
 * All callbacks are invoked on the client thread.
 */
public final class ApiEvents {

    public static final class MenuChanged {
        public final MenuPath path;
        public final String reason; // e.g. "move", "remove", "import", "replace"
        public MenuChanged(MenuPath path, String reason) { this.path = path; this.reason = reason; }
    }

    public static final class ImportEvent {
        public final MenuPath target;
        public final String json;
        public final int count;
        public ImportEvent(MenuPath target, String json, int count) {
            this.target = target; this.json = json; this.count = count;
        }
    }

    private final List<Consumer<MenuChanged>> menuChanged = new ArrayList<>();
    private final List<Consumer<ImportEvent>>  imported    = new ArrayList<>();

    public void onMenuChanged(Consumer<MenuChanged> cb) { if (cb != null) menuChanged.add(cb); }
    public void onImported(Consumer<ImportEvent> cb) { if (cb != null) imported.add(cb); }

    // impl side triggers
    public void _fireChanged(MenuPath p, String why) {
        if (menuChanged.isEmpty()) return;
        MenuChanged evt = new MenuChanged(p, why);
        for (var cb : List.copyOf(menuChanged)) try { cb.accept(evt); } catch (Throwable ignored) {}
    }
    public void _fireImported(MenuPath p, String json, int n) {
        if (imported.isEmpty()) return;
        ImportEvent evt = new ImportEvent(p, json, n);
        for (var cb : List.copyOf(imported)) try { cb.accept(evt); } catch (Throwable ignored) {}
    }
}
