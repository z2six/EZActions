// MainFile: forge/src/main/java/org/z2six/ezactions/config/ConfigIO.java
package org.z2six.ezactions.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.loading.FMLPaths;
import org.z2six.ezactions.Constants;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Immediate persistence for our client config specs.
 *
 * Forge writes configs on its own cadence. Our in-game ConfigScreen expects:
 * - apply now
 * - persist now
 *
 * We do a best-effort NightConfig write to the correct file path immediately.
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

    // Must match your registration filenames. These match the NeoForge side behavior.
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
            try {
                Constants.LOG.warn("[{}] ConfigIO.saveNow({}) failed: {}", Constants.MOD_NAME, section, t.toString());
            } catch (Throwable ignored) {}
        }
    }

    private static void saveGeneralClient() {
        Path f = configDir().resolve(FILE_GENERAL);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {

                safeLoad(cfg);

                // Matches Forge GeneralClientConfig: b.push("general")
                cfg.set("general.moveWhileRadialOpen", GeneralClientConfig.INSTANCE.moveWhileRadialOpen.get());
                cfg.set("general.commandEditorVisibleLines", GeneralClientConfig.INSTANCE.commandEditorVisibleLines.get());

                cfg.save();
            }

            try {
                Constants.LOG.debug("[{}] Forced save OK: {}", Constants.MOD_NAME, f);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
            } catch (Throwable ignored) {}
        }
    }

    private static void saveAnimClient() {
        Path f = configDir().resolve(FILE_ANIM);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {

                safeLoad(cfg);

                // Matches RadialAnimConfig: b.push("animations")
                cfg.set("animations.animationsEnabled", RadialAnimConfig.CONFIG.animationsEnabled.get());
                cfg.set("animations.animOpenClose", RadialAnimConfig.CONFIG.animOpenClose.get());
                cfg.set("animations.animHover", RadialAnimConfig.CONFIG.animHover.get());
                cfg.set("animations.hoverGrowPct", RadialAnimConfig.CONFIG.hoverGrowPct.get());
                cfg.set("animations.openCloseMs", RadialAnimConfig.CONFIG.openCloseMs.get());

                cfg.save();
            }

            try {
                Constants.LOG.debug("[{}] Forced save OK: {}", Constants.MOD_NAME, f);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
            } catch (Throwable ignored) {}
        }
    }

    private static void saveDesignClient() {
        Path f = configDir().resolve(FILE_DESIGN);
        try {
            Files.createDirectories(f.getParent());

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {

                safeLoad(cfg);

                // DesignClientConfig uses top-level keys (no push)
                cfg.set("deadzone", DesignClientConfig.deadzone.get());
                cfg.set("baseOuterRadius", DesignClientConfig.baseOuterRadius.get());
                cfg.set("ringThickness", DesignClientConfig.ringThickness.get());
                cfg.set("scaleStartThreshold", DesignClientConfig.scaleStartThreshold.get());
                cfg.set("scalePerItem", DesignClientConfig.scalePerItem.get());
                cfg.set("ringColor", DesignClientConfig.ringColor.get());
                cfg.set("hoverColor", DesignClientConfig.hoverColor.get());

                cfg.save();
            }

            try {
                Constants.LOG.debug("[{}] Forced save OK: {}", Constants.MOD_NAME, f);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] Failed to force-save {}: {}", Constants.MOD_NAME, f, t.toString());
            } catch (Throwable ignored) {}
        }
    }

    private static Path configDir() {
        try {
            return FMLPaths.CONFIGDIR.get().resolve(DIR);
        } catch (Throwable t) {
            try {
                Constants.LOG.warn("[{}] FMLPaths.CONFIGDIR unavailable, using relative ./config/{}", Constants.MOD_NAME, DIR);
            } catch (Throwable ignored) {}
            return Path.of("config", DIR);
        }
    }

    private static void safeLoad(CommentedFileConfig cfg) {
        try {
            Path p = cfg.getFile().toPath();
            if (Files.exists(p)) cfg.load();
        } catch (Throwable t) {
            // Fail-soft: if load fails we still overwrite with current in-memory values
            try {
                Constants.LOG.debug("[{}] ConfigIO.safeLoad failed: {}", Constants.MOD_NAME, t.toString());
            } catch (Throwable ignored) {}
        }
    }
}
