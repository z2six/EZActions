// MainFile: src/main/java/org/z2six/ezactions/config/RadialConfig.java
package org.z2six.ezactions.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.z2six.ezactions.Constants;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Separate config for radial look &amp; feel. Now prefers ModConfigSpec (Configured-visible). */
public final class RadialConfig {
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static RadialConfig INSTANCE;

    // Defaults
    public int deadzone = 18;
    public int baseOuterRadius = 72;
    public int ringThickness = 28;
    public int scaleStartThreshold = 8;
    public int scalePerItem = 6;
    public int ringColor = 0xAA000000;  // ARGB
    public int hoverColor = 0xFFF20044; // ARGB

    private static final String NEW_FILE = "design-client.toml";
    private static final String LEGACY_JSON = "radial.json";

    private RadialConfig() {}

    public static RadialConfig get() {
        if (INSTANCE == null) {
            INSTANCE = loadPreferringSpec();
        }
        return INSTANCE;
    }

    // Prefer ModConfigSpec values
    private static RadialConfig loadPreferringSpec() {
        try {
            if (DesignClientConfig.SPEC != null) {
                RadialConfig c = new RadialConfig();
                c.deadzone            = DesignClientConfig.deadzone.get();
                c.baseOuterRadius     = DesignClientConfig.baseOuterRadius.get();
                c.ringThickness       = DesignClientConfig.ringThickness.get();
                c.scaleStartThreshold = DesignClientConfig.scaleStartThreshold.get();
                c.scalePerItem        = DesignClientConfig.scalePerItem.get();
                // CHANGED: read ints directly
                c.ringColor           = DesignClientConfig.ringColor.get();
                c.hoverColor          = DesignClientConfig.hoverColor.get();
                return c;
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] Spec not ready, falling back to file: {}", Constants.MOD_NAME, t.toString());
        }
        return loadOrCreateFromFile();
    }

    // File-based fallback (unchanged except colors now read as number OR string)
    private static RadialConfig loadOrCreateFromFile() {
        Path toml = tomlFile();
        Path legacy = legacyFile();

        if (Files.exists(toml)) {
            try (CommentedFileConfig cfg = CommentedFileConfig.of(toml, TomlFormat.instance())) {
                cfg.load();
                RadialConfig c = new RadialConfig();
                c.deadzone            = getInt(cfg, "deadzone",            c.deadzone);
                c.baseOuterRadius     = getInt(cfg, "baseOuterRadius",     c.baseOuterRadius);
                c.ringThickness       = getInt(cfg, "ringThickness",       c.ringThickness);
                c.scaleStartThreshold = getInt(cfg, "scaleStartThreshold", c.scaleStartThreshold);
                c.scalePerItem        = getInt(cfg, "scalePerItem",        c.scalePerItem);
                c.ringColor           = getColor(cfg, "ringColor",         c.ringColor);
                c.hoverColor          = getColor(cfg, "hoverColor",        c.hoverColor);
                return c;
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Failed to load {}: {} (writing defaults)", Constants.MOD_NAME, toml, t.toString());
                RadialConfig c = new RadialConfig();
                save(c);
                return c;
            }
        }

        if (Files.exists(legacy)) {
            try (Reader r = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
                JsonObject o = G.fromJson(r, JsonObject.class);
                RadialConfig c = new RadialConfig();
                if (o != null) {
                    if (o.has("deadzone"))            c.deadzone = safeInt(o.get("deadzone"),            c.deadzone);
                    if (o.has("baseOuterRadius"))     c.baseOuterRadius = safeInt(o.get("baseOuterRadius"), c.baseOuterRadius);
                    if (o.has("ringThickness"))       c.ringThickness = safeInt(o.get("ringThickness"),   c.ringThickness);
                    if (o.has("scaleStartThreshold")) c.scaleStartThreshold = safeInt(o.get("scaleStartThreshold"), c.scaleStartThreshold);
                    if (o.has("scalePerItem"))        c.scalePerItem = safeInt(o.get("scalePerItem"),     c.scalePerItem);
                    if (o.has("ringColor"))           c.ringColor  = parseColor(o.get("ringColor").getAsString(),  c.ringColor);
                    if (o.has("hoverColor"))          c.hoverColor = parseColor(o.get("hoverColor").getAsString(), c.hoverColor);
                }
                save(c);
                try { Files.move(legacy, legacy.resolveSibling("radial.json.bak"), StandardCopyOption.REPLACE_EXISTING); }
                catch (Throwable ignore) {}
                return c;
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Failed to migrate {}: {}", Constants.MOD_NAME, legacy, t.toString());
                RadialConfig c = new RadialConfig();
                save(c);
                return c;
            }
        }

        RadialConfig c = new RadialConfig();
        save(c);
        return c;
    }

