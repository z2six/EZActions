package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.api.EzActionsApi;
import org.z2six.ezactions.api.DynamicRadialStyle;
import org.z2six.ezactions.api.EditorOps;
import org.z2six.ezactions.api.ImportExport;
import org.z2six.ezactions.api.InputOps;
import org.z2six.ezactions.api.MenuRead;
import org.z2six.ezactions.api.MenuWrite;
import org.z2six.ezactions.api.MenuPath;
import org.z2six.ezactions.api.events.ApiEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.editor.MenuEditorScreen;
import org.z2six.ezactions.gui.editor.menu.MenuNavUtil;
import org.z2six.ezactions.data.json.MenuImportExport;

import java.util.ArrayList;
import java.util.List;

public final class EzActionsApiImpl implements EzActionsApi {

    private final ApiEvents events = new ApiEvents();
    private final MenuRead read = new MenuReadImpl();
    private final MenuWrite write = new MenuWriteImpl(events);
    private final ImportExport io = new ImportExportImpl(events);
    private final InputOps input = new InputOpsImpl();
    private final EditorOps editor = new EditorOpsImpl();

    @Override
    public void openEditor(Screen parent) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.setScreen(new MenuEditorScreen(parent));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] openEditor failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void openConfig(Screen parent) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.setScreen(new org.z2six.ezactions.gui.editor.config.ConfigScreen(parent));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] openConfig failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void openRadial() {
        try { RadialMenu.open(); }
        catch (Throwable t) {
            Constants.LOG.warn("[{}] openRadial failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void openRadialAtBundle(String bundleId) {
        try { RadialMenu.openAtBundle(bundleId); }
        catch (Throwable t) {
            Constants.LOG.warn("[{}] openRadialAtBundle failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public boolean openTemporaryRadial(String jsonItemOrArray, DynamicRadialStyle styleOrNull) {
        try { return editor.openTemporaryRadial(jsonItemOrArray, styleOrNull); }
        catch (Throwable t) {
            Constants.LOG.warn("[{}] openTemporaryRadial failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }

    @Override
    public String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action, IconSpec iconOrNull, boolean locked) {
        if (action == null) return null;
        List<String> path = MenuNavUtil.capturePathTitles();
        try {
            List<MenuItem> dst = (parentIdOrNull == null)
                    ? RadialMenu.rootMutable()
                    : TreeOps.findChildrenListById(RadialMenu.rootMutable(), parentIdOrNull);

            if (dst == null) return null;

            String id = freshId("act");
            MenuItem item = new MenuItem(
                    id,
                    safe(title),
                    safe(noteOrNull),
                    iconOrNull,
                    action,
                    null,  // children null for action
                    false,
                    false,
                    locked
            );
            dst.add(item);
            RadialMenu.persist();
            fireChanged(path, "addAction");
            Constants.LOG.info("[{}] API addAction: id='{}' parent='{}' locked={}",
                    Constants.MOD_NAME, id, parentIdOrNull == null ? "<root>" : parentIdOrNull, locked);
            return id;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] addAction failed: {}", Constants.MOD_NAME, t.toString());
            return null;
        } finally {
            MenuNavUtil.restorePathTitles(path);
        }
    }

    @Override
    public String addBundle(String parentIdOrNull, String title, String noteOrNull, IconSpec iconOrNull,
                            boolean hideFromMainRadial, boolean bundleKeybindEnabled, boolean locked) {
        List<String> path = MenuNavUtil.capturePathTitles();
        try {
            List<MenuItem> dst = (parentIdOrNull == null)
                    ? RadialMenu.rootMutable()
                    : TreeOps.findChildrenListById(RadialMenu.rootMutable(), parentIdOrNull);

            if (dst == null) return null;

            String id = freshId("bundle");
            MenuItem cat = new MenuItem(
                    id,
                    safe(title),
                    safe(noteOrNull),
                    iconOrNull,
                    null,            // action null for category
                    new ArrayList<>(),// children list for category
                    hideFromMainRadial,
                    bundleKeybindEnabled,
                    locked
            );
            dst.add(cat);
            RadialMenu.persist();
            fireChanged(path, "addBundle");
            Constants.LOG.info("[{}] API addBundle: id='{}' parent='{}' hideFromMainRadial={} keybindEnabled={} locked={}",
                    Constants.MOD_NAME, id, parentIdOrNull == null ? "<root>" : parentIdOrNull, hideFromMainRadial, bundleKeybindEnabled, locked);
            return id;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] addBundle failed: {}", Constants.MOD_NAME, t.toString());
            return null;
        } finally {
            MenuNavUtil.restorePathTitles(path);
        }
    }

    @Override
    public boolean removeItem(String id) {
        if (id == null || id.isBlank()) return false;
        List<String> path = MenuNavUtil.capturePathTitles();
        try {
            List<MenuItem> root = RadialMenu.rootMutable();
            if (root == null) return false;
            boolean ok = TreeOps.removeByIdRecursive(root, id);
            if (ok) RadialMenu.persist();
            if (ok) fireChanged(path, "removeItem");
            return ok;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] removeItem failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        } finally {
            MenuNavUtil.restorePathTitles(path);
        }
    }

    @Override
    public boolean moveWithin(String parentIdOrNull, int fromIndex, int toIndex) {
        List<String> path = MenuNavUtil.capturePathTitles();
        try {
            List<MenuItem> list = (parentIdOrNull == null)
                    ? RadialMenu.rootMutable()
                    : TreeOps.findChildrenListById(RadialMenu.rootMutable(), parentIdOrNull);
            if (list == null) return false;

            int n = list.size();
            if (n == 0) return false;
            if (fromIndex < 0 || fromIndex >= n) return false;
            if (toIndex < 0) toIndex = 0;
            if (toIndex > n) toIndex = n;

            MenuItem moved = list.remove(fromIndex);
            if (moved == null) return false;

            if (toIndex > fromIndex) toIndex--;
            list.add(toIndex, moved);
            RadialMenu.persist();
            fireChanged(path, "moveWithin");
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] moveWithin failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        } finally {
            MenuNavUtil.restorePathTitles(path);
        }
    }

    @Override
    public void persist() {
        try { RadialMenu.persist(); } catch (Throwable ignored) {}
    }

    @Override
    public int importFromClipboard() {
        try {
            int count = MenuImportExport.importFromClipboard();
            if (count >= 0) {
                try { events._fireImported(MenuPath.root(), "<clipboard>", count); } catch (Throwable ignored) {}
                fireChanged(List.of(), "importFromClipboard");
            }
            return count;
        }
        catch (Throwable t) {
            Constants.LOG.warn("[{}] importFromClipboard failed: {}", Constants.MOD_NAME, t.toString());
            return -1;
        }
    }

    @Override
    public boolean exportToClipboard() {
        try { MenuImportExport.exportToClipboard(); return true; }
        catch (Throwable t) {
            Constants.LOG.warn("[{}] exportToClipboard failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }

    // ---- Batch 2 accessors ----

    @Override public MenuRead menuRead() { return read; }
    @Override public MenuWrite menuWrite() { return write; }
    @Override public ImportExport importExport() { return io; }
    @Override public InputOps inputOps() { return input; }
    @Override public EditorOps editorOps() { return editor; }
    @Override public ApiEvents events() { return events; }

    // ---- small local helpers ----

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t) + "_" + Integer.toHexString((int)(Math.random()*0xFFFF));
    }

    private void fireChanged(List<String> pathTitles, String why) {
        try {
            MenuPath p = MenuPath.of(pathTitles == null ? List.of() : pathTitles);
            events._fireChanged(p, why);
        } catch (Throwable ignored) {}
    }
}
