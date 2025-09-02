// MainFile: src/main/java/org/z2six/ezactions/handler/KeyboardHandler.java
package org.z2six.ezactions.handler;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

// NeoForge conflict-context API
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * HOLD-to-open radial with movement passthrough.
 *
 * Key points:
 * - We mirror physical key state (GLFW) into KeyMapping#setDown while the radial screen is open.
 * - Additionally, we temporarily switch movement keys' conflict context to UNIVERSAL while the
 *   radial screen is open; otherwise IN_GAME keys are inactive during GUI screens.
 * - We restore original contexts on close, and we never crash (errors are logged + skipped).
 */
public final class KeyboardHandler {

    private KeyboardHandler() {}

    private static boolean openHeldPrev = false;
    private static boolean suppressUntilRelease = false;

    // --- conflict-context push/pop state ---
    private static boolean contextsPushed = false;
    private static KeyMapping[] trackedKeys = null;
    private static IKeyConflictContext[] prevContexts = null;

    /** Called by RadialMenuScreen when it executes an action on release. */
    public static void suppressReopenUntilReleased() {
        suppressUntilRelease = true;
    }

    /** Client tick (PRE): open/close edge detection and early-mirror (harmless). */
    public static void onClientTickPre(ClientTickEvent.Pre e) {
        try {
            ClientTaskQueue.drain();
            KeyboardHandlerHelper.onClientTick();

            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            // HOTKEY hold logic
            boolean heldNow = isPhysicallyDown(mc, EZActionsKeybinds.OPEN_MENU);

            if (heldNow && !openHeldPrev && !suppressUntilRelease) {
                Constants.LOG.debug("[{}] Radial hotkey pressed; opening at root.", Constants.MOD_NAME);
                RadialMenu.open();
            }

            if (!heldNow && openHeldPrev) {
                // hotkey released
                if (mc.screen instanceof RadialMenuScreen s) {
                    s.onHotkeyReleased(); // may execute & close
                }
                releaseMovementKeys(mc);
                popMovementKeyContexts(mc); // restore IN_GAME contexts
                suppressUntilRelease = false;
            }

            openHeldPrev = heldNow;

            // If our screen is open, ensure contexts are pushed and mirror movement
            if (mc.screen instanceof RadialMenuScreen) {
                pushMovementKeyContexts(mc); // makes movement keys active under a GUI
                tickMovementPassthrough(mc);
            } else {
                // If screen changed away without a clean release, still restore
                if (contextsPushed) {
                    popMovementKeyContexts(mc);
                }
            }

            // Editor key (unchanged)
            if (EZActionsKeybinds.OPEN_EDITOR != null && EZActionsKeybinds.OPEN_EDITOR.consumeClick()) {
                mc.setScreen(new org.z2six.ezactions.gui.editor.MenuEditorScreen(mc.screen));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTickPre: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /** Client tick (POST): final pass so our mirrored state wins for the tick. */
    public static void onClientTickPost(ClientTickEvent.Post e) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            if (mc.screen instanceof RadialMenuScreen) {
                tickMovementPassthrough(mc);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTickPost: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /* -------------------- physical key state -------------------- */

    private static boolean isPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        if (mapping == null || mc == null || mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        if (window == 0L) return false;

        InputConstants.Key key = mapping.getKey();
        if (key == null) return false;

        switch (key.getType()) {
            case KEYSYM -> {
                int code = key.getValue();
                if (code < 0) return false;
                int state = GLFW.glfwGetKey(window, code);
                return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
            }
            case MOUSE -> {
                int btn = key.getValue();
                if (btn < 0) return false;
                int state = GLFW.glfwGetMouseButton(window, btn);
                return state == GLFW.GLFW_PRESS;
            }
            default -> {
                return false;
            }
        }
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
        if (k == null || k.getValue() < 0) {
            InputInjector.setKeyPressed(km, false);
            return;
        }
        long window = mc.getWindow() != null ? mc.getWindow().getWindow() : 0L;
        if (window == 0L) {
            InputInjector.setKeyPressed(km, false);
            return;
        }
        boolean down;
        if (k.getType() == InputConstants.Type.KEYSYM) {
            int state = GLFW.glfwGetKey(window, k.getValue());
            down = (state == GLFW.GLFW_PRESS) || (state == GLFW.GLFW_REPEAT);
        } else if (k.getType() == InputConstants.Type.MOUSE) {
            int state = GLFW.glfwGetMouseButton(window, k.getValue());
            down = (state == GLFW.GLFW_PRESS);
        } else {
            down = false;
        }
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

    /* -------------------- conflict context push/pop -------------------- */

    /** Ensure movement keys are active under GUI by switching to UNIVERSAL while the radial is open. */
    private static void pushMovementKeyContexts(Minecraft mc) {
        if (contextsPushed) return;
        try {
            final Options o = mc.options;
            if (o == null) return;

            trackedKeys = new KeyMapping[] {
                    o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keySprint, o.keyShift
            };
            prevContexts = new IKeyConflictContext[trackedKeys.length];

            for (int i = 0; i < trackedKeys.length; i++) {
                KeyMapping km = trackedKeys[i];
                if (km == null) continue;
                try {
                    IKeyConflictContext prev = km.getKeyConflictContext();
                    prevContexts[i] = prev;
                    km.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
                } catch (Throwable perKey) {
                    Constants.LOG.debug("[{}] Could not push context for key {}: {}", Constants.MOD_NAME, i, perKey.toString());
                }
            }
            contextsPushed = true;
            Constants.LOG.debug("[{}] Movement key contexts pushed (UNIVERSAL).", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] pushMovementKeyContexts failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /** Restore original conflict contexts for movement keys when the radial closes. */
    private static void popMovementKeyContexts(Minecraft mc) {
        if (!contextsPushed) return;
        try {
            if (trackedKeys != null && prevContexts != null) {
                for (int i = 0; i < trackedKeys.length; i++) {
                    KeyMapping km = trackedKeys[i];
                    IKeyConflictContext prev = prevContexts[i];
                    if (km == null || prev == null) continue;
                    try {
                        km.setKeyConflictContext(prev);
                    } catch (Throwable perKey) {
                        Constants.LOG.debug("[{}] Could not pop context for key {}: {}", Constants.MOD_NAME, i, perKey.toString());
                    }
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] popMovementKeyContexts failed: {}", Constants.MOD_NAME, t.toString());
        } finally {
            contextsPushed = false;
            trackedKeys = null;
            prevContexts = null;
            Constants.LOG.debug("[{}] Movement key contexts restored.", Constants.MOD_NAME);
        }
    }
}
