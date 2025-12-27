// src/main/java/org/z2six/ezactions/gui/editor/KeybindPickerScreen.java
// MainFile: src/main/java/org/z2six/ezactions/gui/editor/KeybindPickerScreen.java
package org.z2six.ezactions.gui.editor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.gui.noblur.NoMenuBlurScreen;

import java.util.*;
import java.util.function.Consumer;

/**
 * Scrollable keybind picker with:
 * - Mod-grouped headers (each mod is its own category header)
 * - Filter box (matches mapping ID, localized "flavor" text, mod id, mod display name, category keys)
 * - Click-and-drag scrollbar knob
 * - Defensive logs; no crashes
 */
public final class KeybindPickerScreen extends Screen implements NoMenuBlurScreen {

    private final Screen parent;
    private final Consumer<String> onPick;

    private static final int PADDING = 12;

    private static final int FILTER_H = 20;
    private static final int FILTER_GAP = 8; // space between filter and list
    private static final int ROW_H = 20;

    private static final int BUTTON_W = 60;

    // List starts below filter box
    private int listTopPx = PADDING + FILTER_H + FILTER_GAP;

    private double scrollY = 0;
    private final List<Row> rows = new ArrayList<>();

    // Filter
    private EditBox filterBox;
    private String filterText = "";

    // Drag state for scrollbar knob
    private boolean draggingScrollbar = false;
    private int dragGrabOffsetY = 0; // distance from knob top to cursor when drag starts

    private static final class Row {
        final boolean header;
        final Component label;     // displayed label (localized)
        final KeyMapping mapping;  // null for header
        final String groupName;    // for header rows (debug / optional future use)

        Row(boolean header, Component label, KeyMapping mapping, String groupName) {
            this.header = header;
            this.label = label;
            this.mapping = mapping;
            this.groupName = groupName;
        }
    }

