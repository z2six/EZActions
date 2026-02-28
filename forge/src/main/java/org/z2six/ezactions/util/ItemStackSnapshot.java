package org.z2six.ezactions.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.z2six.ezactions.Constants;

/** Helpers to capture and compare full ItemStack data while ignoring stack count. */
public final class ItemStackSnapshot {
    private static final Gson GSON = new Gson();

    private ItemStackSnapshot() {}

    public static JsonObject encode(ItemStack stack) {
        JsonObject fallback = new JsonObject();
        try {
            if (stack == null || stack.isEmpty()) return fallback;
            JsonObject out = new JsonObject();
            out.addProperty("itemId", itemId(stack));
            out.addProperty("nbt", stack.save(new CompoundTag()).toString());
            out.addProperty("signature", signatureNoCount(stack));
            return out;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ItemStackSnapshot.encode failed: {}", Constants.MOD_NAME, t.toString());
            return fallbackFor(stack);
        }
    }

    public static String signatureNoCount(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) return "{}";
            CompoundTag nbt = stack.save(new CompoundTag());
            Tag stripped = stripCount(nbt);
            return stripped.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    public static String signatureNoCount(JsonObject encoded) {
        try {
            JsonElement stripped = stripCount(encoded == null ? new JsonObject() : encoded);
            return GSON.toJson(stripped);
        } catch (Throwable t) {
            return "{}";
        }
    }

    public static String itemId(ItemStack stack) {
        try {
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return key == null ? "minecraft:air" : key.toString();
        }
        catch (Throwable t) { return "minecraft:air"; }
    }

    private static JsonObject fallbackFor(ItemStack stack) {
        JsonObject o = new JsonObject();
        try { o.addProperty("id", itemId(stack)); } catch (Throwable ignored) {}
        return o;
    }

    private static JsonElement stripCount(JsonElement el) {
        if (el == null || el.isJsonNull()) return new JsonObject();
        if (el.isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement child : el.getAsJsonArray()) out.add(stripCount(child));
            return out;
        }
        if (el.isJsonObject()) {
            JsonObject out = new JsonObject();
            for (var entry : el.getAsJsonObject().entrySet()) {
                String k = entry.getKey();
                if ("count".equalsIgnoreCase(k) || "Count".equalsIgnoreCase(k)) continue;
                out.add(k, stripCount(entry.getValue()));
            }
            return out;
        }
        return el.deepCopy();
    }

    private static Tag stripCount(Tag tag) {
        if (tag == null) return new CompoundTag();
        if (tag instanceof CompoundTag in) {
            CompoundTag out = new CompoundTag();
            for (String key : in.getAllKeys()) {
                if ("count".equalsIgnoreCase(key) || "Count".equalsIgnoreCase(key)) continue;
                Tag child = in.get(key);
                out.put(key, stripCount(child));
            }
            return out;
        }
        if (tag instanceof ListTag inList) {
            ListTag out = new ListTag();
            for (Tag child : inList) {
                out.add(stripCount(child));
            }
            return out;
        }
        return tag.copy();
    }
}
