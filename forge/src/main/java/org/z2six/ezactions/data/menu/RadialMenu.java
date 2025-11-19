// MainFile: src/main/java/org/z2six/ezactions/data/menu/RadialMenu.java
package org.z2six.ezactions.data.menu;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.json.MenuLoader;
import org.z2six.ezactions.gui.RadialMenuScreen;

import java.util.*;

/*
 * Holds the menu model and opens the radial as a Screen (mouse free, gameplay input blocked).
 * Visual blur is disabled for our screens via the NoBlur mixin.
 *
 * Forge 1.20.1 port with Neo features:
 *  - openAtBundle(id): open starting *inside* a specific category (bundle).
 *  - visibleItemsForDisplay(): hides items flagged hideFromMainRadial on root.
 *  - isBundleNameTaken(): global uniqueness check for bundle IDs (id == title).
 *  - moveInCurrent(int from, int to): index-based move (used by MenuEditorScreen).
 *  - canGoBack()/goBack(): restored navigation helpers (used across editor/UI).
 */
public final class RadialMenu {

    private static List<MenuItem> ROOT = new ArrayList<>();
    // PATH is maintained root -> ... -> deepest (append when entering, remove last when going back)
    private static final Deque<MenuItem> PATH = new ArrayDeque<>();

    private RadialMenu() {}

    /* Open the radial as a Screen, always starting at ROOT. */
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

