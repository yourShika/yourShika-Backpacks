package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.gui.FilterMenuHolder;
import de.yourshika.backpacks.gui.FurnaceMenuHolder;
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

        // 0c) Stations-Buttons (Crafting/Stonecutter/Smithing/Furnace/…) – nur wenn
        //     das passende Funktions-Upgrade verbaut ist.
        if (clickedTop && raw >= BackpackMenuHolder.CONTROL_ROW_START) {
            String station = holder.stationAt(raw);
            if (station != null && manager.functionUpgradesOf(holder.backpackId()).contains(station)) {
                event.setCancelled(true);
                UUID id = holder.backpackId();
                String tier = holder.tierKey();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> manager.openStation(player, station, id, tier));
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

        // Hotbar-/Offhand-Swap: nur Funktions-Upgrades in Upgrade-Slots zulassen.
        if (click == ClickType.NUMBER_KEY) {
            ItemStack hotbar = event.getView().getBottomInventory().getItem(event.getHotbarButton());
            if (clickedTop && rejectUpgrade(player, up, top, holder, hotbar)) {
                event.setCancelled(true);
                return;
            }
        }
        if (click == ClickType.SWAP_OFFHAND) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (clickedTop && rejectUpgrade(player, up, top, holder, off)) {
                event.setCancelled(true);
                return;
            }
        }

        // Cursor-Platzierung in einen Upgrade-Slot.
        if (clickedTop && holder.isUpgradeSlot(raw) && rejectUpgrade(player, up, top, holder, event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Shift-Click aus dem Spieler-Inventar: nur Funktions-Upgrades, kontrolliert.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !clickedTop) {
            ItemStack moving = event.getCurrentItem();
            if (moving == null || moving.getType().isAir()) return;
            if (rejectUpgrade(player, up, top, holder, moving)) {
                event.setCancelled(true);
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
        ItemStack dragged = event.getOldCursor();
        boolean badItem = onlyFunction(up, dragged) || furnaceConflict(up, top, holder, dragged);
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

    // --- Portable Furnace (Smelting/Blasting/Smoking Upgrade) --------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FurnaceMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        boolean clickedTop = raw < top.getSize();
        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        // Zurück-Button: Furnace schließen (Items zurück) und Backpack öffnen.
        if (clickedTop && raw == FurnaceMenuHolder.BACK_SLOT) {
            event.setCancelled(true);
            UUID id = holder.backpackId();
            manager.closeFurnace(player, holder);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String err = manager.openById(player, id);
                if (err == null) return;
                player.closeInventory();
            });
            return;
        }

        // Doppelklick / Backpacks niemals.
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (items.isBackpack(event.getCursor()) || items.isBackpack(event.getCurrentItem())) {
            event.setCancelled(true);
            denyNesting(player);
            return;
        }

        // Shift-Click aus dem Spieler-Inventar: nach Brennstoff/Eingabe routen.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !clickedTop) {
            ItemStack moving = event.getCurrentItem();
            if (moving == null || moving.getType().isAir()) return;
            event.setCancelled(true);
            int target = manager.isFuelItem(moving)
                    ? FurnaceMenuHolder.FUEL_SLOT
                    : (manager.isSmeltable(holder.type(), moving) ? FurnaceMenuHolder.INPUT_SLOT : -1);
            if (target < 0) return; // nicht schmelzbar & kein Brennstoff -> nichts tun
            ItemStack leftover = mergeIntoSlot(top, target, moving.clone());
            event.setCurrentItem(leftover);
            player.updateInventory();
            return;
        }

        // Klicks auf Deko-/gesperrte Slots im oberen Inventar blocken.
        if (clickedTop && !holder.isInteractable(raw)) {
            event.setCancelled(true);
            return;
        }

        // Output-Slot ist nur Entnahme (kein Ablegen/Tauschen).
        if (clickedTop && holder.isOutput(raw)) {
            ItemStack cursor = event.getCursor();
            boolean placing = cursor != null && !cursor.getType().isAir();
            if (placing || click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FurnaceMenuHolder holder)) return;
        boolean draggingBackpack = items.isBackpack(event.getOldCursor());
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) continue;
            // In den oberen Bereich nur Eingabe/Brennstoff – nie Output/Deko.
            if (draggingBackpack || raw == FurnaceMenuHolder.OUTPUT_SLOT || !holder.isInteractable(raw)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FurnaceMenuHolder holder
                && event.getPlayer() instanceof Player player) {
            manager.closeFurnace(player, holder);
        }
    }

    // --- Compacting-Filter (Ghost-Slots) ----------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFilterClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FilterMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int raw = event.getRawSlot();
        boolean clickedTop = raw < top.getSize();

        // Zurück-Button: speichern und Backpack öffnen.
        if (clickedTop && raw == FilterMenuHolder.BACK_SLOT) {
            event.setCancelled(true);
            UUID id = holder.backpackId();
            manager.saveFilter(holder);
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.openById(player, id));
            return;
        }

        if (!clickedTop) return; // Klicks im Spieler-Inventar normal lassen.
        event.setCancelled(true); // Filter sind Ghost-Slots – niemals echte Items bewegen.
        if (!holder.isFilterSlot(raw)) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            if (items.isBackpack(cursor)) { denyNesting(player); return; }
            ItemStack ghost = new ItemStack(cursor.getType());          // nur Typ als Muster
            top.setItem(raw, ghost);
        } else {
            top.setItem(raw, null);                                      // leeren
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFilterDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof FilterMenuHolder) {
            for (int raw : event.getRawSlots()) {
                if (raw < top.getSize()) { event.setCancelled(true); return; }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFilterClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FilterMenuHolder holder) {
            manager.saveFilter(holder);
        }
    }

    /** Legt {@code moving} in einen einzelnen Slot (stapelnd), gibt den Rest zurück. */
    private ItemStack mergeIntoSlot(Inventory inv, int slot, ItemStack moving) {
        ItemStack existing = inv.getItem(slot);
        if (existing == null || existing.getType().isAir()) {
            inv.setItem(slot, moving);
            return null;
        }
        if (existing.isSimilar(moving)) {
            int space = existing.getMaxStackSize() - existing.getAmount();
            int add = Math.min(space, moving.getAmount());
            existing.setAmount(existing.getAmount() + add);
            moving.setAmount(moving.getAmount() - add);
        }
        return moving.getAmount() > 0 ? moving : null;
    }

    /**
     * Prüft ein in die Upgrade-GUI gelegtes Item und meldet dem Spieler den Grund
     * einer Ablehnung. Gibt true zurück, wenn das Item NICHT abgelegt werden darf.
     *
     * <p>Erlaubt sind ausschließlich <b>Funktions-Upgrades</b> (Smoker, Magnet, …) –
     * kein Upgrade-Leder und keine Tier-Upgrades. Von den Schmelz-Upgrades
     * (Smelting/Blasting/Smoking) darf nur <b>eines</b> pro Rucksack verbaut sein.</p>
     */
    private boolean rejectUpgrade(Player player, UpgradeItemFactory up, Inventory top,
                                  UpgradeMenuHolder holder, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (onlyFunction(up, item)) {
            plugin.messages().send(player, "upgrades.no-upgrade-item");
            return true;
        }
        if (furnaceConflict(up, top, holder, item)) {
            plugin.messages().send(player, "upgrades.one-furnace");
            return true;
        }
        return false;
    }

    /** true, wenn das Item kein (reines) Funktions-Upgrade ist. */
    private boolean onlyFunction(UpgradeItemFactory up, ItemStack item) {
        return item != null && !item.getType().isAir() && !up.isFunctionUpgrade(item);
    }

    /**
     * true, wenn {@code item} ein Schmelz-Upgrade ist und im Rucksack bereits ein
     * (anderes) Schmelz-Upgrade verbaut ist – nur eines ist erlaubt.
     */
    private boolean furnaceConflict(UpgradeItemFactory up, Inventory top,
                                    UpgradeMenuHolder holder, ItemStack item) {
        String fn = up.getFunctionType(item);
        if (fn == null || !de.yourshika.backpacks.upgrade.FunctionUpgrade.isFurnaceId(fn)) return false;
        for (int i = 0; i < holder.upgradeSlots(); i++) {
            String present = up.getFunctionType(top.getItem(i));
            if (present != null && de.yourshika.backpacks.upgrade.FunctionUpgrade.isFurnaceId(present)) {
                return true;
            }
        }
        return false;
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
