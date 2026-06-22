package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verwaltet alle externen, optionalen Module (Hooks).
 *
 * <p>Module aktivieren sich <strong>automatisch</strong>, sobald das benötigte
 * Plugin installiert und das Modul in der Config aktiviert ist (Standard:
 * aktiviert). Fehlt das Plugin, bleibt das Modul still inaktiv – das Plugin läuft
 * weiterhin vollständig eigenständig. Einzelne Module lassen sich über
 * {@code /bp modules} live ab-/anschalten.</p>
 */
public final class ModuleManager {

    private final YourShikaBackpacks plugin;
    private final List<Module> modules = new ArrayList<>();
    private final List<ExternalItemModule> itemModules = new ArrayList<>();

    public ModuleManager(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        register(new PlaceholderModule(plugin));
        register(new PacketEventsModule(plugin));
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

    /** (De-)Aktiviert alle Module gemäß aktueller Konfiguration (automatisch). */
    public void reload() {
        for (Module module : modules) {
            module.disable();
        }
        int active = 0;
        for (Module module : modules) {
            if (!module.isEnabledInConfig()) continue;     // bewusst deaktiviert
            if (!module.isPluginPresent()) continue;        // Plugin fehlt -> still inaktiv
            try {
                module.enable();
                active++;
                plugin.getLogger().info("Hook aktiv: " + module.displayName());
            } catch (Throwable t) {
                plugin.getLogger().warning("Modul '" + module.displayName() + "' konnte nicht aktiviert werden: " + t.getMessage());
            }
        }
        plugin.getLogger().info("Aktive Hooks: " + active + "/" + modules.size());
    }

    public void shutdown() {
        for (Module module : modules) {
            module.disable();
        }
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
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

    /** Überlagert ein beliebiges Plugin-Item anhand einer externen Provider-ID. */
    public void applyExternalModel(ItemStack item, String providerId) {
        if (providerId == null || providerId.isBlank()) return;
        for (ExternalItemModule module : itemModules) {
            if (module.isActive()) {
                module.apply(item, providerId);
                return;
            }
        }
    }
}
