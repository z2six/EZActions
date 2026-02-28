// MainFile: src/main/java/org/z2six/ezactions/data/menu/RadialMenu.java
package org.z2six.ezactions.data.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.json.MenuLoader;
import org.z2six.ezactions.gui.RadialMenuScreen;
import org.z2six.ezactions.util.BundleHotkeyManager;

import java.util.*;

/**
 * Holds the menu model and opens the radial as a Screen (mouse free, gameplay input blocked).
 * Visual blur is disabled for our screens via the NoBlur mixin.
 *
 * New responsibilities:
 *  - openAtBundle(id): open the radial starting *inside* a specific category (bundle).
 *  - visibleItemsForDisplay(): view used by RadialMenuScreen that hides bundles flagged
 *    hideFromMainRadial on the root page.
 *  - isBundleNameTaken(): global check used by the editor to enforce unique bundle IDs
 *    (since bundle id == bundle title).
 */
public final class RadialMenu {

    private static List<MenuItem> ROOT = new ArrayList<>();
    // PATH is maintained root -> ... -> deepest (append when entering, remove last when going back)
    private static final Deque<MenuItem> PATH = new ArrayDeque<>();
    private static List<MenuItem> TEMP_ROOT = null;
    private static final Deque<MenuItem> TEMP_PATH = new ArrayDeque<>();
    private static TemporaryStyle TEMP_STYLE = null;
    private static Screen TEMP_RETURN_SCREEN = null;
    private static TemporaryStyle PREVIEW_STYLE = null;

    private RadialMenu() {}

    /** Optional style overrides for a temporary API radial session. */
    public static final class TemporaryStyle {
        public final Integer ringColor;
        public final Integer hoverColor;
        public final Integer borderColor;
        public final Integer textColor;
        public final Boolean animationsEnabled;
        public final Boolean animOpenClose;
        public final Boolean animHover;
        public final Integer openCloseMs;
        public final Double hoverGrowPct;
        public final String openStyle;
        public final String openDirection;
        public final String hoverStyle;
        public final Integer deadzone;
        public final Integer baseOuterRadius;
        public final Integer ringThickness;
        public final Integer scaleStartThreshold;
        public final Integer scalePerItem;
        public final Integer sliceGapDeg;
        public final String designStyle;

        public TemporaryStyle(Integer ringColor, Integer hoverColor, Integer borderColor, Integer textColor,
                              Boolean animationsEnabled, Boolean animOpenClose, Boolean animHover,
                              Integer openCloseMs, Double hoverGrowPct, String openStyle, String openDirection, String hoverStyle,
                              Integer deadzone, Integer baseOuterRadius, Integer ringThickness,
                              Integer scaleStartThreshold, Integer scalePerItem, Integer sliceGapDeg, String designStyle) {
            this.ringColor = ringColor;
            this.hoverColor = hoverColor;
            this.borderColor = borderColor;
            this.textColor = textColor;
            this.animationsEnabled = animationsEnabled;
            this.animOpenClose = animOpenClose;
            this.animHover = animHover;
            this.openCloseMs = openCloseMs;
            this.hoverGrowPct = hoverGrowPct;
            this.openStyle = openStyle;
            this.openDirection = openDirection;
            this.hoverStyle = hoverStyle;
            this.deadzone = deadzone;
            this.baseOuterRadius = baseOuterRadius;
            this.ringThickness = ringThickness;
            this.scaleStartThreshold = scaleStartThreshold;
            this.scalePerItem = scalePerItem;
            this.sliceGapDeg = sliceGapDeg;
            this.designStyle = designStyle;
        }
    }

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

            clearTemporarySession();
            ensureLoaded();
            PATH.clear(); // important: always open at root
            mc.setScreen(new RadialMenuScreen());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to open radial: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /**
     * Open the radial starting at the category (bundle) with the given ID.
     * ID is expected to equal the bundle title (enforced by the editor).
     *
     * - If the bundle is found, PATH will contain the categories on the path from root to the target.
     * - If not found, falls back to open() at root.
     */
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

            clearTemporarySession();
            ensureLoaded();
            PATH.clear();

            List<MenuItem> path = findPathToCategory(bundleId);
            if (path == null || path.isEmpty()) {
                Constants.LOG.info("[{}] Bundle '{}' not found; opening radial at root instead.", Constants.MOD_NAME, bundleId);
                mc.setScreen(new RadialMenuScreen());
                return;
            }

            for (MenuItem cat : path) {
                PATH.addLast(cat);
            }

