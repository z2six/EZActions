// MainFile: neoforge/src/main/java/org/z2six/ezactions/config/RadialConfig.java
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

/**
 * Runtime view for radial design values.
 *
 * IMPORTANT:
 * - This class used to cache INSTANCE and never refresh, causing changes not to apply.
 * - We keep caching but add invalidate() and ensure save paths align with the spec.
 */
public final class RadialConfig {
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static volatile RadialConfig INSTANCE;

    public int deadzone = 18;
    public int baseOuterRadius = 72;
    public int ringThickness = 28;
    public int scaleStartThreshold = 8;
    public int scalePerItem = 6;
    public int ringColor = 0xAA000000;  // ARGB
    public int hoverColor = 0xFFF20044; // ARGB
    public int borderColor = 0x66FFFFFF;
    public int textColor = 0xFFFFFFFF;
    public int sliceGapDeg = 0;
    public String designStyle = "SOLID";

    private static final String NEW_FILE = "design-client.toml";
    private static final String LEGACY_JSON = "radial.json";

    private RadialConfig() {}

    public static RadialConfig get() {
        RadialConfig inst = INSTANCE;
        if (inst == null) {
            inst = loadPreferringSpec();
            INSTANCE = inst;
        }
        return inst;
    }

    /**
     * Call this after your config screen saves, so the next render tick uses fresh values.
     */
    public static void invalidate() {
        INSTANCE = null;
    }

    private static RadialConfig loadPreferringSpec() {
        try {
            // If spec exists, read directly from it (current live values).
            if (DesignClientConfig.SPEC != null) {
                RadialConfig c = new RadialConfig();
                c.deadzone            = DesignClientConfig.deadzone.get();
                c.baseOuterRadius     = DesignClientConfig.baseOuterRadius.get();
                c.ringThickness       = DesignClientConfig.ringThickness.get();
                c.scaleStartThreshold = DesignClientConfig.scaleStartThreshold.get();
                c.scalePerItem        = DesignClientConfig.scalePerItem.get();
                c.ringColor           = DesignClientConfig.ringColor.get();
                c.hoverColor          = DesignClientConfig.hoverColor.get();
                c.borderColor         = DesignClientConfig.borderColor.get();
                c.textColor           = DesignClientConfig.textColor.get();
                c.sliceGapDeg         = DesignClientConfig.sliceGapDeg.get();
                c.designStyle         = normalizeDesignStyle(DesignClientConfig.designStyle.get());
                return c;
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] RadialConfig spec read failed; falling back to file: {}", Constants.MOD_NAME, t.toString());
        }
        return loadOrCreateFromFile();
    }

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
                c.borderColor         = getColor(cfg, "borderColor",       c.borderColor);
                c.textColor           = getColor(cfg, "textColor",         c.textColor);
                c.sliceGapDeg         = getInt(cfg, "sliceGapDeg",         c.sliceGapDeg);
                c.designStyle         = normalizeDesignStyle(getString(cfg, "designStyle", c.designStyle));
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
                    if (o.has("borderColor"))         c.borderColor = parseColor(o.get("borderColor").getAsString(), c.borderColor);
                    if (o.has("textColor"))           c.textColor = parseColor(o.get("textColor").getAsString(), c.textColor);
                    if (o.has("sliceGapDeg"))         c.sliceGapDeg = safeInt(o.get("sliceGapDeg"), c.sliceGapDeg);
                    if (o.has("designStyle"))         c.designStyle = normalizeDesignStyle(o.get("designStyle").getAsString());
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
        try {
            // Prefer writing into the spec (NeoForge can persist it later),
            // but we still might be used in dev contexts; best-effort only.
            if (DesignClientConfig.SPEC != null) {
                DesignClientConfig.deadzone.set(c.deadzone);
                DesignClientConfig.baseOuterRadius.set(c.baseOuterRadius);
                DesignClientConfig.ringThickness.set(c.ringThickness);
                DesignClientConfig.scaleStartThreshold.set(c.scaleStartThreshold);
                DesignClientConfig.scalePerItem.set(c.scalePerItem);
                DesignClientConfig.ringColor.set(c.ringColor);
                DesignClientConfig.hoverColor.set(c.hoverColor);
                DesignClientConfig.borderColor.set(c.borderColor);
                DesignClientConfig.textColor.set(c.textColor);
                DesignClientConfig.sliceGapDeg.set(c.sliceGapDeg);
                DesignClientConfig.designStyle.set(normalizeDesignStyle(c.designStyle));
                return;
            }
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] Could not write DesignClientConfig; falling back to file: {}", Constants.MOD_NAME, t.toString());
        }

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
            root.set("borderColor",         c.borderColor);
            root.set("textColor",           c.textColor);
            root.set("sliceGapDeg",         c.sliceGapDeg);
            root.set("designStyle",         normalizeDesignStyle(c.designStyle));

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

    private static String getString(CommentedFileConfig cfg, String key, String dflt) {
        try {
            Object v = cfg.get(key);
            if (v instanceof String s && !s.isBlank()) return s;
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

    private static String normalizeDesignStyle(String in) {
        if (in == null) return "SOLID";
        String up = in.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (up) {
            case "SOLID", "SEGMENTED", "OUTLINE", "GLASS" -> up;
            default -> "SOLID";
        };
    }
}
