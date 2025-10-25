// KeyboardHandlerHelper.java
package org.z2six.ezactions.helper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.z2six.ezactions.Constants;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/*
 * // MainFile: KeyboardHandlerHelper.java
 * Legacy tick-based key "press" simulation.
 * This is what makes unassigned/purged keybinds still triggerable.
 *
 * We keep a queue of keys to "tap" and on each client tick we flip them down and release safely.
 * Nothing here touches real GLFW events; that's handled by InputInjector when needed.
 */
public final class KeyboardHandlerHelper {

    /* Keys scheduled for a one-tick tap. */
    private static final Deque<TapRequest> QUEUED_TAPS = new ArrayDeque<>();

    /* Keys currently being held down by our helper; we auto-release next tick. */
    private static final Deque<KeyMapping> HELD_THIS_TICK = new ArrayDeque<>();

    private KeyboardHandlerHelper() {}

    /* Enqueue a one-tick tap for the given mapping (safe when unbound). */
    public static void enqueueTap(KeyMapping mapping) {
        if (mapping == null) {
            Constants.LOG.warn("[{}] enqueueTap called with null mapping; skipping.", Constants.MOD_NAME);
            return;
        }
        QUEUED_TAPS.addLast(new TapRequest(mapping));
        Constants.LOG.debug("[{}] Enqueued tick-tap for mapping '{}'.", Constants.MOD_NAME, mapping.getName());
    }

    /* Call every client tick (Pre is fine). */
    public static void onClientTick() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            // Game not ready—clear state defensively (no crashes)
            if (!QUEUED_TAPS.isEmpty() || !HELD_THIS_TICK.isEmpty()) {
                Constants.LOG.debug("[{}] Clearing tap queues (no level). queued={}, held={}",
                        Constants.MOD_NAME, QUEUED_TAPS.size(), HELD_THIS_TICK.size());
            }
            QUEUED_TAPS.clear();
            releaseHeldKeys();
            return;
        }

        // First, release anything we held last tick.
        releaseHeldKeys();

        // Then, process queued taps for this tick.
        for (Iterator<TapRequest> it = QUEUED_TAPS.iterator(); it.hasNext(); ) {
            TapRequest req = it.next();
            KeyMapping map = req.mapping;
            try {
                // Flip down
                map.setDown(true);
                HELD_THIS_TICK.addLast(map);
                // Many listeners poll consumeClick()—trigger it once.
                map.consumeClick();

                Constants.LOG.debug("[{}] Tick-press mapping '{}'", Constants.MOD_NAME, map.getName());
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Failed to tick-press mapping '{}': {}", Constants.MOD_NAME, map.getName(), t.toString());
            } finally {
                it.remove();
            }
        }
    }

    private static void releaseHeldKeys() {
        while (!HELD_THIS_TICK.isEmpty()) {
            KeyMapping m = HELD_THIS_TICK.pollFirst();
            try {
                m.setDown(false);
                Constants.LOG.debug("[{}] Released mapping '{}'", Constants.MOD_NAME, m.getName());
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Failed to release mapping '{}': {}", Constants.MOD_NAME, m.getName(), t.toString());
            }
        }
    }

    /* Simple request record so we can extend later (e.g., hold duration) without changing queue type. */
    private record TapRequest(@Nullable KeyMapping mapping) {}
}
