// src/main/java/org/z2six/ezactions/gui/editor/KeybindPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.*;
import java.util.function.Consumer;

/**
 * // MainFile: KeybindPickerScreen.java
 * Scrollable, mod-grouped keybind list with readable labels.
 * Calls onPick.accept(km.getName()) -> e.g. "key.inventory"
 * Defensive logs; no crashes.
 *
 * Added:
 * - Click-and-drag scrollbar knob.
 * - Robust math for knob to/from scrollY mapping.
 * - Debug logs for layout/drag diagnostics.
 * - Filter box: filters by mapping id, localized label ("flavor text"), and mod/category text.
 * - Results stay grouped by mod (header per mod), not a flat list.
 */
public final class KeybindPickerScreen extends Screen implements NoMenuBlurScreen {

    private final Screen parent;
    private final Consumer<String> onPick;

    private static final int PADDING = 12;
    private static final int ROW_H = 20;
    private static final int BUTTON_W = 60;

    // Top filter bar layout
    private static final int FILTER_H = 20;
    private static final int FILTER_GAP = 8; // gap between filter and list
    private static final int FILTER_MAX_LEN = 128;

    private EditBox filterBox;

    private double scrollY = 0;
    private final List<Row> rows = new ArrayList<>();
    private KeyMapping[] allKeyMappings = new KeyMapping[0];

    // Drag state for scrollbar knob
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0; // distance from knob top to cursor when drag starts

    private static final class Row {
        final boolean header;
        final Component label;     // display label for header or mapping
        final KeyMapping mapping;  // null for header

        Row(boolean header, Component label, KeyMapping mapping) {
            this.header = header;
            this.label = label;
            this.mapping = mapping;
        }
    }

