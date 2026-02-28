package org.z2six.ezactions.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.z2six.ezactions.Constants;

/*
 * Forge 1.20.1 note:
 * Vanilla 1.20.1 has no menu blur pass (this appears in 1.21+), so this mixin
 * is intentionally inert to keep behavior identical and avoid invalid targets.
 */
@Mixin(GameRenderer.class)
public abstract class NoBlurGameRendererMixin {
    static {
        try {
            Constants.LOG.debug("[{}] NoBlurGameRendererMixin inert on Forge 1.20.1.", Constants.MOD_NAME);
        } catch (Throwable ignored) {
        }
    }
}
