package de.yourshika.backpacks.config;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persistiert die pro-Spieler-Einstellungen, die sonst nur zur Laufzeit im
 * Speicher lägen (und beim Neustart auf Standard zurückfielen): aktuell die
 * <b>Magnet</b>- und <b>Pet-Booster</b>-An/Aus-Schalter.
 *
 * <p>Gespeichert wird bewusst nur, wer ein Feature <em>abgeschaltet</em> hat
 * (Standard = an), als UUID-Liste in {@code player-settings.yml}. Die Datei ist
 * klein und wird nur bei einer Änderung (Kommando) geschrieben.</p>
 */
public final class PlayerSettings {

    private static final String MAGNET_OFF = "magnet-off";
    private static final String BOOSTER_OFF = "booster-off";

    private final YourShikaBackpacks plugin;
    private final File file;

    public PlayerSettings(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-settings.yml");
    }

    /** Lädt die gespeicherten "off"-Spieler in die übergebenen Laufzeit-Sets. */
    public void load(Set<UUID> magnetOff, Set<UUID> boosterOff) {
        magnetOff.clear();
        boosterOff.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        readInto(yaml, MAGNET_OFF, magnetOff);
        readInto(yaml, BOOSTER_OFF, boosterOff);
    }

    /** Schreibt die aktuellen "off"-Sets in die Datei (bei jeder Änderung). */
    public void save(Set<UUID> magnetOff, Set<UUID> boosterOff) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(MAGNET_OFF, toStrings(magnetOff));
        yaml.set(BOOSTER_OFF, toStrings(boosterOff));
        try {
            yaml.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("player-settings.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private static void readInto(YamlConfiguration yaml, String path, Set<UUID> set) {
        for (String s : yaml.getStringList(path)) {
            try {
                set.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // ungültige UUID -> überspringen
            }
        }
    }

    private static List<String> toStrings(Set<UUID> set) {
        List<String> out = new ArrayList<>(set.size());
        for (UUID u : set) out.add(u.toString());
        return out;
    }
}
