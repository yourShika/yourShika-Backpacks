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
    private de.yourshika.backpacks.upgrade.UpgradeItemFactory upgradeItems;
    private de.yourshika.backpacks.upgrade.UpgradeManager upgradeManager;
    private de.yourshika.backpacks.upgrade.FunctionUpgradeManager functionUpgrades;
    private de.yourshika.backpacks.place.PlaceableManager placeableManager;

    private BukkitTask autosaveTask;

    /** Aktuelle Struktur-Version der config.yml. */
    private static final int CONFIG_VERSION = 7;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        saveResourceIfMissing("messages_en.yml");

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
        moduleManager.reload();

        // Listener registrieren.
        Bukkit.getPluginManager().registerEvents(new BackpackGuiListener(this, manager), this);
        Bukkit.getPluginManager().registerEvents(new BackpackInteractListener(this, manager), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.BackpackSecurityListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.DyeCraftListener(this), this);

        // Platzierbare Backpacks.
        this.placeableManager = new de.yourshika.backpacks.place.PlaceableManager(this, manager);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.PlaceableListener(this, manager, placeableManager), this);

        // Rezepte für das Recipe Book (und JEI/REI/EMI) freischalten.
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.RecipeDiscoveryListener(this), this);

        // Rezepte.
        this.recipeManager = new RecipeManager(this, tiers, itemFactory);
        Bukkit.getPluginManager().registerEvents(recipeManager, this);
        this.recipeManager.registerAll();

        // Upgrade-System (Upgrade-Items, Tier-Kette, Smithing-Veredelung).
        this.upgradeItems = new de.yourshika.backpacks.upgrade.UpgradeItemFactory(this);
        this.upgradeManager = new de.yourshika.backpacks.upgrade.UpgradeManager(
                this, tiers, itemFactory, upgradeItems, manager);
        Bukkit.getPluginManager().registerEvents(upgradeManager, this);
        this.upgradeManager.registerAll();

        // Funktionale Upgrades (Pickup, Magnet, Crafting, ...).
        this.functionUpgrades = new de.yourshika.backpacks.upgrade.FunctionUpgradeManager(
                this, upgradeItems, upgradeManager);
        this.functionUpgrades.registerAll();
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.UpgradeEffectListener(this, manager), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.backpacks.listener.ItemUpdateListener(this), this);
        new de.yourshika.backpacks.listener.UpgradeMagnetTask(this, manager)
                .runTaskTimer(this, 20L, 8L);

        // Befehle.
        BackpackCommand command = new BackpackCommand(this, manager, tiers);
        PluginCommand pc = getCommand("backpack");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

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
                var holder = player.getOpenInventory().getTopInventory().getHolder();
                // Backpack- und Furnace-GUIs schließen (Furnace gibt Items zurück).
                if (holder instanceof BackpackMenuHolder
                        || holder instanceof de.yourshika.backpacks.gui.FurnaceMenuHolder) {
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
        if (upgradeManager != null) {
            upgradeManager.unregisterAll();
        }
        if (functionUpgrades != null) {
            functionUpgrades.unregisterAll();
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
        moduleManager.reload();
        recipeManager.registerAll();
        upgradeManager.registerAll();
        functionUpgrades.registerAll();
        refreshOnlineBackpacks();
        refreshOnlineUpgrades();
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        startAutosave();
    }

    /**
     * Aktualisiert eine veraltete config.yml automatisch. Bei einer
     * Struktur-Änderung (neue Tiers/Upgrades/Rezept-Regeln) wird die alte Datei
     * gesichert und die mitgelieferte neue Standardkonfiguration eingespielt.
     * So greifen Änderungen (z.B. die Upgrade-Kette) auch auf bestehenden Servern.
     */
    private void migrateConfig() {
        int version = getConfig().getInt("config-version", 1);
        if (version >= CONFIG_VERSION) return;

        File cfg = new File(getDataFolder(), "config.yml");
        String backupName = "config-backup-v" + version + "-" + System.currentTimeMillis() + ".yml";
        try {
            if (cfg.exists()) {
                java.nio.file.Files.copy(cfg.toPath(),
                        new File(getDataFolder(), backupName).toPath());
            }
            saveResource("config.yml", true); // mit aktuellen Defaults überschreiben
            reloadConfig();
            getLogger().warning("config.yml war veraltet (v" + version + ") und wurde auf v"
                    + CONFIG_VERSION + " aktualisiert. Die alte Datei wurde als '" + backupName
                    + "' gesichert – bitte eigene Anpassungen ggf. erneut übertragen.");
        } catch (Exception ex) {
            getLogger().severe("config.yml konnte nicht migriert werden: " + ex.getMessage());
        }
    }

    private void saveResourceIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists() && getResource(name) != null) {
            saveResource(name, false);
        }
    }

    /**
     * Schaltet ein einzelnes Modul um (persistiert + Live-Reload). Es werden
     * BEWUSST keine Rezepte neu registriert – {@code Bukkit.removeRecipe} ist
     * extrem teuer und würde den Server einfrieren. Stattdessen werden nur die
     * sichtbaren Backpack-Items online befindlicher Spieler aktualisiert
     * (Texturen an/aus).
     */
    public void setModuleEnabled(String id, boolean value) {
        getConfig().set("hooks.modules." + id, value);
        saveConfig();
        pluginConfig.load();
        moduleManager.reload();
        // Kanonische Upgrade-Items an den neuen Hook-Status anpassen (Texturen an/aus),
        // dann alle sichtbaren Items online befindlicher Spieler aktualisieren.
        if (upgradeManager != null) upgradeManager.rebuildItems();
        if (functionUpgrades != null) functionUpgrades.rebuildItems();
        refreshOnlineBackpacks();
        refreshOnlineUpgrades();
    }

    /** Aktualisiert Modell/Textur aller Backpack-Items online befindlicher Spieler. */
    public void refreshOnlineBackpacks() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayerItems(player);
        }
    }

    /** Alias – aktualisiert sowohl Backpacks als auch Upgrade-Items aller Spieler. */
    public void refreshOnlineUpgrades() {
        refreshOnlineBackpacks();
    }

    /**
     * Bringt ALLE Plugin-Items im Inventar eines Spielers auf den aktuellen Stand:
     * Backpacks (Name/Lore/Modell) und Upgrade-Items (auf die kanonische Version
     * normalisiert). So „funktionieren" alte Items nach einem Plugin-Update
     * automatisch wieder und tragen die neuen Texturen/Daten – nichts bleibt veraltet.
     */
    public void refreshPlayerItems(org.bukkit.entity.Player player) {
        if (itemFactory == null) return;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            if (itemFactory.isBackpack(item)) {
                var tier = tiers.get(itemFactory.getTierKey(item));
                if (tier != null) {
                    itemFactory.refresh(item, tier);
                    changed = true;
                }
                continue;
            }
            org.bukkit.inventory.ItemStack canonical = canonicalUpgrade(item);
            if (canonical != null) {
                canonical.setAmount(item.getAmount());
                contents[i] = canonical;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
            player.updateInventory();
        }
    }

    /** Liefert das aktuelle kanonische Upgrade-Item zu einem Upgrade-Item (oder null). */
    private org.bukkit.inventory.ItemStack canonicalUpgrade(org.bukkit.inventory.ItemStack item) {
        if (upgradeItems.isUpgradeBase(item)) {
            return upgradeManager.baseUpgradeItem();
        }
        String tier = upgradeItems.getUpgradeTarget(item);
        if (tier != null) {
            return upgradeManager.upgradeItem(tier);
        }
        String fn = upgradeItems.getFunctionType(item);
        if (fn != null) {
            return functionUpgrades.item(fn);
        }
        return null;
    }

    public void debug(String message) {
        if (pluginConfig != null && pluginConfig.debug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Admin-Audit-Log: schreibt eine Zeile nach {@code audit.log} im Datenordner
     * (z.B. für Give/OpenID/Delete/Transfer). Konfigurierbar über {@code audit.enabled}.
     */
    public synchronized void audit(String actor, String action, String detail) {
        if (!getConfig().getBoolean("audit.enabled", true)) return;
        String line = "[" + java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()) + "] "
                + (actor == null ? "?" : actor) + " " + action + " " + (detail == null ? "" : detail);
        try {
            java.io.File f = new java.io.File(getDataFolder(), "audit.log");
            try (java.io.FileWriter w = new java.io.FileWriter(f, true)) {
                w.write(line + System.lineSeparator());
            }
        } catch (Exception ex) {
            getLogger().warning("Audit-Log konnte nicht geschrieben werden: " + ex.getMessage());
        }
    }

    /** Pfad der eigenen Plugin-JAR (für den Self-Updater). */
    public File pluginJarFile() {
        return getFile();
    }

    public PluginConfig pluginConfig() { return pluginConfig; }
    public MessageManager messages() { return messages; }
    public TierRegistry tiers() { return tiers; }
    public BackpackManager manager() { return manager; }
    public BackpackItemFactory itemFactory() { return itemFactory; }
    public ModuleManager moduleManager() { return moduleManager; }
    public RecipeManager recipeManager() { return recipeManager; }
    public de.yourshika.backpacks.upgrade.UpgradeManager upgradeManager() { return upgradeManager; }
    public de.yourshika.backpacks.upgrade.FunctionUpgradeManager functionUpgrades() { return functionUpgrades; }
    public de.yourshika.backpacks.upgrade.UpgradeItemFactory upgradeItems() { return upgradeItems; }
    public de.yourshika.backpacks.place.PlaceableManager placeableManager() { return placeableManager; }
}
