package de.yourshika.backpacks.upgrade;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet die funktionalen Upgrades: baut deren kanonische Items, registriert
 * die (idempotenten) Crafting-Rezepte und stellt sie für GUI/Logik bereit.
 */
public final class FunctionUpgradeManager {

    private final YourShikaBackpacks plugin;
    private final UpgradeItemFactory upgrades;
    private final UpgradeManager tierUpgrades;

    private final Map<String, ItemStack> items = new LinkedHashMap<>();
    private final List<NamespacedKey> registered = new ArrayList<>();

    public FunctionUpgradeManager(YourShikaBackpacks plugin, UpgradeItemFactory upgrades,
                                  UpgradeManager tierUpgrades) {
        this.plugin = plugin;
        this.upgrades = upgrades;
        this.tierUpgrades = tierUpgrades;
    }

    public void registerAll() {
        buildItems();
        if (!plugin.pluginConfig().recipesEnabled()) return;
        if (!plugin.getConfig().getBoolean("upgrades.functions.enabled", true)) {
            plugin.getLogger().info("Funktions-Upgrades deaktiviert (config: upgrades.functions.enabled).");
            return;
        }
        ItemStack base = tierUpgrades.baseUpgradeItem();
        int count = 0;
        for (FunctionUpgrade up : FunctionUpgrade.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "func_" + up.id());
            if (Bukkit.getRecipe(key) != null) {           // idempotent (kein removeRecipe!)
                if (!registered.contains(key)) registered.add(key);
                continue;
            }
            try {
                ShapedRecipe recipe = new ShapedRecipe(key, items.get(up.id()));
                recipe.shape(up.shape().toArray(new String[0]));
                String shapeStr = String.join("", up.shape());
                for (Map.Entry<Character, Material> e : up.ingredients().entrySet()) {
                    recipe.setIngredient(e.getKey(), new RecipeChoice.MaterialChoice(e.getValue()));
                }
                if (shapeStr.indexOf('U') >= 0) {
                    recipe.setIngredient('U', new RecipeChoice.ExactChoice(base));
                }
                // Advanced-Varianten benötigen ihr Basis-Upgrade ('X').
                if (up.requires() != null && shapeStr.indexOf('X') >= 0) {
                    ItemStack required = items.get(up.requires());
                    if (required == null) { continue; } // Basis noch nicht gebaut -> überspringen
                    recipe.setIngredient('X', new RecipeChoice.ExactChoice(required));
                }
                recipe.setGroup("yourshika_upgrades");
                Bukkit.addRecipe(recipe);
                registered.add(key);
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Funktions-Upgrade-Rezept '" + up.id() + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Funktions-Upgrade-Rezepte registriert: " + count);
    }

    private void buildItems() {
        items.clear();
        for (FunctionUpgrade up : FunctionUpgrade.values()) {
            int cmd = plugin.getConfig().getInt("upgrades.functions.models." + up.id() + ".custom-model-data",
                    up.customModelData());
            String model = plugin.getConfig().getString("upgrades.functions.models." + up.id() + ".item-model", "");
            String provider = plugin.getConfig().getString("upgrades.functions.models." + up.id() + ".provider-id",
                    up.providerId());
            items.put(up.id(), upgrades.functionUpgrade(up.id(), up.displayName(), up.lore(), cmd, model, provider));
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Throwable ignored) {
            }
        }
        registered.clear();
    }

    public List<NamespacedKey> keys() {
        return List.copyOf(registered);
    }

    /** Kanonisches Funktions-Upgrade-Item (Kopie) oder null. */
    public ItemStack item(String id) {
        ItemStack item = items.get(id);
        return item == null ? null : item.clone();
    }
}
