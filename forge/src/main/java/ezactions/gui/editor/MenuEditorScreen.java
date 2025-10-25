// MainFile: src/main/java/org/z2six/ezactions/gui/editor/MenuEditorScreen.java
package ezactions.gui.editor;

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
import org.z2six.ezactions.data.json.MenuImportExport;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.gui.editor.menu.MenuNavUtil;
import org.z2six.ezactions.gui.editor.menu.Rows;
import org.z2six.ezactions.gui.editor.menu.ScrollbarMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Menu Editor (main options screen).
 * - Left: action buttons (add key/command/category, edit, remove)
 * - Right: list of items. Categories show as "(RMB to open) Name". RMB enters category; LMB selects/drag.
 * - Breadcrumb + "Back to root" and optional "Back to XYZ" rows when inside a category.
 * - Drag & drop reorder; drop into a category; drop on Back rows to move out.
 * - Notes: if an item has a non-empty note, a gold 🔖 appears before the title with tooltip on hover.
 */
public final class MenuEditorScreen extends Screen {

    // Layout constants
    private static final int PAD = 8;
    private static final int LEFT_W = 160;
    private static final int ROW_H = 24;
    private static final int ICON_SZ = 18;

    // Drag visuals
    private static final int BLUE = 0x802478FF;
    private static final int BLUE_FULL = 0xFF2478FF;
    private static final int HILITE = 0x202478FF;
    private static final int ROW_BG = 0x20FFFFFF;

    // Scrollbar visuals
    private static final int SB_W = 6;
    private static final int SB_BG = 0x40000000;
    private static final int SB_KNOB = 0x80FFFFFF;
    private static final int SB_KNOB_MINH = 20;

    // Bookmark symbol
    private static final String BOOKMARK_SYM = "§6🔖§r";
    private static final int BOOKMARK_PAD_RIGHT = 4;

    private final Screen parent;

    // UI state
    private EditBox filterBox;
    private Button btnAddKey, btnAddCmd, btnAddCat, btnEdit, btnRemove;
    private Button btnImport, btnExport, btnClose, btnConfig;

    // List geometry
    private int listLeft, listTop, listWidth, listHeight;

    // Rows
    private final List<Rows> rows = new ArrayList<>();
    private int hoveredRow = -1;
    private int selectedRow = -1;

    // Scroll & drag
    private double scrollY = 0.0;
    private boolean dragging = false;
    private int dragRowIdx = -1;
    private int dragGhostOffsetY = 0;
    private int dropAt = -1;

    // Drop-into-category during drag
    private MenuItem dropTargetCategory = null;
    private int dropTargetRowIdx = -1;

    // Special drop targets
    private enum DropSpecial { NONE, BACK_ROOT, BACK_PARENT }
    private DropSpecial dropSpecial = DropSpecial.NONE;

    // Scrollbar drag
    private boolean sbDragging = false;
    private int sbGrabDy = 0;

    // --- Constructors --------------------------------------------------------

    public MenuEditorScreen() { this(null); }

    public MenuEditorScreen(Screen parent) {
        super(Component.literal("EZ Actions - Menu Editor"));
        this.parent = parent;
    }

    // --- Helpers -------------------------------------------------------------

    private static String safeNote(MenuItem mi) {
        try { return mi == null ? null : mi.note(); } catch (Throwable ignored) { return null; }
    }

