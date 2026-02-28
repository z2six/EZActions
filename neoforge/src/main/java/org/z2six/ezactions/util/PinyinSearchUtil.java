package org.z2six.ezactions.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Lightweight pinyin token helper for search indexing.
 *
 * Uses ICU Transliterator via reflection when available:
 *   Han-Latin; Latin-ASCII; NFD; [:Nonspacing Mark:] Remove; NFC; Any-Lower
 *
 * No hard dependency is introduced; if ICU is unavailable, returns empty tokens.
 */
public final class PinyinSearchUtil {

    public record Tokens(String spaced, String compact, String initials) {
        public static final Tokens EMPTY = new Tokens("", "", "");
    }

    private static volatile boolean initTried = false;
    private static @Nullable Method mGetInstance = null;
    private static @Nullable Method mTransform = null;
    private static @Nullable Object transliterator = null;

    private PinyinSearchUtil() {}

    public static Tokens tokens(@Nullable String input) {
        if (input == null || input.isBlank()) return Tokens.EMPTY;

        String py = transliterate(input);
        if (py.isBlank()) return Tokens.EMPTY;

        String spaced = normalize(py);
        if (spaced.isBlank()) return Tokens.EMPTY;

        String compact = spaced.replace(" ", "");
        String initials = initials(spaced);
        return new Tokens(spaced, compact, initials);
    }

    private static String initials(String spaced) {
        if (spaced == null || spaced.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        String[] parts = spaced.split(" ");
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            char c = part.charAt(0);
            if (c >= 'a' && c <= 'z') sb.append(c);
            else if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        boolean prevSpace = true;
        String lower = raw.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean alphaNum = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (alphaNum) {
                sb.append(c);
                prevSpace = false;
            } else {
                if (!prevSpace) {
                    sb.append(' ');
                    prevSpace = true;
                }
            }
        }
        int n = sb.length();
        while (n > 0 && sb.charAt(n - 1) == ' ') n--;
        return n == sb.length() ? sb.toString() : sb.substring(0, n);
    }

    private static String transliterate(String input) {
        ensureInit();
        if (transliterator == null || mTransform == null) return "";
        try {
            Object out = mTransform.invoke(transliterator, input);
            return out == null ? "" : String.valueOf(out);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static synchronized void ensureInit() {
        if (initTried) return;
        initTried = true;
        try {
            Class<?> c = Class.forName("com.ibm.icu.text.Transliterator");
            mGetInstance = c.getMethod("getInstance", String.class);
            mTransform = c.getMethod("transform", String.class);
            transliterator = mGetInstance.invoke(null,
                    "Han-Latin; Latin-ASCII; NFD; [:Nonspacing Mark:] Remove; NFC; Any-Lower");
        } catch (Throwable ignored) {
            mGetInstance = null;
            mTransform = null;
            transliterator = null;
        }
    }
}

