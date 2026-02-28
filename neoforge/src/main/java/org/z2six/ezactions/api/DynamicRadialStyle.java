package org.z2six.ezactions.api;

import org.jetbrains.annotations.Nullable;

/** Optional render/style overrides for a temporary radial opened via API. */
public final class DynamicRadialStyle {
    private final @Nullable Integer ringColor;
    private final @Nullable Integer hoverColor;
    private final @Nullable Integer borderColor;
    private final @Nullable Integer textColor;
    private final @Nullable Boolean animationsEnabled;
    private final @Nullable Boolean animOpenClose;
    private final @Nullable Boolean animHover;
    private final @Nullable Integer openCloseMs;
    private final @Nullable Double hoverGrowPct;
    private final @Nullable String openStyle;
    private final @Nullable String openDirection;
    private final @Nullable String hoverStyle;
    private final @Nullable Integer deadzone;
    private final @Nullable Integer baseOuterRadius;
    private final @Nullable Integer ringThickness;
    private final @Nullable Integer scaleStartThreshold;
    private final @Nullable Integer scalePerItem;
    private final @Nullable Integer sliceGapDeg;
    private final @Nullable String designStyle;

    public DynamicRadialStyle(@Nullable Integer ringColor,
                              @Nullable Integer hoverColor,
                              @Nullable Integer borderColor,
                              @Nullable Integer textColor,
                              @Nullable Boolean animationsEnabled,
                              @Nullable Boolean animOpenClose,
                              @Nullable Boolean animHover,
                              @Nullable Integer openCloseMs,
                              @Nullable Double hoverGrowPct,
                              @Nullable String openStyle,
                              @Nullable String openDirection,
                              @Nullable String hoverStyle,
                              @Nullable Integer deadzone,
                              @Nullable Integer baseOuterRadius,
                              @Nullable Integer ringThickness,
                              @Nullable Integer scaleStartThreshold,
                              @Nullable Integer scalePerItem,
                              @Nullable Integer sliceGapDeg,
                              @Nullable String designStyle) {
        this.ringColor = ringColor;
        this.hoverColor = hoverColor;
        this.borderColor = borderColor;
        this.textColor = textColor;
        this.animationsEnabled = animationsEnabled;
        this.animOpenClose = animOpenClose;
        this.animHover = animHover;
        this.openCloseMs = openCloseMs;
        this.hoverGrowPct = hoverGrowPct;
        this.openStyle = openStyle;
        this.openDirection = openDirection;
        this.hoverStyle = hoverStyle;
        this.deadzone = deadzone;
        this.baseOuterRadius = baseOuterRadius;
        this.ringThickness = ringThickness;
        this.scaleStartThreshold = scaleStartThreshold;
        this.scalePerItem = scalePerItem;
        this.sliceGapDeg = sliceGapDeg;
        this.designStyle = designStyle;
    }

    public @Nullable Integer ringColor() { return ringColor; }
    public @Nullable Integer hoverColor() { return hoverColor; }
    public @Nullable Integer borderColor() { return borderColor; }
    public @Nullable Integer textColor() { return textColor; }
    public @Nullable Boolean animationsEnabled() { return animationsEnabled; }
    public @Nullable Boolean animOpenClose() { return animOpenClose; }
    public @Nullable Boolean animHover() { return animHover; }
    public @Nullable Integer openCloseMs() { return openCloseMs; }
    public @Nullable Double hoverGrowPct() { return hoverGrowPct; }
    public @Nullable String openStyle() { return openStyle; }
    public @Nullable String openDirection() { return openDirection; }
    public @Nullable String hoverStyle() { return hoverStyle; }
    public @Nullable Integer deadzone() { return deadzone; }
    public @Nullable Integer baseOuterRadius() { return baseOuterRadius; }
    public @Nullable Integer ringThickness() { return ringThickness; }
    public @Nullable Integer scaleStartThreshold() { return scaleStartThreshold; }
    public @Nullable Integer scalePerItem() { return scalePerItem; }
    public @Nullable Integer sliceGapDeg() { return sliceGapDeg; }
    public @Nullable String designStyle() { return designStyle; }
}
