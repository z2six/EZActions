// MainFile: src/main/java/org/z2six/ezactions/mixin/NoBlurGameRendererMixin.java
package org.z2six.ezactions.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

/**
 * Cancels the menu blur pass for our screens only.
 * - Lower priority so shader/renderer mods can hook first.
 * - Optional injections (require=0/expect=0) so we never hard-fail if names change.
 */
@Mixin(value = GameRenderer.class, priority = 100) // default is 1000; lower number = applied later
public abstract class NoBlurGameRendererMixin {
    /*
    // Mapping 1: present on some 1.21.1 mappings
    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void ezactions$skipMenuBlur$process(float delta, CallbackInfo ci) {
        cancelIfOurScreen(ci);
    }

    // Mapping 2: present on other mapping sets / forks
    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void ezactions$skipMenuBlur$render(float delta, CallbackInfo ci) {
        cancelIfOurScreen(ci);
    }

    private static void cancelIfOurScreen(CallbackInfo ci) {
        Screen s = Minecraft.getInstance().screen;
        if (s == null) return;

        if (s instanceof NoMenuBlurScreen) {
            ci.cancel();
            return;
        }
        // Safety: any of our screens, in case someone forgets the marker interface
        String cn = s.getClass().getName();
        if (cn != null && cn.startsWith("org.z2six.ezactions.")) {
            ci.cancel();
        }
    }

     */
}
