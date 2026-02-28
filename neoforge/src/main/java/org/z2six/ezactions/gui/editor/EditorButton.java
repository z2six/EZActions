package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Shared themed button for editor/config screens. */
public final class EditorButton extends AbstractButton {

    private final Runnable onPress;
    private boolean forcedHovered = false;

    public EditorButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        super(x, y, w, h, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (this.active && this.onPress != null) {
            this.onPress.run();
        }
    }

    public EditorButton setForcedHovered(boolean forcedHovered) {
        this.forcedHovered = forcedHovered;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isHoveredOrFocused() || forcedHovered;

        int border = !active ? 0x6640556D : 0xAA2C425B;
        int bgTop = !active ? 0x66333F4C : (hovered ? 0xF429435F : 0xE61B2D42);
        int bgBottom = !active ? 0x66303B47 : (hovered ? 0xF422394F : 0xE6152537);
        int txt = !active ? 0x8896A9BE : (hovered ? 0xFFF4FAFF : 0xFFE2EDF9);
        int topAccent = !active ? 0x406E7C8C : (hovered ? 0xFF66B5FF : 0x50FFFFFF);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fillGradient(x, y, x + w, y + h, bgTop, bgBottom);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, topAccent);

        Font font = Minecraft.getInstance().font;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawCenteredString(font, getMessage(), x + (w / 2), ty, txt);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
