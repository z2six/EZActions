// MainFile: src/main/java/org/z2six/ezactions/gui/RadialScreenDraw.java
package org.z2six.ezactions.gui;

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
import org.z2six.ezactions.config.RadialAnimConfigView;
import org.z2six.ezactions.config.RadialConfig;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.anim.SliceHoverAnim;

import java.util.List;
import java.util.Locale;

/** Drawing helpers for radial ring with style + animation variants. */
public final class RadialScreenDraw {

    private RadialScreenDraw() {}

    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx,
                                RadialScreenMath.Radii rr,
                                SliceHoverAnim hover,
                                float openProgress) {
        try {
            if (items == null || items.isEmpty()) {
                g.fill(cx - 1, cy - 6, cx + 1, cy + 6, 0xFFFFFFFF);
                g.fill(cx - 6, cy - 1, cx + 6, cy + 1, 0xFFFFFFFF);
                return;
            }

            RadialConfig cfg = RadialConfig.get();
            RadialAnimConfigView anim = RadialAnimConfigView.get();
            RadialMenu.TemporaryStyle temp = RadialMenu.temporaryStyle();

            boolean animationsEnabled = pick(temp == null ? null : temp.animationsEnabled, anim.animationsEnabled);
            boolean animOpenClose = pick(temp == null ? null : temp.animOpenClose, anim.animOpenClose);
            boolean animHover = pick(temp == null ? null : temp.animHover, anim.animHover);
            double hoverGrowPct = clampNonNeg(pick(temp == null ? null : temp.hoverGrowPct, anim.hoverGrowPct));

            String openStyle = normalize(pick(temp == null ? null : temp.openStyle, anim.openStyle), "WIPE");
            String openDirection = normalize(pick(temp == null ? null : temp.openDirection, anim.openDirection), "CW");
            String hoverStyle = normalize(pick(temp == null ? null : temp.hoverStyle, anim.hoverStyle), "FILL_SCALE");
            String designStyle = normalize(pick(temp == null ? null : temp.designStyle, cfg.designStyle), "SOLID");

            int ringColor = pick(temp == null ? null : temp.ringColor, cfg.ringColor);
            int hoverColor = pick(temp == null ? null : temp.hoverColor, cfg.hoverColor);
            int borderColor = pick(temp == null ? null : temp.borderColor, cfg.borderColor);
            int textColor = pick(temp == null ? null : temp.textColor, cfg.textColor);
            int sliceGapDeg = clampInt(pick(temp == null ? null : temp.sliceGapDeg, cfg.sliceGapDeg), 0, 16);

            final int n = Math.max(1, items.size());
            final double step = (Math.PI * 2.0) / n;
            final double gapRad = Math.toRadians(sliceGapDeg);
            final double sweep = clamp01(openProgress) * (Math.PI * 2.0);
            final boolean useWipe = animationsEnabled && animOpenClose && "WIPE".equals(openStyle);
            final boolean useFade = animationsEnabled && animOpenClose && "FADE".equals(openStyle);
            final float fadeFactor = useFade ? clamp01(openProgress) : 1.0f;
            final boolean ccw = "CCW".equals(openDirection);

            final boolean hoverFillMode = switch (hoverStyle) {
                case "FILL_SCALE", "FILL_ONLY" -> true;
                default -> false;
            };
            final boolean hoverScaleMode = switch (hoverStyle) {
                case "FILL_SCALE", "SCALE_ONLY" -> true;
                default -> false;
            };

            for (int i = 0; i < n; i++) {
                double a0 = (-Math.PI / 2.0) + i * step;
                double a1 = a0 + step;

                if (useWipe) {
                    SectorClip clip = clipBySweep(i, step, sweep, ccw);
                    if (clip == null) continue;
                    a0 = clip.a0;
                    a1 = clip.a1;
                }

                double width = a1 - a0;
                if (width <= 0.0001) continue;
                double localGap = Math.min(gapRad, width * 0.8);
                if ("SEGMENTED".equals(designStyle) || "OUTLINE".equals(designStyle) || "GLASS".equals(designStyle)) {
                    a0 += localGap * 0.5;
                    a1 -= localGap * 0.5;
                }
                if (a1 <= a0) continue;

                int baseColor = applyGlobalFade(ringColor, fadeFactor);
                double rInner = rr.inner();
                double rOuter = rr.outer();

                if (animationsEnabled && animHover && hoverScaleMode && hover != null) {
                    float grow = clamp01(hover.scaleFor(i));
                    if (grow > 0f) {
                        rOuter = rOuter * (1.0 + hoverGrowPct * grow);
                    }
                }

                drawBaseSlice(g, designStyle, cx, cy, rInner, rOuter, a0, a1, baseColor, borderColor, fadeFactor);
            }

            // Instant fill highlight when hover anim is disabled but fill mode enabled.
            if ((!animationsEnabled || !animHover) && hoverFillMode && hoveredIdx >= 0 && hoveredIdx < n) {
                double a0 = (-Math.PI / 2.0) + hoveredIdx * step;
                double a1 = a0 + step;
                if (useWipe) {
                    SectorClip clip = clipBySweep(hoveredIdx, step, sweep, ccw);
                    if (clip != null) {
                        a0 = clip.a0;
                        a1 = clip.a1;
                    } else {
                        a1 = a0;
                    }
                }
                if (a1 > a0) {
                    int c = applyGlobalFade(hoverColor, fadeFactor);
                    fillRingSector(g, cx, cy, rr.inner(), rr.outer(), a0, a1, c);
                }
            }

            // Animated hover fill.
            if (animationsEnabled && animHover && hoverFillMode && hover != null && hoveredIdx >= 0 && hoveredIdx < n) {
                double a0 = (-Math.PI / 2.0) + hoveredIdx * step;
                double a1 = a0 + step;
                if (useWipe) {
                    SectorClip clip = clipBySweep(hoveredIdx, step, sweep, ccw);
                    if (clip == null) {
                        a1 = a0;
                    } else {
                        a0 = clip.a0;
                        a1 = clip.a1;
                    }
                }
                if (a1 > a0) {
                    float s = clamp01(hover.sweepFor(hoveredIdx));
                    if (s > 0f) {
                        double rInner = rr.inner();
                        double rOuter = rInner + (rr.outer() - rInner) * s;
                        int c = applyGlobalFade(hoverColor, fadeFactor);
                        fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, c);
                    }
                }
            }

            // Icons
            final double rMidBase = (rr.inner() + rr.outer()) * 0.5;
            for (int i = 0; i < n; i++) {
                double ang = (-Math.PI / 2.0) + (i + 0.5) * step;
                double rMid = rMidBase;
                if (animationsEnabled && animHover && hoverScaleMode && hover != null) {
                    float grow = clamp01(hover.scaleFor(i));
                    if (grow > 0f) {
                        rMid = rMid * (1.0 + (hoverGrowPct * 0.5) * grow);
                    }
                }
                int ix = cx + (int) Math.round(Math.cos(ang) * rMid);
                int iy = cy + (int) Math.round(Math.sin(ang) * rMid);
                if (i < items.size()) {
                    IconRenderer.drawIcon(g, ix, iy, items.get(i).icon());
                }
            }

        } catch (Throwable t) {
            org.z2six.ezactions.Constants.LOG.warn("[{}] drawRing error: {}",
                    org.z2six.ezactions.Constants.MOD_NAME, t.toString());
        }
    }

    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx, RadialScreenMath.Radii rr) {
        drawRing(g, font, cx, cy, items, hoveredIdx, rr, null, 1.0f);
    }

    public static void drawRing(GuiGraphics g, Font font, int cx, int cy,
                                List<MenuItem> items, int hoveredIdx) {
        RadialScreenMath.Radii rr = RadialScreenMath.computeRadii(items == null ? 0 : items.size());
        drawRing(g, font, cx, cy, items, hoveredIdx, rr, null, 1.0f);
    }

    private static void drawBaseSlice(GuiGraphics g, String style, int cx, int cy,
                                      double rInner, double rOuter, double a0, double a1,
                                      int ringColor, int borderColor, float fadeFactor) {
        switch (style) {
            case "OUTLINE" -> {
                fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, withAlphaScale(ringColor, 0.22f));
                fillRingSector(g, cx, cy, rOuter - 2.0, rOuter, a0, a1, applyGlobalFade(borderColor, fadeFactor));
                fillRingSector(g, cx, cy, rInner, rInner + 2.0, a0, a1, applyGlobalFade(borderColor, fadeFactor));
            }
            case "GLASS" -> {
                fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, withAlphaScale(ringColor, 0.55f));
                fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, withAlphaScale(borderColor, 0.10f));
                fillRingSector(g, cx, cy, rOuter - 1.5, rOuter, a0, a1, applyGlobalFade(borderColor, fadeFactor));
            }
            default -> fillRingSector(g, cx, cy, rInner, rOuter, a0, a1, ringColor);
        }
    }

    private static SectorClip clipBySweep(int i, double step, double sweep, boolean ccw) {
        double phi0 = i * step;
        double phi1 = phi0 + step;
        double c0;
        double c1;
        if (!ccw) {
            c0 = phi0;
            c1 = Math.min(phi1, sweep);
        } else {
            double threshold = (Math.PI * 2.0) - sweep;
            c0 = Math.max(phi0, threshold);
            c1 = phi1;
        }
        if (c1 <= c0) return null;
        double a0 = (-Math.PI / 2.0) + c0;
        double a1 = (-Math.PI / 2.0) + c1;
        return new SectorClip(a0, a1);
    }

    private record SectorClip(double a0, double a1) {}

    private static int applyGlobalFade(int argb, float fade) {
        return withAlphaScale(argb, fade);
    }

    private static int withAlphaScale(int argb, float scale) {
        int a = (argb >>> 24) & 0xFF;
        int na = clampInt(Math.round(a * clamp01(scale)), 0, 255);
        return (argb & 0x00FFFFFF) | (na << 24);
    }

    private static <T> T pick(T override, T fallback) {
        return override != null ? override : fallback;
    }

    private static String normalize(String in, String dflt) {
        if (in == null) return dflt;
        String up = in.trim().toUpperCase(Locale.ROOT);
        return switch (dflt) {
            case "WIPE" -> ("WIPE".equals(up) || "FADE".equals(up) || "NONE".equals(up)) ? up : dflt;
            case "CW" -> ("CW".equals(up) || "CCW".equals(up)) ? up : dflt;
            case "FILL_SCALE" -> ("FILL_SCALE".equals(up) || "FILL_ONLY".equals(up) || "SCALE_ONLY".equals(up) || "NONE".equals(up)) ? up : dflt;
            case "SOLID" -> ("SOLID".equals(up) || "SEGMENTED".equals(up) || "OUTLINE".equals(up) || "GLASS".equals(up)) ? up : dflt;
            default -> up.isEmpty() ? dflt : up;
        };
    }

    private static double clampNonNeg(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) return 0;
        return v;
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float clamp01(double v) {
        return clamp01((float) v);
    }

    /** Filled sector of a ring (anti-aliased via enough segments). ARGB color. */
    private static void fillRingSector(GuiGraphics g, int cx, int cy,
                                       double rInner, double rOuter,
                                       double a0, double a1, int argb) {
        try {
            if (a1 <= a0) return;
            int segs = Math.max(12, (int) Math.ceil((a1 - a0) * 48));

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
                double t = (double) i / (double) segs;
                double ang = a0 + (a1 - a0) * t;
                float cos = (float) Math.cos(ang);
                float sin = (float) Math.sin(ang);

                float xOuter = (float) (cx + cos * rOuter);
                float yOuter = (float) (cy + sin * rOuter);
                float xInner = (float) (cx + cos * rInner);
                float yInner = (float) (cy + sin * rInner);

                buf.addVertex(pose, xOuter, yOuter, 0).setColor(r, gn, b, a);
                buf.addVertex(pose, xInner, yInner, 0).setColor(r, gn, b, a);
            }

            BufferUploader.drawWithShader(buf.buildOrThrow());
            RenderSystem.disableBlend();
        } catch (Throwable ignored) {}
    }
}
