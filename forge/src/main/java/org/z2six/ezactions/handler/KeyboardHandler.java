// MainFile: src/main/java/org/z2six/ezactions/handler/KeyboardHandler.java
package org.z2six.ezactions.handler;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.RadialMenuScreen;
import org.z2six.ezactions.helper.ClientTaskQueue;
import org.z2six.ezactions.helper.InputInjector;
import org.z2six.ezactions.helper.KeyboardHandlerHelper;
import org.z2six.ezactions.util.EZActionsKeybinds;

// conflict contexts (Forge package)
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

// general config toggle
import org.z2six.ezactions.config.GeneralClientConfig;

// NEW: sequencer tick
import org.z2six.ezactions.util.CommandSequencer;

/*
 * HOLD-to-open radial with optional movement passthrough (toggle in general-client.toml).
 *
 * - If moveWhileRadialOpen is true (default), we:
 *     * push movement keys' conflict context to UNIVERSAL while the radial is open
 *     * mirror physical key state into KeyMapping#setDown during START and END client ticks
 * - If false, we do not alter contexts or mirror movement keys.
 * - Always restores original contexts on close. Never crashes; logs and skips on errors.
 */
public final class KeyboardHandler {

    private KeyboardHandler() {}

    private static boolean openHeldPrev = false;
    private static boolean suppressUntilRelease = false;

    private static boolean contextsPushed = false;
    private static KeyMapping[] trackedKeys = null;
    private static IKeyConflictContext[] prevContexts = null;

    /* Called by RadialMenuScreen when it executes an action on release. */
    public static void suppressReopenUntilReleased() {
        suppressUntilRelease = true;
    }

    /* Forge 1.20.1: ClientTickEvent has phases (START/END). This handler expects START (pre). */
    public static void onClientTickPre(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        try {
            ClientTaskQueue.drain();
            KeyboardHandlerHelper.onClientTick();

            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            boolean heldNow = isPhysicallyDown(mc, EZActionsKeybinds.OPEN_MENU);

            if (heldNow && !openHeldPrev && !suppressUntilRelease) {
                Constants.LOG.debug("[{}] Radial hotkey pressed; opening at root.", Constants.MOD_NAME);
                RadialMenu.open();
            }

            if (!heldNow && openHeldPrev) {
                if (mc.screen instanceof RadialMenuScreen s) {
                    s.onHotkeyReleased();
                }
                releaseMovementKeys(mc);
                popMovementKeyContexts(mc);
                suppressUntilRelease = false;
            }

            openHeldPrev = heldNow;

            // Respect toggle: allow movement only if enabled
            boolean allowMove = true;
            try { allowMove = GeneralClientConfig.INSTANCE.moveWhileRadialOpen.get(); } catch (Throwable ignored) {}
            if (mc.screen instanceof RadialMenuScreen) {
                if (allowMove) {
                    pushMovementKeyContexts(mc);
                    tickMovementPassthrough(mc);
                } else {
                    if (contextsPushed) popMovementKeyContexts(mc);
                }
            } else if (contextsPushed) {
                popMovementKeyContexts(mc);
            }

            if (EZActionsKeybinds.OPEN_EDITOR != null && EZActionsKeybinds.OPEN_EDITOR.consumeClick()) {
                mc.setScreen(new org.z2six.ezactions.gui.editor.MenuEditorScreen(mc.screen));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTickPre: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /* Forge 1.20.1: ClientTickEvent has phases (START/END). This handler expects END (post). */
    public static void onClientTickPost(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;

            boolean allowMove = true;
            try { allowMove = GeneralClientConfig.INSTANCE.moveWhileRadialOpen.get(); } catch (Throwable ignored) {}
            if (allowMove && mc.screen instanceof RadialMenuScreen) {
                tickMovementPassthrough(mc);
            }

            // NEW: run sequenced multi-commands (cheap no-op when idle)
            CommandSequencer.tickClient();

        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Exception during onClientTickPost: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- physical key state helpers ---

    private static boolean isPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        if (mapping == null || mc == null || mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        if (window == 0L) return false;

        InputConstants.Key key = mapping.getKey();
        if (key == null) return false;

        return switch (key.getType()) {
            case KEYSYM -> {
                int code = key.getValue();
                int state = (code >= 0) ? GLFW.glfwGetKey(window, code) : GLFW.GLFW_RELEASE;
                yield state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
            }
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    // --- movement passthrough ---

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
            mirrorKey(mc, o.keyShift);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Movement passthrough tick failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private static void mirrorKey(Minecraft mc, KeyMapping km) {
        if (km == null) return;
        InputConstants.Key k = km.getKey();
        if (k == null) { InputInjector.setKeyPressed(km, false); return; }
        long window = (mc.getWindow() != null) ? mc.getWindow().getWindow() : 0L;
        if (window == 0L) { InputInjector.setKeyPressed(km, false); return; }

        boolean down = switch (k.getType()) {
            case KEYSYM -> {
                int state = GLFW.glfwGetKey(window, k.getValue());
                yield (state == GLFW.GLFW_PRESS) || (state == GLFW.GLFW_REPEAT);
            }
            case MOUSE -> GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
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

    // --- conflict context push/pop ---

    private static void pushMovementKeyContexts(Minecraft mc) {
        if (contextsPushed) return;
        try {
            final Options o = mc.options;
            if (o == null) return;

            trackedKeys = new KeyMapping[] { o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keySprint, o.keyShift };
            prevContexts = new IKeyConflictContext[trackedKeys.length];

            for (int i = 0; i < trackedKeys.length; i++) {
                KeyMapping km = trackedKeys[i];
                if (km == null) continue;
                try {
                    prevContexts[i] = km.getKeyConflictContext();
                    km.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
                } catch (Throwable perKey) {
                    Constants.LOG.debug("[{}] Could not push context for movement key {}: {}", Constants.MOD_NAME, i, perKey.toString());
                }
            }
            contextsPushed = true;
            Constants.LOG.debug("[{}] Movement key contexts pushed (UNIVERSAL).", Constants.MOD_NAME);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] pushMovementKeyContexts failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

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
                        Constants.LOG.debug("[{}] Could not pop context for movement key {}: {}", Constants.MOD_NAME, i, perKey.toString());
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
