package org.z2six.ezactions.gui.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.z2six.ezactions.gui.compat.GuiGraphics;
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
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(Minecraft.getInstance(), poseStack);
        int x = this.x;
        int y = this.y;
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isHoveredOrFocused() || forcedHovered;

        int border = !active ? 0x66444444 : 0xAA3A3A3A;
        int bgTop = !active ? 0x66222222 : (hovered ? 0xFF1E1E1E : 0xF0141414);
        int bgBottom = !active ? 0x661A1A1A : (hovered ? 0xFF181818 : 0xF0101010);
        int txt = !active ? 0x88969696 : (hovered ? 0xFFF7F7F7 : 0xFFE4E4E4);
        int cornerAccent = (!active || !hovered) ? 0xFF000000 : ActionEditorUi.ACCENT;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fillGradient(x, y, x + w, y + h, bgTop, bgBottom);
        drawInnerCorners(g, x, y, w, h, cornerAccent);

        Font font = Minecraft.getInstance().font;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawCenteredString(font, getMessage(), x + (w / 2), ty, txt);
    }

    private static void drawInnerCorners(GuiGraphics g, int x, int y, int w, int h, int color) {
        int in = 2;
        int len = 4;

        int lx = x + in;
        int rx = x + w - 1 - in;
        int ty = y + in;
        int by = y + h - 1 - in;

        // top-left
        g.fill(lx, ty, lx + 1, ty + len, color);
        g.fill(lx, ty, lx + len, ty + 1, color);
        // top-right
        g.fill(rx, ty, rx + 1, ty + len, color);
        g.fill(rx - len + 1, ty, rx + 1, ty + 1, color);
        // bottom-left
        g.fill(lx, by - len + 1, lx + 1, by + 1, color);
        g.fill(lx, by, lx + len, by + 1, color);
        // bottom-right
        g.fill(rx, by - len + 1, rx + 1, by + 1, color);
        g.fill(rx - len + 1, by, rx + 1, by + 1, color);
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

