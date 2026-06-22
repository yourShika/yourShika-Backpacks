package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verwaltet alle externen, experimentellen Module (Hooks).
 *
 * <p>Zentraler Master-Schalter ist {@code hooks.experimental}. Ist er
 * {@code false} (Standard), wird <strong>kein</strong> Modul aktiviert – das
 * Plugin läuft vollständig eigenständig. Ist er {@code true}, werden nur jene
 * Module aktiviert, die zusätzlich einzeln aktiviert und deren Plugins
 * installiert sind.</p>
 */
public final class ModuleManager {

    private final YourShikaBackpacks plugin;
    private final List<Module> modules = new ArrayList<>();
    private final List<ExternalItemModule> itemModules = new ArrayList<>();

    public ModuleManager(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        register(new PlaceholderModule(plugin));
        register(new ProtocolLibModule(plugin));
        // Custom-Item-Anbindung: bewusst nur Oraxen (frei nutzbar, reife API).
        // Nexo (kostenpflichtig) und ItemsAdder (Premium) wurden entfernt.
        register(new OraxenModule(plugin));
    }

    private void register(Module module) {
        modules.add(module);
        if (module instanceof ExternalItemModule item) {
            itemModules.add(item);
        }
    }

    /** (De-)Aktiviert alle Module gemäß aktueller Konfiguration. */
    public void reload() {
        for (Module module : modules) {
            module.disable();
        }
        if (!plugin.pluginConfig().hooksExperimental()) {
            plugin.getLogger().info("Externe Module gesperrt (hooks.experimental: false). Plugin läuft eigenständig.");
            return;
        }
        int active = 0;
        for (Module module : modules) {
            if (!module.isEnabledInConfig()) continue;
            if (!module.isPluginPresent()) {
                plugin.getLogger().info("Modul '" + module.displayName() + "' aktiviert, aber Plugin '"
                        + module.requiredPlugin() + "' fehlt – übersprungen.");
                continue;
            }
            try {
                module.enable();
                active++;
                plugin.getLogger().info("Modul aktiv: " + module.displayName());
            } catch (Throwable t) {
                plugin.getLogger().warning("Modul '" + module.displayName() + "' konnte nicht aktiviert werden: " + t.getMessage());
            }
        }
        plugin.getLogger().info("Experimentelle Module aktiv: " + active + "/" + modules.size());
    }

    public void shutdown() {
        for (Module module : modules) {
            module.disable();
        }
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
    }

    public boolean experimentalEnabled() {
        return plugin.pluginConfig().hooksExperimental();
    }

    /** Aktuell aktiver externer Item-Anbieter (oder null = Vanilla). */
    public CustomItemProvider activeItemProvider() {
        for (ExternalItemModule module : itemModules) {
            if (module.isActive()) return module;
        }
        return null;
    }

    /** Überlagert das Vanilla-Modell mit einem aktiven externen Anbieter (falls vorhanden). */
    public void applyExternalModel(ItemStack item, BackpackTier tier) {
        CustomItemProvider provider = activeItemProvider();
        if (provider != null) {
            provider.apply(item, tier);
        }
    }
}
