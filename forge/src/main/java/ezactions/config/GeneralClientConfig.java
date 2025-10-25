// MainFile: src/main/java/org/z2six/ezactions/config/GeneralClientConfig.java
package ezactions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.z2six.ezactions.Constants;

/**
 * // MainFile: GeneralClientConfig.java
 *
 * Client-only general settings for EZActions, exposed via ForgeConfigSpec.
 * This is a straight NeoForge -> Forge 1.20.1 port:
 * - ModConfigSpec.*  -> ForgeConfigSpec.*
 * - Builder usage retained; no behavior changes.
 *
 * The instance pattern (INSTANCE + SPEC) is kept so existing registration code
 * that expects .SPEC and .INSTANCE remains identical.
 *
 * Defensive approach:
 * - No hard crashes from static init; we guard the static block with try/catch.
 * - Values are declared final (spec entries) and validated by ForgeConfigSpec.
 */
public final class GeneralClientConfig {

    // --------------------------
    // Static access: INSTANCE + SPEC
    // --------------------------
    public static final GeneralClientConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    // --------------------------
    // Config values (instance-bound)
    // --------------------------
    /** Allow limited player movement while the radial menu is open (client-side UX). */
    public final ForgeConfigSpec.BooleanValue moveWhileRadialOpen;

    /** Visible line count for the command editor UI (client-side text widget sizing). */
    public final ForgeConfigSpec.IntValue commandEditorVisibleLines;

    // --------------------------
    // Construction via Builder (used by SPEC.configure(...))
    // --------------------------
    private GeneralClientConfig(final ForgeConfigSpec.Builder b) {
        // Group for readability in generated TOML
        b.push("general");

        this.moveWhileRadialOpen = b
                .comment("If true, allow basic movement inputs while the EZActions radial is open.")
                .define("moveWhileRadialOpen", false);

        this.commandEditorVisibleLines = b
                .comment("How many lines are shown in the command editor text box (visual only).")
                .defineInRange("commandEditorVisibleLines", 6, 1, 32);

        b.pop();
    }

    // --------------------------
    // Static initialization
    // --------------------------
    static {
        GeneralClientConfig instanceTmp;
        ForgeConfigSpec specTmp;

        try {
            Pair<GeneralClientConfig, ForgeConfigSpec> pair =
                    new ForgeConfigSpec.Builder().configure(GeneralClientConfig::new);
            instanceTmp = pair.getLeft();
            specTmp = pair.getRight();

            try {
                Constants.LOG.debug("[{}] GeneralClientConfig initialized successfully (ForgeConfigSpec).", Constants.MOD_NAME);
            } catch (Throwable ignored) {
                // Logging must never break class init.
            }
        } catch (Throwable t) {
            // Fail-soft: create a minimal spec so the game can still boot and log the problem.
            instanceTmp = new GeneralClientConfig(new ForgeConfigSpec.Builder());
            specTmp = new ForgeConfigSpec.Builder().build();

            try {
                Constants.LOG.warn("[{}] GeneralClientConfig static init failed: {}. Using fallback defaults.",
                        Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {
                // Avoid cascading failures from logging.
            }
        }

        INSTANCE = instanceTmp;
        SPEC = specTmp;
    }

    // --------------------------
    // Helpers (optional programmatic accessors)
    // --------------------------

    /**
     * Safe accessor with null/throw protection; never crashes callers.
     */
    public static boolean isMoveWhileRadialOpen() {
        try {
            return INSTANCE.moveWhileRadialOpen.get();
        } catch (Throwable t) {
            try {
                Constants.LOG.debug("[{}] Read moveWhileRadialOpen failed: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
            return false;
        }
    }

    /**
     * Safe accessor with clamp; never crashes callers.
     */
    public static int getCommandEditorVisibleLines() {
        try {
            int v = INSTANCE.commandEditorVisibleLines.get();
            if (v < 1) return 1;
            if (v > 32) return 32;
            return v;
        } catch (Throwable t) {
            try {
                Constants.LOG.debug("[{}] Read commandEditorVisibleLines failed: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
            return 6; // default
        }
    }
}
