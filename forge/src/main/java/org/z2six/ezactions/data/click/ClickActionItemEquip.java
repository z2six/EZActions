package org.z2six.ezactions.data.click;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.helper.ItemEquipExecutor;
import org.z2six.ezactions.util.ItemStackSnapshot;

import java.util.EnumMap;
import java.util.Map;

/** Action that equips recorded items into specific equipment slots. */
public final class ClickActionItemEquip implements IClickAction {

    public enum TargetSlot {
        MAINHAND("mainhand", "Mainhand"),
        OFFHAND("offhand", "Offhand"),
        HELMET("helmet", "Helmet"),
        CHESTPLATE("chestplate", "Chestplate"),
        LEGGINGS("leggings", "Leggings"),
        BOOTS("boots", "Boots");

        private final String key;
        private final String label;

        TargetSlot(String key, String label) { this.key = key; this.label = label; }
        public String key() { return key; }
        public String label() { return label; }
        public Component labelComponent() {
            return Component.translatable("ezactions.gui.item_equip.slot." + key);
        }

        public static @Nullable TargetSlot byKey(String key) {
            if (key == null) return null;
            for (TargetSlot s : values()) if (s.key.equalsIgnoreCase(key)) return s;
            return null;
        }
    }

    public static final class StoredItem {
        private final String signatureNoCount;
        private final String itemId;
        private final String displayName;
        private final JsonObject encodedStack;

        public StoredItem(String signatureNoCount, String itemId, String displayName, JsonObject encodedStack) {
            this.signatureNoCount = signatureNoCount == null ? "{}" : signatureNoCount;
            this.itemId = itemId == null ? "minecraft:air" : itemId;
            this.displayName = displayName == null ? this.itemId : displayName;
            this.encodedStack = encodedStack == null ? new JsonObject() : encodedStack;
        }

        public String signatureNoCount() { return signatureNoCount; }
        public String itemId() { return itemId; }
        public String displayName() { return displayName; }
        public JsonObject encodedStack() { return encodedStack; }

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("signature", signatureNoCount);
            o.addProperty("itemId", itemId);
            o.addProperty("displayName", displayName);
            o.add("stack", encodedStack.deepCopy());
            return o;
        }

