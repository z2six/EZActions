// MainFile: neoforge/src/main/java/org/z2six/ezactions/mixin/NoBlurGameRendererMixin.java
package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disable blur while ANY EZActions GUI is open.
 *
 * Policy:
 * - If the current screen class is in package "org.z2six.ezactions.", cancel blur.
 * - Otherwise do nothing (vanilla + other mods blur work normally).
 *
 * No logging; fail-safe (never crash render loop).
 */
@Mixin(value = GameRenderer.class, priority = 100) // lower number = applied later than default 1000
public abstract class NoBlurGameRendererMixin {

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void ezactions$skipBlur_process(float delta, CallbackInfo ci) {
        cancelIfEzActionsScreen(ci);
    }

    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void ezactions$skipBlur_render(float delta, CallbackInfo ci) {
        cancelIfEzActionsScreen(ci);
    }

    private static void cancelIfEzActionsScreen(CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            Screen s = mc.screen;
            if (s == null) return;

            // Disable blur for any of our GUI screens.
            String cn = s.getClass().getName();
            if (cn != null && cn.startsWith("org.z2six.ezactions.")) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Intentionally swallow: render loop must never crash due to our mixin.
        }
    }
}
