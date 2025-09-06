// MainFile: src/main/java/org/z2six/ezactions/config/RadialAnimConfigView.java
package org.z2six.ezactions.config;

import org.z2six.ezactions.Constants;

/**
 * Read-only snapshot of animation settings, backed by the NeoForge TOML spec.
 * Never throws; falls back to sane defaults if the spec isn't available.
 */
public final class RadialAnimConfigView {

    public final boolean animationsEnabled;
    public final boolean animOpenClose;
    public final boolean animHover;
    public final double  hoverGrowPct;
    public final int     openCloseMs;

    private static final RadialAnimConfigView INSTANCE = new RadialAnimConfigView();

    public static RadialAnimConfigView get() { return INSTANCE; }

    private RadialAnimConfigView() {
        boolean ae = true, aoc = true, ah = true;
        double  hgp = 0.05D;
        int     ocm = 250;

        try {
            // Pull directly from the ModConfigSpec values.
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
