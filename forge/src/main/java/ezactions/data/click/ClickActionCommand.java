// MainFile: src/main/java/org/z2six/ezactions/data/click/ClickActionCommand.java
package ezactions.data.click;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.util.CommandSequencer;

/**
 * Runs one or more server commands from the client using the proper command path so the
 * client can sign message/component arguments when required (e.g. "say", "tellraw").
 * Lines are treated as separate commands. Pass commands WITH or WITHOUT a leading '/';
 * we'll strip it before sending.
 *
 * Crash-safe: all failures are logged and return false.
 */
public final class ClickActionCommand implements IClickAction {

    private final String commandRaw; // as stored (may include newlines and/or leading '/')
    private final int delayTicks;    // 0 = no delay; >0 => schedule with sequencer

    public ClickActionCommand(String command) {
        this(command, 0);
    }

    public ClickActionCommand(String command, int delayTicks) {
        this.commandRaw = command == null ? "" : command.trim();
        this.delayTicks = Math.max(0, delayTicks);
    }

    // --- Expose fields for editors/serialization helpers ----

    /** Returns the command exactly as stored (may include leading '/' and newlines). */
    public String getCommand() { return this.commandRaw; }

    /** Alias for reflection-based callers that look for "command()". */
    public String command() { return this.commandRaw; }

    /** Per-action delay between lines in ticks (0 = immediate). */
    public int getDelayTicks() { return this.delayTicks; }

    // --- IClickAction --------------------------------------------------------

    @Override
    public String getId() {
        String base = commandRaw.isEmpty() ? "<empty>" : commandRaw;
        return "cmd:" + base;
    }

    @Override
    public ClickActionType getType() {
        return ClickActionType.COMMAND;
    }

    @Override
    public Component getDisplayName() {
        // Show first line (normalized) as a compact label
        String s = normalizedFirstLine();
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

            final String[] lines = splitLinesNormalized(this.commandRaw);
            if (lines.length == 0) {
                Constants.LOG.warn("[{}] Command execute: empty command.", Constants.MOD_NAME);
                return false;
            }

            final int dly = this.delayTicks;
            if (dly <= 0 || lines.length == 1) {
                // Immediate dispatch on the client thread
                mc.execute(() -> {
                    for (String cmd : lines) {
                        try {
                            player.connection.sendCommand(cmd);
                            Constants.LOG.debug("[{}] Sent command: {}", Constants.MOD_NAME, cmd);
                        } catch (Throwable t) {
                            Constants.LOG.warn("[{}] sendCommand failed for '{}': {}", Constants.MOD_NAME, cmd, t.toString());
                        }
                    }
                });
            } else {
                // Schedule with a per-line delay
                CommandSequencer.enqueue(lines, dly);
                Constants.LOG.debug("[{}] Enqueued {} commands with {} tick(s) delay.", Constants.MOD_NAME, lines.length, dly);
            }
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
            o.addProperty("delayTicks", this.delayTicks); // backward compatible; absent => 0
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionCommand serialize failed: {}", Constants.MOD_NAME, t.toString());
        }
        return o;
    }

    public static ClickActionCommand deserialize(JsonObject o) {
        try {
            String cmd = o.has("command") ? o.get("command").getAsString() : "";
            int dly = o.has("delayTicks") ? Math.max(0, o.get("delayTicks").getAsInt()) : 0;
            return new ClickActionCommand(cmd, dly);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionCommand deserialize failed: {}", Constants.MOD_NAME, t.toString());
            return new ClickActionCommand("");
        }
    }

    // --- Helpers -------------------------------------------------------------

    private static String[] splitLinesNormalized(String raw) {
        if (raw == null) return new String[0];
        String[] in = raw.replace("\r", "").split("\n");
        java.util.ArrayList<String> out = new java.util.ArrayList<>(in.length);
        for (String line : in) {
            String s = line == null ? "" : line.trim();
            if (s.isEmpty()) continue;
            if (s.startsWith("/")) s = s.substring(1);
            out.add(s);
        }
        return out.toArray(new String[0]);
    }

    private String normalizedFirstLine() {
        String raw = (this.commandRaw == null) ? "" : this.commandRaw.replace("\r", "");
        int nl = raw.indexOf('\n');
        String first = (nl >= 0) ? raw.substring(0, nl) : raw;
        first = first.trim();
        if (first.startsWith("/")) first = first.substring(1);
        return first;
    }
}
