package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sicherheits-Listener: verhindert Missbrauch des Backpack-Items.
 *
 * <ul>
 *   <li><b>Pferderüstung:</b> Da Backpacks technisch auf
 *       {@code LEATHER_HORSE_ARMOR} basieren, wird unterbunden, dass sie als
 *       echte Pferderüstung auf ein Pferd/Lama gelegt werden können – sonst
 *       würde der Spieler sein Backpack im Tier-Inventar „verlieren“.</li>
 *   <li><b>Hopper/Automation:</b> Backpacks dürfen nicht von Hoppern oder
 *       anderen Automationen bewegt oder eingesammelt werden.</li>
 * </ul>
 */
public final class BackpackSecurityListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackItemFactory items;

    public BackpackSecurityListener(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.items = plugin.itemFactory();
    }

    // --- Pferderüstung -----------------------------------------------------

    /** Rechtsklick mit Backpack auf ein Pferd/Lama (würde es als Rüstung anlegen). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractHorse(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractHorse)) return;
        EquipmentSlot hand = event.getHand();
        ItemStack inHand = hand == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        if (items.isBackpack(inHand)) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "error.no-horse-armor");
        }
    }

    /**
     * Verhindert, dass ein Backpack in das Pferde-Rüstungs-Inventar
     * ({@link HorseInventory}) gelegt wird. Lade-Inventare von Eseln/Lamas
     * (Truhen) bleiben unberührt – dort dürfen Backpacks normal gelagert werden.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHorseInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory() instanceof HorseInventory)) return;

        if (items.isBackpack(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick().isKeyboardClick()) {
            ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
            if (items.isBackpack(hotbar)) {
                event.setCancelled(true);
                return;
            }
        }
        // Shift-Click eines Backpacks aus dem Spieler-Inventar würde es als
        // Rüstung anlegen.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && items.isBackpack(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHorseInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory() instanceof HorseInventory)) return;
        if (!items.isBackpack(event.getOldCursor())) return;
        for (int raw : event.getRawSlots()) {
            if (raw < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- Hopper / Automation ----------------------------------------------

    /** Hopper/Dropper/etc. dürfen Backpacks nicht zwischen Inventaren bewegen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (items.isBackpack(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Hopper/Hopper-Minecart dürfen herumliegende Backpacks nicht aufsaugen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        if (event.getItem() != null && items.isBackpack(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
