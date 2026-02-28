// MainFile: neoforge/src/main/java/org/z2six/ezactions/config/ConfigIO.java
package org.z2six.ezactions.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.fml.loading.FMLPaths;
import org.z2six.ezactions.Constants;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Explicit, immediate persistence for our client config specs.
 *
 * NeoForge writes configs on its own schedule, but our custom in-game screen expects:
 * - values apply immediately
 * - values persist immediately
 *
 * We therefore force-save the relevant files ourselves using NightConfig.
 */
public final class ConfigIO {

    private ConfigIO() {}

    public enum Section {
        GENERAL,
        ANIM,
        DESIGN,
        ALL
    }

    private static final String DIR = Constants.MOD_ID;

    private static final String FILE_GENERAL = "general-client.toml";
    private static final String FILE_ANIM    = "anim-client.toml";
    private static final String FILE_DESIGN  = "design-client.toml";

    public static void saveNow(Section section) {
        try {
            switch (section) {
                case GENERAL -> saveGeneralClient();
                case ANIM -> saveAnimClient();
                case DESIGN -> saveDesignClient();
                case ALL -> {
                    saveGeneralClient();
                    saveAnimClient();
                    saveDesignClient();
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ConfigIO.saveNow({}) failed: {}", Constants.MOD_NAME, section, t.toString());
        }
    }

    private static void saveGeneralClient() {
        Path f = configDir().resolve(FILE_GENERAL);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {
                safeLoad(cfg);

                // Matches your spec structure: push("general")
                cfg.set("general.moveWhileRadialOpen", GeneralClientConfig.CONFIG.moveWhileRadialOpen.get());
                cfg.set("general.showRadialHoverLabel", GeneralClientConfig.CONFIG.showRadialHoverLabel.get());
                cfg.set("general.commandEditorVisibleLines", GeneralClientConfig.CONFIG.commandEditorVisibleLines.get());

                cfg.save();
            }

        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
        }
    }

    private static void saveAnimClient() {
        Path f = configDir().resolve(FILE_ANIM);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {
                safeLoad(cfg);

                // Matches your spec structure: push("animations")
                cfg.set("animations.animationsEnabled", RadialAnimConfig.CONFIG.animationsEnabled.get());
                cfg.set("animations.animOpenClose", RadialAnimConfig.CONFIG.animOpenClose.get());
                cfg.set("animations.animHover", RadialAnimConfig.CONFIG.animHover.get());
                cfg.set("animations.hoverGrowPct", RadialAnimConfig.CONFIG.hoverGrowPct.get());
                cfg.set("animations.openCloseMs", RadialAnimConfig.CONFIG.openCloseMs.get());
                cfg.set("animations.openStyle", RadialAnimConfig.CONFIG.openStyle.get());
                cfg.set("animations.openDirection", RadialAnimConfig.CONFIG.openDirection.get());
                cfg.set("animations.hoverStyle", RadialAnimConfig.CONFIG.hoverStyle.get());

                cfg.save();
            }

        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
        }
    }

    private static void saveDesignClient() {
        Path f = configDir().resolve(FILE_DESIGN);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {
                safeLoad(cfg);

                // DesignClientConfig does NOT push a section; keys are top-level.
                cfg.set("deadzone", DesignClientConfig.deadzone.get());
                cfg.set("baseOuterRadius", DesignClientConfig.baseOuterRadius.get());
                cfg.set("ringThickness", DesignClientConfig.ringThickness.get());
                cfg.set("scaleStartThreshold", DesignClientConfig.scaleStartThreshold.get());
                cfg.set("scalePerItem", DesignClientConfig.scalePerItem.get());
                cfg.set("ringColor", DesignClientConfig.ringColor.get());
                cfg.set("hoverColor", DesignClientConfig.hoverColor.get());
                cfg.set("borderColor", DesignClientConfig.borderColor.get());
                cfg.set("textColor", DesignClientConfig.textColor.get());
                cfg.set("sliceGapDeg", DesignClientConfig.sliceGapDeg.get());
                cfg.set("designStyle", DesignClientConfig.designStyle.get());

                cfg.save();
            }

        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
        }
    }

    private static Path configDir() {
        try {
            return FMLPaths.CONFIGDIR.get().resolve(DIR);
        } catch (Throwable t) {
            // best-effort fallback
            return Path.of("config", DIR);
        }
    }

    private static void safeLoad(CommentedFileConfig cfg) {
        try {
            if (Files.exists(cfg.getFile().toPath())) {
                cfg.load();
            }
        } catch (Throwable ignored) {
            // If load fails, we still want to overwrite with current in-memory values.
        }
    }
}
