package de.yourshika.backpacks.storage;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.util.ItemSerialization;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * YAML-Implementierung als einfache Alternative zu SQLite. Speichert alle
 * Backpacks in einer Datei. Für sehr große Server ist SQLite empfohlen.
 */
public final class YamlBackpackStorage implements BackpackStorage {

    private final YourShikaBackpacks plugin;
    private final File file;
    private YamlConfiguration config;
    private final Object lock = new Object();

    public YamlBackpackStorage(YourShikaBackpacks plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    @Override
    public void init() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Datenordner konnte nicht erstellt werden: " + parent);
        }
        if (!file.exists()) file.createNewFile();
        config = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("YAML-Speicher initialisiert (" + file.getName() + ").");
    }

    private void persist() {
        try {
            config.save(file);
        } catch (Exception ex) {
            plugin.getLogger().severe("YAML-Speicher konnte nicht geschrieben werden: " + ex.getMessage());
        }
    }

    @Override
    public BackpackData load(UUID id) {
        synchronized (lock) {
            ConfigurationSection sec = config.getConfigurationSection(id.toString());
            if (sec == null) return null;
            try {
                BackpackData data = new BackpackData(id);
                String owner = sec.getString("owner");
                data.owner(owner == null ? null : UUID.fromString(owner));
                data.tier(sec.getString("tier"));
                data.mainColor(sec.getString("main-color"));
                data.accentColor(sec.getString("accent-color"));
                data.contents(ItemSerialization.fromBase64(sec.getString("contents")));
                data.upgrades(ItemSerialization.fromBase64(sec.getString("upgrades")));
                data.furnace(ItemSerialization.fromBase64(sec.getString("furnace")));
                data.furnaceCook(sec.getInt("furnace-cook"));
                data.furnaceBurn(sec.getInt("furnace-burn"));
                data.compactFilter(ItemSerialization.fromBase64(sec.getString("compact-filter")));
                data.pickupFilter(ItemSerialization.fromBase64(sec.getString("pickup-filter")));
                data.name(sec.getString("name"));
                data.storedXp(sec.getInt("stored-xp"));
                data.placed(sec.getBoolean("placed"));
                data.world(sec.getString("world"));
                data.position(sec.getDouble("x"), sec.getDouble("y"), sec.getDouble("z"));
                data.created(sec.getLong("created"));
                data.modified(sec.getLong("modified"));
                return data;
            } catch (RuntimeException ex) {
                // Beschädigter Datensatz darf nicht die Autosave-Schleife für ALLE
                // Backpacks abbrechen (B4).
                plugin.getLogger().severe("Backpack " + id + " konnte nicht geladen werden: " + ex.getMessage());
                return null;
            }
        }
    }

    @Override
    public void save(BackpackData data) {
        data.touch();
        synchronized (lock) {
            ConfigurationSection sec = config.createSection(data.id().toString());
            sec.set("owner", data.owner() == null ? null : data.owner().toString());
            sec.set("tier", data.tier());
            sec.set("main-color", data.mainColor());
            sec.set("accent-color", data.accentColor());
            sec.set("contents", ItemSerialization.toBase64(data.contents()));
            sec.set("upgrades", ItemSerialization.toBase64(data.upgrades()));
            sec.set("furnace", ItemSerialization.toBase64(data.furnace()));
            sec.set("furnace-cook", data.furnaceCook());
            sec.set("furnace-burn", data.furnaceBurn());
            sec.set("compact-filter", ItemSerialization.toBase64(data.compactFilter()));
            sec.set("pickup-filter", ItemSerialization.toBase64(data.pickupFilter()));
            sec.set("name", data.name());
            sec.set("stored-xp", data.storedXp());
            sec.set("placed", data.placed());
            sec.set("world", data.world());
            sec.set("x", data.x());
            sec.set("y", data.y());
            sec.set("z", data.z());
            sec.set("created", data.created());
            sec.set("modified", data.modified());
            persist();
        }
    }

    @Override
    public void delete(UUID id) {
        synchronized (lock) {
            config.set(id.toString(), null);
            persist();
        }
    }

    @Override
    public boolean exists(UUID id) {
        synchronized (lock) {
            return config.contains(id.toString());
        }
    }

    @Override
    public List<UUID> listByOwner(UUID owner) {
        List<UUID> result = new ArrayList<>();
        synchronized (lock) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection sec = config.getConfigurationSection(key);
                if (sec == null) continue;
                if (owner.toString().equals(sec.getString("owner"))) {
                    try {
                        result.add(UUID.fromString(key));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int count() {
        synchronized (lock) {
            return config.getKeys(false).size();
        }
    }

    @Override
    public List<BackpackMeta> allMeta() {
        List<BackpackMeta> result = new ArrayList<>();
        synchronized (lock) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection sec = config.getConfigurationSection(key);
                if (sec == null) continue;
                try {
                    String owner = sec.getString("owner");
                    result.add(new BackpackMeta(
                            UUID.fromString(key),
                            owner == null ? null : UUID.fromString(owner),
                            sec.getString("tier"),
                            sec.getBoolean("placed")));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    @Override
    public void close() {
        synchronized (lock) {
            persist();
        }
    }
}
