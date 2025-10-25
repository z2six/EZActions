package ezactions.gui.overlay;

import net.minecraft.client.gui.GuiGraphics;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.data.icon.IconSpec;
import org.z2six.ezactions.gui.IconRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Draws small "linger" icons after an action is activated (even after the radial closes).
 * NOTE: This class is not registered to any event yet; we’ll wire it up
 * in the "modify" step by calling render(...) from a HUD overlay event.
 */
public final class RadialLingerOverlay {

    private RadialLingerOverlay() {}

    /** One linger job. */
    private static final class Job {
        final IconSpec icon;
        final int x;
        final int y;
        final long startMs;
        final int durationMs;

        Job(IconSpec icon, int x, int y, int durationMs) {
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.startMs = System.currentTimeMillis();
            this.durationMs = Math.max(0, durationMs);
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    /** Queue a linger icon at screen-space coordinates (centered on x,y). */
    public static void add(IconSpec icon, int screenX, int screenY, int durationMs) {
        try {
            if (icon == null || durationMs <= 0) return;
            JOBS.add(new Job(icon, screenX, screenY, durationMs));
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Linger add failed: {}", Constants.MOD_NAME, t.toString());
        }
    }

    /**
     * Render active linger icons. Call this from a HUD overlay event
     * (e.g., in the modify step we’ll hook into NeoForge’s GUI render pass).
     */
    public static void render(GuiGraphics g) {
        if (JOBS.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            double t = (now - j.startMs) / (double) j.durationMs;
            if (t >= 1.0) {
                it.remove();
                continue;
            }

            // Simple fade-out by drawing a subtle overlay box behind/around the icon.
            // (We’ll switch to alpha-tinted icon draw when we refactor IconRenderer for tint.)
            try {
                // Draw icon itself
                IconRenderer.drawIcon(g, j.x, j.y, j.icon);

                // Dim-out overlay proportional to t (start subtle, grow to ~60% towards the end)
                int alpha = (int) (t * 150.0); // 0..150
                int aargb = (alpha << 24);     // black with variable alpha
                g.fill(j.x - 12, j.y - 12, j.x + 12, j.y + 12, aargb);
            } catch (Throwable ex) {
                Constants.LOG.warn("[{}] Linger render error: {}", Constants.MOD_NAME, ex.toString());
            }
        }
    }

    /** Clear all pending linger effects. */
    public static void clear() {
        JOBS.clear();
    }
}
