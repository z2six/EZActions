// MainFile: src/main/java/org/z2six/ezactions/util/BundleHotkeyManager.java
package org.z2six.ezactions.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
