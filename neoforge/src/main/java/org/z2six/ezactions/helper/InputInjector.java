// MainFile: src/main/java/org/z2six/ezactions/helper/InputInjector.java
package org.z2six.ezactions.helper;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.mixin.KeyboardHandlerAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Centralized input delivery + utility setters used by the radial and editor.
 *
 * NeoForge 1.21.1 edition:
 * - Diagnostics: mapping resolution, AUTO decision making, key/scancode, required modifiers, fallback reasons.
 * - Scancode handling: if key has no scan, derive via GLFW.glfwGetKeyScancode.
 * - Temporary-binding shim (UNBOUND or NO-SCAN):
 *     * Bind mapping to a spare key that has a valid scancode and isn't used, press/release, then restore next tick.
 * - Modifier support:
 *     * Detect required KeyModifier via reflection (NeoForge or Forge). If present, prefer a "modifier shim":
 *          - Temporarily set KeyModifier=NONE (+ fix scancode if needed), inject plain key, restore orig modifier+key.
 *     * If API isnâ€™t available, synthesize modifiers:
 *          - Physically press CTRL/SHIFT/ALT that arenâ€™t already down, send main key with mods mask, then release only
 *            the modifiers we pressed.
 * - AUTO behavior:
 *     * If UNBOUND or NO-SCAN â†’ try shim (with modifiers, if needed).
 *     * If modifiers required and caller asked for TICK â†’ elevate to INPUT (TICK cannot emulate chords).
 * - Defensive and verbose; avoids crashes and restores state on failure.
 */
public final class InputInjector {

    public enum DeliveryMode { AUTO, INPUT, TICK }

    private InputInjector() {}

