// MainFile: src/main/java/org/z2six/ezactions/data/click/ClickActionCommand.java
package org.z2six.ezactions.data.click;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;

/**
 * Sends a client-side command (unsigned); leading '/' is optional.
 * If the player or connection is missing, we log and return false.
 */
public final class ClickActionCommand implements IClickAction {

    private final String command; // without leading '/'

    public ClickActionCommand(String command) {
        if (command != null && command.startsWith("/")) {
            this.command = command.substring(1);
        } else {
            this.command = command == null ? "" : command;
        }
    }

    @Override
    public String getId() {
        return "CMD:" + command;
    }

    @Override
    public ClickActionType getType() {
        return ClickActionType.COMMAND;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("/" + command);
    }

    @Override
    public boolean execute(Minecraft mc) {
        try {
            if (mc == null || mc.player == null) {
                logWarn("ClickActionCommand: no player/MC when running '/{}'", command);
                return false;
            }
            LocalPlayer p = mc.player;
            if (p.connection == null) {
                logWarn("ClickActionCommand: player connection not ready for '/{}'", command);
                return false;
            }
            p.connection.sendUnsignedCommand(command);
            logDebug("ClickActionCommand executed: '/{}'", command);
            return true;
        } catch (Throwable t) {
            logWarn("ClickActionCommand exception '/{}': {}", command, t.toString());
            return false;
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        try {
            o.addProperty("type", getType().name());
            o.addProperty("command", command);
        } catch (Throwable t) {
            logWarn("ClickActionCommand serialize error: {}", t.toString());
        }
        return o;
    }

    public static ClickActionCommand deserialize(JsonObject o) {
        try {
            String cmd = o.getAsJsonPrimitive("command").getAsString();
            return new ClickActionCommand(cmd);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionCommand deserialize failed: {}", Constants.MOD_NAME, t.toString());
            return new ClickActionCommand("");
        }
    }
}
