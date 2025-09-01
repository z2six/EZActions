// MainFile: src/main/java/org/z2six/minemenuplus/gui/RadialScreenMath.java
package org.z2six.minemenuplus.gui;

import org.z2six.minemenuplus.Constants;

/**
 * // MainFile: RadialScreenMath.java
 * Pure math helpers for the radial menu screen (sector picking, radii, etc.).
 * Kept separate so RadialMenuScreen stays lean.
 */
public final class RadialScreenMath {

    private RadialScreenMath() {}

    /** Outer radius used to validate hover range (in GUI pixels). */
    public static final double RADIUS_OUTER = 140.0;

    /** Deadzone radius around the center that selects nothing. */
    public static final double DEADZONE = 18.0;

    /**
     * Return the sector index under the mouse or -1 if none.
     *
     * @param mouseX gui mouse x
     * @param mouseY gui mouse y
     * @param cx center x
     * @param cy center y
     * @param sectors number of items (sectors), must be >= 1
     */
    public static int pickSector(double mouseX, double mouseY, int cx, int cy, int sectors) {
        try {
            if (sectors <= 0) return -1;

            double dx = mouseX - cx;
            double dy = mouseY - cy;
            double dist2 = dx * dx + dy * dy;

            if (dist2 < DEADZONE * DEADZONE) return -1;
            if (dist2 > (RADIUS_OUTER * RADIUS_OUTER) * 1.2) return -1;

            // Angle with 0 at the top (negative Y), clockwise
            double ang = Math.atan2(dy, dx); // [-pi, +pi], 0 on +X
            ang += Math.PI / 2.0;            // rotate so 0 is at top
            if (ang < 0) ang += Math.PI * 2.0;

            double step = (Math.PI * 2.0) / sectors;
            int idx = (int) Math.floor(ang / step);
            if (idx < 0 || idx >= sectors) return -1;
            return idx;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] pickSector error: {}", Constants.MOD_NAME, t.toString());
            return -1;
        }
    }
}
