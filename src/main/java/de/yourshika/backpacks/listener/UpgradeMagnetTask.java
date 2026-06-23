package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Zieht für Spieler mit Magnet-/Advanced-Magnet-/Advanced-Pickup-Upgrade
 * herumliegende Items im Wirkradius heran. Das eigentliche Einsammeln übernimmt
 * danach die normale Aufsammel-Mechanik (ggf. mit Pickup-Upgrade in den Rucksack).
 */
public final class UpgradeMagnetTask extends BukkitRunnable {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;

    public UpgradeMagnetTask(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int radius = manager.magnetRadius(player);
            if (radius <= 0) continue;

            Location target = player.getLocation().add(0, 0.6, 0);
            for (var entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof Item item)) continue;
                if (item.getPickupDelay() > 40) continue;          // gerade erst geworfen
                if (manager.items().isBackpack(item.getItemStack())) continue; // Backpacks nicht magnetisieren

                Vector dir = target.toVector().subtract(item.getLocation().toVector());
                double dist = dir.length();
                if (dist < 1.2 || dist > radius) continue;          // sehr nah -> normales Aufsammeln
                item.setVelocity(dir.normalize().multiply(Math.min(0.6, 0.18 * dist)));
            }
        }
    }
}
