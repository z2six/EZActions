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

/**
 * Editor for ClickActionKey items + icon chooser + keybind picker.
 * - Preserves typed values while choosing icons/keybinds (kept in draft fields).
 * - Uses crisp background (no vanilla blur).
 */
public final class KeyActionEditScreen extends Screen {

    /** Callback: (newItem, editingOrNull) -> void */
    @FunctionalInterface
    public interface SaveHandler extends BiConsumer<MenuItem, MenuItem> { }

    // Construction
    private final Screen parent;
    private final MenuItem editing;        // null => creating new
    private final SaveHandler onSave;      // if null we fall back to RadialMenu add/replace

    // Draft state (survives child pickers)
    private String draftTitle = "";
    private String draftMapping = "";
    private boolean draftToggle = false;
    private InputInjector.DeliveryMode draftMode = InputInjector.DeliveryMode.AUTO;
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    // Widgets
    private EditBox titleBox;
    private EditBox mappingBox;
    private CycleButton<InputInjector.DeliveryMode> modeCycle;
    private CycleButton<Boolean> toggleCycle;

    // --- Constructors --------------------------------------------------------

    public KeyActionEditScreen(Screen parent, MenuItem editing) {
        this(parent, editing, null);
    }

    public KeyActionEditScreen(Screen parent, MenuItem editing, SaveHandler onSave) {
        super(Component.literal(editing == null ? "Add Key Action" : "Edit Key Action"));
        this.parent = parent;
        this.editing = editing;
        this.onSave = onSave;

        // Seed drafts from existing item, if any
        if (editing != null && editing.action() instanceof ClickActionKey ck) {
            this.draftTitle   = safe(editing.title());
            this.draftMapping = safe(ck.mappingName());
            this.draftToggle  = ck.toggle();
            this.draftMode    = ck.mode();
            IconSpec ic = editing.icon();
            if (ic != null) this.draftIcon = ic;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // --- Screen lifecycle ----------------------------------------------------

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 40;

        titleBox = new EditBox(this.font, cx - 120, y, 240, 20, Component.literal("Title"));
        titleBox.setValue(draftTitle);
        titleBox.setHint(Component.literal("Title (e.g., Inventory)"));
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);
        y += 28;

        mappingBox = new EditBox(this.font, cx - 120, y, 240, 20, Component.literal("Mapping Name"));
        mappingBox.setValue(draftMapping);
        mappingBox.setHint(Component.literal("KeyMapping id (e.g., key.inventory)"));
        mappingBox.setResponder(s -> draftMapping = safe(s));
        addRenderableWidget(mappingBox);
        y += 28;

        // Keybind picker
        addRenderableWidget(Button.builder(Component.literal("Pick from Keybinds…"), b -> {
            this.minecraft.setScreen(new KeybindPickerScreen(this, mapping -> {
                if (mapping != null && !mapping.isEmpty()) {
                    draftMapping = mapping;
                    if (mappingBox != null) mappingBox.setValue(mapping);
                }
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 120, y, 240, 20).build());
        y += 28;

        // Icon picker
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = ic == null ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 120, y, 240, 20).build());
        y += 28;

        // Delivery + Toggle
        modeCycle = addRenderableWidget(
                CycleButton.builder((InputInjector.DeliveryMode dm) -> Component.literal(dm.name()))
                        .withValues(InputInjector.DeliveryMode.AUTO, InputInjector.DeliveryMode.INPUT, InputInjector.DeliveryMode.TICK)
                        .withInitialValue(draftMode)
                        .create(cx - 120, y, 116, 20, Component.literal("Delivery"))
        );
        toggleCycle = addRenderableWidget(
                CycleButton.onOffBuilder(draftToggle)
                        .create(cx + 4, y, 116, 20, Component.literal("Toggle"))
        );
        y += 28;

        // Save / Cancel / Back
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(cx - 120, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 36, y, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 48, y, 80, 20).build());
    }

    private void onSavePressed() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftMapping = safe(mappingBox == null ? draftMapping : mappingBox.getValue()).trim();
            draftMode = modeCycle == null ? draftMode : modeCycle.getValue();
            draftToggle = toggleCycle != null && Boolean.TRUE.equals(toggleCycle.getValue());

            if (draftTitle.isEmpty() || draftMapping.isEmpty()) {
                Constants.LOG.warn("[{}] KeyActionEdit: Title or Mapping empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            MenuItem newItem = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("key"),
                    draftTitle,
                    draftIcon,
                    new ClickActionKey(draftMapping, draftToggle, draftMode),
                    java.util.List.of()
            );

            if (onSave != null) {
                onSave.accept(newItem, editing);
                this.minecraft.setScreen(parent);
                return;
            }

            // Fallback (normally MenuEditorScreen passes onSave)
            boolean ok;
            if (editing == null) ok = RadialMenu.addToCurrent(newItem);
            else                 ok = RadialMenu.replaceInCurrent(editing.id(), newItem);
            if (!ok) {
                Constants.LOG.info("[{}] Page full or replace failed for '{}'.", Constants.MOD_NAME, newItem.title());
            }
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeyActionEdit onSave failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- Render --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Custom, crisp background (no vanilla blur)
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(12, 52, this.width - 12, this.height - 36, 0xC0101010);

        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 12, 0xFFFFFF);
        g.drawString(this.font, "Title:", this.width / 2 - 120, 28, 0xA0A0A0);
        g.drawString(this.font, "Mapping Name:", this.width / 2 - 120, 56, 0xA0A0A0);

        // Small live preview of the chosen icon (right side)
        try {
            IconRenderer.drawIcon(g, this.width - 28, 28, this.draftIcon);
        } catch (Throwable ignored) {}

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
