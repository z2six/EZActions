// MainFile: src/main/java/org/z2six/ezactions/data/click/ClickActionKey.java
package org.z2six.ezactions.data.click;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.helper.InputInjector;

import java.util.Objects;

/**
 * // MainFile: ClickActionKey.java
 *
 * Executes a KeyMapping by name (translation key like "key.inventory" or localized label "Inventory").
 * Supports AUTO/INPUT/TICK delivery and optional toggle.
 */
public final class ClickActionKey implements IClickAction {

    private final String mappingName; // e.g. "key.inventory" or "Inventory"
    private final boolean toggle;
    private final InputInjector.DeliveryMode mode;

    public ClickActionKey(String mappingName, boolean toggle, InputInjector.DeliveryMode mode) {
        this.mappingName = Objects.requireNonNull(mappingName);
        this.toggle = toggle;
        this.mode = (mode == null) ? InputInjector.DeliveryMode.AUTO : mode;
    }

    public String mappingName() { return mappingName; }
    public boolean toggle() { return toggle; }
    public InputInjector.DeliveryMode mode() { return mode; }

    @Override
    public boolean execute(Minecraft mc) {
        try {
            Constants.LOG.debug("[{}] Key tap: '{}'", Constants.MOD_NAME, mappingName);
            return InputInjector.deliver(mappingName, toggle, mode);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ClickActionKey execute failed '{}': {}", Constants.MOD_NAME, mappingName, t.toString());
            return false;
        }
    }

    @Override
    public ClickActionType getType() {
        return ClickActionType.KEY;
    }

    @Override
    public String getId() {
        return "key:" + mappingName;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(mappingName);
    }

    /* ---------- JSON helpers, expected by MenuLoader ---------- */

    public com.google.gson.JsonObject serialize() {
        var o = new com.google.gson.JsonObject();
        o.addProperty("type", "KEY");
        o.addProperty("name", mappingName);
        o.addProperty("toggle", toggle);
        o.addProperty("mode", mode.name());
        return o;
    }

    public static ClickActionKey deserialize(com.google.gson.JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : "key.inventory";
        boolean tog = o.has("toggle") && o.get("toggle").getAsBoolean();
        InputInjector.DeliveryMode dm = InputInjector.DeliveryMode.AUTO;
        if (o.has("mode")) {
            try { dm = InputInjector.DeliveryMode.valueOf(o.get("mode").getAsString()); }
            catch (IllegalArgumentException ignored) {}
        }
        return new ClickActionKey(name, tog, dm);
    }
}
