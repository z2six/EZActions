// MainFile: src/main/java/org/z2six/minemenuplus/data/json/ClickActionSerializer.java
package org.z2six.minemenuplus.data.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.click.*;

/**
 * Minimal polymorphic serializer for IClickAction.
 * File format:
 *  { "type":"KEY", ... } or { "type":"COMMAND", ... }
 */
public final class ClickActionSerializer {

    private ClickActionSerializer() {}

    public static JsonObject serialize(IClickAction action) {
        try {
            return action.serialize();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionSerializer.serialize failed for {}: {}", Constants.MOD_NAME, action.getId(), t.toString());
            JsonObject o = new JsonObject();
            o.addProperty("type", action.getType().name());
            o.addProperty("error", t.toString());
            return o;
        }
    }

    public static IClickAction deserialize(JsonObject obj) {
        String typeStr = GsonHelper.getAsString(obj, "type", "");
        ClickActionType type;
        try {
            type = ClickActionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Unknown action type: " + typeStr);
        }

        return switch (type) {
            case KEY -> ClickActionKey.deserialize(obj);
            case COMMAND -> ClickActionCommand.deserialize(obj);
        };
    }
}
