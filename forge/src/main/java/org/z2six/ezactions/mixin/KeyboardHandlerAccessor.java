package org.z2six.ezactions.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
    @Invoker("keyPress")
    void ezactions$keyPress(long window, int key, int scancode, int action, int mods);
}
