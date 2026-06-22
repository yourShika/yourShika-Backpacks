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
