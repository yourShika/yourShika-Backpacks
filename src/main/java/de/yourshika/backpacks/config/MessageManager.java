package de.yourshika.backpacks.config;

import de.yourshika.backpacks.YourShikaBackpacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Lädt und verwaltet alle Spieler-Nachrichten (Standard: Deutsch). Alle Texte
 * sind über die messages_de.yml konfigurierbar und unterstützen MiniMessage
 * sowie &-Farbcodes (Legacy). Platzhalter werden als &lt;name&gt; eingesetzt.
 */
public final class MessageManager {

    private final YourShikaBackpacks plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private YamlConfiguration messages;
    private Component prefix = Component.empty();

    public MessageManager(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    /** Alle mitgelieferten Sprachen – werden beim Start auf die Platte extrahiert. */
    public static final String[] BUNDLED_LANGUAGES = {"en", "de", "pl"};

    /**
     * Extrahiert (falls fehlend) und aktualisiert ALLE mitgelieferten Sprachdateien
     * auf der Platte, damit der Admin jede Sprache frei per {@code language:} wählen
     * und editieren kann – und beim Versionswechsel nichts verloren geht: neue
     * Schlüssel werden ergänzt, bestehende (eigene) Werte bleiben erhalten.
     */
    public void syncBundledLanguages() {
        for (String lang : BUNDLED_LANGUAGES) {
            syncLanguageFile(lang);
        }
    }

    /**
     * Stellt sicher, dass {@code messages_<lang>.yml} existiert und alle neuen
     * Schlüssel der mitgelieferten Datei enthält (bestehende Werte bleiben
     * unangetastet). Gibt die Datei zurück (oder null, wenn die Sprache nicht
     * mitgeliefert wird und keine Datei existiert).
     */
    private File syncLanguageFile(String language) {
        String fileName = "messages_" + language.toLowerCase() + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);
        InputStream bundledStream = plugin.getResource(fileName);
        if (bundledStream == null) {
            // Nicht mitgelieferte Sprache: nur nutzbar, wenn der Admin die Datei selbst anlegt.
            return file.exists() ? file : null;
        }
        if (!file.exists()) {
            plugin.saveResource(fileName, false); // frische Standarddatei (inkl. Kommentare)
        }
        // Merge: neue Schlüssel der mitgelieferten Datei ergänzen, eigene Werte behalten.
        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new InputStreamReader(bundledStream, StandardCharsets.UTF_8));
        boolean changed = false;
        for (String key : bundled.getKeys(true)) {
            if (bundled.isConfigurationSection(key)) continue; // nur Blattwerte setzen
            if (!current.contains(key)) {
                current.set(key, bundled.get(key));
                changed = true;
            }
        }
        if (changed) {
            try {
                current.save(file);
            } catch (Exception ex) {
                plugin.getLogger().warning("Sprachdatei " + fileName + " konnte nicht aktualisiert werden: " + ex.getMessage());
            }
        }
        return file;
    }

    public void load(String language) {
        // Aktive Sprachdatei sicherstellen + auf den neuesten Stand bringen.
        File file = syncLanguageFile(language);
        if (file == null || !file.exists()) {
            // Unbekannte/fehlende Sprache -> auf Englisch zurückfallen.
            file = syncLanguageFile("en");
            if (file == null) file = new File(plugin.getDataFolder(), "messages_en.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);

        // Runtime-Fallback auf gebündeltes Englisch für alles noch Fehlende.
        InputStream def = plugin.getResource("messages_en.yml");
        if (def != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(def, StandardCharsets.UTF_8)));
        }

        String prefixRaw = messages.getString("prefix", "<gold>[Backpacks]</gold> ");
        this.prefix = deserialize(prefixRaw);
    }

    private Component deserialize(String raw) {
        if (raw == null) raw = "";
        // &-Codes in MiniMessage-kompatible Form bringen (einfache Unterstützung).
        return mini.deserialize(legacyToMini(raw));
    }

    private String legacyToMini(String s) {
        // Wandelt gängige &-Codes nur dann um, wenn sie vorkommen – MiniMessage bleibt voll nutzbar.
        return s
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }

    /** Liefert eine Nachricht als Component (ohne Prefix). */
    public Component component(String key, TagResolver... resolvers) {
        String raw = messages.getString(key, "<red>Fehlende Nachricht: " + key + "</red>");
        return mini.deserialize(legacyToMini(raw), resolvers);
    }

    /** Sendet eine Nachricht mit Prefix an einen Empfänger. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(prefix.append(component(key, resolvers)));
    }

    /** Sendet eine Nachricht ohne Prefix. */
    public void sendRaw(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(component(key, resolvers));
    }

    /** Sendet eine Nachricht in die Action Bar eines Spielers (ohne Prefix). */
    public void sendActionBar(org.bukkit.entity.Player to, String key, TagResolver... resolvers) {
        to.sendActionBar(component(key, resolvers));
    }

    public Component prefix() {
        return prefix;
    }

    public MiniMessage mini() {
        return mini;
    }

    /** Bequemer Platzhalter-Builder: ph("name", "wert"). */
    public static TagResolver ph(String name, String value) {
        return Placeholder.unparsed(name, value == null ? "" : value);
    }

    public static TagResolver phComp(String name, Component value) {
        return Placeholder.component(name, value);
    }
}
