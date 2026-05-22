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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Async disk I/O executor — prevents file reads/writes from blocking the tick thread.
    private static final ExecutorService DISK_IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ezactions-icon-io");
        t.setDaemon(true);
        return t;
    });

    // Non-volatile — read/written only under LOCK, except the volatile result flag.
    private static boolean diskLoadSubmitted = false;
    private static volatile boolean diskLoadReady = false;

    private VanillaIconCache() {}

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Call on each client tick to incrementally build the index. */
    public static void tickWarmup() {
        synchronized (LOCK) {
            if (COMPLETE) return;

            // Phase 1: kick off async disk load once.
            if (!diskLoadSubmitted) {
                diskLoadSubmitted = true;
                final int currentHash = computeRegistryHash();
                DISK_IO.execute(() -> {
                    List<Entry> loaded = tryLoadDiskCacheSync(currentHash);
                    if (loaded != null) {
                        synchronized (LOCK) {
                            if (!COMPLETE && ITER == null) {
                                ENTRIES = loaded;
                                TARGET_COUNT = loaded.size();
                                COMPLETE = true;
                                ITER = null;
                            }
                        }
                    }
                    diskLoadReady = true;
                });
            }

            // Phase 2: if async load hasn't finished yet, fall through to registry scan.
            if (diskLoadReady) {
                // If the executor already set COMPLETE, we're done.
                if (COMPLETE) return;
                // Disk cache was missing or mismatched — proceed with registry scan.
            }

            ensureStarted();
            pump(320, 1_500_000L);
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
            diskLoadSubmitted = false;
            diskLoadReady = false;
            reset();
        }
    }

    // ── Internal build logic ────────────────────────────────────────────────────

    private static void ensureStarted() {
        if (COMPLETE || ITER != null) return;
        // Disk cache is loaded asynchronously in tickWarmup() — if it didn't
        // arrive in time, fall through to the registry scan.
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

    /**
     * Rolling hash with a prime multiplier.
     * Much less collision-prone than a plain sum — two different registries
     * will produce different hashes with extremely high probability.
     * Items are iterated in registry order which is deterministic at runtime.
     */
    private static int computeRegistryHash() {
        int hash = 0;
        int count = 0;
        for (Map.Entry<ResourceKey<Item>, Item> e : BuiltInRegistries.ITEM.entrySet()) {
            hash = hash * 31 + e.getKey().location().hashCode();
            count++;
        }
        return hash ^ (count * 0x9E3779B9); // xor with golden-ratio-weighted count
    }

    /** Called on DISK_IO thread. Returns loaded entries or null on miss/mismatch. */
    private static List<Entry> tryLoadDiskCacheSync(int currentHash) {
        try {
            if (!Files.exists(CACHE_PATH)) return null;
            String json = Files.readString(CACHE_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return null;

            int version = root.get("version").getAsInt();
            if (version != CACHE_VERSION) return null;

            int diskHash = root.get("registryHash").getAsInt();
            if (diskHash != currentHash) {
                Constants.LOG.debug("[{}] Registry changed, rebuilding icon cache.", Constants.MOD_NAME);
                return null;
            }

            JsonArray items = root.getAsJsonArray("items");
            if (items == null || items.isEmpty()) return null;

            List<Entry> loaded = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                JsonObject obj = items.get(i).getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.has("name") ? obj.get("name").getAsString() : id;
                String search = obj.has("search") ? obj.get("search").getAsString()
                        : (id + " item").toLowerCase(Locale.ROOT);
                loaded.add(new Entry(id, name, search));
            }
            Constants.LOG.debug("[{}] Loaded {} icons from disk cache.", Constants.MOD_NAME, items.size());
            return loaded;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to load icon cache: {}", Constants.MOD_NAME, t.toString());
            return null;
        }
    }

    /** Snapshot ENTRIES under lock, then submit the write to DISK_IO. */
    private static void saveDiskCache() {
        final List<Entry> snapshot;
        final int hash;
        synchronized (LOCK) {
            if (ENTRIES == null || ENTRIES.isEmpty()) return;
            snapshot = List.copyOf(ENTRIES);
            hash = computeRegistryHash();
        }
        DISK_IO.execute(() -> {
            try {
                Files.createDirectories(CACHE_PATH.getParent());
                JsonObject root = new JsonObject();
                root.addProperty("version", CACHE_VERSION);
                root.addProperty("registryHash", hash);
                root.addProperty("itemCount", snapshot.size());

                JsonArray items = new JsonArray();
                for (Entry entry : snapshot) {
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
                Constants.LOG.debug("[{}] Saved {} icons to disk cache.", Constants.MOD_NAME, snapshot.size());
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Failed to save icon cache: {}", Constants.MOD_NAME, t.toString());
            }
        });
    }
}
