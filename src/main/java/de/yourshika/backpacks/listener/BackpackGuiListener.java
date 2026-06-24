package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.gui.ModulesMenuHolder;
import de.yourshika.backpacks.gui.UpgradeMenuHolder;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.upgrade.UpgradeItemFactory;

import java.util.UUID;
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

        // 0b) Upgrades-Button öffnet die separate Upgrade-GUI.
        if (clickedTop && holder.isUpgradeButton(raw)) {
            event.setCancelled(true);
            UUID id = holder.backpackId();
            String tier = holder.tierKey();
            // Nächster Tick: das Schließen des Backpacks speichert den Inhalt,
            // danach öffnet die Upgrade-GUI sauber.
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.openUpgrades(player, id, tier));
            return;
        }

        // 0c) Stations-Buttons (Crafting/Stonecutter/Smithing) – nur wenn das
        //     passende Funktions-Upgrade verbaut ist.
        if (clickedTop && raw >= BackpackMenuHolder.CONTROL_ROW_START) {
            String station = BackpackMenuHolder.stationAt(raw);
            if (station != null && manager.functionUpgradesOf(holder.backpackId()).contains(station)) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTask(plugin, () -> manager.openStation(player, station));
                return;
            }
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
            if (clickedTop && !canStoreBackpack(holder, hotbar)) {
                event.setCancelled(true);
                denyNesting(player);
                return;
            }
        }
        if (click == ClickType.SWAP_OFFHAND) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            boolean invalidBackpack = !canStoreBackpack(holder, offhand);
            if (clickedTop && (holder.isLocked(raw) || invalidBackpack)) {
                event.setCancelled(true);
                if (invalidBackpack) denyNesting(player);
                return;
            }
        }

        // 4) Anti-Nesting: kein Backpack auf den Cursor in das Lager legen.
        if (clickedTop && !canStoreBackpack(holder, event.getCursor())) {
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
                if (!canStoreBackpack(holder, moving)) {
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

    // /bp modules: Klicks schalten Master-Schalter bzw. einzelne Module um.
    @EventHandler(priority = EventPriority.HIGH)
    public void onModulesClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ModulesMenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof ModulesMenuHolder)) {
            return; // Klick im Spieler-Inventar – ignorieren.
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("yourshika.backpack.admin.modules")) return;

        int raw = event.getRawSlot();
        String moduleId = holder.moduleAt(raw);
        if (moduleId != null) {
            boolean now = plugin.pluginConfig().isModuleEnabled(moduleId);
            plugin.setModuleEnabled(moduleId, !now);
            de.yourshika.backpacks.gui.ModulesMenu.refresh(plugin, holder);
        }
    }

    // /bp info: reine Anzeige-/Navigations-GUI.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInfoClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof de.yourshika.backpacks.gui.InfoMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof de.yourshika.backpacks.gui.InfoMenuHolder)) {
            return;
        }
        String action = holder.actionAt(event.getRawSlot());
        if (action != null) {
            de.yourshika.backpacks.gui.InfoMenu.openAction(plugin, player, action);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInfoDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof de.yourshika.backpacks.gui.InfoMenuHolder) {
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

        boolean badBackpack = !canStoreBackpack(holder, event.getOldCursor());
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) continue; // Slot im Spieler-Inventar – egal.
            if (holder.isLocked(raw) || badBackpack) {
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
        if (raw < top.getSize() && !canStoreBackpack(holder, event.getCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) denyNesting(player);
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

    // --- Upgrade-GUI -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUpgradeClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof UpgradeMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        boolean clickedTop = raw < top.getSize();
        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        UpgradeItemFactory up = plugin.upgradeItems();

        // Zurück-Button.
        if (clickedTop && raw == holder.backButtonSlot()) {
            event.setCancelled(true);
            UUID id = holder.backpackId();
            manager.saveUpgrades(holder);
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.openById(player, id));
            return;
        }
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (clickedTop && holder.isLocked(raw)) {
            event.setCancelled(true);
            return;
        }

        // Hotbar-/Offhand-Swap: nur Upgrade-Items in Upgrade-Slots zulassen.
        if (click == ClickType.NUMBER_KEY) {
            ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
            if (clickedTop && notAllowedUpgrade(up, hotbar)) {
                event.setCancelled(true);
                denyUpgrade(player);
                return;
            }
        }
        if (click == ClickType.SWAP_OFFHAND) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (clickedTop && notAllowedUpgrade(up, off)) {
                event.setCancelled(true);
                denyUpgrade(player);
                return;
            }
        }

        // Cursor-Platzierung in einen Upgrade-Slot.
        if (clickedTop && holder.isUpgradeSlot(raw) && notAllowedUpgrade(up, event.getCursor())) {
            event.setCancelled(true);
            denyUpgrade(player);
            return;
        }

        // Shift-Click aus dem Spieler-Inventar: nur Upgrade-Items, kontrolliert.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !clickedTop) {
            ItemStack moving = event.getCurrentItem();
            if (moving == null || moving.getType().isAir()) return;
            if (!up.isAnyUpgrade(moving)) {
                event.setCancelled(true);
                denyUpgrade(player);
                return;
            }
            event.setCancelled(true);
            ItemStack leftover = moveIntoStorage(top, holder.upgradeSlots(), moving.clone());
            event.setCurrentItem(leftover);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUpgradeDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof UpgradeMenuHolder holder)) return;
        UpgradeItemFactory up = plugin.upgradeItems();
        boolean badItem = notAllowedUpgrade(up, event.getOldCursor());
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) continue;
            if (holder.isLocked(raw) || badItem) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpgradeClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof UpgradeMenuHolder holder) {
            manager.saveUpgrades(holder);
        }
    }

    /** true, wenn das Item nicht air und KEIN Upgrade-Item ist. */
    private boolean notAllowedUpgrade(UpgradeItemFactory up, ItemStack item) {
        return item != null && !item.getType().isAir() && !up.isAnyUpgrade(item);
    }

    private void denyUpgrade(Player player) {
        plugin.messages().send(player, "upgrades.no-upgrade-item");
    }

    private void denyNesting(Player player) {
        plugin.messages().send(player, "error.no-nesting");
    }

    private boolean canStoreBackpack(BackpackMenuHolder holder, ItemStack item) {
        return !items.isBackpack(item) || manager.canStoreBackpackInside(holder.backpackId(), item);
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
