// MainFile: src/main/java/org/z2six/ezactions/data/menu/MenuItem.java
package org.z2six.ezactions.data.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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
 *
 * Notes:
 *  - Both actions and categories can carry an optional "note".
 *  - Titles and notes now support rich text via Minecraft {@link Component}.
 *    Backwards compatibility: string accessors still exist and return getString().
 *
 * Bundle-related flags:
 *  - hideFromMainRadial: if true, this item is hidden from the *root* radial menu.
 *    It still exists in the menu model and can be targeted by direct bundle hotkeys.
 *  - bundleKeybindEnabled: if true on a category, we will register a KeyMapping for it
 *    on next restart so it can be opened directly via a hotkey.
 */
public final class MenuItem {

    private final String id;

    // New: rich text components (never null; empty = Component.literal(""))
    private final Component titleC;
    private final Component noteC;

    // Back-compat read helpers (derived from components)
    private final String titleStr;
    private final String noteStr;

    private final IconSpec icon;        // visual icon spec
    private final IClickAction action;  // null => category
    private final List<MenuItem> children; // backing, mutable list for categories

    // New bundle-related flags
    private final boolean hideFromMainRadial;
    private final boolean bundleKeybindEnabled;

    // ------------ Constructors ------------

    public MenuItem(String id,
                    String title,
                    String note,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children) {
        this(
                id,
                safeLiteral(title),
                safeLiteral(note),
                icon,
                action,
                children,
                false,
                false
        );
    }

    /** Preferred constructor using Components directly. */
    public MenuItem(String id,
                    Component title,
                    Component note,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children) {
        this(
                id,
                title,
                note,
                icon,
                action,
                children,
                false,
                false
        );
    }

    /** Full constructor including bundle flags. */
    public MenuItem(String id,
                    Component title,
                    Component note,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children,
                    boolean hideFromMainRadial,
                    boolean bundleKeybindEnabled) {
        this.id = Objects.requireNonNullElse(id, "item_" + Long.toUnsignedString(System.nanoTime(), 36));

        this.titleC = title == null ? Component.literal("Unnamed") : title;
        this.noteC  = note  == null ? Component.literal("")       : note;

        this.titleStr = this.titleC.getString();
        this.noteStr  = this.noteC.getString();

        this.icon = icon == null ? IconSpec.item("minecraft:stone") : icon;
        this.action = action; // nullable => category

        // Backing, MUTABLE list (no unmodifiable wrapper here!)
        if (children == null) {
            this.children = new ArrayList<>();
        } else {
            this.children = new ArrayList<>(children);
        }

        this.hideFromMainRadial = hideFromMainRadial;
        this.bundleKeybindEnabled = bundleKeybindEnabled;
    }

    /** Backward-compat constructor (no note provided). */
    public MenuItem(String id,
                    String title,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children) {
        this(id, title, "", icon, action, children);
    }

    /** Full String-based constructor including bundle flags. */
    public MenuItem(String id,
                    String title,
                    String note,
                    IconSpec icon,
                    IClickAction action,
                    List<MenuItem> children,
                    boolean hideFromMainRadial,
                    boolean bundleKeybindEnabled) {
        this(
                id,
                safeLiteral(title),
                safeLiteral(note),
                icon,
                action,
                children,
                hideFromMainRadial,
                bundleKeybindEnabled
        );
    }

    // ------------ Accessors ------------

    public String id() { return id; }

    /** Legacy string accessor (derived from Component). */
    public String title() { return titleStr; }

    /** Legacy string accessor (derived from Component). Empty string if none. */
    public String note() { return noteStr; }

    /** Preferred rich text accessor. Never null. */
    public Component titleComponent() { return titleC; }

    /** Preferred rich text accessor. Never null; may be empty. */
    public Component noteComponent()  { return noteC; }

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

    /** Whether this item should be hidden from the main radial root. */
    public boolean hideFromMainRadial() {
        return hideFromMainRadial;
    }

    /** Whether this category should get its own keybind (registered at next restart). */
    public boolean bundleKeybindEnabled() {
        return bundleKeybindEnabled;
    }

