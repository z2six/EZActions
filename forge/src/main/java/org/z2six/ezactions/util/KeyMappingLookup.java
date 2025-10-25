// MainFile: src/main/java/org/z2six/ezactions/util/KeyMappingLookup.java
package org.z2six.ezactions.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;

/*
 * Helpers for dealing with KeyMappings in a defensive, crash-free way.
 */
public final class KeyMappingLookup {

    private KeyMappingLookup() {}

    /*
     * Find a KeyMapping by its internal name (e.g., "key.minimap.open").
     * We match on KeyMapping#getName() exact string.
     */
    @Nullable
    public static KeyMapping findByName(String mappingName) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            Options opts = mc.options;
            if (opts == null || opts.keyMappings == null) return null;
            for (KeyMapping km : opts.keyMappings) {
                if (km != null && mappingName.equals(km.getName())) {
                    return km;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeyMappingLookup.findByName failed for '{}': {}", Constants.MOD_NAME, mappingName, t.toString());
        }
        return null;
    }

    /* Localized label for a key mapping (for UI). */
    public static Component localizedName(KeyMapping mapping) {
        try {
            String key = mapping.getName();
            return Component.literal(I18n.get(key));
        } catch (Throwable t) {
            return Component.literal(mapping.getName());
        }
    }

    /*
     * Attempt to derive GLFW key + scancode for a mapping. Returns nulls when not possible
     * (e.g., mouse, unknown, or unbound). That’s fine — our injector will fall back.
     */
    public static @Nullable Integer glfwKey(KeyMapping mapping) {
        try {
            InputConstants.Key key = mapping.getKey();
            if (key == null) return null;
            if (key.getType() != InputConstants.Type.KEYSYM) return null; // ignore mouse, scancodes
            return key.getValue(); // GLFW key code
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to resolve GLFW key for '{}': {}", Constants.MOD_NAME, mapping.getName(), t.toString());
            return null;
        }
    }

    public static @Nullable Integer glfwScancode(@Nullable Integer glfwKey) {
        try {
            if (glfwKey == null) return null;
            // LWJGL utility: scancode for a key code, if available
            int scan = GLFW.glfwGetKeyScancode(glfwKey);
            return scan == 0 ? null : scan;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to resolve scancode for key {}: {}", Constants.MOD_NAME, glfwKey, t.toString());
            return null;
        }
    }
}
