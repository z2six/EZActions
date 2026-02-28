package org.z2six.ezactions.helper;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionItemEquip;
import org.z2six.ezactions.util.ItemStackSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Tick-driven background executor for ITEM_EQUIP actions.
 * New actions cancel/replace in-flight plans immediately.
 */
public final class ItemEquipExecutor {
    private ItemEquipExecutor() {}

    private static final int SLOT_HELMET = 5;
    private static final int SLOT_CHEST = 6;
    private static final int SLOT_LEGS = 7;
    private static final int SLOT_BOOTS = 8;
    private static final int SLOT_OFFHAND = 45;

    private static final List<ClickActionItemEquip.TargetSlot> ORDER = List.of(
            ClickActionItemEquip.TargetSlot.HELMET,
            ClickActionItemEquip.TargetSlot.CHESTPLATE,
            ClickActionItemEquip.TargetSlot.LEGGINGS,
            ClickActionItemEquip.TargetSlot.BOOTS,
            ClickActionItemEquip.TargetSlot.OFFHAND,
            ClickActionItemEquip.TargetSlot.MAINHAND
    );
    private static final int VERIFY_DELAY_TICKS = 2;
    private static final int MAX_RETRIES_PER_SLOT = 6;

    private static Plan ACTIVE = null;

    public static void begin(ClickActionItemEquip action) {
        if (action == null) return;
        ACTIVE = new Plan(action);
        Constants.LOG.debug("[{}] ItemEquipExecutor: started new plan (replacing old).", Constants.MOD_NAME);
    }

