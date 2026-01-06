// MainFile: forge/src/main/java/org/z2six/ezactions/config/RadialAnimConfigView.java
package org.z2six.ezactions.config;

import org.z2six.ezactions.Constants;

/**
 * Read-only snapshot of animation settings.
 *
 * IMPORTANT:
 * The old Forge file had a static singleton INSTANCE, which permanently cached values.
 * That is exactly the bug we fixed on NeoForge.
 *
 * This Forge version returns a fresh snapshot each call (same behavior as NeoForge fix).
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
            try {
                Constants.LOG.warn("[{}] RadialAnimConfigView: defaults in use ({}).", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
        }

        this.animationsEnabled = ae;
        this.animOpenClose     = aoc;
        this.animHover         = ah;
        this.hoverGrowPct      = hgp;
        this.openCloseMs       = ocm;
    }
}
