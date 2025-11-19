// MainFile: src/main/java/org/z2six/ezactions/gui/editor/CategoryEditScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;

import java.util.ArrayList;
import java.util.List;

/*
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/CategoryEditScreen.java
 *
 * Editor for Category (Bundle) items, now with Note support and bundle flags:
 *  - Hide from main radial
 *  - Enable keybind (register bundle key on next restart)
 *
 * Field spacing per row:
 *   Label
 *   10 px
 *   Field
 *   5 px
 *   (next Label)
 *
 * Buttons:
 *   - First button row starts 10 px after the element above it.
 *   - Each successive button row has 5 px vertical gap.
 *
 * Labels are drawn in render(); init() places widgets only.
 */
public final class CategoryEditScreen extends Screen {

    // Spacing rules
    private static final int LABEL_TO_FIELD = 10;         // label -> field
    private static final int FIELD_TO_NEXT_LABEL = 5;     // field -> next label

    // Button spacing
    private static final int FIRST_BUTTON_ROW_OFFSET = 10; // after last field to first button row
    private static final int BETWEEN_BUTTON_ROWS = 5;      // between successive button rows

    private final Screen parent;
    private final MenuItem editing; // null => add new

    // Draft state survives child pickers
    private String draftTitle = "";
    private String draftNote  = "";
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");
    private boolean draftHideFromMainRadial = false;
    private boolean draftEnableKeybind = false;

    // Widgets
    private EditBox titleBox;
    private EditBox noteBox;
    private Checkbox hideFromMainCheckbox;
    private Checkbox enableKeybindCheckbox;

    // "Applied on next restart" hint (only when toggling Enable keybind)
    private boolean showRestartHint = false;

