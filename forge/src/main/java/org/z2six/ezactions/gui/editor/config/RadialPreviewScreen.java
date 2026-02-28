package org.z2six.ezactions.gui.editor.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.editor.ActionEditorUi;
import org.z2six.ezactions.gui.RadialScreenDraw;
import org.z2six.ezactions.gui.RadialScreenMath;
import org.z2six.ezactions.gui.anim.SliceHoverAnim;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.ArrayList;
import java.util.List;

/** Looped visual preview for radial style/animation config. */
public final class RadialPreviewScreen extends Screen implements NoMenuBlurScreen {
    private static final long HOLD_MS = 1000L;

    private final Screen parent;
    private final RadialMenu.TemporaryStyle style;
    private final List<MenuItem> sampleItems = new ArrayList<>();
    private final SliceHoverAnim hoverAnim = new SliceHoverAnim();

    private long phaseStartMs = 0L;
    private int phase = 0; // 0=open, 1=hold-open, 2=close, 3=hold-closed

    public RadialPreviewScreen(Screen parent, RadialMenu.TemporaryStyle style) {
        super(Component.translatable("ezactions.gui.radial_preview.title"));
        this.parent = parent;
        this.style = style;
    }

    @Override
    protected void init() {
        int bw = 96;
        int bh = 20;
        int bx = (this.width - bw) / 2;
        int by = this.height - 34;
        addRenderableWidget(ActionEditorUi.button(bx, by, bw, bh, Component.translatable("ezactions.gui.common.back"), this::onClose));

        sampleItems.clear();
        sampleItems.add(new MenuItem("pv1", Component.translatable("ezactions.gui.radial_preview.sample.sword"), Component.empty(), IconSpec.item("minecraft:iron_sword"), null, List.of()));
        sampleItems.add(new MenuItem("pv2", Component.translatable("ezactions.gui.radial_preview.sample.bow"), Component.empty(), IconSpec.item("minecraft:bow"), null, List.of()));
        sampleItems.add(new MenuItem("pv3", Component.translatable("ezactions.gui.radial_preview.sample.torch"), Component.empty(), IconSpec.item("minecraft:torch"), null, List.of()));
        sampleItems.add(new MenuItem("pv4", Component.translatable("ezactions.gui.radial_preview.sample.pickaxe"), Component.empty(), IconSpec.item("minecraft:diamond_pickaxe"), null, List.of()));
        sampleItems.add(new MenuItem("pv5", Component.translatable("ezactions.gui.radial_preview.sample.apple"), Component.empty(), IconSpec.item("minecraft:apple"), null, List.of()));
        sampleItems.add(new MenuItem("pv6", Component.translatable("ezactions.gui.radial_preview.sample.shield"), Component.empty(), IconSpec.item("minecraft:shield"), null, List.of()));
        sampleItems.add(new MenuItem("pv7", Component.translatable("ezactions.gui.radial_preview.sample.pearl"), Component.empty(), IconSpec.item("minecraft:ender_pearl"), null, List.of()));
        sampleItems.add(new MenuItem("pv8", Component.translatable("ezactions.gui.radial_preview.sample.potion"), Component.empty(), IconSpec.item("minecraft:potion"), null, List.of()));

        RadialMenu.setPreviewStyle(style);
        phase = 0;
        phaseStartMs = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xCC000000);
        g.drawCenteredString(this.font, Component.translatable("ezactions.gui.radial_preview.loops"), this.width / 2, 8, 0xFFEDEDED);

        long now = System.currentTimeMillis();
        int animMs = Math.max(150, style == null || style.openCloseMs == null ? 250 : style.openCloseMs);
        float openProgress = computeProgress(now, animMs);

        int n = Math.max(1, sampleItems.size());
        int hovered = (int) ((now / 800L) % n);
        hoverAnim.tick(now, hovered, n);

        int cx = this.width / 2;
        int cy = this.height / 2;
        RadialScreenMath.Radii rr = RadialScreenMath.computeRadii(sampleItems.size());
        RadialScreenDraw.drawRing(g, this.font, cx, cy, sampleItems, hovered, rr, hoverAnim, openProgress);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private float computeProgress(long now, int animMs) {
        long dt = now - phaseStartMs;
        switch (phase) {
            case 0 -> {
                if (dt >= animMs) {
                    phase = 1;
                    phaseStartMs = now;
                    return 1.0f;
                }
                return clamp01(dt / (float) animMs);
            }
            case 1 -> {
                if (dt >= HOLD_MS) {
                    phase = 2;
                    phaseStartMs = now;
                }
                return 1.0f;
            }
            case 2 -> {
                if (dt >= animMs) {
                    phase = 3;
                    phaseStartMs = now;
                    return 0.0f;
                }
                return 1.0f - clamp01(dt / (float) animMs);
            }
            default -> {
                if (dt >= HOLD_MS) {
                    phase = 0;
                    phaseStartMs = now;
                }
                return 0.0f;
            }
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    @Override
    public void onClose() {
        RadialMenu.clearPreviewStyle();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
