// MainFile: src/main/java/org/z2six/minemenuplus/gui/RadialScreenDraw.java
package org.z2six.minemenuplus.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.config.RadialConfig;
import org.z2six.minemenuplus.data.menu.MenuItem;

import java.util.List;

/** Drawing helpers for radial ring with filled slices + icons. */
public final class RadialScreenDraw {

    private RadialScreenDraw() {}

    // icon radius placed mid-ring
    private static double iconRadius(double rInner, double rOuter) {
        return (rInner + rOuter) * 0.5;
    }

    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx, RadialScreenMath.Radii rr) {
        try {
            if (items == null || items.isEmpty()) {
                // Minimal crosshair only (no hint text)
                g.fill(cx - 1, cy - 6, cx + 1, cy + 6, 0xFFFFFFFF);
                g.fill(cx - 6, cy - 1, cx + 6, cy + 1, 0xFFFFFFFF);
                return;
            }

            RadialConfig cfg = RadialConfig.get();
            int n = Math.max(1, items.size());
            double step = Math.PI * 2.0 / n;

            // Draw all slices (filled ring sectors)
            for (int i = 0; i < n; i++) {
                double a0 = (-Math.PI / 2.0) + i * step;
                double a1 = a0 + step;
                int color = (i == hoveredIdx) ? cfg.hoverColor : cfg.ringColor;
                fillRingSector(g, cx, cy, rr.inner(), rr.outer(), a0, a1, color);
            }

            // Draw icons centered along each slice
            for (int i = 0; i < n; i++) {
                double ang = (-Math.PI / 2.0) + (i + 0.5) * step;
                double r = iconRadius(rr.inner(), rr.outer());
                int ix = cx + (int)Math.round(Math.cos(ang) * r);
                int iy = cy + (int)Math.round(Math.sin(ang) * r);
                if (i < items.size()) {
                    IconRenderer.drawIcon(g, ix, iy, items.get(i).icon());
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] drawRing error: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /** Filled sector of a ring (anti-aliased via enough segments). ARGB color. */
    private static void fillRingSector(GuiGraphics g, int cx, int cy,
                                       double rInner, double rOuter,
                                       double a0, double a1, int argb) {
        try {
            int segs = Math.max(12, (int)Math.ceil((a1 - a0) * 48)); // smoothness

            float a = ((argb >>> 24) & 0xFF) / 255f;
            float r = ((argb >>> 16) & 0xFF) / 255f;
            float gn = ((argb >>> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;

            Matrix4f pose = g.pose().last().pose();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i <= segs; i++) {
                double t = (double)i / (double)segs;
                double ang = a0 + (a1 - a0) * t;
                float cos = (float)Math.cos(ang);
                float sin = (float)Math.sin(ang);

                float xOuter = (float)(cx + cos * rOuter);
                float yOuter = (float)(cy + sin * rOuter);
                float xInner = (float)(cx + cos * rInner);
                float yInner = (float)(cy + sin * rInner);

                buf.addVertex(pose, xOuter, yOuter, 0).setColor(r, gn, b, a);
                buf.addVertex(pose, xInner, yInner, 0).setColor(r, gn, b, a);
            }

            BufferUploader.drawWithShader(buf.buildOrThrow());
            RenderSystem.disableBlend();
        } catch (Throwable ignored) {
            // If any rendering mismatch slips through, we just skip this slice to stay crash-safe.
        }
    }
}
