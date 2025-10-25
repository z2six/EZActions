// MainFile: src/main/java/org/z2six/ezactions/gui/editor/CommandActionEditScreen.java
package ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.config.GeneralClientConfig;
import org.z2six.ezactions.data.click.ClickActionCommand;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.IconRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * // MainFile: src/main/java/org/z2six/ezactions/gui/editor/CommandActionEditScreen.java
 *
 * Field rows:
 *   Label
 *   10 px
 *   Field (or multiline)
 *   5 px
 *   (next Label)
 *
 * Buttons:
 *   - First button row starts 10 px after the element above it.
 *   - Each successive button row has 5 px vertical gap.
 */
public final class CommandActionEditScreen extends Screen {

    // Field spacing
    private static final int LABEL_TO_FIELD = 10;         // label -> field
    private static final int FIELD_TO_NEXT_LABEL = 5;     // field -> next label

    // Button spacing
    private static final int FIRST_BUTTON_ROW_OFFSET = 10; // after last field to first button row
    private static final int BETWEEN_BUTTON_ROWS = 5;      // between successive button rows

    private final Screen parent;
    private final MenuItem editing; // null => add new

    // Draft state that survives pickers
    private String   draftTitle   = "";
    private String   draftNote    = "";
    private String   draftCommand = "/say hi";
    private int      draftDelayTicks = 0;
    private IconSpec draftIcon    = IconSpec.item("minecraft:stone");

    // Widgets
    private EditBox titleBox;
    private EditBox noteBox;
    private MultiLineEditBox cmdBox;
    private EditBox delayBox;