    /* Open the radial starting at the category (bundle) with the given ID (id == bundle title). */
    public static void openAtBundle(String bundleId) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                Constants.LOG.debug("[{}] openAtBundle ignored: client/world not ready.", Constants.MOD_NAME);
                return;
            }
            if (mc.screen != null || mc.isPaused()) {
                final String scr = (mc.screen == null) ? "none" : mc.screen.getClass().getSimpleName();
                Constants.LOG.debug("[{}] openAtBundle ignored: screen={}, paused={}, bundleId={}",
                        Constants.MOD_NAME, scr, mc.isPaused(), bundleId);
                return;
            }

            ensureLoaded();
            PATH.clear();

            List<MenuItem> path = findPathToCategory(bundleId);
            if (path == null || path.isEmpty()) {
                Constants.LOG.info("[{}] Bundle '{}' not found; opening radial at root instead.", Constants.MOD_NAME, bundleId);
                mc.setScreen(new RadialMenuScreen());
                return;
            }
            for (MenuItem cat : path) PATH.addLast(cat);
            Constants.LOG.debug("[{}] openAtBundle resolved path depth={} for id='{}'", Constants.MOD_NAME, PATH.size(), bundleId);
            mc.setScreen(new RadialMenuScreen());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] openAtBundle failed for '{}': {}", Constants.MOD_NAME, bundleId, t.toString());
            open();
        }
    }

    /* Manually reset to root (used by editor or tests). */
    public static void resetToRoot() {
        PATH.clear();
    }

    public static void enterCategory(MenuItem cat) {
        if (cat == null || !cat.isCategory()) return;
        // append so iteration order is root -> deepest
        PATH.addLast(cat);
    }

    /* === RESTORED: navigation helpers used by editor/UI === */
    public static boolean canGoBack() { return !PATH.isEmpty(); }

    public static void goBack() {
        if (!PATH.isEmpty()) PATH.removeLast();
    }
    /* ====================================================== */

    /* Returns the current page's mutable list (full model; editor uses this). */
    public static List<MenuItem> currentItems() {
        ensureLoaded();
        List<MenuItem> items = ROOT;
        // walk root -> deepest
        for (MenuItem cat : PATH) {
            items = cat.childrenMutable();
        }
        return items;
    }

    /*
     * Returns the mutable list for the parent level of the current page.
     * - When depth == 0 (at root), returns null (no parent).
     * - When depth == 1, returns ROOT.
     * - When depth >= 2, returns childrenMutable() of the second-last category in PATH.
     */
    public static List<MenuItem> parentItems() {
        ensureLoaded();
        if (PATH.isEmpty()) return null;
        List<MenuItem> items = ROOT;
        Iterator<MenuItem> it = PATH.iterator();
        while (it.hasNext()) {
            MenuItem cat = it.next();
            if (!it.hasNext()) return items; // 'items' contains the parent list of the deepest category
            items = cat.childrenMutable();
        }
        return null; // defensive
    }

    /**
     * Returns the list of items to be shown in the radial UI for the current page.
     *
     * - On root (PATH empty), items with hideFromMainRadial==true are filtered out.
     * - On deeper pages, items are returned as-is.
     */
    public static List<MenuItem> visibleItemsForDisplay() {
        try {
            List<MenuItem> base = currentItems();
            if (base == null) return Collections.emptyList();

            if (PATH.isEmpty()) {
                List<MenuItem> out = new ArrayList<>(base.size());
                for (MenuItem mi : base) {
                    if (mi == null) continue;
                    if (mi.hideFromMainRadial()) continue;
                    out.add(mi);
                }
                return out;
            }
            return base;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] visibleItemsForDisplay failed: {}", Constants.MOD_NAME, t.toString());
            return Collections.emptyList();
        }
    }

    /* Human-friendly titles for breadcrumb UI: ["root", "Cat1", "Sub", ...]. */
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

    /* Reload model from disk, reset path to root. */
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

    /* Direct mutable access to root (editor use). */
    public static List<MenuItem> rootMutable() {
        ensureLoaded();
        return ROOT;
    }

    /* No cap: allow any number of items on a page. */
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

    /* Legacy delta-move by id (kept for compatibility). */
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

    /*
     * Move by indices (used by MenuEditorScreen).
     * Indices are clamped; dropping past the end appends. Persists on success.
     */
    public static boolean moveInCurrent(int from, int to) {
        try {
            List<MenuItem> items = currentItems();
            if (items == null) return false;

            int n = items.size();
            if (n <= 1) return false;

            if (from < 0 || from >= n) return false;
            if (to < 0) to = 0;
            if (to > n) to = n;

            MenuItem m = items.remove(from);
            if (to > from) to--;
            items.add(to, m);

            persist();
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] moveInCurrent failed {} -> {}: {}",
                    Constants.MOD_NAME, from, to, t.toString());
            return false;
        }
    }

    /* Persist the entire menu tree to disk. */
    public static void persist() {
        try {
            MenuLoader.saveMenu(ROOT);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to persist menu: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- Bundle helpers ---

    /* Check whether a bundle name (id) is already used by another category in the tree. */
    public static boolean isBundleNameTaken(String candidateId, MenuItem self) {
        try {
            ensureLoaded();
            if (candidateId == null) return false;
            String id = candidateId.trim();
            if (id.isEmpty()) return false;
            return isBundleNameTakenInList(ROOT, id, self);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] isBundleNameTaken failed for '{}': {}", Constants.MOD_NAME, candidateId, t.toString());
            return false;
        }
    }

    private static boolean isBundleNameTakenInList(List<MenuItem> list, String id, MenuItem self) {
        if (list == null) return false;
        for (MenuItem mi : list) {
            if (mi == null) continue;
            if (mi.isCategory()) {
                if (mi != self && id.equals(mi.id())) {
                    return true;
                }
                if (isBundleNameTakenInList(mi.childrenMutable(), id, self)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Find a path of categories from root to the category with the given id. */
    private static List<MenuItem> findPathToCategory(String bundleId) {
        List<MenuItem> path = new ArrayList<>();
        for (MenuItem rootChild : ROOT) {
            if (rootChild == null) continue;
            if (dfsCategory(rootChild, bundleId, path)) {
                return path;
            }
        }
        return null;
    }

    private static boolean dfsCategory(MenuItem current, String bundleId, List<MenuItem> path) {
        if (current == null || !current.isCategory()) return false;
        path.add(current);
        try {
            if (Objects.equals(current.id(), bundleId)) {
                return true;
            }
            for (MenuItem child : current.children()) {
                if (dfsCategory(child, bundleId, path)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] dfsCategory error on '{}': {}", Constants.MOD_NAME, current.id(), t.toString());
        }
        // backtrack
        path.remove(path.size() - 1);
        return false;
    }
}
