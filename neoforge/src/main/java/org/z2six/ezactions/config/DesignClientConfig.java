// MainFile: src/main/java/org/z2six/ezactions/config/DesignClientConfig.java
package org.z2six.ezactions.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.z2six.ezactions.Constants;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configured-visible client config (ModConfigSpec) for radial design.
 * File: config/ezactions/design-client.toml
 *
 * Colors are IntValue with a full signed 32-bit range so Configured
 * won't crash while editing and spec construction can't fail on bad ranges.
 */
public final class DesignClientConfig {

    private DesignClientConfig() {}

    // ---- SPEC + values (all static) ----
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue deadzone;
    public static final ModConfigSpec.IntValue baseOuterRadius;
    public static final ModConfigSpec.IntValue ringThickness;
    public static final ModConfigSpec.IntValue scaleStartThreshold;
    public static final ModConfigSpec.IntValue scalePerItem;
    public static final ModConfigSpec.ConfigValue<String> designStyle;
    public static final ModConfigSpec.IntValue sliceGapDeg;

    // Colors as ARGB ints (signed 32-bit). Use full int range.
    public static final ModConfigSpec.IntValue ringColor;
    public static final ModConfigSpec.IntValue hoverColor;
    public static final ModConfigSpec.IntValue borderColor;
    public static final ModConfigSpec.IntValue textColor;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        deadzone            = b.comment("Deadzone radius in pixels where no slice is selected.")
                .defineInRange("deadzone", 18, 0, 90);
        baseOuterRadius     = b.comment("Outer radius of the ring in pixels (UI scale 1.0).")
                .defineInRange("baseOuterRadius", 72, 24, 512);
        ringThickness       = b.comment("Ring thickness in pixels.")
                .defineInRange("ringThickness", 28, 6, 256);
        scaleStartThreshold = b.comment("Minimum item count before scaling down begins.")
                .defineInRange("scaleStartThreshold", 8, 0, 128);
        scalePerItem        = b.comment("Outer-radius increment per extra item above threshold (pixels).")
                .defineInRange("scalePerItem", 6, 0, 100);
        designStyle         = b.comment("Design style: SOLID, SEGMENTED, OUTLINE, GLASS.")
                .define("designStyle", "SOLID", o -> {
                    if (!(o instanceof String s)) return false;
                    String up = s.trim().toUpperCase(java.util.Locale.ROOT);
                    return "SOLID".equals(up) || "SEGMENTED".equals(up)
                            || "OUTLINE".equals(up) || "GLASS".equals(up);
                });
        sliceGapDeg         = b.comment("Gap between slices in degrees (0..12).")
                .defineInRange("sliceGapDeg", 0, 0, 12);

        // IMPORTANT: use full signed range; 0xFFFFFFFF doesn't fit in int as a positive
        ringColor           = b.comment("ARGB color as int (0xAARRGGBB). Signed 32-bit; negatives are normal for opaque colors.")
                .defineInRange("ringColor", 0xAA000000, Integer.MIN_VALUE, Integer.MAX_VALUE);
        hoverColor          = b.comment("Hover ARGB color as int (0xAARRGGBB).")
                .defineInRange("hoverColor", 0xFFFC0553, Integer.MIN_VALUE, Integer.MAX_VALUE);
        borderColor         = b.comment("Border ARGB color as int (0xAARRGGBB).")
                .defineInRange("borderColor", 0x66FFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
        textColor           = b.comment("Center text ARGB color as int (0xAARRGGBB).")
                .defineInRange("textColor", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);

        SPEC = b.build();
    }

