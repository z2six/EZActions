// MainFile: src/main/java/org/z2six/minemenuplus/util/MineMenuKeybinds.java
package org.z2six.minemenuplus.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.z2six.minemenuplus.Constants;

/**
 * Registers the MineMenuPlus keybinds.
 * - OPEN_MENU: backtick by default
 * - OPEN_EDITOR: UNBOUND by default (user can assign)
 */
public final class MineMenuKeybinds {

    public static KeyMapping OPEN_MENU;
    public static KeyMapping OPEN_EDITOR;

    private MineMenuKeybinds() {}

    /** MOD-bus listener (no annotations). */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        try {
            OPEN_MENU = new KeyMapping(
                    "key.minemenuplus.open",
                    GLFW.GLFW_KEY_GRAVE_ACCENT, // default: backtick
                    "key.categories.minemenuplus"
            );
            e.register(OPEN_MENU);

            OPEN_EDITOR = new KeyMapping(
                    "key.minemenuplus.open_editor",
                    InputConstants.UNKNOWN.getValue(), // default: unbound
                    "key.categories.minemenuplus"
            );
            e.register(OPEN_EDITOR);

            Constants.LOG.debug("[{}] Registered keybinds: {}, {}", Constants.MOD_NAME,
                    OPEN_MENU.getName(), OPEN_EDITOR.getName());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register keybinds: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
