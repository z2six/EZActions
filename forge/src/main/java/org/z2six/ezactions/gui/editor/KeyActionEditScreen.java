// MainFile: src/main/java/org/z2six/ezactions/gui/editor/KeyActionEditScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.helper.InputInjector;

import java.util.function.BiConsumer;

/*
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/KeyActionEditScreen.java
 *
 * Spacing rule per field row:
 *   Label
 *   10 px
 *   Field
 *   5 px
 *   (next Label)
 *
 * Buttons:
 *   - First button row starts 10 px after the element above it.
 *   - Each successive button row has 5 px vertical gap from the previous button row.
 *
 * Labels are drawn in render(); init() places widgets only.
 */
public final class KeyActionEditScreen extends Screen {

    /* Callback: (newItem, editingOrNull) -> void */
    @FunctionalInterface
    public interface SaveHandler extends BiConsumer<MenuItem, MenuItem> { }

    // Field sizes
    private static final int FIELD_W = 240;
    private static final int FIELD_H = 20;

    // Spacing rules (fields)
    private static final int LABEL_TO_FIELD = 10;         // label -> field
    private static final int FIELD_TO_NEXT_LABEL = 5;     // field -> next label

    // Spacing rules (buttons)
    private static final int FIRST_BUTTON_ROW_OFFSET = 10; // after last field to first button row
    private static final int BETWEEN_BUTTON_ROWS = 5;      // between successive button rows

    // Construction
    private final Screen parent;
    private final MenuItem editing;        // null => creating new
    private final SaveHandler onSave;      // optional override

    // Draft state (survives child pickers)
    private String draftTitle = "";
    private String draftNote = "";
    private String draftMapping = "";
    private boolean draftToggle = false;
    private InputInjector.DeliveryMode draftMode = InputInjector.DeliveryMode.AUTO;
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    // Widgets
    private EditBox titleBox;
    private EditBox noteBox;
    private EditBox mappingBox;
    private CycleButton<InputInjector.DeliveryMode> modeCycle;
    private CycleButton<Boolean> toggleCycle;

    public KeyActionEditScreen(Screen parent, MenuItem editing) { this(parent, editing, null); }

