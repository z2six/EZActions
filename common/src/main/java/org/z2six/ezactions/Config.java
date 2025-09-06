// MainFile: src/main/java/org/z2six/ezactions/Config.java
package org.z2six.ezactions;

/**
 * Minimal config scaffold for ezactions.
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
