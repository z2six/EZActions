// MainFile: src/main/java/org/z2six/ezactions/gui/RadialScreenDraw.java
package org.z2six.ezactions.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.config.RadialAnimConfigView;
import org.z2six.ezactions.config.RadialConfig;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.gui.anim.SliceHoverAnim;

import java.util.List;

/** Drawing helpers for radial ring with filled slices + icons (with animation hooks). */
public final class RadialScreenDraw {

    private RadialScreenDraw() {}

    // Preferred path with radii + hover anim + openProgress
    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx,
                                RadialScreenMath.Radii rr,
                                SliceHoverAnim hover,
                                float openProgress) {
        try {
            if (items == null || items.isEmpty()) {
                // Minimal crosshair only (no hint text)
                g.fill(cx - 1, cy - 6, cx + 1, cy + 6, 0xFFFFFFFF);
                g.fill(cx - 6, cy - 1, cx + 6, cy + 1, 0xFFFFFFFF);
                return;
            }

            org.z2six.ezactions.config.RadialConfig cfg = org.z2six.ezactions.config.RadialConfig.get();
            org.z2six.ezactions.config.RadialAnimConfigView anim = org.z2six.ezactions.config.RadialAnimConfigView.get();

            final int n = Math.max(1, items.size());
            final double step = Math.PI * 2.0 / n;

            // Open/close wipe: sweep limit in absolute angle; start at -PI/2 (12 o'clock).
            final double sweepLimit = (-Math.PI / 2.0) + clamp01(openProgress) * (Math.PI * 2.0);

            // Draw all base slices (ringColor), clipped by open/close sweep if enabled
            for (int i = 0; i < n; i++) {
                double a0 = (-Math.PI / 2.0) + i * step;
                double a1 = a0 + step;

                if (anim.animationsEnabled && anim.animOpenClose) {
                    if (sweepLimit <= a0) continue;       // not yet revealed
                    if (sweepLimit < a1) a1 = sweepLimit; // partially revealed
                }

                int color = cfg.ringColor;
                double rInner = rr.inner();
                double rOuter = rr.outer();

                // Hover grow (pop-out) – outer-only expansion
                if (anim.animationsEnabled && anim.animHover && hover != null) {
                    float grow = clamp01(hover.scaleFor(i));  // 0..1
                    if (grow > 0f) {
                        final double growPct = 0.05; // keep consistent with view defaults
                        rOuter = rOuter * (1.0 + growPct * grow);
                    }
                }

                fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, color);
            }

            // If animations are OFF, still highlight hovered slice (instant)
            if (!(anim.animationsEnabled && anim.animHover) && hoveredIdx >= 0 && hoveredIdx < n) {
                double a0 = (-Math.PI / 2.0) + hoveredIdx * step;
                double a1 = a0 + step;

                if (anim.animOpenClose) {
                    if (sweepLimit > a0) {
                        if (sweepLimit < a1) a1 = sweepLimit;
                        fillRingSector(g, cx, cy, rr.inner(), rr.outer(), a0, a1, cfg.hoverColor);
                    }
                } else {
                    fillRingSector(g, cx, cy, rr.inner(), rr.outer(), a0, a1, cfg.hoverColor);
                }
            }

            // Hover colorization overlay — inside→out radial fill on the hovered slice
            if (anim.animationsEnabled && anim.animHover && hover != null && hoveredIdx >= 0 && hoveredIdx < n) {
                int i = hoveredIdx;
                double a0 = (-Math.PI / 2.0) + i * step;
                double a1 = a0 + step;

                if (anim.animOpenClose) {
                    if (sweepLimit > a0) {
                        if (sweepLimit < a1) a1 = sweepLimit;
                        float sweep = clamp01(hover.sweepFor(i)); // 0..1 radial factor
                        if (sweep > 0f) {
                            double rInner = rr.inner();
                            double rOuterFill = rInner + (rr.outer() - rInner) * sweep;
                            fillRingSector(g, cx, cy, rInner, rOuterFill, a0, a1, cfg.hoverColor);
                        }
                    }
                } else {
                    float sweep = clamp01(hover.sweepFor(hoveredIdx)); // 0..1 radial factor
                    if (sweep > 0f) {
                        double rInner = rr.inner();
                        double rOuterFill = rInner + (rr.outer() - rInner) * sweep;
                        fillRingSector(g, cx, cy, rInner, rOuterFill, a0, a1, cfg.hoverColor);
                    }
                }
            }

            // Draw icons centered along each slice (nudge outward on grow for hovered look)
            final double rMidBase = (rr.inner() + rr.outer()) * 0.5;
            for (int i = 0; i < n; i++) {
                double ang = (-Math.PI / 2.0) + (i + 0.5) * step;

                double rMid = rMidBase;
                if (anim.animationsEnabled && anim.animHover && hover != null) {
                    float grow = clamp01(hover.scaleFor(i));
                    if (grow > 0f) {
                        final double growPct = 0.05; // in sync with slice grow
                        rMid = rMid * (1.0 + (growPct * 0.5) * grow);
                    }
                }

                int ix = cx + (int)Math.round(Math.cos(ang) * rMid);
                int iy = cy + (int)Math.round(Math.sin(ang) * rMid);
                if (i < items.size()) {
                    org.z2six.ezactions.gui.IconRenderer.drawIcon(g, ix, iy, items.get(i).icon());
                }
            }

            // Center label for the currently hovered item (uses Component for styling/translation)
            if (hoveredIdx >= 0 && hoveredIdx < items.size()) {
                try {
                    net.minecraft.network.chat.Component comp = items.get(hoveredIdx).titleComponent();
                    if (comp != null && !comp.getString().isEmpty()) {
                        int tw = font.width(comp.getVisualOrderText());
                        g.drawString(font, comp, cx - (tw / 2), cy - (font.lineHeight / 2), 0xFFFFFFFF, false);
                    }
                } catch (Throwable t) {
                    org.z2six.ezactions.Constants.LOG.debug("[{}] Center label draw failed: {}", org.z2six.ezactions.Constants.MOD_NAME, t.toString());
                }
            }
        } catch (Throwable t) {
            org.z2six.ezactions.Constants.LOG.warn("[{}] drawRing error: {}", org.z2six.ezactions.Constants.MOD_NAME, t.toString());
        }
    }

    // Compatibility: radii only (no anims)
    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx, RadialScreenMath.Radii rr) {
        drawRing(g, font, cx, cy, items, hoveredIdx, rr, null, 1.0f);
    }

    // Compatibility: minimal signature
    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx) {
        RadialScreenMath.Radii rr = RadialScreenMath.computeRadii(items == null ? 0 : items.size());
        drawRing(g, font, cx, cy, items, hoveredIdx, rr, null, 1.0f);
    }

    // --- Internal: filled ring sector ---------------------------------------

    /** Filled sector of a ring (anti-aliased via enough segments). ARGB color. */
    private static void fillRingSector(GuiGraphics g, int cx, int cy,
                                       double rInner, double rOuter,
                                       double a0, double a1, int argb) {
        try {
            if (a1 <= a0) return;

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
            // If any rendering mismatch slips through, skip slice to remain crash-safe.
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
