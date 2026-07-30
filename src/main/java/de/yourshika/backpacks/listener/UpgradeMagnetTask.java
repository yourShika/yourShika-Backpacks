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

            // Ziel ist die BRUSTHÖHE des Spielers (~1 Block über den Füßen) = Zentrum
            // der Aufsammel-Box. So werden Items IN die Box gezogen statt über den Kopf.
            Location target = player.getLocation().add(0, 1.0, 0);
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
                if (dist > radius) continue;

                // Sehr nah: den Restschwung DÄMPFEN, damit das Item in die Aufsammel-Box
                // fällt und von der Vanilla-Mechanik sicher aufgesammelt wird – statt mit
                // Aufwärts-Restschwung darüber hinweg zu gleiten (Ursache für "nicht sammelbar").
                if (dist < 1.0) {
                    item.setVelocity(dir.multiply(0.25));           // sanft Richtung Spieler, kein Boost
                    processed++;
                    continue;
                }

                Vector pull = dir.normalize().multiply(Math.min(0.6, 0.2 * dist));
                // Leichten Auftrieb NUR, wenn das Item deutlich UNTER dem Ziel liegt
                // (damit es nicht am Boden klebt). Liegt es schon auf/über Zielhöhe, wird
                // NICHT nach oben gedrückt – sonst fliegt es über den Kopf.
                if (item.getLocation().getY() < target.getY() - 0.5 && pull.getY() < 0.1) {
                    pull.setY(0.1);
                }
                item.setVelocity(pull);
                processed++;
            }
            if (processed > 0 && plugin.achievements() != null) {
                plugin.achievements().trigger(player, "magnet");
            }
        }
    }
}
