package org.z2six.ezactions.api.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.z2six.ezactions.api.DynamicRadialStyle;
import org.z2six.ezactions.api.EditorOps;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.editor.MenuEditorScreen;

import java.util.ArrayList;
import java.util.List;

final class EditorOpsImpl implements EditorOps {
    @Override
    public void openEditor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreen(new MenuEditorScreen(mc.screen));
    }

    @Override
    public void openConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.setScreen(new org.z2six.ezactions.gui.editor.config.ConfigScreen(mc.screen));
    }

    @Override
    public void openRadial() {
        RadialMenu.open();
    }

    @Override
    public void openRadialAtBundle(String bundleId) {
        RadialMenu.openAtBundle(bundleId);
    }

    @Override
    public boolean openTemporaryRadial(String jsonItemOrArray, DynamicRadialStyle styleOrNull) {
        try {
            JsonElement el = JsonParser.parseString(jsonItemOrArray == null ? "[]" : jsonItemOrArray.trim());
            List<MenuItem> out = new ArrayList<>();
            if (el.isJsonObject()) {
                out.add(JsonCodec.fromJson(el.getAsJsonObject()));
            } else if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement e : arr) {
                    if (e != null && e.isJsonObject()) out.add(JsonCodec.fromJson(e.getAsJsonObject()));
                }
            } else {
                return false;
            }
            RadialMenu.TemporaryStyle style = toStyle(styleOrNull);
            return RadialMenu.openTemporary(out, style);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static RadialMenu.TemporaryStyle toStyle(DynamicRadialStyle in) {
        if (in == null) return null;
        return new RadialMenu.TemporaryStyle(
                in.ringColor(),
                in.hoverColor(),
                in.borderColor(),
                in.textColor(),
                in.animationsEnabled(),
                in.animOpenClose(),
                in.animHover(),
                in.openCloseMs(),
                in.hoverGrowPct(),
                in.openStyle(),
                in.openDirection(),
                in.hoverStyle(),
                in.deadzone(),
                in.baseOuterRadius(),
                in.ringThickness(),
                in.scaleStartThreshold(),
                in.scalePerItem(),
                in.sliceGapDeg(),
                in.designStyle()
        );
    }
}