    public KeyActionEditScreen(Screen parent, MenuItem editing, SaveHandler onSave) {
        super(Component.literal(editing == null ? "Add Key Action" : "Edit Key Action"));
        this.parent = parent;
        this.editing = editing;
        this.onSave = onSave;

        if (editing != null && editing.action() instanceof ClickActionKey ck) {
            this.draftTitle   = safe(editing.title());
            try { this.draftNote = safe(editing.note()); } catch (Throwable ignored) {}
            this.draftMapping = safe(ck.mappingName());
            this.draftToggle  = ck.toggle();
            this.draftMode    = ck.mode();
            IconSpec ic = editing.icon();
            if (ic != null) this.draftIcon = ic;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // Place fields; labels are drawn in render() at (fieldY - LABEL_TO_FIELD)
        int y = 52;

        // Title field
        titleBox = new EditBox(this.font, cx - (FIELD_W / 2), y, FIELD_W, FIELD_H, Component.literal("Title"));
        titleBox.setValue(draftTitle);
        titleBox.setHint(Component.literal("Title (e.g., Inventory)"));
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);
        y += FIELD_H + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Note field
        noteBox = new EditBox(this.font, cx - (FIELD_W / 2), y, FIELD_W, FIELD_H, Component.literal("Note"));
        noteBox.setValue(draftNote);
        noteBox.setHint(Component.literal("Optional note (tooltip in editor)"));
        noteBox.setResponder(s -> draftNote = safe(s));
        addRenderableWidget(noteBox);
        y += FIELD_H + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Mapping field
        mappingBox = new EditBox(this.font, cx - (FIELD_W / 2), y, FIELD_W, FIELD_H, Component.literal("Mapping Name"));
        mappingBox.setValue(draftMapping);
        mappingBox.setHint(Component.literal("KeyMapping id (e.g., key.inventory)"));
        mappingBox.setResponder(s -> draftMapping = safe(s));
        addRenderableWidget(mappingBox);
        y += FIELD_H;

        // --- Button rows stack ---
        // 1) First button row starts 10px after last field
        y += FIRST_BUTTON_ROW_OFFSET;

        // Row 1: keybind picker button
        addRenderableWidget(Button.builder(Component.literal("Pick from Keybinds…"), b -> {
            try {
                this.minecraft.setScreen(new KeybindPickerScreen(this, mapping -> {
                    if (mapping != null && !mapping.isEmpty()) {
                        draftMapping = mapping;
                        if (mappingBox != null) mappingBox.setValue(mapping);
                    }
                    this.minecraft.setScreen(this);
                }));
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeyActionEdit: opening KeybindPickerScreen failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(cx - (FIELD_W / 2), y, FIELD_W, FIELD_H).build());
        y += FIELD_H + BETWEEN_BUTTON_ROWS;

        // Row 2: icon picker button
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            try {
                this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                    draftIcon = ic == null ? IconSpec.item("minecraft:stone") : ic;
                    this.minecraft.setScreen(this);
                }));
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeyActionEdit: opening IconPickerScreen failed: {}", Constants.MOD_NAME, t.toString());
            }
        }).bounds(cx - (FIELD_W / 2), y, FIELD_W, FIELD_H).build());
        y += FIELD_H + BETWEEN_BUTTON_ROWS;

        // Row 3: delivery + toggle on one row (same row height as field)
        modeCycle = addRenderableWidget(
                CycleButton.builder((InputInjector.DeliveryMode dm) -> Component.literal(dm.name()))
                        .withValues(InputInjector.DeliveryMode.AUTO, InputInjector.DeliveryMode.INPUT, InputInjector.DeliveryMode.TICK)
                        .withInitialValue(draftMode)
                        .create(cx - (FIELD_W / 2), y, (FIELD_W / 2) - 4, FIELD_H, Component.literal("Delivery"))
        );
        toggleCycle = addRenderableWidget(
                CycleButton.onOffBuilder(draftToggle)
                        .create(cx + 4, y, (FIELD_W / 2) - 4, FIELD_H, Component.literal("Toggle"))
        );
        y += FIELD_H + BETWEEN_BUTTON_ROWS;

        // Row 4: Save / Cancel / Back
        int totalW = (80 * 3) + (8 * 2);
        int leftX = cx - (totalW / 2);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(leftX, y, 80, FIELD_H).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(leftX + 80 + 8, y, 80, FIELD_H).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(leftX + 2 * (80 + 8), y, 80, FIELD_H).build());
        // --- end button stack ---
    }

    private void onSavePressed() {
        try {
            draftTitle   = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote    = safe(noteBox  == null ? draftNote  : noteBox.getValue()).trim();
            draftMapping = safe(mappingBox == null ? draftMapping : mappingBox.getValue()).trim();
            draftMode = modeCycle == null ? draftMode : modeCycle.getValue();
            draftToggle = toggleCycle != null && Boolean.TRUE.equals(toggleCycle.getValue());

            if (draftTitle.isEmpty() || draftMapping.isEmpty()) {
                Constants.LOG.warn("[{}] KeyActionEdit: Title or Mapping empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            // Assumes MenuItem supports a 'note' field
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
        // Background
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(12, 52, this.width - 12, this.height - 36, 0xC0101010);

        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 20, 0xFFFFFF);

        // Labels: draw at (fieldY - LABEL_TO_FIELD)
        if (titleBox != null) {
            g.drawString(this.font, "Title:", titleBox.getX(), titleBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (noteBox != null) {
            g.drawString(this.font, "Note:", noteBox.getX(), noteBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (mappingBox != null) {
            g.drawString(this.font, "Mapping Name:", mappingBox.getX(), mappingBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }

        // Icon preview (top-right)
        try {
            IconRenderer.drawIcon(g, this.width - 28, 28, this.draftIcon);
        } catch (Throwable ignored) {}

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { this.minecraft.setScreen(parent); }
}
