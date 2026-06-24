package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.place.PlaceableManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * Behandelt Interaktionen mit platzierten Backpacks: Rechtsklick öffnet,
 * Schlagen hebt auf. Außerdem werden die Display-/Interaction-Entities vor
 * Explosionen geschützt, damit kein Backpack verloren geht.
 */
public final class PlaceableListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final PlaceableManager placeable;

    public PlaceableListener(YourShikaBackpacks plugin, BackpackManager manager, PlaceableManager placeable) {
        this.plugin = plugin;
        this.manager = manager;
        this.placeable = placeable;
    }

    /**
     * Rechtsklick auf ein platziertes Backpack → öffnen.
     * Sneak-Rechtsklick → aufheben (zuverlässig, da Interaction-Entities keine
     * verlässlichen Angriffs-Events liefern).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // nicht doppelt (Off-Hand) auslösen
        Entity entity = event.getRightClicked();
        if (!placeable.isPlacedEntity(entity)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            if (!player.hasPermission("yourshika.backpack.place")) {
                plugin.messages().send(player, "error.no-permission");
                return;
            }
            UUID id = placeable.backpackIdOf(entity);
            if (id != null) {
                if (manager.isOpen(id)) {
                    plugin.messages().send(player, "error.already-open");
                    return;
                }
                var data = manager.storage().load(id);
                if (data != null && !manager.canAccess(player, data.owner())) {
                    plugin.messages().send(player, "error.not-owner");
                    return;
                }
            }
            if (placeable.pickup(entity)) {
                plugin.messages().send(player, "place.removed");
            } else {
                plugin.messages().send(player, "error.invalid-backpack");
            }
            return;
        }
        if (!player.hasPermission("yourshika.backpack.open")) {
            plugin.messages().send(player, "error.no-permission");
            return;
        }
        UUID id = placeable.backpackIdOf(entity);
        if (id == null) return;
        String error = manager.openById(player, id);
        if (error != null) {
            plugin.messages().send(player, error);
        }
    }

    /** Platzierte Backpacks nehmen keinen Schaden (Schutz, kein Verlust). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (placeable.isPlacedEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** Schutz vor Explosionen u.ä. – platzierte Backpacks nehmen keinen Schaden. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (placeable.isPlacedEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
