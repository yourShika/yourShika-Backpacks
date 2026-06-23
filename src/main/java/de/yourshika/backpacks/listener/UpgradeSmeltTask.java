package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Schmilzt Items in Rucksäcken mit Smelting-/Blasting-/Smoking-Upgrade (mit
 * Brennstoff aus dem Rucksack). Verarbeitet pro Durchlauf gebündelt die
 * Backpacks online befindlicher Spieler.
 */
public final class UpgradeSmeltTask extends BukkitRunnable {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;

    private final Map<Material, ItemStack> furnace = new HashMap<>();
    private final Map<Material, ItemStack> blast = new HashMap<>();
    private final Map<Material, ItemStack> smoker = new HashMap<>();

    public UpgradeSmeltTask(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        buildMaps();
    }

    private void buildMaps() {
        Iterator<Recipe> it = plugin.getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof BlastingRecipe r) {
                put(blast, r);
            } else if (recipe instanceof SmokingRecipe r) {
                put(smoker, r);
            } else if (recipe instanceof FurnaceRecipe r) {
                put(furnace, r);
            }
        }
        plugin.getLogger().info("Schmelz-Rezepte geladen: Ofen " + furnace.size()
                + ", Hochofen " + blast.size() + ", Räucherofen " + smoker.size());
    }

    private void put(Map<Material, ItemStack> map, CookingRecipe<?> r) {
        RecipeChoice choice = r.getInputChoice();
        if (choice instanceof RecipeChoice.MaterialChoice mc) {
            for (Material m : mc.getChoices()) {
                map.putIfAbsent(m, r.getResult());
            }
        }
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int done = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (done >= 3) break;                       // pro Spieler/Durchlauf begrenzen
                if (!manager.items().isBackpack(item)) continue;
                UUID id = manager.items().getId(item);
                if (id == null) continue;
                var fns = manager.functionUpgradesOf(id);
                boolean any = false;
                if (fns.contains("blasting")) any |= manager.smelt(id, blast);
                if (fns.contains("smoking")) any |= manager.smelt(id, smoker);
                if (fns.contains("smelting")) any |= manager.smelt(id, furnace);
                if (any) done++;
            }
        }
    }
}
