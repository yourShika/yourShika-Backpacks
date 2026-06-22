package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.gui.ModulesMenuHolder;
import de.yourshika.backpacks.item.BackpackItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Schützt die Backpack-GUI vor sämtlichen Dupe- und Verlust-Vektoren:
 * gesperrte Slots, Shift-Click, Drag, Hotbar-Swap, Offhand-Swap, Doppelklick
 * und Backpacks-in-Backpacks. Steuert außerdem das Blättern und speichert beim
 * Schließen sicher.
 */
public final class BackpackGuiListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final BackpackItemFactory items;

    public BackpackGuiListener(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = manager.items();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BackpackMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        boolean clickedTop = raw < top.getSize();
        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        // 0) Blättern.
        if (clickedTop && holder.isPrevButton(raw)) {
            event.setCancelled(true);
            manager.changePage(holder, -1);
            return;
        }
        if (clickedTop && holder.isNextButton(raw)) {
            event.setCancelled(true);
            manager.changePage(holder, +1);
            return;
        }

        // 1) Doppelklick (Items zusammenführen) komplett unterbinden.
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // 2) Klick auf gesperrte Slots (Steuerleiste / inaktive Lager-Slots) blocken.
        if (clickedTop && holder.isLocked(raw)) {
            event.setCancelled(true);
            return;
        }

        // 3) Hotbar-/Offhand-Swap absichern; Anti-Nesting prüfen.
        if (click == ClickType.NUMBER_KEY) {
            if (clickedTop && holder.isLocked(raw)) {
                event.setCancelled(true);
                return;
            }
            ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
            if (clickedTop && items.isBackpack(hotbar)) {
                event.setCancelled(true);
                denyNesting(player);
                return;
            }
        }
        if (click == ClickType.SWAP_OFFHAND) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (clickedTop && (holder.isLocked(raw) || items.isBackpack(offhand))) {
                event.setCancelled(true);
                return;
            }
        }

        // 4) Anti-Nesting: kein Backpack auf den Cursor in das Lager legen.
        if (clickedTop && items.isBackpack(event.getCursor())) {
            event.setCancelled(true);
            denyNesting(player);
            return;
        }

        // 5) Shift-Click aus dem Spieler-Inventar kontrolliert behandeln.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            boolean fromPlayer = !clickedTop;
            if (fromPlayer) {
                ItemStack moving = event.getCurrentItem();
                if (moving == null || moving.getType().isAir()) return;
                if (items.isBackpack(moving)) {
                    event.setCancelled(true);
                    denyNesting(player);
                    return;
                }
                // Manuelles, sicheres Verschieben ausschließlich in aktive Lager-Slots.
                event.setCancelled(true);
                ItemStack leftover = moveIntoStorage(top, holder.activeCount(), moving.clone());
                event.setCurrentItem(leftover);
                player.updateInventory();
                return;
            } else {
                // Shift-Click aus dem Backpack heraus: gesperrte Slots schützen.
                if (holder.isLocked(raw)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Reine Anzeige-GUI /bp modules: jede Interaktion unterbinden.
    @EventHandler(priority = EventPriority.HIGH)
    public void onModulesClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ModulesMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onModulesDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ModulesMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BackpackMenuHolder holder)) return;

        boolean draggingBackpack = items.isBackpack(event.getOldCursor());
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) continue; // Slot im Spieler-Inventar – egal.
            if (holder.isLocked(raw) || draggingBackpack) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Creative-Klicks sind ein eigener Event-Typ – gesperrte Slots ebenfalls schützen.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BackpackMenuHolder holder)) return;
        int raw = event.getRawSlot();
        if (raw < top.getSize() && holder.isLocked(raw)) {
            event.setCancelled(true);
        }
        if (raw < top.getSize() && items.isBackpack(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof BackpackMenuHolder holder) {
            manager.saveAndRelease(holder);
            plugin.debug("Backpack " + holder.backpackId() + " gespeichert & freigegeben.");
        }
    }

    private void denyNesting(Player player) {
        plugin.messages().send(player, "error.no-nesting");
    }

    /**
     * Verschiebt ein Item ausschließlich in die aktiven Lager-Slots
     * (0..activeCount-1) der aktuellen Seite. Stapelt zuerst in passende Stacks,
     * danach in leere Slots. Gibt den Rest zurück (oder null).
     */
    private ItemStack moveIntoStorage(Inventory inv, int active, ItemStack moving) {
        int max = moving.getMaxStackSize();
        for (int i = 0; i < active && moving.getAmount() > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot != null && slot.isSimilar(moving)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                if (space > 0) {
                    int add = Math.min(space, moving.getAmount());
                    slot.setAmount(slot.getAmount() + add);
                    moving.setAmount(moving.getAmount() - add);
                }
            }
        }
        for (int i = 0; i < active && moving.getAmount() > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType().isAir()) {
                int add = Math.min(max, moving.getAmount());
                ItemStack copy = moving.clone();
                copy.setAmount(add);
                inv.setItem(i, copy);
                moving.setAmount(moving.getAmount() - add);
            }
        }
        return moving.getAmount() > 0 ? moving : null;
    }
}
