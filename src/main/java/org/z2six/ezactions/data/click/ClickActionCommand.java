// MainFile: src/main/java/org/z2six/ezactions/data/click/ClickActionCommand.java
package org.z2six.ezactions.data.click;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;

/**
 * Runs a server command from the client using the proper command path so the
 * client can sign message/component arguments when required (e.g. "say", "tellraw").
 * Pass commands WITH or WITHOUT a leading '/'; we'll strip it before sending.
 *
 * Crash-safe: all failures are logged and return false.
 */
public final class ClickActionCommand implements IClickAction {

    private final String commandRaw; // as stored (may have a leading '/')

    public ClickActionCommand(String command) {
        this.commandRaw = command == null ? "" : command.trim();
    }

    // --- IClickAction --------------------------------------------------------

    @Override
    public String getId() {
        // Short, stable id for logs/tooltips
        String base = commandRaw.isEmpty() ? "<empty>" : commandRaw;
        return "cmd:" + base;
    }

    @Override
    public ClickActionType getType() {
        return ClickActionType.COMMAND;
    }

    @Override
    public Component getDisplayName() {
        // Show the command (trimmed), but don’t spam UI with leading slash
        String s = normalized();
        return Component.literal(s.isEmpty() ? "(empty)" : s);
    }

    @Override
    public boolean execute(Minecraft mc) {
        try {
            if (mc == null) {
                Constants.LOG.warn("[{}] Command execute: no Minecraft instance.", Constants.MOD_NAME);
                return false;
            }
            final LocalPlayer player = mc.player;
            if (player == null || player.connection == null) {
                Constants.LOG.warn("[{}] Command execute: no player or connection.", Constants.MOD_NAME);
                return false;
            }

            final String cmd = normalized();
            if (cmd.isBlank()) {
                Constants.LOG.warn("[{}] Command execute: empty command.", Constants.MOD_NAME);
                return false;
            }

            // Ensure we’re on the client thread; this path signs arguments as needed.
            mc.execute(() -> {
                try {
                    player.connection.sendCommand(cmd); // IMPORTANT: no leading '/'
                    Constants.LOG.debug("[{}] Sent command: {}", Constants.MOD_NAME, cmd);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] sendCommand failed for '{}': {}", Constants.MOD_NAME, cmd, t.toString());
                }
            });
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Command execute error for '{}': {}", Constants.MOD_NAME, commandRaw, t.toString());
            return false;
        }
    }

    // --- JSON ----------------------------------------------------------------

    @Override
    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        try {
            o.addProperty("type", getType().name());
            o.addProperty("command", this.commandRaw);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionCommand serialize failed: {}", Constants.MOD_NAME, t.toString());
        }
        return o;
    }

    public static ClickActionCommand deserialize(JsonObject o) {
        try {
            String cmd = o.has("command") ? o.get("command").getAsString() : "";
            return new ClickActionCommand(cmd);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionCommand deserialize failed: {}", Constants.MOD_NAME, t.toString());
            return new ClickActionCommand("");
        }
    }

    // --- Helpers -------------------------------------------------------------

    private String normalized() {
        String s = this.commandRaw == null ? "" : this.commandRaw.trim();
        if (s.startsWith("/")) s = s.substring(1);
        return s;
    }
}
