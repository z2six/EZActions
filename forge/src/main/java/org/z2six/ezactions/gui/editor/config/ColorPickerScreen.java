package org.z2six.ezactions.gui.editor.config;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import org.z2six.ezactions.gui.compat.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.gui.EzScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.z2six.ezactions.gui.editor.ActionEditorUi;

import java.util.Objects;
import java.util.function.Consumer;

/** Themed color picker with scrollable content area and shared editor styling. */
public class ColorPickerScreen extends EzScreen {

    private final Screen parent;
    private final Consumer<Integer> onPick;

    private int alpha, red, green, blue;
    private float hue, sat, val;

    private EditBox hexBox;
    private EditBox alphaBox;
    private AlphaSlider alphaSlider;

    private ActionEditorUi.Panel panel;
    private final ActionEditorUi.ScrollArea scroll = new ActionEditorUi.ScrollArea();
    private int bodyX;
    private int bodyY;
    private int bodyW;
    private int bodyH;

    private int contentLeft;
    private int contentRight;
    private int hexBaseY;
    private int alphaBaseY;
    private int svBaseY;
    private int buttonBaseY;

    private int svX, svW, svH;
    private int hueX, hueW, hueH;

    private boolean updatingUI = false;
    private boolean draggingSV = false;
    private boolean draggingHue = false;