    public CommandActionEditScreen(Screen parent, MenuItem editing) {
        super(Component.literal(editing == null ? "Add Command" : "Edit Command"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null && editing.action() instanceof ClickActionCommand cc) {
            this.draftTitle = safe(editing.title());
            try { this.draftNote = safe(editing.note()); } catch (Throwable ignored) {}

            String extracted = "";
            try { extracted = cc.getCommand(); } catch (Throwable ignored) {}
            if (extracted == null || extracted.isEmpty()) extracted = tryExtractCommandString(cc);
            if (!extracted.isEmpty()) this.draftCommand = extracted;

            int delay = 0;
            try { delay = cc.getDelayTicks(); } catch (Throwable ignored) {}
            if (delay <= 0) delay = tryExtractDelayTicks(cc);
            this.draftDelayTicks = Math.max(0, delay);

            if (editing.icon() != null) this.draftIcon = editing.icon();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String tryExtractCommandString(ClickActionCommand cc) {
        String[] methodNames = { "command", "getCommand", "getCmd", "cmd" };
        for (String mname : methodNames) {
            try {
                Method m = cc.getClass().getMethod(mname);
                Object v = m.invoke(cc);
                if (v instanceof String s && !s.isEmpty()) return s;
            } catch (Throwable ignored) {}
        }
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

    private static int tryExtractDelayTicks(ClickActionCommand cc) {
        String[] methods = { "getDelayTicks", "delayTicks", "getDelay", "delay" };
        for (String mname : methods) {
            try {
                Method m = cc.getClass().getMethod(mname);
                Object v = m.invoke(cc);
                if (v instanceof Number n) return n.intValue();
            } catch (Throwable ignored) {}
        }
        String[] fields = { "delayTicks", "delay", "ticksDelay" };
        for (String fname : fields) {
            try {
                Field f = cc.getClass().getDeclaredField(fname);
                f.setAccessible(true);
                Object v = f.get(cc);
                if (v instanceof Number n) return n.intValue();
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // Title field
        int y = 48;
        titleBox = new EditBox(this.font, cx - 160, y, 320, 20, Component.literal("Title"));
        titleBox.setHint(Component.literal("Title (e.g., Say Time)"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        addRenderableWidget(titleBox);
        y += 20 + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Note field
        noteBox = new EditBox(this.font, cx - 160, y, 320, 20, Component.literal("Note"));
        noteBox.setHint(Component.literal("Optional note (tooltip in editor)"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        addRenderableWidget(noteBox);
        y += 20 + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Command box
        int cfgLines = 5;
        try { cfgLines = GeneralClientConfig.getCommandEditorVisibleLines(); }
        catch (Throwable t) {
            Constants.LOG.debug("[{}] commandEditorVisibleLines read failed; using default 5: {}", Constants.MOD_NAME, t.toString());
            cfgLines = 5;
        }
        if (cfgLines < 1) cfgLines = 1;
        if (cfgLines > 20) cfgLines = 20;

        final int V_PADDING = 6;
        int cmdH = this.font.lineHeight * cfgLines + V_PADDING;

        cmdBox = new MultiLineEditBox(
                this.font,
                cx - 160, y, 320, cmdH,
                Component.literal("Command"),
                Component.literal(draftCommand)
        );
        cmdBox.setCharacterLimit(32767);
        cmdBox.setValue(draftCommand);
        cmdBox.setValueListener(s -> draftCommand = safe(s));
        addRenderableWidget(cmdBox);
        y += cmdH + FIELD_TO_NEXT_LABEL + LABEL_TO_FIELD;

        // Delay field
        delayBox = new EditBox(this.font, cx - 160, y, 80, 20, Component.literal("Delay (ticks)"));
        delayBox.setValue(this.draftDelayTicks > 0 ? Integer.toString(this.draftDelayTicks) : "");
        delayBox.setResponder(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                draftDelayTicks = Math.max(0, v);
            } catch (Throwable ignored) {}
        });
        addRenderableWidget(delayBox);
        y += 20;

        // --- Button rows stack ---
        // First button row offset (10px after element above)
        y += FIRST_BUTTON_ROW_OFFSET;

        // Row 1: icon picker
        addRenderableWidget(Button.builder(Component.literal("Choose Icon"), b -> {
            this.minecraft.setScreen(new IconPickerScreen(this, ic -> {
                draftIcon = (ic == null) ? IconSpec.item("minecraft:stone") : ic;
                this.minecraft.setScreen(this);
            }));
        }).bounds(cx - 160, y, 320, 20).build());
        y += 20 + BETWEEN_BUTTON_ROWS;

        // Row 2: Save / Cancel / Back
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSavePressed())
                .bounds(cx - 160, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 52, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(cx + 56, y, 100, 20).build());
        // --- end button stack ---
    }

    private void onSavePressed() {
        try {
            draftTitle   = safe(titleBox == null ? draftTitle : titleBox.getValue()).trim();
            draftNote    = safe(noteBox  == null ? draftNote  : noteBox.getValue()).trim();
            draftCommand = safe(cmdBox   == null ? draftCommand : cmdBox.getValue()).trim();

            int delay = 0;
            try {
                String s = (delayBox == null) ? "" : delayBox.getValue().trim();
                delay = s.isEmpty() ? 0 : Math.max(0, Integer.parseInt(s));
            } catch (Throwable ignored) {}
            draftDelayTicks = delay;

            if (draftTitle.isEmpty() || draftCommand.isEmpty()) {
                Constants.LOG.warn("[{}] CommandEdit: Title or Command empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            // Assumes MenuItem supports a 'note' field
            MenuItem item = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("cmd"),
                    draftTitle,
                    draftNote,
                    draftIcon,
                    new ClickActionCommand(draftCommand, draftDelayTicks),
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

        // Labels above their boxes
        final int labelX = this.width / 2 - 160;
        if (titleBox != null) {
            g.drawString(this.font, "Title:",   labelX, titleBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (noteBox != null) {
            g.drawString(this.font, "Note:",    labelX, noteBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (cmdBox != null) {
            g.drawString(this.font, "Command:", labelX, cmdBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }
        if (delayBox != null) {
            g.drawString(this.font, "Multi-command delay (ticks):", labelX, delayBox.getY() - LABEL_TO_FIELD, 0xA0A0A0);
        }

        // Icon preview (top-right)
        int boxW = 60, boxH = 60;
        int bx = this.width - boxW - 16;
        int by = 16;
        g.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, 0x40FFFFFF);
        g.fill(bx, by, bx + boxW, by + boxH, 0x20202020);
        g.drawString(this.font, "Icon", bx + 18, by + 4, 0xA0A0A0);
        try {
            IconRenderer.drawIcon(g, bx + boxW / 2, by + boxH / 2 + 6, draftIcon);
        } catch (Throwable ignored) {}

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { this.minecraft.setScreen(parent); }
}
