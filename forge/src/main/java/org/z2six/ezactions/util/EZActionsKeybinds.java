// MainFile: src/main/java/org/z2six/ezactions/util/EZActionsKeybinds.java
package org.z2six.ezactions.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;

/*
 * Registers the ezactions keybinds.
 * - OPEN_MENU: backtick by default
 * - OPEN_EDITOR: UNBOUND by default (user can assign)
 */
public final class EZActionsKeybinds {

    public static KeyMapping OPEN_MENU;
    public static KeyMapping OPEN_EDITOR;

    private EZActionsKeybinds() {}

    /* MOD-bus listener (no annotations). */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        try {
            OPEN_MENU = new KeyMapping(
                    "key.ezactions.open",
                    GLFW.GLFW_KEY_GRAVE_ACCENT, // default: backtick
                    "key.categories.ezactions"
            );
            e.register(OPEN_MENU);

            OPEN_EDITOR = new KeyMapping(
                    "key.ezactions.open_editor",
                    InputConstants.UNKNOWN.getValue(), // default: unbound (-1)
                    "key.categories.ezactions"
            );
            e.register(OPEN_EDITOR);

            Constants.LOG.debug("[{}] Registered keybinds: {}, {}", Constants.MOD_NAME,
                    OPEN_MENU.getName(), OPEN_EDITOR.getName());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to register keybinds: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
