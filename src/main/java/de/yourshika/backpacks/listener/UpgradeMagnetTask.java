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
        // Cooldown/Drossel gegen Lag bei sehr vielen Drops (#53).
        int maxPerTick = Math.max(1, plugin.getConfig().getInt("upgrades.magnet.max-per-tick", 60));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Gedrosselter Hinweis, wenn Magnet/Pickup wegen mehrerer gleichartiger
            // Rucksäcke automatisch deaktiviert sind.
            manager.maybeWarnDuplicates(player);
            int radius = manager.magnetRadius(player);
            if (radius <= 0) continue;

            Location target = player.getLocation().add(0, 0.6, 0);
            int processed = 0;
            for (var entity : player.getNearbyEntities(radius, radius, radius)) {
                if (processed >= maxPerTick) break;                 // Drossel
                if (!(entity instanceof Item item)) continue;
                if (manager.items().isBackpack(item.getItemStack())) continue; // Backpacks nicht magnetisieren

                // Item-Owner & Pickup-Delay stärker respektieren (#54).
                int delay = item.getPickupDelay();
                if (delay >= 32767) continue;                       // nie aufsammelbar
                java.util.UUID owner = item.getOwner();
                if (owner != null && !owner.equals(player.getUniqueId())) continue; // gehört jemand anderem
                if (delay > 10) continue;                           // gerade geworfen -> Werfer bevorzugen

                Vector dir = target.toVector().subtract(item.getLocation().toVector());
                double dist = dir.length();
                if (dist < 1.2 || dist > radius) continue;          // sehr nah -> normales Aufsammeln
                Vector pull = dir.normalize().multiply(Math.min(0.6, 0.18 * dist));
                // Items über Blöcke gleiten lassen statt in den Boden zu ziehen (#…):
                // ein leichter Auftrieb verhindert, dass sie in Blöcken verschwinden.
                if (pull.getY() < 0.18) pull.setY(0.18);
                item.setVelocity(pull);
                processed++;
            }
            if (processed > 0 && plugin.achievements() != null) {
                plugin.achievements().trigger(player, "magnet");
            }
        }
    }
}
