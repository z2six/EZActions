// MainFile: src/main/java/org/z2six/ezactions/config/RadialAnimConfig.java
package org.z2six.ezactions.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.z2six.ezactions.Constants;

/**
 * NeoForge TOML config spec for animation settings.
 *
 * Generates: config/ezactions/anim-client.toml   (when registered by the mod)
 *
 * Keep this class free of side effects: it only holds the Spec and the values.
 */
public final class RadialAnimConfig {

    public static final ModConfigSpec SPEC;
    public static final RadialAnimConfig CONFIG;

    // --- spec values (public finals per NeoForge docs) -----------------------
    public final ModConfigSpec.BooleanValue animationsEnabled;
    public final ModConfigSpec.BooleanValue animOpenClose;
    public final ModConfigSpec.BooleanValue animHover;
    public final ModConfigSpec.DoubleValue  hoverGrowPct;
    public final ModConfigSpec.IntValue     openCloseMs;

    static {
        // Build CONFIG instance + SPEC in one go
        Pair<RadialAnimConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(RadialAnimConfig::new);
        CONFIG = pair.getLeft();
        SPEC   = pair.getRight();
    }

    private RadialAnimConfig(ModConfigSpec.Builder b) {
        b.push("animations");

        animationsEnabled = b
                .comment("Master switch for all animations.")
                .translation("ezactions.config.animationsEnabled")
                .define("animationsEnabled", true);

        animOpenClose = b
                .comment("Animate the menu opening/closing with a radial wipe.")
                .translation("ezactions.config.animOpenClose")
                .define("animOpenClose", true);

        animHover = b
                .comment("Animate hover (slice grow + fill).")
                .translation("ezactions.config.animHover")
                .define("animHover", true);

        hoverGrowPct = b
                .comment("How much the hovered slice grows at full hover (0.00..0.50).")
                .translation("ezactions.config.hoverGrowPct")
                .defineInRange("hoverGrowPct", 0.05D, 0.0D, 0.5D);

        openCloseMs = b
                .comment("Menu open/close animation duration in milliseconds (0..2000).")
                .translation("ezactions.config.openCloseMs")
                .defineInRange("openCloseMs", 125, 0, 2000);

        b.pop();
    }

    /** Convenience getters with clamping (defensive, never throws). */
    public boolean animationsEnabled() {
        try { return animationsEnabled.get(); } catch (Throwable t) { log(t); return true; }
    }
    public boolean animOpenClose() {
        try { return animOpenClose.get(); } catch (Throwable t) { log(t); return true; }
    }
    public boolean animHover() {
        try { return animHover.get(); } catch (Throwable t) { log(t); return true; }
    }
    public double hoverGrowPct() {
        try {
            double v = hoverGrowPct.get();
            if (Double.isNaN(v) || Double.isInfinite(v)) return 0.05D;
            return Math.max(0.0D, Math.min(0.5D, v));
        } catch (Throwable t) { log(t); return 0.05D; }
    }
    public int openCloseMs() {
        try {
            int v = openCloseMs.get();
            if (v < 0) return 0;
            if (v > 2000) return 2000;
            return v;
        } catch (Throwable t) { log(t); return 125; }
    }

    private static void log(Throwable t) {
        Constants.LOG.debug("[{}] RadialAnimConfig read failed: {}", Constants.MOD_NAME, t.toString());
    }

    private RadialAnimConfig() { throw new AssertionError("unreachable"); }
}
