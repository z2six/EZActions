// MainFile: src/main/java/org/z2six/ezactions/gui/editor/CategoryEditScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

/**
 * Editor for Category items.
 * - Preserves typed title while opening child pickers
 * - Shows live icon preview
 * - Persists to current level (root or category) on Save
 */
public final class CategoryEditScreen extends Screen {

    private final Screen parent;
    private final MenuItem editing; // null => add new

    // Draft state survives child pickers
    private String draftTitle = "";
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    // Widgets
    private EditBox titleBox;

    public CategoryEditScreen(Screen parent, MenuItem editing) {
        super(Component.literal(editing == null ? "Add Bundle" : "Edit Bundle"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null) {
            this.draftTitle = safe(editing.title());
            if (editing.icon() != null) this.draftIcon = editing.icon();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 48;

        titleBox = new EditBox(this.font, cx - 140, y, 280, 20, Component.literal("Title"));
        titleBox.setHint(Component.literal("Bundle title (e.g., Utilities)"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);
        y += 28;

        // Icon picker
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = (ic == null) ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 140, y, 280, 20).build());
        y += 28;

        // Save / Cancel
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(cx - 140, y, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 44, y, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 52, y, 90, 20).build());
    }

    private void onSavePressed() {
        try {
            draftTitle = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            if (draftTitle.isEmpty()) {
                Constants.LOG.warn("[{}] CategoryEdit: Title empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            // Preserve children if editing an existing category
            List<MenuItem> children = new ArrayList<>();
            if (editing != null) {
                try {
                    // Prefer childrenMutable() if available
                    children.addAll(editing.childrenMutable());
                } catch (Throwable t) {
                    // Fallback: if immutable or not exposed, we simply keep empty (rare)
                }
            }

            MenuItem newItem = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("cat"),
                    draftTitle,
                    draftIcon,
                    null, // action == null => category
                    children
            );

            boolean ok;
            if (editing == null) ok = RadialMenu.addToCurrent(newItem);
            else                 ok = RadialMenu.replaceInCurrent(editing.id(), newItem);

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
        g.drawString(this.font, "Title:", this.width / 2 - 140, 34, 0xA0A0A0);

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

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
