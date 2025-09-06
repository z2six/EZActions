package org.z2six.ezactions.gui.anim;

/**
 * Per-slice hover animation:
 * - scaleFor(i): 0..1 grow factor for slice i (used to enlarge outer radius)
 * - sweepFor(i): 0..1 clockwise color sweep progress for slice i
 *
 * Time-based smoothing; stable with changing slice count.
 */
public final class SliceHoverAnim {

    // Default timings (ms)
    private static final int DEFAULT_GROW_MS  = 100;
    private static final int DEFAULT_SWEEP_MS = 180;

    private long lastTickMs = 0L;
    private int sliceCount = 0;
    private int hovered = -1;

    private float[] grow;   // 0..1
    private float[] sweep;  // 0..1

    public SliceHoverAnim() {
        this.grow  = new float[0];
        this.sweep = new float[0];
    }

    /** Simple overload used by RadialMenuScreen. */
    public void tick(long nowMs, int hoveredIndex, int sliceCount) {
        tick(nowMs, hoveredIndex, sliceCount, DEFAULT_GROW_MS, DEFAULT_SWEEP_MS);
    }

    /** Full tick that allows custom durations. */
    public void tick(long nowMs, int hoveredIndex, int sliceCount, int growMs, int sweepMs) {
        if (sliceCount != this.sliceCount) {
            this.sliceCount = Math.max(0, sliceCount);
            this.grow  = new float[this.sliceCount];
            this.sweep = new float[this.sliceCount];
            this.hovered = -1; // reset to avoid ghost values
        }

        long dtMs = (lastTickMs == 0L) ? 0L : Math.max(0L, nowMs - lastTickMs);
        lastTickMs = nowMs;

        // Ensure arrays are sized
        if (this.grow.length != this.sliceCount) {
            this.grow = new float[this.sliceCount];
        }
        if (this.sweep.length != this.sliceCount) {
            this.sweep = new float[this.sliceCount];
        }

        // Smoothing factors per tick
        float kGrow  = (growMs  <= 0) ? 1f : clamp01((float)dtMs / (float)growMs);
        float kSweep = (sweepMs <= 0) ? 1f : clamp01((float)dtMs / (float)sweepMs);

        this.hovered = hoveredIndex;

        for (int i = 0; i < this.sliceCount; i++) {
            boolean isTarget = (i == this.hovered);

            float targetGrow  = isTarget ? 1f : 0f;
            float targetSweep = isTarget ? 1f : 0f;

            // Exponential approach to target (frame-rate independent)
            this.grow[i]  += (targetGrow  - this.grow[i])  * kGrow;
            this.sweep[i] += (targetSweep - this.sweep[i]) * kSweep;

            // Numerical safety
            if (Math.abs(this.grow[i]  - targetGrow)  < 0.001f) this.grow[i]  = targetGrow;
            if (Math.abs(this.sweep[i] - targetSweep) < 0.001f) this.sweep[i] = targetSweep;
        }
    }

    /** 0..1 grow for slice index. */
    public float scaleFor(int index) {
        if (index < 0 || index >= grow.length) return 0f;
        return clamp01(grow[index]);
    }

    /** 0..1 sweep (clockwise) for slice index. */
    public float sweepFor(int index) {
        if (index < 0 || index >= sweep.length) return 0f;
        return clamp01(sweep[index]);
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}
