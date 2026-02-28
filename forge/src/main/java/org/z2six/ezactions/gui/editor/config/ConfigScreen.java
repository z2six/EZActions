// MainFile: neoforge/src/main/java/org/z2six/ezactions/gui/editor/config/ConfigScreen.java
package org.z2six.ezactions.gui.editor.config;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.gui.compat.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.z2six.ezactions.gui.EzScreen;
import net.minecraft.network.chat.Component;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.menu.RadialMenu;
import org.z2six.ezactions.gui.editor.ActionEditorUi;
import org.z2six.ezactions.gui.editor.EditorCycleButton;
import org.z2six.ezactions.gui.editor.EditorButton;
import org.z2six.ezactions.config.ConfigIO;
import org.z2six.ezactions.config.DesignClientConfig;
import org.z2six.ezactions.config.GeneralClientConfig;
import org.z2six.ezactions.config.RadialAnimConfig;
import org.z2six.ezactions.config.RadialConfig;

import java.util.Locale;

public final class ConfigScreen extends EzScreen {

    private final Screen parent;

    private enum Section { GENERAL, ANIM, DESIGN }
    private Section section = Section.GENERAL;

    private boolean skipReloadDraftsOnce = false;

    private boolean draftMoveWhileRadialOpen;
    private boolean draftShowRadialHoverLabel;
    private int draftCmdVisibleLines;

    private boolean draftAnimEnabled;
    private boolean draftAnimOpenClose;
    private boolean draftAnimHover;
    private double draftHoverGrowPct;
    private int draftOpenCloseMs;
    private String draftOpenStyle;
    private String draftOpenDirection;
    private String draftHoverStyle;

    private int draftDeadzone;
    private int draftBaseOuterRadius;
    private int draftRingThickness;
    private int draftScaleStartThreshold;
    private int draftScalePerItem;
    private int draftSliceGapDeg;
    private String draftDesignStyle;
    private int draftRingColor;
    private int draftHoverColor;
    private int draftBorderColor;
    private int draftTextColor;

    private EditorCycleButton<Boolean> wMoveWhileOpen;
    private EditorCycleButton<Boolean> wShowHoverLabel;
    private EditBox wCmdLines;

    private EditorCycleButton<Boolean> wAnimEnabled;
    private EditorCycleButton<Boolean> wAnimOpenClose;
    private EditorCycleButton<Boolean> wAnimHover;
    private EditorCycleButton<String> wOpenStyle;
    private EditorCycleButton<String> wOpenDirection;
    private EditorCycleButton<String> wHoverStyle;
    private EditBox wHoverGrowPct;
    private EditBox wOpenCloseMs;

