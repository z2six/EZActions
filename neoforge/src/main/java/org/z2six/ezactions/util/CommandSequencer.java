// MainFile: src/main/java/org/z2six/ezactions/util/CommandSequencer.java
package org.z2six.ezactions.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.z2six.ezactions.Constants;

import java.util.ArrayDeque;

/**
 * Simple client-side sequencer for multi-command actions.
 * Enqueue a list of commands with a per-line delay (in ticks). Each client tick
 * decrements the timer; when it hits 0, we send the next command via sendCommand.
 *
 * Crash-safe: skips gracefully if player or connection goes away; clears queue.
 * Thread model: called and ticked on the client thread.
 */
public final class CommandSequencer {

    private CommandSequencer() {}

    private static final ArrayDeque<String> queue = new ArrayDeque<>();
    private static int delayTicks = 0;
    private static int ticksLeft = 0;

    /** Enqueue a new sequence; replaces any in-flight sequence. */
    public static void enqueue(String[] commands, int perLineDelayTicks) {
        try {
            queue.clear();
            if (commands != null) {
                for (String s : commands) {
                    if (s != null && !s.isBlank()) queue.addLast(s.trim());
                }
            }
            delayTicks = Math.max(0, perLineDelayTicks);
            ticksLeft = 0; // send first command on next tick
            Constants.LOG.debug("[{}] CommandSequencer: queued {} cmd(s), delay={} ticks.", Constants.MOD_NAME, queue.size(), delayTicks);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CommandSequencer.enqueue failed: {}", Constants.MOD_NAME, t.toString());
            queue.clear();
            delayTicks = 0;
            ticksLeft = 0;
        }
    }

    /** Called from client tick (POST is fine). */
    public static void tickClient() {
        try {
            if (queue.isEmpty()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) { clear(); return; }
            LocalPlayer p = mc.player;
            if (p == null || p.connection == null) { clear(); return; }

            if (ticksLeft > 0) {
                ticksLeft--;
                return;
            }

            String next = queue.pollFirst();
            if (next == null) { clear(); return; }

            // Send on the client thread; we are already on it in client tick.
            try {
                p.connection.sendCommand(next);
                Constants.LOG.debug("[{}] Sequencer sent command: {}", Constants.MOD_NAME, next);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Sequencer send failed for '{}': {}", Constants.MOD_NAME, next, t.toString());
            }

            // Schedule next
            if (queue.isEmpty()) {
                clear();
            } else {
                ticksLeft = Math.max(0, delayTicks);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CommandSequencer.tickClient failed: {}", Constants.MOD_NAME, t.toString());
            clear();
        }
    }

    private static void clear() {
        queue.clear();
        ticksLeft = 0;
        // keep delayTicks as last used value; harmless either way
    }
}
