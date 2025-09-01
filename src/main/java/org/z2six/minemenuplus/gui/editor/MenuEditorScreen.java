// MainFile: src/main/java/org/z2six/minemenuplus/gui/editor/MenuEditorScreen.java
package org.z2six.minemenuplus.gui.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.click.ClickActionType;
import org.z2six.minemenuplus.data.click.IClickAction;
import org.z2six.minemenuplus.data.icon.IconSpec;
import org.z2six.minemenuplus.data.menu.MenuItem;
import org.z2six.minemenuplus.data.menu.RadialMenu;
import org.z2six.minemenuplus.gui.IconRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Menu Editor (main options screen).
 * - Left: action buttons (add key/command/category, edit, remove)
 * - Right: list of items. Categories show as "(RMB to open) Name". RMB opens the category.
 * - When inside a category, a breadcrumb header "root/…/…" is shown above a red "Back" pseudo-row.
 * - Drag & drop to reorder, with a blue insertion indicator line.
 * - All mutations are persisted via RadialMenu helpers.
 */
public final class MenuEditorScreen extends Screen {

    // Layout constants
    private static final int PAD = 8;
    private static final int LEFT_W = 160;
    private static final int ROW_H = 24;
    private static final int ICON_SZ = 18;

    // Breadcrumb header height (inside list panel)
    private static final int HEADER_H = 16;

    // Drag visuals
    private static final int BLUE = 0x802478FF;
    private static final int HILITE = 0x202478FF;
    private static final int ROW_BG = 0x20101010;

    // Construction
    private final Screen parent;

    // UI state
    private EditBox filterBox;
    private Button btnAddKey;
    private Button btnAddCmd;
    private Button btnAddCat;
    private Button btnEdit;
    private Button btnRemove;
    private Button btnClose;

    // List geometry
    private int listLeft, listTop, listWidth, listHeight;

    // Rows
    private final List<Row> rows = new ArrayList<>();
    private int hoveredRow = -1;
    private int selectedRow = -1;

    // Scroll & drag
    private double scrollY = 0.0;
    private boolean dragging = false;
    private int dragRowIdx = -1;    // index in rows[]
    private int dragGhostOffsetY = 0;
    private int dropAt = -1;        // insertion position (between rows)

    // --- Row model -----------------------------------------------------------

    private sealed interface Row {
        record BackRow() implements Row {}
        record ItemRow(MenuItem item) implements Row {}
    }

    // --- Constructors --------------------------------------------------------

    // Keep no-arg so KeyboardHandler can still do new MenuEditorScreen()
    public MenuEditorScreen() { this(null); }

    public MenuEditorScreen(Screen parent) {
        super(Component.literal("EZ Actions - Menu Editor"));
        this.parent = parent;
    }

    // --- Helpers -------------------------------------------------------------