    public static void tickClient() {
        Plan plan = ACTIVE;
        if (plan == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.gameMode == null) {
            ACTIVE = null;
            return;
        }
        LocalPlayer player = mc.player;
        AbstractContainerMenu menu = player.inventoryMenu;
        if (menu == null) {
            ACTIVE = null;
            return;
        }
        HolderLookup.Provider regs = player.level().registryAccess();

        // Execute one queued click step per tick.
        if (!plan.pendingClicks.isEmpty()) {
            Integer slot = plan.pendingClicks.pollFirst();
            if (slot != null) {
                try {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.PICKUP, player);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] ItemEquip click step failed: {}", Constants.MOD_NAME, t.toString());
                }
            }
            if (plan.pendingClicks.isEmpty()) {
                plan.verifyDelayTicks = VERIFY_DELAY_TICKS;
            }
            return;
        }

        if (plan.verifyDelayTicks > 0) {
            plan.verifyDelayTicks--;
            return;
        }

        // Continue/retry the current target until it verifies or exhausts retries.
        if (plan.currentTarget != null && plan.currentDesired != null) {
            if (verifyCurrentTarget(plan, menu, player, regs)) {
                clearCurrent(plan);
            } else if (plan.currentRetries >= MAX_RETRIES_PER_SLOT) {
                Constants.LOG.debug("[{}] ItemEquipExecutor: giving up on {} after {} retries.",
                        Constants.MOD_NAME, plan.currentTarget.key(), plan.currentRetries);
                clearCurrent(plan);
            } else {
                if (attemptCurrentTarget(plan, menu, player, regs)) {
                    plan.currentRetries++;
                    return;
                }
                // Source not found anymore; skip this target and continue with others.
                clearCurrent(plan);
            }
        }

        // Pick next target.
        while (plan.nextIndex < ORDER.size()) {
            ClickActionItemEquip.TargetSlot targetKind = ORDER.get(plan.nextIndex++);
            ClickActionItemEquip.StoredItem desired = plan.action.target(targetKind);
            if (desired == null) continue; // empty editor target = no-op

            int targetSlotId = resolveTargetSlot(targetKind, player);
            if (targetSlotId < 0) continue;

            if (matches(stackAt(menu, targetSlotId), desired, regs)) {
                continue;
            }

            plan.currentTarget = targetKind;
            plan.currentDesired = desired;
            plan.currentRetries = 0;

            if (attemptCurrentTarget(plan, menu, player, regs)) {
                plan.currentRetries++;
                return;
            }
            clearCurrent(plan);
        }

        // All targets processed.
        ACTIVE = null;
        Constants.LOG.debug("[{}] ItemEquipExecutor: plan completed.", Constants.MOD_NAME);
    }

    private static int resolveTargetSlot(ClickActionItemEquip.TargetSlot target, LocalPlayer player) {
        return switch (target) {
            case MAINHAND -> 36 + player.getInventory().selected;
            case OFFHAND -> SLOT_OFFHAND;
            case HELMET -> SLOT_HELMET;
            case CHESTPLATE -> SLOT_CHEST;
            case LEGGINGS -> SLOT_LEGS;
            case BOOTS -> SLOT_BOOTS;
        };
    }

    private static int findBestMatchingSource(AbstractContainerMenu menu,
                                              ClickActionItemEquip.StoredItem desired,
                                              int excludeSlotId,
                                              HolderLookup.Provider regs) {
        int bestSlot = -1;
        int bestCount = -1;

        // Armor + inventory + hotbar + offhand
        for (int slot = SLOT_HELMET; slot <= SLOT_BOOTS; slot++) {
            if (slot == excludeSlotId) continue;
            ItemStack s = stackAt(menu, slot);
            if (matches(s, desired, regs) && s.getCount() > bestCount) {
                bestSlot = slot; bestCount = s.getCount();
            }
        }
        for (int slot = 9; slot <= 44; slot++) {
            if (slot == excludeSlotId) continue;
            ItemStack s = stackAt(menu, slot);
            if (matches(s, desired, regs) && s.getCount() > bestCount) {
                bestSlot = slot; bestCount = s.getCount();
            }
        }
        if (SLOT_OFFHAND != excludeSlotId) {
            ItemStack s = stackAt(menu, SLOT_OFFHAND);
            if (matches(s, desired, regs) && s.getCount() > bestCount) {
                bestSlot = SLOT_OFFHAND; bestCount = s.getCount();
            }
        }
        return bestSlot;
    }

    private static boolean attemptCurrentTarget(Plan plan,
                                                AbstractContainerMenu menu,
                                                LocalPlayer player,
                                                HolderLookup.Provider regs) {
        if (plan.currentTarget == null || plan.currentDesired == null) return false;

        // If a previous packet left a carried item, stash it first to avoid corrupting the swap sequence.
        ItemStack carried = carried(menu);
        if (!carried.isEmpty()) {
            int dump = findAnyEmptyStorage(menu);
            if (dump >= 0) {
                plan.pendingClicks.addLast(dump);
                return true;
            }
            return false;
        }

        int targetSlotId = resolveTargetSlot(plan.currentTarget, player);
        if (targetSlotId < 0) return false;

        ItemStack targetStack = stackAt(menu, targetSlotId);
        int sourceSlotId = findBestMatchingSource(menu, plan.currentDesired, targetSlotId, regs);
        if (sourceSlotId < 0) {
            return false;
        }

        boolean targetWasEmpty = targetStack.isEmpty();
        plan.pendingClicks.addLast(sourceSlotId);
        plan.pendingClicks.addLast(targetSlotId);
        if (!targetWasEmpty) {
            plan.pendingClicks.addLast(sourceSlotId);
        }
        return true;
    }

    private static boolean verifyCurrentTarget(Plan plan,
                                               AbstractContainerMenu menu,
                                               LocalPlayer player,
                                               HolderLookup.Provider regs) {
        if (plan.currentTarget == null || plan.currentDesired == null) return true;
        int targetSlotId = resolveTargetSlot(plan.currentTarget, player);
        if (targetSlotId < 0) return true;
        return matches(stackAt(menu, targetSlotId), plan.currentDesired, regs);
    }

    private static void clearCurrent(Plan plan) {
        plan.currentTarget = null;
        plan.currentDesired = null;
        plan.currentRetries = 0;
        plan.verifyDelayTicks = 0;
    }

    private static int findAnyEmptyStorage(AbstractContainerMenu menu) {
        for (int slot = 9; slot <= 44; slot++) {
            if (stackAt(menu, slot).isEmpty()) return slot;
        }
        if (stackAt(menu, SLOT_OFFHAND).isEmpty()) return SLOT_OFFHAND;
        for (int slot = SLOT_HELMET; slot <= SLOT_BOOTS; slot++) {
            if (stackAt(menu, slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static ItemStack carried(AbstractContainerMenu menu) {
        try {
            return menu.getCarried();
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean matches(ItemStack stack, ClickActionItemEquip.StoredItem desired, HolderLookup.Provider regs) {
        try {
            if (stack == null || stack.isEmpty() || desired == null) return false;
            String sig = ItemStackSnapshot.signatureNoCount(stack, regs);
            if (desired.signatureNoCount().equals(sig)) return true;

            // Compatibility for entries saved by older snapshots where wrapped signatures were persisted.
            try {
                JsonObject encoded = desired.encodedStack();
                if (encoded != null && encoded.has("signature")) {
                    String legacy = encoded.get("signature").getAsString();
                    if (sig.equals(legacy)) return true;
                }
            } catch (Throwable ignored) {}

            // Semantic fallback for legacy saved entries: decode expected stack and compare item+NBT directly.
            try {
                ItemStack expected = decodeStoredStack(desired);
                if (!expected.isEmpty()) {
                    if (ItemStack.isSameItemSameTags(stack, expected)) return true;
                    if (ItemStack.isSameItem(stack, expected) && tagsEqualIgnoringZeroDamage(stack, expected)) return true;
                }
            } catch (Throwable ignored) {}

            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean tagsEqualIgnoringZeroDamage(ItemStack a, ItemStack b) {
        try {
            CompoundTag ta = a.getTag() == null ? new CompoundTag() : a.getTag().copy();
            CompoundTag tb = b.getTag() == null ? new CompoundTag() : b.getTag().copy();
            stripZeroDamage(ta);
            stripZeroDamage(tb);
            return ta.equals(tb);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void stripZeroDamage(CompoundTag t) {
        if (t == null) return;
        try {
            if (t.contains("Damage") && t.getInt("Damage") == 0) {
                t.remove("Damage");
            }
        } catch (Throwable ignored) {}
    }

    private static ItemStack decodeStoredStack(ClickActionItemEquip.StoredItem desired) {
        if (desired == null) return ItemStack.EMPTY;
        JsonObject encoded = desired.encodedStack();
        if (encoded == null) return ItemStack.EMPTY;

        // Preferred path: modern encoded stack object
        try {
            ItemStack decoded = ItemStack.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElse(ItemStack.EMPTY);
            if (decoded != null && !decoded.isEmpty()) return decoded;
        } catch (Throwable ignored) {}

        // Legacy path: stack.nbt SNBT string
        try {
            if (encoded.has("nbt")) {
                CompoundTag tag = TagParser.parseTag(encoded.get("nbt").getAsString());
                if (!tag.contains("Count")) tag.putByte("Count", (byte) 1);
                if (!tag.contains("id") && desired.itemId() != null) tag.putString("id", desired.itemId());
                ItemStack decoded = ItemStack.of(tag);
                if (!decoded.isEmpty()) return decoded;
            }
        } catch (Throwable ignored) {}

        // Legacy path: stack.signature SNBT-like object without Count
        try {
            if (encoded.has("signature")) {
                CompoundTag tag = TagParser.parseTag(encoded.get("signature").getAsString());
                if (!tag.contains("Count")) tag.putByte("Count", (byte) 1);
                if (!tag.contains("id") && desired.itemId() != null) tag.putString("id", desired.itemId());
                ItemStack decoded = ItemStack.of(tag);
                if (!decoded.isEmpty()) return decoded;
            }
        } catch (Throwable ignored) {}

        return ItemStack.EMPTY;
    }

    private static ItemStack stackAt(AbstractContainerMenu menu, int slotId) {
        try {
            if (menu == null || slotId < 0 || slotId >= menu.slots.size()) return ItemStack.EMPTY;
            return menu.getSlot(slotId).getItem();
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static final class Plan {
        final ClickActionItemEquip action;
        int nextIndex = 0;
        final Deque<Integer> pendingClicks = new ArrayDeque<>();
        int verifyDelayTicks = 0;
        int currentRetries = 0;
        ClickActionItemEquip.TargetSlot currentTarget = null;
        ClickActionItemEquip.StoredItem currentDesired = null;

        Plan(ClickActionItemEquip action) {
            this.action = action;
        }
    }
}