    private EditBox wDeadzone;
    private EditBox wOuter;
    private EditBox wThick;
    private EditBox wScaleStart;
    private EditBox wScalePer;
    private EditBox wSliceGap;
    private EditorCycleButton<String> wDesignStyle;
    private EditorButton wRingPick;
    private EditorButton wHoverPick;
    private EditorButton wBorderPick;
    private EditorButton wTextPick;
    private EditorButton btnGeneral;
    private EditorButton btnAnim;
    private EditorButton btnDesign;
    private final ActionEditorUi.ScrollArea scroll = new ActionEditorUi.ScrollArea();
    private int rightViewX;
    private int rightViewY;
    private int rightViewW;
    private int rightViewH;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("ezactions.gui.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!skipReloadDraftsOnce) {
            readCurrentIntoDrafts();
        } else {
            skipReloadDraftsOnce = false;
        }
        buildUI();
    }

    private void readCurrentIntoDrafts() {
        try {
            draftMoveWhileRadialOpen = GeneralClientConfig.CONFIG.moveWhileRadialOpen();
            draftShowRadialHoverLabel = GeneralClientConfig.CONFIG.showRadialHoverLabel();
            draftCmdVisibleLines = GeneralClientConfig.CONFIG.commandEditorVisibleLines();

            draftAnimEnabled = RadialAnimConfig.CONFIG.animationsEnabled();
            draftAnimOpenClose = RadialAnimConfig.CONFIG.animOpenClose();
            draftAnimHover = RadialAnimConfig.CONFIG.animHover();
            draftHoverGrowPct = RadialAnimConfig.CONFIG.hoverGrowPct();
            draftOpenCloseMs = RadialAnimConfig.CONFIG.openCloseMs();
            draftOpenStyle = safeStyle(RadialAnimConfig.CONFIG.openStyle(), "WIPE", "WIPE", "FADE", "NONE");
            draftOpenDirection = safeStyle(RadialAnimConfig.CONFIG.openDirection(), "CW", "CW", "CCW");
            draftHoverStyle = safeStyle(RadialAnimConfig.CONFIG.hoverStyle(), "FILL_SCALE", "FILL_SCALE", "FILL_ONLY", "SCALE_ONLY", "NONE");

            draftDeadzone = DesignClientConfig.deadzone.get();
            draftBaseOuterRadius = DesignClientConfig.baseOuterRadius.get();
            draftRingThickness = DesignClientConfig.ringThickness.get();
            draftScaleStartThreshold = DesignClientConfig.scaleStartThreshold.get();
            draftScalePerItem = DesignClientConfig.scalePerItem.get();
            draftSliceGapDeg = DesignClientConfig.sliceGapDeg.get();
            draftDesignStyle = safeStyle(DesignClientConfig.designStyle.get(), "SOLID", "SOLID", "SEGMENTED", "OUTLINE", "GLASS");
            draftRingColor = DesignClientConfig.ringColor.get();
            draftHoverColor = DesignClientConfig.hoverColor.get();
            draftBorderColor = DesignClientConfig.borderColor.get();
            draftTextColor = DesignClientConfig.textColor.get();
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ConfigScreen: failed reading current specs: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private void buildUI() {
        this.clearWidgets();
        this.scroll.reset();

        final int pad = 12;
        final int leftW = 160;
        final int fieldW = 180;
        final int fieldH = 20;
        final int rightInnerPad = 16;
        final int headerY = pad + 8;
        final int formTopGap = 18;

        int x = pad;
        int y = pad;

        btnGeneral = ActionEditorUi.button(x, y, leftW, 20, Component.translatable("ezactions.gui.config.section.general"), () -> {
            section = Section.GENERAL;
            buildUI();
        }).setForcedHovered(section == Section.GENERAL);
        addRenderableWidget(btnGeneral);
        y += 24;

        btnAnim = ActionEditorUi.button(x, y, leftW, 20, Component.translatable("ezactions.gui.config.section.animations"), () -> {
            section = Section.ANIM;
            buildUI();
        }).setForcedHovered(section == Section.ANIM);
        addRenderableWidget(btnAnim);
        y += 24;

        btnDesign = ActionEditorUi.button(x, y, leftW, 20, Component.translatable("ezactions.gui.config.section.design"), () -> {
            section = Section.DESIGN;
            buildUI();
        }).setForcedHovered(section == Section.DESIGN);
        addRenderableWidget(btnDesign);

        int bottom = this.height - pad;
        addRenderableWidget(ActionEditorUi.button(x, bottom - 22, leftW, 20, Component.translatable("ezactions.gui.common.save"), this::onSave));
        addRenderableWidget(ActionEditorUi.button(x, bottom - 46, leftW, 20, Component.translatable("ezactions.gui.common.back"), this::onClose));
        addRenderableWidget(ActionEditorUi.button(x, bottom - 70, leftW, 20, Component.translatable("ezactions.gui.common.preview"), this::onPreview));

        int rightPanelX = pad + leftW + pad;
        int formX = rightPanelX + rightInnerPad;
        int formY = headerY + formTopGap;
        rightViewX = formX - 2;
        rightViewY = formY - 2;
        rightViewW = this.width - pad - rightViewX - 4;
        rightViewH = this.height - pad - rightViewY - 6;
        int row = 0;

        switch (section) {
            case GENERAL -> {
                wMoveWhileOpen = scroll.track(addRenderableWidget(
                        ActionEditorUi.cycleButton(
                                formX,
                                formY + row * 28,
                                fieldW,
                                fieldH,
                                Component.translatable("ezactions.gui.config.general.move_while_open"),
                                draftMoveWhileRadialOpen,
                                b -> Component.translatable(Boolean.TRUE.equals(b) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                                b -> draftMoveWhileRadialOpen = Boolean.TRUE.equals(b),
                                Boolean.TRUE,
                                Boolean.FALSE
                        )
                ));
                row++;

                wShowHoverLabel = scroll.track(addRenderableWidget(
                        ActionEditorUi.cycleButton(
                                formX,
                                formY + row * 28,
                                fieldW,
                                fieldH,
                                Component.translatable("ezactions.gui.config.general.show_hover_label"),
                                draftShowRadialHoverLabel,
                                b -> Component.translatable(Boolean.TRUE.equals(b) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                                b -> draftShowRadialHoverLabel = Boolean.TRUE.equals(b),
                                Boolean.TRUE,
                                Boolean.FALSE
                        )
                ));
                row++;

                wCmdLines = new EditBox(this.font, formX, formY + row * 28, fieldW, fieldH, Component.literal(""));
                wCmdLines.setValue(Integer.toString(draftCmdVisibleLines));
                scroll.track(addRenderableWidget(wCmdLines));
            }

            case ANIM -> {
                wAnimEnabled = scroll.track(addRenderableWidget(
                        ActionEditorUi.cycleButton(
                                formX,
                                formY + row * 28,
                                fieldW,
                                fieldH,
                                Component.translatable("ezactions.gui.config.anim.enabled"),
                                draftAnimEnabled,
                                b -> Component.translatable(Boolean.TRUE.equals(b) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                                b -> draftAnimEnabled = Boolean.TRUE.equals(b),
                                Boolean.TRUE,
                                Boolean.FALSE
                        )
                ));
                row++;

                wAnimOpenClose = scroll.track(addRenderableWidget(
                        ActionEditorUi.cycleButton(
                                formX,
                                formY + row * 28,
                                fieldW,
                                fieldH,
                                Component.translatable("ezactions.gui.config.anim.open_close"),
                                draftAnimOpenClose,
                                b -> Component.translatable(Boolean.TRUE.equals(b) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                                b -> draftAnimOpenClose = Boolean.TRUE.equals(b),
                                Boolean.TRUE,
                                Boolean.FALSE
                        )
                ));
                row++;

                wAnimHover = scroll.track(addRenderableWidget(
                        ActionEditorUi.cycleButton(
                                formX,
                                formY + row * 28,
                                fieldW,
                                fieldH,
                                Component.translatable("ezactions.gui.config.anim.hover"),
                                draftAnimHover,
                                b -> Component.translatable(Boolean.TRUE.equals(b) ? "ezactions.gui.common.toggle.on" : "ezactions.gui.common.toggle.off"),
                                b -> draftAnimHover = Boolean.TRUE.equals(b),
                                Boolean.TRUE,
                                Boolean.FALSE
                        )
                ));
                row++;

                wOpenStyle = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                        formX,
                        formY + row * 28,
                        fieldW,
                        fieldH,
                        Component.translatable("ezactions.gui.config.anim.open_style"),
                        draftOpenStyle,
                        v -> Component.translatable("ezactions.gui.config.value.open_style." + (v == null ? "wipe" : v.toLowerCase(Locale.ROOT))),
                        v -> draftOpenStyle = v == null ? draftOpenStyle : v,
                        "WIPE",
                        "FADE",
                        "NONE"
                )));
                row++;

                wOpenDirection = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                        formX,
                        formY + row * 28,
                        fieldW,
                        fieldH,
                        Component.translatable("ezactions.gui.config.anim.direction"),
                        draftOpenDirection,
                        v -> Component.translatable("ezactions.gui.config.value.direction." + (v == null ? "cw" : v.toLowerCase(Locale.ROOT))),
                        v -> draftOpenDirection = v == null ? draftOpenDirection : v,
                        "CW",
                        "CCW"
                )));
                row++;

                wHoverStyle = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                        formX,
                        formY + row * 28,
                        fieldW,
                        fieldH,
                        Component.translatable("ezactions.gui.config.anim.hover_style"),
                        draftHoverStyle,
                        v -> Component.translatable("ezactions.gui.config.value.hover_style." + (v == null ? "fill_scale" : v.toLowerCase(Locale.ROOT))),
                        v -> draftHoverStyle = v == null ? draftHoverStyle : v,
                        "FILL_SCALE",
                        "FILL_ONLY",
                        "SCALE_ONLY",
                        "NONE"
                )));
                row++;

                wHoverGrowPct = new EditBox(this.font, formX, formY + row * 28, fieldW, fieldH, Component.literal(""));
                wHoverGrowPct.setValue(Double.toString(draftHoverGrowPct));
                scroll.track(addRenderableWidget(wHoverGrowPct));
                row++;

                wOpenCloseMs = new EditBox(this.font, formX, formY + row * 28, fieldW, fieldH, Component.literal(""));
                wOpenCloseMs.setValue(Integer.toString(draftOpenCloseMs));
                scroll.track(addRenderableWidget(wOpenCloseMs));
            }

            case DESIGN -> {
                wDeadzone = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftDeadzone); row++;
                wOuter = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftBaseOuterRadius); row++;
                wThick = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftRingThickness); row++;
                wScaleStart = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftScaleStartThreshold); row++;
                wScalePer = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftScalePerItem); row++;
                wSliceGap = addIntBox(formX, formY + row * 28, fieldW, fieldH, draftSliceGapDeg); row++;

                wDesignStyle = scroll.track(addRenderableWidget(ActionEditorUi.cycleButton(
                        formX,
                        formY + row * 28,
                        fieldW,
                        fieldH,
                        Component.translatable("ezactions.gui.config.design.style"),
                        draftDesignStyle,
                        v -> Component.translatable("ezactions.gui.config.value.design_style." + (v == null ? "solid" : v.toLowerCase(Locale.ROOT))),
                        v -> draftDesignStyle = v == null ? draftDesignStyle : v,
                        "SOLID",
                        "SEGMENTED",
                        "OUTLINE",
                        "GLASS"
                )));
                row++;

                wRingPick = ActionEditorUi.button(formX, formY + row * 28, fieldW, fieldH, Component.translatable("ezactions.gui.config.design.pick_ring_color"), () -> {
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftRingColor, argb -> draftRingColor = argb));
                });
                scroll.track(addRenderableWidget(wRingPick));
                row++;

                wHoverPick = ActionEditorUi.button(formX, formY + row * 28, fieldW, fieldH, Component.translatable("ezactions.gui.config.design.pick_hover_color"), () -> {
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftHoverColor, argb -> draftHoverColor = argb));
                });
                scroll.track(addRenderableWidget(wHoverPick));
                row++;

                wBorderPick = ActionEditorUi.button(formX, formY + row * 28, fieldW, fieldH, Component.translatable("ezactions.gui.config.design.pick_border_color"), () -> {
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftBorderColor, argb -> draftBorderColor = argb));
                });
                scroll.track(addRenderableWidget(wBorderPick));
                row++;

                wTextPick = ActionEditorUi.button(formX, formY + row * 28, fieldW, fieldH, Component.translatable("ezactions.gui.config.design.pick_text_color"), () -> {
                    this.skipReloadDraftsOnce = true;
                    this.minecraft.setScreen(new ColorPickerScreen(this, draftTextColor, argb -> draftTextColor = argb));
                });
                scroll.track(addRenderableWidget(wTextPick));
            }
        }

        scroll.include(formY, formY + (row * 28) + 60);
        scroll.layout(rightViewY, rightViewY + rightViewH);
    }

    private EditBox addIntBox(int x, int y, int w, int h, int value) {
        EditBox eb = new EditBox(this.font, x, y, w, h, Component.literal(""));
        eb.setValue(Integer.toString(value));
        scroll.track(addRenderableWidget(eb));
        return eb;
    }

    private void onPreview() {
        try {
            readDraftsFromWidgetsForPreviewOnly();
            this.skipReloadDraftsOnce = true;
            RadialMenu.TemporaryStyle s = new RadialMenu.TemporaryStyle(
                    draftRingColor, draftHoverColor, draftBorderColor, draftTextColor,
                    draftAnimEnabled, draftAnimOpenClose, draftAnimHover,
                    draftOpenCloseMs, draftHoverGrowPct, draftOpenStyle, draftOpenDirection, draftHoverStyle,
                    draftDeadzone, draftBaseOuterRadius, draftRingThickness,
                    draftScaleStartThreshold, draftScalePerItem, draftSliceGapDeg, draftDesignStyle
            );
            this.minecraft.setScreen(new RadialPreviewScreen(this, s));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Preview failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    private void readDraftsFromWidgetsForPreviewOnly() {
        if (section == Section.ANIM) {
            draftAnimEnabled = wAnimEnabled != null && Boolean.TRUE.equals(wAnimEnabled.getValue());
            draftAnimOpenClose = wAnimOpenClose != null && Boolean.TRUE.equals(wAnimOpenClose.getValue());
            draftAnimHover = wAnimHover != null && Boolean.TRUE.equals(wAnimHover.getValue());
            draftOpenStyle = safeStyle(wOpenStyle == null ? draftOpenStyle : wOpenStyle.getValue(), "WIPE", "WIPE", "FADE", "NONE");
            draftOpenDirection = safeStyle(wOpenDirection == null ? draftOpenDirection : wOpenDirection.getValue(), "CW", "CW", "CCW");
            draftHoverStyle = safeStyle(wHoverStyle == null ? draftHoverStyle : wHoverStyle.getValue(),
                    "FILL_SCALE", "FILL_SCALE", "FILL_ONLY", "SCALE_ONLY", "NONE");
            draftHoverGrowPct = clampDouble(parseSafeDouble(wHoverGrowPct, draftHoverGrowPct), 0.0, 0.5);
            draftOpenCloseMs = clamp(parseSafeInt(wOpenCloseMs, draftOpenCloseMs), 0, 2000);
        } else if (section == Section.DESIGN) {
            draftDeadzone = clamp(parseSafeInt(wDeadzone, draftDeadzone), 0, 90);
            draftBaseOuterRadius = clamp(parseSafeInt(wOuter, draftBaseOuterRadius), 24, 512);
            draftRingThickness = clamp(parseSafeInt(wThick, draftRingThickness), 6, 256);
            draftScaleStartThreshold = clamp(parseSafeInt(wScaleStart, draftScaleStartThreshold), 0, 128);
            draftScalePerItem = clamp(parseSafeInt(wScalePer, draftScalePerItem), 0, 100);
            draftSliceGapDeg = clamp(parseSafeInt(wSliceGap, draftSliceGapDeg), 0, 12);
            draftDesignStyle = safeStyle(wDesignStyle == null ? draftDesignStyle : wDesignStyle.getValue(),
                    "SOLID", "SOLID", "SEGMENTED", "OUTLINE", "GLASS");
        }
    }

    private void onSave() {
        try {
            switch (section) {
                case GENERAL -> {
                    draftMoveWhileRadialOpen = wMoveWhileOpen != null && Boolean.TRUE.equals(wMoveWhileOpen.getValue());
                    draftShowRadialHoverLabel = wShowHoverLabel != null && Boolean.TRUE.equals(wShowHoverLabel.getValue());
                    draftCmdVisibleLines = clamp(parseSafeInt(wCmdLines, 5), 1, 20);

                    GeneralClientConfig.CONFIG.moveWhileRadialOpen.set(draftMoveWhileRadialOpen);
                    GeneralClientConfig.CONFIG.showRadialHoverLabel.set(draftShowRadialHoverLabel);
                    GeneralClientConfig.CONFIG.commandEditorVisibleLines.set(draftCmdVisibleLines);
                    ConfigIO.saveNow(ConfigIO.Section.GENERAL);
                }
                case ANIM -> {
                    draftAnimEnabled = wAnimEnabled != null && Boolean.TRUE.equals(wAnimEnabled.getValue());
                    draftAnimOpenClose = wAnimOpenClose != null && Boolean.TRUE.equals(wAnimOpenClose.getValue());
                    draftAnimHover = wAnimHover != null && Boolean.TRUE.equals(wAnimHover.getValue());
                    draftOpenStyle = safeStyle(wOpenStyle == null ? draftOpenStyle : wOpenStyle.getValue(), "WIPE", "WIPE", "FADE", "NONE");
                    draftOpenDirection = safeStyle(wOpenDirection == null ? draftOpenDirection : wOpenDirection.getValue(), "CW", "CW", "CCW");
                    draftHoverStyle = safeStyle(wHoverStyle == null ? draftHoverStyle : wHoverStyle.getValue(),
                            "FILL_SCALE", "FILL_SCALE", "FILL_ONLY", "SCALE_ONLY", "NONE");
                    draftHoverGrowPct = clampDouble(parseSafeDouble(wHoverGrowPct, 0.05), 0.0, 0.5);
                    draftOpenCloseMs = clamp(parseSafeInt(wOpenCloseMs, 125), 0, 2000);

                    RadialAnimConfig.CONFIG.animationsEnabled.set(draftAnimEnabled);
                    RadialAnimConfig.CONFIG.animOpenClose.set(draftAnimOpenClose);
                    RadialAnimConfig.CONFIG.animHover.set(draftAnimHover);
                    RadialAnimConfig.CONFIG.openStyle.set(draftOpenStyle);
                    RadialAnimConfig.CONFIG.openDirection.set(draftOpenDirection);
                    RadialAnimConfig.CONFIG.hoverStyle.set(draftHoverStyle);
                    RadialAnimConfig.CONFIG.hoverGrowPct.set(draftHoverGrowPct);
                    RadialAnimConfig.CONFIG.openCloseMs.set(draftOpenCloseMs);
                    ConfigIO.saveNow(ConfigIO.Section.ANIM);
                }
                case DESIGN -> {
                    draftDeadzone = clamp(parseSafeInt(wDeadzone, 18), 0, 90);
                    draftBaseOuterRadius = clamp(parseSafeInt(wOuter, 72), 24, 512);
                    draftRingThickness = clamp(parseSafeInt(wThick, 28), 6, 256);
                    draftScaleStartThreshold = clamp(parseSafeInt(wScaleStart, 8), 0, 128);
                    draftScalePerItem = clamp(parseSafeInt(wScalePer, 6), 0, 100);
                    draftSliceGapDeg = clamp(parseSafeInt(wSliceGap, 0), 0, 12);
                    draftDesignStyle = safeStyle(wDesignStyle == null ? draftDesignStyle : wDesignStyle.getValue(),
                            "SOLID", "SOLID", "SEGMENTED", "OUTLINE", "GLASS");

                    DesignClientConfig.deadzone.set(draftDeadzone);
                    DesignClientConfig.baseOuterRadius.set(draftBaseOuterRadius);
                    DesignClientConfig.ringThickness.set(draftRingThickness);
                    DesignClientConfig.scaleStartThreshold.set(draftScaleStartThreshold);
                    DesignClientConfig.scalePerItem.set(draftScalePerItem);
                    DesignClientConfig.sliceGapDeg.set(draftSliceGapDeg);
                    DesignClientConfig.designStyle.set(draftDesignStyle);
                    DesignClientConfig.ringColor.set(draftRingColor);
                    DesignClientConfig.hoverColor.set(draftHoverColor);
                    DesignClientConfig.borderColor.set(draftBorderColor);
                    DesignClientConfig.textColor.set(draftTextColor);
                    ConfigIO.saveNow(ConfigIO.Section.DESIGN);

                    // Design values are cached in RadialConfig.
                    RadialConfig.invalidate();
                }
            }

            Constants.LOG.info("[{}] Config saved (section: {})", Constants.MOD_NAME, section.name());
            this.minecraft.setScreen(parent);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Config save failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        final int pad = 12;
        final int leftW = 160;

        g.fill(0, 0, width, height, 0x88000000);
        g.fill(pad, pad, pad + leftW, this.height - pad, 0xC0101010);
        int rightPanelX = pad + leftW + pad;
        g.fill(rightPanelX, pad, this.width - pad, this.height - pad, 0xC0101010);
        scroll.layout(rightViewY, rightViewY + rightViewH);

        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        switch (section) {
            case GENERAL, ANIM, DESIGN -> renderHoverAssistTooltip(g, mouseX, mouseY);
        }

        scroll.drawScrollbar(g, rightViewX, rightViewY, rightViewW, rightViewH);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHoverAssistTooltip(GuiGraphics g, int mouseX, int mouseY) {
        switch (section) {
            case GENERAL -> {
                if (renderTipIfHovered(g, mouseX, mouseY, wCmdLines, Component.translatable("ezactions.gui.config.tooltip.general.visible_lines"))) return;
            }
            case ANIM -> {
                if (renderTipIfHovered(g, mouseX, mouseY, wHoverGrowPct, Component.translatable("ezactions.gui.config.tooltip.anim.hover_grow"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wOpenCloseMs, Component.translatable("ezactions.gui.config.tooltip.anim.open_close_ms"))) return;
            }
            case DESIGN -> {
                if (renderTipIfHovered(g, mouseX, mouseY, wDeadzone, Component.translatable("ezactions.gui.config.tooltip.design.deadzone"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wOuter, Component.translatable("ezactions.gui.config.tooltip.design.outer_radius"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wThick, Component.translatable("ezactions.gui.config.tooltip.design.ring_thickness"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wScaleStart, Component.translatable("ezactions.gui.config.tooltip.design.scale_start"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wScalePer, Component.translatable("ezactions.gui.config.tooltip.design.scale_per_item"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wSliceGap, Component.translatable("ezactions.gui.config.tooltip.design.slice_gap"))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wRingPick, Component.translatable("ezactions.gui.config.tooltip.design.ring_color", ColorUtil.toHexARGB(draftRingColor)))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wHoverPick, Component.translatable("ezactions.gui.config.tooltip.design.hover_color", ColorUtil.toHexARGB(draftHoverColor)))) return;
                if (renderTipIfHovered(g, mouseX, mouseY, wBorderPick, Component.translatable("ezactions.gui.config.tooltip.design.border_color", ColorUtil.toHexARGB(draftBorderColor)))) return;
                renderTipIfHovered(g, mouseX, mouseY, wTextPick, Component.translatable("ezactions.gui.config.tooltip.design.text_color", ColorUtil.toHexARGB(draftTextColor)));
            }
        }
    }

    private boolean renderTipIfHovered(GuiGraphics g, int mouseX, int mouseY, AbstractWidget widget, Component text) {
        if (widget == null || text == null || text.getString().isBlank()) {
            return false;
        }
        if (!widget.visible || !widget.active) {
            return false;
        }
        int x = widget.x;
        int y = widget.y;
        int w = widget.getWidth();
        int h = widget.getHeight();
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
            return false;
        }
        g.renderTooltip(this.font, text, mouseX, mouseY);
        return true;
    }

    private static int parseSafeInt(EditBox eb, int dflt) {
        try { return Integer.parseInt(eb.getValue().trim()); } catch (Throwable ignored) { return dflt; }
    }

    private static double parseSafeDouble(EditBox eb, double dflt) {
        try { return Double.parseDouble(eb.getValue().trim()); } catch (Throwable ignored) { return dflt; }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clampDouble(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String safeStyle(String in, String dflt, String... allowed) {
        if (in == null) return dflt;
        String up = in.trim().toUpperCase(Locale.ROOT);
        for (String s : allowed) if (s.equals(up)) return up;
        return dflt;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scroll.mouseScrolled(mouseX, mouseY, delta, rightViewX, rightViewY, rightViewW, rightViewH)) {
            scroll.layout(rightViewY, rightViewY + rightViewH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}




