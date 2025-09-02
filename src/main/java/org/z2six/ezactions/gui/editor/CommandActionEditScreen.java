// MainFile: src/main/java/org/z2six/ezactions/gui/editor/CommandActionEditScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionCommand;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Editor for "run command" items.
 * - Preserves typed title/command while opening child pickers
 * - Shows live icon preview
 * - Persists to current level on Save
 */
public final class CommandActionEditScreen extends Screen {

    private final Screen parent;
    private final MenuItem editing; // null => add new

    // Draft state that survives pickers
    private String draftTitle = "";
    private String draftCommand = "/say hi";
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    // Widgets
    private EditBox titleBox;
    private EditBox cmdBox;

    public CommandActionEditScreen(Screen parent, MenuItem editing) {
        super(Component.literal(editing == null ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null && editing.action() instanceof ClickActionCommand cc) {
            this.draftTitle = safe(editing.title());

            // Prefer direct API; fallback to reflection for older builds
            String extracted = "";
            try { extracted = cc.getCommand(); } catch (Throwable ignored) {}
            if (extracted == null || extracted.isEmpty()) {
                extracted = tryExtractCommandString(cc);
            }
            if (!extracted.isEmpty()) this.draftCommand = extracted;

            if (editing.icon() != null) this.draftIcon = editing.icon();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /** Attempts to read the command string from a ClickActionCommand (legacy reflection fallback). */
    private static String tryExtractCommandString(ClickActionCommand cc) {
        // 1) Methods
        String[] methodNames = { "command", "getCommand", "getCmd", "cmd" };
        for (String mname : methodNames) {
            try {
                Method m = cc.getClass().getMethod(mname);
                Object v = m.invoke(cc);
                if (v instanceof String s && !s.isEmpty()) return s;
            } catch (Throwable ignored) {}
        }
        // 2) Fields
        String[] fieldNames = { "command", "cmd", "commandRaw" };
        for (String fname : fieldNames) {
            try {
                Field f = cc.getClass().getDeclaredField(fname);
                f.setAccessible(true);
                Object v = f.get(cc);
                if (v instanceof String s && !s.isEmpty()) return s;
            } catch (Throwable ignored) {}
        }
        return "";
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // Title input
        int titleY = 48;
        titleBox = new EditBox(this.font, cx - 160, titleY, 320, 20, Component.literal("Title"));
        titleBox.setHint(Component.literal("Title (e.g., Say Time)"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);

        // Place Command input LOWER so the "Command:" label (drawn above it) has extra breathing room
        final int LABEL_ABOVE = 14;   // pixels label sits above its text box
        final int EXTRA_GAP   = 8;    // extra space between Title box bottom and Command label
        int cmdY = titleBox.getY() + titleBox.getHeight() + EXTRA_GAP + LABEL_ABOVE;

        cmdBox = new EditBox(this.font, cx - 160, cmdY, 320, 20, Component.literal("Command"));
        cmdBox.setHint(Component.literal("Command (e.g., /time query)"));
        cmdBox.setValue(draftCommand);
        cmdBox.setResponder(s -> draftCommand = safe(s));
        addRenderableWidget(cmdBox);

        int y = cmdY + 28;

        // Icon picker
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = (ic == null) ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 160, y, 320, 20).build());
        y += 28;

        // Save / Cancel / Back
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(cx - 160, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 52, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 56, y, 100, 20).build());
    }

    private void onSavePressed() {
        try {
            draftTitle   = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftCommand = safe(cmdBox == null ? draftCommand : cmdBox.getValue()).trim();

            if (draftTitle.isEmpty() || draftCommand.isEmpty()) {
                Constants.LOG.warn("[{}] CommandEdit: Title or Command empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            MenuItem item = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("cmd"),
                    draftTitle,
                    draftIcon,
                    new ClickActionCommand(draftCommand),
                    java.util.List.of()
            );

            boolean ok = (editing == null)
                    ? RadialMenu.addToCurrent(item)
                    : RadialMenu.replaceInCurrent(editing.id(), item);

            if (!ok) {
                Constants.LOG.info("[{}] Command save failed for '{}'.", Constants.MOD_NAME, draftTitle);
            }

            if (parent instanceof MenuEditorScreen m) {
                m.refreshFromChild();
            }
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] CommandEdit onSave failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Background
        g.fill(0, 0, this.width, this.height, 0x88000000);
        g.fill(12, 52, this.width - 12, this.height - 36, 0xC0101010);

        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 14, 0xFFFFFF);

        // Labels drawn directly above their boxes
        int titleLabelX = this.width / 2 - 160;
        int cmdLabelX   = titleLabelX;

        int titleLabelY = (titleBox != null ? titleBox.getY() : 48) - 14;
        int cmdLabelY   = (cmdBox   != null ? cmdBox.getY()   : 76) - 14;

        g.drawString(this.font, "Title:",   titleLabelX, titleLabelY, 0xA0A0A0);
        g.drawString(this.font, "Command:", cmdLabelX,   cmdLabelY,   0xA0A0A0);

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
