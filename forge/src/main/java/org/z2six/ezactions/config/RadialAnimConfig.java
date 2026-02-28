// MainFile: src/main/java/org/z2six/ezactions/config/RadialAnimConfig.java
package org.z2six.ezactions.config;

import net.minecraftforge.common.ForgeConfigSpec;
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

    public static final ForgeConfigSpec SPEC;
    public static final RadialAnimConfig CONFIG;

    // --- spec values (public finals per NeoForge docs) -----------------------
    public final ForgeConfigSpec.BooleanValue animationsEnabled;
    public final ForgeConfigSpec.BooleanValue animOpenClose;
    public final ForgeConfigSpec.BooleanValue animHover;
    public final ForgeConfigSpec.DoubleValue  hoverGrowPct;
    public final ForgeConfigSpec.IntValue     openCloseMs;
    public final ForgeConfigSpec.ConfigValue<String> openStyle;
    public final ForgeConfigSpec.ConfigValue<String> openDirection;
    public final ForgeConfigSpec.ConfigValue<String> hoverStyle;

    static {
        // Build CONFIG instance + SPEC in one go
        Pair<RadialAnimConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(RadialAnimConfig::new);
        CONFIG = pair.getLeft();
        SPEC   = pair.getRight();
    }

    private RadialAnimConfig(ForgeConfigSpec.Builder b) {
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

        openStyle = b
                .comment("Open/close style: WIPE, FADE, NONE")
                .translation("ezactions.config.openStyle")
                .define("openStyle", "WIPE", o -> {
                    if (!(o instanceof String s)) return false;
                    String up = s.trim().toUpperCase(java.util.Locale.ROOT);
                    return "WIPE".equals(up) || "FADE".equals(up) || "NONE".equals(up);
                });

        openDirection = b
                .comment("Wipe direction: CW or CCW")
                .translation("ezactions.config.openDirection")
                .define("openDirection", "CW", o -> {
                    if (!(o instanceof String s)) return false;
                    String up = s.trim().toUpperCase(java.util.Locale.ROOT);
                    return "CW".equals(up) || "CCW".equals(up);
                });

        hoverStyle = b
                .comment("Hover style: FILL_SCALE, FILL_ONLY, SCALE_ONLY, NONE")
                .translation("ezactions.config.hoverStyle")
                .define("hoverStyle", "FILL_SCALE", o -> {
                    if (!(o instanceof String s)) return false;
                    String up = s.trim().toUpperCase(java.util.Locale.ROOT);
                    return "FILL_SCALE".equals(up) || "FILL_ONLY".equals(up)
                            || "SCALE_ONLY".equals(up) || "NONE".equals(up);
                });

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
    public String openStyle() {
        try { return normalize(openStyle.get(), "WIPE", java.util.Set.of("WIPE", "FADE", "NONE")); }
        catch (Throwable t) { log(t); return "WIPE"; }
    }
    public String openDirection() {
        try { return normalize(openDirection.get(), "CW", java.util.Set.of("CW", "CCW")); }
        catch (Throwable t) { log(t); return "CW"; }
    }
    public String hoverStyle() {
        try { return normalize(hoverStyle.get(), "FILL_SCALE", java.util.Set.of("FILL_SCALE", "FILL_ONLY", "SCALE_ONLY", "NONE")); }
        catch (Throwable t) { log(t); return "FILL_SCALE"; }
    }

    private static void log(Throwable t) {
        Constants.LOG.debug("[{}] RadialAnimConfig read failed: {}", Constants.MOD_NAME, t.toString());
    }

    private static String normalize(String in, String dflt, java.util.Set<String> allowed) {
        if (in == null) return dflt;
        String up = in.trim().toUpperCase(java.util.Locale.ROOT);
        return allowed.contains(up) ? up : dflt;
    }

    private RadialAnimConfig() { throw new AssertionError("unreachable"); }
}
