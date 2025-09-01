// MainFile: src/main/java/org/z2six/minemenuplus/gui/RadialScreenDraw.java
package org.z2six.minemenuplus.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.menu.MenuItem;

import java.util.List;

/**
 * Safe drawing helpers for the radial menu (icons, highlights). Handles empty lists.
 */
public final class RadialScreenDraw {

    private RadialScreenDraw() {}

    public static final double LABEL_RADIUS = 95.0;

    public static void drawRing(GuiGraphics g, Font font, int cx, int cy, List<MenuItem> items, int hoveredIndex) {
        try {
            if (items == null || items.isEmpty()) {
                g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
                g.drawCenteredString(font, "Empty category", cx, cy - 24, 0xAAAAAA);
                g.drawCenteredString(font, "Right-click to go back", cx, cy - 10, 0xAAAAAA);
                return;
            }

            int n = Math.max(1, items.size());
            double step = Math.PI * 2.0 / n;

            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);

            for (int i = 0; i < n; i++) {
                double angle = (-Math.PI / 2.0) + i * step;
                int ix = cx + (int) Math.round(Math.cos(angle) * LABEL_RADIUS);
                int iy = cy + (int) Math.round(Math.sin(angle) * LABEL_RADIUS);

                if (i == hoveredIndex) {
                    g.fill(ix - 10, iy - 10, ix + 10, iy + 10, 0x40FFFF00);
                }

                if (i < items.size()) {
                    MenuItem mi = items.get(i);
                    // Your IconRenderer expects IconSpec; MenuItem.icon() already returns that type
                    IconRenderer.drawIcon(g, ix, iy, mi.icon());
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] drawRing error: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
