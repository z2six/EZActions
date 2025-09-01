// MainFile: src/main/java/org/z2six/minemenuplus/Config.java
package org.z2six.minemenuplus;

/**
 * Minimal config scaffold for MineMenuPlus.
 * We’ll wire proper NeoForge config spec later; for now these values
 * are read directly by our helpers. No event subscribers here to avoid
 * version/package drift during bootstrap.
 *
 * Debug logs are handled by callers; this class is intentionally dumb.
 */
public final class Config {

    private Config() {}

    /** General, user-facing toggles. */
    public static final class General {
        /** Allow real input injection via the keyboard callback (mixin). */
        public static boolean allowInputInjection = true;

        /** Don’t inject input while an EditBox (text field) is focused. */
        public static boolean respectTextFields = true;

        /** Cooldown in ms so one menu click => one virtual key tap. */
        public static int inputInjectionCooldownMs = 150;
    }
}
