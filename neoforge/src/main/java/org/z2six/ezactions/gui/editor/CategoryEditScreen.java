package org.z2six.ezactions.gui.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.ArrayList;
import java.util.List;

public final class CategoryEditScreen extends Screen {

    private final Screen parent;
    private final MenuItem editing;

    private String draftTitle = "";
    private String draftNote = "";
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");
    private boolean draftHideFromMainRadial = false;
    private boolean draftEnableKeybind = false;

    private EditBox titleBox;
    private EditBox noteBox;
    private Checkbox hideFromMainCheckbox;
    private Checkbox enableKeybindCheckbox;

    private boolean showRestartHint = false;
    private final ActionEditorUi.ScrollArea scroll = new ActionEditorUi.ScrollArea();

    private ActionEditorUi.Panel panel;
    private int bodyX;
    private int bodyY;
    private int bodyW;
    private int bodyH;
    private int cardBaseY;
    private int cardBaseH;
    private int iconX;
    private int iconBaseY;

    public CategoryEditScreen(Screen parent, MenuItem editing) {
        super(Component.translatable(editing == null
                ? "ezactions.gui.bundle.title.add"
                : "ezactions.gui.bundle.title.edit"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null) {
            this.draftTitle = safe(editing.title());
            try {
                this.draftNote = safe(editing.note());
            } catch (Throwable ignored) {}
            if (editing.icon() != null) {
                this.draftIcon = editing.icon();
            }
            try {
                this.draftHideFromMainRadial = editing.hideFromMainRadial();
                this.draftEnableKeybind = editing.bundleKeybindEnabled();
            } catch (Throwable ignored) {}
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void init() {
        this.panel = ActionEditorUi.panel(this.width, this.height, 720, 420, 10);
        this.scroll.reset();

        bodyX = panel.x() + 14;
        bodyY = panel.y() + 34;
        bodyW = panel.w() - 28;
        bodyH = panel.h() - 42;

        int iconAreaW = 58;
        int fieldW = Math.max(220, bodyW - iconAreaW - 14);
        int fieldX = bodyX;

        iconX = fieldX + fieldW + 18;
        iconBaseY = bodyY + 26;

        int y = bodyY + 18;

        titleBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.title"));
        titleBox.setHint(Component.translatable("ezactions.gui.bundle.hint.title"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        scroll.track(addRenderableWidget(titleBox));
        y += 30;

        noteBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.note"));
        noteBox.setHint(Component.translatable("ezactions.gui.hint.note_optional"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        scroll.track(addRenderableWidget(noteBox));

        int checksY = y + 42;
        hideFromMainCheckbox = Checkbox.builder(Component.translatable("ezactions.gui.bundle.hide_from_main"), this.font)
                .pos(bodyX + 12, checksY)
                .selected(draftHideFromMainRadial)
                .onValueChange((cb, value) -> draftHideFromMainRadial = value)
                .maxWidth(bodyW - 24)
                .build();
        scroll.track(addRenderableWidget(hideFromMainCheckbox));

        enableKeybindCheckbox = Checkbox.builder(Component.translatable("ezactions.gui.bundle.enable_keybind"), this.font)
                .pos(bodyX + 12, checksY + 24)
                .selected(draftEnableKeybind)
                .onValueChange((cb, value) -> {
                    draftEnableKeybind = value;
                    showRestartHint = true;
                })
                .maxWidth(bodyW - 24)
                .build();
        scroll.track(addRenderableWidget(enableKeybindCheckbox));

        int buttonY = checksY + 52;
        int totalW = (96 * 3) + (8 * 2);
        int left = panel.x() + (panel.w() - totalW) / 2;
        cardBaseY = bodyY;
        cardBaseH = (buttonY - bodyY) + 34;

        scroll.track(addRenderableWidget(ActionEditorUi.button(left, buttonY, 96, 20, Component.translatable("ezactions.gui.common.save"), this::onSavePressed)));
        scroll.track(addRenderableWidget(ActionEditorUi.button(left + 104, buttonY, 96, 20, Component.translatable("ezactions.gui.common.cancel"), this::onClose)));
        scroll.track(addRenderableWidget(ActionEditorUi.button(left + 208, buttonY, 96, 20, Component.translatable("ezactions.gui.common.back"), () -> this.minecraft.setScreen(parent))));

        scroll.include(bodyY, bodyY + cardBaseH);
        scroll.layout(bodyY, bodyY + bodyH);
    }

    private void onSavePressed() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote = safe(noteBox == null ? draftNote : noteBox.getValue()).trim();

            if (draftTitle.isEmpty()) {
                Constants.LOG.warn("[{}] CategoryEdit: Title empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            String newId = draftTitle;
            if (RadialMenu.isBundleNameTaken(newId, editing)) {
                Constants.LOG.warn("[{}] CategoryEdit: Duplicate bundle title/id '{}' detected; save aborted.",
                        Constants.MOD_NAME, newId);
                try {
                    if (titleBox != null) {
                        titleBox.setTextColor(0xFFFF5555);
                    }
                } catch (Throwable ignored) {}
                return;
            }

            List<MenuItem> children = new ArrayList<>();
            if (editing != null) {
                try {
                    children.addAll(editing.childrenMutable());
                } catch (Throwable ignored) {}
            }

            MenuItem newItem = new MenuItem(
                    newId,
                    draftTitle,
                    draftNote,
                    draftIcon,
                    null,
                    children,
                    draftHideFromMainRadial,
                    draftEnableKeybind
            );

            boolean ok = (editing == null)
                    ? RadialMenu.addToCurrent(newItem)
                    : RadialMenu.replaceInCurrent(editing.id(), newItem);

            if (!ok) {
                Constants.LOG.info("[{}] Category save failed (page full or replace failed) '{}'.", Constants.MOD_NAME, draftTitle);
            }

            if (parent instanceof MenuEditorScreen m) {
                m.refreshFromChild();
            }
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CategoryEdit onSave failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        ActionEditorUi.drawFrame(g, this.font, this.width, this.height, panel, this.title);
        scroll.layout(bodyY, bodyY + bodyH);
        ActionEditorUi.drawCard(g, this.font, bodyX, scroll.y(cardBaseY), bodyW, cardBaseH, Component.empty());

        if (titleBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.title"), titleBox.getX(), titleBox.getY() - 10);
        }
        if (noteBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.note"), noteBox.getX(), noteBox.getY() - 10);
        }

        ActionEditorUi.drawIconCard(g, this.font, iconX, scroll.y(iconBaseY), 32, Component.translatable("ezactions.gui.field.icon"), draftIcon, iconHit(mouseX, mouseY));
        scroll.drawScrollbar(g, bodyX, bodyY, bodyW, bodyH);

        if (showRestartHint && enableKeybindCheckbox != null) {
            int hx = enableKeybindCheckbox.getX();
            int hy = enableKeybindCheckbox.getY() + 22;
            Component msg = Component.translatable("ezactions.message.restart_required_short").withStyle(ChatFormatting.RED);
            g.drawString(this.font, msg.getString(), hx, hy, 0xFFFF6666);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && iconHit(mouseX, mouseY)) {
            openIconPicker();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scroll.mouseScrolled(mouseX, mouseY, deltaY, bodyX, bodyY, bodyW, bodyH)) {
            scroll.layout(bodyY, bodyY + bodyH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
}
