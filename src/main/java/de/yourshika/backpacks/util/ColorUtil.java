package de.yourshika.backpacks.util;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

import java.util.Locale;

/**
 * Hilfsfunktionen rund um Farben. Backpacks speichern Haupt- und Akzentfarbe
 * als {@link DyeColor}. Für die spätere Resourcepack-/RGB-Darstellung lässt
 * sich daraus jederzeit ein {@link Color} oder {@link TextColor} ableiten.
 */
public final class ColorUtil {

    private ColorUtil() {}

    public static DyeColor parseDye(String name, DyeColor fallback) {
        if (name == null) return fallback;
        try {
            return DyeColor.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public static TextColor toTextColor(DyeColor dye) {
        if (dye == null) return TextColor.color(0xFFFFFF);
        Color c = dye.getColor();
        return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
    }

    public static String pretty(DyeColor dye) {
        if (dye == null) return "-";
        String n = dye.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