    public ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> onPick) {
        super(Component.translatable("ezactions.gui.color_picker.title"));
        this.parent = parent;
        this.onPick = onPick;

        this.alpha = (initialArgb >>> 24) & 0xFF;
        this.red = (initialArgb >>> 16) & 0xFF;
        this.green = (initialArgb >>> 8) & 0xFF;
        this.blue = initialArgb & 0xFF;

        float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.scroll.reset();

        panel = ActionEditorUi.panel(this.width, this.height, 520, 430, 10);
        bodyX = panel.x() + 14;
        bodyY = panel.y() + 34;
        bodyW = panel.w() - 28;
        bodyH = panel.h() - 42;

        contentLeft = bodyX + 14;
        contentRight = bodyX + bodyW - 14;

        hexBaseY = bodyY + 18;
        alphaBaseY = hexBaseY + 28;
        svBaseY = alphaBaseY + 34;
        svH = 128;
        hueW = 14;
        hueH = svH;
        int previewW = 44;
        int fixedRight = hueW + 12 + previewW;
        svX = contentLeft;
        svW = Math.max(140, (contentRight - contentLeft) - fixedRight - 8);
        hueX = svX + svW + 8;
        buttonBaseY = svBaseY + svH + 16;

        int fieldWidth = Math.min(180, Math.max(120, contentRight - contentLeft - 6));
        int fieldHeight = 20;
        Font font = this.font;

        this.hexBox = new EditBox(font, contentLeft, hexBaseY, fieldWidth, fieldHeight, Component.translatable("ezactions.gui.color_picker.hex"));
        this.hexBox.setMaxLength(9);
        this.hexBox.setValue(ColorUtil.toHexARGB(alpha, red, green, blue));
        this.hexBox.setResponder(text -> {
            if (updatingUI) return;
            try {
                int[] argb = ColorUtil.parseHexARGB(text);
                alpha = argb[0];
                red = argb[1];
                green = argb[2];
                blue = argb[3];
                float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
                hue = hsv[0];
                sat = hsv[1];
                val = hsv[2];
                syncFromModel();
            } catch (Exception ignored) {}
        });
        scroll.track(addRenderableWidget(this.hexBox));

        this.alphaBox = new EditBox(font, contentLeft, alphaBaseY, 60, fieldHeight, Component.translatable("ezactions.gui.color_picker.alpha"));
        this.alphaBox.setMaxLength(3);
        this.alphaBox.setValue(Integer.toString(Math.round(alpha * 100f / 255f)));
        this.alphaBox.setResponder(text -> {
            if (updatingUI) return;
            int pct = parseIntSafe(text, -1);
            if (pct >= 0 && pct <= 100) {
                alpha = Mth.clamp(Math.round(pct * 2.55f), 0, 255);
                syncFromModel();
            }
        });
        scroll.track(addRenderableWidget(this.alphaBox));

        int sliderX = contentLeft + 70;
        int sliderW = Math.max(100, contentRight - sliderX);
        this.alphaSlider = new AlphaSlider(sliderX, alphaBaseY, sliderW, fieldHeight,
                (double) alpha / 255.0,
                () -> {
                    if (updatingUI) return;
                    alpha = (int) Math.round(alphaSlider.getValue() * 255.0);
                    syncFromModel();
                });
        scroll.track(addRenderableWidget(this.alphaSlider));

        scroll.track(addRenderableWidget(ActionEditorUi.button(contentLeft, buttonBaseY, 80, 20, Component.translatable("gui.ok"), () -> {
            int picked = ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
            if (onPick != null) onPick.accept(picked);
            this.minecraft.setScreen(parent);
        })));
        scroll.track(addRenderableWidget(ActionEditorUi.button(contentLeft + 90, buttonBaseY, 80, 20, Component.translatable("gui.cancel"),
                () -> this.minecraft.setScreen(parent))));

        scroll.include(bodyY, buttonBaseY + 26);
        scroll.layout(bodyY, bodyY + bodyH);
        syncFromModel();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        ActionEditorUi.drawFrame(gfx, this.font, this.width, this.height, panel, this.title);
        scroll.layout(bodyY, bodyY + bodyH);
        ActionEditorUi.drawCard(gfx, this.font, bodyX, bodyY, bodyW, bodyH, "");

        int clipX1 = bodyX + 1;
        int clipY1 = bodyY + 1;
        int clipX2 = bodyX + bodyW - 1;
        int clipY2 = bodyY + bodyH - 1;

        int drawSvY = scroll.y(svBaseY);
        int drawHueY = drawSvY;
        int previewX = hueX + hueW + 12;
        int previewY = drawSvY;
        int previewW = 44;
        int previewH = 20;

        gfx.enableScissor(clipX1, clipY1, clipX2, clipY2);
        try {
            drawSVSquare(gfx, svX, drawSvY, svW, svH);

            int svHandleX = (int) (svX + (sat * (svW - 1)));
            int svHandleY = (int) (drawSvY + ((1f - val) * (svH - 1)));
            gfx.fill(svHandleX - 2, svHandleY - 2, svHandleX + 3, svHandleY + 3, 0xFFFFFFFF);
            gfx.fill(svHandleX - 1, svHandleY - 1, svHandleX + 2, svHandleY + 2, 0xFF000000);

            drawHueBar(gfx, hueX, drawHueY, hueW, hueH);
            int hueYPos = (int) (drawHueY + (hue * (hueH - 1)));
            gfx.fill(hueX - 1, hueYPos - 1, hueX + hueW + 1, hueYPos + 2, 0xFFFFFFFF);
            gfx.fill(hueX, hueYPos, hueX + hueW, hueYPos + 1, 0xFF000000);

            int argb = ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
            drawChecker(gfx, previewX, previewY, previewW, previewH, 4);
            gfx.fill(previewX, previewY, previewX + previewW, previewY + previewH, argb);
        } finally {
            gfx.disableScissor();
        }

        scroll.drawScrollbar(gfx, bodyX, bodyY, bodyW, bodyH);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int drawSvY = scroll.y(svBaseY);
        int drawHueY = drawSvY;
        if (inside(mouseX, mouseY, svX, drawSvY, svW, svH)) {
            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        if (inside(mouseX, mouseY, hueX, drawHueY, hueW, hueH)) {
            draggingHue = true;
            updateHueFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingSV) {
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        if (draggingHue) {
            updateHueFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSV = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scroll.mouseScrolled(mouseX, mouseY, delta, bodyX, bodyY, bodyW, bodyH)) {
            scroll.layout(bodyY, bodyY + bodyH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void updateSVFromMouse(double mx, double my) {
        int drawSvY = scroll.y(svBaseY);
        float sx = (float) ((mx - svX) / (double) (svW - 1));
        float vy = (float) ((my - drawSvY) / (double) (svH - 1));
        sat = ColorUtil.clamp01(sx);
        val = ColorUtil.clamp01(1f - vy);
        int[] rgb = ColorUtil.hsvToRgb(hue, sat, val);
        red = rgb[0];
        green = rgb[1];
        blue = rgb[2];
        syncFromModel();
    }

    private void updateHueFromMouse(double my) {
        int drawHueY = scroll.y(svBaseY);
        float ty = (float) ((my - drawHueY) / (double) (hueH - 1));
        hue = ColorUtil.wrap01(ty);
        int[] rgb = ColorUtil.hsvToRgb(hue, sat, val);
        red = rgb[0];
        green = rgb[1];
        blue = rgb[2];
        syncFromModel();
    }

    private void syncFromModel() {
        updatingUI = true;
        try {
            float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];

            setBoxSilently(hexBox, ColorUtil.toHexARGB(alpha, red, green, blue));

            int pct = Math.round(alpha * 100f / 255f);
            setBoxSilently(alphaBox, Integer.toString(pct));
            alphaSlider.setFromOutside(alpha / 255.0);
        } finally {
            updatingUI = false;
        }
    }

    private void setBoxSilently(EditBox box, String value) {
        if (!Objects.equals(box.getValue(), value)) {
            box.setValue(value);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int parseIntSafe(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void drawChecker(GuiGraphics gfx, int x, int y, int w, int h, int cell) {
        int c1 = 0xFFB0B0B0;
        int c2 = 0xFF8A8A8A;
        for (int yy = y; yy < y + h; yy += cell) {
            for (int xx = x; xx < x + w; xx += cell) {
                boolean alt = (((xx - x) / cell) + ((yy - y) / cell)) % 2 == 0;
                gfx.fill(xx, yy, Math.min(xx + cell, x + w), Math.min(yy + cell, y + h), alt ? c1 : c2);
            }
        }
    }

    private void drawSVSquare(GuiGraphics gfx, int x, int y, int w, int h) {
        int[] rgb = ColorUtil.hsvToRgb(hue, 1f, 1f);
        int base = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        gfx.fill(x, y, x + w, y + h, base);

        RenderSystem.enableBlend();
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            int x0 = x + (i * w) / steps;
            int x1 = x + ((i + 1) * w) / steps;
            float t = 1f - (i + 0.5f) / steps;
            int a = (int) (t * 255f);
            int col = (a << 24) | 0x00FFFFFF;
            gfx.fill(x0, y, x1, y + h, col);
        }
        gfx.fillGradient(x, y, x + w, y + h, 0x00000000, 0xFF000000);
        RenderSystem.disableBlend();
    }

    private void drawHueBar(GuiGraphics gfx, int x, int y, int w, int h) {
        int segH = h / 6;
        int y0 = y;
        fillHueSegment(gfx, x, y0, w, segH, 0f, 1f / 6f);
        y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 1f / 6f, 2f / 6f);
        y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 2f / 6f, 3f / 6f);
        y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 3f / 6f, 4f / 6f);
        y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 4f / 6f, 5f / 6f);
        y0 += segH;
        fillHueSegment(gfx, x, y0, w, h - 5 * segH, 5f / 6f, 1f);
    }

    private void fillHueSegment(GuiGraphics gfx, int x, int y, int w, int h, float h0, float h1) {
        int steps = Math.max(8, h / 2);
        for (int i = 0; i < steps; i++) {
            float t = (i + 0.5f) / steps;
            float hh = Mth.lerp(t, h0, h1);
            int[] rgb = ColorUtil.hsvToRgb(hh, 1f, 1f);
            int col = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
            int yy0 = y + (i * h) / steps;
            int yy1 = y + ((i + 1) * h) / steps;
            gfx.fill(x, yy0, x + w, yy1, col);
        }
    }

    private static class AlphaSlider extends AbstractSliderButton {
        private final Runnable onChanged;

        AlphaSlider(int x, int y, int w, int h, double initial, Runnable onChanged) {
            super(x, y, w, h, Component.empty(), Mth.clamp(initial, 0.0, 1.0));
            this.onChanged = onChanged;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int pct = (int) Math.round(this.value * 100.0);
            this.setMessage(Component.translatable("ezactions.gui.color_picker.alpha_percent", pct));
        }

        @Override
        protected void applyValue() {
            if (onChanged != null) onChanged.run();
        }

        void setFromOutside(double v) {
            double nv = Mth.clamp(v, 0.0, 1.0);
            if (Math.abs(nv - this.value) < 1e-6) return;
            this.value = nv;
            updateMessage();
        }

        double getValue() {
            return this.value;
        }
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



