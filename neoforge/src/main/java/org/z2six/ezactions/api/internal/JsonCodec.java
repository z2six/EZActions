package org.z2six.ezactions.api.internal;

import com.google.gson.*;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.json.ClickActionSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal JSON bridge for MenuItem <-> JsonObject.
 * - Icons: serialized as null for now (future-safe placeholder).
 * - Actions: delegated to ClickActionSerializer (raw data object).
 */
final class JsonCodec {
    private JsonCodec() {}

    static JsonObject toJson(MenuItem mi) {
        JsonObject o = new JsonObject();
        put(o, "id", mi.id());
        put(o, "title", mi.title());
        put(o, "note", mi.note());
        o.add("icon", JsonNull.INSTANCE); // reserved for future
        if (mi.isCategory()) {
            o.addProperty("type", "BUNDLE");
            List<MenuItem> kids = null;
            try { kids = mi.childrenMutable(); } catch (Throwable ignored) {}
            JsonArray arr = new JsonArray();
            if (kids != null) {
                for (MenuItem k : kids) {
                    try { arr.add(toJson(k)); } catch (Throwable ignored) {}
                }
            }
            o.add("children", arr);
        } else {
            o.addProperty("type", "ACTION");
            JsonObject a = new JsonObject();
            a.addProperty("kind", "internal");
            try {
                IClickAction act = mi.action(); // if you have it; if not, serializer must be able to get it from item
                JsonObject data = ClickActionSerializer.serialize(act);
                a.add("data", data != null ? data : new JsonObject());
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] JsonCodec: action serialize failed: {}", Constants.MOD_NAME, t.toString());
                a.add("data", new JsonObject());
            }
            o.add("action", a);
        }
        return o;
    }

    static MenuItem fromJson(JsonObject o) {
        String id = optString(o, "id", freshId("api"));
        String title = optString(o, "title", "");
        String note = optString(o, "note", "");

        String type = optString(o, "type", "ACTION");
        if ("BUNDLE".equals(type)) {
            List<MenuItem> children = new ArrayList<>();
            JsonArray arr = o.has("children") && o.get("children").isJsonArray() ? o.getAsJsonArray("children") : new JsonArray();
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    try { children.add(fromJson(el.getAsJsonObject())); }
                    catch (Throwable ignored) {}
                }
            }
            return new MenuItem(id, title, note, null, null, children);
        } else {
            // ACTION
            IClickAction act = null;
            try {
                JsonObject action = (o.has("action") && o.get("action").isJsonObject()) ? o.getAsJsonObject("action") : new JsonObject();
                JsonObject data = action.has("data") && action.get("data").isJsonObject() ? action.getAsJsonObject("data") : new JsonObject();
                act = ClickActionSerializer.deserialize(data);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] JsonCodec: action deserialize failed: {}", Constants.MOD_NAME, t.toString());
            }
            return new MenuItem(id, title, note, null, act, null);
        }
    }

    // convenience

    static JsonArray toJsonArray(List<MenuItem> list) {
        JsonArray arr = new JsonArray();
        if (list != null) for (MenuItem mi : list) arr.add(toJson(mi));
        return arr;
    }

    static List<MenuItem> fromJsonArray(JsonArray arr) {
        List<MenuItem> out = new ArrayList<>();
        for (JsonElement el : arr) if (el.isJsonObject()) out.add(fromJson(el.getAsJsonObject()));
        return out;
    }

    private static void put(JsonObject o, String k, String v) {
        if (v == null) o.add(k, JsonNull.INSTANCE); else o.addProperty(k, v);
    }

    private static String optString(JsonObject o, String k, String def) {
        if (o == null || !o.has(k)) return def;
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? def : e.getAsString();
    }

    private static String freshId(String prefix) {
        long t = System.currentTimeMillis();
        return prefix + "_" + Long.toHexString(t) + "_" + Integer.toHexString((int)(Math.random()*0xFFFF));
    }
}
