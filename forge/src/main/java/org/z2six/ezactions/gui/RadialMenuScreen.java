// MainFile: src/main/java/org/z2six/ezactions/gui/RadialMenuScreen.java
package org.z2six.ezactions.gui;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.gui.compat.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.config.GeneralClientConfig;
import org.z2six.ezactions.config.RadialAnimConfigView;
import org.z2six.ezactions.config.RadialConfig;
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
 *
 * New:
 * - Uses RadialMenu.visibleItemsForDisplay() so items flagged hideFromMainRadial are
 *   hidden from the root radial, while still existing in the model for bundle hotkeys.
 */
public final class RadialMenuScreen extends EzScreen implements NoMenuBlurScreen {

    private int hoveredIndex = -1;

    // Anim state (open/close + hover)
    private final RadialTransition openTrans = new RadialTransition();
    private final SliceHoverAnim hoverAnim = new SliceHoverAnim();

    public RadialMenuScreen() {
        super(Component.translatable("ezactions.gui.radial.title"));
    }

    @Override
    protected void init() {
        super.init();
        // Start open wipe (temporary API style may override duration)
        org.z2six.ezactions.data.menu.RadialMenu.TemporaryStyle temp = RadialMenu.temporaryStyle();
        if (temp != null && temp.openCloseMs != null) {
            openTrans.startOpening(System.currentTimeMillis(), Math.max(1, temp.openCloseMs));
        } else {
            openTrans.start(+1);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Called by KeyboardHandler on hotkey release (falling edge). */
    public void onHotkeyReleased() {
        try {
            List<MenuItem> items = RadialMenu.visibleItemsForDisplay();
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
        List<MenuItem> items = java.util.List.of();
        int cx = this.width / 2;
        int cy = this.height / 2;
        try {
            items = RadialMenu.visibleItemsForDisplay();

            final int count = (items == null) ? 0 : items.size();
            final Radii rr = RadialScreenMath.computeRadii(count);

            hoveredIndex = (count <= 0)
                    ? -1
                    : RadialScreenMath.pickSector(mouseX, mouseY, cx, cy, count, rr);

            // Tick hover animation state
            hoverAnim.tick(System.currentTimeMillis(), hoveredIndex, count);

            // Decide open/close progress via config
            final RadialAnimConfigView view = RadialAnimConfigView.get();
            final org.z2six.ezactions.data.menu.RadialMenu.TemporaryStyle temp = RadialMenu.temporaryStyle();
            final boolean animationsEnabled = (temp != null && temp.animationsEnabled != null) ? temp.animationsEnabled : view.animationsEnabled;
            final boolean animOpenClose = (temp != null && temp.animOpenClose != null) ? temp.animOpenClose : view.animOpenClose;
            final float openProg = (animationsEnabled && animOpenClose)
                    ? openTrans.progress()
                    : 1.0f;

            // Draw ring with animations wired in
            RadialScreenDraw.drawRing(g, this.font, cx, cy, items, hoveredIndex, rr, hoverAnim, openProg);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial render error: {}", Constants.MOD_NAME, t.toString());
        }

        super.render(g, mouseX, mouseY, partialTick);
        renderHoveredLabel(g, items, cx, cy);
    }

    private void renderHoveredLabel(GuiGraphics g, List<MenuItem> items, int cx, int cy) {
        try {
            if (!GeneralClientConfig.CONFIG.showRadialHoverLabel()) return;
            if (items == null || items.isEmpty()) return;
            if (hoveredIndex < 0 || hoveredIndex >= items.size()) return;

            MenuItem mi = items.get(hoveredIndex);
            if (mi == null) return;
            Component comp = mi.titleComponent();
            if (comp == null || comp.getString().isEmpty()) return;

            int textW = this.font.width(comp.getVisualOrderText());
            int x = cx - (textW / 2);
            int y = cy - (this.font.lineHeight / 2);
            int padX = 6;
            int padY = 3;
            g.fill(x - padX, y - padY, x + textW + padX, y + this.font.lineHeight + padY, 0xB0000000);

            RadialMenu.TemporaryStyle temp = RadialMenu.temporaryStyle();
            int textColor = (temp != null && temp.textColor != null) ? temp.textColor : RadialConfig.get().textColor;
            g.drawString(this.font, comp, x, y, textColor, false);
        } catch (Throwable ignored) {}
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
                List<MenuItem> items = RadialMenu.visibleItemsForDisplay();
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
            Constants.LOG.debug("[{}] Radial: execute action id='{}' title='{}' (closing then deferring)",
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
            net.minecraft.client.gui.screens.Screen ret = RadialMenu.onRadialClosed();
            if (mc != null) mc.setScreen(ret);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial onClose error: {}", Constants.MOD_NAME, t.toString());
        }
    }
}



