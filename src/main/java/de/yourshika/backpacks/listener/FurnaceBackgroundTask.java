package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Lässt die portablen Schmelz-Stationen (Smelting/Blasting/Smoking) der Backpacks
 * online befindlicher Spieler auch dann weiterlaufen, wenn die Furnace-GUI gerade
 * nicht offen ist (#3), und aktualisiert die Live-Lore des Furnace-Icons in offenen
 * Backpack-GUIs.
 */
public final class FurnaceBackgroundTask extends BukkitRunnable {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;

    public FurnaceBackgroundTask(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        Set<UUID> processed = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Schmelz-Stationen getragener Backpacks im Hintergrund weiterlaufen lassen.
            for (ItemStack item : player.getInventory().getContents()) {
                if (!manager.items().isBackpack(item)) continue;
                UUID id = manager.items().getId(item);
                if (id == null || !processed.add(id)) continue;
                var fns = manager.functionUpgradesOf(id);
                if (fns.contains("smelting") || fns.contains("blasting") || fns.contains("smoking")) {
                    manager.backgroundFurnaceStep(id);
                }
            }
            // Live-Lore des Furnace-Icons aktualisieren, falls eine Backpack-GUI offen ist.
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackMenuHolder holder) {
                manager.refreshStationLore(holder);
            }
        }
    }
}
