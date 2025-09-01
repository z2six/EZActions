package org.z2six.minemenuplus.gui;

import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.config.RadialConfig;

/** Math helpers for the radial menu. */
public final class RadialScreenMath {

    private RadialScreenMath() {}

    /** Dynamic radii based on item count and config. */
    public static Radii computeRadii(int items) {
        try {
            RadialConfig c = RadialConfig.get();
            int n = Math.max(0, items);
            int extra = Math.max(0, n - Math.max(0, c.scaleStartThreshold));
            double rOuter = c.baseOuterRadius + extra * Math.max(0, c.scalePerItem);
            double rInner = Math.max(8, rOuter - Math.max(8, c.ringThickness));
            return new Radii(rInner, rOuter, c.deadzone);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] computeRadii error: {}", Constants.MOD_NAME, t.toString());
            // Safe fallback
            return new Radii(42.0, 72.0, 18);
        }
    }

    public record Radii(double inner, double outer, int deadzone) {}

    /**
     * Pick sector index or -1 if the cursor is inside deadzone or way outside.
     * Angles start at the top (12 o'clock) and increase clockwise.
     */
    public static int pickSector(double mouseX, double mouseY, int cx, int cy, int sectors, Radii rr) {
        try {
            if (sectors <= 0) return -1;

            double dx = mouseX - cx;
            double dy = mouseY - cy;
            double dist2 = dx * dx + dy * dy;

            if (dist2 < (double) rr.deadzone() * rr.deadzone()) return -1;
            double maxR = rr.outer() * 1.35; // tolerant outer bound
            if (dist2 > maxR * maxR) return -1;

            double ang = Math.atan2(dy, dx); // [-pi, +pi], 0 on +X
            ang += Math.PI / 2.0;            // 0 at top
            if (ang < 0) ang += Math.PI * 2.0;

            double step = (Math.PI * 2.0) / sectors;
            int idx = (int) Math.floor(ang / step);
            return (idx < 0 || idx >= sectors) ? -1 : idx;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] pickSector error: {}", Constants.MOD_NAME, t.toString());
            return -1;
        }
    }
}
