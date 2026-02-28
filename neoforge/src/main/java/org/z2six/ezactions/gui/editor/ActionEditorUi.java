package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.gui.IconRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shared visual style and panel helpers for action editor screens. */
public final class ActionEditorUi {

    public static final int LABEL_COLOR = 0x9FB0C4;
    public static final int BODY_TEXT = 0xDCE7F5;
    public static final int MUTED_TEXT = 0x8FA0B5;
    public static final int CARD_BG = 0xCC121B27;
    public static final int CARD_EDGE = 0x6E2D425A;

    private static final int SCRIM_BG = 0x96060B11;
    private static final int PANEL_BG = 0xDE0F1722;
    private static final int PANEL_BORDER = 0xAA2C425B;
    private static final int HEADER_BG = 0xF0182433;
    private static final int HEADER_ACCENT = 0xFF49A0FF;

    private ActionEditorUi() {}

    public static Panel panel(int screenW, int screenH, int desiredW, int desiredH, int margin) {
        int safeMargin = Math.max(6, margin);
        int w = Math.max(360, Math.min(desiredW, screenW - (safeMargin * 2)));
        int h = Math.max(240, Math.min(desiredH, screenH - (safeMargin * 2)));
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        return new Panel(x, y, w, h);
    }

    public static void drawFrame(GuiGraphics g, Font font, int screenW, int screenH, Panel panel, Component title) {
        g.fill(0, 0, screenW, screenH, SCRIM_BG);

        g.fill(panel.x() - 2, panel.y() - 2, panel.right() + 2, panel.bottom() + 2, PANEL_BORDER);
        g.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), PANEL_BG);

        g.fill(panel.x(), panel.y(), panel.right(), panel.y() + 26, HEADER_BG);
        g.fill(panel.x() + 1, panel.y() + 25, panel.right() - 1, panel.y() + 26, HEADER_ACCENT);

        g.drawCenteredString(font, title, panel.x() + (panel.w() / 2), panel.y() + 9, 0xEAF2FC);
    }

    public static void drawCard(GuiGraphics g, Font font, int x, int y, int w, int h, Component heading) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, CARD_EDGE);
        g.fill(x, y, x + w, y + h, CARD_BG);
        if (heading != null && !heading.getString().isEmpty()) {
            g.drawString(font, heading, x + 8, y + 6, LABEL_COLOR);
        }
    }

    public static void drawCard(GuiGraphics g, Font font, int x, int y, int w, int h, String heading) {
        drawCard(g, font, x, y, w, h, heading == null ? Component.empty() : Component.literal(heading));
    }

    public static void drawFieldLabel(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, LABEL_COLOR);
    }

    public static void drawFieldLabel(GuiGraphics g, Font font, String text, int x, int y) {
        drawFieldLabel(g, font, text == null ? Component.empty() : Component.literal(text), x, y);
    }

    public static void drawIconCard(GuiGraphics g, Font font, int x, int y, int size, Component label, IconSpec icon) {
        drawIconCard(g, font, x, y, size, label, icon, false);
    }

    public static void drawIconCard(GuiGraphics g, Font font, int x, int y, int size, Component label, IconSpec icon, boolean hovered) {
        int edge = hovered ? 0xFF66B5FF : 0x6E3A506A;
        int fill = hovered ? 0xE61A2A3D : 0xD60F1722;
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, edge);
        g.fill(x, y, x + size, y + size, fill);
        if (label != null && !label.getString().isEmpty()) {
            int lw = font.width(label);
            g.drawString(font, label, x + Math.max(0, (size - lw) / 2), y - 10, MUTED_TEXT);
        }
        try {
            IconRenderer.drawIcon(g, x + (size / 2), y + (size / 2), icon);
        } catch (Throwable ignored) {}
    }

    public static void drawIconCard(GuiGraphics g, Font font, int x, int y, int size, String label, IconSpec icon) {
        drawIconCard(g, font, x, y, size, label == null ? Component.empty() : Component.literal(label), icon, false);
    }

    public static void drawIconCard(GuiGraphics g, Font font, int x, int y, int size, String label, IconSpec icon, boolean hovered) {
        drawIconCard(g, font, x, y, size, label == null ? Component.empty() : Component.literal(label), icon, hovered);
    }

    public static EditorButton button(int x, int y, int w, int h, Component label, Runnable onPress) {
        return new EditorButton(x, y, w, h, label, onPress);
    }

    public static <T> EditorCycleButton<T> cycleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            List<T> values,
            T initialValue,
            Function<T, Component> valueText,
            Consumer<T> onValueChanged
    ) {
        return new EditorCycleButton<>(x, y, w, h, label, values, initialValue, valueText, onValueChanged);
    }

    @SafeVarargs
    public static <T> EditorCycleButton<T> cycleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            T initialValue,
            Function<T, Component> valueText,
            Consumer<T> onValueChanged,
            T... values
    ) {
        return new EditorCycleButton<>(x, y, w, h, label, Arrays.asList(values), initialValue, valueText, onValueChanged);
    }

    public record Panel(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
    }

    public static final class ScrollArea {
        private static final int SCROLL_STEP = 18;
        private static final int SB_W = 4;
        private static final int SB_TRACK = 0x5530455D;
        private static final int SB_KNOB = 0xCC87A7CC;

        private final List<Entry> widgets = new ArrayList<>();
        private int contentTop = Integer.MAX_VALUE;
        private int contentBottom = Integer.MIN_VALUE;
        private int offset = 0;
        private int maxOffset = 0;

        public <T extends AbstractWidget> T track(T widget) {
            if (widget == null) return null;
            widgets.add(new Entry(widget, widget.getY()));
            include(widget.getY(), widget.getY() + widget.getHeight());
            return widget;
        }

        public void include(int yTop, int yBottom) {
            contentTop = Math.min(contentTop, yTop);
            contentBottom = Math.max(contentBottom, yBottom);
        }

        public int y(int baseY) {
            return baseY - offset;
        }

        public int offset() {
            return offset;
        }

        public void reset() {
            offset = 0;
        }

        public void layout(int viewTop, int viewBottom) {
            if (contentTop == Integer.MAX_VALUE || contentBottom == Integer.MIN_VALUE) {
                maxOffset = 0;
                offset = 0;
                return;
            }
            maxOffset = Math.max(0, contentBottom - viewBottom + 4);
            offset = Math.max(0, Math.min(offset, maxOffset));

            for (Entry e : widgets) {
                int y = e.baseY - offset;
                e.widget.setY(y);
                e.widget.visible = (y + e.widget.getHeight() > viewTop) && (y < viewBottom);
            }
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double deltaY,
                                     int viewX, int viewY, int viewW, int viewH) {
            boolean inside = mouseX >= viewX && mouseX <= viewX + viewW && mouseY >= viewY && mouseY <= viewY + viewH;
            if (!inside || maxOffset <= 0) return false;
            int before = offset;
            if (deltaY < 0) offset = Math.min(maxOffset, offset + SCROLL_STEP);
            else if (deltaY > 0) offset = Math.max(0, offset - SCROLL_STEP);
            return before != offset;
        }

        public void drawScrollbar(GuiGraphics g, int viewX, int viewY, int viewW, int viewH) {
            if (maxOffset <= 0) return;

            int contentH = Math.max(1, contentBottom - contentTop);
            int x1 = viewX + viewW - SB_W - 2;
            int x2 = x1 + SB_W;
            int y1 = viewY + 2;
            int y2 = viewY + viewH - 2;
            int trackH = Math.max(1, y2 - y1);

            int knobH = Math.max(14, (int) ((viewH / (float) contentH) * trackH));
            knobH = Math.min(trackH, knobH);
            int travel = Math.max(1, trackH - knobH);
            int knobY = y1 + (int) ((offset / (float) maxOffset) * travel);

            g.fill(x1, y1, x2, y2, SB_TRACK);
            g.fill(x1, knobY, x2, knobY + knobH, SB_KNOB);
        }

        private record Entry(AbstractWidget widget, int baseY) {}
    }
}
