// MainFile: src/main/java/org/z2six/ezactions/data/menu/MenuItem.java
package org.z2six.ezactions.data.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.click.IClickAction;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.data.json.ClickActionSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable-ish menu entry: either an action (has IClickAction, children empty)
 * or a category/page (action == null, may have children).
 *
 * IMPORTANT:
 *  - children is backed by a mutable list so the editor can mutate it in-place
 *    via childrenMutable(). This fixes "adding inside a category does nothing".
 *  - children() returns an unmodifiable view for read-only use.
 */
public final class MenuItem {

    private final String id;
    private final String title;         // plain label
    private final IconSpec icon;        // visual icon spec
    private final IClickAction action;  // null => category
    private final List<MenuItem> children; // backing, mutable list for categories

    public MenuItem(String id,
                    String title,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children) {
        this.id = Objects.requireNonNullElse(id, "item_" + Long.toUnsignedString(System.nanoTime(), 36));
        this.title = Objects.requireNonNullElse(title, "Unnamed");
        this.icon = icon == null ? IconSpec.item("minecraft:stone") : icon;
        this.action = action; // nullable => category

        // Backing, MUTABLE list (no unmodifiable wrapper here!)
        if (children == null) {
            this.children = new ArrayList<>();
        } else {
            this.children = new ArrayList<>(children);
        }
    }

    // -------- Accessors --------
    public String id() { return id; }
    public String title() { return title; }
    public IconSpec icon() { return icon; }
    public IClickAction action() { return action; }
    // Alias retained for older call sites:
    public IClickAction clickAction() { return action; }

    /** Read-only children view. */
    public List<MenuItem> children() {
        return Collections.unmodifiableList(children);
    }

    /** Editor/RadialMenu needs to mutate the actual list. */
    public List<MenuItem> childrenMutable() {
        return children; // <-- backing list, not a copy
    }

    public boolean isCategory() {
        return action == null;
    }

    // -------- Updaters --------

    /** Return a copy with a different icon. Never crashes. */
    public MenuItem withIcon(IconSpec newIcon) {
        try {
            IconSpec use = (newIcon == null) ? IconSpec.item("minecraft:stone") : newIcon;
            // keep current children (backing list) when copying
            return new MenuItem(this.id, this.title, use, this.action, this.children);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] MenuItem.withIcon failed: {}", Constants.MOD_NAME, t.toString());
            return this;
        }
    }

    /** Return a copy with a different title. */
    public MenuItem withTitle(String newTitle) {
        String use = (newTitle == null || newTitle.isBlank()) ? this.title : newTitle;
        return new MenuItem(this.id, use, this.icon, this.action, this.children);
    }

    /** Return a copy with a different action (converts category->action if non-null). */
    public MenuItem withAction(IClickAction newAction) {
        // when this becomes an action, children should be empty
        return new MenuItem(this.id, this.title, this.icon, newAction, Collections.emptyList());
    }

    /** Return a copy with different children (converts to category; action becomes null). */
    public MenuItem withChildren(List<MenuItem> newChildren) {
        return new MenuItem(this.id, this.title, this.icon, null, newChildren);
    }

    // -------- JSON (de)serialization --------

    /** Serialize to JSON used by MenuLoader. */
    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        try {
            o.addProperty("id", this.id);
            o.addProperty("title", this.title);
            // store icon id (string) — IconSpec.item(id) can restore
            String iconId = "minecraft:stone";
            try { iconId = this.icon.id(); } catch (Throwable ignored) {}
            o.addProperty("icon", iconId);

            if (this.action != null) {
                o.add("action", ClickActionSerializer.serialize(this.action));
            } else {
                JsonArray arr = new JsonArray();
                for (MenuItem child : this.children) {
                    arr.add(child.serialize());
                }
                o.add("children", arr);
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] MenuItem.serialize failed: {}", Constants.MOD_NAME, t.toString());
        }
        return o;
    }

    /** Deserialize from JSON used by MenuLoader. */
    public static MenuItem deserialize(JsonObject o) {
        try {
            String id = getString(o, "id", "item_" + Long.toUnsignedString(System.nanoTime(), 36));
            String title = getString(o, "title", "Unnamed");
            String iconId = getString(o, "icon", "minecraft:stone");

            IClickAction action = null;
            List<MenuItem> children = Collections.emptyList();

            if (o.has("action") && o.get("action").isJsonObject()) {
                action = ClickActionSerializer.deserialize(o.getAsJsonObject("action"));
            } else if (o.has("children") && o.get("children").isJsonArray()) {
                List<MenuItem> list = new ArrayList<>();
                for (JsonElement el : o.getAsJsonArray("children")) {
                    if (el.isJsonObject()) {
                        list.add(deserialize(el.getAsJsonObject()));
                    }
                }
                children = list;
            }

            return new MenuItem(id, title, IconSpec.item(iconId), action, children);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] MenuItem.deserialize failed: {}", Constants.MOD_NAME, t.toString());
            // return a safe placeholder so the menu keeps working
            return new MenuItem("invalid", "Invalid", IconSpec.item("minecraft:barrier"), null, Collections.emptyList());
        }
    }

    private static String getString(JsonObject o, String key, String def) {
        try {
            if (o.has(key) && o.get(key).isJsonPrimitive()) {
                return o.get(key).getAsString();
            }
        } catch (Throwable ignored) {}
        return def;
    }

    // -------- factories --------

    /** Create an action item. */
    public static MenuItem action(String id, String title, IconSpec icon, IClickAction act) {
        return new MenuItem(id, title, icon, act, Collections.emptyList());
    }

    /** Create a category item (page). */
    public static MenuItem category(String id, String title, IconSpec icon, List<MenuItem> children) {
        return new MenuItem(id, title, icon, null, children);
    }

    @Override
    public String toString() {
        int childCount = (children == null) ? 0 : children.size();
        return "MenuItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", icon=" + (icon == null ? "null" : icon.id()) +
                ", action=" + (action == null ? "<category>" : action.getType()) +
                ", children=" + childCount +
                '}';
    }
}