            Constants.LOG.debug("[{}] openAtBundle resolved path depth={} for id='{}'", Constants.MOD_NAME, PATH.size(), bundleId);
            mc.setScreen(new RadialMenuScreen());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] openAtBundle failed for '{}': {}", Constants.MOD_NAME, bundleId, t.toString());
            open();
        }
    }

    /** Open a one-off radial session using API-provided items (not persisted). */
    public static boolean openTemporary(List<MenuItem> rootItems, TemporaryStyle style) {
        return openTemporary(rootItems, style, null);
    }

    public static boolean openTemporary(List<MenuItem> rootItems, TemporaryStyle style, Screen returnTo) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                Constants.LOG.debug("[{}] openTemporary ignored: client/world not ready.", Constants.MOD_NAME);
                return false;
            }
            if (mc.screen != null || mc.isPaused()) {
                final String scr = (mc.screen == null) ? "none" : mc.screen.getClass().getSimpleName();
                Constants.LOG.debug("[{}] openTemporary ignored: screen={}, paused={}", Constants.MOD_NAME, scr, mc.isPaused());
                return false;
            }

            TEMP_ROOT = (rootItems == null) ? new ArrayList<>() : new ArrayList<>(rootItems);
            TEMP_PATH.clear();
            TEMP_STYLE = style;
            TEMP_RETURN_SCREEN = returnTo;
            mc.setScreen(new RadialMenuScreen());
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] openTemporary failed: {}", Constants.MOD_NAME, t.toString());
            clearTemporarySession();
            return false;
        }
    }

    /** Called by radial screen close to clean up transient sessions. */
    public static Screen onRadialClosed() {
        Screen ret = TEMP_RETURN_SCREEN;
        clearTemporarySession();
        return ret;
    }

    /** True while a temporary API-driven radial session is active. */
    public static boolean isTemporarySession() {
        return TEMP_ROOT != null;
    }

    /** Active temporary style overrides, if any. */
    public static TemporaryStyle temporaryStyle() {
        return TEMP_STYLE != null ? TEMP_STYLE : PREVIEW_STYLE;
    }

    /** Preview-only style override (used by config preview UI). */
    public static void setPreviewStyle(TemporaryStyle style) {
        PREVIEW_STYLE = style;
    }

    /** Clears preview-only style override. */
    public static void clearPreviewStyle() {
        PREVIEW_STYLE = null;
    }

    /** Manually reset to root (used by editor or tests). */
    public static void resetToRoot() {
        activePath().clear();
    }

    public static void enterCategory(MenuItem cat) {
        if (cat == null || !cat.isCategory()) return;
        // append so iteration order is root -> deepest
        activePath().addLast(cat);
    }

    /**
     * Returns the mutable list for the parent level of the current page.
     * - When depth == 0 (at root), returns null (no parent).
     * - When depth == 1, returns ROOT.
     * - When depth >= 2, returns childrenMutable() of the second-last category in PATH.
     */
    public static List<MenuItem> parentItems() {
        ensureLoaded();
        Deque<MenuItem> path = activePath();
        if (path.isEmpty()) return null; // at root: no parent

        // Walk root -> ... -> current, while remembering the list at the previous depth.
        List<MenuItem> items = activeRoot();
        Iterator<MenuItem> it = path.iterator();
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

    public static boolean canGoBack() { return !activePath().isEmpty(); }

    public static void goBack() {
        Deque<MenuItem> path = activePath();
        if (!path.isEmpty()) path.removeLast();
    }

    /** Returns the current page's mutable list (full model; editor uses this). */
    public static List<MenuItem> currentItems() {
        ensureLoaded();
        List<MenuItem> items = activeRoot();
        // walk root -> deepest
        for (MenuItem cat : activePath()) {
            items = cat.childrenMutable();
        }
        return items;
    }

    /**
     * Returns the list of items to be shown in the radial UI for the current page.
     *
     * - On root (PATH empty), items with hideFromMainRadial==true are filtered out.
     * - On deeper pages, items are returned as-is.
     *
     * This keeps the editor/model operating on the full list via currentItems(),
     * while RadialMenuScreen uses this filtered view.
     */
    public static List<MenuItem> visibleItemsForDisplay() {
        try {
            List<MenuItem> base = currentItems();
            if (base == null) return Collections.emptyList();

            if (activePath().isEmpty()) {
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

    /** Human-friendly titles for breadcrumb UI: ["root", "Cat1", "Sub", ...]. */
    public static List<String> pathTitles() {
        ensureLoaded();
        List<String> out = new ArrayList<>();
        out.add("root");
        for (MenuItem cat : activePath()) {
            String t = cat == null ? "" : (cat.title() == null ? "" : cat.title());
            out.add(t.isEmpty() ? Component.translatable("ezactions.common.unnamed").getString() : t);
        }
        return out;
    }

    /** Reload model from disk, reset path to root. */
    public static void reload() {
        try {
            ROOT = MenuLoader.loadMenu();
            PATH.clear();
            clearTemporarySession();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] RadialMenu reload failed: {}", Constants.MOD_NAME, t.toString());
            ROOT = new ArrayList<>();
            PATH.clear();
            clearTemporarySession();
        }
    }

    private static void ensureLoaded() {
        if (ROOT.isEmpty()) {
            reload();
        }
    }

    private static List<MenuItem> activeRoot() {
        return TEMP_ROOT != null ? TEMP_ROOT : ROOT;
    }

    private static Deque<MenuItem> activePath() {
        return TEMP_ROOT != null ? TEMP_PATH : PATH;
    }

    private static void clearTemporarySession() {
        TEMP_ROOT = null;
        TEMP_PATH.clear();
        TEMP_STYLE = null;
        TEMP_RETURN_SCREEN = null;
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
        boolean removed = cur.removeIf(mi -> mi != null && Objects.equals(mi.id(), id) && !mi.locked());
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
            BundleHotkeyManager.notifyRestartRequiredIfNeeded("persist");
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
            boolean removed = items.removeIf(mi -> mi != null && Objects.equals(mi.id(), id) && !mi.locked());
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

    // --- Bundle helpers ---

    /**
     * Check whether a bundle name (id) is already used by another category in the tree.
     *
     * - Only categories (action == null) are considered "bundles" for this purpose.
     * - The 'self' entry (currently being edited) is ignored so you can re-save with the same name.
     */
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

    /** Find a path of categories from root to the category with the given id. */
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