    /** string-based entry (e.g. "key.inventory" or localized "Inventory") */
    public static boolean deliver(String mappingName, boolean toggle, DeliveryMode mode) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                Constants.LOG.warn("[{}] deliver('{}'): Minecraft instance is null", Constants.MOD_NAME, mappingName);
                return false;
            }

            Resolution res = resolveMappingByName(mc.options, mappingName);
            if (res.mapping == null) {
                Constants.LOG.warn("[{}] deliver('{}'): mapping not found (tried exact, localized, contains).",
                        Constants.MOD_NAME, mappingName);
                return false;
            }

            logResolved(res);
            return deliverKey(res.mapping, null, null, 0, toggle, mode);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] deliver('{}') failed: {}", Constants.MOD_NAME, mappingName, t.toString());
            return false;
        }
    }

    /** lower-level entry */
    public static boolean deliverKey(KeyMapping mapping,
                                     @Nullable Integer explicitGlfwKey,
                                     @Nullable Integer explicitScanCode,
                                     int glfwMods,
                                     boolean toggle,
                                     DeliveryMode mode) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                Constants.LOG.warn("[{}] deliverKey: Minecraft instance null for mapping '{}'", Constants.MOD_NAME, safeName(mapping));
                return false;
            }

            if (isTextInputFocused(mc)) {
                Constants.LOG.debug("[{}] Input injection blocked: text field (Chat) focused.", Constants.MOD_NAME);
                return false;
            }

            final InputConstants.Key current = mapping.getKey();
            final String keyType = (current == null) ? "UNKNOWN" : current.getType().name();

            int key  = (explicitGlfwKey  != null) ? explicitGlfwKey  : keyCodeFrom(mapping);
            int scan = (explicitScanCode != null) ? explicitScanCode : -1;

            final boolean unbound = isUnbound(current);

            // Try to DERIVE scancode if we have a key but no scan
            int derivedScan = (key >= 0) ? GLFW.glfwGetKeyScancode(key) : 0;
            if (scan <= 0 && derivedScan > 0) {
                scan = derivedScan;
            }

            // Detect required modifiers (NeoForge or Forge KeyModifier via reflection)
            ModReq req = detectRequiredModifiers(mapping);

            // Base decision for AUTO (pre-shim/pre-mod elevate)
            final DeliveryMode nominalEff = (mode == DeliveryMode.AUTO)
                    ? (unbound ? DeliveryMode.TICK : DeliveryMode.INPUT)
                    : mode;

            // Log diagnostic
            Constants.LOG.debug(
                    "[{}] Key action fired: mapping='{}' mode={} (nominalEff={}) toggle={} " +
                            "[reason: {}] [type={}, glfwKey={}, scan={}, derivedScan={}, mods={} reqMods={}]",
                    Constants.MOD_NAME, safeName(mapping), mode, nominalEff, toggle,
                    unbound ? "UNBOUND" : (scan > 0 ? "BOUND+SCAN" : "BOUND+NO_SCAN"),
                    keyType, key, scan, derivedScan, glfwMods, req.brief()
            );

            // If modifiers are required, TICK cannot emulate a chord -> elevate to INPUT
            DeliveryMode eff = nominalEff;
            if (req.any() && eff == DeliveryMode.TICK) {
                Constants.LOG.debug("[{}] Elevating TICK->INPUT because '{}' requires modifiers: {}",
                        Constants.MOD_NAME, safeName(mapping), req.brief());
                eff = DeliveryMode.INPUT;
            }

            // ---- Shim / modifier decisions ----

            // A) If modifiers are required at all, try a KeyModifier shim first (NeoForge/Forge via reflection).
            if (req.any()) {
                boolean ok = deliverViaModifierShim(mc, mapping);
                if (ok) return true;

                // If KeyModifier shim failed/unavailable:
                if (scan <= 0) {
                    // No valid scancode -> temp-key shim with synthesized modifiers
                    boolean ok2 = deliverInputViaTemporaryBinding(mc, mapping, req);
                    if (ok2) return true;
                    Constants.LOG.warn("[{}] ModShim failed and temp-key shim failed for '{}'; falling back to legacy TICK.",
                            Constants.MOD_NAME, safeName(mapping));
                    return deliverTick(mapping, toggle);
                }

                // Have a scan; try synthesized modifiers directly
                return deliverInputWithModifiers(mc, key, scan, req);
            }

            // B) No modifiers required: keep existing behavior with improved shims.
            // 1) Explicit INPUT but no valid scancode -> temp-key shim
            if (eff == DeliveryMode.INPUT && scan <= 0) {
                boolean ok = deliverInputViaTemporaryBinding(mc, mapping, req /*empty*/);
                if (ok) return true;
                Constants.LOG.warn("[{}] INPUT (no-scan) shim failed for '{}'; falling back to legacy TICK.",
                        Constants.MOD_NAME, safeName(mapping));
                return deliverTick(mapping, toggle);
            }

            // 2) AUTO: UNBOUND or NO-SCAN -> temp-key shim
            if (mode == DeliveryMode.AUTO && (unbound || scan <= 0)) {
                boolean ok = deliverInputViaTemporaryBinding(mc, mapping, req /*empty*/);
                if (ok) return true;
                Constants.LOG.warn("[{}] AUTO shim failed for '{}' (unboundOrNoScan={}): falling back to {}.",
                        Constants.MOD_NAME, safeName(mapping), (unbound || scan <= 0), (unbound ? "TICK" : "INPUT"));
                return unbound ? deliverTick(mapping, toggle) : deliverInputWithModifiers(mc, key, scan, req /*empty*/);
            }

            // ---- Normal path ----
            return switch (eff) {
                case INPUT -> deliverInputWithModifiers(mc, key, scan, req /*empty -> acts like plain INPUT*/);
                case TICK  -> deliverTick(mapping, toggle);
                case AUTO  -> unbound ? deliverTick(mapping, toggle) : deliverInputWithModifiers(mc, key, scan, req /*empty*/);
            };

        } catch (Throwable t) {
            Constants.LOG.warn("[{}] deliverKey('{}') failed: {}", Constants.MOD_NAME, safeName(mapping), t.toString());
            return false;
        }
    }

    /* -------------------- radial/editor helpers -------------------- */

    /** Set a mapping's pressed state (used for movement passthrough while the radial is open). */
    public static void setKeyPressed(@Nullable KeyMapping mapping, boolean down) {
        if (mapping == null) return;
        try { mapping.setDown(down); } catch (Throwable ignored) {}
    }

    /** Convenience wrapper: resolve by name then set pressed state. */
    public static void setMappingPressed(String nameOrKey, boolean down) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) return;
            Resolution res = resolveMappingByName(mc.options, nameOrKey);
            setKeyPressed(res.mapping, down);
        } catch (Throwable ignored) {}
    }

    /* -------------------- Modifiers: detection + synthesis -------------------- */

    /** Left-side keys are sufficient for most bindings; side-specific bindings are rare. */
    private static final int MODKEY_CTRL  = GLFW.GLFW_KEY_LEFT_CONTROL;
    private static final int MODKEY_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    private static final int MODKEY_ALT   = GLFW.GLFW_KEY_LEFT_ALT;

    /** Compact record for required modifiers. */
    private record ModReq(boolean ctrl, boolean shift, boolean alt) {
        boolean any() { return ctrl || shift || alt; }
        int toBitmask() {
            int m = 0;
            if (ctrl)  m |= GLFW.GLFW_MOD_CONTROL;
            if (shift) m |= GLFW.GLFW_MOD_SHIFT;
            if (alt)   m |= GLFW.GLFW_MOD_ALT;
            return m;
        }
        String brief() { return (ctrl?"C":"-")+(shift?"S":"-")+(alt?"A":"-"); }
    }

    /** Detect required modifier via reflection.
     * Tries NeoForge then Forge KeyModifier enums. Falls back to NONE if not available. */
    private static ModReq detectRequiredModifiers(KeyMapping mapping) {
        try {
            // Try NeoForge first
            Enum<?> km = keyModGet(mapping, true);
            if (km == null) {
                // Try Forge as fallback (just in case)
                km = keyModGet(mapping, false);
            }
            if (km == null) return new ModReq(false, false, false);
            String name = km.name();
            boolean ctrl  = "CONTROL".equals(name) || "CTRL".equals(name);
            boolean shift = "SHIFT".equals(name);
            boolean alt   = "ALT".equals(name);
            return new ModReq(ctrl, shift, alt);
        } catch (Throwable ignored) {
            return new ModReq(false, false, false);
        }
    }

    private static boolean deliverInputWithModifiers(Minecraft mc, int glfwKey, int glfwScanCode, ModReq req) {
        try {
            final long window = (mc.getWindow() != null) ? mc.getWindow().getWindow() : 0L;
            if (window == 0L) {
                Constants.LOG.warn("[{}] INPUT+MODS: window handle missing", Constants.MOD_NAME);
                return false;
            }
            if (glfwKey < 0) {
                Constants.LOG.warn("[{}] INPUT+MODS: invalid main key (<0).", Constants.MOD_NAME);
                return false;
            }
            if (glfwScanCode <= 0) {
                Constants.LOG.warn("[{}] INPUT+MODS: scancode {} (<=0). Some keys may be ignored.", Constants.MOD_NAME, glfwScanCode);
            }

            // Determine which modifiers are already physically held
            boolean ctrlDown  = isPhysicallyDown(window, MODKEY_CTRL);
            boolean shiftDown = isPhysicallyDown(window, MODKEY_SHIFT);
            boolean altDown   = isPhysicallyDown(window, MODKEY_ALT);

            // We only press modifiers we require AND that are not already down
            List<Integer> pressedByUs = new ArrayList<>(3);
            if (req.ctrl && !ctrlDown)   pressModifier(window, MODKEY_CTRL, pressedByUs);
            if (req.shift && !shiftDown) pressModifier(window, MODKEY_SHIFT, pressedByUs);
            if (req.alt && !altDown)     pressModifier(window, MODKEY_ALT, pressedByUs);

            // Build mods bitmask to match the effective physical state for this press
            int mods = 0;
            if (req.ctrl || ctrlDown)   mods |= GLFW.GLFW_MOD_CONTROL;
            if (req.shift || shiftDown) mods |= GLFW.GLFW_MOD_SHIFT;
            if (req.alt || altDown)     mods |= GLFW.GLFW_MOD_ALT;

            // Make it effectively final for lambda usage
            final int modsMask = mods;

            Constants.LOG.debug("[{}] INPUT+MODS: mainKey={} scan={} mods={} pressedByUs={}",
                    Constants.MOD_NAME, glfwKey, glfwScanCode, modsMask, pressedByUs);

            // Press main key now
            KeyboardHandlerAccessor acc = (KeyboardHandlerAccessor)(Object) mc.keyboardHandler;
            acc.ezactions$keyPress(window, glfwKey, glfwScanCode, GLFW.GLFW_PRESS, modsMask);

            // Release main key & any modifiers we pressed, next tick
            ClientTaskQueue.post(() -> {
                try {
                    acc.ezactions$keyPress(window, glfwKey, glfwScanCode, GLFW.GLFW_RELEASE, modsMask);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] INPUT+MODS main release failed: {}", Constants.MOD_NAME, t.toString());
                }
                // Release only the modifiers we pressed (do not touch user-held ones)
                for (int modKey : pressedByUs) {
                    try {
                        int modScan = safeScan(modKey);
                        acc.ezactions$keyPress(window, modKey, modScan, GLFW.GLFW_RELEASE, 0);
                    } catch (Throwable t) {
                        Constants.LOG.warn("[{}] INPUT+MODS mod release failed (modKey={}): {}", Constants.MOD_NAME, modKey, t.toString());
                    }
                }
            });

            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] INPUT+MODS delivery exception: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }

    private static void pressModifier(long window, int modKey, List<Integer> pressedByUs) {
        try {
            int modScan = safeScan(modKey);
            KeyboardHandlerAccessor acc = (KeyboardHandlerAccessor)(Object) Minecraft.getInstance().keyboardHandler;
            acc.ezactions$keyPress(window, modKey, modScan, GLFW.GLFW_PRESS, 0);
            pressedByUs.add(modKey);
            Constants.LOG.debug("[{}] INPUT+MODS: pressed modifier {}", Constants.MOD_NAME, modKey);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] INPUT+MODS mod press failed (modKey={}): {}", Constants.MOD_NAME, modKey, t.toString());
        }
    }

    private static boolean isPhysicallyDown(long window, int key) {
        try {
            int state = GLFW.glfwGetKey(window, key);
            return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
        } catch (Throwable t) {
            return false;
        }
    }

    private static int safeScan(int glfwKey) {
        try {
            int sc = GLFW.glfwGetKeyScancode(glfwKey);
            return sc > 0 ? sc : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /* -------------------- UNBOUND/NO-SCAN -> temporary-binding INPUT shim (with modifiers) -------------------- */

    private record TempKey(int glfwKey, int scancode) {}

    /**
     * Temporarily bind to a spare key with a real scancode, inject INPUT press/release WITH MODIFIERS, restore next tick.
     */
    private static boolean deliverInputViaTemporaryBinding(Minecraft mc, KeyMapping mapping, ModReq req) {
        try {
            final Options opts = mc.options;
            if (opts == null) return false;

            TempKey temp = findTemporaryKey(opts);
            if (temp == null) {
                Constants.LOG.warn("[{}] Shim: no suitable temporary key found (in-use or scancode<=0).", Constants.MOD_NAME);
                return false;
            }

            final InputConstants.Key oldKey = mapping.getKey();
            final InputConstants.Key newKey = InputConstants.Type.KEYSYM.getOrCreate(temp.glfwKey);

            try {
                mapping.setKey(newKey);
                KeyMapping.resetMapping();
                Constants.LOG.debug("[{}] Shim: temporarily bound '{}' to tempKey={} (scan={})",
                        Constants.MOD_NAME, safeName(mapping), temp.glfwKey, temp.scancode);
            } catch (Throwable t) {
                Constants.LOG.warn("[{}] Shim: failed to set temporary key: {}", Constants.MOD_NAME, t.toString());
                return false;
            }

            boolean pressed = deliverInputWithModifiers(mc, temp.glfwKey, temp.scancode, req);

            // Restore on next tick
            ClientTaskQueue.post(() -> {
                try {
                    mapping.setKey(oldKey == null ? InputConstants.UNKNOWN : oldKey);
                    KeyMapping.resetMapping();
                    Constants.LOG.debug("[{}] Shim: restored '{}' to {}.",
                            Constants.MOD_NAME, safeName(mapping),
                            (oldKey == null || oldKey.getValue() < 0) ? "UNBOUND" : ("key=" + oldKey.getValue()));
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] Shim: failed to restore original key for '{}': {}",
                            Constants.MOD_NAME, safeName(mapping), t.toString());
                }
            });

            return pressed;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] Shim exception for '{}': {}", Constants.MOD_NAME, safeName(mapping), t.toString());
            return false;
        }
    }

    /** Pick a temp key that (a) isnâ€™t bound and (b) has a valid scancode. */
    @Nullable
    private static TempKey findTemporaryKey(Options opts) {
        List<Integer> candidates = new ArrayList<>(32);
        // Uncommon-but-present keys first
        candidates.add(GLFW.GLFW_KEY_MENU);          // 348
        candidates.add(GLFW.GLFW_KEY_KP_DIVIDE);     // 331
        candidates.add(GLFW.GLFW_KEY_KP_MULTIPLY);   // 332
        candidates.add(GLFW.GLFW_KEY_KP_SUBTRACT);   // 333
        candidates.add(GLFW.GLFW_KEY_KP_ADD);        // 334
        candidates.add(GLFW.GLFW_KEY_KP_DECIMAL);    // 330
        // Mid/high F-keys (avoid F1-F6/F12 hotspots)
        candidates.add(GLFW.GLFW_KEY_F11);
        candidates.add(GLFW.GLFW_KEY_F10);
        candidates.add(GLFW.GLFW_KEY_F9);
        candidates.add(GLFW.GLFW_KEY_F8);
        candidates.add(GLFW.GLFW_KEY_F7);
        // Utility keys
        candidates.add(GLFW.GLFW_KEY_PRINT_SCREEN);  // 283
        candidates.add(GLFW.GLFW_KEY_SCROLL_LOCK);   // 280
        candidates.add(GLFW.GLFW_KEY_PAUSE);         // 284
        // Last-resort OEM punctuation
        candidates.add(GLFW.GLFW_KEY_SEMICOLON);     // 59
        candidates.add(GLFW.GLFW_KEY_APOSTROPHE);    // 39
        candidates.add(GLFW.GLFW_KEY_WORLD_1);       // 161 (likely no scan -> skip)
        candidates.add(GLFW.GLFW_KEY_WORLD_2);       // 162 (likely no scan -> skip)

        StringBuilder tried = new StringBuilder(128);
        for (int cand : candidates) {
            if (isKeyInUse(opts, cand)) { tried.append(cand).append("(in-use), "); continue; }
            int sc = GLFW.glfwGetKeyScancode(cand);
            if (sc <= 0) { tried.append(cand).append("(no-scan), "); continue; }
            Constants.LOG.debug("[{}] Shim: selected tempKey {} with scancode {} (tried: {}).",
                    Constants.MOD_NAME, cand, sc, tried.toString());
            return new TempKey(cand, sc);
        }
        Constants.LOG.debug("[{}] Shim: exhausted candidates (tried: {}).", Constants.MOD_NAME, tried.toString());
        return null;
    }

    /** Is any KeyMapping currently bound to this GLFW key? */
    private static boolean isKeyInUse(Options opts, int glfwKey) {
        try {
            if (opts == null || opts.keyMappings == null) return false;
            for (KeyMapping km : opts.keyMappings) {
                if (km == null) continue;
                InputConstants.Key k = km.getKey();
                if (k != null && k.getType() == InputConstants.Type.KEYSYM && k.getValue() == glfwKey) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /* -------------------- NeoForge/Forge KeyModifier reflection helpers + modifier shim -------------------- */

    @SuppressWarnings("unchecked")
    private static @Nullable Class<? extends Enum<?>> keyModEnum(boolean neoFirst) {
        // Try NeoForge first (1.21.x): net.neoforged.neoforge.client.settings.KeyModifier
        // Fallback to Forge (compat):   net.minecraftforge.client.settings.KeyModifier
        String[] candidates = neoFirst
                ? new String[] {
                "net.neoforged.neoforge.client.settings.KeyModifier",
                "net.minecraftforge.client.settings.KeyModifier"
        }
                : new String[] {
                "net.minecraftforge.client.settings.KeyModifier",
                "net.neoforged.neoforge.client.settings.KeyModifier"
        };
        for (String fqn : candidates) {
            try {
                return (Class<? extends Enum<?>>) Class.forName(fqn);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static @Nullable Enum<?> keyModGet(KeyMapping mapping, boolean neoFirst) {
        try {
            var m = mapping.getClass().getMethod("getKeyModifier");
            Object km = m.invoke(mapping);
            return (km instanceof Enum<?>) ? (Enum<?>) km : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Enum<?> keyModConstant(String name, boolean neoFirst) {
        try {
            Class<? extends Enum<?>> cls = keyModEnum(neoFirst);
            if (cls == null) return null;
            @SuppressWarnings({"rawtypes","unchecked"})
            Enum<?> v = Enum.valueOf((Class) cls, name);
            return v;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** setKeyModifierAndCode(KeyModifier, InputConstants.Key) via reflection (NeoForge/Forge). */
    private static boolean setKeyModifierAndCode(KeyMapping mapping, @Nullable Enum<?> keyMod, InputConstants.Key key) {
        // Try NeoForge signature first, then Forge â€” both typically share the same method name/signature.
        for (boolean neoFirst : new boolean[]{true, false}) {
            try {
                Class<? extends Enum<?>> kmEnum = keyModEnum(neoFirst);
                if (kmEnum == null) continue;
                var m = mapping.getClass().getMethod("setKeyModifierAndCode", kmEnum, InputConstants.Key.class);
                m.invoke(mapping, keyMod, key);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /** Temporarily set KeyModifier=NONE (and optionally swap to a temp scancode key), inject, then restore. */
    private static boolean deliverViaModifierShim(Minecraft mc, KeyMapping mapping) {
        try {
            final Options opts = mc.options;
            if (opts == null) return false;

            // Require KeyModifier API (NeoForge or Forge)
            Enum<?> origMod = keyModGet(mapping, true);
            if (origMod == null) {
                // Maybe it's present under Forge location only
                origMod = keyModGet(mapping, false);
            }
            Enum<?> NONE = keyModConstant("NONE", true);
            if (NONE == null) {
                NONE = keyModConstant("NONE", false);
            }
            if (NONE == null) {
                // API not present -> cannot modifier-shim
                return false;
            }

            // Decide which key to use (keep current if it has a valid scancode; else use temp key)
            InputConstants.Key oldKey = mapping.getKey();
            int currentKeyCode = (oldKey != null) ? oldKey.getValue() : -1;
            int currentScan = (currentKeyCode >= 0) ? GLFW.glfwGetKeyScancode(currentKeyCode) : 0;

            int useKeyCode;
            int useScan;
            boolean usingTempKey = false;

            if (currentScan > 0 && currentKeyCode >= 0) {
                useKeyCode = currentKeyCode;
                useScan = currentScan;
            } else {
                TempKey temp = findTemporaryKey(opts);
                if (temp == null) {
                    Constants.LOG.warn("[{}] ModShim: no suitable temp key for '{}'.", Constants.MOD_NAME, safeName(mapping));
                    return false;
                }
                useKeyCode = temp.glfwKey;
                useScan = temp.scancode;
                usingTempKey = true;
            }

            // Apply modifier NONE + desired key
            InputConstants.Key newKey = InputConstants.Type.KEYSYM.getOrCreate(useKeyCode);
            boolean setOk = setKeyModifierAndCode(mapping, NONE, newKey);
            if (!setOk) return false;
            KeyMapping.resetMapping();
            Constants.LOG.debug("[{}] ModShim: '{}' -> modifier=NONE key={} (scan={}){}",
                    Constants.MOD_NAME, safeName(mapping), useKeyCode, useScan, usingTempKey ? " [temp]" : "");

            // Inject plain input (no synthesized modifier needed)
            boolean pressed = deliverInput(mc, useKeyCode, useScan, 0 /* mods */);

            // Restore on next tick
            final Enum<?> restoreMod = (origMod == null) ? NONE : origMod;
            final String restoreModLabel = (origMod == null) ? "NONE?" : origMod.name();
            final InputConstants.Key restoreKeyFinal = (oldKey == null) ? InputConstants.UNKNOWN : oldKey;

            ClientTaskQueue.post(() -> {
                try {
                    if (setKeyModifierAndCode(mapping, restoreMod, restoreKeyFinal)) {
                        KeyMapping.resetMapping();
                        Constants.LOG.debug("[{}] ModShim: restored '{}' to modifier={} key={}.",
                                Constants.MOD_NAME, safeName(mapping),
                                restoreModLabel,
                                (oldKey == null ? "UNBOUND" : oldKey.getValue()));
                    } else {
                        // Fallback: restore key only
                        mapping.setKey(restoreKeyFinal);
                        KeyMapping.resetMapping();
                        Constants.LOG.debug("[{}] ModShim: restored '{}' key only (modifier restore failed).",
                                Constants.MOD_NAME, safeName(mapping));
                    }
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] ModShim: restore failed for '{}': {}",
                            Constants.MOD_NAME, safeName(mapping), t.toString());
                }
            });

            return pressed;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] ModShim exception for '{}': {}", Constants.MOD_NAME, safeName(mapping), t.toString());
            return false;
        }
    }

    /* -------------------- Legacy INPUT mode (no modifiers) [kept for internal reuse] -------------------- */

    private static boolean deliverInput(Minecraft mc, int glfwKey, int glfwScanCode, int glfwMods) {
        try {
            long window = mc.getWindow() != null ? mc.getWindow().getWindow() : 0L;
            if (window == 0L) {
                Constants.LOG.warn("[{}] INPUT: window handle missing", Constants.MOD_NAME);
                return false;
            }
            if (glfwKey < 0) {
                Constants.LOG.warn("[{}] INPUT: invalid key (<0).", Constants.MOD_NAME);
                return false;
            }
            if (glfwScanCode <= 0) {
                Constants.LOG.warn("[{}] INPUT: scancode is {} (<=0). Some keys will be ignored by the input pipeline.",
                        Constants.MOD_NAME, glfwScanCode);
            }

            KeyboardHandlerAccessor acc = (KeyboardHandlerAccessor)(Object) mc.keyboardHandler;

            // press now
            acc.ezactions$keyPress(window, glfwKey, glfwScanCode, GLFW.GLFW_PRESS, glfwMods);

            // release next tick
            ClientTaskQueue.post(() -> {
                try {
                    acc.ezactions$keyPress(window, glfwKey, glfwScanCode, GLFW.GLFW_RELEASE, glfwMods);
                } catch (Throwable t) {
                    Constants.LOG.warn("[{}] INPUT release failed: {}", Constants.MOD_NAME, t.toString());
                }
            });

            return true;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] INPUT delivery exception: {}", Constants.MOD_NAME, t.toString());
            return false;
        }
    }

    /* -------------------- TICK mode (setDown true->false) -------------------- */

    private static boolean deliverTick(KeyMapping mapping, boolean toggle) {
        try {
            if (toggle) {
                boolean newState = !mapping.isDown();
                mapping.setDown(newState);
                Constants.LOG.debug("[{}] TICK toggle '{}' -> {}", Constants.MOD_NAME, safeName(mapping), newState);
                return true;
            } else {
                mapping.setDown(true);
                ClientTaskQueue.post(() -> {
                    try { mapping.setDown(false); }
                    catch (Throwable t) {
                        Constants.LOG.warn("[{}] TICK release failed: {}", Constants.MOD_NAME, t.toString());
                    }
                });
                Constants.LOG.debug("[{}] TICK tap '{}'", Constants.MOD_NAME, safeName(mapping));
                return true;
            }
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] TICK delivery exception '{}': {}", Constants.MOD_NAME, safeName(mapping), t.toString());
            return false;
        }
    }

    /* -------------------- misc helpers -------------------- */

    private static boolean isTextInputFocused(Minecraft mc) {
        try {
            Screen s = mc.screen;
            return s instanceof ChatScreen;
        } catch (Throwable t) {
            return false;
        }
    }

    private static int keyCodeFrom(KeyMapping mapping) {
        try {
            InputConstants.Key k = mapping.getKey();
            return k != null ? k.getValue() : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static boolean isUnbound(InputConstants.Key k) {
        // treat <0 as unbound on 1.21.x
        return (k == null) || (k.getValue() < 0);
    }

    private static String safeName(KeyMapping km) {
        try { return km.getName(); } catch (Throwable t) { return "<unknown-key>"; }
    }

    /* ---------- Name resolution with diagnostics ---------- */

    private record Resolution(@Nullable KeyMapping mapping, String matchKind, String needleShown) {}

    @Nullable
    private static Resolution resolveExact(Options opts, String needle) {
        KeyMapping[] all = opts.keyMappings;
        if (all == null || all.length == 0) return null;
        for (KeyMapping km : all) {
            if (km == null) continue;
            String disp = km.getName(); // translation key (e.g., key.inventory)
            if (needle.equalsIgnoreCase(disp)) {
                return new Resolution(km, "exact-translation-key", disp);
            }
        }
        return null;
    }

    @Nullable
    private static Resolution resolveLocalized(Options opts, String needle) {
        KeyMapping[] all = opts.keyMappings;
        if (all == null || all.length == 0) return null;
        for (KeyMapping km : all) {
            if (km == null) continue;
            String localized = Component.translatable(km.getName()).getString();
            if (needle.equalsIgnoreCase(localized)) {
                return new Resolution(km, "exact-localized", localized);
            }
        }
        return null;
    }

    @Nullable
    private static Resolution resolveContains(Options opts, String needle) {
        KeyMapping[] all = opts.keyMappings;
        if (all == null || all.length == 0) return null;
        String nlc = needle.toLowerCase(Locale.ROOT);
        for (KeyMapping km : all) {
            if (km == null) continue;
            String keyName = km.getName();
            String loc = Component.translatable(keyName).getString();
            if (keyName.equalsIgnoreCase(needle) ||
                    keyName.toLowerCase(Locale.ROOT).contains(nlc) ||
                    loc.equalsIgnoreCase(needle) ||
                    loc.toLowerCase(Locale.ROOT).contains(nlc)) {
                return new Resolution(km, "contains", loc);
            }
        }
        return null;
    }

    private static Resolution resolveMappingByName(Options opts, String nameOrKey) {
        try {
            String needle = Objects.requireNonNullElse(nameOrKey, "").trim();
            if (needle.isEmpty()) return new Resolution(null, "empty", "");

            // Prefer translation-key exact matches (robust across locales)
            Resolution r = resolveExact(opts, needle);
            if (r != null) return r;

            // Then exact match against localized label (for legacy/manual input)
            r = resolveLocalized(opts, needle);
            if (r != null) return r;

            // Finally, heuristic contains match across both key and localized forms
            r = resolveContains(opts, needle);
            if (r != null) return r;

            return new Resolution(null, "not-found", needle);
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] resolveMappingByName('{}') failed: {}", Constants.MOD_NAME, nameOrKey, t.toString());
            return new Resolution(null, "exception", nameOrKey);
        }
    }

    private static void logResolved(Resolution res) {
        if (res == null || res.mapping == null) return;
        try {
            InputConstants.Key k = res.mapping.getKey();
            String type = (k == null) ? "UNKNOWN" : k.getType().name();
            int val = (k == null) ? -1 : k.getValue();
            Constants.LOG.debug(
                    "[{}] Resolved mapping: name='{}' match={} localized='{}' keyType={} keyVal={}",
                    Constants.MOD_NAME, res.mapping.getName(), res.matchKind,
                    Component.translatable(res.mapping.getName()).getString(),
                    type, val
            );
        } catch (Throwable ignored) {}
    }
}
