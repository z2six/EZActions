package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.RadialMenuScreen;

@Mixin(Gui.class)
public abstract class NoCrosshairWhileRadialMixin {

    private static long ezactions$lastLogMs = 0L;

    @Inject(
            method = "renderCrosshair(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ezactions$hideCrosshairWhileRadial_1192(PoseStack poseStack, CallbackInfo ci) {
        cancelIfRadialOpen(ci, "renderCrosshair(PoseStack)");
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
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            try {
                Constants.LOG.debug("[{}] Crosshair suppression failed ({}): {}", Constants.MOD_NAME, hookName, t.toString());
            } catch (Throwable ignored) {
            }
        }
    }
}
