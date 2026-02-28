// MainFile: src/main/java/org/z2six/ezactions/data/json/MenuImportExport.java
package org.z2six.ezactions.data.json;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.util.ClipboardIO;

import java.util.ArrayList;
import java.util.List;

/**
 * Import/Export helpers for the radial menu model.
 *
 * Export:
 *  - Reads the live model from RadialMenu.rootMutable()
 *  - Serializes with MenuItem.serialize()
 *  - Pretty-prints to JSON and copies to system clipboard
 *
 * Import:
 *  - Reads text from clipboard
 *  - Parses as JSON array
 *  - Validates by deserializing each element via MenuItem.deserialize()
 *  - On full success, replaces root and persists to disk
 *
 * All methods are crash-safe: they catch and log exceptions and return booleans.
 */
public final class MenuImportExport {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MenuImportExport() {}

    /**
     * Export current menu to clipboard as pretty JSON (same shape as menu.json).
     * @return number of items exported (>=0) on success, -1 on failure.
     */
    public static int exportToClipboard() {
        try {
            List<MenuItem> root = RadialMenu.rootMutable();
            if (root == null) {
                Constants.LOG.warn("[{}] Export: root is null.", Constants.MOD_NAME);
                return -1;
            }

            JsonArray arr = new JsonArray();
            for (MenuItem mi : root) {
                try {
                    JsonObject o = mi.serialize();
                    if (o != null) arr.add(o);
                } catch (Throwable t) {
                    // Skip a single bad item but keep export going
                    Constants.LOG.warn("[{}] Export: skipping item due to serialize error: {}", Constants.MOD_NAME, t.toString());
                }
            }

            String json = GSON.toJson(arr);
            boolean ok = ClipboardIO.setClipboard(json);
            if (!ok) {
                Constants.LOG.warn("[{}] Export: failed to write to clipboard.", Constants.MOD_NAME);
                return -1;
            }

            Constants.LOG.info("[{}] Exported {} menu items to clipboard.", Constants.MOD_NAME, arr.size());
            safeStatusMessage(Component.translatable("ezactions.message.export_success", arr.size()));
            return arr.size();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Export failed: {}", Constants.MOD_NAME, t.toString());
            safeStatusMessage(Component.translatable("ezactions.message.export_failed"));
            return -1;
        }
    }

    /**
     * Import menu from clipboard JSON.
     * The clipboard must contain a JSON array with objects accepted by MenuItem.deserialize().
     *
     * @return number of items imported (>=0) on success, -1 on failure.
     */
    public static int importFromClipboard() {
        try {
            String text = ClipboardIO.getClipboard();
            if (text == null || text.trim().isEmpty()) {
                Constants.LOG.info("[{}] Import: clipboard empty.", Constants.MOD_NAME);
                safeStatusMessage(Component.translatable("ezactions.message.clipboard_empty"));
                return -1;
            }

            JsonElement rootEl;
            try {
                rootEl = com.google.gson.JsonParser.parseString(text);
            } catch (Throwable parseEx) {
                Constants.LOG.info("[{}] Import: clipboard not JSON: {}", Constants.MOD_NAME, parseEx.toString());
                safeStatusMessage(Component.translatable("ezactions.message.clipboard_not_json"));
                return -1;
            }

            if (!rootEl.isJsonArray()) {
                Constants.LOG.info("[{}] Import: root must be an array.", Constants.MOD_NAME);
                safeStatusMessage(Component.translatable("ezactions.message.import_root_not_array"));
                return -1;
            }

            JsonArray arr = rootEl.getAsJsonArray();
            List<MenuItem> fresh = new ArrayList<>(arr.size());

            // Validate & construct all first (atomic replace)
            int idx = 0;
            for (JsonElement el : arr) {
                idx++;
                if (!el.isJsonObject()) {
                    Constants.LOG.info("[{}] Import: entry #{} is not an object, aborting.", Constants.MOD_NAME, idx);
                    safeStatusMessage(Component.translatable("ezactions.message.import_entry_not_object", idx));
                    return -1;
                }
                JsonObject obj = el.getAsJsonObject();
                try {
                    MenuItem mi = MenuItem.deserialize(obj);
                    if (mi == null) throw new JsonParseException("deserialize returned null");
                    fresh.add(mi);
                } catch (Throwable t) {
                    Constants.LOG.info("[{}] Import: entry #{} invalid: {}", Constants.MOD_NAME, idx, t.toString());
                    safeStatusMessage(Component.translatable("ezactions.message.import_entry_invalid", idx));
                    return -1;
                }
            }

            // Success: replace model and persist
            try {
                List<MenuItem> live = RadialMenu.rootMutable();
                live.clear();
                live.addAll(fresh);
                RadialMenu.persist();
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Import: failed to persist: {}", Constants.MOD_NAME, t.toString());
                safeStatusMessage(Component.translatable("ezactions.message.import_failed_saving"));
                return -1;
            }

            Constants.LOG.info("[{}] Imported {} items from clipboard.", Constants.MOD_NAME, fresh.size());
            safeStatusMessage(Component.translatable("ezactions.message.import_success", fresh.size()));
            return fresh.size();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Import failed: {}", Constants.MOD_NAME, t.toString());
            safeStatusMessage(Component.translatable("ezactions.message.import_failed"));
            return -1;
        }
    }

    // --- UI feedback (non-fatal if MC not ready) ----------------------------

    private static void safeStatusMessage(Component msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gui != null) {
                // Small unobtrusive status line (top-right overlay alternative)
                mc.gui.setOverlayMessage(msg, false);
            }
        } catch (Throwable ignored) {
            // last-resort: nothing; logs are already written above
        }
    }
}
