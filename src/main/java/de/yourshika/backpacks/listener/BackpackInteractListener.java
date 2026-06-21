package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Öffnet ein Backpack per Rechtsklick (Haupt- oder Nebenhand) und verhindert
 * dabei das versehentliche Platzieren des Item-Blocks.
 */
public final class BackpackInteractListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final BackpackItemFactory items;

    public BackpackInteractListener(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = manager.items();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.pluginConfig().rightClickOpen()) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        if (hand == EquipmentSlot.OFF_HAND && !plugin.pluginConfig().offhandOpen()) return;
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;

        ItemStack item = event.getItem();
        if (!items.isBackpack(item)) return;

        Player player = event.getPlayer();

        // Block-Platzierung des Item-Materials unterbinden.
        event.setCancelled(true);

        if (!player.hasPermission("yourshika.backpack.use")
                || !player.hasPermission("yourshika.backpack.open")) {
            plugin.messages().send(player, "error.no-permission");
            return;
        }
        if (!plugin.pluginConfig().isWorldAllowed(player.getWorld().getName())) {
            plugin.messages().send(player, "error.world-disabled");
            return;
        }

        String error = manager.openFromItem(player, item);
        if (error != null) {
            plugin.messages().send(player, error);
            return;
        }

        // Eventuell wurde eine Lazy-ID in das Item geschrieben – zurück in die Hand legen.
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.getInventory().setItemInOffHand(item);
        }
    }
}
