// MainFile: src/main/java/org/z2six/ezactions/util/BundleHotkeyManager.java
package org.z2six.ezactions.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.*;

/**
 * Handles dynamic keybind registration for bundle categories.
 *
 * - On keymapping registration, we scan the current menu tree (from disk) and register
 *   an unbound KeyMapping for each category where bundleKeybindEnabled == true.
 * - The mapping between bundle id (which equals its title) and KeyMapping is stored so
 *   KeyboardHandler can detect presses and open the radial directly inside that bundle.
 *
 * All changes to which bundles have keybinds are applied on *next restart*, because
 * key mappings are registered only during RegisterKeyMappingsEvent.
 */
public final class BundleHotkeyManager {

    private static final Map<String, KeyMapping> BUNDLE_KEYS_BY_ID = new HashMap<>();
    private static final Set<String> RESTART_WARNED_IDS = new HashSet<>();
    private static long lastWarnAtMs = 0L;

    private BundleHotkeyManager() {}

    /** Called from EZActionsKeybinds.onRegisterKeyMappings. */
    public static void registerBundleKeyMappings(RegisterKeyMappingsEvent e) {
        BUNDLE_KEYS_BY_ID.clear();
        try {
            // Load menu model from disk so we see the last saved bundles.
            RadialMenu.rootMutable(); // ensureLoaded inside

            List<MenuItem> root = new ArrayList<>(RadialMenu.rootMutable());
            Set<String> usedIds = new HashSet<>();
            List<MenuItem> bundles = new ArrayList<>();
            collectBundles(root, bundles, usedIds);

            if (bundles.isEmpty()) {
                Constants.LOG.debug("[{}] BundleHotkeyManager: no bundles with keybinds enabled.", Constants.MOD_NAME);
                return;
            }

            for (MenuItem bundle : bundles) {
                String id = bundle.id();
                if (id == null || id.isBlank()) continue;

                String cleanId = sanitizeBundleId(id);
                String keyName = "key." + Constants.MOD_ID + ".bundle." + cleanId;
                String category = "key.categories." + Constants.MOD_ID + ".bundles";

                KeyMapping mapping = new KeyMapping(
                        keyName,
                        KeyConflictContext.IN_GAME,
                        KeyModifier.NONE,
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(), // default: unbound
                        category
                );
                e.register(mapping);
                BUNDLE_KEYS_BY_ID.put(id, mapping);

                Constants.LOG.debug("[{}] Registered bundle keybind: id='{}', keyName='{}', category='{}'",
                        Constants.MOD_NAME, id, keyName, category);
            }

            Constants.LOG.info("[{}] BundleHotkeyManager: registered {} bundle keybind(s).",
                    Constants.MOD_NAME, BUNDLE_KEYS_BY_ID.size());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] BundleHotkeyManager.registerBundleKeyMappings failed: {}", Constants.MOD_NAME, t.toString());
            BUNDLE_KEYS_BY_ID.clear();
        }
    }

    /** Exposed for KeyboardHandler: mapping bundleId -> KeyMapping. */
    public static Map<String, KeyMapping> getBundleKeyMappings() {
        return BUNDLE_KEYS_BY_ID;
    }

    /**
     * Show a one-time client message when keybind-enabled bundles exist that are not yet
     * registered in this session (which means restart is required).
     */
    public static void notifyRestartRequiredIfNeeded(String source) {
        try {
            Set<String> enabled = enabledBundleIdsNow();
            Set<String> pending = new HashSet<>(enabled);
            pending.removeAll(BUNDLE_KEYS_BY_ID.keySet());

            // Keep the warning set in sync when entries are no longer pending.
            RESTART_WARNED_IDS.retainAll(pending);
            if (pending.isEmpty()) return;

            Set<String> fresh = new HashSet<>(pending);
            fresh.removeAll(RESTART_WARNED_IDS);
            if (fresh.isEmpty()) return;

            long now = System.currentTimeMillis();
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (now - lastWarnAtMs < 1200L) return;

            lastWarnAtMs = now;
            RESTART_WARNED_IDS.addAll(fresh);

            String sample = fresh.iterator().next();
            int extra = fresh.size() - 1;
            Component msg = (extra > 0)
                    ? Component.translatable("ezactions.message.restart_required_bundles_more", sample, extra)
                    : Component.translatable("ezactions.message.restart_required_bundles_one", sample);
            mc.player.displayClientMessage(msg, false);
            Constants.LOG.info("[{}] Restart needed for {} bundle keybind change(s). source={}",
                    Constants.MOD_NAME, fresh.size(), source == null ? "unknown" : source);
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] notifyRestartRequiredIfNeeded failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private static Set<String> enabledBundleIdsNow() {
        Set<String> out = new HashSet<>();
        try {
            List<MenuItem> root = new ArrayList<>(RadialMenu.rootMutable());
            collectEnabledIds(root, out);
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] enabledBundleIdsNow failed: {}", Constants.MOD_NAME, t.toString());
        }
        return out;
    }

    private static void collectEnabledIds(List<MenuItem> src, Set<String> out) {
        if (src == null || out == null) return;
        for (MenuItem mi : src) {
            if (mi == null || !mi.isCategory()) continue;
            try {
                if (mi.bundleKeybindEnabled() && mi.id() != null && !mi.id().isBlank()) {
                    out.add(mi.id());
                }
                collectEnabledIds(mi.childrenMutable(), out);
            } catch (Throwable ignored) {}
        }
    }

    private static void collectBundles(List<MenuItem> src, List<MenuItem> out, Set<String> usedIds) {
        if (src == null) return;
        for (MenuItem mi : src) {
            if (mi == null) continue;
            try {
                if (mi.isCategory()) {
                    if (mi.bundleKeybindEnabled()) {
                        String id = mi.id();
                        if (id != null && !id.isBlank() && usedIds.add(id)) {
                            out.add(mi);
                        }
                    }
                    // Recurse into children either way; sub-bundles are also valid.
                    collectBundles(mi.childrenMutable(), out, usedIds);
                }
            } catch (Throwable t) {
                Constants.LOG.debug("[{}] collectBundles skipped entry due to error: {}", Constants.MOD_NAME, t.toString());
            }
        }
    }

    private static String sanitizeBundleId(String id) {
        // Keep it simple: lowercase and replace anything not [a-z0-9_.-] with '_'
        String lower = id.toLowerCase(Locale.ROOT).trim();
        if (lower.isEmpty()) return "unnamed";
        return lower.replaceAll("[^a-z0-9_.-]", "_");
    }
}
