package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.gui.editor.menu.MenuNavUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

final class TreeOps {
    private TreeOps() {}

    /** Returns the mutable children list of the item whose id equals targetId; root is searched recursively. */
    static List<MenuItem> findChildrenListById(List<MenuItem> root, String targetId) {
        if (root == null || targetId == null) return null;
        for (MenuItem mi : root) {
            if (mi == null) continue;
            if (Objects.equals(mi.id(), targetId)) {
                try { return mi.childrenMutable(); }
                catch (Throwable ignored) { return null; }
            }
            if (mi.isCategory()) {
                List<MenuItem> sub = null;
                try { sub = mi.childrenMutable(); } catch (Throwable ignored) {}
                if (sub != null) {
                    List<MenuItem> r = findChildrenListById(sub, targetId);
                    if (r != null) return r;
                }
            }
        }
        return null;
    }

    /** Removes an item by id at any depth. Returns true if removed. */
    static boolean removeByIdRecursive(List<MenuItem> list, String id) {
        if (list == null || id == null) return false;
        for (Iterator<MenuItem> it = list.iterator(); it.hasNext();) {
            MenuItem mi = it.next();
            if (mi == null) continue;
            if (Objects.equals(mi.id(), id)) {
                if (mi.locked()) return false;
                it.remove();
                return true;
            }
        }
        for (MenuItem mi : list) {
            if (mi == null || !mi.isCategory()) continue;
            List<MenuItem> sub = null;
            try { sub = mi.childrenMutable(); } catch (Throwable ignored) {}
            if (sub != null && removeByIdRecursive(sub, id)) return true;
        }
        return false;
    }

    /** Returns the first item matching id, or null. */
    static MenuItem findFirstById(List<MenuItem> list, String id) {
        if (list == null || id == null) return null;
        for (MenuItem mi : list) {
            if (mi == null) continue;
            if (Objects.equals(mi.id(), id)) return mi;
            if (mi.isCategory()) {
                List<MenuItem> sub = null;
                try { sub = mi.childrenMutable(); } catch (Throwable ignored) {}
                MenuItem r = findFirstById(sub, id);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Follow a chain of bundle titles from root; return the children list of the final bundle, or root if empty titles. */
    static List<MenuItem> findBundleByTitles(List<MenuItem> root, List<String> titles) {
        if (root == null) return null;
        List<MenuItem> cur = root;
        if (titles == null || titles.isEmpty()) return cur;

        for (String title : titles) {
            boolean found = false;
            if (cur == null) return null;
            for (MenuItem mi : cur) {
                if (mi == null || !mi.isCategory()) continue;
                String t = safe(mi.title());
                if (t.equals(title)) {
                    try { cur = mi.childrenMutable(); } catch (Throwable ignore) { cur = null; }
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        return cur;
    }

    /** True if path of titles exists. */
    static boolean existsPath(List<MenuItem> root, List<String> titles) {
        return findBundleByTitles(root, titles) != null;
    }

    /** Null-safe wrapper for children list of a bundle. */
    static List<MenuItem> childrenOf(MenuItem bundle) {
        if (bundle == null || !bundle.isCategory()) return null;
        try { return bundle.childrenMutable(); } catch (Throwable ignored) { return null; }
    }

    /** Current UI breadcrumb titles (from your editor/radial util), null-safe. */
    static List<String> currentPathTitles() {
        try {
            List<String> raw = MenuNavUtil.capturePathTitles();
            if (raw == null || raw.isEmpty()) return List.of();
            List<String> out = new ArrayList<>(raw);
            if (!out.isEmpty() && "root".equalsIgnoreCase(out.get(0))) {
                out.remove(0);
            }
            return out;
        }
        catch (Throwable ignored) { return List.of(); }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
