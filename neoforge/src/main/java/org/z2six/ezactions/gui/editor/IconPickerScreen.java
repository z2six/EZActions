// MainFile: src/main/java/org/z2six/ezactions/gui/editor/IconPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;
import org.z2six.ezactions.util.CustomIconManager;
import org.z2six.ezactions.util.PinyinSearchUtil;

import org.z2six.ezactions.VanillaIconCache;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Scrollable icon grid (vanilla items + custom 16x16 PNG icons).
 *
 * Performance design:
 * - Vanilla icon index is built incrementally across ticks (no first-open freeze).
 * - Visible icons are hydrated first (name/search), then background hydration continues.
 * - Filtering runs asynchronously with debounce.
 */
public final class IconPickerScreen extends Screen implements NoMenuBlurScreen {

    private static final class PickEntry {
        final IconSpec icon;
        final String id;
        final boolean custom;
        final Item itemRef; // null for custom entries

        volatile String searchText;
        volatile String displayName;
        volatile boolean richReady;
        volatile boolean pinyinQueued;

        PickEntry(IconSpec icon, String id, String searchText, String displayName,
                  boolean custom, Item itemRef, boolean richReady) {
            this.icon = icon;
            this.id = id;
            this.searchText = searchText;
            this.displayName = displayName;
            this.custom = custom;
            this.itemRef = itemRef;
            this.richReady = richReady;
            this.pinyinQueued = false;
        }
    }

    private record FilterResult(int generation, List<PickEntry> matches) {}

    private final Screen parent;
    private final Consumer<IconSpec> onPick;

    // Cache delegated to VanillaIconCache (common module).

