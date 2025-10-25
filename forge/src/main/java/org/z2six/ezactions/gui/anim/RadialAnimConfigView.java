package org.z2six.ezactions.gui.anim;

/*
 * Runtime view of animation configuration with safe defaults.
 * Kept decoupled from your config system for now; when we add
 * RadialMenuConfig fields we’ll source these values there.
 */
public final class RadialAnimConfigView {

    private RadialAnimConfigView() {}

    /* -------- Master + feature toggles (defaults) -------- */

    // Global on/off for ALL animations
    public static boolean enableAnimations() { return true; }

    public static boolean enableHoverAnim() { return true; }

    public static boolean enableActivateAnim() { return true; }

    public static boolean enableOpenCloseAnim() { return true; }

    public static boolean enableCategoryAnim() { return true; }

    /* -------- Hover animation knobs -------- */

    /* Scale factor for hovered slice pop (e.g., 1.05 = +5%). */
    public static double hoverScale() { return 1.05; }

    /* Duration (ms) for pop scale-in/out. */
    public static int hoverScaleMs() { return 100; }

    /* Duration (ms) for clockwise wipe overlay on hover. */
    public static int hoverWipeMs() { return 160; }

    /* Hover overlay color (ARGB). */
    public static int hoverColor() { return 0xFFCC3344; }

    /* Fallback tint color when animations are disabled (ARGB). */
    public static int instantHoverColor() { return 0xFFCC3344; }

    /* Optional small angle hysteresis to reduce boundary flicker (degrees). */
    public static double hoverHysteresisDeg() { return 0.0; }

    /* -------- Activation animation knobs -------- */

    /* Whole radial fade-out duration on activation (ms). */
    public static int radialFadeOutMs() { return 100; }

    /* Icon linger duration (ms) after radial closes. */
    public static int iconLingerMs() { return 500; }

    /* -------- Open/Close timings -------- */

    /* Open radial clockwise wipe (ms). */
    public static int openWipeMs() { return 250; }

    /* Close radial wipe (ms). */
    public static int closeWipeMs() { return 200; }

    /* -------- Category transition timings -------- */

    /* Wipe-out old category (ms). */
    public static int categoryWipeOutMs() { return 125; }

    /* Wipe-in new category (ms). */
    public static int categoryWipeInMs() { return 125; }
}