    public KeybindPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.literal("Choose Keybinding"));
        this.parent = parent;
        this.onPick = onPick;
    }

    public static void open(Screen parent, Consumer<String> onPick) {
        Minecraft.getInstance().setScreen(new KeybindPickerScreen(parent, onPick));
    }

    @Override
    protected void init() {
        try {
            // Layout (in case resolution changed)
            this.listTopPx = PADDING + FILTER_H + FILTER_GAP;

            // Filter box
            filterBox = new EditBox(this.font, PADDING, PADDING, Math.max(60, this.width - PADDING * 2), FILTER_H, Component.literal("Filter"));
            filterBox.setHint(Component.literal("Filter (key id / name / mod)…"));
            filterBox.setValue(safe(filterText));
            filterBox.setResponder(s -> {
                filterText = safe(s);
                rebuildRows();
            });
            addRenderableWidget(filterBox);

            // Build initial rows
            rebuildRows();

            Constants.LOG.debug("[{}] KeybindPicker init OK (rows={}, filter='{}')",
                    Constants.MOD_NAME, rows.size(), safe(filterText).trim());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker init failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private void rebuildRows() {
        try {
            Options opts = Objects.requireNonNull(Minecraft.getInstance().options);
            KeyMapping[] all = Objects.requireNonNull(opts.keyMappings);

            final String q = normalize(filterText);
            final List<String> tokens = tokenize(q);

            // Group key mappings by MOD display name
            Map<String, List<KeyMapping>> byMod = new HashMap<>();

            int total = 0;
            int kept = 0;

            for (KeyMapping km : all) {
                if (km == null) continue;
                total++;

                String mappingId = safe(km.getName());
                String localizedName = safe(Component.translatable(mappingId).getString());
                String categoryKey = safe(km.getCategory());
                String categoryLocalized = safe(Component.translatable(categoryKey).getString());

                ModMeta mod = resolveModMeta(km);
                String modId = safe(mod.modId);
                String modName = safe(mod.displayName);
                String groupName = modName.isEmpty() ? "Minecraft" : modName;

                // Filtering: match tokens against a combined haystack
                if (!tokens.isEmpty()) {
                    String hay =
                            (mappingId + " " +
                                    localizedName + " " +
                                    modId + " " +
                                    modName + " " +
                                    categoryKey + " " +
                                    categoryLocalized)
                                    .toLowerCase(Locale.ROOT);

                    boolean ok = true;
                    for (String tok : tokens) {
                        if (tok.isEmpty()) continue;
                        if (!hay.contains(tok)) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;
                }

                kept++;
                byMod.computeIfAbsent(groupName, k -> new ArrayList<>()).add(km);
            }

            // Sort mod groups by name
            List<String> groups = new ArrayList<>(byMod.keySet());
            groups.sort(String.CASE_INSENSITIVE_ORDER);

            // Rebuild row list (headers + items)
            rows.clear();

            for (String group : groups) {
                List<KeyMapping> list = byMod.get(group);
                if (list == null || list.isEmpty()) continue;

                // Header row per mod
                rows.add(new Row(true, Component.literal(group), null, group));

                // Sort mappings within mod by localized label
                list.sort(Comparator.comparing(km -> {
                    try {
                        return Component.translatable(safe(km.getName())).getString();
                    } catch (Throwable ignored) {
                        return safe(km.getName());
                    }
                }, String.CASE_INSENSITIVE_ORDER));

                for (KeyMapping km : list) {
                    rows.add(new Row(false, Component.translatable(safe(km.getName())), km, group));
                }
            }

            // Reset/clamp scroll
            scrollY = clamp(scrollY, 0, Math.max(0, rows.size() * ROW_H - viewHeight()));

            Constants.LOG.debug("[{}] KeybindPicker rebuildRows: total={}, kept={}, groups={}, rows={}, filter='{}'",
                    Constants.MOD_NAME, total, kept, groups.size(), rows.size(), q);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker rebuildRows failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    // --- Scroll wheel (Forge 1.20.1 signature) ------------------------------

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        try {
            // Only scroll when over list or scrollbar area (avoid scrolling when hovering filter box)
            boolean overList = my >= listTopPx && my <= (height - PADDING);
            boolean overScrollbar = isOverScrollbar(mx, my);

            if (!overList && !overScrollbar) {
                return super.mouseScrolled(mx, my, delta);
            }

            double content = rows.size() * ROW_H;
            double view = viewHeight();
            if (content > view) {
                scrollY = clamp(scrollY - delta * 32.0, 0, content - view);
            }
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] KeybindPicker mouseScrolled failed: {}", Constants.MOD_NAME, t.toString());
            return true;
        }
    }

    // --- Click handling ------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Let widgets (filter box) handle focus/click first.
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

        // 2) Fall back to list button clicks
        int x = PADDING;
        int y = (int) (listTopPx - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;

            // Skip if off-screen (tiny micro-opt)
            if (ry + ROW_H < listTopPx || ry > height - PADDING) continue;

            if (r.header) continue;

            int btnX = x + Math.min(usableW, 360);
            int btnY = ry + 3;

            if (mx >= btnX && mx <= btnX + BUTTON_W && my >= btnY && my <= btnY + (ROW_H - 6)) {
                try {
                    String mappingKey = r.mapping.getName(); // e.g. "key.inventory"
                    Constants.LOG.info("[{}] KeybindPicker: picked {}", Constants.MOD_NAME, mappingKey);
                    onPick.accept(mappingKey);
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

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Ensure typing works in filter box
        try {
            if (super.charTyped(codePoint, modifiers)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ensure keyboard navigation/backspace works in filter box
        try {
            if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    // --- Render --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0000000);

        // Let widgets render (filter box)
        super.render(g, mouseX, mouseY, partialTick);

        int x = PADDING;
        int y = (int) (listTopPx - scrollY);
        int usableW = width - PADDING * 2;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + i * ROW_H;

            // Clip-ish: skip if out of list region
            if (ry + ROW_H < listTopPx || ry > height - PADDING) continue;

            if (r.header) {
                // Header: mod category title
                g.drawString(this.font, r.label, x, ry + 4, 0xFFFFAA);
                g.fill(x, ry + ROW_H - 2, x + usableW, ry + ROW_H - 1, 0x40FFFFFF);
            } else {
                // Item: localized keybind name
                g.drawString(this.font, r.label, x, ry + 5, 0xFFFFFF);

                int btnX = x + Math.min(usableW, 360);
                int btnY = ry + 3;
                int btnW = BUTTON_W;
                int btnH = ROW_H - 6;

                g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x40000000);
                Component b = Component.literal("Use");
                int tw = this.font.width(b);
                g.drawString(this.font, b, btnX + (btnW - tw) / 2, btnY + 5, 0xFFFFFF);
            }
        }

        drawScrollbar(g);
    }

    // --- Scrollbar math / drawing -------------------------------------------

    private int viewHeight() {
        // Visible list region height (below filter box)
        return Math.max(1, (height - PADDING) - listTopPx);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private boolean isOverScrollbar(double mx, double my) {
        double content = rows.size() * ROW_H;
        int view = viewHeight();
        if (content <= view) return false;

        ScrollbarMetrics m = computeScrollbarMetrics(content, view);
        return mx >= m.barX && mx <= (m.barX + m.barW) && my >= m.barY && my <= (m.barY + m.barH);
    }

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

        // Track spans the list region (below filter)
        m.barY = listTopPx;
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

    // ------------------------------------------------------------------------

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    // --- Mod resolution + filtering helpers ---------------------------------

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String normalize(String s) {
        return safe(s).trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> tokenize(String qLower) {
        if (qLower == null) return List.of();
        String q = qLower.trim();
        if (q.isEmpty()) return List.of();
        String[] parts = q.split("\\s+");
        ArrayList<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static final class ModMeta {
        final String modId;
        final String displayName;

        ModMeta(String modId, String displayName) {
            this.modId = modId;
            this.displayName = displayName;
        }
    }

    private static ModMeta resolveModMeta(KeyMapping km) {
        try {
            if (km == null) return new ModMeta("", "");

            // 1) Try mod id from keybind translation key: "key.<modid>...."
            String nameKey = safe(km.getName());
            String cand = extractModIdFromKeyName(nameKey);
            ModMeta meta = lookupMod(cand);
            if (!meta.displayName.isEmpty()) return meta;

            // 2) Try mod id from category translation key: "key.categories.<modid>" (common for mods)
            String catKey = safe(km.getCategory());
            cand = extractModIdFromCategoryKey(catKey);
            meta = lookupMod(cand);
            if (!meta.displayName.isEmpty()) return meta;

            // 3) Unknown / vanilla
            return new ModMeta("", "Minecraft");
        } catch (Throwable t) {
            Constants.LOG.debug("[{}] resolveModMeta failed safely: {}", Constants.MOD_NAME, t.toString());
            return new ModMeta("", "Minecraft");
        }
    }

    private static String extractModIdFromKeyName(String key) {
        try {
            // Expect: key.<modid>.<something> OR key.<modid>_<something>... (we only take the segment until '.')
            if (key == null) return "";
            if (!key.startsWith("key.")) return "";
            String rest = key.substring("key.".length());
            int dot = rest.indexOf('.');
            if (dot <= 0) return "";
            String seg = rest.substring(0, dot).trim();
            return seg;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String extractModIdFromCategoryKey(String catKey) {
        try {
            // Expect: key.categories.<modid> (sometimes followed by more segments)
            if (catKey == null) return "";
            final String pfx = "key.categories.";
            if (!catKey.startsWith(pfx)) return "";
            String rest = catKey.substring(pfx.length());
            int dot = rest.indexOf('.');
            String seg = (dot >= 0) ? rest.substring(0, dot) : rest;
            seg = seg.trim();
            return seg;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static ModMeta lookupMod(String modIdCandidate) {
        try {
            String id = safe(modIdCandidate).trim();
            if (id.isEmpty()) return new ModMeta("", "");

            Optional<? extends net.minecraftforge.forgespi.language.IModInfo> infoOpt = Optional.empty();

            // ModList lookup is the authoritative way to get the display name in Forge
            try {
                var contOpt = ModList.get().getModContainerById(id);
                if (contOpt.isPresent()) {
                    var info = contOpt.get().getModInfo();
                    if (info != null) infoOpt = Optional.of(info);
                }
            } catch (Throwable ignored) {}

            if (infoOpt.isPresent()) {
                String display = safe(infoOpt.get().getDisplayName()).trim();
                if (!display.isEmpty()) {
                    return new ModMeta(id, display);
                }
            }

            // If it's a real mod but has no display name for some reason, at least return the id.
            return new ModMeta(id, id);
        } catch (Throwable t) {
            return new ModMeta("", "");
        }
    }
}
