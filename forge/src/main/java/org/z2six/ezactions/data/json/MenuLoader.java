// MainFile: src/main/java/org/z2six/ezactions/data/json/MenuLoader.java
package org.z2six.ezactions.data.json;

import com.google.gson.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.ClickActionCommand;
import org.z2six.ezactions.data.click.ClickActionKey;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.menu.MenuItem;
import org.z2six.ezactions.helper.InputInjector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads/saves the menu structure from JSON in the config directory.
 * Path: {@code <config>/ezactions/menu.json}
 */
public final class MenuLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "menu.json";

    private MenuLoader() {}

    public static Path getMenuPath() {
        Path cfg = FMLPaths.CONFIGDIR.get();
        Path dir = cfg.resolve(Constants.MOD_ID);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Constants.LOG.warn("[{}] Failed to ensure config dir {}: {}", Constants.MOD_NAME, dir, e.toString());
        }
        return dir.resolve(FILE_NAME);
    }

    /** Load menu or create defaults if missing/broken. */
    public static List<MenuItem> loadMenu() {
        Path path = getMenuPath();
        if (!Files.exists(path)) {
            Constants.LOG.info("[{}] Menu file missing; writing defaults to {}", Constants.MOD_NAME, path);
            List<MenuItem> defaults = defaultMenu();
            saveMenu(defaults);
            return defaults;
        }

        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootEl = JsonParser.parseReader(r);
            if (!rootEl.isJsonArray()) throw new JsonParseException("Root must be an array");
            JsonArray arr = rootEl.getAsJsonArray();
            List<MenuItem> items = new ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                items.add(MenuItem.deserialize(el.getAsJsonObject()));
            }
            Constants.LOG.debug("[{}] Loaded {} menu items.", Constants.MOD_NAME, items.size());
            return items;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Failed to load menu.json: {} (writing defaults)", Constants.MOD_NAME, t.toString());
            List<MenuItem> defaults = defaultMenu();
            saveMenu(defaults);
            return defaults;
        }
    }

    public static void saveMenu(List<MenuItem> items) {
        Path path = getMenuPath();
        JsonArray arr = new JsonArray();
        for (MenuItem mi : items) arr.add(mi.serialize());

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            GSON.toJson(arr, w);
            Constants.LOG.debug("[{}] Saved menu to {}", Constants.MOD_NAME, path);
        } catch (IOException e) {
            Constants.LOG.warn("[{}] Failed to save menu.json: {}", Constants.MOD_NAME, e.toString());
        }
    }

    /** Default items (safe even if keys are purged). */
    private static List<MenuItem> defaultMenu() {
        List<MenuItem> out = new ArrayList<>();
        out.add(new MenuItem(
                "inventory",
                "Inventory",
                IconSpec.item("minecraft:chest"),
                new ClickActionKey("key.inventory", /*toggle*/false, InputInjector.DeliveryMode.AUTO),
                List.of()
        ));
        out.add(new MenuItem(
                "advancements",
                "Advancements",
                IconSpec.item("minecraft:book"),
                new ClickActionKey("key.advancements", false, InputInjector.DeliveryMode.AUTO),
                List.of()
        ));
        out.add(new MenuItem(
                "chat",
                "Open Chat",
                IconSpec.item("minecraft:paper"),
                new ClickActionKey("key.chat", false, InputInjector.DeliveryMode.AUTO),
                List.of()
        ));
        out.add(new MenuItem(
                "saytime",
                "Say /time query",
                IconSpec.item("minecraft:clock"),
                new ClickActionCommand("/time query daytime"),
                List.of()
        ));
        return out;
    }
}
