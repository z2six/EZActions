// MainFile: src/main/java/org/z2six/ezactions/gui/editor/IconPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

//*
 //* // MainFile: IconPickerScreen.java
 //* Scrollable icon grid (mouse wheel + visible scrollbar) with a text filter (EditBox).
 //* Returns IconSpec via callback (onPick).
 //* Defensive logging; never crashes.
 //*
 //* Added:
 //* - Click-and-drag scrollbar knob (keeps wheel scrolling).
 //* - Centralized geometry so render & input share the same math.
 //* - Debug logs for drag start/end and layout diagnostics.
 //*/
public final class IconPickerScreen extends Screen implements NoMenuBlurScreen {

    private final Screen parent;
    private final Consumer<IconSpec> onPick;
    private final List<String> allIcons = new ArrayList<>();
    private String filter = "";
    private double scrollY = 0;
    private EditBox filterBox;

    // layout
    private static final int PADDING = 12;
    private static final int CELL = 24;
    private static final int GAP = 8;

    // Scrollbar drag state
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0; // distance from knob top to cursor when drag starts

    public IconPickerScreen(Screen parent, Consumer<IconSpec> onPick) {
        super(Component.literal("Choose Icon"));
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
                    Math.max(120, this.width - PADDING * 2 - 20), 18, Component.literal("Filter"));
            filterBox.setValue(filter);
            filterBox.setResponder(s -> {
                filter = s;
                // keep scroll in-bounds when the result set shrinks/expands
                double content = contentHeight();
                double view = viewHeight();
                scrollY = clamp(scrollY, 0, Math.max(0, content - view));
            });
            addRenderableWidget(filterBox);

            // populate minecraft:item ids
            var reg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            for (var e : reg.entrySet()) {
                ResourceLocation id = e.getKey().location();
                allIcons.add(id.getNamespace() + ":" + id.getPath());
            }
            allIcons.sort(String::compareToIgnoreCase);

            // ensure scroll is valid for current content/view
            scrollY = clamp(scrollY, 0, Math.max(0, contentHeight() - viewHeight()));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // 1.20.1 override: (mouseX, mouseY, delta). Wheel delta is a single value.
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double content = contentHeight();
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - delta * 32.0, 0, content - view);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        // 1) Scrollbar knob gets priority so it isn't blocked by grid/filter clicks
        if (beginScrollbarDragIfHit(mx, my)) {
            return true;
        }

        // 2) Grid selection
        int left = PADDING;
        int top = PADDING + 24; // below filter
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int x0 = left;
        int y0 = (int) (top - scrollY);

        List<String> filtered = filtered();
        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x0 + col * (CELL + GAP);
            int cy = y0 + row * (CELL + GAP);
            if (mx >= cx && mx <= cx + CELL && my >= cy && my <= cy + CELL) {
                String id = filtered.get(i);
                try {
                    onPick.accept(IconSpec.item(id));
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Icon onPick failed: {}", Constants.MOD_NAME, t.toString());
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            try {
                applyDragToScroll(my);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] IconPicker drag update failed: {}", Constants.MOD_NAME, t.toString());
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            Constants.LOG.debug("[{}] IconPicker: scrollbar drag end (scrollY={})", Constants.MOD_NAME, scrollY);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);

        int left = PADDING;
        int top = PADDING + 24;
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int x0 = left;
        int y0 = (int) (top - scrollY);

        List<String> filtered = filtered();
        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x0 + col * (CELL + GAP);
            int cy = y0 + row * (CELL + GAP);
            IconRenderer.drawIcon(g, cx + CELL / 2, cy + CELL / 2, IconSpec.item(filtered.get(i)));
        }

        drawScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // helpers

    private List<String> filtered() {
        if (filter == null || filter.isBlank()) return allIcons;
        String f = filter.toLowerCase(Locale.ROOT);
        return allIcons.stream().filter(s -> s.contains(f)).toList();
    }

    private int viewHeight() {
        return height - (PADDING + 24) - PADDING;
    }

    private double contentHeight() {
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int rows = (int) Math.ceil(filtered().size() / (double) cols);
        return rows * (CELL + GAP);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---- Scrollbar geometry & interaction ----------------------------------

    private static final class ScrollbarMetrics {
        int barX, barY, barW, barH;
        int knobY, knobH;
    }

    private ScrollbarMetrics computeScrollbarMetrics(double content, int view) {
        ScrollbarMetrics m = new ScrollbarMetrics();
        m.barW = 6;
        m.barX = width - PADDING - m.barW;
        m.barY = PADDING + 24; // aligned with grid top (under the filter)
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
        // Track
        g.fill(m.barX, m.barY, m.barX + m.barW, m.barY + m.barH, 0x40000000);
        // Knob
        g.fill(m.barX + 1, m.knobY, m.barX + m.barW - 1, m.knobY + m.knobH, 0x80FFFFFF);
    }

    /* Start drag if mouse is inside knob; returns true if drag began. */
    private boolean beginScrollbarDragIfHit(double mx, double my) {
        double content = contentHeight();
        int view = viewHeight();
        if (content <= view) return false;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        boolean inKnob = mx >= m.barX + 1 && mx <= m.barX + m.barW - 1 && my >= m.knobY && my <= m.knobY + m.knobH;
        if (inKnob) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) (my - m.knobY);
            Constants.LOG.debug("[{}] IconPicker: scrollbar drag start (grabOffsetY={}, scrollY={})",
                    Constants.MOD_NAME, dragGrabOffsetY, scrollY);
            return true;
        }
        // Optional: click on the track to page up/down — intentionally omitted to avoid surprises.
        return false;
    }

    /* Convert current mouseY (while dragging) back into scrollY. */
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
}
