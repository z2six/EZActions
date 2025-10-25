// MainFile: src/main/java/org/z2six/ezactions/gui/editor/config/ConfigScreen.java
package ezactions.gui.editor.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.config.DesignClientConfig;
import org.z2six.ezactions.config.GeneralClientConfig;
import org.z2six.ezactions.config.RadialAnimConfig;

import static java.lang.Integer.parseInt;

/**
 * In-game config UI (General / Animations / Design) with live previews + color pickers.
 * Saves directly into existing ModConfigSpec values (no crashes; defensive parsing).
 *
 * Changes here implement the right-panel inner margin and ensure the section title
 * sits above all fields (no overlap). Labels render to the RIGHT of each text box.
 */
public final class ConfigScreen extends Screen {

    private final Screen parent;

    // Section toggle
    private enum Section { GENERAL, ANIM, DESIGN }
    private Section section = Section.GENERAL;

    // One-shot flag: when returning from a child screen (e.g., color picker), skip reloading drafts from spec
    private boolean skipReloadDraftsOnce = false;

    // --- Drafts (read from current specs on init) ----------------------------
    // General
    private boolean draftMoveWhileRadialOpen;
    private int draftCmdVisibleLines;

    // Anim
    private boolean draftAnimEnabled;
    private boolean draftAnimOpenClose;
    private boolean draftAnimHover;
    private double  draftHoverGrowPct;
    private int     draftOpenCloseMs;

    // Design
    private int draftDeadzone;
    private int draftBaseOuterRadius;
    private int draftRingThickness;
    private int draftScaleStartThreshold;
    private int draftScalePerItem;
    private int draftRingColor;   // ARGB
    private int draftHoverColor;  // ARGB

    // Widgets we need to read from on Save
    // General
    private CycleButton<Boolean> wMoveWhileOpen;
    private EditBox wCmdLines;

    // Anim
    private CycleButton<Boolean> wAnimEnabled;
    private CycleButton<Boolean> wAnimOpenClose;
    private CycleButton<Boolean> wAnimHover;
    private EditBox wHoverGrowPct;
    private EditBox wOpenCloseMs;

    // Design
    private EditBox wDeadzone, wOuter, wThick, wScaleStart, wScalePer;
    private Button  wRingPick, wHoverPick;

