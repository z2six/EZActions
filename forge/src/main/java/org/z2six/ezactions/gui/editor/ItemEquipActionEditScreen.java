package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.gui.compat.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.gui.EzScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionItemEquip;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Editor for ITEM_EQUIP action. */
public final class ItemEquipActionEditScreen extends EzScreen {

    private static final int SLOT = 18;
    private static final int SRC_CELL = 20;
    private static final int SRC_GAP = 4;

    private static final ClickActionItemEquip.TargetSlot[] SLOT_ORDER = {
            ClickActionItemEquip.TargetSlot.MAINHAND,
            ClickActionItemEquip.TargetSlot.OFFHAND,
            ClickActionItemEquip.TargetSlot.HELMET,
            ClickActionItemEquip.TargetSlot.CHESTPLATE,
            ClickActionItemEquip.TargetSlot.LEGGINGS,
            ClickActionItemEquip.TargetSlot.BOOTS
    };
    private static final Map<ClickActionItemEquip.TargetSlot, ResourceLocation> SLOT_PLACEHOLDER_TEX = Map.of(
            ClickActionItemEquip.TargetSlot.MAINHAND, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/mainhand.png"),
            ClickActionItemEquip.TargetSlot.OFFHAND, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/offhand.png"),
            ClickActionItemEquip.TargetSlot.HELMET, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/helmet.png"),
            ClickActionItemEquip.TargetSlot.CHESTPLATE, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/chestplate.png"),
            ClickActionItemEquip.TargetSlot.LEGGINGS, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/leggings.png"),
            ClickActionItemEquip.TargetSlot.BOOTS, ResourceLocation.tryParse("ezactions:textures/gui/item_equip_slots/boots.png")
    );

    private final Screen parent;
    private final MenuItem editing;

    private String draftTitle = "";
    private String draftNote = "";
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");
    private final EnumMap<ClickActionItemEquip.TargetSlot, ClickActionItemEquip.StoredItem> draftTargets =
            new EnumMap<>(ClickActionItemEquip.TargetSlot.class);

    private EditBox titleBox;
    private EditBox noteBox;

    private final List<SourceEntry> source = new ArrayList<>();
    private int sourceScrollRows = 0;

    private final ActionEditorUi.ScrollArea scroll = new ActionEditorUi.ScrollArea();
    private ActionEditorUi.Panel panel;

    private int bodyX;
    private int bodyY;
    private int bodyW;
    private int bodyH;

    private int topCardX;
    private int topCardY;
    private int topCardW;
    private int topCardH;

    private int targetCardX;
    private int targetCardY;
    private int targetCardW;
    private int targetCardH;

    private int targetGridX;
    private int targetGridY;
    private int targetSlotGap;

    private int sourceCardX;
    private int sourceCardY;
    private int sourceCardW;
    private int sourceCardH;

    private int sourceX;
    private int sourceY;
    private int sourceW;
    private int sourceH;
    private int sourceCols;
    private int sourceVisibleRows;

    private int iconX;
    private int iconBaseY;

    private ClickActionItemEquip.StoredItem draggingItem = null;
    private ItemStack draggingStack = ItemStack.EMPTY;

    private static final class SourceEntry {
        final ItemStack stack;
        final ClickActionItemEquip.StoredItem stored;

        SourceEntry(ItemStack stack, ClickActionItemEquip.StoredItem stored) {
            this.stack = stack;
            this.stored = stored;
        }
    }

