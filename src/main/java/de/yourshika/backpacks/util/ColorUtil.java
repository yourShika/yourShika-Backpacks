package de.yourshika.backpacks.util;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

import java.util.Locale;

/**
 * Hilfsfunktionen rund um Farben. Backpacks speichern Haupt- und Akzentfarbe als
 * freien <em>Farb-Token</em>: entweder ein {@link DyeColor}-Name (z.B. {@code BROWN})
 * oder ein Hex-Wert (z.B. {@code #A0703C}). Daraus lassen sich jederzeit eine
 * {@link Color} (für die echte Leder-Färbung) bzw. ein {@link TextColor}
 * (für Anzeigetexte) ableiten.
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** Prüft, ob ein Token als DyeColor-Name oder Hex interpretiert werden kann. */
    public static boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        return parseHex(token) != null || parseDyeOrNull(token) != null;
    }

    /** Wandelt einen Token in eine Bukkit-{@link Color} (für LeatherArmorMeta). */
    public static Color toBukkitColor(String token, Color fallback) {
        Color hex = parseHex(token);
        if (hex != null) return hex;
        DyeColor dye = parseDyeOrNull(token);
        if (dye != null) return dye.getColor();
        return fallback;
    }

    /** Wandelt einen Token in einen Adventure-{@link TextColor} (für Anzeigetexte). */
    public static TextColor toTextColor(String token, TextColor fallback) {
        Color c = toBukkitColor(token, null);
        if (c != null) return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
        return fallback;
    }

    /** Liefert einen 6-stelligen Hex-String ohne '#' (für MiniMessage-Tags). */
    public static String hex6(String token, String fallbackHex6) {
        Color c = toBukkitColor(token, null);
        if (c == null) return fallbackHex6;
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Mischt mehrere Farb-Tokens zu einem Hex-Wert ({@code #RRGGBB}) durch
     * Mittelung der RGB-Kanäle. Leere Liste ergibt {@code null}.
     */
    public static String blendToHex(java.util.List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return null;
        long r = 0, g = 0, b = 0;
        int n = 0;
        for (String token : tokens) {
            Color c = toBukkitColor(token, null);
            if (c == null) continue;
            r += c.getRed();
            g += c.getGreen();
            b += c.getBlue();
            n++;
        }
        if (n == 0) return null;
        return String.format("#%02X%02X%02X", (int) (r / n), (int) (g / n), (int) (b / n));
    }

    /** Lesbare Darstellung eines Tokens: Hex bleibt Hex, DyeColor wird hübsch formatiert. */
    public static String pretty(String token) {
        if (token == null || token.isBlank()) return "-";
        if (parseHex(token) != null) return token.startsWith("#") ? token.toUpperCase(Locale.ROOT)
                : "#" + token.toUpperCase(Locale.ROOT);
        DyeColor dye = parseDyeOrNull(token);
        if (dye != null) return prettyDye(dye);
        return token;
    }

    /** Normalisiert einen Token (Hex auf Großbuchstaben, DyeColor auf Großbuchstaben). */
    public static String normalize(String token, String fallback) {
        if (!isValid(token)) return fallback;
        Color hex = parseHex(token);
        if (hex != null) return (token.startsWith("#") ? token : "#" + token).toUpperCase(Locale.ROOT);
        return token.toUpperCase(Locale.ROOT);
    }

    /**
     * Liefert den naechstliegenden Vanilla-Dye-Key in lower_case. Das ist fuer
     * Resourcepack-Varianten gedacht, wenn Minecraft nur eine feste Textur pro
     * Akzentfarbe rendern kann.
     */
    public static String nearestDyeKey(String token, String fallback) {
        Color source = toBukkitColor(token, toBukkitColor(fallback, Color.WHITE));
        DyeColor best = DyeColor.WHITE;
        double bestScore = Double.MAX_VALUE;
        for (DyeColor dye : DyeColor.values()) {
            Color c = dye.getColor();
            double score = squared(source.getRed() - c.getRed())
                    + squared(source.getGreen() - c.getGreen())
                    + squared(source.getBlue() - c.getBlue());
            if (score < bestScore) {
                best = dye;
                bestScore = score;
            }
        }
        return best.name().toLowerCase(Locale.ROOT);
    }

    private static int squared(int value) {
        return value * value;
    }

    private static Color parseHex(String token) {
        if (token == null) return null;
        String s = token.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;
        try {
            int rgb = Integer.parseInt(s, 16);
            return Color.fromRGB(rgb);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static DyeColor parseDyeOrNull(String name) {
        if (name == null) return null;
        try {
            return DyeColor.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String prettyDye(DyeColor dye) {
        String n = dye.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
