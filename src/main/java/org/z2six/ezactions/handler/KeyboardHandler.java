package org.z2six.ezactions.handler;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.RadialMenuScreen;
import org.z2six.ezactions.helper.ClientTaskQueue;
import org.z2six.ezactions.helper.InputInjector;
import org.z2six.ezactions.helper.KeyboardHandlerHelper;
import org.z2six.ezactions.util.EZActionsKeybinds;

/**
 * HOLD-to-open radial with WASD passthrough.
 * Reads physical (GLFW) state so it remains stable while a Screen is open.
 */
public final class KeyboardHandler {

    private KeyboardHandler() {}

    private static boolean openHeldPrev = false;
    private static boolean suppressUntilRelease = false;

    /** Called by RadialMenuScreen when it executes an action on release. */
    public static void suppressReopenUntilReleased() {
        suppressUntilRelease = true;
    }

    public static void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            ClientTaskQueue.drain();
            KeyboardHandlerHelper.onClientTick();

            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            // HOLD logic (read physical state)
            boolean heldNow = isPhysicallyDown(mc, EZActionsKeybinds.OPEN_MENU);

            // rising edge → open at root (no disk reload every time)
            if (heldNow && !openHeldPrev && !suppressUntilRelease) {
                Constants.LOG.debug("[{}] Radial hotkey pressed; opening at root.", Constants.MOD_NAME);
                RadialMenu.open();
            }

            // falling edge → let the screen decide (release-to-activate)
            if (!heldNow && openHeldPrev) {
                if (mc.screen instanceof RadialMenuScreen s) {
                    s.onHotkeyReleased(); // may execute & close
                }
                releaseMovementKeys(mc);
                suppressUntilRelease = false;
            }

            openHeldPrev = heldNow;

            // WASD passthrough while radial is open
            if (mc.screen instanceof RadialMenuScreen) {
                tickMovementPassthrough(mc);
            }

            // Editor key
            if (EZActionsKeybinds.OPEN_EDITOR != null && EZActionsKeybinds.OPEN_EDITOR.consumeClick()) {
                mc.setScreen(new org.z2six.ezactions.gui.editor.MenuEditorScreen(mc.screen));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTick: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /* -------------------- physical key state -------------------- */

    private static boolean isPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        if (mapping == null || mc == null || mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        if (window == 0L) return false;

        InputConstants.Key key = mapping.getKey();
        if (key == null) return false;

        if (key.getType() == InputConstants.Type.KEYSYM) {
            int code = key.getValue();
            if (code < 0) return false;
            int state = GLFW.glfwGetKey(window, code);
            return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            int btn = key.getValue();
            if (btn < 0) return false;
            int state = GLFW.glfwGetMouseButton(window, btn);
            return state == GLFW.GLFW_PRESS;
        }
        return false;
    }

    /* -------------------- WASD passthrough -------------------- */

    private static void tickMovementPassthrough(Minecraft mc) {
        try {
            final Options o = mc.options;
            if (o == null) return;
            mirrorKey(mc, o.keyUp);
            mirrorKey(mc, o.keyDown);
            mirrorKey(mc, o.keyLeft);
            mirrorKey(mc, o.keyRight);
            mirrorKey(mc, o.keyJump);
            mirrorKey(mc, o.keySprint);
            mirrorKey(mc, o.keyShift); // sneak
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Movement passthrough tick failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private static void mirrorKey(Minecraft mc, KeyMapping km) {
        if (km == null) return;
        final InputConstants.Key k = km.getKey();
        if (k == null || k.getValue() < 0 || k.getType() != InputConstants.Type.KEYSYM) {
            InputInjector.setKeyPressed(km, false);
            return;
        }
        long window = mc.getWindow() != null ? mc.getWindow().getWindow() : 0L;
        if (window == 0L) {
            InputInjector.setKeyPressed(km, false);
            return;
        }
        int state = GLFW.glfwGetKey(window, k.getValue());
        boolean down = (state == GLFW.GLFW_PRESS) || (state == GLFW.GLFW_REPEAT);
        InputInjector.setKeyPressed(km, down);
    }

    private static void releaseMovementKeys(Minecraft mc) {
        try {
            final Options o = mc.options;
            if (o == null) return;
            InputInjector.setKeyPressed(o.keyUp, false);
            InputInjector.setKeyPressed(o.keyDown, false);
            InputInjector.setKeyPressed(o.keyLeft, false);
            InputInjector.setKeyPressed(o.keyRight, false);
            InputInjector.setKeyPressed(o.keyJump, false);
            InputInjector.setKeyPressed(o.keySprint, false);
            InputInjector.setKeyPressed(o.keyShift, false);
        } catch (Throwable ignored) {}
    }
}