    public ItemEquipActionEditScreen(Screen parent, MenuItem editing) {
        super(Component.translatable(editing == null
                ? "ezactions.gui.item_equip.title.add"
                : "ezactions.gui.item_equip.title.edit"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null) {
            draftTitle = safe(editing.title());
            draftNote = safe(editing.note());
            if (editing.icon() != null) {
                draftIcon = editing.icon();
            }
            if (editing.action() instanceof ClickActionItemEquip eq) {
                draftTargets.putAll(eq.targets());
            }
        }
    }

    @Override
    protected void init() {
        this.panel = ActionEditorUi.panel(this.width, this.height, 980, 680, 8);
        this.scroll.reset();

        bodyX = panel.x() + 14;
        bodyY = panel.y() + 34;
        bodyW = panel.w() - 28;
        bodyH = panel.h() - 42;

        topCardX = bodyX;
        topCardY = bodyY;
        topCardW = bodyW;
        topCardH = 98;

        targetCardX = bodyX;
        targetCardY = topCardY + topCardH + 10;
        targetCardW = bodyW;
        targetCardH = 74;

        sourceCardX = bodyX;
        sourceCardY = targetCardY + targetCardH + 10;
        sourceCardW = bodyW;
        sourceCardH = 232;

        int iconAreaW = 58;
        int fieldW = Math.max(220, topCardW - iconAreaW - 24);
        int fieldX = topCardX + 12;

        iconX = topCardX + topCardW - 46;
        iconBaseY = topCardY + 34;

        titleBox = new EditBox(this.font, fieldX, topCardY + 26, fieldW, 20, Component.translatable("ezactions.gui.field.title"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        scroll.track(addRenderableWidget(titleBox));

        noteBox = new EditBox(this.font, fieldX, topCardY + 56, fieldW, 20, Component.translatable("ezactions.gui.field.note"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        scroll.track(addRenderableWidget(noteBox));

        int slotsWidth = (SLOT * SLOT_ORDER.length);
        int available = targetCardW - 24;
        targetSlotGap = Math.max(2, (available - slotsWidth) / Math.max(1, SLOT_ORDER.length - 1));
        int rowWidth = slotsWidth + (targetSlotGap * (SLOT_ORDER.length - 1));
        targetGridX = targetCardX + Math.max(10, (targetCardW - rowWidth) / 2);
        targetGridY = targetCardY + 34;

        sourceX = sourceCardX + 10;
        sourceY = sourceCardY + 28;
        sourceW = sourceCardW - 20;
        sourceH = Math.max(40, sourceCardH - 40);

        int pitch = SRC_CELL + SRC_GAP;
        sourceCols = Math.max(1, sourceW / pitch);
        sourceVisibleRows = Math.max(1, (sourceH + SRC_GAP) / pitch);

        int buttonY = sourceCardY + sourceCardH + 12;
        int totalW = (96 * 3) + (8 * 2);
        int left = panel.x() + (panel.w() - totalW) / 2;

        scroll.track(addRenderableWidget(ActionEditorUi.button(left, buttonY, 96, 20, Component.translatable("ezactions.gui.common.save"), this::onSave)));
        scroll.track(addRenderableWidget(ActionEditorUi.button(left + 104, buttonY, 96, 20, Component.translatable("ezactions.gui.common.cancel"), this::onClose)));
        scroll.track(addRenderableWidget(ActionEditorUi.button(left + 208, buttonY, 96, 20, Component.translatable("ezactions.gui.common.back"), () -> this.minecraft.setScreen(parent))));

        scroll.track(addRenderableWidget(ActionEditorUi.button(sourceCardX + sourceCardW - 108, sourceCardY + 6, 100, 18, Component.translatable("ezactions.gui.item_equip.refresh_items"), this::rebuildSource)));

        scroll.include(bodyY, buttonY + 28);
        scroll.layout(bodyY, bodyY + bodyH);

        rebuildSource();
    }

    private void rebuildSource() {
        source.clear();
        sourceScrollRows = 0;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }
            var inv = mc.player.getInventory();

            addStack(inv.offhand.get(0));
            for (ItemStack st : inv.armor) {
                addStack(st);
            }
            for (ItemStack st : inv.items) {
                addStack(st);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ItemEquip source rebuild failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private void addStack(ItemStack st) {
        if (st == null || st.isEmpty()) {
            return;
        }
        var stored = ClickActionItemEquip.StoredItem.fromStack(st);
        if (stored != null) {
            source.add(new SourceEntry(st.copy(), stored));
        }
    }

    private void onSave() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote = safe(noteBox == null ? draftNote : noteBox.getValue()).trim();
            if (draftTitle.isEmpty()) {
                return;
            }

            ClickActionItemEquip action = new ClickActionItemEquip(draftTargets);
            MenuItem item = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("equip"),
                    draftTitle,
                    draftNote,
                    draftIcon,
                    action,
                    List.of()
            );

            boolean ok = (editing == null)
                    ? RadialMenu.addToCurrent(item)
                    : RadialMenu.replaceInCurrent(editing.id(), item);

            if (!ok) {
                Constants.LOG.info("[{}] ItemEquip save failed for '{}'.", Constants.MOD_NAME, draftTitle);
            }

            if (parent instanceof MenuEditorScreen m) {
                m.refreshFromChild();
            }
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ItemEquip save failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ActionEditorUi.drawFrame(g, this.font, this.width, this.height, panel, this.title);
        scroll.layout(bodyY, bodyY + bodyH);

        ActionEditorUi.drawCard(g, this.font, topCardX, scroll.y(topCardY), topCardW, topCardH, Component.empty());
        ActionEditorUi.drawCard(g, this.font, targetCardX, scroll.y(targetCardY), targetCardW, targetCardH, Component.translatable("ezactions.gui.item_equip.card.targets"));
        ActionEditorUi.drawCard(g, this.font, sourceCardX, scroll.y(sourceCardY), sourceCardW, sourceCardH, Component.translatable("ezactions.gui.item_equip.card.source"));

        if (titleBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.title"), titleBox.x, titleBox.y - 10);
        }
        if (noteBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.note"), noteBox.x, noteBox.y - 10);
        }

        ActionEditorUi.drawIconCard(g, this.font, iconX, scroll.y(iconBaseY), 32, Component.translatable("ezactions.gui.field.icon"), draftIcon, iconHit(mouseX, mouseY));

        drawTargetSlots(g, mouseX, mouseY);
        drawSourceGrid(g, mouseX, mouseY);
        scroll.drawScrollbar(g, bodyX, bodyY, bodyW, bodyH);

        super.render(g, mouseX, mouseY, partialTick);

        if (draggingItem != null && !draggingStack.isEmpty()) {
            g.renderItem(draggingStack, mouseX - 8, mouseY - 8);
        }
    }

    private void drawTargetSlots(GuiGraphics g, int mouseX, int mouseY) {
        ClickActionItemEquip.TargetSlot hovered = null;
        for (int i = 0; i < SLOT_ORDER.length; i++) {
            var slot = SLOT_ORDER[i];
            int[] p = slotPos(i);
            int x = p[0];
            int y = p[1];

            g.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, 0x6E2B2B2B);
            g.fill(x, y, x + SLOT, y + SLOT, 0xD6101010);

            ClickActionItemEquip.StoredItem st = draftTargets.get(slot);
            if (st != null) {
                try {
                    IconRenderer.drawIcon(g, x + (SLOT / 2), y + (SLOT / 2), IconSpec.item(st.itemId()));
                } catch (Throwable ignored) {}
            } else {
                try {
                    drawPlaceholderTexture(g, slot, x, y);
                } catch (Throwable ignored) {}
            }

            if (mouseX >= x && mouseX <= x + SLOT && mouseY >= y && mouseY <= y + SLOT) {
                hovered = slot;
            }
        }

        if (hovered != null) {
            ClickActionItemEquip.StoredItem st = draftTargets.get(hovered);
            if (st != null) {
                g.renderTooltip(this.font, Component.translatable(
                        "ezactions.gui.item_equip.tooltip.slot_filled",
                        slotLabel(hovered),
                        st.displayName(),
                        st.itemId()
                ), mouseX, mouseY);
            } else {
                g.renderTooltip(this.font, Component.translatable(
                        "ezactions.gui.item_equip.tooltip.slot_empty",
                        slotLabel(hovered)
                ), mouseX, mouseY);
            }
        }
    }

    private void drawSourceGrid(GuiGraphics g, int mouseX, int mouseY) {
        int pitch = SRC_CELL + SRC_GAP;
        int maxRows = Math.max(0, (int) Math.ceil(source.size() / (double) sourceCols));
        int maxScroll = Math.max(0, maxRows - sourceVisibleRows);
        if (sourceScrollRows > maxScroll) {
            sourceScrollRows = maxScroll;
        }

        int start = sourceScrollRows * sourceCols;
        int end = Math.min(source.size(), start + (sourceVisibleRows * sourceCols));

        int hoveredIndex = -1;
        int drawSourceY = scroll.y(sourceY);
        for (int i = start; i < end; i++) {
            int rel = i - start;
            int col = rel % sourceCols;
            int row = rel / sourceCols;
            int x = sourceX + (col * pitch);
            int y = drawSourceY + (row * pitch);

            g.fill(x - 1, y - 1, x + SRC_CELL + 1, y + SRC_CELL + 1, 0x44444444);
            g.fill(x, y, x + SRC_CELL, y + SRC_CELL, 0xCC111111);

            ItemStack st = source.get(i).stack;
            g.renderItem(st, x + 2, y + 2);
            g.renderItemDecorations(this.font, st, x + 2, y + 2);

            if (mouseX >= x && mouseX <= x + SRC_CELL && mouseY >= y && mouseY <= y + SRC_CELL) {
                hoveredIndex = i;
            }
        }

        String scrollText = Component.translatable("ezactions.gui.item_equip.rows",
                (sourceScrollRows + 1),
                Math.max(1, maxRows)).getString();
        g.drawString(this.font, scrollText, sourceCardX + 10, scroll.y(sourceCardY + sourceCardH - 10), ActionEditorUi.MUTED_TEXT);

        if (hoveredIndex >= 0 && hoveredIndex < source.size()) {
            SourceEntry e = source.get(hoveredIndex);
            g.renderTooltip(this.font,
                    Component.translatable("ezactions.gui.item_equip.tooltip.source_item", e.stack.getHoverName(), e.stack.getCount()),
                    mouseX,
                    mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && iconHit(mouseX, mouseY)) {
            openIconPicker();
            return true;
        }

        if (button == 1) {
            ClickActionItemEquip.TargetSlot slot = targetAt(mouseX, mouseY);
            if (slot != null) {
                draftTargets.remove(slot);
                return true;
            }
        }

        if (button == 0) {
            int idx = sourceIndexAt(mouseX, mouseY);
            if (idx >= 0 && idx < source.size()) {
                SourceEntry e = source.get(idx);
                draggingItem = copyStored(e.stored);
                draggingStack = e.stack.copy();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingItem != null) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingItem != null) {
            ClickActionItemEquip.TargetSlot target = targetAt(mouseX, mouseY);
            if (target != null) {
                draftTargets.put(target, copyStored(draggingItem));
            }
            draggingItem = null;
            draggingStack = ItemStack.EMPTY;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int drawSourceY = scroll.y(sourceY);
        if (mouseX >= sourceX && mouseX <= sourceX + sourceW && mouseY >= drawSourceY && mouseY <= drawSourceY + sourceH) {
            int maxRows = Math.max(0, (int) Math.ceil(source.size() / (double) sourceCols));
            int maxScroll = Math.max(0, maxRows - sourceVisibleRows);
            int before = sourceScrollRows;
            if (delta < 0) {
                sourceScrollRows = Math.min(maxScroll, sourceScrollRows + 1);
            } else if (delta > 0) {
                sourceScrollRows = Math.max(0, sourceScrollRows - 1);
            }
            if (before != sourceScrollRows) {
                return true;
            }
        }

        if (scroll.mouseScrolled(mouseX, mouseY, delta, bodyX, bodyY, bodyW, bodyH)) {
            scroll.layout(bodyY, bodyY + bodyH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private void openIconPicker() {
        this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
            draftIcon = (ic == null) ? IconSpec.item("minecraft:stone") : ic;
            this.minecraft.setScreen(this);
        }));
    }

    private boolean iconHit(double mouseX, double mouseY) {
        int y = scroll.y(iconBaseY);
        return mouseX >= iconX && mouseX <= iconX + 32 && mouseY >= y && mouseY <= y + 32;
    }

    private int[] slotPos(int idx) {
        int x = targetGridX + (idx * (SLOT + targetSlotGap));
        int y = scroll.y(targetGridY);
        return new int[]{x, y};
    }

    private ClickActionItemEquip.TargetSlot targetAt(double mouseX, double mouseY) {
        for (int i = 0; i < SLOT_ORDER.length; i++) {
            int[] p = slotPos(i);
            int x = p[0];
            int y = p[1];
            if (mouseX >= x && mouseX <= x + SLOT && mouseY >= y && mouseY <= y + SLOT) {
                return SLOT_ORDER[i];
            }
        }
        return null;
    }

    private int sourceIndexAt(double mouseX, double mouseY) {
        int drawSourceY = scroll.y(sourceY);
        if (!(mouseX >= sourceX && mouseX <= sourceX + sourceW && mouseY >= drawSourceY && mouseY <= drawSourceY + sourceH)) {
            return -1;
        }

        int start = sourceScrollRows * sourceCols;
        int end = Math.min(source.size(), start + (sourceVisibleRows * sourceCols));
        int pitch = SRC_CELL + SRC_GAP;

        for (int i = start; i < end; i++) {
            int rel = i - start;
            int col = rel % sourceCols;
            int row = rel / sourceCols;
            int x = sourceX + (col * pitch);
            int y = drawSourceY + (row * pitch);
            if (mouseX >= x && mouseX <= x + SRC_CELL && mouseY >= y && mouseY <= y + SRC_CELL) {
                return i;
            }
        }

        return -1;
    }

    private static void drawPlaceholderTexture(GuiGraphics g, ClickActionItemEquip.TargetSlot slot, int x, int y) {
        ResourceLocation tex = SLOT_PLACEHOLDER_TEX.get(slot);
        if (tex == null) {
            return;
        }
        g.blit(tex, x + 1, y + 1, 0.0f, 0.0f, SLOT - 2, SLOT - 2, SLOT - 2, SLOT - 2);
    }

    private static ClickActionItemEquip.StoredItem copyStored(ClickActionItemEquip.StoredItem in) {
        if (in == null) {
            return null;
        }
        return new ClickActionItemEquip.StoredItem(
                in.signatureNoCount(),
                in.itemId(),
                in.displayName(),
                in.encodedStack().deepCopy().getAsJsonObject()
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Component slotLabel(ClickActionItemEquip.TargetSlot slot) {
        if (slot == null) {
            return Component.empty();
        }
        return Component.translatable("ezactions.gui.item_equip.slot." + slot.key());
    }
}




