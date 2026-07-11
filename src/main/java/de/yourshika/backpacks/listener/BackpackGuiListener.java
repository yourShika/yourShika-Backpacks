package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.gui.FilterMenuHolder;
import de.yourshika.backpacks.gui.FurnaceMenuHolder;
import de.yourshika.backpacks.gui.ModulesMenuHolder;
import de.yourshika.backpacks.gui.TrashMenuHolder;
import de.yourshika.backpacks.gui.XpMenuHolder;
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

        // 0) Blättern. Shift-Klick springt zur ersten/letzten Seite.
        if (clickedTop && holder.isPrevButton(raw)) {
            event.setCancelled(true);
            if (event.isShiftClick()) manager.goToPage(holder, 0);
            else manager.changePage(holder, -1);
            return;
        }
        if (clickedTop && holder.isNextButton(raw)) {
            event.setCancelled(true);
            if (event.isShiftClick()) manager.goToPage(holder, holder.pageCount() - 1);
            else manager.changePage(holder, +1);
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
            if (station != null && manager.stationActive(holder.backpackId(), station)) {
                event.setCancelled(true);
                UUID id = holder.backpackId();
                String tier = holder.tierKey();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> manager.openStation(player, station, id, tier));
                return;
            }
        }

        // 0d) Klick auf das Info-Item sortiert den Backpack-Inhalt.
        if (clickedTop && holder.isInfoButton(raw)) {
            event.setCancelled(true);
            manager.sortBackpack(holder, player);
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

    // /bp recall: Auswahl-GUI (mehrere platzierte Backpacks).
    @EventHandler(priority = EventPriority.HIGH)
    public void onRecallClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof de.yourshika.backpacks.gui.RecallMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof de.yourshika.backpacks.gui.RecallMenuHolder)) {
            return; // Klick im Spieler-Inventar.
        }
        int raw = event.getRawSlot();
        if (holder.isAll(raw)) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int n = plugin.placeableManager().recall(player);
                if (n > 0) {
                    plugin.audit(player.getName(), "RECALL", n + " backpack(s)");
                    plugin.messages().send(player, "place.recalled",
                            de.yourshika.backpacks.config.MessageManager.ph("count", String.valueOf(n)));
                } else {
                    plugin.messages().send(player, "place.recall-none");
                }
            });
            return;
        }
        java.util.UUID id = holder.at(raw);
        if (id == null) return;
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.placeableManager().recallOne(player, id)) {
                plugin.audit(player.getName(), "RECALL", String.valueOf(id));
                plugin.messages().send(player, "place.recalled",
                        de.yourshika.backpacks.config.MessageManager.ph("count", "1"));
            } else {
                plugin.messages().send(player, "place.recall-none");
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRecallDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof de.yourshika.backpacks.gui.RecallMenuHolder) {
            event.setCancelled(true);
        }
    }

    // Trash-GUI: Confirm-Button löscht; Schließen ohne Klick gibt die Items zurück.
    @EventHandler(priority = EventPriority.HIGH)
    public void onTrashClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TrashMenuHolder holder)) return;
        int raw = event.getRawSlot();
        boolean clickedTop = raw < top.getSize();

        // Rucksäcke (auch in Shulker/Bundle) dürfen NIE in den Trash – sonst Verlust.
        if (event.getWhoClicked() instanceof Player player && trashWouldHoldBackpack(event, top, clickedTop)) {
            event.setCancelled(true);
            denyNesting(player);
            return;
        }

        if (!holder.confirm()) return; // Nicht-Confirm: freie Interaktion, Items verfallen beim Schließen.
        if (clickedTop && raw == TrashMenuHolder.CONFIRM_SLOT) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int n = manager.confirmTrash(holder);
            plugin.messages().send(player, "trash.deleted",
                    de.yourshika.backpacks.config.MessageManager.ph("count", String.valueOf(n)));
            player.closeInventory();
        }
    }

    /** true, wenn dieser Klick ein Backpack in das Trash-Oberinventar bewegen würde. */
    private boolean trashWouldHoldBackpack(InventoryClickEvent event, Inventory top, boolean clickedTop) {
        ClickType click = event.getClick();
        // Shift-Click aus dem Spieler-Inventar wandert in den Trash.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !clickedTop) {
            return cantStore(event.getCurrentItem());
        }
        if (!clickedTop) return false; // sonstige Klicks im Spieler-Inventar sind ok.
        if (click == ClickType.NUMBER_KEY) {
            return cantStore(event.getView().getBottomInventory().getItem(event.getHotbarButton()));
        }
        if (click == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player p) {
            return cantStore(p.getInventory().getItemInOffHand());
        }
        return cantStore(event.getCursor()); // Cursor-Platzierung in den Trash.
    }

    private boolean cantStore(ItemStack item) {
        return item != null && !item.getType().isAir() && items.isOrContainsBackpack(item);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTrashDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TrashMenuHolder holder)) return;
        boolean draggingBackpack = items.isOrContainsBackpack(event.getOldCursor());
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) continue;
            if (draggingBackpack || (holder.confirm() && raw == TrashMenuHolder.CONFIRM_SLOT)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTrashClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TrashMenuHolder holder
                && event.getPlayer() instanceof Player player) {
            manager.returnTrash(holder, player);
        }
    }

    // XP-Storage-GUI: reine Button-GUI (ein-/auszahlen).
    @EventHandler(priority = EventPriority.HIGH)
    public void onXpClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof XpMenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof XpMenuHolder)) {
            return; // Klick im Spieler-Inventar – ignorieren.
        }
        int raw = event.getRawSlot();
        if (raw == XpMenuHolder.BACK_SLOT) {
            UUID id = holder.backpackId();
            plugin.getServer().getScheduler().runTask(plugin, () -> manager.openById(player, id));
            return;
        }
        String action = switch (raw) {
            case XpMenuHolder.DEPOSIT_LEVEL -> "deposit_level";
            case XpMenuHolder.DEPOSIT_ALL -> "deposit_all";
            case XpMenuHolder.WITHDRAW_LEVEL -> "withdraw_level";
            case XpMenuHolder.WITHDRAW_ALL -> "withdraw_all";
            default -> null;
        };
        if (action == null) return;
        boolean changed = manager.xpAction(player, holder.backpackId(), action);
        manager.renderXp(holder, player);
        if (changed) {
            de.yourshika.backpacks.util.Sounds.play(plugin, player, "upgrade");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onXpDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof XpMenuHolder) {
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
        var topHolder = top.getHolder();
        int raw = event.getRawSlot();

        if (topHolder instanceof BackpackMenuHolder holder) {
            if (raw < top.getSize() && holder.isLocked(raw)) {
                event.setCancelled(true);
            }
            if (raw < top.getSize() && !canStoreBackpack(holder, event.getCursor())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) denyNesting(player);
            }
            return;
        }

        // Alle übrigen Plugin-GUIs (Upgrade, Furnace, Filter, Trash, XP, ...) haben
        // ebenfalls keine Creative-Absicherung: ein Creative-Set könnte ein Backpack
        // in einen Upgrade-/Furnace-Slot oder in den Trash schieben und damit die
        // Nesting-/Verlust-Schutzmechanismen umgehen (B3). Kein Backpack darf per
        // Creative in eine dieser GUIs gelangen.
        if (raw < top.getSize() && isPluginGuiHolder(topHolder)
                && plugin.itemFactory().isOrContainsBackpack(event.getCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) denyNesting(player);
        }
    }

    /** true, wenn der Holder eine der Plugin-eigenen GUIs ist. */
    private boolean isPluginGuiHolder(org.bukkit.inventory.InventoryHolder holder) {
        return holder != null
                && holder.getClass().getName().startsWith("de.yourshika.backpacks.gui.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof BackpackMenuHolder holder) {
            Player closer = event.getPlayer() instanceof Player p ? p : null;
            manager.saveAndRelease(holder, closer);
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
        if (clickedTop && holder.isUpgradeSlot(raw)) {
            ItemStack cursor = event.getCursor();
            if (rejectUpgrade(player, up, top, holder, cursor)) {
                event.setCancelled(true);
                return;
            }
            if (cursor != null && !cursor.getType().isAir()) {
                de.yourshika.backpacks.util.Sounds.play(plugin, player, "upgrade");
            }
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
        boolean badItem = onlyFunction(up, dragged) || furnaceConflict(up, top, holder, dragged)
                || stationOverflow(up, top, holder, dragged);
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
            if (event.getPlayer() instanceof Player player && plugin.achievements() != null) {
                grantUpgradeAchievements(player, holder);
            }
        }
    }

    /** Vergibt Achievements rund um eingebaute Upgrades beim Schließen der Upgrade-GUI. */
    private void grantUpgradeAchievements(Player player, UpgradeMenuHolder holder) {
        var ach = plugin.achievements();
        java.util.Set<String> fns = manager.functionUpgradesOf(holder.backpackId());
        if (fns.isEmpty()) return;
        ach.trigger(player, "upgrade");
        if (fns.contains("everlasting")) ach.trigger(player, "everlasting");
        int stations = 0;
        boolean advanced = false;
        for (String fn : fns) {
            var def = de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(fn);
            if (def != null && def.isStation()) stations++;
            if (fn.startsWith("advanced_")) advanced = true;
        }
        if (advanced) ach.trigger(player, "advanced");
        if (stations >= 5) ach.trigger(player, "stations");
        if (fns.size() >= holder.upgradeSlots() && holder.upgradeSlots() > 0) {
            ach.trigger(player, "full_upgrades");
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

        // Presets / Filter leeren (#20). Presets gibt es nur beim Compacting-Filter;
        // der Pickup-Filter ist NBT-genau, dort sind die Preset-Slots nur Deko.
        if (clickedTop) {
            String preset = holder.presetAt(raw);
            if (preset != null && !holder.isPickup()) {
                event.setCancelled(true);
                manager.applyPreset(holder, preset);
                return;
            }
            if (holder.isClear(raw)) {
                event.setCancelled(true);
                manager.clearFilter(holder);
                return;
            }
        }

        if (!clickedTop) return; // Klicks im Spieler-Inventar normal lassen.
        event.setCancelled(true); // Filter sind Ghost-Slots – niemals echte Items bewegen.
        if (!holder.isFilterSlot(raw)) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            if (items.isBackpack(cursor)) { denyNesting(player); return; }
            // Pickup-Filter merkt sich das ganze Item (NBT-genau, für Custom-Items);
            // Compacting-Filter nur den Typ als Muster.
            ItemStack ghost;
            if (holder.isPickup()) {
                ghost = cursor.clone();
                ghost.setAmount(1);
            } else {
                ghost = new ItemStack(cursor.getType());
            }
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
        if (stationOverflow(up, top, holder, item)) {
            plugin.messages().send(player, "upgrades.too-many-stations",
                    de.yourshika.backpacks.config.MessageManager.ph("max",
                            String.valueOf(BackpackMenuHolder.MAX_STATIONS)));
            return true;
        }
        return false;
    }

    /**
     * true, wenn {@code item} ein Stations-Upgrade ist und der Rucksack damit mehr
     * Stationen hätte, als gleichzeitig angezeigt werden können
     * ({@link BackpackMenuHolder#MAX_STATIONS}). So beißen sich die Buttons nie um Platz.
     */
    private boolean stationOverflow(UpgradeItemFactory up, Inventory top,
                                    UpgradeMenuHolder holder, ItemStack item) {
        String fn = up.getFunctionType(item);
        var def = fn == null ? null : de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(fn);
        if (def == null || !def.isStation()) return false;
        int stations = 0;
        for (int i = 0; i < holder.upgradeSlots(); i++) {
            String present = up.getFunctionType(top.getItem(i));
            var p = present == null ? null : de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(present);
            if (p != null && p.isStation()) stations++;
        }
        return stations >= BackpackMenuHolder.MAX_STATIONS;
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
        de.yourshika.backpacks.util.Sounds.play(plugin, player, "error");
    }

    private boolean canStoreBackpack(BackpackMenuHolder holder, ItemStack item) {
        if (item == null) return true;
        if (items.isBackpack(item)) {
            return manager.canStoreBackpackInside(holder.backpackId(), item);
        }
        // Deep-Nesting (#51): Backpack versteckt in Shulker/Bundle -> immer blocken.
        return !items.isOrContainsBackpack(item);
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
