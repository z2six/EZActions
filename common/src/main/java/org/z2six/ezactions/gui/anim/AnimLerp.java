package org.z2six.ezactions.gui.anim;

/**
 * Tiny easing / interpolation helpers for time-based UI.
 */
public final class AnimLerp {
    private AnimLerp() {}

    /** Clamp to [0,1]. */
    public static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    /** Linear interpolation. */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Given now/start/duration (ms), return normalized progress [0..1]. */
    public static double progress(long nowMs, long startMs, int durationMs) {
        if (durationMs <= 0) return 1.0;
        double t = (nowMs - startMs) / (double) durationMs;
        return clamp01(t);
    }

    /* ---------- Easing ---------- */

    /** Ease-out cubic (fast start, slow end). */
    public static double easeOutCubic(double t) {
        t = clamp01(t);
        double inv = (t - 1.0);
        return 1.0 + inv * inv * inv;
    }

    /** Ease-in cubic (slow start, fast end). */
    public static double easeInCubic(double t) {
        t = clamp01(t);
        return t * t * t;
    }

    /** Symmetric ease-in-out (quadratic). */
    public static double easeInOutQuad(double t) {
        t = clamp01(t);
        if (t < 0.5) return 2.0 * t * t;
        return -1.0 + (4.0 - 2.0 * t) * t;
    }
}
