// MainFile: src/main/java/org/z2six/ezactions/gui/IconRenderer.java
package org.z2six.ezactions.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.util.CustomIconManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders IconSpec to the screen. Currently supports ITEM icons.
 * Uses ResourceLocation.tryParse for 1.21.x compatibility and falls back safely.
 */
public final class IconRenderer {

    private IconRenderer() {}
    private static final Map<String, ItemStack> ITEM_STACK_CACHE = new ConcurrentHashMap<>();

    public static void drawIcon(GuiGraphics g, int x, int y, IconSpec icon) {
        try {
            if (icon == null) {
                drawItem(g, x, y, new ItemStack(getFallbackItem()));
                return;
            }
            switch (icon.kind()) {
                case ITEM -> {
                    ItemStack stack = cachedStack(icon.id());
                    drawItem(g, x, y, stack);
                }
                case CUSTOM -> {
                    ResourceLocation tex = CustomIconManager.textureForId(icon.id());
                    if (tex != null) {
                        g.blit(tex, x - 8, y - 8, 0.0f, 0.0f, 16, 16, 16, 16);
                    } else {
                        drawItem(g, x, y, new ItemStack(getFallbackItem()));
                    }
                }
                default -> drawItem(g, x, y, new ItemStack(getFallbackItem()));
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconRenderer error for '{}': {}", Constants.MOD_NAME,
                    icon == null ? "<null>" : icon.toString(), t.toString());
            drawItem(g, x, y, new ItemStack(getFallbackItem()));
        }
    }

    private static Item resolveItem(String id) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) return getFallbackItem();
            Item it = BuiltInRegistries.ITEM.get(rl);
            return it == null ? getFallbackItem() : it;
        } catch (Throwable t) {
            return getFallbackItem();
        }
    }

    private static ItemStack cachedStack(String id) {
        String key = (id == null || id.isBlank()) ? "minecraft:barrier" : id;
        return ITEM_STACK_CACHE.computeIfAbsent(key, k -> new ItemStack(resolveItem(k)));
    }

    private static Item getFallbackItem() {
        // Simple and bulletproof for 1.21.x
        return Items.BARRIER;
    }

    private static void drawItem(GuiGraphics g, int x, int y, ItemStack stack) {
        g.renderItem(stack, x - 8, y - 8); // center around (x,y)
    }
}
