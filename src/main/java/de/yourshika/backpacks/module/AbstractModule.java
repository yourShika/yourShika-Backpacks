package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;

/**
 * Gemeinsame Basis für {@link Module}-Implementierungen. Kümmert sich um
 * Plugin-Erkennung, Config-Flag und Aktiv-Status. Die konkrete Aktivierungs-
 * logik liefern Unterklassen über {@link #onEnable()} / {@link #onDisable()}.
 */
public abstract class AbstractModule implements Module {

    protected final YourShikaBackpacks plugin;
    private final String id;
    private final String displayName;
    private final String description;
    private final String requiredPlugin;

    private boolean active;

    protected AbstractModule(YourShikaBackpacks plugin, String id, String displayName,
                             String description, String requiredPlugin) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredPlugin = requiredPlugin;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String description() { return description; }
    @Override public String requiredPlugin() { return requiredPlugin; }

    @Override
    public boolean isPluginPresent() {
        return requiredPlugin != null && Bukkit.getPluginManager().getPlugin(requiredPlugin) != null;
    }

    @Override
    public boolean isEnabledInConfig() {
        return plugin.pluginConfig().isModuleEnabled(id);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public final void enable() throws Throwable {
        if (active) return;
        onEnable();
        active = true;
    }

    @Override
    public final void disable() {
        if (!active) return;
        try {
            onDisable();
        } catch (Throwable t) {
            plugin.getLogger().warning("Modul '" + id + "' konnte nicht sauber deaktiviert werden: " + t.getMessage());
        } finally {
            active = false;
        }
    }

    /** Tatsächliche Aktivierung – darf werfen. */
    protected abstract void onEnable() throws Throwable;

    /** Tatsächliche Deaktivierung. */
    protected void onDisable() {
    }
}
