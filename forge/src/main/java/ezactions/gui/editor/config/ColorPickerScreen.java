// MainFile: src/main/java/org/z2six/ezactions/gui/editor/config/ColorPickerScreen.java
package ezactions.gui.editor.config;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * NeoForge/Minecraft 1.21.1 compatible color picker:
 * - Hex field (#AARRGGBB / #RRGGBB)
 * - Alpha percent field (0..100) + slider
 * - Hue slider + SV square (simple gradient rendering)
 * - OK/Cancel with callback so parent can persist
 */
public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final Consumer<Integer> onPick;

    // ARGB channels (0..255)
    private int alpha, red, green, blue;

    // HSV (0..1)
    private float hue, sat, val;

    // UI
    private EditBox hexBox;
    private EditBox alphaBox;
    private AlphaSlider alphaSlider;

    // layout
    private int contentLeft;
    private int contentTop;

    // picker rects
    private int svX, svY, svW, svH;
    private int hueX, hueY, hueW, hueH;

    // internal guard to avoid responder loops
    private boolean updatingUI = false;

    // dragging
    private boolean draggingSV = false;
    private boolean draggingHue = false;

    public ColorPickerScreen(Screen parent, int initialArgb, Consumer<Integer> onPick) {
        super(Component.literal("Pick Color"));
        this.parent = parent;
        this.onPick = onPick;

        this.alpha = (initialArgb >>> 24) & 0xFF;
        this.red   = (initialArgb >>> 16) & 0xFF;
        this.green = (initialArgb >>> 8)  & 0xFF;
        this.blue  = (initialArgb)        & 0xFF;

        float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
    }

    @Override
    protected void init() {
        super.init();

        // content inset
        int marginLeft = 20;
        int marginTop  = 20;
        this.contentLeft = marginLeft;
        Font font = this.font;
        int titleY = marginTop;
        this.contentTop = titleY + font.lineHeight + 6;

        // --- Controls layout ---
        int fieldWidth = 140;
        int fieldHeight = 20;
        int y = contentTop;

        // Hex box
        this.hexBox = new EditBox(font, contentLeft, y, fieldWidth, fieldHeight, Component.literal("Hex"));
        this.hexBox.setMaxLength(9); // including '#'
        this.hexBox.setValue(ColorUtil.toHexARGB(alpha, red, green, blue));
        this.hexBox.setResponder(text -> {
            if (updatingUI) return;
            try {
                int[] argb = ColorUtil.parseHexARGB(text);
                alpha = argb[0]; red = argb[1]; green = argb[2]; blue = argb[3];
                float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
                hue = hsv[0]; sat = hsv[1]; val = hsv[2];
                syncFromModel();
            } catch (Exception ignored) {
                // ignore while typing
            }
        });
        this.addRenderableWidget(this.hexBox);

        // Alpha box (0..100)
        y += fieldHeight + 6;
        this.alphaBox = new EditBox(font, contentLeft, y, 60, fieldHeight, Component.literal("Alpha"));
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
        this.addRenderableWidget(this.alphaBox);

        // Alpha slider (0..1)
        int sliderX = contentLeft + 70;
        this.alphaSlider = new AlphaSlider(sliderX, y, 120, fieldHeight,
                (double) alpha / 255.0,
                () -> {
                    if (updatingUI) return;
                    alpha = (int) Math.round(alphaSlider.getValue() * 255.0);
                    syncFromModel();
                });
        this.addRenderableWidget(this.alphaSlider);

        // SV square below
        y += fieldHeight + 10;
        this.svX = contentLeft;
        this.svY = y;
        this.svW = 190;
        this.svH = 120;

        // Hue vertical slider to the right of SV square
        this.hueX = svX + svW + 8;
        this.hueY = svY;
        this.hueW = 14;
        this.hueH = svH;

        // OK / Cancel
        int btnY = svY + svH + 12;
        Button ok = Button.builder(Component.translatable("gui.ok"), btn -> {
            int picked = ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
            if (onPick != null) onPick.accept(picked);
            this.minecraft.setScreen(parent);
        }).bounds(contentLeft, btnY, 80, 20).build();

        Button cancel = Button.builder(Component.translatable("gui.cancel"), btn -> this.minecraft.setScreen(parent))
                .bounds(contentLeft + 90, btnY, 80, 20).build();

        this.addRenderableWidget(ok);
        this.addRenderableWidget(cancel);

        // Initial sync (ensures slider and boxes are coherent)
        syncFromModel();
    }

    // --- Rendering ---

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Forge 1.20.1: Screen#renderBackground(GuiGraphics) only
        this.renderBackground(gfx);

        // Title (drawn above everything, but our widgets are placed below due to contentTop)
        gfx.drawString(this.font, this.title, contentLeft, contentTop - (this.font.lineHeight + 6), 0x808080, false);

        // SV square
        drawSVSquare(gfx, svX, svY, svW, svH);

        // SV handle
        int svHandleX = (int) (svX + (sat * (svW - 1)));
        int svHandleY = (int) (svY + ((1f - val) * (svH - 1)));
        gfx.fill(svHandleX - 2, svHandleY - 2, svHandleX + 3, svHandleY + 3, 0xFFFFFFFF);
        gfx.fill(svHandleX - 1, svHandleY - 1, svHandleX + 2, svHandleY + 2, 0xFF000000);

        // Hue bar
        drawHueBar(gfx, hueX, hueY, hueW, hueH);

        int hueYPos = (int) (hueY + (hue * (hueH - 1)));
        gfx.fill(hueX - 1, hueYPos - 1, hueX + hueW + 1, hueYPos + 2, 0xFFFFFFFF);
        gfx.fill(hueX, hueYPos, hueX + hueW, hueYPos + 1, 0xFF000000);

        // Small preview swatch with checker
        int previewX = hueX + hueW + 12;
        int previewY = svY;
        int previewW = 40;
        int previewH = 20;
        int argb = ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
        drawChecker(gfx, previewX, previewY, previewW, previewH, 4);
        gfx.fill(previewX, previewY, previewX + previewW, previewY + previewH, argb);

        // Right-side labels for fields
        gfx.drawString(this.font, Component.literal("Hex"),
                contentLeft + 140 + 8, contentTop + 4, 0xFFAAAAAA, false);
        gfx.drawString(this.font, Component.literal("Alpha %"),
                contentLeft + 70 + 120 + 8, contentTop + 4 + 26, 0xFFAAAAAA, false);

        super.render(gfx, mouseX, mouseY, partialTick);
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
        // Base: pure hue (sat=1, val=1)
        int[] rgb = ColorUtil.hsvToRgb(hue, 1f, 1f);
        int base = 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        gfx.fill(x, y, x + w, y + h, base);

        RenderSystem.enableBlend();
        // Overlay left->right white gradient (slice-based)
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            int x0 = x + (i * w) / steps;
            int x1 = x + ((i + 1) * w) / steps;
            float t = 1f - (i + 0.5f) / steps;
            int a = (int) (t * 255f);
            int col = (a << 24) | 0x00FFFFFF;
            gfx.fill(x0, y, x1, y + h, col);
        }
        // Overlay top->bottom black gradient
        gfx.fillGradient(x, y, x + w, y + h, 0x00000000, 0xFF000000);
        RenderSystem.disableBlend();
    }

    private void drawHueBar(GuiGraphics gfx, int x, int y, int w, int h) {
        int segH = h / 6;
        int y0 = y;
        fillHueSegment(gfx, x, y0, w, segH, 0f, 1f/6f); y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 1f/6f, 2f/6f); y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 2f/6f, 3f/6f); y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 3f/6f, 4f/6f); y0 += segH;
        fillHueSegment(gfx, x, y0, w, segH, 4f/6f, 5f/6f); y0 += segH;
        fillHueSegment(gfx, x, y0, w, h - 5*segH, 5f/6f, 1f);
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

    // --- Mouse handling for SV and Hue ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inside(mouseX, mouseY, svX, svY, svW, svH)) {
            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }
        if (inside(mouseX, mouseY, hueX, hueY, hueW, hueH)) {
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

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void updateSVFromMouse(double mx, double my) {
        float sx = (float) ((mx - svX) / (double) (svW - 1));
        float vy = (float) ((my - svY) / (double) (svH - 1));
        sat = ColorUtil.clamp01(sx);
        val = ColorUtil.clamp01(1f - vy);
        int[] rgb = ColorUtil.hsvToRgb(hue, sat, val);
        red = rgb[0]; green = rgb[1]; blue = rgb[2];
        syncFromModel();
    }

    private void updateHueFromMouse(double my) {
        float ty = (float) ((my - hueY) / (double) (hueH - 1));
        hue = ColorUtil.wrap01(ty);
        int[] rgb = ColorUtil.hsvToRgb(hue, sat, val);
        red = rgb[0]; green = rgb[1]; blue = rgb[2];
        syncFromModel();
    }

    // --- Sync helpers (guarded to prevent responder loops) ---

    private void syncFromModel() {
        updatingUI = true;
        try {
            float[] hsv = ColorUtil.rgbToHsv(red, green, blue);
            hue = hsv[0]; sat = hsv[1]; val = hsv[2];

            // hex field
            setBoxSilently(hexBox, ColorUtil.toHexARGB(alpha, red, green, blue));

            // alpha percent box + slider
            int pct = Math.round(alpha * 100f / 255f);
            setBoxSilently(alphaBox, Integer.toString(pct));
            alphaSlider.setFromOutside(alpha / 255.0);
        } finally {
            updatingUI = false;
        }
    }

    private void setBoxSilently(EditBox box, String value) {
        if (!Objects.equals(box.getValue(), value)) {
            box.setValue(value); // will fire responder, but we are under updatingUI
        }
    }

    private int parseIntSafe(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // --- Slider ---

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
            this.setMessage(Component.literal("Alpha: " + pct + "%"));
        }

        @Override
        protected void applyValue() {
            if (onChanged != null) onChanged.run();
        }

        /** Programmatic setter that also refreshes label without causing external loops. */
        void setFromOutside(double v) {
            double nv = Mth.clamp(v, 0.0, 1.0);
            if (Math.abs(nv - this.value) < 1e-6) return;
            this.value = nv;
            updateMessage();
        }

        /** Exposes the protected slider value to the outer screen. */
        double getValue() {
            return this.value;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
