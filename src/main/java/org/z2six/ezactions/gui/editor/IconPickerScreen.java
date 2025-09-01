// MainFile: src/main/java/org/z2six/ezactions/gui/editor/IconPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.IconRenderer;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * // MainFile: IconPickerScreen.java
 * Scrollable icon grid (mouse wheel + visible scrollbar) with a text filter (EditBox).
 * Returns IconSpec via callback (onPick).
 * Defensive logging; never crashes.
 */
public final class IconPickerScreen extends Screen implements NoMenuBlurScreen {

    private final Screen parent;
    private final Consumer<IconSpec> onPick;
    private final List<String> allIcons = new ArrayList<>();
    private String filter = "";
    private double scrollY = 0;
    private EditBox filterBox;

    // layout
    private static final int PADDING = 12;
    private static final int CELL = 24;
    private static final int GAP = 8;

    public IconPickerScreen(Screen parent, Consumer<IconSpec> onPick) {
        super(Component.literal("Choose Icon"));
        this.parent = parent;
        this.onPick = onPick;
    }

    public static void open(Screen parent, Consumer<IconSpec> onPick) {
        Minecraft.getInstance().setScreen(new IconPickerScreen(parent, onPick));
    }

    @Override
    protected void init() {
        try {
            filterBox = new EditBox(this.font, PADDING, PADDING,
                    Math.max(120, this.width - PADDING * 2 - 20), 18, Component.literal("Filter"));
            filterBox.setValue(filter);
            filterBox.setResponder(s -> filter = s);
            addRenderableWidget(filterBox);

            // populate minecraft:item ids
            var reg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            for (var e : reg.entrySet()) {
                ResourceLocation id = e.getKey().location();
                allIcons.add(id.getNamespace() + ":" + id.getPath());
            }
            allIcons.sort(String::compareToIgnoreCase);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] IconPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // NOTE: signatures differ a bit across versions; avoid @Override to keep it robust
    public boolean mouseScrolled(double mx, double my, double delta) {
        double content = contentHeight();
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - delta * 32.0, 0, content - view);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int left = PADDING;
        int top = PADDING + 24; // below filter
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int x0 = left;
        int y0 = (int) (top - scrollY);

        List<String> filtered = filtered();
        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x0 + col * (CELL + GAP);
            int cy = y0 + row * (CELL + GAP);
            if (mx >= cx && mx <= cx + CELL && my >= cy && my <= cy + CELL) {
                String id = filtered.get(i);
                try {
                    onPick.accept(IconSpec.item(id));
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Icon onPick failed: {}", Constants.MOD_NAME, t.toString());
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);

        int left = PADDING;
        int top = PADDING + 24;
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int x0 = left;
        int y0 = (int) (top - scrollY);

        List<String> filtered = filtered();
        for (int i = 0; i < filtered.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x0 + col * (CELL + GAP);
            int cy = y0 + row * (CELL + GAP);
            IconRenderer.drawIcon(g, cx + CELL / 2, cy + CELL / 2, IconSpec.item(filtered.get(i)));
        }

        drawScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // helpers

    private List<String> filtered() {
        if (filter == null || filter.isBlank()) return allIcons;
        String f = filter.toLowerCase(Locale.ROOT);
        return allIcons.stream().filter(s -> s.contains(f)).toList();
    }

    private int viewHeight() {
        return height - (PADDING + 24) - PADDING;
    }

    private double contentHeight() {
        int cols = Math.max(1, (width - PADDING * 2) / (CELL + GAP));
        int rows = (int) Math.ceil(filtered().size() / (double) cols);
        return rows * (CELL + GAP);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void drawScrollbar(GuiGraphics g) {
        double content = contentHeight();
        int view = viewHeight();
        if (content <= view) return;

        int barX = width - PADDING - 6;
        int barY = PADDING + 24;
        int barH = view;
        g.fill(barX, barY, barX + 6, barY + barH, 0x40000000);

        double ratio = view / content;
        int knobH = Math.max(20, (int) (barH * ratio));
        int knobY = (int) (barY + (barH - knobH) * (scrollY / (content - view)));

        g.fill(barX + 1, knobY, barX + 5, knobY + knobH, 0x80FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double content = contentHeight();
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - deltaY * 32.0, 0, Math.max(0, content - view));
        }
        return true;
    }

}