    public static void save(RadialConfig c) {
        // Prefer writing through the SPEC
        try {
            if (DesignClientConfig.SPEC != null) {
                DesignClientConfig.deadzone.set(c.deadzone);
                DesignClientConfig.baseOuterRadius.set(c.baseOuterRadius);
                DesignClientConfig.ringThickness.set(c.ringThickness);
                DesignClientConfig.scaleStartThreshold.set(c.scaleStartThreshold);
                DesignClientConfig.scalePerItem.set(c.scalePerItem);
                DesignClientConfig.ringColor.set(c.ringColor);
                DesignClientConfig.hoverColor.set(c.hoverColor);
                return; // NeoForge persists
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] Could not write DesignClientConfig; falling back to file: {}", Constants.MOD_NAME, t.toString());
        }

        // Fallback to file writer
        Path f = tomlFile();
        try {
            Files.createDirectories(f.getParent());
            Config root = Config.of(TomlFormat.instance());
            root.set("deadzone",            c.deadzone);
            root.set("baseOuterRadius",     c.baseOuterRadius);
            root.set("ringThickness",       c.ringThickness);
            root.set("scaleStartThreshold", c.scaleStartThreshold);
            root.set("scalePerItem",        c.scalePerItem);
            root.set("ringColor",           c.ringColor);
            root.set("hoverColor",          c.hoverColor);

            try (CommentedFileConfig cfg = CommentedFileConfig.builder(f, TomlFormat.instance())
                    .sync().preserveInsertionOrder().build()) {
                cfg.load();
                cfg.putAll(root);
                cfg.save();
            }
        } catch (Throwable e) {
            Constants.LOG.warn("[{}] Failed to save {}: {}", Constants.MOD_NAME, f, e.toString());
        }
    }

    // helpers
    private static Path tomlFile() {
        try {
            Path game = Minecraft.getInstance().gameDirectory.toPath();
            return game.resolve("config").resolve(Constants.MOD_ID).resolve(NEW_FILE);
        } catch (Throwable t) {
            return Path.of("config", Constants.MOD_ID, NEW_FILE);
        }
    }
    private static Path legacyFile() {
        try {
            Path game = Minecraft.getInstance().gameDirectory.toPath();
            return game.resolve("config").resolve(Constants.MOD_ID).resolve(LEGACY_JSON);
        } catch (Throwable t) {
            return Path.of("config", Constants.MOD_ID, LEGACY_JSON);
        }
    }
    private static int getInt(CommentedFileConfig cfg, String key, int dflt) {
        try {
            Object v = cfg.get(key);
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) return Integer.parseInt(s.trim());
        } catch (Throwable ignored) {}
        return dflt;
    }
    private static int getColor(CommentedFileConfig cfg, String key, int dflt) {
        try {
            Object v = cfg.get(key);
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) return parseColor(s, dflt);
        } catch (Throwable ignored) {}
        return dflt;
    }
    private static int parseColor(String s, int fallback) {
        try {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) return fallback;
            if (t.startsWith("0x") || t.startsWith("0X")) t = t.substring(2);
            else if (t.startsWith("#")) t = t.substring(1);
            if (t.matches("(?i)^[0-9a-f]{6}$")) t = "FF" + t; // add opaque alpha
            long val = Long.parseUnsignedLong(t, 16);
            return (int) val;
        } catch (Throwable e) {
            Constants.LOG.warn("[{}] Bad color literal '{}'; using fallback 0x{}", Constants.MOD_NAME, s, Integer.toHexString(fallback));
            return fallback;
        }
    }
    private static int safeInt(com.google.gson.JsonElement el, int dflt) {
        try { return el.getAsInt(); } catch (Throwable ignored) { return dflt; }
    }
}
