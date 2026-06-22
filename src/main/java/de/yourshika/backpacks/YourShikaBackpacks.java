package de.yourshika.backpacks;

import de.yourshika.backpacks.command.BackpackCommand;
import de.yourshika.backpacks.config.MessageManager;
import de.yourshika.backpacks.config.PluginConfig;
import de.yourshika.backpacks.craft.RecipeManager;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.item.BackpackKeys;
import de.yourshika.backpacks.listener.BackpackGuiListener;
import de.yourshika.backpacks.listener.BackpackInteractListener;
import de.yourshika.backpacks.module.ModuleManager;
import de.yourshika.backpacks.storage.BackpackStorage;
import de.yourshika.backpacks.storage.SqliteBackpackStorage;
import de.yourshika.backpacks.storage.YamlBackpackStorage;
import de.yourshika.backpacks.tier.TierRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * Hauptklasse von "yourShika Backpack's". Initialisiert Konfiguration,
 * Persistenz, Tiers, GUI-Listener, Befehle, Rezepte und das experimentelle
 * Modul-System (externe Hooks).
 */
public final class YourShikaBackpacks extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessageManager messages;
    private TierRegistry tiers;
    private BackpackKeys keys;
    private BackpackItemFactory itemFactory;
    private BackpackStorage storage;
    private BackpackManager manager;
    private RecipeManager recipeManager;
    private ModuleManager moduleManager;

    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages_de.yml");

        this.pluginConfig = new PluginConfig(this);
        this.pluginConfig.load();

        this.messages = new MessageManager(this);
        this.messages.load(pluginConfig.language());

        this.keys = new BackpackKeys(this);
        this.itemFactory = new BackpackItemFactory(this, keys);

        this.tiers = new TierRegistry(this);
        this.tiers.load(getConfig().getConfigurationSection("tiers"));

        // Persistenz initialisieren.
        try {
            this.storage = createStorage();
            this.storage.init();
        } catch (Exception ex) {
            getLogger().severe("Speicher konnte nicht initialisiert werden – Plugin wird deaktiviert: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.manager = new BackpackManager(this, storage, itemFactory, tiers);

        // Modul-System (externe, experimentelle Hooks) – vor der Rezept-/Item-Erstellung.
        this.moduleManager = new ModuleManager(this);

        // Listener registrieren.
        Bukkit.getPluginManager().registerEvents(new BackpackGuiListener(this, manager), this);
        Bukkit.getPluginManager().registerEvents(new BackpackInteractListener(this, manager), this);

        // Rezepte.
        this.recipeManager = new RecipeManager(this, tiers, itemFactory);
        Bukkit.getPluginManager().registerEvents(recipeManager, this);
        this.recipeManager.registerAll();

        // Befehle.
        BackpackCommand command = new BackpackCommand(this, manager, tiers);
        PluginCommand pc = getCommand("backpack");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        // Externe Module gemäß Config (Standard: alle gesperrt).
        moduleManager.reload();

        // Autosave.
        startAutosave();

        getLogger().info("yourShika Backpack's v" + getPluginMeta().getVersion() + " aktiviert (Paper 26.1.x / Java 25).");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        // Alle offenen Backpacks speichern und Betrachter schließen.
        if (manager != null) {
            manager.saveAllOpen();
            for (var player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackMenuHolder) {
                    player.closeInventory();
                }
            }
        }
        if (moduleManager != null) {
            moduleManager.shutdown();
        }
        if (recipeManager != null) {
            recipeManager.unregisterAll();
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("yourShika Backpack's deaktiviert.");
    }

    private BackpackStorage createStorage() {
        File dir = getDataFolder();
        if ("YAML".equals(pluginConfig.storageType())) {
            return new YamlBackpackStorage(this, new File(dir, "backpacks.yml"));
        }
        // Standard: SQLite.
        return new SqliteBackpackStorage(this, new File(dir, "backpacks.db"));
    }

    private void startAutosave() {
        if (!pluginConfig.autosaveEnabled()) return;
        long ticks = pluginConfig.autosaveIntervalMinutes() * 60L * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                manager.saveAllOpen();
                debug("Autosave abgeschlossen.");
            } catch (Exception ex) {
                getLogger().warning("Autosave fehlgeschlagen: " + ex.getMessage());
            }
        }, ticks, ticks);
    }

    /** Lädt Config, Nachrichten, Tiers, Rezepte und Module neu (für /backpack reload). */
    public void reloadAll() {
        reloadConfig();
        pluginConfig.load();
        messages.load(pluginConfig.language());
        tiers.load(getConfig().getConfigurationSection("tiers"));
        recipeManager.registerAll();
        moduleManager.reload();
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        startAutosave();
    }

    private void saveResourceIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists() && getResource(name) != null) {
            saveResource(name, false);
        }
    }

    public void debug(String message) {
        if (pluginConfig != null && pluginConfig.debug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public PluginConfig pluginConfig() { return pluginConfig; }
    public MessageManager messages() { return messages; }
    public TierRegistry tiers() { return tiers; }
    public BackpackManager manager() { return manager; }
    public BackpackItemFactory itemFactory() { return itemFactory; }
    public ModuleManager moduleManager() { return moduleManager; }
}
