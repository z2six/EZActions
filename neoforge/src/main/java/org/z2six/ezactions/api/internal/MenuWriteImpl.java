package org.z2six.ezactions.api.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.z2six.ezactions.api.MenuPath;
import org.z2six.ezactions.api.MenuWrite;
import org.z2six.ezactions.api.ApiMenuItem;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MenuWriteImpl implements MenuWrite {

    @Override
    public boolean moveWithin(MenuPath path, int fromIndex, int toIndex) {
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        if (at == null) return false;

        int n = at.size();
        if (n == 0) return false;
        if (fromIndex < 0 || fromIndex >= n) return false;
        if (toIndex < 0) toIndex = 0;
        if (toIndex > n) toIndex = n;

        MenuItem moved = at.remove(fromIndex);
        if (moved == null) return false;
        if (toIndex > fromIndex) toIndex--;
        at.add(toIndex, moved);
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        return true;
    }

    @Override
    public boolean moveTo(String itemId, MenuPath targetBundle) {
        if (itemId == null || itemId.isBlank()) return false;
        List<MenuItem> root = RadialMenu.rootMutable();

        // locate source parent + item
        Holder src = findParentAndItem(root, itemId);
        if (src == null || src.item == null || src.parent == null) return false;

        // locate target bundle list
        List<MenuItem> dst = (targetBundle == null || targetBundle.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, targetBundle.titles());

        if (dst == null) return false;

        // remove from source and append to dst
        if (src.index >= 0 && src.index < src.parent.size()) {
            MenuItem removed = src.parent.remove(src.index);
            dst.add(removed);
            try { RadialMenu.persist(); } catch (Throwable ignored) {}
            return true;
        }
        return false;
    }

    @Override
    public boolean removeFirst(MenuPath path, java.util.function.Predicate<ApiMenuItem> predicate) {
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        if (at == null || predicate == null) return false;

        for (int i = 0; i < at.size(); i++) {
            MenuItem mi = at.get(i);
            ApiMenuItem snap = new ApiMenuItem(
                    safe(mi.id()), safe(mi.title()), mi.isCategory(),
                    mi.isCategory() ? "BUNDLE" : "ACTION",
                    mi.note(), null
            );
            if (predicate.test(snap)) {
                at.remove(i);
                try { RadialMenu.persist(); } catch (Throwable ignored) {}
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeById(String id) {
        boolean ok = TreeOps.removeByIdRecursive(RadialMenu.rootMutable(), id);
        if (ok) try { RadialMenu.persist(); } catch (Throwable ignored) {}
        return ok;
    }

    @Override
    public boolean ensureBundles(MenuPath path) {
        if (path == null || path.titles().isEmpty()) return false;
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> cur = root;
        boolean createdAny = false;

        for (String title : path.titles()) {
            MenuItem found = null;
            if (cur != null) {
                for (MenuItem mi : cur) {
                    if (mi != null && mi.isCategory() && safe(mi.title()).equals(title)) {
                        found = mi;
                        break;
                    }
                }
            }
            if (found == null) {
                // create the bundle
                MenuItem cat = new MenuItem(freshId("bundle"), title, "", null, null, new ArrayList<>());
                if (cur == null) {
                    Constants.LOG.warn("[{}] ensureBundles: no current list; aborting", Constants.MOD_NAME);
                    return createdAny;
                }
                cur.add(cat);
                createdAny = true;
                cur = TreeOps.childrenOf(cat);
            } else {
                cur = TreeOps.childrenOf(found);
            }
        }

        if (createdAny) try { RadialMenu.persist(); } catch (Throwable ignored) {}
        return createdAny;
    }

    @Override
    public Optional<String> upsertFromJson(MenuPath path, String jsonItemOrArray) {
        try {
            List<MenuItem> root = RadialMenu.rootMutable();
            List<MenuItem> at = (path == null || path.titles().isEmpty())
                    ? root
                    : TreeOps.findBundleByTitles(root, path.titles());
            if (at == null) return Optional.empty();

            JsonElement el = JsonParser.parseString(jsonItemOrArray == null ? "[]" : jsonItemOrArray.trim());
            List<MenuItem> incoming = new ArrayList<>();
            if (el.isJsonObject()) {
                incoming.add(JsonCodec.fromJson(el.getAsJsonObject()));
            } else if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement e : arr) if (e.isJsonObject()) incoming.add(JsonCodec.fromJson(e.getAsJsonObject()));
            } else {
                return Optional.empty();
            }

            String lastId = null;
            for (MenuItem item : incoming) {
                String id = safe(item.id());
                if (!id.isEmpty()) {
                    // replace if id exists at this level
                    int idx = indexOfId(at, id);
                    if (idx >= 0) at.set(idx, item); else at.add(item);
                    lastId = id;
                } else {
                    // ensure id before adding
                    String gen = freshId("api");
                    MenuItem withId = new MenuItem(gen, item.title(), item.note(), item.icon(), item.action(), item.children());
                    at.add(withId);
                    lastId = gen;
                }
            }
            try { RadialMenu.persist(); } catch (Throwable ignored) {}

            return Optional.ofNullable(lastId);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] upsertFromJson failed: {}", Constants.MOD_NAME, t.toString());
            return Optional.empty();
        }
    }

    // helpers

    private static int indexOfId(List<MenuItem> list, String id) {
        if (list == null || id == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            MenuItem mi = list.get(i);
            if (mi != null && id.equals(mi.id())) return i;
        }
        return -1;
    }

    private static Holder findParentAndItem(List<MenuItem> root, String id) {
        if (root == null || id == null) return null;
        // search at this level
        for (int i = 0; i < root.size(); i++) {
            MenuItem mi = root.get(i);
            if (mi != null && id.equals(mi.id())) return new Holder(root, i, mi);
        }
        // recurse
        for (MenuItem mi : root) {
            if (mi == null || !mi.isCategory()) continue;
            List<MenuItem> sub = TreeOps.childrenOf(mi);
            Holder h = findParentAndItem(sub, id);
            if (h != null) return h;
        }
        return null;
    }

    private static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t) + "_" + Integer.toHexString((int)(Math.random()*0xFFFF));
    }
    private static String safe(String s) { return s == null ? "" : s; }

    private static final class Holder {
        final List<MenuItem> parent; final int index; final MenuItem item;
        Holder(List<MenuItem> p, int i, MenuItem m) { parent = p; index = i; item = m; }
    }
}
