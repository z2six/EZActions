package org.z2six.ezactions.gui.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 1.19.2 compatibility shim for code written against 1.20+ GuiGraphics.
 * Implements only the methods used by EZActions.
 */
public class GuiGraphics {

    private final Minecraft minecraft;
    private final PoseStack pose;

    public GuiGraphics(Minecraft minecraft, PoseStack pose) {
        this.minecraft = minecraft;
        this.pose = pose;
    }

    public PoseStack pose() {
        return this.pose;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(this.pose, x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = this.pose.last().pose();

        float aTop = ((colorTop >>> 24) & 0xFF) / 255.0f;
        float rTop = ((colorTop >>> 16) & 0xFF) / 255.0f;
        float gTop = ((colorTop >>> 8) & 0xFF) / 255.0f;
        float bTop = (colorTop & 0xFF) / 255.0f;

        float aBot = ((colorBottom >>> 24) & 0xFF) / 255.0f;
        float rBot = ((colorBottom >>> 16) & 0xFF) / 255.0f;
        float gBot = ((colorBottom >>> 8) & 0xFF) / 255.0f;
        float bBot = (colorBottom & 0xFF) / 255.0f;

        buffer.vertex(matrix, x2, y1, 0).color(rTop, gTop, bTop, aTop).endVertex();
        buffer.vertex(matrix, x1, y1, 0).color(rTop, gTop, bTop, aTop).endVertex();
        buffer.vertex(matrix, x1, y2, 0).color(rBot, gBot, bBot, aBot).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(rBot, gBot, bBot, aBot).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        GuiComponent.drawCenteredString(this.pose, font, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        GuiComponent.drawCenteredString(this.pose, font, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color) {
        return font.draw(this.pose, text, x, y, color);
    }

    public int drawString(Font font, String text, int x, int y, int color) {
        return font.draw(this.pose, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(this.pose, x, y, 0, u, v, width, height, texWidth, texHeight);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        this.minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(Font font, Component text, int mouseX, int mouseY) {
        Screen screen = this.minecraft.screen;
        if (screen != null) {
            screen.renderTooltip(this.pose, text, mouseX, mouseY);
        }
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = this.minecraft.getWindow().getGuiScale();
        int fbHeight = this.minecraft.getWindow().getHeight();

        int sx = Mth.floor(x1 * scale);
        int sy = Mth.floor(fbHeight - (y2 * scale));
        int sw = Mth.floor((x2 - x1) * scale);
        int sh = Mth.floor((y2 - y1) * scale);

        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    public void disableScissor() {
        RenderSystem.disableScissor();
    }

    public void flush() {
        // No-op on 1.19.2.
    }
}