    public ConfigScreen(Screen parent) {
        super(Component.literal("EZ Actions – Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!skipReloadDraftsOnce) {
            readCurrentIntoDrafts();
        } else {
            skipReloadDraftsOnce = false; // consume the one-shot skip
        }
        buildUI();
    }

    private void readCurrentIntoDrafts() {
        try {
            // General
            draftMoveWhileRadialOpen = GeneralClientConfig.isMoveWhileRadialOpen();
            draftCmdVisibleLines     = GeneralClientConfig.getCommandEditorVisibleLines();

            // Anim
            draftAnimEnabled    = RadialAnimConfig.CONFIG.animationsEnabled();
            draftAnimOpenClose  = RadialAnimConfig.CONFIG.animOpenClose();
            draftAnimHover      = RadialAnimConfig.CONFIG.animHover();
            draftHoverGrowPct   = RadialAnimConfig.CONFIG.hoverGrowPct();
            draftOpenCloseMs    = RadialAnimConfig.CONFIG.openCloseMs();

            // Design
            draftDeadzone            = DesignClientConfig.deadzone.get();
            draftBaseOuterRadius     = DesignClientConfig.baseOuterRadius.get();
            draftRingThickness       = DesignClientConfig.ringThickness.get();
            draftScaleStartThreshold = DesignClientConfig.scaleStartThreshold.get();
            draftScalePerItem        = DesignClientConfig.scalePerItem.get();
            draftRingColor           = DesignClientConfig.ringColor.get();
            draftHoverColor          = DesignClientConfig.hoverColor.get();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ConfigScreen: failed reading current specs: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private void buildUI() {
        this.clearWidgets();

        final int PAD = 12;
        final int LEFT_W = 160;
        final int FIELD_W = 160;
        final int FIELD_H = 20;

        // Right panel inner margins & header spacing (implements the margin suggestion)
        final int RIGHT_INNER_PAD = 16;   // left inset inside right panel
        final int HEADER_Y = PAD + 8;     // header baseline inside right panel
        final int FORM_TOP_GAP = 18;      // gap below header before fields begin

        // Left section chooser
        int x = PAD, y = PAD;
        addRenderableWidget(Button.builder(Component.literal("General"), b -> {
            section = Section.GENERAL; buildUI();
        }).bounds(x, y, LEFT_W, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Animations"), b -> {
            section = Section.ANIM; buildUI();
        }).bounds(x, y, LEFT_W, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.literal("Design"), b -> {
            section = Section.DESIGN; buildUI();
        }).bounds(x, y, LEFT_W, 20).build());

        // Bottom buttons (Save / Back) — anchored to bottom of left panel
        int bottom = this.height - PAD;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSave())
                .bounds(x, bottom - 22, LEFT_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(x, bottom - 22 - 24, LEFT_W, 20).build());

        // Right panel form origin (panel bg drawn in render())
        int rightPanelX = PAD + LEFT_W + PAD;
        int formX = rightPanelX + RIGHT_INNER_PAD;          // inner left margin
        int formY = HEADER_Y + FORM_TOP_GAP;                // fields start below header
        int row = 0;

        switch (section) {
            case GENERAL -> {
                wMoveWhileOpen = addRenderableWidget(
                        CycleButton.onOffBuilder(draftMoveWhileRadialOpen)
                                .create(formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal("Move While Radial Open"))
                );
                row++;

                wCmdLines = new EditBox(this.font, formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal(""));
                wCmdLines.setValue(Integer.toString(draftCmdVisibleLines));
                addRenderableWidget(wCmdLines);
            }

            case ANIM -> {
                wAnimEnabled = addRenderableWidget(
                        CycleButton.onOffBuilder(draftAnimEnabled)
                                .create(formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal("Animations Enabled"))
                );
                row++;

                wAnimOpenClose = addRenderableWidget(
                        CycleButton.onOffBuilder(draftAnimOpenClose)
                                .create(formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal("Open/Close Wipe"))
                );
                row++;

                wAnimHover = addRenderableWidget(
                        CycleButton.onOffBuilder(draftAnimHover)
                                .create(formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal("Hover Animation"))
                );
                row++;

                wHoverGrowPct = new EditBox(this.font, formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal(""));
                wHoverGrowPct.setValue(Double.toString(draftHoverGrowPct));
                addRenderableWidget(wHoverGrowPct);
                row++;

                wOpenCloseMs = new EditBox(this.font, formX, formY + row * 28, FIELD_W, FIELD_H, Component.literal(""));
                wOpenCloseMs.setValue(Integer.toString(draftOpenCloseMs));
                addRenderableWidget(wOpenCloseMs);
            }

            case DESIGN -> {
                wDeadzone = addIntBox(formX, formY + row * 28, FIELD_W, FIELD_H, draftDeadzone); row++;
                wOuter    = addIntBox(formX, formY + row * 28, FIELD_W, FIELD_H, draftBaseOuterRadius); row++;
                wThick    = addIntBox(formX, formY + row * 28, FIELD_W, FIELD_H, draftRingThickness); row++;
                wScaleStart = addIntBox(formX, formY + row * 28, FIELD_W, FIELD_H, draftScaleStartThreshold); row++;
                wScalePer   = addIntBox(formX, formY + row * 28, FIELD_W, FIELD_H, draftScalePerItem); row++;

                int btnY = formY + row * 28;
                wRingPick = Button.builder(Component.literal("Pick Ring Color…"), b -> {
                    // Avoid re-reading spec on return so chosen color remains visible
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftRingColor, argb -> {
                        draftRingColor = argb; // draft only; click Save to persist
                    }));
                }).bounds(formX, btnY, FIELD_W, FIELD_H).build();
                addRenderableWidget(wRingPick);
                row++;

                wHoverPick = Button.builder(Component.literal("Pick Hover Color…"), b -> {
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftHoverColor, argb -> {
                        draftHoverColor = argb; // draft only; click Save to persist
                    }));
                }).bounds(formX, formY + row * 28, FIELD_W, FIELD_H).build();
                addRenderableWidget(wHoverPick);
            }
        }
    }

    private EditBox addIntBox(int x, int y, int w, int h, int value) {
        EditBox eb = new EditBox(this.font, x, y, w, h, Component.literal(""));
        eb.setValue(Integer.toString(value));
        addRenderableWidget(eb);
        return eb;
    }

    private void onSave() {
        try {
            switch (section) {
                case GENERAL -> {
                    draftMoveWhileRadialOpen = wMoveWhileOpen != null && Boolean.TRUE.equals(wMoveWhileOpen.getValue());
                    draftCmdVisibleLines = clamp(parseSafeInt(wCmdLines, 5), 1, 20);

                    // Forge 1.20.1: write through INSTANCE values
                    GeneralClientConfig.INSTANCE.moveWhileRadialOpen.set(draftMoveWhileRadialOpen);
                    GeneralClientConfig.INSTANCE.commandEditorVisibleLines.set(draftCmdVisibleLines);
                }
                case ANIM -> {
                    draftAnimEnabled   = wAnimEnabled != null && Boolean.TRUE.equals(wAnimEnabled.getValue());
                    draftAnimOpenClose = wAnimOpenClose != null && Boolean.TRUE.equals(wAnimOpenClose.getValue());
                    draftAnimHover     = wAnimHover != null && Boolean.TRUE.equals(wAnimHover.getValue());
                    draftHoverGrowPct  = clampDouble(parseSafeDouble(wHoverGrowPct, 0.05), 0.0, 0.5);
                    draftOpenCloseMs   = clamp(parseSafeInt(wOpenCloseMs, 125), 0, 2000);

                    RadialAnimConfig.CONFIG.animationsEnabled.set(draftAnimEnabled);
                    RadialAnimConfig.CONFIG.animOpenClose.set(draftAnimOpenClose);
                    RadialAnimConfig.CONFIG.animHover.set(draftAnimHover);
                    RadialAnimConfig.CONFIG.hoverGrowPct.set(draftHoverGrowPct);
                    RadialAnimConfig.CONFIG.openCloseMs.set(draftOpenCloseMs);
                }
                case DESIGN -> {
                    draftDeadzone            = clamp(parseSafeInt(wDeadzone, 18), 0, 90);
                    draftBaseOuterRadius     = clamp(parseSafeInt(wOuter, 72), 24, 512);
                    draftRingThickness       = clamp(parseSafeInt(wThick, 28), 6, 256);
                    draftScaleStartThreshold = clamp(parseSafeInt(wScaleStart, 8), 0, 128);
                    draftScalePerItem        = clamp(parseSafeInt(wScalePer, 6), 0, 100);

                    DesignClientConfig.deadzone.set(draftDeadzone);
                    DesignClientConfig.baseOuterRadius.set(draftBaseOuterRadius);
                    DesignClientConfig.ringThickness.set(draftRingThickness);
                    DesignClientConfig.scaleStartThreshold.set(draftScaleStartThreshold);
                    DesignClientConfig.scalePerItem.set(draftScalePerItem);
                    DesignClientConfig.ringColor.set(draftRingColor);
                    DesignClientConfig.hoverColor.set(draftHoverColor);
                }
            }

            buildUI(); // reflect clamps
            Constants.LOG.info("[{}] Config saved (section: {})", Constants.MOD_NAME, section.name());
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Config save failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        final int PAD = 12;
        final int LEFT_W = 160;
        final int RIGHT_INNER_PAD = 16;
        final int LABEL_COLOR = 0xA0A0A0;

        // Backgrounds
        g.fill(0, 0, width, height, 0x88000000);
        // Left panel
        g.fill(PAD, PAD, PAD + LEFT_W, this.height - PAD, 0xC0101010);
        // Right panel
        int rightPanelX = PAD + LEFT_W + PAD;
        g.fill(rightPanelX, PAD, this.width - PAD, this.height - PAD, 0xC0101010);

        // Title & section header
        g.drawCenteredString(this.font, this.title.getString(), this.width / 2, 8, 0xFFFFFF);

        String sec = switch (section) {
            case GENERAL -> "General";
            case ANIM    -> "Animations";
            case DESIGN  -> "Design";
        };
        int headerX = rightPanelX + RIGHT_INNER_PAD;  // left margin INSIDE the right panel
        int headerY = PAD + 8;                        // header baseline
        g.drawString(this.font, sec, headerX, headerY, LABEL_COLOR);

        // Draw labels to the RIGHT of each EditBox (per section)
        switch (section) {
            case GENERAL -> {
                drawRightLabel(g, wCmdLines, "Visible lines (1–20)");
            }
            case ANIM -> {
                drawRightLabel(g, wHoverGrowPct, "Hover Grow % (0.0–0.5)");
                drawRightLabel(g, wOpenCloseMs,  "Open/Close (ms)");
            }
            case DESIGN -> {
                drawRightLabel(g, wDeadzone,    "Deadzone");
                drawRightLabel(g, wOuter,       "Outer Radius");
                drawRightLabel(g, wThick,       "Ring Thickness");
                drawRightLabel(g, wScaleStart,  "Scale Start");
                drawRightLabel(g, wScalePer,    "Scale / Item");

                // Color previews to the right of buttons
                if (wRingPick != null) {
                    int x = wRingPick.getX() + wRingPick.getWidth() + 8;
                    int y = wRingPick.getY();
                    drawColorPreview(g, x, y, draftRingColor);
                    g.drawString(this.font, ColorUtil.toHexARGB(draftRingColor), x + 64 + 6, y + 4, 0xFFFFFF);
                }
                if (wHoverPick != null) {
                    int x = wHoverPick.getX() + wHoverPick.getWidth() + 8;
                    int y = wHoverPick.getY();
                    drawColorPreview(g, x, y, draftHoverColor);
                    g.drawString(this.font, ColorUtil.toHexARGB(draftHoverColor), x + 64 + 6, y + 4, 0xFFFFFF);
                }
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Draws a grey label to the RIGHT of an EditBox, with small vertical padding. */
    private void drawRightLabel(GuiGraphics g, EditBox eb, String label) {
        if (eb == null) return;
        final int GAP = 8;
        int lx = eb.getX() + eb.getWidth() + GAP;
        int ly = eb.getY() + 4; // align with field text
        g.drawString(this.font, label, lx, ly, 0xA0A0A0);
    }

    private void drawColorPreview(GuiGraphics g, int x, int y, int argb) {
        // checker bg 64x18
        int sw = 64, sh = 18, cell = 6;
        for (int yy = 0; yy < sh; yy += cell) {
            for (int xx = 0; xx < sw; xx += cell) {
                int c = (((xx / cell) + (yy / cell)) % 2 == 0) ? 0xFFCCCCCC : 0xFFFFFFFF;
                g.fill(x + xx, y, x + Math.min(xx + cell, sw), y + Math.min(sh, cell), c);
            }
        }
        g.fill(x, y, x + sw, y + sh, argb);
        g.fill(x, y, x + sw, y + 1, 0xFF000000);
        g.fill(x, y + sh - 1, x + sw, y + sh, 0xFF000000);
        g.fill(x, y, x + 1, y + sh, 0xFF000000);
        g.fill(x + sw - 1, y, x + sw, y + sh, 0xFF000000);
    }

    private static int parseSafeInt(EditBox eb, int dflt) {
        try { return parseInt(eb.getValue().trim()); } catch (Throwable ignored) { return dflt; }
    }
    private static double parseSafeDouble(EditBox eb, double dflt) {
        try { return Double.parseDouble(eb.getValue().trim()); } catch (Throwable ignored) { return dflt; }
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clampDouble(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
