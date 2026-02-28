package org.z2six.ezactions.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.Constants;

/** Helpers to capture and compare full ItemStack data while ignoring stack count. */
public final class ItemStackSnapshot {
    private static final Gson GSON = new Gson();

    private ItemStackSnapshot() {}

    public static JsonObject encode(ItemStack stack, HolderLookup.Provider registries) {
        JsonObject fallback = new JsonObject();
        try {
            if (stack == null || stack.isEmpty()) return fallback;
            if (registries == null) return fallbackFor(stack);

            JsonElement el = ItemStack.CODEC.encodeStart(
                            registries.createSerializationContext(JsonOps.INSTANCE), stack)
                    .result()
                    .orElse(null);
            if (el != null && el.isJsonObject()) {
                return el.getAsJsonObject();
            }
            return fallbackFor(stack);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ItemStackSnapshot.encode failed: {}", Constants.MOD_NAME, t.toString());
            return fallbackFor(stack);
        }
    }

    public static String signatureNoCount(ItemStack stack, HolderLookup.Provider registries) {
        return signatureNoCount(encode(stack, registries));
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
        try { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
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
}

