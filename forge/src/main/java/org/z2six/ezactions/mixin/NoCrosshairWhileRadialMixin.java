// MainFile: forge/src/main/java/org/z2six/ezactions/mixin/NoCrosshairWhileRadialMixin.java
package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.RadialMenuScreen;

/**
 * Hides the vanilla crosshair only while EZActions' radial menu is open.
 *
 * Forge / MC 1.20.1:
 * - Gui#renderCrosshair(GuiGraphics) is the relevant method.
 *
 * IMPORTANT:
 * Do NOT reference 1.21+ overloads (DeltaTracker) on Forge 1.20.1, because
 * they do not exist and will cause mapping/compile errors even with require=0.
 */
@Mixin(Gui.class)
public abstract class NoCrosshairWhileRadialMixin {

    // Throttle debug logs to avoid spamming every frame.
    private static long ezactions$lastLogMs = 0L;

    /**
     * 1.20.1 signature:
     * void renderCrosshair(GuiGraphics)
     */
    @Inject(
            method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ezactions$hideCrosshairWhileRadial_1201(GuiGraphics graphics, CallbackInfo ci) {
        cancelIfRadialOpen(ci, "renderCrosshair(GuiGraphics)");
    }

    private static void cancelIfRadialOpen(CallbackInfo ci, String hookName) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            if (mc.screen instanceof RadialMenuScreen) {
                ci.cancel();

                long now = System.currentTimeMillis();
                if (now - ezactions$lastLogMs > 2000L) {
                    ezactions$lastLogMs = now;
                    try {
                        Constants.LOG.debug("[{}] Crosshair suppressed via {} because RadialMenuScreen is open.",
                                Constants.MOD_NAME, hookName);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            // Never crash render loop; just skip and keep going.
            try {
                Constants.LOG.debug("[{}] Crosshair suppression failed ({}): {}", Constants.MOD_NAME, hookName, t.toString());
            } catch (Throwable ignored) {}
        }
    }
}
