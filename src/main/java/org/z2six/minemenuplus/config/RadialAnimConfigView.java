package org.z2six.minemenuplus.config;

import org.z2six.minemenuplus.Constants;

/**
 * Safe, read-only view for animation-related settings.
 * Uses defaults if RadialConfig doesn't expose these fields.
 */
public final class RadialAnimConfigView {

    public final boolean animationsEnabled; // master switch
    public final boolean animOpenClose;     // radial wipe on open/close
    public final boolean animHover;         // hover grow/sweep
    public final double  hoverGrowPct;      // e.g., 0.05 (5% grow)
    public final int     openCloseMs;       // e.g., 250 ms

    private static final RadialAnimConfigView INSTANCE = new RadialAnimConfigView();

    public static RadialAnimConfigView get() { return INSTANCE; }

    private RadialAnimConfigView() {
        // Defaults (always present even if RadialConfig doesn't have fields)
        boolean defAnimationsEnabled = true;
        boolean defAnimOpenClose     = true;
        boolean defAnimHover         = true;
        double  defHoverGrowPct      = 0.05;
        int     defOpenCloseMs       = 250;

        boolean ae = defAnimationsEnabled;
        boolean aoc = defAnimOpenClose;
        boolean ah  = defAnimHover;
        double  hgp = defHoverGrowPct;
        int     ocm = defOpenCloseMs;

        try {
            // If RadialConfig exists and has a singleton, try to read fields
            RadialConfig cfg = RadialConfig.get(); // your existing config class
            if (cfg != null) {
                ae  = readBool(cfg, "animationsEnabled", defAnimationsEnabled);
                aoc = readBool(cfg, "animOpenClose",     defAnimOpenClose);
                ah  = readBool(cfg, "animHover",         defAnimHover);
                hgp = readDouble(cfg, "hoverGrowPct",    defHoverGrowPct);
                // Some people name this openCloseMs or animOpenCloseMs; try both
                ocm = readInt(cfg, "openCloseMs",
                        readInt(cfg, "animOpenCloseMs", defOpenCloseMs));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] RadialAnimConfigView: falling back to defaults: {}", Constants.MOD_NAME, t.toString());
        }

        this.animationsEnabled = ae;
        this.animOpenClose     = aoc;
        this.animHover         = ah;
        this.hoverGrowPct      = hgp;
        this.openCloseMs       = ocm;
    }

    private static boolean readBool(Object cfg, String field, boolean defVal) {
        try {
            var f = cfg.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(cfg);
            return (v instanceof Boolean b) ? b : defVal;
        } catch (Throwable ignored) { return defVal; }
    }

    private static int readInt(Object cfg, String field, int defVal) {
        try {
            var f = cfg.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(cfg);
            if (v instanceof Number n) return n.intValue();
            return defVal;
        } catch (Throwable ignored) { return defVal; }
    }

    private static double readDouble(Object cfg, String field, double defVal) {
        try {
            var f = cfg.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(cfg);
            if (v instanceof Number n) return n.doubleValue();
            return defVal;
        } catch (Throwable ignored) { return defVal; }
    }
}
