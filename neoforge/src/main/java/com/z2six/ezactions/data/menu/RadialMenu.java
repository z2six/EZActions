// MainFile: src/main/java/org/z2six/ezactions/data/menu/RadialMenu.java
package org.z2six.ezactions.data.menu;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.json.MenuLoader;
import org.z2six.ezactions.gui.RadialMenuScreen;

import java.util.*;

/**
 * Holds the menu model and opens the radial as a Screen (mouse free, gameplay input blocked).
 * Visual blur is disabled for our screens via the NoBlur mixin.
 */
public final class RadialMenu {

    private static List<MenuItem> ROOT = new ArrayList<>();
    // PATH is maintained root -> ... -> deepest (append when entering, remove last when going back)
    private static final Deque<MenuItem> PATH = new ArrayDeque<>();

    private RadialMenu() {}

    /** Open the radial as a Screen, always starting at ROOT. */
    public static void open() {
        try {
            // --- Guard: only open while actively playing (no GUI, not paused, world ready) ---
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                Constants.LOG.debug("[{}] Radial open ignored: client/world not ready (mc={}, player={}, level={}).",
                        Constants.MOD_NAME,
                        (mc != null),
                        (mc != null && mc.player != null),
                        (mc != null && mc.level != null));
                return;
            }
            if (mc.screen != null || mc.isPaused()) {
                final String scr = (mc.screen == null) ? "none" : mc.screen.getClass().getSimpleName();
                Constants.LOG.debug("[{}] Radial open ignored: screen={}, paused={}",
                        Constants.MOD_NAME, scr, mc.isPaused());
                return;
            }
            // -------------------------------------------------------------------------------

            ensureLoaded();
            PATH.clear(); // important: always open at root
            mc.setScreen(new RadialMenuScreen());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to open radial: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /** Manually reset to root (used by editor or tests). */
    public static void resetToRoot() {
        PATH.clear();
    }

    public static void enterCategory(MenuItem cat) {
        if (cat == null || !cat.isCategory()) return;
        // append so iteration order is root -> deepest
        PATH.addLast(cat);
    }

    /**
     * Returns the mutable list for the parent level of the current page.
     * - When depth == 0 (at root), returns null (no parent).
     * - When depth == 1, returns ROOT.
     * - When depth >= 2, returns childrenMutable() of the second-last category in PATH.
     */
    public static List<MenuItem> parentItems() {
        ensureLoaded();
        if (PATH.isEmpty()) return null; // at root: no parent

        // Walk root -> ... -> current, while remembering the list at the previous depth.
        List<MenuItem> items = ROOT;
        Iterator<MenuItem> it = PATH.iterator();
        while (it.hasNext()) {
            MenuItem cat = it.next();
            if (!it.hasNext()) {
                // 'cat' is the deepest category (current page belongs to cat.children)
                // The parent list is the list that contains 'cat' (i.e., 'items')
                return items;
            }
            items = cat.childrenMutable();
        }
        return null; // defensive
    }

    public static boolean canGoBack() { return !PATH.isEmpty(); }

    public static void goBack() {
        if (!PATH.isEmpty()) PATH.removeLast();
    }

    /** Returns the current page's mutable list. */
    public static List<MenuItem> currentItems() {
        ensureLoaded();
        List<MenuItem> items = ROOT;
        // walk root -> deepest
        for (MenuItem cat : PATH) {
            items = cat.childrenMutable();
        }
        return items;
    }

    /** Human-friendly titles for breadcrumb UI: ["root", "Cat1", "Sub", ...]. */
    public static List<String> pathTitles() {
        ensureLoaded();
        List<String> out = new ArrayList<>();
        out.add("root");
        for (MenuItem cat : PATH) {
            String t = cat == null ? "" : (cat.title() == null ? "" : cat.title());
            out.add(t.isEmpty() ? "(unnamed)" : t);
        }
        return out;
    }

    /** Reload model from disk, reset path to root. */
    public static void reload() {
        try {
            ROOT = MenuLoader.loadMenu();
            PATH.clear();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] RadialMenu reload failed: {}", Constants.MOD_NAME, t.toString());
            ROOT = new ArrayList<>();
            PATH.clear();
        }
    }

    private static void ensureLoaded() {
        if (ROOT.isEmpty()) {
            reload();
        }
    }

    /** Direct mutable access to root (editor use). */
    public static List<MenuItem> rootMutable() {
        ensureLoaded();
        return ROOT;
    }

    /** No cap: allow any number of items on a page. */
    public static boolean addToCurrent(MenuItem item) {
        List<MenuItem> cur = currentItems();
        cur.add(item);
        persist();
        return true;
    }

    public static boolean removeFromCurrent(String id) {
        List<MenuItem> cur = currentItems();
        boolean removed = cur.removeIf(mi -> Objects.equals(mi.id(), id));
        if (removed) persist();
        return removed;
    }

    public static boolean replaceInCurrent(String id, MenuItem replacement) {
        List<MenuItem> cur = currentItems();
        for (int i = 0; i < cur.size(); i++) {
            if (Objects.equals(cur.get(i).id(), id)) {
                cur.set(i, replacement);
                persist();
                return true;
            }
        }
        return false;
    }

    /** Legacy delta-move by id (kept for compatibility). */
    public static boolean moveInCurrent(String id, int delta) {
        List<MenuItem> cur = currentItems();
        for (int i = 0; i < cur.size(); i++) {
            if (Objects.equals(cur.get(i).id(), id)) {
                int j = Math.max(0, Math.min(cur.size() - 1, i + delta));
                if (i == j) return false;
                Collections.swap(cur, i, j);
                persist();
                return true;
            }
        }
        return false;
    }

    /** Persist the entire menu tree to disk. */
    public static void persist() {
        try {
            MenuLoader.saveMenu(ROOT);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to persist menu: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- Helpers used by MenuEditorScreen (write-through) ---

    /** Remove by id in the current level, then persist to disk. */
    public static boolean removeInCurrent(String id) {
        try {
            List<MenuItem> items = currentItems();
            if (items == null || id == null) return false;

            boolean removed = items.removeIf(mi -> Objects.equals(mi.id(), id));
            if (removed) {
                persist(); // write-through to disk
            }
            return removed;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] removeInCurrent failed for '{}': {}",
                    Constants.MOD_NAME, id, t.toString());
            return false;
        }
    }

    /**
     * Move an entry within the current level from index {@code from} to slot {@code to}.
     * Indices are clamped; dropping past the end appends. Persists on success.
     */
    public static boolean moveInCurrent(int from, int to) {
        try {
            List<MenuItem> items = currentItems();
            if (items == null) return false;

            int n = items.size();
            if (n <= 1) return false;

            // Clamp
            if (from < 0 || from >= n) return false;
            if (to < 0) to = 0;
            if (to > n) to = n;

            // Remove + insert (adjust 'to' if we removed before it)
            MenuItem m = items.remove(from);
            if (to > from) to--;
            items.add(to, m);

            persist(); // write-through to disk
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] moveInCurrent failed {} -> {}: {}",
                    Constants.MOD_NAME, from, to, t.toString());
            return false;
        }
    }
}