    public static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t);
    }

    private boolean atRoot() {
        return !RadialMenu.canGoBack();
    }

    /** Visible header height inside the list area (0 at root, HEADER_H when inside). */
    private int headerH() {
        return atRoot() ? 0 : HEADER_H;
    }

    /** Live list for whatever level the editor is currently showing. */
    private List<MenuItem> current() {
        List<MenuItem> it = RadialMenu.currentItems();
        return (it == null) ? List.of() : it;
    }

    private void rebuildRows() {
        rows.clear();
        if (!atRoot()) {
            rows.add(new Row.BackRow());
        }
        String q = filterBox != null ? filterBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (MenuItem mi : current()) {
            if (q.isEmpty()) {
                rows.add(new Row.ItemRow(mi));
            } else {
                String title = mi.title() == null ? "" : mi.title();
                if (title.toLowerCase(Locale.ROOT).contains(q)) {
                    rows.add(new Row.ItemRow(mi));
                }
            }
        }

        // Clamp selection
        if (selectedRow >= rows.size()) selectedRow = rows.size() - 1;
        if (selectedRow < -1) selectedRow = -1;
    }

    private int contentCount() {
        int c = 0;
        for (Row r : rows) if (r instanceof Row.ItemRow) c++;
        return c;
    }

    private int rowCount() {
        return rows.size();
    }

    private int visibleRowCount() {
        int vh = listHeight - headerH();
        return Math.max(0, vh / ROW_H);
    }

    private int firstVisibleRow() {
        return Math.max(0, (int)Math.floor(scrollY / ROW_H));
    }

    private int lastVisibleRow() {
        return Math.min(rowCount() - 1, firstVisibleRow() + visibleRowCount());
    }

    private int mouseToRow(double mouseY) {
        int y = (int)mouseY - (listTop + headerH()) + (int)scrollY;
        if (y < 0) return -1;
        int idx = y / ROW_H;
        return idx >= 0 && idx < rowCount() ? idx : -1;
    }

    private void ensureSelectedVisible() {
        if (selectedRow < 0) return;
        int topPix = (int)scrollY;
        int selTop = selectedRow * ROW_H;
        int selBot = selTop + ROW_H;
        int winTop = topPix;
        int winBot = topPix + (listHeight - headerH());

        if (selTop < winTop) {
            scrollY = selTop;
        } else if (selBot > winBot) {
            scrollY = selBot - (listHeight - headerH());
        }
        if (scrollY < 0) scrollY = 0;
    }

    private int rowToContentIndex(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= rows.size()) return -1;
        Row r = rows.get(rowIdx);
        if (r instanceof Row.BackRow) return -1;
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row rr = rows.get(i);
            if (rr instanceof Row.ItemRow ir) {
                if (i == rowIdx) return count;
                count++;
            }
        }
        return -1;
    }

    private int contentIndexToRow(int contentIdx) {
        if (contentIdx < 0) return -1;
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (r instanceof Row.ItemRow) {
                if (count == contentIdx) return i;
                count++;
            }
        }
        return -1;
    }

    // --- Screen lifecycle ----------------------------------------------------

    @Override
    protected void init() {
        int left = PAD;
        int top = PAD;
        int right = this.width - PAD;
        int bottom = this.height - PAD;

        // Left column
        int x = left;
        int y = top;

        // Filter
        filterBox = new EditBox(this.font, x, y, LEFT_W, 20, Component.literal("Filter"));
        filterBox.setHint(Component.literal("Filter…"));
        filterBox.setResponder(s -> rebuildRows());
        addRenderableWidget(filterBox);
        y += 24;

        // Add Key (save-handler so it adds to the *current* level)
        btnAddKey = Button.builder(Component.literal("Add Key Action"), b -> {
            var parent = this;
            this.minecraft.setScreen(new KeyActionEditScreen(
                    parent,
                    /* editing */ null,
                    (newItem, editingOrNull) -> {
                        List<MenuItem> target = current();
                        if (editingOrNull == null) {
                            target.add(newItem);
                        } else {
                            for (int i = 0; i < target.size(); i++) {
                                if (Objects.equals(target.get(i).id(), editingOrNull.id())) {
                                    target.set(i, newItem);
                                    break;
                                }
                            }
                        }
                        RadialMenu.persist();
                        rebuildRows();
                        int idx2 = -1;
                        for (int i2 = 0; i2 < rows.size(); i2++) {
                            Row r = rows.get(i2);
                            if (r instanceof Row.ItemRow ir && Objects.equals(ir.item().id(), newItem.id())) {
                                idx2 = i2; break;
                            }
                        }
                        if (idx2 >= 0) {
                            selectedRow = idx2;
                            ensureSelectedVisible();
                        }
                    }
            ));
        }).bounds(x, y, LEFT_W, 20).build();
        addRenderableWidget(btnAddKey);
        y += 24;

        // Add Command
        btnAddCmd = Button.builder(Component.literal("Add Command"), b -> {
            this.minecraft.setScreen(new CommandActionEditScreen(this, null));
        }).bounds(x, y, LEFT_W, 20).build();
        addRenderableWidget(btnAddCmd);
        y += 24;

        // Add Category
        btnAddCat = Button.builder(Component.literal("Add Category"), b -> {
            this.minecraft.setScreen(new CategoryEditScreen(this, null));
        }).bounds(x, y, LEFT_W, 20).build();
        addRenderableWidget(btnAddCat);
        y += 24;

        // Edit / Remove
        btnEdit = Button.builder(Component.literal("Edit Selected"), b -> onEditSelected())
                .bounds(x, y, LEFT_W, 20).build();
        addRenderableWidget(btnEdit);
        y += 24;

        btnRemove = Button.builder(Component.literal("Remove Selected"), b -> onRemoveSelected())
                .bounds(x, y, LEFT_W, 20).build();
        addRenderableWidget(btnRemove);
        y += 24;

        // Close
        btnClose = Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x, bottom - 22, LEFT_W, 20).build();
        addRenderableWidget(btnClose);

        // List area on the right
        listLeft = left + LEFT_W + PAD;
        listTop = top;
        listWidth = right - listLeft;
        listHeight = bottom - top;

        scrollY = 0;
        selectedRow = -1;
        dragging = false;
        dragRowIdx = -1;
        dropAt = -1;

        rebuildRows();
    }

    private void onEditSelected() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return;
        Row r = rows.get(selectedRow);
        if (r instanceof Row.BackRow) {
            RadialMenu.goBack();
            rebuildRows();
            return;
        }
        MenuItem mi = ((Row.ItemRow) r).item();

        if (mi.isCategory()) {
            this.minecraft.setScreen(new CategoryEditScreen(this, mi));
            return;
        }
        IClickAction act = mi.action();
        if (act == null) return;

        ClickActionType t = act.getType();
        if (t == ClickActionType.KEY) {
            var parent = this;
            this.minecraft.setScreen(new KeyActionEditScreen(
                    parent,
                    mi,
                    (newItem, editingOrNull) -> {
                        List<MenuItem> target = current();
                        for (int i = 0; i < target.size(); i++) {
                            if (Objects.equals(target.get(i).id(), newItem.id())) {
                                target.set(i, newItem);
                                break;
                            }
                        }
                        RadialMenu.persist();
                        rebuildRows();
                        int idx = -1;
                        for (int i = 0; i < rows.size(); i++) {
                            Row rr = rows.get(i);
                            if (rr instanceof Row.ItemRow ir && Objects.equals(ir.item().id(), newItem.id())) {
                                idx = i; break;
                            }
                        }
                        if (idx >= 0) {
                            selectedRow = idx;
                            ensureSelectedVisible();
                        }
                    }
            ));
        } else if (t == ClickActionType.COMMAND) {
            this.minecraft.setScreen(new CommandActionEditScreen(this, mi));
        }
    }

    private void onRemoveSelected() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return;
        Row r = rows.get(selectedRow);
        if (r instanceof Row.BackRow) return;

        MenuItem mi = ((Row.ItemRow) r).item();
        String id = mi.id();
        try {
            boolean ok = RadialMenu.removeInCurrent(id); // persist + save
            if (!ok) {
                Constants.LOG.info("[{}] Remove failed for '{}'.", Constants.MOD_NAME, id);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Remove exception for '{}': {}", Constants.MOD_NAME, id, t.toString());
        }
        selectedRow = -1;
        rebuildRows();
    }

    // --- Render --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(PAD, PAD, PAD + LEFT_W, this.height - PAD, 0xC0101010);
        g.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xC0101010);

        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 6, 0xFFFFFF);

        // Breadcrumb header inside list (only when not at root)
        int header = headerH();
        if (header > 0) {
            List<String> crumbs = RadialMenu.pathTitles();
            String path = String.join("/", crumbs);
            g.drawString(this.font, path, listLeft + 6, listTop + 4, 0xFFFFFF);
            // subtle divider
            g.fill(listLeft + 4, listTop + header - 2, listLeft + listWidth - 4, listTop + header - 1, 0x30FFFFFF);
        }

        int first = firstVisibleRow();
        int last = lastVisibleRow();

        hoveredRow = mouseToRow(mouseY);

        for (int i = first; i <= last; i++) {
            int y = listTop + header + (i * ROW_H) - (int)scrollY;
            if (y + ROW_H < listTop + header || y > listTop + listHeight) continue;

            boolean sel = (i == selectedRow);
            boolean hov = (i == hoveredRow);

            if (sel) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, HILITE);
            else if (hov) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, ROW_BG);

            Row r = rows.get(i);
            if (r instanceof Row.BackRow) {
                String txt = ChatFormatting.RED + "Back";
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - 9) / 2, 0xFF0000);
            } else if (r instanceof Row.ItemRow ir) {
                MenuItem mi = ir.item();
                int textX = listLeft + 8;
                IconSpec icon = mi.icon();
                if (icon != null) {
                    IconRenderer.drawIcon(g, listLeft + 8 + ICON_SZ / 2, y + ROW_H / 2, icon);
                    textX += ICON_SZ + 6;
                }

                String name = mi.title() == null ? "(untitled)" : mi.title();
                if (mi.isCategory()) name = "(RMB to open) " + name;
                g.drawString(this.font, name, textX, y + (ROW_H - 9) / 2, 0xFFFFFF);

                IClickAction act = mi.action();
                String t = (act != null) ? act.getType().name() : "CATEGORY";
                int tw = this.font.width(t);
                g.drawString(this.font, t, listLeft + listWidth - tw - 8, y + (ROW_H - 9) / 2, 0xA0A0A0);
            }
        }

        if (dragging && dragRowIdx >= 0 && dragRowIdx < rows.size()) {
            int yGhost = (int) (mouseY - dragGhostOffsetY);
            g.fill(listLeft, yGhost, listLeft + listWidth, yGhost + ROW_H, 0x40FFFFFF);

            Row r = rows.get(dragRowIdx);
            if (r instanceof Row.ItemRow ir) {
                MenuItem mi = ir.item();
                String name = mi.title() == null ? "(untitled)" : mi.title();
                if (mi.isCategory()) name = "(RMB to open) " + name;
                g.drawString(this.font, name, listLeft + 8, yGhost + (ROW_H - 9) / 2, 0xFFFFFF);
            } else if (r instanceof Row.BackRow) {
                g.drawString(this.font, ChatFormatting.RED + "Back", listLeft + 8, yGhost + (ROW_H - 9) / 2, 0xFF0000);
            }

            if (dropAt >= 0) {
                int yLine = listTop + header + (dropAt * ROW_H) - (int)scrollY;
                g.fill(listLeft, yLine - 1, listLeft + listWidth, yLine + 1, BLUE);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    // --- Mouse interaction ---------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inList = mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight;

        if (inList) {
            // Ignore clicks in breadcrumb header area
            if (!atRoot() && mouseY < listTop + HEADER_H) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            int idx = mouseToRow(mouseY);
            if (idx >= 0 && idx < rowCount()) {
                selectedRow = idx;
                ensureSelectedVisible();

                Row r = rows.get(idx);

                // RMB -> enter category (do not start drag)
                if (button == 1) {
                    if (r instanceof Row.ItemRow ir) {
                        MenuItem mi = ir.item();
                        if (mi.isCategory()) {
                            RadialMenu.enterCategory(mi);
                            scrollY = 0;
                            selectedRow = -1;
                            rebuildRows();
                            return true;
                        }
                    }
                    return true; // ignore RMB on BackRow / leaf rows for now
                }

                // LMB behavior: Back goes up; ItemRow starts drag (for both actions & categories)
                if (button == 0) {
                    if (r instanceof Row.BackRow) {
                        RadialMenu.goBack();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Row.ItemRow) {
                        dragging = true;
                        dragRowIdx = idx;
                        int rowTop = listTop + headerH() + (idx * ROW_H) - (int)scrollY;
                        dragGhostOffsetY = (int)mouseY - rowTop;
                        dropAt = computeDropAt(mouseY);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int computeDropAt(double mouseY) {
        int raw = mouseToRow(mouseY);
        if (raw < 0) {
            if (mouseY < listTop + headerH()) return atRoot() ? 0 : 1; // keep Back (if any) pinned
            if (mouseY > listTop + headerH() + rowCount() * ROW_H) return rowCount();
            return -1;
        }
        int within = (int)mouseY - (listTop + headerH() + (raw * ROW_H) - (int)scrollY);
        return (within < ROW_H / 2) ? raw : raw + 1;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            dropAt = computeDropAt(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            int fromRow = dragRowIdx;
            int toRow = dropAt;

            dragging = false;
            dragRowIdx = -1;

            if (fromRow >= 0 && toRow >= 0 && fromRow != toRow) {
                int fromContent = rowToContentIndex(fromRow);
                int toContent = rowToContentIndex(toRow);
                if (toContent < 0) toContent = contentCount(); // append

                if (fromContent >= 0 && toContent >= 0) {
                    try {
                        boolean ok = RadialMenu.moveInCurrent(fromContent, toContent); // write-through
                        if (!ok) {
                            Constants.LOG.info("[{}] Move failed: {} -> {}", Constants.MOD_NAME, fromContent, toContent);
                        }
                    } catch (Throwable t) {
                        Constants.LOG.warn("[{}] Move exception: {} -> {} : {}", Constants.MOD_NAME, fromContent, toContent, t.toString());
                    }
                    rebuildRows();
                    int newRow = contentIndexToRow(toContent > fromContent ? (toContent - 1) : toContent);
                    selectedRow = newRow;
                    ensureSelectedVisible();
                }
            }

            dropAt = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight) {

            int totalPx = rowCount() * ROW_H;
            int maxScroll = Math.max(0, totalPx - (listHeight - headerH()));
            scrollY -= deltaY * ROW_H * 2;
            if (scrollY < 0) scrollY = 0;
            if (scrollY > maxScroll) scrollY = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    /** Called by child edit screens after they save, so our list reflects changes immediately. */
    public void refreshFromChild() {
        try {
            this.rebuildRows();
        } catch (Throwable ignored) {}
    }

    // --- Close ---------------------------------------------------------------

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
