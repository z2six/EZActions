// MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/MenuNavUtil.java
package org.z2six.ezactions.gui.editor.menu;

import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/MenuNavUtil.java
 *
 * Utilities for capturing/restoring navigation path without leaving the user's current view.
 * Strategy:
 *  - Capture current path as titles ({@code List<String>}).
 *  - For operations that need to mutate parent/root, we temporarily navigate,
 *    perform the mutation, then restore the original path by walking titles.
 *
 * NOTE: Title-based re-entry assumes titles are unique along a given path level.
 *       If duplicates exist, the first matching title is chosen.
 */
public final class MenuNavUtil {

    private MenuNavUtil() {}

    /** Returns a defensive copy of the current path titles; never null. */
    public static List<String> capturePathTitles() {
        try {
            List<String> parts = RadialMenu.pathTitles();
            if (parts == null) return List.of();
            return new ArrayList<>(parts);
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] capturePathTitles failed: {}", Constants.MOD_NAME, t.toString());
            return List.of();
        }
    }

    /** Navigate all the way to root. */
    public static void goToRoot() {
        int guards = 256;
        while (RadialMenu.canGoBack() && guards-- > 0) {
            RadialMenu.goBack();
        }
    }

    /**
     * Restore a previously captured path by titles. Starts from root and enters each title in order.
     * If any step fails, it stops early (best effort).
     */
    public static void restorePathTitles(List<String> titles) {
        try {
            goToRoot();
            if (titles == null || titles.isEmpty()) return;

            // We expect titles to represent the full chain down to the current bundle.
            for (String title : titles) {
                if (title == null || title.isEmpty()) continue;

                List<MenuItem> level = RadialMenu.currentItems();
                if (level == null) break;

                MenuItem next = null;
                for (MenuItem mi : level) {
                    if (mi != null && mi.isCategory()) {
                        String t = mi.title();
                        if (t != null && Objects.equals(t, title)) {
                            next = mi; break;
                        }
                    }
                }
                if (next == null) break;
                RadialMenu.enterCategory(next);
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] restorePathTitles failed: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
