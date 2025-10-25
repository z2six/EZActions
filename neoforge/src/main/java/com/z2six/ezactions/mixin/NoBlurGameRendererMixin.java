// MainFile: src/main/java/org/z2six/ezactions/mixin/NoBlurGameRendererMixin.java
package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

/**
 * // MainFile: NoBlurGameRendererMixin.java
 * Cancels the menu blur pass for our screens.
 * Primary check: implements NoMenuBlurScreen.
 * Secondary safety: package-prefix check to catch any new screens we add.
 */
@Mixin(GameRenderer.class)
public abstract class NoBlurGameRendererMixin {

    @Inject(method = {"processBlurEffect", "renderBlur"}, at = @At("HEAD"), cancellable = true)
    private void ezactions$skipMenuBlur(float delta, CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Screen s = mc.screen;
            if (s == null) return;

            // Primary: explicit marker
            if (s instanceof NoMenuBlurScreen) {
                ci.cancel();
                return;
            }
            // Secondary: any of our screens, even if someone forgot the marker
            String cn = s.getClass().getName();
            if (cn != null && cn.startsWith("org.z2six.ezactions.")) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // Never break rendering; just log.
            Constants.LOG.warn("[{}] NoBlur mixin failed: {}", Constants.MOD_NAME, t.toString());
        }
    }
}
