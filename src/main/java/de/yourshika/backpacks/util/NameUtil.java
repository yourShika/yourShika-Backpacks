package de.yourshika.backpacks.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rendert vom Spieler vergebene Backpack-Namen mit Farb-Unterstützung:
 * <ul>
 *   <li><b>Hex</b>: {@code &#RRGGBB} oder MiniMessage {@code <#RRGGBB>}</li>
 *   <li><b>Gradient/Rainbow</b>: MiniMessage {@code <gradient:#a:#b>…</gradient>}, {@code <rainbow>…}</li>
 *   <li><b>Minecraft-Farbcodes</b>: {@code &a}, {@code &c}, {@code &l} … (Legacy)</li>
 * </ul>
 *
 * <p>Enthält der Name keine Formatierung, wird er schlicht in Gold dargestellt.
 * Interaktive MiniMessage-Tags ({@code click}/{@code hover}/{@code insertion}) werden
 * bewusst entfernt, damit ein Name keine Aktionen auslösen kann.</p>
 */
public final class NameUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern UNSAFE = Pattern.compile("(?i)</?(click|hover|insertion)[^>]*>");

    private NameUtil() {}

    /** Roh-Name -> Anzeige-Component (mit Farben/Formatierung), niemals kursiv. */
    public static Component render(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.text("").decoration(TextDecoration.ITALIC, false);
        }
        boolean hasFormatting = raw.indexOf('&') >= 0 || raw.indexOf('<') >= 0;
        String mm = legacyToMini(stripUnsafe(raw));
        if (!hasFormatting) {
            mm = "<gold>" + mm + "</gold>";
        }
        try {
            return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
        } catch (Exception ex) {
            // Bei ungültiger Formatierung den Rohtext schlicht anzeigen.
            return Component.text(raw).decoration(TextDecoration.ITALIC, false);
        }
    }

    private static String stripUnsafe(String s) {
        return UNSAFE.matcher(s).replaceAll("");
    }

    /** Wandelt &-Farbcodes und &#RRGGBB in MiniMessage-Tags um. */
    private static String legacyToMini(String s) {
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "<#" + m.group(1) + ">");
        }
        m.appendTail(sb);
        return sb.toString()
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }
}