    public CategoryEditScreen(Screen parent, MenuItem editing) {
        super(Component.literal(editing == null ? "Add Bundle" : "Edit Bundle"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null) {
            this.draftTitle = safe(editing.title());
            try { this.draftNote = safe(editing.note()); } catch (Throwable ignored) {}
            if (editing.icon() != null) this.draftIcon = editing.icon();
            try {
                this.draftHideFromMainRadial = editing.hideFromMainRadial();
                this.draftEnableKeybind = editing.bundleKeybindEnabled();
            } catch (Throwable ignored) {}
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // Start at first field Y; labels render at (fieldY - LABEL_TO_FIELD)
        int y = 48;

        // Title
        titleBox = new EditBox(this.font, cx - 140, y, 280, 20, Component.literal("Title"));
        titleBox.setHint(Component.literal("Bundle title (e.g., Utilities)"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);
        y += 20 + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Note
        noteBox = new EditBox(this.font, cx - 140, y, 280, 20, Component.literal("Note"));
        noteBox.setHint(Component.literal("Optional note (shown as tooltip in editor)"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        addRenderableWidget(noteBox);
        y += 20;

        // --- Bundle flags (checkboxes) ---
        y += 6;

        // Forge 1.20.1 does not have Checkbox.builder(...). Use constructor and hook onPress.
        hideFromMainCheckbox = new Checkbox(cx - 140, y, 280, 20,
                Component.literal("Hide from main radial"), draftHideFromMainRadial) {
            @Override
            public void onPress() {
                super.onPress();
                try {
                    draftHideFromMainRadial = this.selected();
                } catch (Throwable ignored) {}
            }
        };
        // Ensure initial draft reflects the widget (in case MC toggled anything internally)
        try { draftHideFromMainRadial = hideFromMainCheckbox.selected(); } catch (Throwable ignored) {}
        addRenderableWidget(hideFromMainCheckbox);
        y += 20 + 4;

        enableKeybindCheckbox = new Checkbox(cx - 140, y, 280, 20,
                Component.literal("Enable keybind"), draftEnableKeybind) {
            @Override
            public void onPress() {
                super.onPress();
                try {
                    draftEnableKeybind = this.selected();
                    showRestartHint = true; // show red "applied on next restart"
                } catch (Throwable ignored) {}
            }
        };
        try { draftEnableKeybind = enableKeybindCheckbox.selected(); } catch (Throwable ignored) {}
        addRenderableWidget(enableKeybindCheckbox);
        y += 20;

        // --- Button rows stack ---
        // First button row offset (10px after element above)
        y += FIRST_BUTTON_ROW_OFFSET;

        // Row 1: Icon picker
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = (ic == null) ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 140, y, 280, 20).build());
        y += 20 + BETWEEN_BUTTON_ROWS;

        // Row 2: Save / Cancel / Back
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(cx - 140, y, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 44, y, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 52, y, 90, 20).build());
        // --- end button stack ---
    }

    private void onSavePressed() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote  = safe(noteBox  == null ? draftNote  : noteBox.getValue()).trim();

            // Re-sync flags from widgets (in case user didn’t click them after opening)
            try {
                if (hideFromMainCheckbox != null) draftHideFromMainRadial = hideFromMainCheckbox.selected();
                if (enableKeybindCheckbox != null) draftEnableKeybind = enableKeybindCheckbox.selected();
            } catch (Throwable ignored) {}

            if (draftTitle.isEmpty()) {
                Constants.LOG.warn("[{}] CategoryEdit: Title empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            // Enforce "bundle id == bundle title" uniqueness across categories.
            String newId = draftTitle;
            if (RadialMenu.isBundleNameTaken(newId, editing)) {
                Constants.LOG.warn("[{}] CategoryEdit: Duplicate bundle title/id '{}' detected; save aborted.",
                        Constants.MOD_NAME, newId);
                try {
                    if (titleBox != null) titleBox.setTextColor(0xFFFF5555);
                } catch (Throwable ignored) {}
                return;
            }

            // Preserve children if editing an existing category
            List<MenuItem> children = new ArrayList<>();
            if (editing != null) {
                try {
                    children.addAll(editing.childrenMutable());
                } catch (Throwable t) {
                    // If immutable/not exposed, leave empty (rare)
                }
            }

            boolean hideFlag = draftHideFromMainRadial;
            boolean keybindFlag = draftEnableKeybind;

            // NOTE: id == title; bundle hotkeys reference this id by name
            MenuItem newItem = new MenuItem(
                    newId,
                    draftTitle,
                    draftNote,
                    draftIcon,
                    null, // action == null => category (bundle)
                    children,
                    hideFlag,
                    keybindFlag
            );

            boolean ok = (editing == null)
                    ? RadialMenu.addToCurrent(newItem)
                    : RadialMenu.replaceInCurrent(editing.id(), newItem);

            if (!ok) {
                Constants.LOG.info("[{}] Category save failed (page full or replace failed) '{}'.", Constants.MOD_NAME, draftTitle);
            }

            if (parent instanceof MenuEditorScreen m) {
                m.refreshFromChild(); // rebuild list when we return
            }
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CategoryEdit onSave failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Background
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(12, 52, this.width - 12, this.height - 36, 0xC0101010);

        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 14, 0xFFFFFF);

        // Labels above their boxes with LABEL_TO_FIELD spacing
        if (titleBox != null) {
            g.drawString(this.font, "Title:", titleBox.getX(), titleBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (noteBox != null) {
            g.drawString(this.font, "Note:", noteBox.getX(), noteBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }

        // Icon preview box (top-right)
        int boxW = 60, boxH = 60;
        int bx = this.width - boxW - 16;
        int by = 16;
        g.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, 0x40FFFFFF); // border
        g.fill(bx, by, bx + boxW, by + boxH, 0x20202020);
        g.drawString(this.font, "Icon", bx + 18, by + 4, 0xA0A0A0);
        try {
            IconRenderer.drawIcon(g, bx + boxW / 2, by + boxH / 2 + 6, draftIcon);
        } catch (Throwable ignored) {}

        // Restart hint (only when Enable keybind was toggled)
        if (showRestartHint && enableKeybindCheckbox != null) {
            int hx = enableKeybindCheckbox.getX();
            int hy = enableKeybindCheckbox.getY() + 22;
            Component msg = Component.literal("Applied on next restart").withStyle(ChatFormatting.RED);
            g.drawString(this.font, msg.getString(), hx, hy, 0xFFFF5555);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { this.minecraft.setScreen(parent); }
}
