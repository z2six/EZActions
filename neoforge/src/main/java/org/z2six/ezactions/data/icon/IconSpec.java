// MainFile: src/main/java/org/z2six/ezactions/data/icon/IconSpec.java
package org.z2six.ezactions.data.icon;

import com.google.gson.JsonObject;
import org.z2six.ezactions.Constants;

/**
 * Simple icon descriptor.
 * Supports:
 * - ITEM icons (rendered from an ItemStack)
 * - CUSTOM icons loaded from config/ezactions/icons as 16x16 PNGs
 */
public final class IconSpec {

    public enum Kind { ITEM, CUSTOM }

    private final Kind kind;
    private final String id; // e.g., "minecraft:stone" for Kind.ITEM

    public IconSpec(Kind kind, String id) {
        this.kind = kind == null ? Kind.ITEM : kind;
        this.id = id == null ? "minecraft:stone" : id;
    }

    public static IconSpec item(String itemId) {
        return new IconSpec(Kind.ITEM, itemId);
    }

    public static IconSpec custom(String customId) {
        return new IconSpec(Kind.CUSTOM, customId);
    }

    public Kind kind() { return kind; }
    public String id() { return id; }

    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        try {
            o.addProperty("kind", kind.name());
            o.addProperty("id", id);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconSpec serialize failed: {}", Constants.MOD_NAME, t.toString());
        }
        return o;
    }

    public static IconSpec deserialize(JsonObject o) {
        try {
            String k = o.has("kind") ? o.get("kind").getAsString() : "ITEM";
            String id = o.has("id") ? o.get("id").getAsString() : "minecraft:stone";
            Kind kind = Kind.ITEM;
            try { kind = Kind.valueOf(k); } catch (IllegalArgumentException ignored) {}
            return new IconSpec(kind, id);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconSpec deserialize failed: {}", Constants.MOD_NAME, t.toString());
            return new IconSpec(Kind.ITEM, "minecraft:stone");
        }
    }

    @Override
    public String toString() {
        return kind + ":" + id;
    }
}
