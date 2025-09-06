package org.z2six.ezactions.helper;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.z2six.ezactions.Constants;
import org.z2six.ezactions.mixin.KeyboardHandlerAccessor;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Centralized input delivery + utility setters used by the radial and editor.
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

            KeyMapping km = resolveMappingByName(mc.options, mappingName);
            if (km == null) {
                // also try the localized translation of the needle
                String localized = Component.translatable(mappingName).getString();
                km = resolveMappingByName(mc.options, localized);
            }

            if (km == null) {
                Constants.LOG.warn("[{}] deliver('{}'): mapping not found", Constants.MOD_NAME, mappingName);
                return false;
            }

            return deliverKey(km, null, null, 0, toggle, mode);
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
                Constants.LOG.info("[{}] Input injection blocked: text field focused.", Constants.MOD_NAME);
                return false;
            }

            int key  = explicitGlfwKey  != null ? explicitGlfwKey  : keyCodeFrom(mapping);
            int scan = explicitScanCode != null ? explicitScanCode : -1;

            boolean unbound  = isUnbound(mapping.getKey());
            DeliveryMode eff = (mode == DeliveryMode.AUTO)
                    ? (unbound ? DeliveryMode.TICK : DeliveryMode.INPUT)
                    : mode;

            Constants.LOG.info("[{}] Key action fired: mapping='{}' mode={} toggle={} [key={}, scan={}, mods={}]",
                    Constants.MOD_NAME, safeName(mapping), eff, toggle, key, scan, glfwMods);

            return switch (eff) {
                case INPUT -> deliverInput(mc, key, scan, glfwMods);
                case TICK  -> deliverTick(mapping, toggle);
                case AUTO  -> unbound ? deliverTick(mapping, toggle) : deliverInput(mc, key, scan, glfwMods);
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
        try {
            mapping.setDown(down);
        } catch (Throwable ignored) {}
    }

    /** Convenience wrapper: resolve by name then set pressed state. */
    public static void setMappingPressed(String nameOrKey, boolean down) {
        try {
            final Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) return;
            KeyMapping km = resolveMappingByName(mc.options, nameOrKey);
            setKeyPressed(km, down);
        } catch (Throwable ignored) {}
    }

    /* -------------------- INPUT mode (GLFW press/release) -------------------- */

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
                Constants.LOG.info("[{}] TICK toggle '{}' -> {}", Constants.MOD_NAME, safeName(mapping), newState);
                return true;
            } else {
                mapping.setDown(true);
                ClientTaskQueue.post(() -> {
                    try { mapping.setDown(false); }
                    catch (Throwable t) {
                        Constants.LOG.warn("[{}] TICK release failed: {}", Constants.MOD_NAME, t.toString());
                    }
                });
                Constants.LOG.info("[{}] TICK tap '{}'", Constants.MOD_NAME, safeName(mapping));
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
            if (s == null) return false;
            return s.getTitle() != null && s.getTitle().getString().toLowerCase().contains("chat");
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
        try {
            // In your mappings, getName() already returns a String
            return km.getName();
        } catch (Throwable t) {
            return "<unknown-key>";
        }
    }

    @Nullable
    private static KeyMapping resolveMappingByName(Options opts, String nameOrKey) {
        try {
            String needle = Objects.requireNonNullElse(nameOrKey, "").trim();
            if (needle.isEmpty()) return null;

            KeyMapping[] all = opts.keyMappings;
            if (all == null || all.length == 0) return null;

            // 1) exact match against displayed name (already a String here)
            for (KeyMapping km : all) {
                if (km == null) continue;
                String disp = km.getName();
                if (needle.equalsIgnoreCase(disp)) return km;
            }

            // 2) try translatable(needle)
            String localized = Component.translatable(needle).getString();
            if (!localized.equals(needle)) {
                for (KeyMapping km : all) {
                    if (km == null) continue;
                    String disp = km.getName();
                    if (localized.equalsIgnoreCase(disp)) return km;
                }
            }

            // 3) contains match as a last resort
            for (KeyMapping km : all) {
                if (km == null) continue;
                String disp = km.getName();
                if (disp.equalsIgnoreCase(needle) || disp.toLowerCase().contains(needle.toLowerCase())) {
                    return km;
                }
            }

            return null;
        } catch (Throwable t) {
            Constants.LOG.warn("[{}] resolveMappingByName('{}') failed: {}", Constants.MOD_NAME, nameOrKey, t.toString());
            return null;
        }
    }
}
