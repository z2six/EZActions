// MainFile: src/main/java/org/z2six/minemenuplus/gui/RadialMenuScreen.java
package org.z2six.minemenuplus.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.minemenuplus.Constants;
import org.z2six.minemenuplus.data.menu.MenuItem;
import org.z2six.minemenuplus.data.menu.RadialMenu;
import org.z2six.minemenuplus.gui.noblur.NoMenuBlurScreen;

import java.util.List;

/**
 * // MainFile: RadialMenuScreen.java
 * Same UI as before, but action execution is deferred by one client tick (via InputInjector),
 * and we only close the screen here. (Inventory/chat/advancements now work again.)
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        try {
            g.fill(0, 0, this.width, this.height, 0x55000000);

            List<MenuItem> items = RadialMenu.currentItems();
            int cx = this.width / 2;
            int cy = this.height / 2;

            hoveredIndex = (items == null || items.isEmpty())
                    ? -1
                    : RadialScreenMath.pickSector(mouseX, mouseY, cx, cy, items.size());

            RadialScreenDraw.drawRing(g, this.font, cx, cy, items, hoveredIndex);

            if (items != null && !items.isEmpty() && hoveredIndex >= 0 && hoveredIndex < items.size()) {
                MenuItem mi = items.get(hoveredIndex);
                g.drawCenteredString(this.font, mi.title(), cx, cy - 6, 0xFFFFFF);
            } else {
                String hint = RadialMenu.canGoBack()
                        ? "Right-click: Back | Esc: Close"
                        : "Esc: Close";
                g.drawCenteredString(this.font, hint, cx, cy + 14, 0xAAAAAA);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial render error: {}", Constants.MOD_NAME, t.toString());
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            if (button == 1) {
                if (RadialMenu.canGoBack()) {
                    RadialMenu.goBack();
                    this.minecraft.setScreen(new RadialMenuScreen());
                    return true;
                }
                onClose();
                return true;
            }

            if (button == 0) {
                List<MenuItem> items = RadialMenu.currentItems();
                if (items == null || items.isEmpty()) return true;
                if (hoveredIndex < 0 || hoveredIndex >= items.size()) return true;

                MenuItem mi = items.get(hoveredIndex);

                if (mi.isCategory()) {
                    RadialMenu.enterCategory(mi);
                    this.minecraft.setScreen(new RadialMenuScreen());
                    return true;
                }

                // Close first; ClickAction implementations will handle their own deferral.
                Constants.LOG.info("[{}] Radial: choose action id='{}' title='{}' (closing then deferring)",
                        Constants.MOD_NAME, mi.id(), mi.title());

                onClose();

                // Let the action run (it will use InputInjector which defers actual press/release)
                Minecraft mc = this.minecraft;
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
                return true;
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Radial mouseClicked error: {}", Constants.MOD_NAME, t.toString());
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
