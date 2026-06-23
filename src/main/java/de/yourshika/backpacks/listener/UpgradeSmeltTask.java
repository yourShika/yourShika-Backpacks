package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Schmilzt Items in Rucksäcken mit Smelting-/Blasting-/Smoking-Upgrade (mit
 * Brennstoff aus dem Rucksack). Verarbeitet pro Durchlauf gebündelt die
 * Backpacks online befindlicher Spieler. Die Rezeptkarten liefert der
 * {@link BackpackManager}.
 */
public final class UpgradeSmeltTask extends BukkitRunnable {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;

    public UpgradeSmeltTask(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int done = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (done >= 3) break;
                if (!manager.items().isBackpack(item)) continue;
                UUID id = manager.items().getId(item);
                if (id == null) continue;
                var fns = manager.functionUpgradesOf(id);
                boolean any = false;
                if (fns.contains("blasting")) any |= manager.smelt(id, manager.smeltMap("blast"));
                if (fns.contains("smoking")) any |= manager.smelt(id, manager.smeltMap("smoker"));
                if (fns.contains("smelting")) any |= manager.smelt(id, manager.smeltMap("furnace"));
                if (any) done++;
            }
        }
    }
}
