package de.yourshika.backpacks.config;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

/**
 * Typisierter Zugriff auf die config.yml. Wird bei /backpack reload neu geladen.
 */
public final class PluginConfig {

    private final YourShikaBackpacks plugin;

    private String language;
    private boolean debug;
    private String storageType;
    private boolean autosaveEnabled;
    private int autosaveIntervalMinutes;
    private boolean recipesEnabled;
    private boolean placeableEnabled;
    private boolean allowNesting;
    private boolean rightClickOpen;
    private boolean offhandOpen;
    private List<String> worldWhitelist;
    private List<String> worldBlacklist;

    public PluginConfig(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        language = c.getString("language", "de");
        debug = c.getBoolean("debug", false);
        storageType = c.getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT);
        autosaveEnabled = c.getBoolean("storage.autosave.enabled", true);
        autosaveIntervalMinutes = Math.max(1, c.getInt("storage.autosave.interval-minutes", 5));
        recipesEnabled = c.getBoolean("crafting.enabled", true);
        placeableEnabled = c.getBoolean("placeable.enabled", false);
        allowNesting = c.getBoolean("security.allow-nesting", false);
        rightClickOpen = c.getBoolean("open.right-click", true);
        offhandOpen = c.getBoolean("open.offhand", true);
        worldWhitelist = c.getStringList("worlds.whitelist");
        worldBlacklist = c.getStringList("worlds.blacklist");
    }

    public boolean isWorldAllowed(String world) {
        if (worldWhitelist != null && !worldWhitelist.isEmpty()) {
            return worldWhitelist.contains(world);
        }
        if (worldBlacklist != null && worldBlacklist.contains(world)) {
            return false;
        }
        return true;
    }

    public String language() { return language; }
    public boolean debug() { return debug; }
    public String storageType() { return storageType; }
    public boolean autosaveEnabled() { return autosaveEnabled; }
    public int autosaveIntervalMinutes() { return autosaveIntervalMinutes; }
    public boolean recipesEnabled() { return recipesEnabled; }
    public boolean placeableEnabled() { return placeableEnabled; }
    public boolean allowNesting() { return allowNesting; }
    public boolean rightClickOpen() { return rightClickOpen; }
    public boolean offhandOpen() { return offhandOpen; }
}
