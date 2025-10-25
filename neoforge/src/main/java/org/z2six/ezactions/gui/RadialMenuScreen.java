package org.z2six.ezactions.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.config.RadialAnimConfigView;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.RadialScreenMath.Radii;
import org.z2six.ezactions.gui.anim.RadialTransition;
import org.z2six.ezactions.gui.anim.SliceHoverAnim;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;
import org.z2six.ezactions.handler.KeyboardHandler;

import java.util.List;

/**
 * Radial menu:
 * - Hold-to-open. Release = execute hovered action (non-category).
 * - Game continues; mouse is used for selection.
 * - LMB on action: close+execute; LMB on category: drill in (stay open).
 * - RMB: go back.
 */
public final class RadialMenuScreen extends Screen implements NoMenuBlurScreen {

    private int hoveredIndex = -1;

    // Anim state (open/close + hover)
    private final RadialTransition openTrans = new RadialTransition();
    private final SliceHoverAnim hoverAnim = new SliceHoverAnim();

    public RadialMenuScreen() {
        super(Component.literal("ezactions Radial"));
    }

    @Override
    protected void init() {
        super.init();
        // Start open wipe (config will gate its usage during render)
        openTrans.start(+1);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Called by KeyboardHandler on hotkey release (falling edge). */
    public void onHotkeyReleased() {
        try {
            List<MenuItem> items = RadialMenu.currentItems();
            if (items != null && !items.isEmpty()
                    && hoveredIndex >= 0 && hoveredIndex < items.size()) {
                MenuItem mi = items.get(hoveredIndex);
                if (!mi.isCategory()) {
                    KeyboardHandler.suppressReopenUntilReleased();
                    executeAndClose(mi);
                    return;
                }
            }
            Minecraft.getInstance().setScreen(null);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] onHotkeyReleased error: {}", Constants.MOD_NAME, t.toString());
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        try {
            List<MenuItem> items = RadialMenu.currentItems();
            final int cx = this.width / 2;
            final int cy = this.height / 2;

            final int count = (items == null) ? 0 : items.size();
            final Radii rr = RadialScreenMath.computeRadii(count);

            hoveredIndex = (count <= 0)
                    ? -1
                    : RadialScreenMath.pickSector(mouseX, mouseY, cx, cy, count, rr);

            // Tick hover animation state
            hoverAnim.tick(System.currentTimeMillis(), hoveredIndex, count);

            // Decide open/close progress via config
            final RadialAnimConfigView view = RadialAnimConfigView.get();
            final float openProg = (view.animationsEnabled && view.animOpenClose)
                    ? openTrans.progress()
                    : 1.0f;

            // Draw ring with animations wired in
            RadialScreenDraw.drawRing(g, this.font, cx, cy, items, hoveredIndex, rr, hoverAnim, openProg);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial render error: {}", Constants.MOD_NAME, t.toString());
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            if (button == 1) { // RMB → back
                if (RadialMenu.canGoBack()) {
                    RadialMenu.goBack();
                    this.minecraft.setScreen(new RadialMenuScreen());
                } else {
                    onClose();
                }
                return true;
            }

            if (button == 0) { // LMB
                List<MenuItem> items = RadialMenu.currentItems();
                if (items == null || items.isEmpty()) return true;
                if (hoveredIndex < 0 || hoveredIndex >= items.size()) return true;

                MenuItem mi = items.get(hoveredIndex);
                if (mi.isCategory()) {
                    RadialMenu.enterCategory(mi);
                    this.minecraft.setScreen(new RadialMenuScreen());
                    return true;
                } else {
                    KeyboardHandler.suppressReopenUntilReleased();
                    executeAndClose(mi);
                    return true;
                }
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial mouseClicked error: {}", Constants.MOD_NAME, t.toString());
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void executeAndClose(MenuItem mi) {
        try {
            Constants.LOG.info("[{}] Radial: execute action id='{}' title='{}' (closing then deferring)",
                    Constants.MOD_NAME, mi.id(), mi.title());
            Minecraft mc = this.minecraft;
            onClose(); // close first
            mc.execute(() -> {
                try {
                    boolean ok = mi.action() != null && mi.action().execute(mc);
                    if (!ok) {
                        Constants.LOG.info("[{}] Radial action returned false for '{}'", Constants.MOD_NAME, mi.id());
                    }
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Radial deferred execution error for '{}': {}", Constants.MOD_NAME, mi.id(), t.toString());
                }
            });
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] executeAndClose error: {}", Constants.MOD_NAME, t.toString());
            onClose();
        }
    }

    @Override
    public void onClose() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) mc.setScreen(null);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial onClose error: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
