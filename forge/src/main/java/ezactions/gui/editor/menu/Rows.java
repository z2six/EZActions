// MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/Rows.java
package ezactions.gui.editor.menu;

import org.z2six.ezactions.data.menu.MenuItem;

/**
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/Rows.java
 *
 * Row model types used by MenuEditorScreen's list.
 * Kept simple (no sealed hierarchy to avoid cross-version quirks).
 */
public interface Rows {

    /** Non-interactive breadcrumb label row. */
    final class BreadcrumbRow implements Rows {
        private final String path;
        public BreadcrumbRow(String path) { this.path = path; }
        public String path() { return path; }
    }

    /** Interactive "Back to root" pseudo-action row. */
    final class BackToRootRow implements Rows { }

    /** Interactive "Back to XYZ" pseudo-action row. */
    final class BackToParentRow implements Rows {
        private final String parentName;
        public BackToParentRow(String parentName) { this.parentName = parentName; }
        public String parentName() { return parentName; }
    }

    /** Normal item row for a MenuItem (action or bundle). */
    final class ItemRow implements Rows {
        private final MenuItem item;
        public ItemRow(MenuItem item) { this.item = item; }
        public MenuItem item() { return item; }
    }
}
