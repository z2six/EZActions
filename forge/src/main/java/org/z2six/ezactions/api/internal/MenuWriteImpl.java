package org.z2six.ezactions.api.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.z2six.ezactions.api.MenuPath;
import org.z2six.ezactions.api.MenuWrite;
import org.z2six.ezactions.api.ApiMenuItem;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.api.events.ApiEvents;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MenuWriteImpl implements MenuWrite {
    private final ApiEvents events;

    MenuWriteImpl(ApiEvents events) {
        this.events = events;
    }

    @Override
    public Optional<String> addAction(MenuPath path, String title, String noteOrNull, IconSpec iconOrNull, IClickAction action, boolean locked) {
        if (action == null) return Optional.empty();
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        if (at == null) return Optional.empty();

        String id = freshId("act");
        MenuItem item = new MenuItem(
                id,
                safe(title),
                safe(noteOrNull),
                iconOrNull,
                action,
                null,
                false,
                false,
                locked
        );
        at.add(item);
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(path, "addAction");
        Constants.LOG.info("[{}] API addAction: id='{}' path='{}' locked={}",
                Constants.MOD_NAME, id, pathToString(path), locked);
        return Optional.of(id);
    }

    @Override
    public Optional<String> addBundle(MenuPath path, String title, String noteOrNull, IconSpec iconOrNull,
                                      boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked) {
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        if (at == null) return Optional.empty();

        String id = freshId("bundle");
        MenuItem item = new MenuItem(
                id,
                safe(title),
                safe(noteOrNull),
                iconOrNull,
                null,
                new ArrayList<>(),
                hideFromMainRadial,
                bundleKeybindEnabled,
                locked
        );
        at.add(item);
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(path, "addBundle");
        Constants.LOG.info("[{}] API addBundle: id='{}' path='{}' hideFromMainRadial={} keybindEnabled={} locked={}",
                Constants.MOD_NAME, id, pathToString(path), hideFromMainRadial, bundleKeybindEnabled, locked);
        return Optional.of(id);
    }

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
        fireChanged(path, "moveWithin");
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
            fireChanged(targetBundle, "moveTo");
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
            if (mi != null && mi.locked()) continue;
            ApiMenuItem snap = ApiMapper.toApi(mi);
            if (predicate.test(snap)) {
                at.remove(i);
                try { RadialMenu.persist(); } catch (Throwable ignored) {}
                fireChanged(path, "removeFirst");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeById(String id) {
        boolean ok = TreeOps.removeByIdRecursive(RadialMenu.rootMutable(), id);
        if (ok) {
            try { RadialMenu.persist(); } catch (Throwable ignored) {}
            fireChanged(MenuPath.root(), "removeById");
            Constants.LOG.info("[{}] API removeById: id='{}'", Constants.MOD_NAME, id);
        }
        return ok;
    }

    @Override
    public boolean updateMeta(String id, String titleOrNull, String noteOrNull, IconSpec iconOrNull) {
        if (id == null || id.isBlank()) return false;
        List<MenuItem> root = RadialMenu.rootMutable();
        Holder h = findParentAndItem(root, id);
        if (h == null || h.item == null || h.parent == null || h.index < 0 || h.index >= h.parent.size()) return false;

        MenuItem next = h.item;
        if (titleOrNull != null) next = next.withTitle(titleOrNull);
        if (noteOrNull != null) next = next.withNote(noteOrNull);
        if (iconOrNull != null) next = next.withIcon(iconOrNull);
        h.parent.set(h.index, next);
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(MenuPath.root(), "updateMeta");
        Constants.LOG.info("[{}] API updateMeta: id='{}' titleChanged={} noteChanged={} iconChanged={}",
                Constants.MOD_NAME, id, titleOrNull != null, noteOrNull != null, iconOrNull != null);
        return true;
    }

    @Override
    public boolean replaceAction(String id, IClickAction action) {
        if (id == null || id.isBlank() || action == null) return false;
        List<MenuItem> root = RadialMenu.rootMutable();
        Holder h = findParentAndItem(root, id);
        if (h == null || h.item == null || h.parent == null || h.index < 0 || h.index >= h.parent.size()) return false;
        if (h.item.isCategory()) return false;
        h.parent.set(h.index, h.item.withAction(action));
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(MenuPath.root(), "replaceAction");
        Constants.LOG.info("[{}] API replaceAction: id='{}' type={}", Constants.MOD_NAME, id, action.getType());
        return true;
    }

    @Override
    public boolean setBundleFlags(String id, boolean hideFromMainRadial, boolean bundleKeybindEnabled) {
        if (id == null || id.isBlank()) return false;
        List<MenuItem> root = RadialMenu.rootMutable();
        Holder h = findParentAndItem(root, id);
        if (h == null || h.item == null || h.parent == null || h.index < 0 || h.index >= h.parent.size()) return false;
        if (!h.item.isCategory()) return false;
        MenuItem next = h.item
                .withHideFromMainRadial(hideFromMainRadial)
                .withBundleKeybindEnabled(bundleKeybindEnabled);
        h.parent.set(h.index, next);
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(MenuPath.root(), "setBundleFlags");
        Constants.LOG.info("[{}] API setBundleFlags: id='{}' hideFromMainRadial={} keybindEnabled={}",
                Constants.MOD_NAME, id, hideFromMainRadial, bundleKeybindEnabled);
        return true;
    }

    @Override
    public boolean setLocked(String id, boolean locked) {
        if (id == null || id.isBlank()) return false;
        List<MenuItem> root = RadialMenu.rootMutable();
        Holder h = findParentAndItem(root, id);
        if (h == null || h.item == null || h.parent == null || h.index < 0 || h.index >= h.parent.size()) return false;
        h.parent.set(h.index, h.item.withLocked(locked));
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
        fireChanged(MenuPath.root(), "setLocked");
        Constants.LOG.info("[{}] API setLocked: id='{}' locked={}", Constants.MOD_NAME, id, locked);
        return true;
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
                MenuItem cat = new MenuItem(
                        freshId("bundle"),
                        title,
                        "",
                        null,
                        null,
                        new ArrayList<>(),
                        false,
                        false
                );
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

        if (createdAny) {
            try { RadialMenu.persist(); } catch (Throwable ignored) {}
            fireChanged(path, "ensureBundles");
        }
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
                    MenuItem withId = new MenuItem(
                            gen,
                            item.titleComponent(),
                            item.noteComponent(),
                            item.icon(),
                            item.action(),
                            item.children(),
                            item.hideFromMainRadial(),
                            item.bundleKeybindEnabled(),
                            item.locked()
                    );
                    at.add(withId);
                    lastId = gen;
                }
            }
            try { RadialMenu.persist(); } catch (Throwable ignored) {}
            fireChanged(path, "upsertFromJson");

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
    private static String pathToString(MenuPath path) {
        if (path == null || path.titles() == null || path.titles().isEmpty()) return "<root>";
        return String.join("/", path.titles());
    }

    private void fireChanged(MenuPath path, String why) {
        try {
            if (events != null) events._fireChanged(path == null ? MenuPath.root() : path, why);
        } catch (Throwable ignored) {}
    }

    private static final class Holder {
        final List<MenuItem> parent; final int index; final MenuItem item;
        Holder(List<MenuItem> p, int i, MenuItem m) { parent = p; index = i; item = m; }
    }
}