    public static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t);
    }

    private boolean atRoot() { return !RadialMenu.canGoBack(); }

    private List<MenuItem> current() {
        List<MenuItem> it = RadialMenu.currentItems();
        return (it == null) ? List.of() : it;
    }

    /** Parent bundle title or "root" if at depth 1 (parent is root). */
    private String parentTitle() {
        try {
            List<String> parts = RadialMenu.pathTitles();
            if (parts != null && parts.size() >= 2) {
                return parts.get(parts.size() - 2);
            }
        } catch (Throwable ignored) {}
        return "root";
    }

    /** Header rows count depends on depth. */
    private int headerRowCount() {
        if (atRoot()) return 0;
        try {
            List<String> parts = RadialMenu.pathTitles();
            int n = (parts == null) ? 0 : parts.size();
            return (n >= 2) ? 3 : 2; // breadcrumb + back-root + (optional) back-parent
        } catch (Throwable ignored) {
            return 2;
        }
    }

    private void rebuildRows() {
        rows.clear();
        if (!atRoot()) {
            // Breadcrumb display
            String path = "root";
            try {
                List<String> parts = RadialMenu.pathTitles();
                if (parts != null && !parts.isEmpty()) path = String.join("/", parts);
            } catch (Throwable t) {
                Constants.LOG.debug("[{}] Breadcrumb build failed: {}", Constants.MOD_NAME, t.toString());
            }
            rows.add(new Rows.BreadcrumbRow(path));
            rows.add(new Rows.BackToRootRow());
            String parent = parentTitle();
            if (!"root".equalsIgnoreCase(parent)) {
                rows.add(new Rows.BackToParentRow(parent));
            }
        }

        String q = filterBox != null ? filterBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (MenuItem mi : current()) {
            if (q.isEmpty()) {
                rows.add(new Rows.ItemRow(mi));
            } else {
                String title = mi.title() == null ? "" : mi.title();
                if (title.toLowerCase(Locale.ROOT).contains(q)) {
                    rows.add(new Rows.ItemRow(mi));
                }
            }
        }

        if (selectedRow >= rows.size()) selectedRow = rows.size() - 1;
        if (selectedRow < -1) selectedRow = -1;
        clampScroll();

        dropTargetCategory = null;
        dropTargetRowIdx = -1;
        dropSpecial = DropSpecial.NONE;
    }

    private int rowCount() { return rows.size(); }
    private int contentCount() {
        int c = 0;
        for (Rows r : rows) if (r instanceof Rows.ItemRow) c++;
        return c;
    }
    private int visibleRowCount() { return Math.max(0, listHeight / ROW_H); }
    private int firstVisibleRow() { return Math.max(0, (int)Math.floor(scrollY / ROW_H)); }
    private int lastVisibleRow()  { return Math.min(rowCount() - 1, firstVisibleRow() + visibleRowCount()); }

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

        if (selTop < winTop) scrollY = selTop;
        else if (selBot > winBot) scrollY = selBot - listHeight;

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
        Rows r = rows.get(rowIdx);
        if (!(r instanceof Rows.ItemRow)) return -1;
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            Rows rr = rows.get(i);
            if (rr instanceof Rows.ItemRow ir) {
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
            Rows r = rows.get(i);
            if (r instanceof Rows.ItemRow) {
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

        // Add Key
        btnAddKey = Button.builder(Component.literal("Add Key Action"), b -> {
            var parent = this;
            this.minecraft.setScreen(new KeyActionEditScreen(
                    parent, null,
                    (newItem, editingOrNull) -> {
                        List<MenuItem> target = current();
                        if (editingOrNull == null) target.add(newItem);
                        else {
                            for (int i1 = 0; i1 < target.size(); i1++) {
                                if (Objects.equals(target.get(i1).id(), editingOrNull.id())) {
                                    target.set(i1, newItem); break;
                                }
                            }
                        }
                        RadialMenu.persist();
                        rebuildRows();
                        int idx2 = -1;
                        for (int i2 = 0; i2 < rows.size(); i2++) {
                            Rows r = rows.get(i2);
                            if (r instanceof Rows.ItemRow ir && Objects.equals(ir.item().id(), newItem.id())) {
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

        // --- Bottom-anchored controls (two rows, two columns) ----------------
        final int BTN_H = 20;
        final int VSTEP = 24;
        final int H_GAP = PAD;
        final int HALF_W = (LEFT_W - H_GAP) / 2;

        Component importLabel = Component.literal("⇩ Import").withStyle(ChatFormatting.AQUA);
        Component exportLabel = Component.literal("⇧ Export").withStyle(ChatFormatting.AQUA);

        int yRowBottom = bottom - 22;
        int xLeftCol   = x;
        int xRightCol  = x + HALF_W + H_GAP;

        btnClose = Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(xLeftCol, yRowBottom, HALF_W, BTN_H).build();
        addRenderableWidget(btnClose);

        btnConfig = Button.builder(Component.literal("Config"), b -> {
            try {
                this.minecraft.setScreen(new org.z2six.ezactions.gui.editor.config.ConfigScreen(this));
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Config button handler failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(xRightCol, yRowBottom, HALF_W, BTN_H).build();
        addRenderableWidget(btnConfig);

        int yRowTop = yRowBottom - VSTEP;

        btnImport = Button.builder(importLabel, b -> {
            try {
                int n = MenuImportExport.importFromClipboard();
                if (n >= 0) {
                    rebuildRows();
                    selectedRow = -1;
                    scrollY = 0;
                }
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Import button action failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(xLeftCol, yRowTop, HALF_W, BTN_H).build();
        addRenderableWidget(btnImport);

        btnExport = Button.builder(exportLabel, b -> {
            try {
                MenuImportExport.exportToClipboard();
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Export button action failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(xRightCol, yRowTop, HALF_W, BTN_H).build();
        addRenderableWidget(btnExport);
        // ---------------------------------------------------------------------

        // --- List area on the right ---
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

        dropTargetCategory = null;
        dropTargetRowIdx = -1;
        dropSpecial = DropSpecial.NONE;

        rebuildRows();
    }

    private void onEditSelected() {
        if (selectedRow < 0 || selectedRow >= rows.size()) return;
        Rows r = rows.get(selectedRow);

        if (r instanceof Rows.BackToRootRow) {
            MenuNavUtil.goToRoot();
            rebuildRows();
            return;
        } else if (r instanceof Rows.BackToParentRow) {
            RadialMenu.goBack();
            rebuildRows();
            return;
        }

        MenuItem mi = ((Rows.ItemRow) r).item();

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
                            Rows rr = rows.get(i);
                            if (rr instanceof Rows.ItemRow ir && Objects.equals(ir.item().id(), newItem.id())) {
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
        Rows r = rows.get(selectedRow);
        if (!(r instanceof Rows.ItemRow)) return;

        MenuItem mi = ((Rows.ItemRow) r).item();
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

        int first = firstVisibleRow();
        int last  = lastVisibleRow();

        hoveredRow = hitList(mouseX, mouseY) ? mouseToRow(mouseY) : -1;

        boolean tooltipRendered = false;

        for (int i = first; i <= last; i++) {
            int y = listTop + (i * ROW_H) - (int)scrollY;
            if (y + ROW_H < listTop || y > listTop + listHeight) continue;

            Rows r = rows.get(i);
            boolean isBreadcrumb = (r instanceof Rows.BreadcrumbRow);
            boolean isBackRoot   = (r instanceof Rows.BackToRootRow);
            boolean isBackParent = (r instanceof Rows.BackToParentRow);

            boolean sel = (i == selectedRow) && !isBreadcrumb;
            boolean hov = (i == hoveredRow) && !isBreadcrumb;

            if (sel) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, HILITE);
            else if (hov) g.fill(listLeft, y, listLeft + listWidth, y + ROW_H, ROW_BG);

            if (isBreadcrumb) {
                String txt = ((Rows.BreadcrumbRow) r).path();
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - this.font.lineHeight) / 2, 0xFFFFFFFF);

            } else if (isBackRoot) {
                String txt = ChatFormatting.RED + "Back to root";
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - this.font.lineHeight) / 2, 0xFF0000);

            } else if (isBackParent) {
                String parent = ((Rows.BackToParentRow) r).parentName();
                String txt = ChatFormatting.RED + "Back to " + parent;
                g.drawString(this.font, txt, listLeft + 8, y + (ROW_H - this.font.lineHeight) / 2, 0xFF0000);

            } else {
                // --- Normal item row (action or bundle) ---
                MenuItem mi = ((Rows.ItemRow) r).item();
                int textX = listLeft + 8;

                // Icon
                IconSpec icon = mi.icon();
                if (icon != null) {
                    IconRenderer.drawIcon(g, listLeft + 8 + ICON_SZ / 2, y + ROW_H / 2, icon);
                    textX += ICON_SZ + 6;
                }

                // Bookmark marker (for ANY item that has a note)
                int titleX = textX;
                String note = safeNote(mi);
                if (note != null && !note.isBlank()) {
                    String sym = BOOKMARK_SYM;
                    int symW = this.font.width(sym);
                    int symX = titleX;
                    int symY = y + (ROW_H - this.font.lineHeight) / 2;

                    g.drawString(this.font, sym, symX, symY, 0xFFFFAA);
                    titleX += symW + BOOKMARK_PAD_RIGHT;

                    if (!tooltipRendered) {
                        boolean over = (mouseX >= symX && mouseX <= symX + symW && mouseY >= y && mouseY <= y + ROW_H);
                        if (over) {
                            try {
                                Component noteC = mi.noteComponent();
                                if (noteC != null && !noteC.getString().isEmpty()) {
                                    g.renderTooltip(this.font, noteC, mouseX, mouseY);
                                    tooltipRendered = true;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                // Name (with RMB hint for categories) — now as Component
                Component nameC = mi.titleComponent();
                if (mi.isCategory()) {
                    nameC = Component.literal("(RMB to open) ")
                            .withStyle(ChatFormatting.RED)
                            .append(nameC);
                }
                g.drawString(this.font, nameC, titleX, y + (ROW_H - this.font.lineHeight) / 2, 0xFFFFFF, false);

                // Right-aligned type label (BUNDLE for categories)
                IClickAction act = mi.action();
                String t = (act != null) ? act.getType().name() : "BUNDLE";
                int tw = this.font.width(t);
                g.drawString(this.font, t, listLeft + listWidth - tw - 8, y + (ROW_H - this.font.lineHeight) / 2, 0xA0A0A0);
            }
        }

        // Drag ghost + insertion/outline (unchanged)
        if (dragging && dragRowIdx >= 0 && dragRowIdx < rows.size()) {
            int yGhost = mouseY - dragGhostOffsetY;
            g.fill(listLeft, yGhost, listLeft + listWidth, yGhost + ROW_H, 0x40FFFFFF);

            Rows r = rows.get(dragRowIdx);
            if (r instanceof Rows.ItemRow ir) {
                MenuItem mi = ir.item();
                int ghostTextX = listLeft + 8;

                IconSpec icon = mi.icon();
                if (icon != null) {
                    try {
                        IconRenderer.drawIcon(g, listLeft + 8 + ICON_SZ / 2, yGhost + ROW_H / 2, icon);
                        ghostTextX += ICON_SZ + 6;
                    } catch (Throwable ignored) {}
                }
                Component nameC = mi.titleComponent();
                if (mi.isCategory()) {
                    nameC = Component.literal("(RMB to open) ")
                            .withStyle(ChatFormatting.RED)
                            .append(nameC);
                }
                g.drawString(this.font, nameC, ghostTextX, yGhost + (ROW_H - this.font.lineHeight) / 2, 0xFFFFFF, false);
            }

            if (dropTargetCategory != null && dropTargetRowIdx >= 0) {
                int y = listTop + (dropTargetRowIdx * ROW_H) - (int)scrollY;
                drawBlueOutline(g, listLeft, y, listLeft + listWidth, y + ROW_H);
            } else if (dropSpecial != DropSpecial.NONE && dropTargetRowIdx >= 0) {
                int y = listTop + (dropTargetRowIdx * ROW_H) - (int)scrollY;
                drawBlueOutline(g, listLeft, y, listLeft + listWidth, y + ROW_H);
            } else if (dropAt >= 0) {
                int yLine = listTop + (dropAt * ROW_H) - (int) scrollY;
                g.fill(listLeft, yLine - 1, listLeft + listWidth, yLine + 1, BLUE);
            }
        }

        // Scrollbar
        ScrollbarMath.Metrics sb = ScrollbarMath.compute(
                listLeft, listTop, listWidth, listHeight,
                SB_W, SB_KNOB_MINH,
                rowCount(), ROW_H, scrollY
        );
        ScrollbarMath.draw(g, sb, SB_BG, SB_KNOB);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBlueOutline(GuiGraphics g, int x1, int y1, int x2, int y2) {
        final int s = 2;
        g.fill(x1, y1, x2, y1 + s, BLUE_FULL);
        g.fill(x1, y2 - s, x2, y2, BLUE_FULL);
        g.fill(x1, y1, x1 + s, y2, BLUE_FULL);
        g.fill(x2 - s, y1, x2, y2, BLUE_FULL);
    }

    // --- Mouse interaction ---------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Scrollbar first
        ScrollbarMath.Metrics sb = ScrollbarMath.compute(
                listLeft, listTop, listWidth, listHeight,
                SB_W, SB_KNOB_MINH,
                rowCount(), ROW_H, scrollY
        );
        boolean inScrollbar = mouseX >= sb.trackX1 && mouseX < sb.trackX2 && mouseY >= sb.trackY1 && mouseY < sb.trackY2;

        if (inScrollbar) {
            boolean inKnob = mouseY >= sb.knobY1 && mouseY <= sb.knobY2;
            if (inKnob && button == 0) {
                sbDragging = true;
                sbGrabDy = (int)mouseY - sb.knobY1;
                return true;
            }
            // click track: center knob there
            int desiredTop = (int)mouseY - (sb.knobY2 - sb.knobY1) / 2;
            int clampedTop = ScrollbarMath.clampKnobTop(sb, desiredTop);
            scrollY = ScrollbarMath.knobTopToScrollY(sb, ROW_H, rowCount(), listHeight, clampedTop);
            clampScroll();
            return true;
        }

        boolean inList = hitList(mouseX, mouseY);
        if (inList) {
            int idx = mouseToRow(mouseY);
            if (idx >= 0 && idx < rowCount()) {
                Rows r = rows.get(idx);
                selectedRow = (r instanceof Rows.BreadcrumbRow) ? -1 : idx;
                ensureSelectedVisible();

                if (button == 1) { // RMB
                    if (r instanceof Rows.BackToRootRow) {
                        MenuNavUtil.goToRoot();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Rows.BackToParentRow) {
                        RadialMenu.goBack();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Rows.ItemRow ir) {
                        MenuItem mi = ir.item();
                        if (mi.isCategory()) {
                            RadialMenu.enterCategory(mi);
                            scrollY = 0;
                            selectedRow = -1;
                            rebuildRows();
                            return true;
                        }
                    }
                } else if (button == 0) { // LMB
                    if (r instanceof Rows.BackToRootRow) {
                        MenuNavUtil.goToRoot();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Rows.BackToParentRow) {
                        RadialMenu.goBack();
                        scrollY = 0;
                        selectedRow = -1;
                        rebuildRows();
                        return true;
                    } else if (r instanceof Rows.ItemRow) {
                        // Start drag
                        dragging = true;
                        dragRowIdx = idx;
                        dragGhostOffsetY = (int)mouseY - (listTop + (idx * ROW_H) - (int)scrollY);
                        dropAt = computeDropAt(mouseY);
                        dropTargetCategory = null;
                        dropTargetRowIdx = -1;
                        dropSpecial = DropSpecial.NONE;
                        try {
                            int fromContent = rowToContentIndex(dragRowIdx);
                            Constants.LOG.debug("[{}] Drag start row={} contentIndex={}", Constants.MOD_NAME, dragRowIdx, fromContent);
                        } catch (Throwable ignored) {}
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int computeDropAt(double mouseY) {
        int raw = mouseToRow(mouseY);
        int header = headerRowCount();
        if (raw < 0) {
            if (mouseY < listTop) return header;
            if (mouseY > listTop + rowCount() * ROW_H) return rowCount();
            return -1;
        }
        if (raw < header) return header;

        int within = (int)mouseY - (listTop + (raw * ROW_H) - (int)scrollY);
        int pos = (within < ROW_H / 2) ? raw : (raw + 1);
        return Math.max(header, pos);
    }

    /** Recompute category/special targets while dragging. */
    private void updateDropTargets(double mouseY) {
        dropTargetCategory = null;
        dropTargetRowIdx = -1;
        dropSpecial = DropSpecial.NONE;

        int idx = mouseToRow(mouseY);
        if (idx < 0 || idx >= rows.size()) return;

        Rows r = rows.get(idx);

        if (r instanceof Rows.ItemRow ir) {
            MenuItem target = ir.item();
            if (target.isCategory()) {
                if (dragRowIdx >= 0 && dragRowIdx < rows.size()) {
                    Rows dr = rows.get(dragRowIdx);
                    if (dr instanceof Rows.ItemRow dir) {
                        MenuItem dragged = dir.item();
                        if (dragged != null && Objects.equals(dragged.id(), target.id())) {
                            return;
                        }
                    }
                }
                dropTargetCategory = target;
                dropTargetRowIdx = idx;
                return;
            }
        }

        if (r instanceof Rows.BackToRootRow) {
            dropSpecial = DropSpecial.BACK_ROOT;
            dropTargetRowIdx = idx; return;
        }
        if (r instanceof Rows.BackToParentRow) {
            dropSpecial = DropSpecial.BACK_PARENT;
            dropTargetRowIdx = idx;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sbDragging && button == 0) {
            ScrollbarMath.Metrics sb = ScrollbarMath.compute(
                    listLeft, listTop, listWidth, listHeight,
                    SB_W, SB_KNOB_MINH,
                    rowCount(), ROW_H, scrollY
            );
            int desiredTop = (int)mouseY - sbGrabDy;
            int clampedTop = ScrollbarMath.clampKnobTop(sb, desiredTop);
            scrollY = ScrollbarMath.knobTopToScrollY(sb, ROW_H, rowCount(), listHeight, clampedTop);
            clampScroll();
            return true;
        }
        if (dragging && button == 0) {
            updateDropTargets(mouseY);
            if (dropTargetCategory != null || dropSpecial != DropSpecial.NONE) {
                dropAt = -1;
            } else {
                dropAt = computeDropAt(mouseY);
            }
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

            // Special targets: move OUT to parent/root WITHOUT changing the final view
            if (dropSpecial != DropSpecial.NONE) {
                try {
                    int fromContent = rowToContentIndex(fromRow);
                    if (fromContent >= 0) {
                        List<MenuItem> cur = current();
                        if (fromContent < cur.size()) {
                            MenuItem moved = cur.remove(fromContent);
                            if (moved != null) {
                                boolean appended = false;

                                if (dropSpecial == DropSpecial.BACK_PARENT) {
                                    List<MenuItem> parent = RadialMenu.parentItems();
                                    if (parent != null) {
                                        parent.add(moved);
                                        appended = true;
                                    }
                                } else if (dropSpecial == DropSpecial.BACK_ROOT) {
                                    List<MenuItem> root = RadialMenu.rootMutable();
                                    if (root != null) {
                                        root.add(moved);
                                        appended = true;
                                    }
                                }

                                if (appended) {
                                    RadialMenu.persist();
                                } else {
                                    // rollback if we couldn't append
                                    int safeIdx = Math.min(fromContent, cur.size());
                                    cur.add(safeIdx, moved);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Drop-back exception: {}", Constants.MOD_NAME, t.toString());
                }

                rebuildRows();
                selectedRow = -1;
                ensureSelectedVisible();

                dropTargetCategory = null;
                dropTargetRowIdx = -1;
                dropSpecial = DropSpecial.NONE;
                dropAt = -1;
                return true;
            }

            // Drop INTO category
            if (dropTargetCategory != null) {
                try {
                    int fromContent = rowToContentIndex(fromRow);
                    if (fromContent >= 0) {
                        List<MenuItem> cur = current();
                        if (fromContent < cur.size()) {
                            MenuItem moved = cur.get(fromContent);

                            if (moved != null && !Objects.equals(moved.id(), dropTargetCategory.id())) {
                                boolean removed = false;
                                try { removed = cur.remove(moved); }
                                catch (Throwable t) {
                                    Constants.LOG.warn("[{}] Drop-into: removal failed: {}", Constants.MOD_NAME, t.toString());
                                }

                                if (removed) {
                                    try {
                                        dropTargetCategory.childrenMutable().add(moved);
                                        RadialMenu.persist();
                                        Constants.LOG.debug("[{}] Dropped '{}' into category '{}'", Constants.MOD_NAME, moved.id(), dropTargetCategory.id());
                                    } catch (Throwable t) {
                                        Constants.LOG.warn("[{}] Drop-into: append failed: {}", Constants.MOD_NAME, t.toString());
                                        try {
                                            int safeIdx = Math.min(fromContent, cur.size());
                                            cur.add(safeIdx, moved);
                                        } catch (Throwable ignored) {}
                                    }
                                } else {
                                    Constants.LOG.info("[{}] Drop-into: source item not removed (id='{}')", Constants.MOD_NAME, moved.id());
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Drop-into exception: {}", Constants.MOD_NAME, t.toString());
                }

                rebuildRows();
                selectedRow = -1;
                ensureSelectedVisible();

                dropTargetCategory = null;
                dropTargetRowIdx = -1;
                dropAt = -1;
                return true;
            }

            // Reorder within current
            if (fromRow >= 0 && toRow >= 0 && fromRow != toRow) {
                int fromContent = rowToContentIndex(fromRow);
                int toContent = rowToContentIndex(toRow);
                if (toContent < 0) toContent = contentCount();

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
            dropTargetCategory = null;
            dropTargetRowIdx = -1;
            dropSpecial = DropSpecial.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // Forge 1.20.1 signature (three args)
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        ScrollbarMath.Metrics sb = ScrollbarMath.compute(
                listLeft, listTop, listWidth, listHeight,
                SB_W, SB_KNOB_MINH,
                rowCount(), ROW_H, scrollY
        );
        boolean onListOrBar =
                (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight) ||
                        (mouseX >= sb.trackX1 && mouseX < sb.trackX2 && mouseY >= sb.trackY1 && mouseY < sb.trackY2);

        if (onListOrBar) {
            scrollY -= delta * ROW_H * 2;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean hitList(double mx, double my) {
        return mx >= listLeft && mx < listLeft + listWidth
                && my >= listTop && my < listTop + listHeight;
    }

    /** Called by child edit screens after they save, so our list reflects changes immediately. */
    public void refreshFromChild() {
        try {
            this.rebuildRows();
        } catch (Throwable ignored) {}
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
