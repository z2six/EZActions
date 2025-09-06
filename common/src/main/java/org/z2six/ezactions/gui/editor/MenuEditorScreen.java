// MainFile: src/main/java/org/z2six/ezactions/gui/editor/MenuEditorScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionType;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.data.json.MenuImportExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Menu Editor (main options screen).
 * - Left: action buttons (add key/command/category, edit, remove)
 * - Right: list of items. Categories show as "(RMB to open) Name". RMB enters category; LMB selects/drag.
 * - When inside a category, a white breadcrumb row appears above a red "Back" row; clicking Back goes to parent.
 * - Drag & drop to reorder, with a blue insertion indicator line.
 * - Scrollbar & mouse wheel supported (like pickers).
 * - All mutations are persisted via RadialMenu helpers.
 */
public final class MenuEditorScreen extends Screen {

    // Layout constants
    private static final int PAD = 8;
    private static final int LEFT_W = 160;
    private static final int ROW_H = 24;
    private static final int ICON_SZ = 18;

    // Drag visuals
    private static final int BLUE = 0x802478FF;
    private static final int HILITE = 0x202478FF;
    private static final int ROW_BG = 0x20FFFFFF;

    // Scrollbar visuals
    private static final int SB_W = 6;        // scrollbar width
    private static final int SB_BG = 0x40000000;
    private static final int SB_KNOB = 0x80FFFFFF;
    private static final int SB_KNOB_MINH = 20;

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
    private Button btnExport;
    private Button btnImport;

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

    // Scrollbar drag
    private boolean sbDragging = false;
    private int sbGrabDy = 0; // mouse offset inside knob while dragging

    // --- Row model -----------------------------------------------------------

    private sealed interface Row {
        record BreadcrumbRow(String path) implements Row {}
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

    /** Live list for whatever level the editor is currently showing. */
    private List<MenuItem> current() {
        List<MenuItem> it = RadialMenu.currentItems();
        return (it == null) ? List.of() : it;
    }

