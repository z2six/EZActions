package org.z2six.ezactions.gui.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CommandActionEditScreen extends Screen {

    private final Screen parent;
    private final MenuItem editing;

    private String draftTitle = "";
    private String draftNote = "";
    private String draftCommand = "/say hi";
    private int draftDelayTicks = 0;
    private boolean draftCycleCommands = false;
    private IconSpec draftIcon = IconSpec.item("minecraft:stone");

    private EditBox titleBox;
    private EditBox noteBox;
    private MultiLineEditBox cmdBox;
    private EditBox delayBox;
    private Checkbox cycleCommandsBox;

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

    public CommandActionEditScreen(Screen parent, MenuItem editing) {
        super(Component.translatable(editing == null
                ? "ezactions.gui.command_action.title.add"
                : "ezactions.gui.command_action.title.edit"));
        this.parent = parent;
        this.editing = editing;

        if (editing != null && editing.action() instanceof ClickActionCommand cc) {
            this.draftTitle = safe(editing.title());
            try {
                this.draftNote = safe(editing.note());
            } catch (Throwable ignored) {}

            String extracted = "";
            try {
                extracted = cc.getCommand();
            } catch (Throwable ignored) {}
            if (extracted == null || extracted.isEmpty()) {
                extracted = tryExtractCommandString(cc);
            }
            if (!extracted.isEmpty()) {
                this.draftCommand = extracted;
            }

            int delay = 0;
            try {
                delay = cc.getDelayTicks();
            } catch (Throwable ignored) {}
            if (delay <= 0) {
                delay = tryExtractDelayTicks(cc);
            }
            this.draftDelayTicks = Math.max(0, delay);
            try {
                this.draftCycleCommands = cc.isCycleCommands();
            } catch (Throwable ignored) {}

            if (editing.icon() != null) {
                this.draftIcon = editing.icon();
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String tryExtractCommandString(ClickActionCommand cc) {
        String[] methodNames = {"command", "getCommand", "getCmd", "cmd"};
        for (String mname : methodNames) {
            try {
                Method m = cc.getClass().getMethod(mname);
                Object v = m.invoke(cc);
                if (v instanceof String s && !s.isEmpty()) {
                    return s;
                }
            } catch (Throwable ignored) {}
        }
        String[] fieldNames = {"command", "cmd", "commandRaw"};
        for (String fname : fieldNames) {
            try {
                Field f = cc.getClass().getDeclaredField(fname);
                f.setAccessible(true);
                Object v = f.get(cc);
                if (v instanceof String s && !s.isEmpty()) {
                    return s;
                }
            } catch (Throwable ignored) {}
        }
        return "";
    }

    private static int tryExtractDelayTicks(ClickActionCommand cc) {
        String[] methods = {"getDelayTicks", "delayTicks", "getDelay", "delay"};
        for (String mname : methods) {
            try {
                Method m = cc.getClass().getMethod(mname);
                Object v = m.invoke(cc);
                if (v instanceof Number n) {
                    return n.intValue();
                }
            } catch (Throwable ignored) {}
        }
        String[] fields = {"delayTicks", "delay", "ticksDelay"};
        for (String fname : fields) {
            try {
                Field f = cc.getClass().getDeclaredField(fname);
                f.setAccessible(true);
                Object v = f.get(cc);
                if (v instanceof Number n) {
                    return n.intValue();
                }
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    @Override
    protected void init() {
        this.panel = ActionEditorUi.panel(this.width, this.height, 820, 520, 10);
        this.scroll.reset();

        bodyX = panel.x() + 14;
        bodyY = panel.y() + 34;
        bodyW = panel.w() - 28;
        bodyH = panel.h() - 42;

        int iconAreaW = 58;
        int fieldW = Math.max(210, bodyW - iconAreaW - 14);
        int fieldX = bodyX;

        iconX = fieldX + fieldW + 18;
        iconBaseY = bodyY + 26;

        int y = bodyY + 18;

        titleBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.title"));
        titleBox.setHint(Component.translatable("ezactions.gui.command_action.hint.title"));
        titleBox.setValue(draftTitle);
        titleBox.setResponder(s -> draftTitle = safe(s));
        scroll.track(addRenderableWidget(titleBox));
        y += 30;

        noteBox = new EditBox(this.font, fieldX, y, fieldW, 20, Component.translatable("ezactions.gui.field.note"));
        noteBox.setHint(Component.translatable("ezactions.gui.hint.note_optional"));
        noteBox.setValue(draftNote);
        noteBox.setResponder(s -> draftNote = safe(s));
        scroll.track(addRenderableWidget(noteBox));

        int cmdY = y + 34;

        int cfgLines = 5;
        try {
            cfgLines = GeneralClientConfig.CONFIG.commandEditorVisibleLines();
        } catch (Throwable ignored) {}
        if (cfgLines < 1) {
            cfgLines = 1;
        }
        if (cfgLines > 20) {
            cfgLines = 20;
        }
        int preferredH = (this.font.lineHeight * cfgLines) + 6;
        int cmdH = Math.max(64, preferredH);
        int delayY = cmdY + cmdH + 34;
        int cycleY = delayY + 24;
        int buttonY = cycleY + 30;
        cardBaseY = bodyY;
        cardBaseH = (buttonY - bodyY) + 34;

        cmdBox = new MultiLineEditBox(
                this.font,
                bodyX,
                cmdY,
                bodyW,
                cmdH,
                Component.translatable("ezactions.gui.field.command"),
                Component.literal(draftCommand)
        );
        cmdBox.setCharacterLimit(32767);
        cmdBox.setValue(draftCommand);
        cmdBox.setValueListener(s -> draftCommand = safe(s));
        scroll.track(addRenderableWidget(cmdBox));

        delayBox = new EditBox(this.font, bodyX, delayY, 140, 20, Component.translatable("ezactions.gui.command_action.field.delay_ticks"));
        delayBox.setValue(this.draftDelayTicks > 0 ? Integer.toString(this.draftDelayTicks) : "");
        delayBox.setResponder(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                draftDelayTicks = Math.max(0, v);
            } catch (Throwable ignored) {}
        });
        scroll.track(addRenderableWidget(delayBox));

        cycleCommandsBox = Checkbox.builder(Component.translatable("ezactions.gui.command_action.cycle_commands"), this.font)
                .pos(bodyX, cycleY)
                .selected(draftCycleCommands)
                .onValueChange((cb, value) -> draftCycleCommands = value)
                .maxWidth(bodyW - 24)
                .build();
        scroll.track(addRenderableWidget(cycleCommandsBox));

        int totalW = (96 * 3) + (8 * 2);
        int left = panel.x() + (panel.w() - totalW) / 2;
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
            draftCommand = safe(cmdBox == null ? draftCommand : cmdBox.getValue()).trim();

            int delay = 0;
            try {
                String s = (delayBox == null) ? "" : delayBox.getValue().trim();
                delay = s.isEmpty() ? 0 : Math.max(0, Integer.parseInt(s));
            } catch (Throwable ignored) {}
            draftDelayTicks = delay;
            draftCycleCommands = cycleCommandsBox != null && cycleCommandsBox.selected();

            if (draftTitle.isEmpty() || draftCommand.isEmpty()) {
                Constants.LOG.warn("[{}] CommandEdit: Title or Command empty; ignoring save.", Constants.MOD_NAME);
                return;
            }

            MenuItem item = new MenuItem(
                    editing != null ? editing.id() : MenuEditorScreen.freshId("cmd"),
                    draftTitle,
                    draftNote,
                    draftIcon,
                    new ClickActionCommand(draftCommand, draftDelayTicks, draftCycleCommands),
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
        ActionEditorUi.drawFrame(g, this.font, this.width, this.height, panel, this.title);
        scroll.layout(bodyY, bodyY + bodyH);
        ActionEditorUi.drawCard(g, this.font, bodyX, scroll.y(cardBaseY), bodyW, cardBaseH, Component.empty());

        if (titleBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.title"), titleBox.getX(), titleBox.getY() - 10);
        }
        if (noteBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.note"), noteBox.getX(), noteBox.getY() - 10);
        }
        if (cmdBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.field.command"), cmdBox.getX(), cmdBox.getY() - 10);
        }
        if (delayBox != null) {
            ActionEditorUi.drawFieldLabel(g, this.font, Component.translatable("ezactions.gui.command_action.field.multi_delay_ticks"), delayBox.getX(), delayBox.getY() - 10);
        }

        ActionEditorUi.drawIconCard(g, this.font, iconX, scroll.y(iconBaseY), 32, Component.translatable("ezactions.gui.field.icon"), this.draftIcon, iconHit(mouseX, mouseY));
        scroll.drawScrollbar(g, bodyX, bodyY, bodyW, bodyH);

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
