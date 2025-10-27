package org.z2six.ezactions.api.internal;

import com.google.gson.*;
import org.z2six.ezactions.api.ImportExport;
import org.z2six.ezactions.api.MenuPath;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.data.menu.RadialMenu;

import java.util.List;
import java.util.Optional;

final class ImportExportImpl implements ImportExport {

    @Override
    public String exportAllJson() {
        List<MenuItem> root = RadialMenu.rootMutable();
        JsonArray arr = JsonCodec.toJsonArray(root);
        return new GsonBuilder().setPrettyPrinting().create().toJson(arr);
    }

    @Override
    public String exportBundleJson(MenuPath path) {
        List<MenuItem> root = RadialMenu.rootMutable();
        List<MenuItem> at = (path == null || path.titles().isEmpty())
                ? root
                : TreeOps.findBundleByTitles(root, path.titles());
        JsonArray arr = JsonCodec.toJsonArray(at);
        return new GsonBuilder().setPrettyPrinting().create().toJson(arr);
    }

    @Override
    public int importInto(MenuPath path, String json) {
        try {
            JsonElement el = JsonParser.parseString(json == null ? "[]" : json.trim());
            JsonArray arr = el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
            List<MenuItem> dst = (path == null || path.titles().isEmpty())
                    ? RadialMenu.rootMutable()
                    : TreeOps.findBundleByTitles(RadialMenu.rootMutable(), path.titles());
            if (dst == null) return 0;

            int count = 0;
            for (JsonElement e : arr) {
                if (e.isJsonObject()) {
                    MenuItem mi = JsonCodec.fromJson(e.getAsJsonObject());
                    dst.add(mi);
                    count++;
                }
            }
            if (count > 0) try { RadialMenu.persist(); } catch (Throwable ignored) {}
            return count;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] importInto failed: {}", Constants.MOD_NAME, t.toString());
            return 0;
        }
    }

    @Override
    public int replaceAll(String json) {
        try {
            JsonElement el = JsonParser.parseString(json == null ? "[]" : json.trim());
            List<MenuItem> root = RadialMenu.rootMutable();
            root.clear();

            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    if (e.isJsonObject()) root.add(JsonCodec.fromJson(e.getAsJsonObject()));
                }
            } else if (el.isJsonObject()) {
                root.add(JsonCodec.fromJson(el.getAsJsonObject()));
            }
            try { RadialMenu.persist(); } catch (Throwable ignored) {}
            return root.size();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] replaceAll failed: {}", Constants.MOD_NAME, t.toString());
            return 0;
        }
    }

    @Override
    public Optional<String> validate(String json) {
        try {
            // Basic shape validation only (we don’t know your full schema constraints yet).
            JsonElement el = JsonParser.parseString(json == null ? "" : json.trim());
            if (!el.isJsonObject() && !el.isJsonArray()) {
                return Optional.of("JSON must be an object or array.");
            }
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of("Invalid JSON: " + t.getMessage());
        }
    }
}
