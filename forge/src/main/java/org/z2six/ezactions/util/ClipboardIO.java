// MainFile: src/main/java/org/z2six/ezactions/util/ClipboardIO.java
package org.z2six.ezactions.util;

import net.minecraft.client.Minecraft;
import org.z2six.ezactions.Constants;

/*
 * Tiny wrapper around Minecraft's clipboard helpers with safe logging.
 * - Never throws; returns null/false on failure and logs the reason.
 */
public final class ClipboardIO {

    private ClipboardIO() {}

    /* Get current system clipboard contents, or null if unavailable. */
    public static String getClipboard() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.keyboardHandler == null) {
                Constants.LOG.debug("[{}] Clipboard get: MC not ready.", Constants.MOD_NAME);
                return null;
            }
            String s = mc.keyboardHandler.getClipboard();
            if (s == null) {
                Constants.LOG.debug("[{}] Clipboard get: empty (null).", Constants.MOD_NAME);
                return null;
            }
            // Log only length to avoid dumping user content in logs.
            Constants.LOG.debug("[{}] Clipboard get: {} chars.", Constants.MOD_NAME, s.length());
            return s;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Clipboard get failed: {}", Constants.MOD_NAME, t.toString());
            return null;
        }
    }

    /*
     * Set system clipboard text.
     * @return true on success, false if MC not ready or the platform rejected it.
     */
    public static boolean setClipboard(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.keyboardHandler == null) {
                Constants.LOG.debug("[{}] Clipboard set: MC not ready.", Constants.MOD_NAME);
                return false;
            }
            if (text == null) text = "";
            mc.keyboardHandler.setClipboard(text);
            Constants.LOG.debug("[{}] Clipboard set ok ({} chars).", Constants.MOD_NAME, text.length());
            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Clipboard set failed: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }
}
