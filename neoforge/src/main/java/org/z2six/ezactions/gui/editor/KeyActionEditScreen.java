package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.helper.InputInjector;

import java.util.function.BiConsumer;

public final class KeyActionEditScreen extends Screen {

    @FunctionalInterface
    public interface SaveHandler extends BiConsumer<MenuItem, MenuItem> {}

    private static final int MAX_LEN_TITLE = 128;
    private static final int MAX_LEN_NOTE = 512;
    private static final int MAX_LEN_MAPPING = 2048;

    private final Screen parent;
    private final MenuItem editing;
    private final SaveHandler onSave;

    private String draftTitle = "";
    private String draftNote = "";
    private String draftMapping = "";
    private boolean draftToggle = false;
    private InputInjector.DeliveryMode draftMode = InputInjector.DeliveryMode.AUTO;
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    private EditBox titleBox;
    private EditBox noteBox;
    private EditBox mappingBox;
    private EditorCycleButton<InputInjector.DeliveryMode> modeCycle;
    private EditorCycleButton<Boolean> toggleCycle;
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

    public KeyActionEditScreen(Screen parent, MenuItem editing) {
        this(parent, editing, null);
    }

    public KeyActionEditScreen(Screen parent, MenuItem editing, SaveHandler onSave) {
        super(Component.translatable(editing == null
                ? "ezactions.gui.key_action.title.add"
                : "ezactions.gui.key_action.title.edit"));
        this.parent = parent;
        this.editing = editing;
        this.onSave = onSave;

        if (editing != null && editing.action() instanceof ClickActionKey ck) {
            this.draftTitle = safe(editing.title());
            try {
                this.draftNote = safe(editing.note());
            } catch (Throwable ignored) {}
            this.draftMapping = safe(ck.mappingName());
            this.draftToggle = ck.toggle();
            this.draftMode = ck.mode();
            if (editing.icon() != null) {
                this.draftIcon = editing.icon();
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void init() {
        this.panel = ActionEditorUi.panel(this.width, this.height, 760, 420, 10);
        this.scroll.reset();

        bodyX = panel.x() + 14;
        bodyY = panel.y() + 34;
        bodyW = panel.w() - 28;
        bodyH = panel.h() - 42;

        int iconAreaW = 58;
        int fieldW = Math.max(200, bodyW - iconAreaW - 14);
        int fieldX = bodyX;

        iconX = fieldX + fieldW + 18;
        iconBaseY = bodyY + 26;

        int y = bodyY + 18;

        titleBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.title"));
        titleBox.setMaxLength(MAX_LEN_TITLE);
        titleBox.setHint(Component.translatable("ezactions.gui.key_action.hint.title"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        scroll.track(addRenderableWidget(titleBox));
        y += 30;

        noteBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.note"));
        noteBox.setMaxLength(MAX_LEN_NOTE);
        noteBox.setHint(Component.translatable("ezactions.gui.hint.note_optional"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        scroll.track(addRenderableWidget(noteBox));
        y += 30;

        mappingBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.mapping_name"));
        mappingBox.setMaxLength(MAX_LEN_MAPPING);
        mappingBox.setHint(Component.translatable("ezactions.gui.key_action.hint.mapping"));
        mappingBox.setValue(draftMapping);
        wireMappingResponder();
        scroll.track(addRenderableWidget(mappingBox));
        y += 30;

        scroll.track(addRenderableWidget(ActionEditorUi.button(fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.key_action.pick_keybind"), () -> {
            try {
                this.minecraft.setScreen(new KeybindPickerScreen(this, mapping -> {
                    try {
                        applyPickedMapping(mapping);
                    } catch (Throwable t) {
                        Constants.LOG.warn("[{}] KeyActionEdit: applyPickedMapping failed: {}", Constants.MOD_NAME, t.toString());
                    }
                    this.minecraft.setScreen(this);
                }));
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeyActionEdit: opening KeybindPickerScreen failed: {}", Constants.MOD_NAME, t.toString());
            }
        })));
        y += 30;

        modeCycle = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                fieldX,
                y,
                (fieldW / 2) - 3,
                20,
                Component.translatable("ezactions.gui.key_action.delivery"),
                draftMode,
                dm -> Component.translatable("ezactions.gui.common.delivery_mode." + (dm == null ? "auto" : dm.name().toLowerCase(java.util.Locale.ROOT))),
                dm -> draftMode = dm == null ? InputInjector.DeliveryMode.AUTO : dm,
                InputInjector.DeliveryMode.AUTO,
                InputInjector.DeliveryMode.INPUT,
                InputInjector.DeliveryMode.TICK
        )));

        toggleCycle = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                fieldX + (fieldW / 2) + 3,
                y,
                (fieldW / 2) - 3,
                20,
                Component.translatable("ezactions.gui.key_action.toggle"),
                draftToggle,
                v -> Component.translatable(Boolean.TRUE.equals(v) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                v -> draftToggle = Boolean.TRUE.equals(v),
                Boolean.TRUE,
                Boolean.FALSE
        )));
        y += 34;

        int buttonY = y;
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

    private void wireMappingResponder() {
        if (mappingBox == null) {
            return;
        }
        mappingBox.setResponder(s -> draftMapping = safe(s));
    }

    private void applyPickedMapping(String mapping) {
        if (mapping == null) {
            return;
        }
        String m = safe(mapping).trim();
        if (m.isEmpty()) {
            return;
        }

        this.draftMapping = m;

        if (mappingBox != null) {
            try {
                int need = Math.max(MAX_LEN_MAPPING, m.length());
                mappingBox.setResponder(s -> {});
                mappingBox.setMaxLength(need);
                mappingBox.setValue(m);
                wireMappingResponder();
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeyActionEdit: failed applying picked mapping to box: {}", Constants.MOD_NAME, t.toString());
                try {
                    wireMappingResponder();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static String chooseBestMapping(String fromBox, String fromDraft) {
        String a = safe(fromBox).trim();
        String b = safe(fromDraft).trim();

        if (a.isEmpty() && b.isEmpty()) {
            return "";
        }
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        if (a.equals(b)) {
            return a;
        }
        if (b.length() > a.length()) {
            return b;
        }
        if (a.length() > b.length()) {
            return a;
        }
        return a;
    }

    private void onSavePressed() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote = safe(noteBox == null ? draftNote : noteBox.getValue()).trim();

            String boxMapping = safe(mappingBox == null ? "" : mappingBox.getValue());
            draftMapping = chooseBestMapping(boxMapping, draftMapping);

            draftMode = modeCycle == null ? draftMode : modeCycle.getValue();
            draftToggle = toggleCycle != null && Boolean.TRUE.equals(toggleCycle.getValue());

            if (draftTitle.isEmpty() || draftMapping.isEmpty()) {
                Constants.LOG.warn("[{}] KeyActionEdit: Title or Mapping empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            MenuItem newItem = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("key"),
                    draftTitle,
                    draftNote,
                    draftIcon,
                    new ClickActionKey(draftMapping, draftToggle, draftMode),
                    java.util.List.of()
            );

            if (onSave != null) {
                onSave.accept(newItem, editing);
                this.minecraft.setScreen(parent);
                return;
            }

            boolean ok = (editing == null)
                    ? RadialMenu.addToCurrent(newItem)
                    : RadialMenu.replaceInCurrent(editing.id(), newItem);

            if (!ok) {
                Constants.LOG.info("[{}] Page full or replace failed for '{}'.", Constants.MOD_NAME, newItem.title());
            }

            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeyActionEdit onSave failed: {}", Constants.MOD_NAME, t.toString());
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
        if (mappingBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.mapping_name"), mappingBox.getX(), mappingBox.getY() - 10);
        }

        ActionEditorUi.drawIconCard(g, this.font, iconX, scroll.y(iconBaseY), 32, Component.translatable("ezactions.gui.field.icon"), this.draftIcon, iconHit(mouseX, mouseY));
        scroll.drawScrollbar(g, bodyX, bodyY, bodyW, bodyH);

        if (mappingBox != null) {
            int mbx = mappingBox.getX();
            int mby = mappingBox.getY();
            int mbw = mappingBox.getWidth();
            int mbh = mappingBox.getHeight();
            boolean over = mouseX >= mbx && mouseX < mbx + mbw && mouseY >= mby && mouseY < mby + mbh;
            if (over) {
                String val = safe(mappingBox.getValue()).trim();
                if (!val.isEmpty() && this.font.width(val) > Math.max(1, mbw - 8)) {
                    g.renderTooltip(this.font, Component.literal(val), mouseX, mouseY);
                }
            }
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
        try {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = ic == null ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeyActionEdit: opening IconPickerScreen failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private boolean iconHit(double mouseX, double mouseY) {
        int y = scroll.y(iconBaseY);
        return mouseX >= iconX && mouseX <= iconX + 32 && mouseY >= y && mouseY <= y + 32;
    }
}
