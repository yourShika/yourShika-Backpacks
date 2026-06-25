package de.yourshika.backpacks.util;

import de.yourshika.backpacks.YourShikaBackpacks;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

/**
 * Spielt konfigurierbare UI-Sounds (Adventure). Alle Sounds sind optional und
 * über {@code sounds.*} in der config.yml ein-/ausschaltbar und austauschbar.
 *
 * <p>Events: {@code open}, {@code upgrade}, {@code sort}, {@code error}.
 * Ein Sound wird übersprungen, wenn {@code sounds.enabled} aus ist oder der
 * Key leer bzw. {@code "none"} ist.</p>
 */
public final class Sounds {

    private Sounds() {}

    public static void play(YourShikaBackpacks plugin, Player player, String event) {
        if (player == null || plugin == null) return;
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("sounds.enabled", true)) return;
        String key = cfg.getString("sounds." + event + ".key", defaultKey(event));
        if (key == null || key.isBlank() || key.equalsIgnoreCase("none")) return;
        Key soundKey;
        try {
            soundKey = Key.key(key);
        } catch (Exception ex) {
            return;
        }
        float volume = (float) cfg.getDouble("sounds." + event + ".volume", 0.6D);
        float pitch = (float) cfg.getDouble("sounds." + event + ".pitch", defaultPitch(event));
        player.playSound(Sound.sound(soundKey, Sound.Source.MASTER, volume, pitch), Sound.Emitter.self());
    }

    private static String defaultKey(String event) {
        return switch (event) {
            case "open" -> "minecraft:block.ender_chest.open";
            case "upgrade" -> "minecraft:block.anvil.use";
            case "sort" -> "minecraft:block.barrel.close";
            case "error" -> "minecraft:block.note_block.bass";
            default -> "none";
        };
    }

    private static float defaultPitch(String event) {
        return switch (event) {
            case "open" -> 1.1F;
            case "upgrade" -> 1.0F;
            case "sort" -> 1.3F;
            case "error" -> 0.7F;
            default -> 1.0F;
        };
    }
}
