package de.yourshika.backpacks.craft;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registriert die konfigurierbaren Crafting-Rezepte der Backpack-Tiers und
 * erzwingt optionale Tier-Permissions beim Craften. Gecraftete Backpacks sind
 * Templates ohne ID – die eindeutige ID wird beim ersten Öffnen vergeben.
 */
public final class RecipeManager implements Listener {

    private final YourShikaBackpacks plugin;
    private final TierRegistry tiers;
    private final BackpackItemFactory items;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public RecipeManager(YourShikaBackpacks plugin, TierRegistry tiers, BackpackItemFactory items) {
        this.plugin = plugin;
        this.tiers = tiers;
        this.items = items;
    }

    public void registerAll() {
        unregisterAll();
        if (!plugin.pluginConfig().recipesEnabled()) {
            plugin.getLogger().info("Crafting global deaktiviert (config: crafting.enabled).");
            return;
        }
        int count = 0;
        for (BackpackTier tier : tiers.all()) {
            BackpackTier.RecipeDefinition def = tier.recipe();
            if (def == null || !def.enabled()) continue;
            if (def.shape() == null || def.shape().isEmpty()) continue;
            try {
                NamespacedKey key = new NamespacedKey(plugin, "backpack_" + tier.key());
                ItemStack result = items.createTemplate(tier);
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape(def.shape().toArray(new String[0]));
                for (Map.Entry<Character, org.bukkit.Material> e : def.ingredients().entrySet()) {
                    recipe.setIngredient(e.getKey(), e.getValue());
                }
                recipe.setGroup("yourshika_backpacks");
                Bukkit.addRecipe(recipe);
                registered.add(key);
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Rezept für Tier '" + tier.key() + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Crafting-Rezepte registriert: " + count);
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

    /** Alle registrierten Rezept-Schlüssel (für die Recipe-Book-Freischaltung). */
    public List<NamespacedKey> keys() {
        return List.copyOf(registered);
    }

    /** Erzwingt Tier-Permissions: ohne Berechtigung verschwindet das Ergebnis. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        ItemStack result = recipe.getResult();
        if (!items.isBackpack(result)) return;

        String tierKey = items.getTierKey(result);
        if (tierKey == null) return;
        String perm = "yourshika.backpack.craft." + tierKey;

        for (HumanEntity viewer : event.getViewers()) {
            if (!viewer.hasPermission(perm)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
