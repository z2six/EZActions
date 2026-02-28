package org.z2six.ezactions.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.z2six.ezactions.gui.compat.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Bridges 1.19.2 Screen rendering (PoseStack) to the mod's GuiGraphics-style UI code.
 */
public abstract class EzScreen extends Screen {

    protected EzScreen(Component title) {
        super(title);
    }

    @Override
    public final void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        GuiGraphics g = new GuiGraphics(Minecraft.getInstance(), poseStack);
        render(g, mouseX, mouseY, partialTick);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g.pose(), mouseX, mouseY, partialTick);
    }
}