    // Async workers: filtering and optional pinyin enrichment.
    private static final ExecutorService FILTER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ezactions-icon-filter");
        t.setDaemon(true);
        t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return t;
    });
    private static final ExecutorService TOKEN_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ezactions-icon-pinyin");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private final List<PickEntry> allIcons = new ArrayList<>();
    private final List<PickEntry> filteredIcons = new ArrayList<>();
    private final List<PickEntry> localVanilla = new ArrayList<>();

    private int vanillaAttachedCount = 0;
    private int vanillaHydrationCursor = 0;
    private int vanillaHydratedCount = 0;

    private final ArrayDeque<PickEntry> priorityHydration = new ArrayDeque<>();
    private final IdentityHashMap<PickEntry, Boolean> priorityHydrationSeen = new IdentityHashMap<>();

    private String filter = "";
    private double scrollY = 0;
    private EditBox filterBox;

    private volatile FilterResult pendingFilterResult = null;
    private volatile boolean asyncFilterRefreshRequested = false;
    private boolean filterDirty = true;
    private boolean filterInFlight = false;
    private int filterGeneration = 0;
    private int appliedFilterGeneration = 0;
    private long filterDirtySinceNs = 0L;

    private boolean hydrationChangedSearch = false;
    private long lastHydrationFilterRefreshNs = 0L;

    private static final int PADDING = 12;
    private static final int CELL = 24;
    private static final int GAP = 8;

    private static final int PREFETCH_ROWS = 4;

    private static final int VANILLA_BUILD_MAX_PER_TICK = 320;
    private static final long VANILLA_BUILD_BUDGET_NS = 1_500_000L; // 1.5 ms/tick

    private static final int HYDRATE_MAX_PER_TICK = 64;
    private static final long HYDRATE_BUDGET_NS = 2_000_000L; // 2 ms/tick

    private static final int HYDRATE_MAX_PER_FRAME = 18;
    private static final long HYDRATE_BUDGET_FRAME_NS = 500_000L; // 0.5 ms/frame

    private static final long FILTER_DEBOUNCE_NS = 130_000_000L; // 130 ms
    private static final long HYDRATION_FILTER_REFRESH_NS = 220_000_000L; // 220 ms

    /** True when VanillaIconCache has indexed the full registry. */
    private static boolean isVicComplete() {
        return VanillaIconCache.isComplete();
    }

    // ── "Rebuild Cache" button (bottom area) ───────────────────────────────────
    private static final String REBUILD_LABEL = "Rebuild Cache";
    private int rebuildBtnLeft, rebuildBtnTop, rebuildBtnRight, rebuildBtnBottom;

    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0;

    public IconPickerScreen(Screen parent, Consumer<IconSpec> onPick) {
        super(Component.translatable("ezactions.gui.icon_picker.title"));
        this.parent = parent;
        this.onPick = onPick;
    }

    public static void open(Screen parent, Consumer<IconSpec> onPick) {
        Minecraft.getInstance().setScreen(new IconPickerScreen(parent, onPick));
    }

    @Override
    protected void init() {
        try {
            // Shrink filter box to make room for "Rebuild Cache" button on its right
            int btnAreaW = this.font.width(REBUILD_LABEL) + 8;
            int filterW = Math.max(120, this.width - PADDING * 2 - btnAreaW - 8);
            filterBox = new EditBox(this.font, PADDING, PADDING,
                    filterW, 18, Component.translatable("ezactions.gui.field.filter"));
            int btnX = PADDING + filterW + 8;
            int btnY = PADDING + 1;
            this.rebuildBtnLeft = btnX;
            this.rebuildBtnTop = btnY;
            this.rebuildBtnRight = btnX + this.font.width(REBUILD_LABEL);
            this.rebuildBtnBottom = btnY + this.font.lineHeight;
            filterBox.setValue(filter);
            filterBox.setSuggestion(Component.translatable("ezactions.gui.icon_picker.hint.filter").getString());
            filterBox.setResponder(s -> {
                filter = s == null ? "" : s;
                if (filter.isEmpty()) {
                    filterBox.setSuggestion(Component.translatable("ezactions.gui.icon_picker.hint.filter").getString());
                } else {
                    filterBox.setSuggestion("");
                }
                requestFilterRefresh(false);
            });
            addRenderableWidget(filterBox);

            allIcons.clear();
            filteredIcons.clear();
            localVanilla.clear();
            priorityHydration.clear();
            priorityHydrationSeen.clear();
            vanillaAttachedCount = 0;
            vanillaHydrationCursor = 0;
            vanillaHydratedCount = 0;
            scrollY = 0;
            filterInFlight = false;
            pendingFilterResult = null;
            asyncFilterRefreshRequested = false;
            hydrationChangedSearch = false;

            // Avoid expensive full folder+texture reload on every open.
            CustomIconManager.ensureLoaded();
            for (String id : CustomIconManager.listIds()) {
                String search = (id + " custom icon").toLowerCase(Locale.ROOT);
                allIcons.add(new PickEntry(IconSpec.custom(id), id, search, id, true, null, true));
            }

            // Kick off the cache if not already building; attach whatever is available.
            VanillaIconCache.tickWarmup();
            attachNewVanillaEntries();

            // When the full cache is already built (pre-warmed or loaded from disk),
            // skip async filter and populate filteredIcons immediately.
            if (VanillaIconCache.isComplete() && filter.isBlank()) {
                filteredIcons.clear();
                filteredIcons.addAll(allIcons);
                appliedFilterGeneration = ++filterGeneration;
                filterDirty = false;
                filterInFlight = false;
                pendingFilterResult = null;
            } else {
                requestFilterRefresh(true);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void tick() {
        // Continue lightweight vanilla index build without blocking UI.
        VanillaIconCache.tickWarmup();
        attachNewVanillaEntries();

        applyPendingFilterResult();
        processAsyncRefreshSignals();

        queueVisibleForPriorityHydration();
        processHydrationBudget(HYDRATE_MAX_PER_TICK, HYDRATE_BUDGET_NS);

        maybeSubmitFilterJob(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!inIconArea(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        double content = contentHeight();
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - deltaY * 32.0, 0, Math.max(0, content - view));
            queueVisibleForPriorityHydration();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (beginScrollbarDragIfHit(mx, my)) return true;

        // "Rebuild Cache" click
        if (mx >= rebuildBtnLeft && mx <= rebuildBtnRight && my >= rebuildBtnTop && my <= rebuildBtnBottom) {
            VanillaIconCache.invalidateDiskCache();
            init();
            return true;
        }

        if (!inIconArea(mx, my)) {
            return super.mouseClicked(mx, my, button);
        }
        int idx = iconIndexAt(mx, my);
        if (idx >= 0 && idx < filteredIcons.size()) {
            try {
                onPick.accept(filteredIcons.get(idx).icon);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Icon onPick failed: {}", Constants.MOD_NAME, t.toString());
            }
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            try {
                applyDragToScroll(my);
            } catch (Throwable ignored) {}
            queueVisibleForPriorityHydration();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Small per-frame budget so visible rows hydrate quickly while scrolling.
        applyPendingFilterResult();
        queueVisibleForPriorityHydration();
        processHydrationBudget(HYDRATE_MAX_PER_FRAME, HYDRATE_BUDGET_FRAME_NS);

        g.fill(0, 0, width, height, 0xA0000000);

        int gridLeft = gridLeft();
        int gridTop = gridTop();
        int gridRight = gridRight();
        int gridBottom = gridBottom();
        g.fill(gridLeft - 1, gridTop - 1, gridRight + 1, gridBottom + 1, 0x6E2B2B2B);
        g.fill(gridLeft, gridTop, gridRight, gridBottom, 0xD6101010);

        int cols = iconCols();
        int x0 = iconAreaLeft();
        int y0 = (int) (iconAreaTop() - scrollY);

        PickEntry hovered = null;
        int cellSpan = CELL + GAP;
        int total = filteredIcons.size();
        int totalRows = total <= 0 ? 0 : (int) Math.ceil(total / (double) cols);
        int firstRow = Math.max(0, (int) Math.floor(scrollY / cellSpan));
        int lastRow = Math.min(totalRows - 1, (int) Math.floor((scrollY + viewHeight()) / cellSpan) + 1);

        g.enableScissor(gridLeft, gridTop, gridRight, gridBottom);
        try {
            for (int row = firstRow; row <= lastRow; row++) {
                for (int col = 0; col < cols; col++) {
                    int i = row * cols + col;
                    if (i < 0 || i >= total) continue;

                    int cx = x0 + col * cellSpan;
                    int cy = y0 + row * cellSpan;
                    IconRenderer.drawIcon(g, cx + CELL / 2, cy + CELL / 2, filteredIcons.get(i).icon);
                    if (mouseX >= cx && mouseX <= cx + CELL && mouseY >= cy && mouseY <= cy + CELL) {
                        hovered = filteredIcons.get(i);
                    }
                }
            }
        } finally {
            g.disableScissor();
        }

        if (!inIconArea(mouseX, mouseY)) hovered = null;

        drawProgress(g);
        drawScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);

        // Rebuild Cache button — to the right of the search bar
        boolean hoveredBtn = mouseX >= rebuildBtnLeft && mouseX <= rebuildBtnRight
                && mouseY >= rebuildBtnTop && mouseY <= rebuildBtnBottom;
        g.drawString(this.font, REBUILD_LABEL, rebuildBtnLeft, rebuildBtnTop,
                hoveredBtn ? 0xFFFF8C00 : 0xFF6CFC05);

        if (hovered != null) {
            if (hovered.custom) {
                g.renderTooltip(this.font, Component.translatable("ezactions.gui.icon_picker.tooltip.custom", hovered.id), mouseX, mouseY);
            } else {
                g.renderTooltip(this.font, Component.translatable("ezactions.gui.icon_picker.tooltip.item", hovered.displayName, hovered.id), mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static String safeName(Item item) {
        try {
            return new ItemStack(item == null ? Items.BARRIER : item).getHoverName().getString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean needsPinyinTokens(String s) {
        if (s == null || s.isBlank()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return true;
        }
        return false;
    }

    /** Wrap VanillaIconCache entries into PickEntry objects and attach to the instance list. */
    private void attachNewVanillaEntries() {
        int total = VanillaIconCache.entryCount();
        if (vanillaAttachedCount >= total) return;

        List<VanillaIconCache.Entry> incoming = VanillaIconCache.slice(vanillaAttachedCount);
        if (incoming.isEmpty()) return;

        for (VanillaIconCache.Entry ce : incoming) {
            ResourceLocation rl = ResourceLocation.tryParse(ce.id());
            Item item = (rl == null) ? null : BuiltInRegistries.ITEM.get(rl);
            boolean rich = ce.displayName() != null && !ce.displayName().equals(ce.id());
            PickEntry pe = new PickEntry(
                    IconSpec.item(ce.id()), ce.id(),
                    ce.searchText(), ce.displayName(),
                    false, item, rich
            );
            allIcons.add(pe);
            localVanilla.add(pe);
            if (rich) vanillaHydratedCount++;
        }
        vanillaAttachedCount += incoming.size();

        requestFilterRefresh(false);
    }

    // (Now handled by the VanillaIconCache-based attachNewVanillaEntries above)

    private void requestFilterRefresh(boolean immediate) {
        filterDirty = true;
        filterDirtySinceNs = immediate ? 0L : System.nanoTime();
        if (immediate) {
            maybeSubmitFilterJob(true);
        }
    }

    private String normalizedFilter() {
        return filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
    }

    private void maybeSubmitFilterJob(boolean force) {
        if (filterInFlight || !filterDirty) {
            return;
        }

        long now = System.nanoTime();
        if (!force) {
            if (filterDirtySinceNs == 0L) {
                filterDirtySinceNs = now;
            }
            if ((now - filterDirtySinceNs) < FILTER_DEBOUNCE_NS) {
                return;
            }
        }

        final int generation = ++filterGeneration;
        final String query = normalizedFilter();
        final List<PickEntry> snapshot = List.copyOf(allIcons);

        filterDirty = false;
        filterInFlight = true;

        FILTER_EXECUTOR.execute(() -> {
            try {
                List<PickEntry> out = new ArrayList<>();
                if (query.isBlank()) {
                    out.addAll(snapshot);
                } else {
                    for (PickEntry entry : snapshot) {
                        String search = entry.searchText;
                        if (search != null && search.contains(query)) {
                            out.add(entry);
                        }
                    }
                }
                pendingFilterResult = new FilterResult(generation, out);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] IconPicker filter task failed: {}", Constants.MOD_NAME, t.toString());
                pendingFilterResult = new FilterResult(generation, List.of());
            }
        });
    }

    private void applyPendingFilterResult() {
        FilterResult result = pendingFilterResult;
        if (result == null) {
            return;
        }

        pendingFilterResult = null;
        filterInFlight = false;

        if (result.generation < appliedFilterGeneration) {
            return;
        }
        appliedFilterGeneration = result.generation;

        filteredIcons.clear();
        filteredIcons.addAll(result.matches);

        scrollY = clamp(scrollY, 0, Math.max(0, contentHeight() - viewHeight()));
    }

    private void processAsyncRefreshSignals() {
        if (asyncFilterRefreshRequested) {
            asyncFilterRefreshRequested = false;
            if (!normalizedFilter().isBlank()) {
                requestFilterRefresh(false);
            }
        }

        if (hydrationChangedSearch && !normalizedFilter().isBlank()) {
            long now = System.nanoTime();
            if ((now - lastHydrationFilterRefreshNs) >= HYDRATION_FILTER_REFRESH_NS) {
                hydrationChangedSearch = false;
                lastHydrationFilterRefreshNs = now;
                requestFilterRefresh(false);
            }
        }
    }

    private void enqueuePriorityHydration(PickEntry entry) {
        if (entry == null || entry.custom || entry.richReady) {
            return;
        }
        if (priorityHydrationSeen.put(entry, Boolean.TRUE) == null) {
            priorityHydration.addLast(entry);
        }
    }

    private PickEntry pollPriorityHydration() {
        while (!priorityHydration.isEmpty()) {
            PickEntry e = priorityHydration.pollFirst();
            priorityHydrationSeen.remove(e);
            if (e != null && !e.richReady && !e.custom) {
                return e;
            }
        }
        return null;
    }

    private PickEntry nextBackgroundHydrationEntry() {
        int n = localVanilla.size();
        if (n <= 0) {
            return null;
        }

        for (int scanned = 0; scanned < n; scanned++) {
            if (vanillaHydrationCursor >= n) {
                vanillaHydrationCursor = 0;
            }
            PickEntry e = localVanilla.get(vanillaHydrationCursor++);
            if (!e.richReady) {
                return e;
            }
        }
        return null;
    }

    private void queueVisibleForPriorityHydration() {
        int total = filteredIcons.size();
        if (total <= 0) {
            return;
        }

        int cols = iconCols();
        if (cols <= 0) {
            return;
        }

        int cellSpan = CELL + GAP;
        int totalRows = (int) Math.ceil(total / (double) cols);

        int firstRow = Math.max(0, (int) Math.floor(scrollY / cellSpan) - PREFETCH_ROWS);
        int lastRow = Math.min(totalRows - 1,
                (int) Math.floor((scrollY + viewHeight()) / cellSpan) + PREFETCH_ROWS + 1);

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (i < 0 || i >= total) continue;
                enqueuePriorityHydration(filteredIcons.get(i));
            }
        }
    }

    private void processHydrationBudget(int maxEntries, long budgetNs) {
        long start = System.nanoTime();
        int done = 0;
        boolean changed = false;

        while (done < maxEntries && (System.nanoTime() - start) < budgetNs) {
            PickEntry entry = pollPriorityHydration();
            if (entry == null) {
                entry = nextBackgroundHydrationEntry();
            }
            if (entry == null) {
                break;
            }

            if (hydrateEntry(entry)) {
                done++;
                changed = true;
            }
        }

        if (changed && !normalizedFilter().isBlank()) {
            hydrationChangedSearch = true;
        }
    }

    private boolean hydrateEntry(PickEntry entry) {
        if (entry == null || entry.custom || entry.richReady) {
            return false;
        }

        String name = safeName(entry.itemRef);
        if (name == null || name.isBlank()) {
            name = entry.id;
        }

        String baseSearch = (entry.id + " " + name + " item").toLowerCase(Locale.ROOT);
        entry.displayName = name;
        entry.searchText = baseSearch;
        entry.richReady = true;
        vanillaHydratedCount++;

        maybeQueuePinyin(entry, name);
        return true;
    }

    private void maybeQueuePinyin(PickEntry entry, String name) {
        if (entry == null || entry.pinyinQueued || !needsPinyinTokens(name)) {
            return;
        }
        entry.pinyinQueued = true;

        TOKEN_EXECUTOR.execute(() -> {
            try {
                PinyinSearchUtil.Tokens py = PinyinSearchUtil.tokens(name);
                if (py == null) {
                    return;
                }
                String extra = (py.spaced() + " " + py.compact() + " " + py.initials()).trim().toLowerCase(Locale.ROOT);
                if (extra.isBlank()) {
                    return;
                }
                String cur = entry.searchText == null ? "" : entry.searchText;
                entry.searchText = (cur + " " + extra).trim();
                asyncFilterRefreshRequested = true;
            } catch (Throwable t) {
                Constants.LOG.debug("[{}] IconPicker pinyin task failed for '{}': {}", Constants.MOD_NAME, entry.id, t.toString());
            }
        });
    }

    private void drawProgress(GuiGraphics g) {
        int built = VanillaIconCache.entryCount();
        int target = VanillaIconCache.targetCount();
        boolean buildDone = VanillaIconCache.isComplete();

        String txt;
        if (!buildDone && target > 0) {
            txt = "Indexing icons: " + built + "/" + Math.max(built, target);
        } else if (vanillaHydratedCount < Math.max(1, localVanilla.size())) {
            txt = "Preparing names: " + vanillaHydratedCount + "/" + localVanilla.size();
        } else {
            txt = "Icons: " + filteredIcons.size();
        }

        g.drawString(this.font, txt, gridLeft() + 6, gridBottom() - 11, 0xA0A0A0);
    }

    private int gridLeft() { return PADDING; }
    private int gridTop() { return PADDING + 24; }
    private int gridRight() { return width - PADDING; }
    private int gridBottom() { return height - PADDING; }
    private int iconAreaLeft() { return gridLeft() + 4; }
    private int iconAreaTop() { return gridTop() + 4; }
    private int iconAreaRight() { return gridRight() - 10; }
    private int iconAreaBottom() { return gridBottom() - 4; }
    private int iconAreaHeight() { return Math.max(1, iconAreaBottom() - iconAreaTop()); }

    private int iconCols() {
        int w = Math.max(1, iconAreaRight() - iconAreaLeft());
        return Math.max(1, w / (CELL + GAP));
    }

    private boolean inIconArea(double mx, double my) {
        return mx >= iconAreaLeft() && mx <= iconAreaRight() && my >= iconAreaTop() && my <= iconAreaBottom();
    }

    private int viewHeight() {
        return iconAreaHeight();
    }

    private double contentHeight() {
        int cols = iconCols();
        int rows = (int) Math.ceil(filteredIcons.size() / (double) cols);
        return rows * (CELL + GAP);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final class ScrollbarMetrics {
        int barX, barY, barW, barH;
        int knobY, knobH;
    }

    private ScrollbarMetrics computeScrollbarMetrics(double content, int view) {
        ScrollbarMetrics m = new ScrollbarMetrics();
        m.barW = 6;
        m.barX = gridRight() - m.barW - 2;
        m.barY = iconAreaTop();
        m.barH = view;

        double ratio = view / content;
        m.knobH = Math.max(20, (int) (m.barH * ratio));
        double denom = Math.max(1.0, content - view);
        m.knobY = (int) (m.barY + (m.barH - m.knobH) * (scrollY / denom));
        return m;
    }

    private void drawScrollbar(GuiGraphics g) {
        double content = contentHeight();
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        g.fill(m.barX, m.barY, m.barX + m.barW, m.barY + m.barH, 0x66101010);
        g.fill(m.barX + 1, m.knobY, m.barX + m.barW - 1, m.knobY + m.knobH, 0xFFFC0553);
    }

    private boolean beginScrollbarDragIfHit(double mx, double my) {
        double content = contentHeight();
        int view = viewHeight();
        if (content <= view) return false;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        boolean inKnob = mx >= m.barX + 1 && mx <= m.barX + m.barW - 1 && my >= m.knobY && my <= m.knobY + m.knobH;
        if (inKnob) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) (my - m.knobY);
            return true;
        }
        return false;
    }

    private void applyDragToScroll(double mouseY) {
        double content = contentHeight();
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        int minY = m.barY;
        int maxY = m.barY + m.barH - m.knobH;
        int newKnobY = (int) clamp(mouseY - dragGrabOffsetY, minY, maxY);

        double trackRange = (double) (m.barH - m.knobH);
        double t = trackRange <= 0 ? 0.0 : (newKnobY - m.barY) / trackRange;
        double maxScroll = Math.max(0, content - view);
        scrollY = clamp(t * maxScroll, 0, maxScroll);
    }

    private int iconIndexAt(double mx, double my) {
        if (!inIconArea(mx, my)) return -1;

        int cols = iconCols();
        int cellSpan = CELL + GAP;
        int localX = (int) (mx - iconAreaLeft());
        int localY = (int) (my - iconAreaTop() + scrollY);
        if (localX < 0 || localY < 0) return -1;

        int col = localX / cellSpan;
        int row = localY / cellSpan;
        if (col < 0 || col >= cols || row < 0) return -1;

        // Ignore clicks inside inter-cell gap.
        if ((localX % cellSpan) > CELL || (localY % cellSpan) > CELL) return -1;

        int idx = row * cols + col;
        return (idx >= 0 && idx < filteredIcons.size()) ? idx : -1;
    }
}

