// MainFile: src/main/java/org/z2six/ezactions/gui/anim/RadialTransition.java
package ezactions.gui.anim;
import org.z2six.ezactions.config.RadialAnimConfigView;

import static org.z2six.ezactions.gui.anim.AnimLerp.clamp01;

/**
 * Tiny state machine for radial open/close and category transitions.
 * Progress is time-based; callers drive it with System.currentTimeMillis().
 */
public final class RadialTransition {

    public enum Phase {
        NONE,
        OPENING,
        CLOSING,
        CAT_WIPE_OUT,
        CAT_WIPE_IN
    }

    private Phase phase = Phase.NONE;
    private long start = 0L;
    private int durationMs = 0;

    public RadialTransition() {}

    public Phase phase() { return phase; }

    public boolean isActive() { return phase != Phase.NONE; }

    public void startOpening(long nowMs, int durMs) {
        phase = Phase.OPENING;
        start = nowMs;
        durationMs = Math.max(0, durMs);
    }

    public void startClosing(long nowMs, int durMs) {
        phase = Phase.CLOSING;
        start = nowMs;
        durationMs = Math.max(0, durMs);
    }

    public void startCatWipeOut(long nowMs, int durMs) {
        phase = Phase.CAT_WIPE_OUT;
        start = nowMs;
        durationMs = Math.max(0, durMs);
    }

    public void startCatWipeIn(long nowMs, int durMs) {
        phase = Phase.CAT_WIPE_IN;
        start = nowMs;
        durationMs = Math.max(0, durMs);
    }

    /**
     * Normalized progress [0..1] of the current transition phase.
     */
    public double phaseProgress(long nowMs) {
        if (phase == Phase.NONE) return 1.0;
        return clamp01(AnimLerp.progress(nowMs, start, durationMs));
    }

    /** True if the active transition has completed. */
    public boolean isDone(long nowMs) {
        return phaseProgress(nowMs) >= 1.0;
    }

    // --- Compatibility helpers so callers can use start(int) / progress() ---

    /** Start now with direction (+1=open, -1=close) using configured duration. */
    public void start(int dir) {
        long now = System.currentTimeMillis();
        int ms = Math.max(1, RadialAnimConfigView.get().openCloseMs);
        if (dir >= 0) startOpening(now, ms);
        else          startClosing(now, ms);
    }

    /** Start at explicit time with direction (+1=open, -1=close) using configured duration. */
    public void start(int dir, long nowMs) {
        int ms = Math.max(1, RadialAnimConfigView.get().openCloseMs);
        if (dir >= 0) startOpening(nowMs, ms);
        else          startClosing(nowMs, ms);
    }

    /** Back-compat alias some call sites may use. */
    public void startNow(int dir) { start(dir); }

    /** Convenience: progress at current time (0..1). */
    public float progress() {
        return (float) phaseProgress(System.currentTimeMillis());
    }

    /** Convenience: progress at explicit time (0..1). */
    public float progress(long nowMs) {
        return (float) phaseProgress(nowMs);
    }

    public void clear() {
        phase = Phase.NONE;
        start = 0L;
        durationMs = 0;
    }
}
