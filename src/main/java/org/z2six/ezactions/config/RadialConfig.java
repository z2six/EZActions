package org.z2six.ezactions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.z2six.ezactions.Constants;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Separate config for radial look & feel (config/ezactions/radial.json). */
public final class RadialConfig {
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static RadialConfig INSTANCE;

    // Defaults
    public int deadzone = 18;                  // px
    public int baseOuterRadius = 72;           // px (smaller minimum size)
    public int ringThickness = 28;             // px
    public int scaleStartThreshold = 8;        // items before scaling
    public int scalePerItem = 6;               // px added per item above threshold
    public int ringColor = 0xAA000000;         // ARGB (semi-black)
    public int hoverColor = 0xFFF20044;        // ARGB (semi-red)

    private RadialConfig() {}

    public static RadialConfig get() {
        if (INSTANCE == null) {
            INSTANCE = loadOrCreate();
        }
        return INSTANCE;
    }

    private static RadialConfig loadOrCreate() {
        Path f = file();
        try {
            if (Files.exists(f)) {
                try (Reader r = Files.newBufferedReader(f)) {
                    JsonObject o = G.fromJson(r, JsonObject.class);
                    RadialConfig c = new RadialConfig();
                    if (o.has("deadzone")) c.deadzone = o.get("deadzone").getAsInt();
                    if (o.has("baseOuterRadius")) c.baseOuterRadius = o.get("baseOuterRadius").getAsInt();
                    if (o.has("ringThickness")) c.ringThickness = o.get("ringThickness").getAsInt();
                    if (o.has("scaleStartThreshold")) c.scaleStartThreshold = o.get("scaleStartThreshold").getAsInt();
                    if (o.has("scalePerItem")) c.scalePerItem = o.get("scalePerItem").getAsInt();
                    if (o.has("ringColor")) c.ringColor = (int)Long.parseLong(o.get("ringColor").getAsString().replace("0x",""),16);
                    if (o.has("hoverColor")) c.hoverColor = (int)Long.parseLong(o.get("hoverColor").getAsString().replace("0x",""),16);
                    return c;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to load radial.json: {}", Constants.MOD_NAME, t.toString());
        }
        // write defaults
        RadialConfig c = new RadialConfig();
        save(c);
        return c;
    }

    public static void save(RadialConfig c) {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("deadzone", c.deadzone);
            o.addProperty("baseOuterRadius", c.baseOuterRadius);
            o.addProperty("ringThickness", c.ringThickness);
            o.addProperty("scaleStartThreshold", c.scaleStartThreshold);
            o.addProperty("scalePerItem", c.scalePerItem);
            o.addProperty("ringColor", String.format("0x%08X", c.ringColor));
            o.addProperty("hoverColor", String.format("0x%08X", c.hoverColor));
            try (Writer w = Files.newBufferedWriter(f)) {
                G.toJson(o, w);
            }
        } catch (IOException e) {
            Constants.LOG.warn("[{}] Failed to save radial.json: {}", Constants.MOD_NAME, e.toString());
        }
    }

    private static Path file() {
        try {
            Path game = Minecraft.getInstance().gameDirectory.toPath();
            return game.resolve("config").resolve("ezactions").resolve("radial.json");
        } catch (Throwable t) {
            return Path.of("config", "ezactions", "radial.json");
        }
    }
}
