package de.yourshika.backpacks.tier;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lädt und verwaltet alle Backpack-Tiers aus der config.yml.
 * Reihenfolge bleibt erhalten (LinkedHashMap), damit Tab-Completion und
 * Listen einer logischen Sortierung folgen.
 */
public final class TierRegistry {

    private final YourShikaBackpacks plugin;
    private final Map<String, BackpackTier> tiers = new LinkedHashMap<>();

    public TierRegistry(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    public void load(ConfigurationSection root) {
        tiers.clear();
        if (root == null) {
            plugin.getLogger().warning("Kein 'tiers'-Abschnitt in der config.yml gefunden.");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                tiers.put(key.toLowerCase(), parse(key.toLowerCase(), sec));
            } catch (Exception ex) {
                plugin.getLogger().warning("Tier '" + key + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Backpack-Tiers geladen: " + tiers.size());
    }

    private BackpackTier parse(String key, ConfigurationSection sec) {
        String displayName = sec.getString("display-name", "<white>" + key + " Backpack</white>");
        Material material = Material.matchMaterial(sec.getString("material", "CHEST"));
        if (material == null) material = Material.CHEST;
        int cmd = sec.getInt("custom-model-data", 0);
        int rows = sec.getInt("storage-rows", 3);
        int upgrades = sec.getInt("upgrade-slots", 0);
        var mainColor = parseDye(sec.getString("default-main-color", "BROWN"));
        var accentColor = parseDye(sec.getString("default-accent-color", "ORANGE"));
        String permission = sec.getString("permission", null);
        if (permission != null && permission.isBlank()) permission = null;
        double price = sec.getDouble("price", 0.0D);
        List<String> lore = sec.getStringList("lore");

        ConfigurationSection recipeSec = sec.getConfigurationSection("recipe");
        BackpackTier.RecipeDefinition recipe = parseRecipe(recipeSec);

        return new BackpackTier(key, displayName, material, cmd, rows, upgrades,
                mainColor, accentColor, permission, price, lore, recipe);
    }

    private BackpackTier.RecipeDefinition parseRecipe(ConfigurationSection sec) {
        if (sec == null) return new BackpackTier.RecipeDefinition(false, List.of(), Map.of());
        boolean enabled = sec.getBoolean("enabled", false);
        List<String> shape = sec.getStringList("shape");
        Map<Character, Material> ingredients = new LinkedHashMap<>();
        ConfigurationSection ing = sec.getConfigurationSection("ingredients");
        if (ing != null) {
            for (String k : ing.getKeys(false)) {
                if (k.isEmpty()) continue;
                Material m = Material.matchMaterial(ing.getString(k, ""));
                if (m != null) ingredients.put(k.charAt(0), m);
            }
        }
        return new BackpackTier.RecipeDefinition(enabled, shape, ingredients);
    }

    private org.bukkit.DyeColor parseDye(String name) {
        try {
            return org.bukkit.DyeColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return org.bukkit.DyeColor.WHITE;
        }
    }

    public BackpackTier get(String key) {
        return key == null ? null : tiers.get(key.toLowerCase());
    }

    public boolean exists(String key) {
        return key != null && tiers.containsKey(key.toLowerCase());
    }

    public Collection<BackpackTier> all() {
        return tiers.values();
    }

    public List<String> keys() {
        return new ArrayList<>(tiers.keySet());
    }
}
