package org.z2six.ezactions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Platform-neutral vanilla item icon index cache.
 *
 * Lives in {@code common/} so every loader (NeoForge, Fabric, Forge) can
 * pre-warm the icon index during startup and persist it to disk across restarts.
 *
 * All methods are thread-safe (synchronized on an internal lock).
 * Only depends on vanilla Minecraft classes + Gson (bundled with Minecraft).
 */
public final class VanillaIconCache {

    /** A single cached item entry with its display metadata. */
    public record Entry(String id, String displayName, String searchText) {}

    private static final Object LOCK = new Object();
    private static List<Entry> ENTRIES;
    private static Iterator<Map.Entry<ResourceKey<Item>, Item>> ITER;
    private static boolean COMPLETE = false;
    private static int TARGET_COUNT = 0;

    // Disk cache
    private static final Path CACHE_PATH = Path.of("config", Constants.MOD_ID, "icon_index.json").toAbsolutePath();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CACHE_VERSION = 1;

    private VanillaIconCache() {}

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Call on each client tick to incrementally build the index. */
    public static void tickWarmup() {
        synchronized (LOCK) {
            if (COMPLETE) return;
            ensureStarted();
            pump(320, 1_500_000L); // max items, budget ns
        }
    }

    /** Total entries currently cached (may be less than target if still building). */
    public static int entryCount() {
        synchronized (LOCK) {
            return ENTRIES == null ? 0 : ENTRIES.size();
        }
    }

    /** Full target count (size of the item registry). 0 until build starts. */
    public static int targetCount() {
        synchronized (LOCK) {
            return TARGET_COUNT;
        }
    }

    /** True once the entire registry has been indexed. */
    public static boolean isComplete() {
        synchronized (LOCK) {
            return COMPLETE;
        }
    }

    /**
     * Return entries from {@code fromIndex} (inclusive) to the end of the list.
     * Thread-safe snapshot; safe to call while the build is still running.
     */
    public static List<Entry> slice(int fromIndex) {
        synchronized (LOCK) {
            if (ENTRIES == null || fromIndex >= ENTRIES.size()) return List.of();
            return List.copyOf(ENTRIES.subList(fromIndex, ENTRIES.size()));
        }
    }

    /** Return an immutable snapshot of all current entries. */
    public static List<Entry> all() {
        synchronized (LOCK) {
            return ENTRIES == null ? List.of() : List.copyOf(ENTRIES);
        }
    }

    /** Delete the disk cache and reset in-memory state so the index is rebuilt. */
    public static void invalidateDiskCache() {
        synchronized (LOCK) {
            try { Files.deleteIfExists(CACHE_PATH); } catch (Throwable ignored) {}
            reset();
        }
    }

    // ── Internal build logic ────────────────────────────────────────────────────

    private static void ensureStarted() {
        if (COMPLETE || ITER != null) return;
        // Try disk cache first
        if (tryLoadDiskCache()) return;
        TARGET_COUNT = Math.max(0, BuiltInRegistries.ITEM.entrySet().size());
        ENTRIES = new ArrayList<>(Math.max(256, TARGET_COUNT));
        ITER = BuiltInRegistries.ITEM.entrySet().iterator();
    }

    private static void pump(int maxItems, long budgetNs) {
        if (COMPLETE || ITER == null) return;
        long deadline = System.nanoTime() + budgetNs;
        int added = 0;
        while (added < maxItems && System.nanoTime() < deadline && ITER.hasNext()) {
            Map.Entry<ResourceKey<Item>, Item> e = ITER.next();
            ResourceLocation rl = e.getKey().location();
            String id = rl.getNamespace() + ":" + rl.getPath();
            String baseSearch = (id + " item").toLowerCase(Locale.ROOT);
            ENTRIES.add(new Entry(id, id, baseSearch));
            added++;
        }
        if (!ITER.hasNext()) {
            ITER = null;
            COMPLETE = true;
            saveDiskCache(); // persist once fully built
        }
    }

    private static void reset() {
        ENTRIES = null;
        ITER = null;
        COMPLETE = false;
        TARGET_COUNT = 0;
    }

    // ── Disk cache ──────────────────────────────────────────────────────────────

    private static int computeRegistryHash() {
        int hash = 0;
        for (Map.Entry<ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
            hash += e.getKey().location().hashCode();
        }
        return hash;
    }

    private static void saveDiskCache() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", CACHE_VERSION);
            root.addProperty("registryHash", computeRegistryHash());
            root.addProperty("itemCount", ENTRIES.size());

            JsonArray items = new JsonArray();
            for (Entry entry : ENTRIES) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", entry.id());
                if (entry.displayName() != null && !entry.displayName().equals(entry.id())) {
                    obj.addProperty("name", entry.displayName());
                }
                if (entry.searchText() != null) {
                    obj.addProperty("search", entry.searchText());
                }
                items.add(obj);
            }
            root.add("items", items);

            Files.writeString(CACHE_PATH, GSON.toJson(root));
            Constants.LOG.debug("[{}] Saved {} icons to disk cache.", Constants.MOD_NAME, ENTRIES.size());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to save icon cache: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private static boolean tryLoadDiskCache() {
        try {
            if (!Files.exists(CACHE_PATH)) return false;
            String json = Files.readString(CACHE_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return false;

            int version = root.get("version").getAsInt();
            if (version != CACHE_VERSION) return false;

            int diskHash = root.get("registryHash").getAsInt();
            if (diskHash != computeRegistryHash()) {
                Constants.LOG.debug("[{}] Registry changed, rebuilding icon cache.", Constants.MOD_NAME);
                return false;
            }

            JsonArray items = root.getAsJsonArray("items");
            if (items == null || items.isEmpty()) return false;

            TARGET_COUNT = items.size();
            ENTRIES = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                JsonObject obj = items.get(i).getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.has("name") ? obj.get("name").getAsString() : id;
                String search = obj.has("search") ? obj.get("search").getAsString()
                        : (id + " item").toLowerCase(Locale.ROOT);
                ENTRIES.add(new Entry(id, name, search));
            }
            COMPLETE = true;
            ITER = null;
            Constants.LOG.debug("[{}] Loaded {} icons from disk cache.", Constants.MOD_NAME, items.size());
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to load icon cache: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }
}
