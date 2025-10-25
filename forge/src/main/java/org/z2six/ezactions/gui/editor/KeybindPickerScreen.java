// MainFile: src/main/java/org/z2six/ezactions/gui/editor/KeybindPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.*;
import java.util.function.Consumer;

 //* // MainFile: KeybindPickerScreen.java
 //* Scrollable, category-grouped keybind list with readable labels.
 //* Calls onPick.accept(km.getName()) -> e.g. "key.inventory"
 //* Defensive logs; no crashes.
 //*
 //* Added:
 //* - Click-and-drag scrollbar knob.
 //* - Robust math for knob <-> scrollY mapping.
 //* - Debug logs for layout/drag diagnostics.
 //*/
public final class KeybindPickerScreen extends Screen implements NoMenuBlurScreen {

    private final Screen parent;
    private final Consumer<String> onPick;

    private static final int PADDING = 12;
    private static final int ROW_H = 20;
    private static final int BUTTON_W = 60;

    private double scrollY = 0;
    private final List<Row> rows = new ArrayList<>();

    // Drag state for scrollbar knob
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0; // distance from knob top to cursor when drag starts

    private static final class Row {
        final boolean header;
        final Component label;    // localized text for display
        final KeyMapping mapping; // null for header
        Row(boolean header, Component label, KeyMapping mapping) {
            this.header = header; this.label = label; this.mapping = mapping;
        }
    }

