package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.api.EzActionsApi;
import org.z2six.ezactions.api.ImportExport;
import org.z2six.ezactions.api.MenuRead;
import org.z2six.ezactions.api.MenuWrite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.editor.MenuEditorScreen;
import org.z2six.ezactions.gui.editor.menu.MenuNavUtil;
import org.z2six.ezactions.data.json.MenuImportExport;

import java.util.ArrayList;
import java.util.List;

public final class EzActionsApiImpl implements EzActionsApi {

    private final MenuRead read = new MenuReadImpl();
    private final MenuWrite write = new MenuWriteImpl();
    private final ImportExport io = new ImportExportImpl();

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
    public String addAction(String parentIdOrNull, String title, String noteOrNull, IClickAction action) {
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
                    null, // IconSpec
                    action,
                    null  // children null for action
            );
            dst.add(item);
            RadialMenu.persist();
            return id;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] addAction failed: {}", Constants.MOD_NAME, t.toString());
            return null;
        } finally {
            MenuNavUtil.restorePathTitles(path);
        }
    }

    @Override
    public String addBundle(String parentIdOrNull, String title, String noteOrNull) {
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
                    null,            // IconSpec
                    null,            // action null for category
                    new ArrayList<>()// children list for category
            );
            dst.add(cat);
            RadialMenu.persist();
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
        try { return MenuImportExport.importFromClipboard(); }
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

    // ---- small local helpers ----

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t) + "_" + Integer.toHexString((int)(Math.random()*0xFFFF));
    }
}
