// MainFile: src/main/java/org/z2six/ezactions/gui/editor/config/ColorUtil.java
package ezactions.gui.editor.config;

import java.util.Locale;
import java.util.Objects;

public final class ColorUtil {

    private ColorUtil() {}

    /** Cheap, allocation-light ARGB -> "#AARRGGBB" (always 8 hex digits). */
    public static String toHexARGB(int a, int r, int g, int b) {
        int argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        String s = Integer.toHexString(argb).toUpperCase(Locale.ROOT);
        if (s.length() < 8) s = "00000000".substring(s.length()) + s;
        return "#" + s;
    }

    /** Overload: packed ARGB int -> "#AARRGGBB". */
    public static String toHexARGB(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b = (argb)        & 0xFF;
        return toHexARGB(a, r, g, b);
    }

    /** Parse "#RRGGBB" or "#AARRGGBB" (leading '#' optional). Returns {a,r,g,b}. */
    public static int[] parseHexARGB(String s) throws IllegalArgumentException {
        String t = Objects.requireNonNull(s, "hex").trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() == 6) t = "FF" + t; // assume opaque if alpha missing
        if (t.length() != 8) throw new IllegalArgumentException("Hex must be 6 or 8 digits");
        int v = (int) Long.parseLong(t, 16);
        return new int[] { (v >>> 24) & 0xFF, (v >>> 16) & 0xFF, (v >>> 8) & 0xFF, v & 0xFF };
    }

    /** HSV in [0..1] -> RGB in [0..255]. */
    public static int[] hsvToRgb(float h, float s, float v) {
        h = wrap01(h);
        s = clamp01(s);
        v = clamp01(v);
        float r = v, g = v, b = v;
        if (s > 0f) {
            float i = (float) Math.floor(h * 6f);
            float f = h * 6f - i;
            float p = v * (1f - s);
            float q = v * (1f - s * f);
            float t = v * (1f - s * (1f - f));
            switch (((int) i) % 6) {
                case 0 -> { r = v; g = t; b = p; }
                case 1 -> { r = q; g = v; b = p; }
                case 2 -> { r = p; g = v; b = t; }
                case 3 -> { r = p; g = q; b = v; }
                case 4 -> { r = t; g = p; b = v; }
                case 5 -> { r = v; g = p; b = q; }
            }
        }
        return new int[] {
                Math.round(r * 255f),
                Math.round(g * 255f),
                Math.round(b * 255f)
        };
    }

    /** RGB in [0..255] -> HSV in [0..1]. Returns {h,s,v}. */
    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = (r & 0xFF) / 255f;
        float gf = (g & 0xFF) / 255f;
        float bf = (b & 0xFF) / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d == 0f) {
            h = 0f;
        } else if (max == rf) {
            h = ( (gf - bf) / d + (gf < bf ? 6f : 0f) ) / 6f;
        } else if (max == gf) {
            h = ( (bf - rf) / d + 2f ) / 6f;
        } else {
            h = ( (rf - gf) / d + 4f ) / 6f;
        }
        float s = max == 0f ? 0f : (d / max);
        float v = max;
        return new float[] { wrap01(h), clamp01(s), clamp01(v) };
    }

    public static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    public static float wrap01(float v) {
        v = v % 1f;
        if (v < 0f) v += 1f;
        return v;
    }
    public static int clamp255(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
