// MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/ScrollbarMath.java
package ezactions.gui.editor.menu;

import net.minecraft.client.gui.GuiGraphics;

/**
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/menu/ScrollbarMath.java
 *
 * Scrollbar geometry + drawing helpers for a single vertical track/knob.
 * This keeps MenuEditorScreen lean and focused on interactions.
 */
public final class ScrollbarMath {

    public static final class Metrics {
        public final int trackX1, trackY1, trackX2, trackY2;
        public final int knobX1, knobY1, knobX2, knobY2;

        private Metrics(int t1, int ty1, int t2, int ty2, int k1, int ky1, int k2, int ky2) {
            this.trackX1 = t1; this.trackY1 = ty1; this.trackX2 = t2; this.trackY2 = ty2;
            this.knobX1  = k1; this.knobY1  = ky1; this.knobX2  = k2; this.knobY2  = ky2;
        }
    }

    public static Metrics compute(
            int listLeft, int listTop, int listWidth, int listHeight,
            int sbWidth, int sbKnobMinH,
            int totalRows, int rowH,
            double scrollY
    ) {
        int totalPx = totalRows * rowH;

        int trackX1 = listLeft + listWidth - sbWidth;
        int trackX2 = listLeft + listWidth;
        int trackY1 = listTop;
        int trackY2 = listTop + listHeight;

        int knobX1 = trackX1 + 1;
        int knobX2 = trackX2 - 1;

        if (totalPx <= listHeight) {
            // Content fits: full-length "knob"
            return new Metrics(trackX1, trackY1, trackX2, trackY2, knobX1, trackY1, knobX2, trackY1 + listHeight);
        }

        double ratio = (double) listHeight / (double) totalPx;
        int knobH = Math.max(sbKnobMinH, (int) (listHeight * ratio));
        int maxScroll = Math.max(0, totalPx - listHeight);
        int knobY = (maxScroll <= 0) ? trackY1
                : (int) (trackY1 + (listHeight - knobH) * (scrollY / maxScroll));

        return new Metrics(trackX1, trackY1, trackX2, trackY2, knobX1, knobY, knobX2, knobY + knobH);
    }

    public static void draw(GuiGraphics g, Metrics m, int trackColor, int knobColor) {
        // Track
        g.fill(m.trackX1, m.trackY1, m.trackX2, m.trackY2, trackColor);
        // Knob
        g.fill(m.knobX1, m.knobY1, m.knobX2, m.knobY2, knobColor);
    }

    public static int clampKnobTop(Metrics m, int desiredTop) {
        int knobH = m.knobY2 - m.knobY1;
        int minTop = m.trackY1;
        int maxTop = m.trackY2 - knobH;
        return Math.max(minTop, Math.min(maxTop, desiredTop));
    }

    public static double knobTopToScrollY(Metrics m, int rowH, int totalRows, int listHeight, int knobTop) {
        int knobH = m.knobY2 - m.knobY1;
        int totalPx = totalRows * rowH;
        int maxScroll = Math.max(0, totalPx - listHeight);
        if (totalPx <= listHeight) return 0;

        double t = (double) (knobTop - m.trackY1) / (double) (listHeight - knobH);
        return t * maxScroll;
    }
}