    public KeybindPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.translatable("ezactions.gui.keybind_picker.title"));
        this.parent = parent;
        this.onPick = onPick;
    }

    public static void open(Screen parent, Consumer<String> onPick) {
        Minecraft.getInstance().setScreen(new KeybindPickerScreen(parent, onPick));
    }

    @Override
    protected void init() {
        try {
            // Filter box at top
            int fx = PADDING;
            int fy = PADDING;
            int fw = Math.max(60, this.width - (PADDING * 2) - 8); // leave a tiny right margin for scrollbar area
            filterBox = new EditBox(this.font, fx, fy, fw, FILTER_H, Component.translatable("ezactions.gui.field.filter"));
            filterBox.setHint(Component.translatable("ezactions.gui.keybind_picker.hint.filter"));
            filterBox.setMaxLength(FILTER_MAX_LEN);
            filterBox.setResponder(s -> {
                try {
                    rebuildRows();
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] KeybindPicker filter rebuild failed: {}", Constants.MOD_NAME, t.toString());
                }
            });
            addRenderableWidget(filterBox);

            // Load mappings once
            Options opts = Objects.requireNonNull(Minecraft.getInstance().options);
            KeyMapping[] all = Objects.requireNonNull(opts.keyMappings);
            this.allKeyMappings = all;

            rebuildRows();

            Constants.LOG.info("[{}] KeybindPicker init ok (totalKeyMappings={}, initialRows={}).",
                    Constants.MOD_NAME,
                    (this.allKeyMappings == null ? 0 : this.allKeyMappings.length),
                    this.rows.size());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker init failed: {}", Constants.MOD_NAME, t.toString());
            // fail-soft
            this.allKeyMappings = new KeyMapping[0];
            this.rows.clear();
            this.scrollY = 0;
        }
    }

    private void rebuildRows() {
        try {
            String qRaw = "";
            try { qRaw = (filterBox == null) ? "" : String.valueOf(filterBox.getValue()); } catch (Throwable ignored) {}
            String q = qRaw == null ? "" : qRaw.trim().toLowerCase(Locale.ROOT);

            String[] tokens = tokenize(q);

            // Group by "mod"
            Map<String, List<KeyMapping>> byMod = new HashMap<>();

            KeyMapping[] all = this.allKeyMappings == null ? new KeyMapping[0] : this.allKeyMappings;
            for (KeyMapping km : all) {
                if (km == null) continue;

                String id = safeStr(km.getName());
                String label = mappingFlavorText(km);
                String catKey = safeStr(km.getCategory());
                String catLabel = categoryFlavorText(catKey);

                String modId = inferModId(km);
                String modDisplay = displayNameFromModId(modId);

                if (tokens.length > 0) {
                    // Combined searchable text; all tokens must match somewhere
                    String combined = (id + " " + label + " " + catKey + " " + catLabel + " " + modId + " " + modDisplay).toLowerCase(Locale.ROOT);
                    if (!allTokensPresent(combined, tokens)) {
                        continue;
                    }
                }

                byMod.computeIfAbsent(modDisplay, k -> new ArrayList<>()).add(km);
            }

            // Sort groups + rows
            List<String> mods = new ArrayList<>(byMod.keySet());
            mods.sort(String.CASE_INSENSITIVE_ORDER);

            rows.clear();

            for (String modDisplay : mods) {
                List<KeyMapping> list = byMod.get(modDisplay);
                if (list == null || list.isEmpty()) continue;

                list.sort(Comparator.comparing(KeybindPickerScreen::mappingFlavorText, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(km -> safeStr(km.getName()), String.CASE_INSENSITIVE_ORDER));

                rows.add(new Row(true, Component.literal(modDisplay), null));
                for (KeyMapping km : list) {
                    // Keep display as flavor/translated key name (as before)
                    rows.add(new Row(false, Component.translatable(km.getName()), km));
                }
            }

            // Clamp scroll after rebuild
            scrollY = clamp(scrollY, 0, Math.max(0, rows.size() * ROW_H - viewHeight()));

            Constants.LOG.debug("[{}] KeybindPicker rebuildRows: q='{}' tokens={} groups={} rows={} viewH={} scrollY={}",
                    Constants.MOD_NAME,
                    qRaw,
                    tokens.length,
                    mods.size(),
                    rows.size(),
                    viewHeight(),
                    scrollY);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker rebuildRows failed: {}", Constants.MOD_NAME, t.toString());
            // fail-soft: keep existing rows
            scrollY = clamp(scrollY, 0, Math.max(0, rows.size() * ROW_H - viewHeight()));
        }
    }

    // --- Scroll wheel (two signatures for cross-version safety) -------------

    public boolean mouseScrolled(double mx, double my, double delta) {
        double content = rows.size() * ROW_H;
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - delta * 32.0, 0, content - view);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double content = rows.size() * ROW_H;
        double view = viewHeight();
        if (content > view) {
            scrollY = clamp(scrollY - deltaY * 32.0, 0, Math.max(0, content - view));
        }
        return true;
    }

    // --- Click handling ------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Let widgets (filter box) handle clicks first.
        try {
            if (super.mouseClicked(mx, my, button)) {
                return true;
            }
        } catch (Throwable ignored) {}

        if (button != 0) return false;

        // 1) Try scrollbar knob first so it "wins" over list clicks.
        if (beginScrollbarDragIfHit(mx, my)) {
            return true;
        }

        // 2) List "Use" button clicks
        int x = PADDING;
        int y = (int) (listTopY() - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;
            if (r.header) continue;

            // Only consider clicks within the list viewport
            if (!inListViewport(ry)) continue;

            int btnX = x + Math.min(usableW, 360);
            int btnY = ry + 3;

            if (mx >= btnX && mx <= btnX + BUTTON_W && my >= btnY && my <= btnY + (ROW_H - 6)) {
                try {
                    if (r.mapping != null) {
                        String mappingKey = r.mapping.getName(); // e.g. "key.inventory"
                        Constants.LOG.debug("[{}] KeybindPicker: picked {}", Constants.MOD_NAME, mappingKey);
                        onPick.accept(mappingKey);
                    } else {
                        Constants.LOG.debug("[{}] KeybindPicker: clicked Use on null mapping row (ignored).", Constants.MOD_NAME);
                    }
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] KeybindPicker onPick failed: {}", Constants.MOD_NAME, t.toString());
                }
                onClose();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            Constants.LOG.debug("[{}] KeybindPicker: scrollbar drag end (scrollY={})", Constants.MOD_NAME, scrollY);
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScrollbar && button == 0) {
            try {
                applyDragToScroll(my);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] KeybindPicker drag update failed: {}", Constants.MOD_NAME, t.toString());
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    // --- Render --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);

        // List
        int x = PADDING;
        int y = (int) (listTopY() - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;

            // Skip rows outside list viewport (keeps things tidy when filter is present)
            if (!inListViewport(ry)) continue;

            if (r.header) {
                g.drawString(this.font, r.label, x, ry + 4, 0xFFFFAA);
                g.fill(x, ry + ROW_H - 2, x + usableW, ry + ROW_H - 1, 0x40FFFFFF);
            } else {
                g.drawString(this.font, r.label, x, ry + 5, 0xFFFFFF);

                int btnX = x + Math.min(usableW, 360);
                int btnY = ry + 3;
                int btnW = BUTTON_W;
                int btnH = ROW_H - 6;

                g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x40000000);
                Component b = Component.translatable("ezactions.gui.keybind_picker.use");
                int tw = this.font.width(b);
                g.drawString(this.font, b, btnX + (btnW - tw) / 2, btnY + 5, 0xFFFFFF);
            }
        }

        drawScrollbar(g);

        // Widgets (filter box)
        super.render(g, mouseX, mouseY, partialTick);
    }

    // --- Layout helpers ------------------------------------------------------

    private int listTopY() {
        // Filter row: PADDING + FILTER_H, then a gap, then list starts
        return PADDING + FILTER_H + FILTER_GAP;
    }

    private int viewHeight() {
        // view is from listTop to bottom padding
        int top = listTopY();
        return Math.max(0, height - top - PADDING);
    }

    private boolean inListViewport(int rowY) {
        int top = listTopY();
        int bot = top + viewHeight();
        return rowY + ROW_H >= top && rowY <= bot;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // --- Scrollbar math / drawing -------------------------------------------

    private void drawScrollbar(GuiGraphics g) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        // Track
        g.fill(m.barX, m.barY, m.barX + m.barW, m.barY + m.barH, 0x40000000);
        // Knob
        g.fill(m.barX + 1, m.knobY, m.barX + m.barW - 1, m.knobY + m.knobH, 0x80FFFFFF);
    }

    private static final class ScrollbarMetrics {
        int barX, barY, barW, barH;
        int knobY, knobH;
    }

    /** Compute positions/sizes for track & knob given current scrollY. */
    private ScrollbarMetrics computeScrollbarMetrics(double content, int view) {
        ScrollbarMetrics m = new ScrollbarMetrics();
        m.barW = 6;
        m.barX = width - PADDING - m.barW;
        m.barY = listTopY();
        m.barH = view;

        double ratio = view / content;
        m.knobH = Math.max(20, (int) (m.barH * ratio));

        // Place knob according to current scrollY
        double denom = Math.max(1.0, content - view);
        m.knobY = (int) (m.barY + (m.barH - m.knobH) * (scrollY / denom));
        return m;
    }

    /** If clicked within knob, start dragging; returns true if drag began. */
    private boolean beginScrollbarDragIfHit(double mx, double my) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return false;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        boolean inKnob = mx >= m.barX + 1 && mx <= m.barX + m.barW - 1 && my >= m.knobY && my <= m.knobY + m.knobH;
        if (inKnob) {
            draggingScrollbar = true;
            dragGrabOffsetY = (int) (my - m.knobY);
            Constants.LOG.debug("[{}] KeybindPicker: scrollbar drag start (grabOffsetY={}, scrollY={})",
                    Constants.MOD_NAME, dragGrabOffsetY, scrollY);
            return true;
        }
        return false;
    }

    /** While dragging, convert mouseY back to scrollY using inverse mapping. */
    private void applyDragToScroll(double mouseY) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);

        // Desired knob top from drag, clamped to track
        int minY = m.barY;
        int maxY = m.barY + m.barH - m.knobH;
        int newKnobY = (int) clamp(mouseY - dragGrabOffsetY, minY, maxY);

        // Inverse mapping: knob pos -> scrollY
        double trackRange = (double) (m.barH - m.knobH);
        double t = trackRange <= 0 ? 0.0 : (newKnobY - m.barY) / trackRange;
        double maxScroll = Math.max(0, content - view);
        scrollY = clamp(t * maxScroll, 0, maxScroll);
    }

    // --- Filter helpers ------------------------------------------------------

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }

    private static String[] tokenize(String qLowerTrimmed) {
        if (qLowerTrimmed == null) return new String[0];
        String s = qLowerTrimmed.trim();
        if (s.isEmpty()) return new String[0];
        // Split on whitespace; ignore empties.
        String[] raw = s.split("\\s+");
        ArrayList<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r == null) continue;
            String t = r.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out.toArray(new String[0]);
    }

    private static boolean allTokensPresent(String combinedLower, String[] tokensLower) {
        if (tokensLower == null || tokensLower.length == 0) return true;
        if (combinedLower == null) return false;
        for (String tok : tokensLower) {
            if (tok == null || tok.isEmpty()) continue;
            if (!combinedLower.contains(tok)) return false;
        }
        return true;
    }

    private static String mappingFlavorText(KeyMapping km) {
        try {
            if (km == null) return "";
            // Using translatable text is the "flavor text" users see in vanilla Controls.
            return Component.translatable(km.getName()).getString();
        } catch (Throwable t) {
            try {
                return km == null ? "" : safeStr(km.getName());
            } catch (Throwable ignored) {
                return "";
            }
        }
    }

    private static String categoryFlavorText(String categoryKey) {
        try {
            if (categoryKey == null || categoryKey.isEmpty()) return "";
            return Component.translatable(categoryKey).getString();
        } catch (Throwable ignored) {
            return safeStr(categoryKey);
        }
    }

    /**
     * Infer a "mod id" for grouping/filtering:
     * - Prefer category key "key.categories.<x>": if <x> is not a vanilla category, treat it as modid.
     * - Else parse mapping id "key.<modid>.<something>" (3+ segments) => modid.
     * - Fallback: "minecraft".
     */
    private static String inferModId(KeyMapping km) {
        try {
            if (km == null) return "minecraft";

            // 1) Category-based hint
            String cat = safeStr(km.getCategory());
            if (cat.startsWith("key.categories.")) {
                String suffix = cat.substring("key.categories.".length());
                // Vanilla categories are not mod ids
                if (!isVanillaCategorySuffix(suffix) && !suffix.isBlank()) {
                    return suffix;
                }
            }

            // 2) Mapping-name-based hint
            String name = safeStr(km.getName()); // e.g. "key.apotheosis.something"
            if (name.startsWith("key.")) {
                String rest = name.substring("key.".length());
                int dot = rest.indexOf('.');
                if (dot > 0) {
                    // If there's ANOTHER dot after modid, it's likely "key.<modid>.<...>"
                    String mod = rest.substring(0, dot);
                    String after = rest.substring(dot + 1);
                    if (!after.isEmpty() && after.indexOf('.') >= 0) {
                        return mod;
                    }
                }
            }

        } catch (Throwable ignored) {}

        return "minecraft";
    }

    private static boolean isVanillaCategorySuffix(String suffix) {
        if (suffix == null) return true;
        String s = suffix.trim().toLowerCase(Locale.ROOT);

        // Common vanilla categories (this list is intentionally conservative).
        return s.isEmpty()
                || s.equals("movement")
                || s.equals("gameplay")
                || s.equals("inventory")
                || s.equals("creative")
                || s.equals("multiplayer")
                || s.equals("ui")
                || s.equals("misc")
                || s.equals("chat");
    }

    private static String displayNameFromModId(String modId) {
        String id = (modId == null || modId.isBlank()) ? "minecraft" : modId.trim();

        if ("minecraft".equalsIgnoreCase(id)) {
            return "Minecraft";
        }

        // Convert "irons_spellbooks" -> "Irons Spellbooks"
        String cleaned = id.replace('-', '_');
        String[] parts = cleaned.split("_+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(capitalize(p));
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? id : out;
    }

    private static String capitalize(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        if (t.length() == 1) return t.toUpperCase(Locale.ROOT);
        return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
    }

    // ------------------------------------------------------------------------

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
