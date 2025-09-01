package org.z2six.minemenuplus.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.menu.MenuItem;
import org.z2six.minemenuplus.data.menu.RadialMenu;
import org.z2six.minemenuplus.gui.RadialScreenMath.Radii;
import org.z2six.minemenuplus.handler.KeyboardHandler;
import org.z2six.minemenuplus.gui.noblur.NoMenuBlurScreen;

import java.util.List;

/**
 * Radial menu:
 * - No screen overlay, game continues; mouse captured for selection.
 * - Hold-to-open. Release = execute hovered action (non-category).
 * - LMB on action: close+execute; LMB on category: drill in (stay open).
 * - RMB: go back.
 */
public final class RadialMenuScreen extends Screen implements NoMenuBlurScreen {

    private int hoveredIndex = -1;

    public RadialMenuScreen() {
        super(Component.literal("MineMenuPlus Radial"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Called by KeyboardHandler on hotkey release (falling edge). */
    public void onHotkeyReleased() {
        try {
            List<MenuItem> items = RadialMenu.currentItems();
            if (items != null && !items.isEmpty()
                    && hoveredIndex >= 0 && hoveredIndex < items.size()) {
                MenuItem mi = items.get(hoveredIndex);
                if (!mi.isCategory()) {
                    // Execute action on release
                    KeyboardHandler.suppressReopenUntilReleased();
                    executeAndClose(mi);
                    return;
                }
            }
            // Nothing actionable hovered → just close
            Minecraft.getInstance().setScreen(null);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] onHotkeyReleased error: {}", Constants.MOD_NAME, t.toString());
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        try {
            // No backdrop fill: game remains fully visible
            List<MenuItem> items = RadialMenu.currentItems();
            int cx = this.width / 2;
            int cy = this.height / 2;

            Radii rr = RadialScreenMath.computeRadii(items == null ? 0 : items.size());

            hoveredIndex = (items == null || items.isEmpty())
                    ? -1
                    : RadialScreenMath.pickSector(mouseX, mouseY, cx, cy, items.size(), rr);

            RadialScreenDraw.drawRing(g, this.font, cx, cy, items, hoveredIndex, rr);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial render error: {}", Constants.MOD_NAME, t.toString());
        }

        // DO NOT draw any hint text in the center
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