    // Registered from ezactions: modBus.addListener(DesignClientConfig::onConfigLoad)
    public static void onConfigLoad(final ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() != SPEC) return;

        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID);
            Path toml = dir.resolve("design-client.toml");
            Path legacyJson = dir.resolve("radial.json");

            boolean migrated = false;

            // Import from an existing flat TOML (we may have written this before registration)
            if (Files.exists(toml)) {
                try (var cfg = com.electronwill.nightconfig.core.file.CommentedFileConfig.of(
                        toml, com.electronwill.nightconfig.toml.TomlFormat.instance())) {
                    cfg.load();

                    boolean hasAny = cfg.contains("deadzone") || cfg.contains("baseOuterRadius")
                            || cfg.contains("ringThickness") || cfg.contains("scaleStartThreshold")
                            || cfg.contains("scalePerItem") || cfg.contains("ringColor")
                            || cfg.contains("hoverColor") || cfg.contains("designStyle")
                            || cfg.contains("sliceGapDeg") || cfg.contains("borderColor")
                            || cfg.contains("textColor");
                    if (hasAny) {
                        setIntSafely(deadzone,            cfg.getOrElse("deadzone",            deadzone.get()));
                        setIntSafely(baseOuterRadius,     cfg.getOrElse("baseOuterRadius",     baseOuterRadius.get()));
                        setIntSafely(ringThickness,       cfg.getOrElse("ringThickness",       ringThickness.get()));
                        setIntSafely(scaleStartThreshold, cfg.getOrElse("scaleStartThreshold", scaleStartThreshold.get()));
                        setIntSafely(scalePerItem,        cfg.getOrElse("scalePerItem",        scalePerItem.get()));
                        setIntSafely(sliceGapDeg,         cfg.getOrElse("sliceGapDeg",         sliceGapDeg.get()));
                        designStyle.set(normalizeStyle(String.valueOf(cfg.getOrElse("designStyle", designStyle.get()))));

                        // Colors: accept number or string; store as int
                        ringColor.set(parseColorAny(cfg.get("ringColor"), ringColor.get()));
                        hoverColor.set(parseColorAny(cfg.get("hoverColor"), hoverColor.get()));
                        borderColor.set(parseColorAny(cfg.get("borderColor"), borderColor.get()));
                        textColor.set(parseColorAny(cfg.get("textColor"), textColor.get()));

                        migrated = true;
                        Constants.LOG.info("[{}] Imported existing design-client.toml into ModConfigSpec (Configured-visible).", Constants.MOD_NAME);
                    }
                } catch (Throwable t) {
                    Constants.LOG.debug("[{}] Could not read existing design-client.toml for migration: {}", Constants.MOD_NAME, t.toString());
                }
            }

            // Fallback: migrate legacy JSON (string colors)
            if (!migrated && Files.exists(legacyJson)) {
                try (BufferedReader r = Files.newBufferedReader(legacyJson, StandardCharsets.UTF_8)) {
                    var obj = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
                    if (obj.has("deadzone"))            deadzone.set(obj.get("deadzone").getAsInt());
                    if (obj.has("baseOuterRadius"))     baseOuterRadius.set(obj.get("baseOuterRadius").getAsInt());
                    if (obj.has("ringThickness"))       ringThickness.set(obj.get("ringThickness").getAsInt());
                    if (obj.has("scaleStartThreshold")) scaleStartThreshold.set(obj.get("scaleStartThreshold").getAsInt());
                    if (obj.has("scalePerItem"))        scalePerItem.set(obj.get("scalePerItem").getAsInt());
                    if (obj.has("ringColor"))           ringColor.set(parseColor(obj.get("ringColor").getAsString(), ringColor.get()));
                    if (obj.has("hoverColor"))          hoverColor.set(parseColor(obj.get("hoverColor").getAsString(), hoverColor.get()));
                    if (obj.has("borderColor"))         borderColor.set(parseColor(obj.get("borderColor").getAsString(), borderColor.get()));
                    if (obj.has("textColor"))           textColor.set(parseColor(obj.get("textColor").getAsString(), textColor.get()));
                    if (obj.has("designStyle"))         designStyle.set(normalizeStyle(obj.get("designStyle").getAsString()));
                    if (obj.has("sliceGapDeg"))         sliceGapDeg.set(obj.get("sliceGapDeg").getAsInt());

                    try {
                        Files.move(legacyJson, legacyJson.resolveSibling("radial.json.bak"),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable ex) {
                        Constants.LOG.debug("[{}] Could not rename legacy radial.json: {}", Constants.MOD_NAME, ex.toString());
                    }
                    Constants.LOG.info("[{}] Migrated legacy radial.json → design-client.toml (ModConfigSpec).", Constants.MOD_NAME);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Could not migrate legacy radial.json: {}", Constants.MOD_NAME, t.toString());
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] DesignClientConfig load hook failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- helpers ---

    private static void setIntSafely(ModConfigSpec.IntValue v, Object raw) {
        try {
            if (raw instanceof Number n) v.set(n.intValue());
            else if (raw instanceof String s) v.set(Integer.parseInt(s.trim()));
        } catch (Throwable ignored) {}
    }

    /** Accept TOML value as number or string; return ARGB int. */
    private static int parseColorAny(Object raw, int dflt) {
        try {
            if (raw == null) return dflt;
            if (raw instanceof Number n) return n.intValue();
            if (raw instanceof String s) return parseColor(s, dflt);
        } catch (Throwable ignored) {}
        return dflt;
    }

    /** Accepts "0xAARRGGBB", "#RRGGBB" (opaque), or "AARRGGBB". */
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

    private static String normalizeStyle(String in) {
        if (in == null) return "SOLID";
        String up = in.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (up) {
            case "SOLID", "SEGMENTED", "OUTLINE", "GLASS" -> up;
            default -> "SOLID";
        };
    }
}
