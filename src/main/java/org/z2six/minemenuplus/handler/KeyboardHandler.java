// MainFile: src/main/java/org/z2six/minemenuplus/handler/KeyboardHandler.java
package org.z2six.minemenuplus.handler;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.menu.RadialMenu;
import org.z2six.minemenuplus.gui.RadialMenuScreen;
import org.z2six.minemenuplus.helper.ClientTaskQueue;
import org.z2six.minemenuplus.helper.KeyboardHandlerHelper;
import org.z2six.minemenuplus.util.MineMenuKeybinds;

/**
 * Client tick: drain tasks, process tick-tap, and toggle our screens.
 */
public final class KeyboardHandler {

    private KeyboardHandler() {}

    public static void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            ClientTaskQueue.drain();
            KeyboardHandlerHelper.onClientTick();

            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            if (MineMenuKeybinds.OPEN_MENU != null && MineMenuKeybinds.OPEN_MENU.consumeClick()) {
                if (mc.screen instanceof RadialMenuScreen) {
                    mc.setScreen(null);
                } else {
                    Constants.LOG.debug("[{}] Open key consumed; opening RadialMenuScreen.", Constants.MOD_NAME);
                    RadialMenu.open();
                }
            }

            if (MineMenuKeybinds.OPEN_EDITOR != null && MineMenuKeybinds.OPEN_EDITOR.consumeClick()) {
                Constants.LOG.debug("[{}] Editor key consumed; opening menu editor.", Constants.MOD_NAME);
                // pass current screen as parent
                mc.setScreen(new org.z2six.minemenuplus.gui.editor.MenuEditorScreen(mc.screen));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTick: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
