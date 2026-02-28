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
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!active || values.isEmpty()) return false;
        if (!isHoveredOrFocused()) return false;
        if (deltaY == 0) return false;
        int dir = deltaY > 0 ? -1 : 1;
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

        int border = !active ? 0x6640556D : 0xAA2C425B;
        int bgTop = !active ? 0x66333F4C : (hovered ? 0xF429435F : 0xE61B2D42);
        int bgBottom = !active ? 0x66303B47 : (hovered ? 0xF422394F : 0xE6152537);
        int txt = !active ? 0x8896A9BE : (hovered ? 0xFFF4FAFF : 0xFFE2EDF9);
        int topAccent = !active ? 0x406E7C8C : (hovered ? 0xFF66B5FF : 0x50FFFFFF);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fillGradient(x, y, x + w, y + h, bgTop, bgBottom);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, topAccent);

        Font font = Minecraft.getInstance().font;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawCenteredString(font, getMessage(), x + (w / 2), ty, txt);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
