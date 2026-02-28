package org.z2six.ezactions.api.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.z2six.ezactions.data.json.ClickActionSerializer;
import org.z2six.ezactions.data.menu.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON bridge for API import/export.
 * Uses the same schema as live menu.json / clipboard import-export.
 */
final class JsonCodec {
    private JsonCodec() {}

    static JsonObject toJson(MenuItem mi) {
        return (mi == null) ? new JsonObject() : mi.serialize();
    }

    static MenuItem fromJson(JsonObject o) {
        return MenuItem.deserialize(o == null ? new JsonObject() : o);
    }

    static JsonArray toJsonArray(List<MenuItem> list) {
        JsonArray arr = new JsonArray();
        if (list != null) {
            for (MenuItem mi : list) arr.add(toJson(mi));
        }
        return arr;
    }

    static List<MenuItem> fromJsonArray(JsonArray arr) {
        List<MenuItem> out = new ArrayList<>();
        if (arr == null) return out;
        for (JsonElement el : arr) {
            if (el != null && el.isJsonObject()) out.add(fromJson(el.getAsJsonObject()));
        }
        return out;
    }

    static Optional<String> validate(JsonElement root) {
        if (root == null || root.isJsonNull()) return Optional.of("JSON is empty.");
        if (root.isJsonArray()) {
            JsonArray arr = root.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (el == null || !el.isJsonObject()) {
                    return Optional.of("Entry #" + (i + 1) + " must be an object.");
                }
                Optional<String> err = validateItem(el.getAsJsonObject(), "$[" + i + "]");
                if (err.isPresent()) return err;
            }
            return Optional.empty();
        }
        if (root.isJsonObject()) {
            return validateItem(root.getAsJsonObject(), "$");
        }
        return Optional.of("JSON must be an object or array.");
    }

    private static Optional<String> validateItem(JsonObject o, String path) {
        if (o == null) return Optional.of(path + " is null.");

        Optional<String> err;
        err = validateStringOrComponent(o, "title", path); if (err.isPresent()) return err;
        err = validateStringOrComponent(o, "note", path); if (err.isPresent()) return err;
        err = validateStringOrNull(o, "id", path); if (err.isPresent()) return err;
        err = validateStringOrNull(o, "icon", path); if (err.isPresent()) return err;
        err = validateBooleanOrNull(o, "hideFromMainRadial", path); if (err.isPresent()) return err;
        err = validateBooleanOrNull(o, "bundleKeybindEnabled", path); if (err.isPresent()) return err;
        err = validateBooleanOrNull(o, "locked", path); if (err.isPresent()) return err;

        boolean hasAction = o.has("action") && o.get("action") != null && o.get("action").isJsonObject();
        boolean hasChildren = o.has("children") && o.get("children") != null && o.get("children").isJsonArray();

        if (hasAction && hasChildren) {
            return Optional.of(path + " cannot contain both 'action' and 'children'.");
        }
        if (!hasAction && !hasChildren) {
            return Optional.of(path + " must contain either 'action' (action item) or 'children' (bundle).");
        }

        if (hasAction) {
            try {
                JsonObject action = o.getAsJsonObject("action");
                if (!action.has("type") || !action.get("type").isJsonPrimitive()) {
                    return Optional.of(path + ".action.type is required and must be a string.");
                }
                ClickActionSerializer.deserialize(action);
            } catch (Throwable t) {
                return Optional.of(path + ".action is invalid: " + t.getMessage());
            }
        }

        if (hasChildren) {
            JsonArray kids = o.getAsJsonArray("children");
            for (int i = 0; i < kids.size(); i++) {
                JsonElement child = kids.get(i);
                if (child == null || !child.isJsonObject()) {
                    return Optional.of(path + ".children[" + i + "] must be an object.");
                }
                Optional<String> childErr = validateItem(child.getAsJsonObject(), path + ".children[" + i + "]");
                if (childErr.isPresent()) return childErr;
            }
        }

        return Optional.empty();
    }

    private static Optional<String> validateStringOrComponent(JsonObject o, String key, String path) {
        if (!o.has(key) || o.get(key) == null || o.get(key).isJsonNull()) return Optional.empty();
        JsonElement el = o.get(key);
        if (el.isJsonPrimitive()) return Optional.empty();
        if (el.isJsonObject() || el.isJsonArray()) return Optional.empty();
        return Optional.of(path + "." + key + " must be a string or text component JSON.");
    }

    private static Optional<String> validateStringOrNull(JsonObject o, String key, String path) {
        if (!o.has(key) || o.get(key) == null || o.get(key).isJsonNull()) return Optional.empty();
        if (o.get(key).isJsonPrimitive()) return Optional.empty();
        return Optional.of(path + "." + key + " must be a string.");
    }

    private static Optional<String> validateBooleanOrNull(JsonObject o, String key, String path) {
        if (!o.has(key) || o.get(key) == null || o.get(key).isJsonNull()) return Optional.empty();
        if (o.get(key).isJsonPrimitive()) {
            try {
                o.get(key).getAsBoolean();
                return Optional.empty();
            } catch (Throwable ignored) {}
        }
        return Optional.of(path + "." + key + " must be a boolean.");
    }
}
