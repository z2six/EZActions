// MainFile: src/main/java/org/z2six/minemenuplus/mixin/KeyboardHandlerAccessor.java
package org.z2six.minemenuplus.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * // MainFile: KeyboardHandlerAccessor.java
 *
 * Invokes the internal GLFW key path that vanilla uses.
 * We call it to synthesize PRESS/RELEASE with our own timing.
 */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
    @Invoker("keyPress")
    void minemenuplus$keyPress(long window, int key, int scancode, int action, int mods);
}
