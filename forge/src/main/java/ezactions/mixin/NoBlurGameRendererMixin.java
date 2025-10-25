// MainFile: src/main/java/org/z2six/ezactions/mixin/NoBlurGameRendererMixin.java
package ezactions.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.z2six.ezactions.Constants;

/**
 * // MainFile: NoBlurGameRendererMixin.java
 *
 * Forge 1.20.1 port note:
 * Vanilla 1.20.1 has NO menu-background blur pass (that feature arrives in 1.21+).
 * To keep behavior 100% identical to vanilla (no blur differences) AND avoid
 * descriptor/mapping warnings, this mixin is intentionally inert on 1.20.1.
 *
 * We keep the Mixin class so the mixin config doesn't need branching by MC version.
 * If/when targeting 1.21+, the NeoForge/1.21 mixin that cancels GameRenderer's
 * blur pass should be used there instead.
 *
 * Defensive logging included; no injections, no behavior changes.
 */
@Mixin(GameRenderer.class)
public abstract class NoBlurGameRendererMixin {
    static {
        try {
            Constants.LOG.debug("[{}] NoBlurGameRendererMixin: inert on Forge 1.20.1 (no vanilla menu blur to cancel).", Constants.MOD_NAME);
        } catch (Throwable t) {
            // Never fail class load due to logging issues.
        }
    }
}