    private void rebuildRows() {
        rows.clear();
        if (!atRoot()) {
            // Breadcrumb path from RadialMenu (defensive fallback to "root")
            String path = "root";
            try {
                List<String> parts = org.z2six.ezactions.data.menu.RadialMenu.pathTitles();
                if (parts != null && !parts.isEmpty()) {
                    path = String.join("/", parts);
                }
            } catch (Throwable t) {
                Constants.LOG.debug("[{}] Breadcrumb build failed: {}", Constants.MOD_NAME, t.toString());
            }
            rows.add(new Row.BreadcrumbRow(path)); // white label
            rows.add(new Row.BackRow());           // red clickable back
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
        clampScroll();
    }

    private int contentCount() {
        int c = 0;
        for (Row r : rows) if (r instanceof Row.ItemRow) c++;
        return c;
    }

    private int rowCount() { return rows.size(); }

    private int visibleRowCount() { return Math.max(0, listHeight / ROW_H); }

    private int firstVisibleRow() { return Math.max(0, (int)Math.floor(scrollY / ROW_H)); }

    private int lastVisibleRow() { return Math.min(rowCount() - 1, firstVisibleRow() + visibleRowCount()); }

    private int mouseToRow(double mouseY) {
        int y = (int)mouseY - listTop + (int)scrollY;
        if (y < 0) return -1;
        int idx = y / ROW_H;
        return idx >= 0 && idx < rowCount() ? idx : -1;
    }

    private void ensureSelectedVisible() {
        if (selectedRow < 0) return;
        int selTop = selectedRow * ROW_H;
        int selBot = selTop + ROW_H;
        int winTop = (int)scrollY;
        int winBot = winTop + listHeight;

        if (selTop < winTop) {
            scrollY = selTop;
        } else if (selBot > winBot) {
            scrollY = selBot - listHeight;
        }
        clampScroll();
    }

    private void clampScroll() {
        int totalPx = rowCount() * ROW_H;
        int maxScroll = Math.max(0, totalPx - listHeight);
        if (scrollY < 0) scrollY = 0;
        if (scrollY > maxScroll) scrollY = maxScroll;
    }

    private int rowToContentIndex(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= rows.size()) return -1;
        Row r = rows.get(rowIdx);
        if (r instanceof Row.BackRow || r instanceof Row.BreadcrumbRow) return -1;
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

        // Add Key (wired with save-handler so it adds to the *current* level)
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
                        // select the newly added/edited row
                        int idx = -1;
                        for (int i = 0; i < rows.size(); i++) {
                            Row r = rows.get(i);
                            if (r instanceof Row.ItemRow ir && Objects.equals(ir.item().id(), newItem.id())) {
                                idx = i; break;
                            }
                        }
                        if (idx >= 0) {
                            selectedRow = idx;
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
        btnAddCat = Button.builder(Component.literal("Add Bundle"), b -> {
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

        // --- Import / Export  ---
        final int BTN_H = 20;
        final int VSTEP = 24; // consistent spacing

        // Labels with simple Unicode arrows; tinted so they stand out
        Component importLabel = Component.literal("⇩ Import").withStyle(ChatFormatting.AQUA);
        Component exportLabel = Component.literal("⇧ Export").withStyle(ChatFormatting.AQUA);

        // Keep Close at bottom - 22 (your convention)
        int yClose = bottom - 22;
        int yExport = yClose - VSTEP;   // directly above Close
        int yImport = yExport - VSTEP;  // above Export

        btnImport = Button.builder(importLabel, b -> {
            try {
                int n = MenuImportExport.importFromClipboard();
                if (n >= 0) {
                    // Refresh UI list after replace
                    rebuildRows();
                    selectedRow = -1;
                    scrollY = 0;
                }
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Import button action failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(x, yImport, LEFT_W, BTN_H).build();
        addRenderableWidget(btnImport);

        btnExport = Button.builder(exportLabel, b -> {
            try {
                MenuImportExport.exportToClipboard();
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Export button action failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(x, yExport, LEFT_W, BTN_H).build();
        addRenderableWidget(btnExport);

        // Close (kept at bottom)
        btnClose = Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x, yClose, LEFT_W, BTN_H).build();
        addRenderableWidget(btnClose);

        // --- List area on the right (unchanged below) ---
        listLeft = left + LEFT_W + PAD;
        listTop = top;
        listWidth = right - listLeft;
        listHeight = bottom - top;

        scrollY = 0;
        selectedRow = -1;
        dragging = false;
        dragRowIdx = -1;
        dropAt = -1;
        sbDragging = false;

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
        if (r instanceof Row.BackRow || r instanceof Row.BreadcrumbRow) return;

        MenuItem mi = ((Row.ItemRow) r).item();
        String id = mi.id();
        try {
            boolean ok = RadialMenu.removeFromCurrent(id);
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
        // Background panels
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(PAD, PAD, PAD + LEFT_W, this.height - PAD, 0xC0101010);
        g.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xC0101010);

        // Title
        int panelCenterX = listLeft + (listWidth / 2);
        g.drawCenteredString(this.font, this.title.getString(), panelCenterX, 6, 0xFFFFFF);

        // Render list
        int first = firstVisibleRow();
        int last = lastVisibleRow();

        hoveredRow = mouseToRow(mouseY);

        for (int i = first; i <= last; i++) {
            int y = listTop + (i * ROW_H) - (int)scrollY;
            if (y + ROW_H < listTop || y > listTop + listHeight) continue;

            Row r = rows.get(i);
            boolean isBreadcrumb = (r instanceof Row.BreadcrumbRow);
            boolean isBack = (r instanceof Row.BackRow);

            boolean sel = (i == selectedRow) && !isBreadcrumb; // don't highlight breadcrumb
            boolean hov = (i == hoveredRow) && !isBreadcrumb;

            if (sel) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, HILITE);
            else if (hov) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, ROW_BG);

            if (isBreadcrumb) {
                String txt = ((Row.BreadcrumbRow) r).path();
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - 9) / 2, 0xFFFFFFFF);
            } else if (isBack) {
                String txt = ChatFormatting.RED + "Back";
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - 9) / 2, 0xFF0000);
            } else {
                MenuItem mi = ((Row.ItemRow) r).item();
                int textX = listLeft + 8;

                IconSpec icon = mi.icon();
                if (icon != null) {
                    IconRenderer.drawIcon(g, listLeft + 8 + ICON_SZ / 2, y + ROW_H / 2, icon);
                    textX += ICON_SZ + 6;
                }

                String name = mi.title() == null ? "(untitled)" : mi.title();
                if (mi.isCategory()) name = "§c(RMB to open)§r " + name;
                g.drawString(this.font, name, textX, y + (ROW_H - 9) / 2, 0xFFFFFF);

                IClickAction act = mi.action();
                String t = (act != null) ? act.getType().name() : "BUNDLE";
                int tw = this.font.width(t);
                g.drawString(this.font, t, listLeft + listWidth - tw - 8, y + (ROW_H - 9) / 2, 0xA0A0A0);
            }
        }

        // Drag ghost + insertion line (restored)
        if (dragging && dragRowIdx >= 0 && dragRowIdx < rows.size()) {
            int yGhost = mouseY - dragGhostOffsetY;
            // semi-transparent strip where the dragged row is following the mouse
            g.fill(listLeft, yGhost, listLeft + listWidth, yGhost + ROW_H, 0x40FFFFFF);

            Row r = rows.get(dragRowIdx);
            if (r instanceof Row.ItemRow ir) {
                MenuItem mi = ir.item();

                int ghostTextX = listLeft + 8;

                // draw icon if present, like normal rows
                IconSpec icon = mi.icon();
                if (icon != null) {
                    try {
                        IconRenderer.drawIcon(g, listLeft + 8 + ICON_SZ / 2, yGhost + ROW_H / 2, icon);
                        ghostTextX += ICON_SZ + 6;
                    } catch (Throwable ignored) {}
                }

                String name = mi.title() == null ? "(untitled)" : mi.title();
                if (mi.isCategory()) name = "§c(RMB to open)§r " + name;
                g.drawString(this.font, name, ghostTextX, yGhost + (ROW_H - 9) / 2, 0xFFFFFF);

            } else if (r instanceof Row.BackRow) {
                g.drawString(this.font, ChatFormatting.RED + "Back", listLeft + 8, yGhost + (ROW_H - 9) / 2, 0xFF0000);

            } else if (r instanceof Row.BreadcrumbRow br) {
                g.drawString(this.font, br.path(), listLeft + 8, yGhost + (ROW_H - 9) / 2, 0xFFFFFFFF);
            }

            // blue insertion line where the item would drop
            if (dropAt >= 0) {
                int yLine = listTop + (dropAt * ROW_H) - (int) scrollY;
                g.fill(listLeft, yLine - 1, listLeft + listWidth, yLine + 1, BLUE);
            }
        }

        // Scrollbar
        drawScrollbar(g);

        // Draw widgets last (so they appear above panels)
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics g) {
        int totalPx = rowCount() * ROW_H;
        if (totalPx <= listHeight) return; // no need

        int trackX1 = listLeft + listWidth - SB_W;
        int trackX2 = listLeft + listWidth;
        int trackY1 = listTop;
        int trackY2 = listTop + listHeight;
        g.fill(trackX1, trackY1, trackX2, trackY2, SB_BG);

        double ratio = (double) listHeight / (double) totalPx;
        int knobH = Math.max(SB_KNOB_MINH, (int) (listHeight * ratio));
        int maxScroll = totalPx - listHeight;
        int knobY = (maxScroll <= 0) ? trackY1
                : (int) (trackY1 + (listHeight - knobH) * (scrollY / maxScroll));

        g.fill(trackX1 + 1, knobY, trackX2 - 1, knobY + knobH, SB_KNOB);
    }

    // --- Mouse interaction ---------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Scrollbar hit-test first
        if (hitScrollbar(mouseX, mouseY)) {
            int[] k = knobRect();
            boolean inKnob = mouseY >= k[1] && mouseY <= k[3];
            if (inKnob && button == 0) {
                sbDragging = true;
                sbGrabDy = (int)mouseY - k[1];
                return true;
            }
            // click track: jump knob
            jumpScrollTo(mouseY);
            return true;
        }

        // Only react if click inside list area for selection / navigation / drag
        boolean inList = mouseX >= listLeft && mouseX < listLeft + listWidth
                && mouseY >= listTop && mouseY < listTop + listHeight;

        if (inList) {
            int idx = mouseToRow(mouseY);
            if (idx >= 0 && idx < rowCount()) {
                Row r = rows.get(idx);

                // NEW: ignore clicks on breadcrumb (non-interactive)
                if (r instanceof Row.BreadcrumbRow) {
                    return true;
                }

                selectedRow = idx;
                ensureSelectedVisible();

                if (button == 1) {
                    // RMB: Back or enter category
                    if (r instanceof Row.BackRow) {
                        RadialMenu.goBack();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Row.ItemRow ir) {
                        MenuItem mi = ir.item();
                        if (mi.isCategory()) {
                            RadialMenu.enterCategory(mi);
                            scrollY = 0;
                            selectedRow = -1;
                            rebuildRows();
                            return true;
                        }
                    }
                } else if (button == 0) {
                    // LMB on item: select/drag; on Back: treat as back for convenience
                    if (r instanceof Row.BackRow) {
                        RadialMenu.goBack();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Row.ItemRow) {
                        // Start drag
                        dragging = true;
                        dragRowIdx = idx;
                        dragGhostOffsetY = (int)mouseY - (listTop + (idx * ROW_H) - (int)scrollY);
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
        int header = atRoot() ? 0 : 2; // breadcrumb + back when not at root
        if (raw < 0) {
            if (mouseY < listTop) return header; // clamp to first content row
            if (mouseY > listTop + rowCount() * ROW_H) return rowCount();
            return -1;
        }
        // Never insert within the header block; clamp to first item row
        if (raw < header) return header;

        int within = (int)mouseY - (listTop + (raw * ROW_H) - (int)scrollY);
        int pos = (within < ROW_H / 2) ? raw : (raw + 1);
        return Math.max(header, pos);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sbDragging && button == 0) {
            dragScrollTo(mouseY - sbGrabDy);
            return true;
        }
        if (dragging && button == 0) {
            dropAt = computeDropAt(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sbDragging && button == 0) {
            sbDragging = false;
            return true;
        }
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
                        boolean ok = RadialMenu.moveInCurrent(fromContent, toContent);
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
        // Scroll only when over the list or scrollbar
        if (hitList(mouseX, mouseY) || hitScrollbar(mouseX, mouseY)) {
            scrollY -= deltaY * ROW_H * 2; // two rows per wheel notch
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private boolean hitList(double mx, double my) {
        return mx >= listLeft && mx < listLeft + listWidth
                && my >= listTop && my < listTop + listHeight;
    }

    private boolean hitScrollbar(double mx, double my) {
        int x1 = listLeft + listWidth - SB_W;
        int x2 = listLeft + listWidth;
        return mx >= x1 && mx < x2 && my >= listTop && my < listTop + listHeight;
    }

    private int[] knobRect() {
        int totalPx = rowCount() * ROW_H;
        int trackX1 = listLeft + listWidth - SB_W;
        int trackX2 = listLeft + listWidth;
        int trackY1 = listTop;
        int trackY2 = listTop + listHeight;

        if (totalPx <= listHeight) {
            return new int[]{trackX1 + 1, trackY1, trackX2 - 1, trackY1 + listHeight};
        }

        double ratio = (double) listHeight / (double) totalPx;
        int knobH = Math.max(SB_KNOB_MINH, (int) (listHeight * ratio));
        int maxScroll = totalPx - listHeight;
        int knobY = (maxScroll <= 0) ? trackY1
                : (int) (trackY1 + (listHeight - knobH) * (scrollY / maxScroll));

        return new int[]{trackX1 + 1, knobY, trackX2 - 1, knobY + knobH};
    }

    private void jumpScrollTo(double mouseY) {
        int totalPx = rowCount() * ROW_H;
        if (totalPx <= listHeight) { scrollY = 0; return; }

        int[] k = knobRect();
        int trackY1 = listTop;
        int knobH = k[3] - k[1];
        int knobTop = (int)mouseY - knobH / 2;
        dragScrollTo(knobTop);
    }

    private void dragScrollTo(double knobTopY) {
        int totalPx = rowCount() * ROW_H;
        int maxScroll = Math.max(0, totalPx - listHeight);
        if (totalPx <= listHeight) { scrollY = 0; return; }

        int trackY1 = listTop;
        int trackY2 = listTop + listHeight;
        int[] k = knobRect();
        int knobH = k[3] - k[1];

        int minTop = trackY1;
        int maxTop = trackY2 - knobH;
        int clampedTop = Math.max(minTop, Math.min(maxTop, (int)knobTopY));

        double t = (double)(clampedTop - trackY1) / (double)(listHeight - knobH);
        scrollY = t * maxScroll;
        clampScroll();
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
