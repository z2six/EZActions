// MainFile: src/main/java/org/z2six/ezactions/config/GeneralClientConfig.java
package org.z2six.ezactions.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.z2six.ezactions.Constants;

/**
 * General client-side TOML config (generated as: config/ezactions/general-client.toml).
 *
 * Only contains settings explicitly requested:
 *  - moveWhileRadialOpen: allow moving while the radial menu is held open (default: true)
 *  - commandEditorVisibleLines: visible line count for the multi-line command editor (default: 5)
 *
 * This class is a simple holder for a NeoForge ForgeConfigSpec; it does not crash.
 */
public final class GeneralClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final GeneralClientConfig CONFIG;

    public final ForgeConfigSpec.BooleanValue moveWhileRadialOpen;
    public final ForgeConfigSpec.BooleanValue showRadialHoverLabel;

    /** Visible line count for the command editor's MultiLineEditBox. */
    public final ForgeConfigSpec.IntValue commandEditorVisibleLines;

    static {
        Pair<GeneralClientConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(GeneralClientConfig::new);
        CONFIG = pair.getLeft();
        SPEC   = pair.getRight();
    }

    private GeneralClientConfig(ForgeConfigSpec.Builder b) {
        b.push("general");

        moveWhileRadialOpen = b
                .comment("Allow moving the player while the radial menu is open (hold hotkey).")
                .translation("ezactions.config.moveWhileRadialOpen")
                .define("moveWhileRadialOpen", true);

        showRadialHoverLabel = b
                .comment("Show the center text label for the currently hovered radial item.")
                .translation("ezactions.config.showRadialHoverLabel")
                .define("showRadialHoverLabel", true);

        commandEditorVisibleLines = b
                .comment(
                        "How many lines are visible in the multi-line Command editor box.",
                        "This only affects the widget height (scrollbar will appear for longer content).",
                        "Range: 1..20, Default: 5"
                )
                .translation("ezactions.config.commandEditorVisibleLines")
                .defineInRange("commandEditorVisibleLines", 5, 1, 20);

        b.pop();
    }

    /** Defensive getter; never throws. */
    public boolean moveWhileRadialOpen() {
        try {
            return moveWhileRadialOpen.get();
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] GeneralClientConfig read failed (moveWhileRadialOpen): {}", Constants.MOD_NAME, t.toString());
            return true;
        }
    }

    /** Defensive getter; never throws. */
    public boolean showRadialHoverLabel() {
        try {
            return showRadialHoverLabel.get();
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] GeneralClientConfig read failed (showRadialHoverLabel): {}", Constants.MOD_NAME, t.toString());
            return true;
        }
    }

    /**
     * Defensive getter; clamps to [1..20], default 5 on error.
     * This only controls the *visible* height of the command editor box.
     */
    public int commandEditorVisibleLines() {
        try {
            int v = commandEditorVisibleLines.get();
            if (v < 1) v = 1;
            if (v > 20) v = 20;
            return v;
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] GeneralClientConfig read failed (commandEditorVisibleLines): {}", Constants.MOD_NAME, t.toString());
            return 5;
        }
    }

    private GeneralClientConfig() { throw new AssertionError("unreachable"); }
}
