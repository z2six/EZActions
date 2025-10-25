// MainFile: src/main/java/org/z2six/ezactions/data/click/IClickAction.java
package org.z2six.ezactions.data.click;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;

/**
 * Contract for all click actions (key press, command, use item, etc).
 * Implementations must be null-safe: never crash; log and return false on failure.
 */
public interface IClickAction {

    /** A stable identifier for this action instance, for logs/UI (can be the action type + short detail). */
    String getId();

    /** The action type (KEY, COMMAND, ...). */
    ClickActionType getType();

    /** Human-friendly title for menus (localizable); avoid heavy work here. */
    default Component getDisplayName() {
        return Component.literal(getType().name());
    }

    /**
     * Execute the action on the client. Return true on success, false otherwise.
     * MUST NOT throw — log and return false.
     */
    boolean execute(Minecraft mc);

    /** Serialize this action to JSON for persistence. MUST NOT throw. */
    JsonObject serialize();

    /** Utility for guarded logging. */
    default void logDebug(String fmt, Object... args) {
        Constants.LOG.debug("[" + Constants.MOD_NAME + "] " + fmt, args);
    }

    default void logWarn(String fmt, Object... args) {
        Constants.LOG.warn("[" + Constants.MOD_NAME + "] " + fmt, args);
    }
}
