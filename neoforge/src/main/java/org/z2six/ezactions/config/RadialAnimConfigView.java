// MainFile: neoforge/src/main/java/org/z2six/ezactions/config/RadialAnimConfigView.java
package org.z2six.ezactions.config;

import org.z2six.ezactions.Constants;

/**
 * Lightweight "view" of RadialAnimConfig for rendering.
 *
 * IMPORTANT:
 * - The old code cached a single INSTANCE with final fields, so config changes never applied.
 * - This version returns a fresh snapshot each call.
 */
public final class RadialAnimConfigView {

    public final boolean animationsEnabled;
    public final boolean animOpenClose;
    public final boolean animHover;
    public final double  hoverGrowPct;
    public final int     openCloseMs;

    public static RadialAnimConfigView get() {
        return new RadialAnimConfigView();
    }

    private RadialAnimConfigView() {
        boolean ae = true, aoc = true, ah = true;
        double  hgp = 0.05D;
        int     ocm = 250;

        try {
            RadialAnimConfig c = RadialAnimConfig.CONFIG;
            ae  = c.animationsEnabled();
            aoc = c.animOpenClose();
            ah  = c.animHover();
            hgp = c.hoverGrowPct();
            ocm = c.openCloseMs();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] RadialAnimConfigView: defaults in use ({}).", Constants.MOD_NAME, t.toString());
        }

        this.animationsEnabled = ae;
        this.animOpenClose     = aoc;
        this.animHover         = ah;
        this.hoverGrowPct      = hgp;
        this.openCloseMs       = ocm;
    }
}
