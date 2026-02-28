package org.z2six.ezactions.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.z2six.ezactions.Constants;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads custom 16x16 PNG icons from config/ezactions/icons. */
public final class CustomIconManager {
    private static final Map<String, ResourceLocation> BY_ID = new HashMap<>();
    private static boolean loaded = false;

    private CustomIconManager() {}

    public static synchronized void ensureLoaded() {
        if (!loaded) reload();
    }

    /** Ensure folder + README exist, even before textures are loaded. */
    public static void ensureFolderReady() {
        try {
            Path dir = iconsDir();
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme,
                        "Put custom icon PNG files here.\n" +
                        "Rules:\n" +
                        "- Must be PNG\n" +
                        "- Must be exactly 16x16 pixels\n" +
                        "- Filenames become icon ids (custom:<name>)\n");
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to prepare custom icon folder: {}", Constants.MOD_NAME, t.toString());
        }
    }

    public static synchronized void reload() {
        loaded = true;
        BY_ID.clear();
        try {
            Path dir = iconsDir();
            ensureFolderReady();

            try (var stream = Files.list(dir)) {
                stream.filter(Files::isRegularFile).forEach(CustomIconManager::tryLoadFile);
            }
            Constants.LOG.info("[{}] Loaded {} custom icon(s).", Constants.MOD_NAME, BY_ID.size());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CustomIconManager.reload failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    public static synchronized List<String> listIds() {
        ensureLoaded();
        List<String> out = new ArrayList<>(BY_ID.keySet());
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public static synchronized ResourceLocation textureForId(String id) {
        ensureLoaded();
        return id == null ? null : BY_ID.get(id);
    }

    public static Path iconsDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID).resolve("icons");
    }

    private static void tryLoadFile(Path p) {
        String name = p.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) return;

        String stem = name.substring(0, Math.max(0, name.length() - 4)).trim();
        if (stem.isEmpty()) return;
        String safeStem = sanitize(stem);
        String id = "custom:" + safeStem;

        NativeImage img = null;
        try (InputStream in = Files.newInputStream(p)) {
            img = NativeImage.read(in);
            if (img == null) return;
            if (img.getWidth() != 16 || img.getHeight() != 16) {
                Constants.LOG.warn("[{}] Skipping custom icon '{}': size is {}x{}, expected 16x16.",
                        Constants.MOD_NAME, name, img.getWidth(), img.getHeight());
                try { img.close(); } catch (Throwable ignored) {}
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getTextureManager() == null) return;

            ResourceLocation rl = ResourceLocation.tryParse(Constants.MOD_ID + ":custom_icons/" + safeStem);
            if (rl == null) return;
            mc.getTextureManager().register(rl, new DynamicTexture(img));
            img = null; // texture now owns image lifecycle
            BY_ID.put(id, rl);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed loading custom icon '{}': {}", Constants.MOD_NAME, name, t.toString());
        } finally {
            if (img != null) {
                try { img.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static String sanitize(String in) {
        String s = in.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_.-]", "_");
        return s.isEmpty() ? "icon" : s;
    }
}