    // ------------ Updaters (copy-with) ------------

    /** Return a copy with a different icon. Never crashes. */
    public MenuItem withIcon(IconSpec newIcon) {
        try {
            IconSpec use = (newIcon == null) ? IconSpec.item("minecraft:stone") : newIcon;
            return new MenuItem(this.id, this.titleC, this.noteC, use, this.action, this.children,
                    this.hideFromMainRadial, this.bundleKeybindEnabled);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] MenuItem.withIcon failed: {}", Constants.MOD_NAME, t.toString());
            return this;
        }
    }

    /** Return a copy with a different title (String). */
    public MenuItem withTitle(String newTitle) {
        Component use = (newTitle == null || newTitle.isBlank())
                ? this.titleC
                : Component.literal(newTitle);
        return new MenuItem(this.id, use, this.noteC, this.icon, this.action, this.children,
                this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with a different title (Component). */
    public MenuItem withTitle(Component newTitle) {
        Component use = (newTitle == null) ? this.titleC : newTitle;
        return new MenuItem(this.id, use, this.noteC, this.icon, this.action, this.children,
                this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with a different note (String). */
    public MenuItem withNote(String newNote) {
        Component use = (newNote == null) ? Component.literal("") : Component.literal(newNote);
        return new MenuItem(this.id, this.titleC, use, this.icon, this.action, this.children,
                this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with a different note (Component). */
    public MenuItem withNote(Component newNote) {
        Component use = (newNote == null) ? Component.literal("") : newNote;
        return new MenuItem(this.id, this.titleC, use, this.icon, this.action, this.children,
                this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with a different action (converts category->action if non-null). */
    public MenuItem withAction(IClickAction newAction) {
        // when this becomes an action, children should be empty; preserve note + flags
        return new MenuItem(this.id, this.titleC, this.noteC, this.icon, newAction,
                Collections.emptyList(), this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with different children (converts to category; preserve note + flags). */
    public MenuItem withChildren(List<MenuItem> newChildren) {
        return new MenuItem(this.id, this.titleC, this.noteC, this.icon, null, newChildren,
                this.hideFromMainRadial, this.bundleKeybindEnabled);
    }

    /** Return a copy with updated bundle visibility flag. */
    public MenuItem withHideFromMainRadial(boolean hide) {
        return new MenuItem(this.id, this.titleC, this.noteC, this.icon, this.action, this.children,
                hide, this.bundleKeybindEnabled);
    }

    /** Return a copy with updated bundle keybind flag. */
    public MenuItem withBundleKeybindEnabled(boolean enabled) {
        return new MenuItem(this.id, this.titleC, this.noteC, this.icon, this.action, this.children,
                this.hideFromMainRadial, enabled);
    }

    // ------------ JSON (de)serialization ------------

    /** Serialize to JSON used by MenuLoader. Writes title/note as JSON Components. */
    public JsonObject serialize() {
        JsonObject o = new JsonObject();
        try {
            o.addProperty("id", this.id);

            // Title & note as JSON text components
            o.add("title", serializeComponent(this.titleC));

            // store icon id (string) — IconSpec.item(id) can restore
            String iconId = "minecraft:stone";
            try { iconId = this.icon.id(); } catch (Throwable ignored) {}
            o.addProperty("icon", iconId);

            // Optional note
            if (this.noteC != null && !this.noteStr.isEmpty()) {
                o.add("note", serializeComponent(this.noteC));
            }

            // Bundle flags (only write when true to keep older files tidy)
            if (this.hideFromMainRadial) {
                o.addProperty("hideFromMainRadial", true);
            }
            if (this.bundleKeybindEnabled) {
                o.addProperty("bundleKeybindEnabled", true);
            }

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

    /** Deserialize from JSON used by MenuLoader. Accepts strings or JSON components for title/note. */
    public static MenuItem deserialize(JsonObject o) {
        try {
            String id = getString(o, "id", "item_" + Long.toUnsignedString(System.nanoTime(), 36));

            // Title: accept primitive string or JSON component
            Component titleC = readComponent(o.get("title"), Component.literal("Unnamed"));

            String iconId = getString(o, "icon", "minecraft:stone");

            // Note: optional; accept string or component
            Component noteC = Component.literal("");
            if (o.has("note")) {
                noteC = readComponent(o.get("note"), Component.literal(""));
            }

            // Bundle flags (backwards compatible; default false)
            boolean hideFromMainRadial = getBoolean(o, "hideFromMainRadial", false);
            boolean bundleKeybindEnabled = getBoolean(o, "bundleKeybindEnabled", false);

            IClickAction action = null;
            List<MenuItem> children = Collections.emptyList();

            if (o.has("action") && o.get("action").isJsonObject()) {
                action = org.z2six.ezactions.data.json.ClickActionSerializer.deserialize(o.getAsJsonObject("action"));
            } else if (o.has("children") && o.get("children").isJsonArray()) {
                List<MenuItem> list = new ArrayList<>();
                for (JsonElement el : o.getAsJsonArray("children")) {
                    if (el.isJsonObject()) {
                        list.add(deserialize(el.getAsJsonObject()));
                    }
                }
                children = list;
            }

            return new MenuItem(id, titleC, noteC, IconSpec.item(iconId), action, children,
                    hideFromMainRadial, bundleKeybindEnabled);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] MenuItem.deserialize failed: {}", Constants.MOD_NAME, t.toString());
            // return a safe placeholder so the menu keeps working
            return new MenuItem("invalid", Component.literal("Invalid"), Component.literal(""),
                    IconSpec.item("minecraft:barrier"), null, Collections.emptyList(), false, false);
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

    private static boolean getBoolean(JsonObject o, String key, boolean def) {
        try {
            if (o.has(key) && o.get(key).isJsonPrimitive()) {
                return o.get(key).getAsJsonPrimitive().getAsBoolean();
            }
        } catch (Throwable ignored) {}
        return def;
    }

    private static Component safeLiteral(String s) {
        if (s == null) return Component.literal("");
        return Component.literal(s);
    }

    private static Component readComponent(JsonElement el, Component fallback) {
        try {
            if (el == null || el instanceof JsonNull) return fallback;
            if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isString()) return Component.literal(p.getAsString());
            }
            // Object/array → full text component
            return ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, el)
                    .result()
                    .orElse(fallback);
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] readComponent fallback due to: {}", Constants.MOD_NAME, t.toString());
            return fallback;
        }
    }

    private static JsonElement serializeComponent(Component c) {
        try {
            return ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, c)
                    .result()
                    .orElseGet(JsonObject::new);
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] serializeComponent fallback due to: {}", Constants.MOD_NAME, t.toString());
            // best-effort: plain string
            return new JsonPrimitive(c == null ? "" : c.getString());
        }
    }

    // -------- factories --------

    /** Create an action item. */
    public static MenuItem action(String id, String title, String note, IconSpec icon, IClickAction act) {
        return new MenuItem(id, safeLiteral(title), safeLiteral(note), icon, act, Collections.emptyList(),
                false, false);
    }

    /** Create a category item (page). Note: callers that care about notes can use the main constructor. */
    public static MenuItem category(String id, String title, IconSpec icon, List<MenuItem> children) {
        return new MenuItem(id, safeLiteral(title), Component.literal(""), icon, null, children,
                false, false);
    }

    @Override
    public String toString() {
        int childCount = (children == null) ? 0 : children.size();
        return "MenuItem{" +
                "id='" + id + '\'' +
                ", title='" + titleStr + '\'' +
                ", noteLen=" + (noteStr == null ? 0 : noteStr.length()) +
                ", icon=" + (icon == null ? "null" : icon.id()) +
                ", action=" + (action == null ? "<category>" : action.getType()) +
                ", children=" + childCount +
                ", hideFromMainRadial=" + hideFromMainRadial +
                ", bundleKeybindEnabled=" + bundleKeybindEnabled +
                '}';
    }
}
