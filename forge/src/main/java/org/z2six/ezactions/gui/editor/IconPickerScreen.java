// MainFile: src/main/java/org/z2six/ezactions/gui/editor/IconPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Scrollable icon grid (vanilla items + custom 16x16 PNG icons). */
public final class IconPickerScreen extends Screen implements NoMenuBlurScreen {

    private record PickEntry(IconSpec icon, String id, String searchText, String displayName, boolean custom) {}

    private final Screen parent;
    private final Consumer<IconSpec> onPick;
    private static volatile List<PickEntry> CACHED_VANILLA = null;
    private final List<PickEntry> allIcons = new ArrayList<>();
    private final List<PickEntry> filteredIcons = new ArrayList<>();
    private String filter = "";
    private double scrollY = 0;
    private EditBox filterBox;

    private static final int PADDING = 12;
    private static final int CELL = 24;
    private static final int GAP = 8;

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
            filterBox = new EditBox(this.font, PADDING, PADDING,
                    Math.max(120, this.width - PADDING * 2 - 20), 18, Component.translatable("ezactions.gui.field.filter"));
            filterBox.setValue(filter);
            filterBox.setHint(Component.translatable("ezactions.gui.icon_picker.hint.filter"));
            filterBox.setResponder(s -> {
                filter = s;
                updateFiltered();
                double content = contentHeight();
                double view = viewHeight();
                scrollY = clamp(scrollY, 0, Math.max(0, content - view));
            });
            addRenderableWidget(filterBox);

            allIcons.clear();

            // Custom icons first.
            CustomIconManager.reload();
            for (String id : CustomIconManager.listIds()) {
                String search = (id + " custom icon").toLowerCase(Locale.ROOT);
                allIcons.add(new PickEntry(IconSpec.custom(id), id, search, id, true));
            }

            // Vanilla item icons (cached once per session).
            allIcons.addAll(cachedVanillaEntries());

            allIcons.sort((a, b) -> {
                if (a.custom != b.custom) return a.custom ? -1 : 1;
                return a.id.compareToIgnoreCase(b.id);
            });

            updateFiltered();
            scrollY = clamp(scrollY, 0, Math.max(0, contentHeight() - viewHeight()));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inIconArea(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        double content = contentHeight();
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - delta * 32.0, 0, Math.max(0, content - view));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (beginScrollbarDragIfHit(mx, my)) return true;

        if (!inIconArea(mx, my)) {
            return super.mouseClicked(mx, my, button);
        }
        int idx = iconIndexAt(mx, my);
        if (idx >= 0 && idx < filteredIcons.size()) {
            try { onPick.accept(filteredIcons.get(idx).icon); }
            catch (Throwable t) { Constants.LOG.warn("[{}] Icon onPick failed: {}", Constants.MOD_NAME, t.toString()); }
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            try { applyDragToScroll(my); } catch (Throwable ignored) {}
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

        drawScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);

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
        try { return new ItemStack(item == null ? Items.BARRIER : item).getHoverName().getString(); }
        catch (Throwable ignored) { return ""; }
    }

    private void updateFiltered() {
        filteredIcons.clear();
        if (filter == null || filter.isBlank()) {
            filteredIcons.addAll(allIcons);
            return;
        }
        String f = filter.toLowerCase(Locale.ROOT);
        for (PickEntry entry : allIcons) {
            if (entry.searchText.contains(f)) {
                filteredIcons.add(entry);
            }
        }
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

    private static List<PickEntry> cachedVanillaEntries() {
        List<PickEntry> cache = CACHED_VANILLA;
        if (cache != null) return cache;
        synchronized (IconPickerScreen.class) {
            cache = CACHED_VANILLA;
            if (cache != null) return cache;

            List<PickEntry> built = new ArrayList<>();
            for (var e : BuiltInRegistries.ITEM.entrySet()) {
                ResourceLocation rl = e.getKey().location();
                String id = rl.getNamespace() + ":" + rl.getPath();
                Item item = e.getValue();
                String name = safeName(item);
                PinyinSearchUtil.Tokens py = PinyinSearchUtil.tokens(name);
                String search = (id + " " + name + " item "
                        + py.spaced() + " " + py.compact() + " " + py.initials()).toLowerCase(Locale.ROOT);
                built.add(new PickEntry(IconSpec.item(id), id, search, name, false));
            }
            built.sort((a, b) -> a.id.compareToIgnoreCase(b.id));
            CACHED_VANILLA = List.copyOf(built);
            return CACHED_VANILLA;
        }
    }
}

