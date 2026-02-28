package org.z2six.ezactions.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
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
            return;
        }

        // Plan next slot operation.
        while (plan.nextIndex < ORDER.size()) {
            ClickActionItemEquip.TargetSlot targetKind = ORDER.get(plan.nextIndex++);
            ClickActionItemEquip.StoredItem desired = plan.action.target(targetKind);
            if (desired == null) continue; // empty editor target = no-op

            int targetSlotId = resolveTargetSlot(targetKind, player);
            if (targetSlotId < 0) continue;

            ItemStack targetStack = stackAt(menu, targetSlotId);
            if (matches(targetStack, desired, regs)) {
                continue; // already right item
            }

            int sourceSlotId = findBestMatchingSource(menu, desired, targetSlotId, regs);
            if (sourceSlotId < 0) {
                // Desired item not found right now; skip this target only.
                continue;
            }

            boolean targetWasEmpty = targetStack.isEmpty();
            // Swap source -> target (old target back to source when occupied)
            plan.pendingClicks.addLast(sourceSlotId);
            plan.pendingClicks.addLast(targetSlotId);
            if (!targetWasEmpty) {
                plan.pendingClicks.addLast(sourceSlotId);
            }
            return;
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

    private static boolean matches(ItemStack stack, ClickActionItemEquip.StoredItem desired, HolderLookup.Provider regs) {
        try {
            if (stack == null || stack.isEmpty() || desired == null) return false;
            String sig = ItemStackSnapshot.signatureNoCount(stack, regs);
            return desired.signatureNoCount().equals(sig);
        } catch (Throwable t) {
            return false;
        }
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

        Plan(ClickActionItemEquip action) {
            this.action = action;
        }
    }
}