    public KeybindPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.literal("Choose Keybinding"));
        this.parent = parent;
        this.onPick = onPick;
    }

    public static void open(Screen parent, Consumer<String> onPick) {
        Minecraft.getInstance().setScreen(new KeybindPickerScreen(parent, onPick));
    }

    @Override
    protected void init() {
        try {
            Options opts = Objects.requireNonNull(Minecraft.getInstance().options);
            KeyMapping[] all = Objects.requireNonNull(opts.keyMappings);

            Map<String, List<KeyMapping>> byCat = new HashMap<>();
            for (KeyMapping km : all) {
                byCat.computeIfAbsent(km.getCategory(), k -> new ArrayList<>()).add(km);
            }

            List<String> cats = new ArrayList<>(byCat.keySet());
            cats.sort(Comparator.comparing(k -> Component.translatable(k).getString(), String.CASE_INSENSITIVE_ORDER));

            rows.clear();
            for (String cat : cats) {
                rows.add(new Row(true, Component.translatable(cat), null));
                List<KeyMapping> list = byCat.get(cat);
                list.sort(Comparator.comparing(km -> Component.translatable(km.getName()).getString(), String.CASE_INSENSITIVE_ORDER));
                for (KeyMapping km : list) {
                    rows.add(new Row(false, Component.translatable(km.getName()), km));
                }
            }
            Constants.LOG.info("[{}] KeybindPicker: built {} rows ({} categories).", Constants.MOD_NAME, rows.size(), cats.size());
            // Reset scroll if content shrank
            scrollY = clamp(scrollY, 0, Math.max(0, rows.size() * ROW_H - viewHeight()));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- Scroll wheel (Forge 1.20.1 signature) ------------------------------
    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double content = rows.size() * ROW_H;
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - delta * 32.0, 0, content - view);
        }
        return true;
    }

    // --- Click handling ------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        // 1) Try scrollbar knob first so it "wins" over list clicks.
        if (beginScrollbarDragIfHit(mx, my)) {
            return true;
        }

        // 2) Fall back to list button clicks
        int x = PADDING;
        int y = (int) (PADDING - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;
            if (r.header) continue;

            int btnX = x + Math.min(usableW, 360);
            int btnY = ry + 3;

            if (mx >= btnX && mx <= btnX + BUTTON_W && my >= btnY && my <= btnY + (ROW_H - 6)) {
                try {
                    String mappingKey = r.mapping.getName(); // e.g. "key.inventory"
                    Constants.LOG.info("[{}] KeybindPicker: picked {}", Constants.MOD_NAME, mappingKey);
                    onPick.accept(mappingKey);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] KeybindPicker onPick failed: {}", Constants.MOD_NAME, t.toString());
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            Constants.LOG.debug("[{}] KeybindPicker: scrollbar drag end (scrollY={})", Constants.MOD_NAME, scrollY);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            try {
                applyDragToScroll(my);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeybindPicker drag update failed: {}", Constants.MOD_NAME, t.toString());
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // --- Render --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);

        int x = PADDING;
        int y = (int) (PADDING - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;

            if (r.header) {
                g.drawString(this.font, r.label, x, ry + 4, 0xFFFFAA);
                g.fill(x, ry + ROW_H - 2, x + usableW, ry + ROW_H - 1, 0x40FFFFFF);
            } else {
                g.drawString(this.font, r.label, x, ry + 5, 0xFFFFFF);

                int btnX = x + Math.min(usableW, 360);
                int btnY = ry + 3;
                int btnW = BUTTON_W;
                int btnH = ROW_H - 6;

                g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x40000000);
                Component b = Component.literal("Use");
                int tw = this.font.width(b);
                g.drawString(this.font, b, btnX + (btnW - tw) / 2, btnY + 5, 0xFFFFFF);
            }
        }

        drawScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    // --- Scrollbar math / drawing -------------------------------------------

    private int viewHeight() {
        return height - PADDING * 2;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void drawScrollbar(GuiGraphics g) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        // Track
        g.fill(m.barX, m.barY, m.barX + m.barW, m.barY + m.barH, 0x40000000);
        // Knob
        g.fill(m.barX + 1, m.knobY, m.barX + m.barW - 1, m.knobY + m.knobH, 0x80FFFFFF);
    }

    private static final class ScrollbarMetrics {
        int barX, barY, barW, barH;
        int knobY, knobH;
    }

    /* Compute positions/sizes for track & knob given current scrollY. */
    private ScrollbarMetrics computeScrollbarMetrics(double content, int view) {
        ScrollbarMetrics m = new ScrollbarMetrics();
        m.barW = 6;
        m.barX = width - PADDING - m.barW;
        m.barY = PADDING;
        m.barH = view;

        double ratio = view / content;
        m.knobH = Math.max(20, (int) (m.barH * ratio));

        // Place knob according to current scrollY
        double denom = Math.max(1.0, content - view);
        m.knobY = (int) (m.barY + (m.barH - m.knobH) * (scrollY / denom));
        return m;
    }

    /* If clicked within knob, start dragging; returns true if drag began. */
    private boolean beginScrollbarDragIfHit(double mx, double my) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return false;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        boolean inKnob = mx >= m.barX + 1 && mx <= m.barX + m.barW - 1 && my >= m.knobY && my <= m.knobY + m.knobH;
        if (inKnob) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) (my - m.knobY);
            Constants.LOG.debug("[{}] KeybindPicker: scrollbar drag start (grabOffsetY={}, scrollY={})",
                    Constants.MOD_NAME, dragGrabOffsetY, scrollY);
            return true;
        }
        // Optional: jump-to-click outside knob (page up/down). We skip to keep behavior simple.
        return false;
    }

    /* While dragging, convert mouseY back to scrollY using inverse mapping. */
    private void applyDragToScroll(double mouseY) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);

        // Desired knob top from drag, clamped to track
        int minY = m.barY;
        int maxY = m.barY + m.barH - m.knobH;
        int newKnobY = (int) clamp(mouseY - dragGrabOffsetY, minY, maxY);

        // Inverse mapping: knob pos -> scrollY
        double trackRange = (double) (m.barH - m.knobH);
        double t = trackRange <= 0 ? 0.0 : (newKnobY - m.barY) / trackRange;
        double maxScroll = Math.max(0, content - view);
        scrollY = clamp(t * maxScroll, 0, maxScroll);
    }

    // ------------------------------------------------------------------------

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
