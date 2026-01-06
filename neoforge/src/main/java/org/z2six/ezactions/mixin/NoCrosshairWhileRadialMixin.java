// MainFile: neoforge/src/main/java/org/z2six/ezactions/mixin/NoCrosshairWhileRadialMixin.java
package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
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
 * NeoForge / MC 1.21.x may use a DeltaTracker parameter for HUD rendering.
 * This mixin supports both:
 * - renderCrosshair(GuiGraphics)
 * - renderCrosshair(GuiGraphics, DeltaTracker)
 *
 * We keep require=0 so the non-matching variant is safely ignored.
 */
@Mixin(Gui.class)
public abstract class NoCrosshairWhileRadialMixin {

    // Throttle debug logs to avoid spamming every frame.
    private static long ezactions$lastLogMs = 0L;

    /**
     * 1.21.x signature (most likely on NeoForge 1.21.1):
     * void renderCrosshair(GuiGraphics, DeltaTracker)
     */
    @Inject(
            method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void ezactions$hideCrosshairWhileRadial_121(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        cancelIfRadialOpen(ci, "renderCrosshair(GuiGraphics,DeltaTracker)");
    }

    /**
     * Older signature (kept for safety / dev env variance):
     * void renderCrosshair(GuiGraphics)
     */
    @Inject(
            method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void ezactions$hideCrosshairWhileRadial_legacy(GuiGraphics graphics, CallbackInfo ci) {
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
                    Constants.LOG.debug("[{}] Crosshair suppressed via {} because RadialMenuScreen is open.",
                            Constants.MOD_NAME, hookName);
                }
            }
        } catch (Throwable t) {
            // Never crash render loop; just skip and keep going.
            try {
                Constants.LOG.debug("[{}] Crosshair suppression failed ({}): {}", Constants.MOD_NAME, hookName, t.toString());
            } catch (Throwable ignored) {
            }
        }
    }
}