        public static @Nullable StoredItem fromJson(JsonObject o) {
            try {
                if (o == null) return null;
                JsonObject stack = o.has("stack") && o.get("stack").isJsonObject()
                        ? o.getAsJsonObject("stack").deepCopy().getAsJsonObject()
                        : new JsonObject();
                String id = o.has("itemId") ? o.get("itemId").getAsString() : "minecraft:air";
                String name = o.has("displayName") ? o.get("displayName").getAsString() : id;

                String sig = o.has("signature") ? o.get("signature").getAsString() : ItemStackSnapshot.signatureNoCount(stack);
                boolean legacySig = looksLegacyWrappedSignature(sig);
                boolean legacyStack = looksLegacyStack(stack);

                if (legacySig && stack.has("signature")) {
                    try { sig = stack.get("signature").getAsString(); } catch (Throwable ignored) {}
                }

                if (legacySig || legacyStack || sig == null || sig.isBlank()) {
                    ItemStack decoded = decodeStackCompat(stack, id);
                    if (!decoded.isEmpty()) {
                        stack = ItemStackSnapshot.encode(decoded, null);
                        sig = ItemStackSnapshot.signatureNoCount(decoded, null);
                        id = ItemStackSnapshot.itemId(decoded);
                        if (!o.has("displayName")) {
                            try { name = decoded.getHoverName().getString(); } catch (Throwable ignored) {}
                        }
                    }
                }

                if (sig == null || sig.isBlank()) {
                    sig = ItemStackSnapshot.signatureNoCount(stack);
                }
                return new StoredItem(sig, id, name, stack);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] StoredItem.fromJson failed: {}", Constants.MOD_NAME, t.toString());
                return null;
            }
        }

        public static @Nullable StoredItem fromStack(ItemStack stack, HolderLookup.Provider registries) {
            try {
                if (stack == null || stack.isEmpty()) return null;
                JsonObject encoded = ItemStackSnapshot.encode(stack, registries);
                String sig = ItemStackSnapshot.signatureNoCount(stack, registries);
                String id = ItemStackSnapshot.itemId(stack);
                String name = stack.getHoverName().getString();
                return new StoredItem(sig, id, name, encoded);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] StoredItem.fromStack failed: {}", Constants.MOD_NAME, t.toString());
                return null;
            }
        }

        private static boolean looksLegacyWrappedSignature(String sig) {
            if (sig == null || sig.isBlank()) return true;
            return sig.contains("\"itemId\"") || sig.contains("\"nbt\"");
        }

        private static boolean looksLegacyStack(JsonObject stack) {
            if (stack == null) return true;
            return stack.has("nbt") || stack.has("itemId") || (!stack.has("id") && stack.has("signature"));
        }

        private static ItemStack decodeStackCompat(JsonObject stack, String fallbackItemId) {
            if (stack == null) return ItemStack.EMPTY;

            try {
                ItemStack decoded = ItemStack.CODEC.parse(JsonOps.INSTANCE, stack).result().orElse(ItemStack.EMPTY);
                if (decoded != null && !decoded.isEmpty()) return decoded;
            } catch (Throwable ignored) {}

            try {
                if (stack.has("nbt")) {
                    CompoundTag tag = TagParser.parseTag(stack.get("nbt").getAsString());
                    if (!tag.contains("Count")) tag.putByte("Count", (byte) 1);
                    if (!tag.contains("id") && fallbackItemId != null && !fallbackItemId.isBlank()) {
                        tag.putString("id", fallbackItemId);
                    }
                    ItemStack decoded = ItemStack.of(tag);
                    if (!decoded.isEmpty()) return decoded;
                }
            } catch (Throwable ignored) {}

            try {
                if (stack.has("signature")) {
                    CompoundTag tag = TagParser.parseTag(stack.get("signature").getAsString());
                    if (!tag.contains("Count")) tag.putByte("Count", (byte) 1);
                    if (!tag.contains("id") && fallbackItemId != null && !fallbackItemId.isBlank()) {
                        tag.putString("id", fallbackItemId);
                    }
                    ItemStack decoded = ItemStack.of(tag);
                    if (!decoded.isEmpty()) return decoded;
                }
            } catch (Throwable ignored) {}

            return ItemStack.EMPTY;
        }
    }

    private final EnumMap<TargetSlot, StoredItem> targets;

    public ClickActionItemEquip(Map<TargetSlot, StoredItem> targets) {
        this.targets = new EnumMap<>(TargetSlot.class);
        if (targets != null) this.targets.putAll(targets);
    }

    public Map<TargetSlot, StoredItem> targets() { return Map.copyOf(targets); }
    public @Nullable StoredItem target(TargetSlot slot) { return targets.get(slot); }
    public void setTarget(TargetSlot slot, @Nullable StoredItem item) {
        if (slot == null) return;
        if (item == null) targets.remove(slot);
        else targets.put(slot, item);
    }

    @Override
    public String getId() {
        return "item_equip:" + targets.size();
    }

    @Override
    public ClickActionType getType() {
        return ClickActionType.ITEM_EQUIP;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("ezactions.action.type.item_equip");
    }

    @Override
    public boolean execute(Minecraft mc) {
        try {
            ItemEquipExecutor.begin(this);
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionItemEquip execute failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "ITEM_EQUIP");
        JsonObject slots = new JsonObject();
        for (var e : targets.entrySet()) {
            if (e.getValue() != null) {
                slots.add(e.getKey().key(), e.getValue().toJson());
            }
        }
        o.add("slots", slots);
        return o;
    }

    public static ClickActionItemEquip deserialize(JsonObject o) {
        EnumMap<TargetSlot, StoredItem> map = new EnumMap<>(TargetSlot.class);
        try {
            if (o != null && o.has("slots") && o.get("slots").isJsonObject()) {
                JsonObject slots = o.getAsJsonObject("slots");
                for (Map.Entry<String, JsonElement> e : slots.entrySet()) {
                    TargetSlot slot = TargetSlot.byKey(e.getKey());
                    if (slot == null || e.getValue() == null || !e.getValue().isJsonObject()) continue;
                    StoredItem item = StoredItem.fromJson(e.getValue().getAsJsonObject());
                    if (item != null) map.put(slot, item);
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionItemEquip deserialize failed: {}", Constants.MOD_NAME, t.toString());
        }
        return new ClickActionItemEquip(map);
    }
}
