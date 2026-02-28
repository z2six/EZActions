package org.z2six.ezactions.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Themed cycle button used instead of vanilla CycleButton in editor/config screens. */
public final class EditorCycleButton<T> extends AbstractButton {

    private final Component label;
    private final List<T> values;
    private final Function<T, Component> valueText;
    private final Consumer<T> onValueChanged;
    private int index;

    public EditorCycleButton(
            int x,
            int y,
            int w,
            int h,
            Component label,
            List<T> values,
            T initialValue,
            Function<T, Component> valueText,
            Consumer<T> onValueChanged
    ) {
        super(x, y, w, h, Component.empty());
        this.label = label == null ? Component.empty() : label;
        this.values = values == null ? List.of() : new ArrayList<>(values);
        this.valueText = valueText == null ? v -> Component.literal(String.valueOf(v)) : valueText;
        this.onValueChanged = onValueChanged;
        this.index = resolveInitialIndex(initialValue);
        refreshMessage();
    }

    private int resolveInitialIndex(T initialValue) {
        if (values.isEmpty()) return 0;
        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(values.get(i), initialValue)) return i;
        }
        return 0;
    }

    @Override
    public void onPress() {
        if (!active || values.isEmpty()) return;
        index = (index + 1) % values.size();
        refreshMessage();
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!active || values.isEmpty()) return false;
        if (!isHoveredOrFocused()) return false;
        if (delta == 0) return false;
        int dir = delta > 0 ? -1 : 1;
        index = (index + dir) % values.size();
        if (index < 0) index += values.size();
        refreshMessage();
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
        return true;
    }

    public T getValue() {
        if (values.isEmpty()) return null;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }

    public void setValue(T value) {
        int i = resolveInitialIndex(value);
        if (i == index) return;
        index = i;
        refreshMessage();
    }

    private void refreshMessage() {
        Component v = valueText.apply(getValue());
        String left = label == null ? "" : label.getString();
        if (left.isEmpty()) {
            setMessage(v == null ? Component.empty() : v);
            return;
        }
        setMessage(Component.translatable("ezactions.gui.common.cycle_format", label, (v == null ? Component.empty() : v)));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isHoveredOrFocused();

        int border = !active ? 0x66444444 : 0xAA3A3A3A;
        int bgTop = !active ? 0x66222222 : (hovered ? 0xFF1E1E1E : 0xF0141414);
        int bgBottom = !active ? 0x661A1A1A : (hovered ? 0xFF181818 : 0xF0101010);
        int txt = !active ? 0x88969696 : (hovered ? 0xFFF7F7F7 : 0xFFE4E4E4);
        int cornerAccent = (!active || !hovered) ? 0xFF000000 : ActionEditorUi.ACCENT;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fillGradient(x, y, x + w, y + h, bgTop, bgBottom);
        drawInnerCorners(g, x, y, w, h, cornerAccent);

        Font font = Minecraft.getInstance().font;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawCenteredString(font, getMessage(), x + (w / 2), ty, txt);
    }

    private static void drawInnerCorners(GuiGraphics g, int x, int y, int w, int h, int color) {
        int in = 2;
        int len = 4;

        int lx = x + in;
        int rx = x + w - 1 - in;
        int ty = y + in;
        int by = y + h - 1 - in;

        // top-left
        g.fill(lx, ty, lx + 1, ty + len, color);
        g.fill(lx, ty, lx + len, ty + 1, color);
        // top-right
        g.fill(rx, ty, rx + 1, ty + len, color);
        g.fill(rx - len + 1, ty, rx + 1, ty + 1, color);
        // bottom-left
        g.fill(lx, by - len + 1, lx + 1, by + 1, color);
        g.fill(lx, by, lx + len, by + 1, color);
        // bottom-right
        g.fill(rx, by - len + 1, rx + 1, by + 1, color);
        g.fill(rx - len + 1, by, rx + 1, by + 1, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

