package de.yourshika.backpacks;

import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.gui.UpgradeMenuHolder;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.storage.BackpackStorage;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import de.yourshika.backpacks.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentraler Dienst für das Öffnen, Befüllen und Speichern von Backpacks.
 *
 * <p>Jedes Backpack öffnet als Doppeltruhe (54 Slots). Nur die für den Tier
 * freigegebenen Lager-Slots sind nutzbar; größere Tiers werden über mehrere
 * Seiten geblättert. Es ist stets nur ein Betrachter pro Backpack erlaubt, um
 * Dupe-Risiken auszuschließen.</p>
 */
public final class BackpackManager {

    private final YourShikaBackpacks plugin;
    private final BackpackStorage storage;
    private final BackpackItemFactory items;
    private final TierRegistry tiers;
    private final MiniMessage mini = MiniMessage.miniMessage();

    /** backpackId -> Viewer-UUID (genau ein Betrachter pro Backpack). */
    private final Map<UUID, UUID> openBackpacks = new ConcurrentHashMap<>();

    /** Cache: backpackId -> Set installierter Funktions-Upgrade-IDs. */
    private final Map<UUID, java.util.Set<String>> functionCache = new ConcurrentHashMap<>();

    public BackpackManager(YourShikaBackpacks plugin, BackpackStorage storage,
                           BackpackItemFactory items, TierRegistry tiers) {
        this.plugin = plugin;
        this.storage = storage;
        this.items = items;
        this.tiers = tiers;
    }

    public BackpackStorage storage() { return storage; }
    public BackpackItemFactory items() { return items; }

    public boolean isOpen(UUID backpackId) {
        return openBackpacks.containsKey(backpackId);
    }

    /**
     * Darf der Spieler ein Backpack mit dem gegebenen Besitzer öffnen/aufheben?
     * Bei {@code owner-only: false} (Standard) immer erlaubt. Ohne hinterlegten
     * Besitzer ebenfalls erlaubt. Admins mit {@code openother} dürfen stets.
     */
    public boolean canAccess(Player player, UUID owner) {
        if (!plugin.pluginConfig().ownerOnly()) return true;
        if (owner == null) return true;
        if (owner.equals(player.getUniqueId())) return true;
        return player.hasPermission("yourshika.backpack.admin.openother");
    }

    /**
     * Öffnet das Backpack-Item für einen Spieler. Gibt einen Fehlermeldung-Key
     * zurück, falls etwas schiefgeht, sonst null bei Erfolg.
     */
    public String openFromItem(Player player, ItemStack item) {
        String tierKey = items.getTierKey(item);
        BackpackTier tier = tiers.get(tierKey);
        if (tier == null) {
            return "error.invalid-backpack";
        }

        if (!canAccess(player, items.getOwner(item))) {
            return "error.not-owner";
        }

        // Lazy-ID: gecraftete/gespawnte Backpacks erhalten beim ersten Öffnen ihre ID.
        UUID id = items.getId(item);
        boolean fresh = false;
        if (id == null) {
            id = UUID.randomUUID();
            items.writeId(item, id);
            fresh = true;
        }

        if (openBackpacks.containsKey(id)) {
            return "error.already-open";
        }

        String main = items.getMainColor(item, tier.defaultMainColor());
        String accent = items.getAccentColor(item, tier.defaultAccentColor());

        // Item bei jedem Öffnen auf den aktuellen Stand bringen (Name/Lore/Modell),
        // ohne ID, Farbe, Besitzer oder Inhalt zu verändern. So aktualisieren sich
        // bereits existierende Backpacks automatisch nach Updates.
        items.refresh(item, tier);

        BackpackData data = storage.load(id);
        if (data == null) {
            data = new BackpackData(id);
            data.owner(player.getUniqueId());
            data.tier(tier.key());
            data.mainColor(main);
            data.accentColor(accent);
            data.contents(new ItemStack[tier.storageSlots()]);
            storage.save(data);
        } else if (!tier.key().equalsIgnoreCase(data.tier())) {
            // Selbstheilung nach Smithing-Veredelung: Tier im Item ist maßgeblich,
            // der Inhalt bleibt über die ID erhalten.
            data.tier(tier.key());
            storage.save(data);
        } else if (fresh) {
            plugin.debug("Frisches Item traf bestehende Daten: " + id);
        }

        openInventory(player, tier, data, main, accent);
        return null;
    }

    /** Öffnet ein Backpack direkt anhand seiner ID (z.B. Admin-Befehl). */
    public String openById(Player player, UUID id) {
        BackpackData data = storage.load(id);
        if (data == null) {
            return "error.id-not-found";
        }
        BackpackTier tier = tiers.get(data.tier());
        if (tier == null) {
            return "error.invalid-backpack";
        }
        if (!canAccess(player, data.owner())) {
            return "error.not-owner";
        }
        if (openBackpacks.containsKey(id)) {
            return "error.already-open";
        }
        String main = data.mainColor() != null ? data.mainColor() : tier.defaultMainColor();
        String accent = data.accentColor() != null ? data.accentColor() : tier.defaultAccentColor();
        openInventory(player, tier, data, main, accent);
        return null;
    }

    private void openInventory(Player player, BackpackTier tier, BackpackData data,
                               String main, String accent) {
        int slotsPerPage = plugin.pluginConfig().storageSlotsPerPage();
        int capacity = tier.storageSlots();
        ItemStack[] buffer = bufferFor(data.contents(), capacity);

        BackpackMenuHolder holder = new BackpackMenuHolder(
                data.id(), tier.key(), capacity, slotsPerPage, buffer, main, accent);

        Inventory inv = Bukkit.createInventory(holder, BackpackMenuHolder.INVENTORY_SIZE,
                title(tier, data, main, accent, holder));
        holder.setInventory(inv);

        renderPage(holder);

        openBackpacks.put(data.id(), player.getUniqueId());
        player.openInventory(inv);
        de.yourshika.backpacks.util.Sounds.play(plugin, player, "open");
    }

    /**
     * Sortiert den gesamten Backpack-Inhalt dupe-sicher: führt gleiche Items zu
     * vollen Stacks zusammen und sortiert nach Material. Operiert nur auf dem
     * Buffer (gleiche Item-Gesamtmenge), danach Neu-Rendern.
     */
    public void sortBackpack(BackpackMenuHolder holder, Player player) {
        flushVisiblePage(holder);
        ItemStack[] buf = holder.buffer();
        int cap = holder.capacity();
        java.util.List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < cap && i < buf.length; i++) {
            ItemStack it = buf[i];
            if (it != null && !it.getType().isAir()) items.add(it.clone());
        }
        // Gleiche Items zu maximalen Stacks zusammenführen.
        java.util.List<ItemStack> merged = new ArrayList<>();
        for (ItemStack it : items) {
            boolean placed = false;
            for (ItemStack m : merged) {
                if (m.isSimilar(it)) {
                    int space = m.getMaxStackSize() - m.getAmount();
                    if (space > 0) {
                        int add = Math.min(space, it.getAmount());
                        m.setAmount(m.getAmount() + add);
                        it.setAmount(it.getAmount() - add);
                    }
                }
                if (it.getAmount() <= 0) { placed = true; break; }
            }
            if (!placed && it.getAmount() > 0) merged.add(it);
        }
        // Nach Material-Name sortieren (stabil, lesbar).
        merged.sort(java.util.Comparator
                .comparing((ItemStack it) -> it.getType().getKey().toString())
                .thenComparing(it -> -it.getAmount()));
        // Zurück in den Buffer schreiben.
        for (int i = 0; i < cap && i < buf.length; i++) {
            buf[i] = i < merged.size() ? merged.get(i) : null;
        }
        renderPage(holder);
        de.yourshika.backpacks.util.Sounds.play(plugin, player, "sort");
    }

    /** Baut einen Buffer mindestens in Kapazitätsgröße auf (überzählige Items bleiben erhalten). */
    private ItemStack[] bufferFor(ItemStack[] contents, int capacity) {
        int len = Math.max(capacity, contents == null ? 0 : contents.length);
        ItemStack[] buffer = new ItemStack[len];
        if (contents != null) {
            System.arraycopy(contents, 0, buffer, 0, Math.min(contents.length, len));
        }
        return buffer;
    }

    /** Zeichnet die aktuelle Seite des Holders vollständig neu. */
    public void renderPage(BackpackMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        BackpackTier tier = tiers.get(holder.tierKey());

        int base = holder.pageBase();
        int active = holder.activeCount();
        ItemStack[] buffer = holder.buffer();

        // Lager-Bereich (Slots 0..44).
        for (int slot = 0; slot < BackpackMenuHolder.CONTROL_ROW_START; slot++) {
            if (slot < active) {
                int global = base + slot;
                inv.setItem(slot, global < buffer.length ? buffer[global] : null);
            } else {
                inv.setItem(slot, lockedFiller());
            }
        }

        fillControlRow(inv, tier, holder);

        // Titel mit Seitenangabe aktualisieren (sofern paging).
        // (Der Titel ist beim Erstellen gesetzt; eine Aktualisierung erfordert
        //  ein Neuöffnen – daher wird die Seite zusätzlich im Info-Item gezeigt.)
    }

    /** Reihenfolge, in der Stations-Buttons (falls verbaut) platziert werden. */
    private static final String[] STATION_ORDER = {
            "crafting", "stonecutter", "smithing", "ender_link",
            "smelting", "blasting", "smoking", "compacting", "trash"
    };

    private void fillControlRow(Inventory inv, BackpackTier tier, BackpackMenuHolder holder) {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = BackpackMenuHolder.CONTROL_ROW_START; slot < BackpackMenuHolder.INVENTORY_SIZE; slot++) {
            inv.setItem(slot, filler);
        }

        // Upgrades-Button (öffnet die separate Upgrade-GUI), wenn der Tier
        // Upgrade-Slots besitzt.
        if (tier != null && tier.upgradeSlots() > 0) {
            inv.setItem(BackpackMenuHolder.UPGRADE_BUTTON, upgradeButton(tier));
        }

        // Info-Item.
        inv.setItem(BackpackMenuHolder.INFO_SLOT, infoItem(tier, holder));

        // Blätter-Buttons.
        if (holder.hasPaging()) {
            if (holder.currentPage() > 0) {
                inv.setItem(BackpackMenuHolder.PREV_SLOT,
                        navButton(Material.ARROW, "<green>◀ Previous page", holder));
            }
            if (holder.currentPage() < holder.pageCount() - 1) {
                inv.setItem(BackpackMenuHolder.NEXT_SLOT,
                        navButton(Material.ARROW, "<green>Next page ▶", holder));
            }
        }

        // Stations-Buttons dynamisch auf freie Kandidaten-Slots verteilen.
        holder.clearStationSlots();
        java.util.Set<String> functions = functionUpgradesOf(holder.backpackId());
        int[] candidates = holder.stationCandidates();
        int idx = 0;
        for (String station : STATION_ORDER) {
            if (!functions.contains(station)) continue;
            if (idx >= candidates.length) break; // keine freien Slots mehr
            int slot = candidates[idx++];
            inv.setItem(slot, stationButton(station));
            holder.assignStation(slot, station);
        }
    }

    /** Wechselt die Seite eines geöffneten Backpacks (dupe-sicher). */
    public void changePage(BackpackMenuHolder holder, int delta) {
        if (!holder.hasPaging()) return;
        int target = holder.currentPage() + delta;
        if (target < 0 || target >= holder.pageCount()) return;
        flushVisiblePage(holder);
        holder.currentPage(target);
        renderPage(holder);
    }

    /** Springt direkt auf eine absolute Seite (z.B. erste/letzte – Shift-Klick). */
    public void goToPage(BackpackMenuHolder holder, int page) {
        if (!holder.hasPaging()) return;
        int target = Math.max(0, Math.min(holder.pageCount() - 1, page));
        if (target == holder.currentPage()) return;
        flushVisiblePage(holder);
        holder.currentPage(target);
        renderPage(holder);
    }

    /** Anzahl belegter Lager-Slots (über alle Seiten) für die Info-Anzeige. */
    private int usedSlots(BackpackMenuHolder holder) {
        ItemStack[] buf = holder.buffer();
        int cap = holder.capacity();
        int used = 0;
        for (int i = 0; i < cap && i < buf.length; i++) {
            ItemStack it = buf[i];
            if (it != null && !it.getType().isAir()) used++;
        }
        return used;
    }

    /** Kopiert die aktuell sichtbare Seite zurück in den Buffer. */
    public void flushVisiblePage(BackpackMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        int base = holder.pageBase();
        int active = holder.activeCount();
        ItemStack[] buffer = holder.buffer();
        for (int slot = 0; slot < active; slot++) {
            int global = base + slot;
            if (global < buffer.length) {
                buffer[global] = inv.getItem(slot);
            }
        }
    }

    private Component title(BackpackTier tier, BackpackData data, String main, String accent,
                            BackpackMenuHolder holder) {
        TextColor mainColor = ColorUtil.toTextColor(main, TextColor.color(0xFFFFFF));
        TextColor accentColor = ColorUtil.toTextColor(accent, TextColor.color(0xFFFFFF));
        String pageSuffix = holder.hasPaging()
                ? " <dark_gray>(Page " + (holder.currentPage() + 1) + "/" + holder.pageCount() + ")" : "";
        return mini.deserialize(tier.displayName() + pageSuffix, TagResolver.resolver(
                Placeholder.component("main_color", Component.text(ColorUtil.pretty(main)).color(mainColor)),
                Placeholder.component("accent_color", Component.text(ColorUtil.pretty(accent)).color(accentColor)),
                Placeholder.unparsed("storage", String.valueOf(tier.storageSlots())),
                Placeholder.unparsed("pages", String.valueOf(holder.pageCount())),
                Placeholder.unparsed("upgrades", String.valueOf(tier.upgradeSlots())),
                Placeholder.unparsed("id", data.id().toString().substring(0, 8))
        )).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack infoItem(BackpackTier tier, BackpackMenuHolder holder) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gold><bold>Backpack Info</bold></gold>"));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<dark_gray><st>                    </st>"));
        if (tier != null) {
            lore.add(line("<gray>Tier: <white>" + capitalizeTier(tier.key())));
            lore.add(line("<gray>Storage: <white>" + tier.storageSlots() + " <gray>slots"));
            lore.add(line("<gray>Upgrade slots: <white>" + tier.upgradeSlots()));
        }
        lore.add(line("<gray>ID: <white>" + holder.backpackId().toString().substring(0, 8)));
        lore.add(line("<gray>Color: " + ColorUtil.pretty(holder.mainColor())
                + " <dark_gray>/</dark_gray> " + ColorUtil.pretty(holder.accentColor())));
        int used = usedSlots(holder);
        int free = Math.max(0, holder.capacity() - used);
        lore.add(line("<gray>Used: <white>" + used + "<gray>/<white>" + holder.capacity()
                + " <dark_gray>•</dark_gray> <green>" + free + " free"));
        if (holder.hasPaging()) {
            lore.add(line("<gray>Page: <white>" + (holder.currentPage() + 1) + "/" + holder.pageCount()));
        }
        java.util.Set<String> fns = functionUpgradesOf(holder.backpackId());
        if (!fns.isEmpty()) {
            lore.add(Component.empty());
            lore.add(line("<gold>Upgrades installed: <white>" + fns.size()));
            for (String fn : fns) {
                var fu = de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(fn);
                String label = fu != null ? fu.displayName() : fn;
                lore.add(line("<dark_gray> • </dark_gray>")
                        .append(mini.deserialize(label).decoration(TextDecoration.ITALIC, false)));
            }
        }
        lore.add(Component.empty());
        lore.add(line("<yellow>Click <gray>to sort the contents"));
        lore.add(line("<dark_gray><st>                    </st>"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String capitalizeTier(String key) {
        return key == null || key.isEmpty() ? "?"
                : Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    private ItemStack stationButton(String station) {
        Material material;
        String name;
        String desc;
        switch (station) {
            case "crafting" -> { material = Material.CRAFTING_TABLE; name = "<#7FD7FF><bold>Crafting</bold>"; desc = "Open a crafting grid."; }
            case "stonecutter" -> { material = Material.STONECUTTER; name = "<#C0C0C0><bold>Stonecutter</bold>"; desc = "Open a stonecutter."; }
            case "smithing" -> { material = Material.SMITHING_TABLE; name = "<#8AB4F8><bold>Smithing Table</bold>"; desc = "Open a smithing table."; }
            case "ender_link" -> { material = Material.ENDER_CHEST; name = "<#A66BFF><bold>Ender Chest</bold>"; desc = "Open your ender chest."; }
            case "smelting" -> { material = Material.FURNACE; name = "<#FF8C42><bold>Portable Furnace</bold>"; desc = "Smelt items with fuel."; }
            case "blasting" -> { material = Material.BLAST_FURNACE; name = "<#FFB36B><bold>Portable Blast Furnace</bold>"; desc = "Smelt ores & metals fast."; }
            case "smoking" -> { material = Material.SMOKER; name = "<#FFD27F><bold>Portable Smoker</bold>"; desc = "Cook food fast."; }
            case "compacting" -> { material = Material.PISTON; name = "<#D2B48C><bold>Compacting Filter</bold>"; desc = "Choose what compacts on close."; }
            case "trash" -> { material = Material.LAVA_BUCKET; name = "<#8B8B8B><bold>Trash</bold>"; desc = "Delete unwanted items."; }
            default -> { material = Material.BARRIER; name = "<gray>Station"; desc = ""; }
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name));
        List<Component> lore = new ArrayList<>();
        if (!desc.isEmpty()) lore.add(line("<gray>" + desc));
        lore.add(Component.empty());
        lore.add(line("<yellow>▶ Click to open"));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** Öffnet die Vanilla-Station (Crafting/Stonecutter/Smithing) für ein Stations-Upgrade. */
    public void openStation(Player player, String station, UUID backpackId, String tierKey) {
        try {
            switch (station) {
                case "crafting" -> player.openWorkbench(null, true);
                case "stonecutter" -> player.openInventory(
                        org.bukkit.inventory.MenuType.STONECUTTER.create(player,
                                net.kyori.adventure.text.Component.text("Stonecutter")));
                case "smithing" -> player.openInventory(
                        org.bukkit.inventory.MenuType.SMITHING.create(player,
                                net.kyori.adventure.text.Component.text("Smithing Table")));
                case "ender_link" -> player.openInventory(player.getEnderChest());
                case "smelting" -> openFurnace(player, "furnace", backpackId, tierKey);
                case "blasting" -> openFurnace(player, "blast", backpackId, tierKey);
                case "smoking" -> openFurnace(player, "smoker", backpackId, tierKey);
                case "compacting" -> openFilter(player, backpackId, tierKey);
                case "trash" -> openTrash(player);
                default -> { }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Station '" + station + "' konnte nicht geöffnet werden: " + t.getMessage());
        }
    }

    /** Öffnet die Trash-GUI (mit optionalem Lösch-Bestätigen-Button). */
    public void openTrash(Player player) {
        boolean confirm = plugin.getConfig().getBoolean("trash.confirm", true);
        var holder = new de.yourshika.backpacks.gui.TrashMenuHolder(confirm);
        Inventory trash = Bukkit.createInventory(holder, de.yourshika.backpacks.gui.TrashMenuHolder.SIZE,
                mini.deserialize("<dark_gray><bold>Trash</bold> <gray>"
                                + (confirm ? "(confirm to delete)" : "(items here are deleted)"))
                        .decoration(TextDecoration.ITALIC, false));
        holder.setInventory(trash);
        if (confirm) {
            trash.setItem(de.yourshika.backpacks.gui.TrashMenuHolder.CONFIRM_SLOT, trashConfirmButton());
        }
        player.openInventory(trash);
    }

    private ItemStack trashConfirmButton() {
        ItemStack item = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<red><bold>Delete items</bold></red>"));
        meta.lore(List.of(
                line("<gray>Click to permanently delete the items."),
                line("<dark_gray>Closing the menu returns them.")));
        item.setItemMeta(meta);
        return item;
    }

    /** Löscht die Items im Trash (Confirm-Button) und gibt deren Anzahl zurück. */
    public int confirmTrash(de.yourshika.backpacks.gui.TrashMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return 0;
        int n = 0;
        for (int i = 0; i < de.yourshika.backpacks.gui.TrashMenuHolder.ITEM_SLOTS; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && !it.getType().isAir()) {
                n += it.getAmount();
                inv.setItem(i, null);
            }
        }
        return n;
    }

    /** Gibt im Confirm-Modus verbliebene (nicht bestätigte) Trash-Items zurück. */
    public void returnTrash(de.yourshika.backpacks.gui.TrashMenuHolder holder, Player player) {
        if (!holder.confirm()) return; // Nicht-Confirm: Items werden verworfen (altes Verhalten).
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        for (int i = 0; i < de.yourshika.backpacks.gui.TrashMenuHolder.ITEM_SLOTS; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && !it.getType().isAir()) {
                player.getInventory().addItem(it).values()
                        .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
                inv.setItem(i, null);
            }
        }
    }

    private ItemStack upgradeButton(BackpackTier tier) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gradient:#6E5BC8:#5BE8D4><bold>Upgrades</bold></gradient>"));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<gray>Upgrade slots: <white>" + tier.upgradeSlots()));
        lore.add(Component.empty());
        lore.add(line("<yellow>▶ Click to open"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(Material material, String name, BackpackMenuHolder holder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name));
        meta.lore(List.of(
                line("<gray>Page <white>" + (holder.currentPage() + 1) + "<gray>/<white>" + holder.pageCount()),
                line("<dark_gray>Click: ±1 page"),
                line("<dark_gray>Shift-Click: first/last page")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack lockedFiller() {
        return pane(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
    }

    private ItemStack pane(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String mm) {
        return mini.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Speichert den Inhalt eines geöffneten Backpacks und gibt den
     * "geöffnet"-Status frei.
     */
    public void saveAndRelease(BackpackMenuHolder holder) {
        try {
            flushVisiblePage(holder);
            // Compacting-Upgrade: 9er-Stacks zu Blöcken verdichten (nach Filter).
            if (functionUpgradesOf(holder.backpackId()).contains("compacting")) {
                compact(holder.buffer(), compactWhitelist(holder.backpackId()));
            }
            BackpackData data = storage.load(holder.backpackId());
            if (data == null) {
                data = new BackpackData(holder.backpackId());
                data.tier(holder.tierKey());
            }
            data.contents(holder.buffer());
            storage.save(data);
        } catch (Exception ex) {
            plugin.getLogger().severe("Backpack " + holder.backpackId()
                    + " konnte beim Schließen nicht gespeichert werden: " + ex.getMessage());
        } finally {
            openBackpacks.remove(holder.backpackId());
        }
    }

    /** Items, die sich aus 9 Stück zu einem Block verdichten lassen. */
    private static final Map<Material, Material> COMPACT = java.util.Map.ofEntries(
            Map.entry(Material.IRON_INGOT, Material.IRON_BLOCK),
            Map.entry(Material.GOLD_INGOT, Material.GOLD_BLOCK),
            Map.entry(Material.DIAMOND, Material.DIAMOND_BLOCK),
            Map.entry(Material.EMERALD, Material.EMERALD_BLOCK),
            Map.entry(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK),
            Map.entry(Material.COPPER_INGOT, Material.COPPER_BLOCK),
            Map.entry(Material.COAL, Material.COAL_BLOCK),
            Map.entry(Material.REDSTONE, Material.REDSTONE_BLOCK),
            Map.entry(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK),
            Map.entry(Material.RAW_IRON, Material.RAW_IRON_BLOCK),
            Map.entry(Material.RAW_GOLD, Material.RAW_GOLD_BLOCK),
            Map.entry(Material.RAW_COPPER, Material.RAW_COPPER_BLOCK),
            Map.entry(Material.SLIME_BALL, Material.SLIME_BLOCK),
            Map.entry(Material.WHEAT, Material.HAY_BLOCK),
            Map.entry(Material.DRIED_KELP, Material.DRIED_KELP_BLOCK),
            Map.entry(Material.BONE_MEAL, Material.BONE_BLOCK),
            Map.entry(Material.NETHER_WART, Material.NETHER_WART_BLOCK));

    // ---- Compacting-Filter -----------------------------------------------

    /** Öffnet die Compacting-Filter-GUI eines Backpacks. */
    public void openFilter(Player player, UUID backpackId, String tierKey) {
        de.yourshika.backpacks.gui.FilterMenuHolder holder =
                new de.yourshika.backpacks.gui.FilterMenuHolder(backpackId, tierKey);
        Inventory inv = Bukkit.createInventory(holder,
                de.yourshika.backpacks.gui.FilterMenuHolder.SIZE,
                mini.deserialize("<#D2B48C><bold>Compacting Filter</bold>").decoration(TextDecoration.ITALIC, false));
        holder.setInventory(inv);

        BackpackData data = storage.load(backpackId);
        ItemStack[] filter = data == null ? null : data.compactFilter();
        if (filter != null) {
            for (int i = 0; i < de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS && i < filter.length; i++) {
                inv.setItem(i, filter[i]);
            }
        }
        ItemStack pane = pane(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS; i < holder.getInventory().getSize(); i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(de.yourshika.backpacks.gui.FilterMenuHolder.INFO_SLOT, filterInfo());
        inv.setItem(de.yourshika.backpacks.gui.FilterMenuHolder.BACK_SLOT, backButton());

        // Presets (#20) + Leeren-Button.
        int[] ps = de.yourshika.backpacks.gui.FilterMenuHolder.PRESET_SLOTS;
        inv.setItem(ps[0], presetButton(Material.IRON_INGOT, "Ores preset", "Iron, gold, copper, diamond, …"));
        inv.setItem(ps[1], presetButton(Material.WHEAT, "Farm preset", "Wheat, kelp, nether wart, …"));
        inv.setItem(ps[2], presetButton(Material.REDSTONE, "Redstone preset", "Redstone & lapis"));
        inv.setItem(ps[3], presetButton(Material.NETHERITE_INGOT, "Misc preset", "All compactable items"));
        inv.setItem(de.yourshika.backpacks.gui.FilterMenuHolder.CLEAR_SLOT,
                presetButton(Material.BARRIER, "Clear filter", "Remove all filter entries"));

        player.openInventory(inv);
        // Preview (#19): zeigt im Chat, was beim Schließen verdichtet würde.
        compactPreview(player, backpackId);
    }

    private ItemStack presetButton(Material material, String name, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<yellow><bold>" + name + "</bold></yellow>"));
        meta.lore(List.of(line("<gray>" + desc), line("<dark_gray>Click to apply")));
        item.setItemMeta(meta);
        return item;
    }

    /** Compacting-Presets: Whitelist-Vorlagen (#20). */
    private static final Map<String, List<Material>> COMPACT_PRESETS = Map.of(
            "ores", List.of(Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.DIAMOND,
                    Material.EMERALD, Material.NETHERITE_INGOT, Material.RAW_IRON, Material.RAW_GOLD,
                    Material.RAW_COPPER, Material.COAL),
            "farm", List.of(Material.WHEAT, Material.DRIED_KELP, Material.NETHER_WART, Material.BONE_MEAL,
                    Material.SLIME_BALL),
            "redstone", List.of(Material.REDSTONE, Material.LAPIS_LAZULI),
            "misc", new ArrayList<>(COMPACT.keySet()));

    /** Füllt die Filter-Slots mit einem Preset. */
    public void applyPreset(de.yourshika.backpacks.gui.FilterMenuHolder holder, String presetId) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        List<Material> mats = COMPACT_PRESETS.get(presetId);
        if (mats == null) return;
        for (int i = 0; i < de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS; i++) inv.setItem(i, null);
        int slot = 0;
        for (Material m : mats) {
            if (slot >= de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS) break;
            inv.setItem(slot++, new ItemStack(m));
        }
    }

    public void clearFilter(de.yourshika.backpacks.gui.FilterMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        for (int i = 0; i < de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS; i++) inv.setItem(i, null);
    }

    /** Zeigt im Chat, welche Items beim Schließen verdichtet würden (#19). */
    public void compactPreview(Player player, UUID backpackId) {
        BackpackData data = storage.load(backpackId);
        if (data == null || data.contents() == null) return;
        java.util.Set<Material> whitelist = compactWhitelist(backpackId);
        java.util.Map<Material, Integer> totals = new java.util.LinkedHashMap<>();
        for (ItemStack it : data.contents()) {
            if (it == null) continue;
            Material block = COMPACT.get(it.getType());
            if (block == null || block == it.getType()) continue;
            if (!whitelist.isEmpty() && !whitelist.contains(it.getType())) continue;
            if (!it.isSimilar(new ItemStack(it.getType()))) continue;
            totals.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (var e : totals.entrySet()) {
            int blocks = e.getValue() / 9;
            if (blocks > 0) parts.add(blocks + "x " + prettyMaterial(COMPACT.get(e.getKey())));
        }
        if (parts.isEmpty()) {
            plugin.messages().send(player, "compact.preview-none");
        } else {
            plugin.messages().send(player, "compact.preview",
                    de.yourshika.backpacks.config.MessageManager.ph("items", String.join(", ", parts)));
        }
    }

    private String prettyMaterial(Material m) {
        String n = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private ItemStack filterInfo() {
        ItemStack item = new ItemStack(Material.PISTON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<#D2B48C><bold>Compacting Filter</bold>"));
        meta.lore(List.of(
                line("<dark_gray><st>                    </st>"),
                line("<gray>Place sample items here to choose"),
                line("<gray>which items get <white>compacted</white> on close."),
                Component.empty(),
                line("<dark_gray>Empty filter = compact everything."),
                line("<dark_gray><st>                    </st>")
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Speichert die Filter-Whitelist eines Backpacks. */
    public void saveFilter(de.yourshika.backpacks.gui.FilterMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        BackpackData data = storage.load(holder.backpackId());
        if (data == null) {
            data = new BackpackData(holder.backpackId());
            data.tier(holder.tierKey());
        }
        ItemStack[] filter = new ItemStack[de.yourshika.backpacks.gui.FilterMenuHolder.FILTER_SLOTS];
        for (int i = 0; i < filter.length; i++) filter[i] = inv.getItem(i);
        data.compactFilter(filter);
        storage.save(data);
    }

    /** Liefert die Whitelist-Materialien eines Backpacks (leer = alles erlaubt). */
    private java.util.Set<Material> compactWhitelist(UUID backpackId) {
        BackpackData data = storage.load(backpackId);
        java.util.Set<Material> set = new java.util.HashSet<>();
        if (data != null && data.compactFilter() != null) {
            for (ItemStack it : data.compactFilter()) {
                if (it != null && !it.getType().isAir()) set.add(it.getType());
            }
        }
        return set;
    }

    /** Verdichtet 9er-Mengen verdichtbarer Items im Buffer zu Blöcken. */
    private void compact(ItemStack[] buffer, java.util.Set<Material> whitelist) {
        java.util.Map<Material, Integer> totals = new java.util.HashMap<>();
        for (ItemStack it : buffer) {
            if (it == null) continue;
            Material block = COMPACT.get(it.getType());
            if (block == null || block == it.getType()) continue;          // nicht verdichtbar
            if (!whitelist.isEmpty() && !whitelist.contains(it.getType())) continue; // nicht in der Whitelist
            if (!it.isSimilar(new ItemStack(it.getType()))) continue;      // nur "plain" Items
            totals.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        for (var e : totals.entrySet()) {
            Material mat = e.getKey();
            int total = e.getValue();
            int blocks = total / 9;
            if (blocks == 0) continue;
            int remainder = total % 9;

            ItemStack[] trial = cloneBuffer(buffer);
            for (int i = 0; i < trial.length; i++) {
                ItemStack it = trial[i];
                if (it != null && it.getType() == mat && it.isSimilar(new ItemStack(mat))) {
                    trial[i] = null;
                }
            }

            ItemStack blockLeftover = addToBuffer(trial, new ItemStack(COMPACT.get(mat), blocks));
            ItemStack itemLeftover = remainder > 0 ? addToBuffer(trial, new ItemStack(mat, remainder)) : null;
            if (blockLeftover == null && itemLeftover == null) {
                copyBuffer(trial, buffer);
            }
        }
    }

    /** Legt einen Stack in freie/passende Buffer-Slots (für Compacting). */
    private ItemStack addToBuffer(ItemStack[] buffer, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemStack moving = stack.clone();
        int max = moving.getMaxStackSize();
        for (int i = 0; i < buffer.length && moving.getAmount() > 0; i++) {
            ItemStack slot = buffer[i];
            if (slot != null && slot.isSimilar(moving)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                if (space > 0) {
                    int add = Math.min(space, moving.getAmount());
                    slot.setAmount(slot.getAmount() + add);
                    moving.setAmount(moving.getAmount() - add);
                }
            }
        }
        for (int i = 0; i < buffer.length && moving.getAmount() > 0; i++) {
            if (buffer[i] == null || buffer[i].getType().isAir()) {
                int add = Math.min(max, moving.getAmount());
                ItemStack copy = moving.clone();
                copy.setAmount(add);
                buffer[i] = copy;
                moving.setAmount(moving.getAmount() - add);
            }
        }
        return moving.getAmount() > 0 ? moving : null;
    }

    private ItemStack[] cloneBuffer(ItemStack[] buffer) {
        ItemStack[] copy = new ItemStack[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            copy[i] = buffer[i] == null ? null : buffer[i].clone();
        }
        return copy;
    }

    private void copyBuffer(ItemStack[] source, ItemStack[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    // Schmelz-Rezeptkarten (lazy aus den Server-Rezepten gebaut).
    private Map<Material, ItemStack> furnaceMap, blastMap, smokerMap;

    private void ensureSmeltMaps() {
        if (furnaceMap != null) return;
        furnaceMap = new java.util.HashMap<>();
        blastMap = new java.util.HashMap<>();
        smokerMap = new java.util.HashMap<>();
        java.util.Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            org.bukkit.inventory.Recipe r = it.next();
            if (r instanceof org.bukkit.inventory.BlastingRecipe b) putCook(blastMap, b);
            else if (r instanceof org.bukkit.inventory.SmokingRecipe s) putCook(smokerMap, s);
            else if (r instanceof org.bukkit.inventory.FurnaceRecipe f) putCook(furnaceMap, f);
        }
    }

    private void putCook(Map<Material, ItemStack> map, org.bukkit.inventory.CookingRecipe<?> r) {
        if (r.getInputChoice() instanceof org.bukkit.inventory.RecipeChoice.MaterialChoice mc) {
            for (Material m : mc.getChoices()) map.putIfAbsent(m, r.getResult());
        }
    }

    public Map<Material, ItemStack> smeltMap(String type) {
        ensureSmeltMaps();
        return switch (type) {
            case "blast" -> blastMap;
            case "smoker" -> smokerMap;
            default -> furnaceMap;
        };
    }

    /** Ist das Item ein gültiger Brennstoff (für die Furnace-GUI)? */
    public boolean isFuelItem(ItemStack item) {
        return item != null && !item.getType().isAir() && fuelValue(item.getType()) > 0;
    }

    /** Lässt sich das Item in der Station vom Typ {@code type} schmelzen? */
    public boolean isSmeltable(String type, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.isSimilar(new ItemStack(item.getType()))) return false; // nur "plain" Items
        return smeltMap(type).containsKey(item.getType());
    }

    /** Wie viele Items eine Einheit dieses Brennstoffs schmilzt (0 = kein Brennstoff). */
    private static int fuelValue(Material m) {
        if (m == null) return 0;
        return switch (m) {
            case COAL, CHARCOAL -> 8;
            case COAL_BLOCK -> 72;
            case BLAZE_ROD -> 12;
            case DRIED_KELP_BLOCK -> 20;
            case LAVA_BUCKET -> 100;
            case BLAST_FURNACE, FURNACE -> 0;
            default -> {
                String n = m.name();
                if (n.endsWith("_PLANKS") || n.endsWith("_LOG") || n.endsWith("_WOOD")
                        || n.endsWith("_SLAB") || n.endsWith("_STAIRS") || n.endsWith("_SAPLING")) yield 1;
                if (m == Material.STICK || m == Material.BAMBOO) yield 1;
                yield 0;
            }
        };
    }

    // ---- Portable Furnace (Smelting / Blasting / Smoking Upgrade) ---------

    /** Koch-Schritte pro Item (Tick-Periode = 2 Ticks). Smoker/Blast = halbe Zeit. */
    private int cookSteps(String type) {
        return "furnace".equals(type) ? 20 : 10;
    }

    /**
     * Öffnet die portable Schmelz-Station eines Backpacks. {@code type} ist
     * "furnace", "blast" oder "smoker". Die GUI smelzt live, solange sie offen ist.
     */
    public void openFurnace(Player player, String type, UUID backpackId, String tierKey) {
        de.yourshika.backpacks.gui.FurnaceMenuHolder holder =
                new de.yourshika.backpacks.gui.FurnaceMenuHolder(backpackId, type, tierKey);
        String title = switch (type) {
            case "blast" -> "<#FFB36B><bold>Portable Blast Furnace</bold>";
            case "smoker" -> "<#FFD27F><bold>Portable Smoker</bold>";
            default -> "<#FF8C42><bold>Portable Furnace</bold>";
        };
        Inventory inv = Bukkit.createInventory(holder,
                de.yourshika.backpacks.gui.FurnaceMenuHolder.SIZE,
                mini.deserialize(title).decoration(TextDecoration.ITALIC, false));
        holder.setInventory(inv);

        // Persistenten Zustand des Backpacks in die GUI laden.
        BackpackData data = storage.load(backpackId);
        if (data != null) {
            ItemStack[] f = data.furnace();
            if (f != null) {
                if (f.length > 0) inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.INPUT_SLOT, f[0]);
                if (f.length > 1) inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.FUEL_SLOT, f[1]);
                if (f.length > 2) inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.OUTPUT_SLOT, f[2]);
            }
            holder.cook(data.furnaceCook());
            holder.burn(data.furnaceBurn());
        }

        renderFurnace(holder);
        player.openInventory(inv);

        // Live-Schmelz-Tick (alle 2 Ticks), solange die GUI offen ist.
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> furnaceTick(holder), 2L, 2L);
        holder.task(task);
    }

    /** Speichert den Furnace-Zustand (Slots + Fortschritt) in die Backpack-Daten. */
    public void saveFurnace(de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        BackpackData data = storage.load(holder.backpackId());
        if (data == null) {
            data = new BackpackData(holder.backpackId());
            data.tier(holder.tierKey());
        }
        ItemStack[] f = new ItemStack[3];
        f[0] = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.INPUT_SLOT);
        f[1] = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.FUEL_SLOT);
        f[2] = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.OUTPUT_SLOT);
        data.furnace(f);
        data.furnaceCook(holder.cook());
        data.furnaceBurn(holder.burn());
        storage.save(data);
    }

    /** Zeichnet die Deko-Slots (Pfeil/Info/Back) der Furnace-GUI neu. */
    public void renderFurnace(de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        ItemStack filler = pane(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < de.yourshika.backpacks.gui.FurnaceMenuHolder.SIZE; i++) {
            if (holder.isInteractable(i)) continue;
            inv.setItem(i, filler);
        }
        inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.ARROW_SLOT, arrowItem(holder));
        inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.INFO_SLOT, furnaceInfo(holder));
        inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.BACK_SLOT, backButton());
    }

    private ItemStack arrowItem(de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        int total = cookSteps(holder.type());
        int pct = total <= 0 ? 0 : (int) Math.round(holder.cook() * 100.0 / total);
        ItemStack item = new ItemStack(holder.burn() > 0 ? Material.BLAZE_POWDER : Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gray>Progress: <white>" + pct + "%"));
        meta.lore(List.of(holder.burn() > 0
                ? line("<#FF8C42>🔥 Burning")
                : line("<dark_gray>No fuel burning")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack furnaceInfo(de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        String kind = switch (holder.type()) {
            case "blast" -> "Blast Furnace";
            case "smoker" -> "Smoker";
            default -> "Furnace";
        };
        meta.displayName(line("<gold><bold>Portable " + kind + "</bold>"));
        meta.lore(List.of(
                line("<dark_gray><st>                    </st>"),
                line("<gray>Put items to cook in the <white>input</white> slot"),
                line("<gray>and fuel in the <white>fuel</white> slot."),
                line("<gray>Collect results from the <white>output</white>."),
                Component.empty(),
                line("<dark_gray>Everything stays in the backpack &"),
                line("<dark_gray>is saved on logout, crash or stop."),
                line("<dark_gray><st>                    </st>")
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Ein Schmelz-Schritt der portablen Station (läuft alle 2 Ticks). */
    private void furnaceTick(de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        Map<Material, ItemStack> recipes = smeltMap(holder.type());

        ItemStack input = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.INPUT_SLOT);
        ItemStack output = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.OUTPUT_SLOT);

        ItemStack result = (input != null && !input.getType().isAir()
                && input.isSimilar(new ItemStack(input.getType())))
                ? recipes.get(input.getType()) : null;

        boolean canOutput = result != null && (output == null || output.getType().isAir()
                || (output.isSimilar(result) && output.getAmount() + result.getAmount() <= output.getMaxStackSize()));

        if (result == null || !canOutput) {
            // Nichts zu schmelzen -> Fortschritt zurücksetzen (Brennstoff bleibt geladen).
            if (holder.cook() != 0) holder.cook(0);
            inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.ARROW_SLOT, arrowItem(holder));
            return;
        }

        // Brennstoff zünden, falls nötig.
        if (holder.burn() <= 0) {
            int lit = consumeFuel(inv);
            if (lit <= 0) {
                if (holder.cook() != 0) holder.cook(0);
                inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.ARROW_SLOT, arrowItem(holder));
                return;
            }
            holder.burn(lit);
        }

        holder.cook(holder.cook() + 1);
        if (holder.cook() >= cookSteps(holder.type())) {
            holder.cook(0);
            holder.burn(holder.burn() - 1);
            // Ergebnis ablegen.
            ItemStack out = result.clone();
            if (output == null || output.getType().isAir()) {
                inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.OUTPUT_SLOT, out);
            } else {
                output.setAmount(output.getAmount() + out.getAmount());
            }
            // Eingabe verringern.
            input.setAmount(input.getAmount() - 1);
            if (input.getAmount() <= 0) {
                inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.INPUT_SLOT, null);
            }
            saveFurnace(holder); // jeder fertige Schmelzvorgang ist ein Checkpoint
        }
        inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.ARROW_SLOT, arrowItem(holder));
        // Zusätzlich alle ~3s sichern, damit ein Crash mitten im Vorgang minimal kostet.
        if (holder.incTicks() % 30 == 0) saveFurnace(holder);
    }

    /** Verbraucht eine Brennstoff-Einheit aus dem Fuel-Slot, liefert dessen Schmelz-Wert. */
    private int consumeFuel(Inventory inv) {
        ItemStack fuel = inv.getItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.FUEL_SLOT);
        if (fuel == null || fuel.getType().isAir()) return 0;
        int value = fuelValue(fuel.getType());
        if (value <= 0) return 0;
        if (fuel.getType() == Material.LAVA_BUCKET) {
            inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.FUEL_SLOT, new ItemStack(Material.BUCKET));
        } else {
            fuel.setAmount(fuel.getAmount() - 1);
            if (fuel.getAmount() <= 0) {
                inv.setItem(de.yourshika.backpacks.gui.FurnaceMenuHolder.FUEL_SLOT, null);
            }
        }
        return value;
    }

    /**
     * Schließt die Furnace-GUI: stoppt den Tick und speichert den kompletten
     * Zustand persistent im Backpack. Die Items bleiben im Backpack (sie wandern
     * NICHT ins Spieler-Inventar) – so geht bei Logout/Crash/Stop nichts verloren.
     */
    public void closeFurnace(Player player, de.yourshika.backpacks.gui.FurnaceMenuHolder holder) {
        if (holder.task() != null) {
            holder.task().cancel();
            holder.task(null);
        }
        saveFurnace(holder);
    }

    /** Speichert alle aktuell offenen Backpacks (Autosave / Shutdown), ohne sie zu schließen. */
    public void saveAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() instanceof BackpackMenuHolder holder) {
                flushVisiblePage(holder);
                BackpackData data = storage.load(holder.backpackId());
                if (data == null) {
                    data = new BackpackData(holder.backpackId());
                    data.tier(holder.tierKey());
                }
                data.contents(holder.buffer());
                storage.save(data);
            }
        }
    }

    // ---- Upgrade-GUI -----------------------------------------------------

    /** Öffnet die separate Upgrade-GUI eines Backpacks. */
    public void openUpgrades(Player player, UUID backpackId, String tierKey) {
        BackpackTier tier = tiers.get(tierKey);
        if (tier == null || tier.upgradeSlots() <= 0) return;
        BackpackData data = storage.load(backpackId);
        if (data == null) {
            data = new BackpackData(backpackId);
            data.tier(tierKey);
        }
        ItemStack[] up = data.upgrades();
        ItemStack[] buffer = new ItemStack[tier.upgradeSlots()];
        if (up != null) {
            System.arraycopy(up, 0, buffer, 0, Math.min(up.length, buffer.length));
        }
        // Eingebaute Upgrade-Items auf den aktuellen kanonischen Stand bringen.
        for (int i = 0; i < buffer.length; i++) {
            ItemStack canonical = normalizeUpgrade(buffer[i]);
            if (canonical != null) {
                canonical.setAmount(Math.max(1, buffer[i].getAmount()));
                buffer[i] = canonical;
            }
        }
        UpgradeMenuHolder holder = new UpgradeMenuHolder(backpackId, tierKey, tier.upgradeSlots(), buffer);
        Inventory inv = Bukkit.createInventory(holder, holder.size(),
                mini.deserialize("<gradient:#6E5BC8:#5BE8D4><bold>Upgrades</bold></gradient> <dark_gray>("
                        + tier.key() + ")").decoration(TextDecoration.ITALIC, false));
        holder.setInventory(inv);
        renderUpgrades(holder);
        player.openInventory(inv);
    }

    private void renderUpgrades(UpgradeMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        ItemStack[] buffer = holder.buffer();
        for (int i = 0; i < holder.upgradeSlots(); i++) {
            inv.setItem(i, i < buffer.length ? buffer[i] : null);
        }
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = holder.upgradeSlots(); i < holder.size(); i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(holder.backButtonSlot(), backButton());
    }

    /** Speichert die Upgrade-Items eines Backpacks aus der Upgrade-GUI. */
    public void saveUpgrades(UpgradeMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        BackpackData data = storage.load(holder.backpackId());
        if (data == null) {
            data = new BackpackData(holder.backpackId());
            data.tier(holder.tierKey());
        }
        ItemStack[] up = new ItemStack[holder.upgradeSlots()];
        for (int i = 0; i < holder.upgradeSlots(); i++) {
            up[i] = inv.getItem(i);
        }
        data.upgrades(up);
        storage.save(data);
        functionCache.put(holder.backpackId(), computeFunctions(up));
    }

    // ---- Funktions-Upgrades ----------------------------------------------

    /** Liefert das aktuelle kanonische Upgrade-Item zu einem Upgrade-Item (oder null). */
    private ItemStack normalizeUpgrade(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        var factory = plugin.upgradeItems();
        if (factory.isUpgradeBase(item)) return plugin.upgradeManager().baseUpgradeItem();
        String tier = factory.getUpgradeTarget(item);
        if (tier != null) return plugin.upgradeManager().upgradeItem(tier);
        String fn = factory.getFunctionType(item);
        if (fn != null) return plugin.functionUpgrades().item(fn);
        return null;
    }

    private java.util.Set<String> computeFunctions(ItemStack[] upgrades) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (upgrades != null) {
            var factory = plugin.upgradeItems();
            for (ItemStack it : upgrades) {
                String fn = factory.getFunctionType(it);
                if (fn != null) set.add(fn);
            }
        }
        return set;
    }

    /** Installierte Funktions-Upgrade-IDs eines Backpacks (gecacht). */
    public java.util.Set<String> functionUpgradesOf(UUID backpackId) {
        java.util.Set<String> cached = functionCache.get(backpackId);
        if (cached != null) return cached;
        BackpackData data = storage.load(backpackId);
        java.util.Set<String> set = computeFunctions(data == null ? null : data.upgrades());
        functionCache.put(backpackId, set);
        return set;
    }

    /** Größter Magnet-Radius unter den eingebauten radius-basierten Upgrades (0 = keiner). */
    public int magnetRadius(Player player) {
        int best = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (!items.isBackpack(it)) continue;
            UUID id = items.getId(it);
            if (id == null) continue;
            for (String fn : functionUpgradesOf(id)) {
                var u = de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(fn);
                if (u != null && u.radius() > best) best = u.radius();
            }
        }
        return best;
    }

    /** Liefert eine Backpack-ID im Inventar des Spielers mit dem Upgrade (oder null). */
    public UUID findBackpackWithFunction(Player player, String functionId) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (!items.isBackpack(it)) continue;
            UUID id = items.getId(it);
            if (id == null) continue;
            if (functionUpgradesOf(id).contains(functionId)) return id;
        }
        return null;
    }

    public boolean depositItemWithFunction(Player player, ItemStack stack, java.util.Set<String> functionIds) {
        if (stack == null || stack.getType().isAir()) return false;
        boolean moved = false;
        for (ItemStack it : player.getInventory().getContents()) {
            if (stack.getAmount() <= 0) break;
            if (!items.isBackpack(it)) continue;
            UUID id = items.getId(it);
            if (id == null) continue;

            java.util.Set<String> functions = functionUpgradesOf(id);
            boolean matches = false;
            for (String functionId : functionIds) {
                if (functions.contains(functionId)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) continue;

            int before = stack.getAmount();
            if (depositItem(id, stack) && stack.getAmount() < before) {
                moved = true;
            }
        }
        return moved;
    }

    public boolean canStoreBackpackInside(UUID targetBackpackId, ItemStack candidate) {
        if (!items.isBackpack(candidate)) return true;
        if (!plugin.pluginConfig().allowNesting()) return false;

        UUID nestedId = items.getId(candidate);
        if (nestedId != null && nestedId.equals(targetBackpackId)) return false;
        if (nestedId == null) return true;

        BackpackData nested = storage.load(nestedId);
        return nested == null || !containsBackpack(nested.contents());
    }

    private boolean containsBackpack(ItemStack[] contents) {
        if (contents == null) return false;
        for (ItemStack item : contents) {
            if (items.isBackpack(item)) return true;
        }
        return false;
    }

    /**
     * Legt ein Item in den Lager-Inhalt eines (nicht geöffneten) Backpacks.
     * Verändert {@code stack} (reduziert Menge). Gibt true zurück, wenn etwas
     * untergebracht wurde. Geöffnete Backpacks werden übersprungen (der offene
     * Puffer ist dann maßgeblich).
     */
    public boolean depositItem(UUID backpackId, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (isOpen(backpackId)) return false;
        BackpackData data = storage.load(backpackId);
        if (data == null) return false;
        BackpackTier tier = tiers.get(data.tier());
        if (tier == null) return false;

        int capacity = tier.storageSlots();
        ItemStack[] contents = data.contents();
        if (contents == null || contents.length < capacity) {
            ItemStack[] grown = new ItemStack[capacity];
            if (contents != null) System.arraycopy(contents, 0, grown, 0, Math.min(contents.length, capacity));
            contents = grown;
        }

        int before = stack.getAmount();
        int max = stack.getMaxStackSize();
        // 1) In passende Stacks einfügen.
        for (int i = 0; i < capacity && stack.getAmount() > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(stack)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                if (space > 0) {
                    int add = Math.min(space, stack.getAmount());
                    slot.setAmount(slot.getAmount() + add);
                    stack.setAmount(stack.getAmount() - add);
                }
            }
        }
        // 2) In leere Slots.
        for (int i = 0; i < capacity && stack.getAmount() > 0; i++) {
            if (contents[i] == null || contents[i].getType().isAir()) {
                int add = Math.min(max, stack.getAmount());
                ItemStack copy = stack.clone();
                copy.setAmount(add);
                contents[i] = copy;
                stack.setAmount(stack.getAmount() - add);
            }
        }
        if (stack.getAmount() == before) return false; // nichts ging rein
        data.contents(contents);
        storage.save(data);
        return true;
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<yellow>◀ Back to backpack"));
        item.setItemMeta(meta);
        return item;
    }

    /** Erzeugt ein neues, sofort registriertes Backpack-Item mit eigener ID. */
    public ItemStack createNew(BackpackTier tier, UUID owner, String main, String accent) {
        UUID id = UUID.randomUUID();
        ItemStack item = items.create(tier, id, main, accent);
        if (owner != null) {
            String name = Bukkit.getOfflinePlayer(owner).getName();
            items.writeOwner(item, owner, name);
            items.applyDisplay(item, tier, id, main, accent);
        }
        BackpackData data = new BackpackData(id);
        data.owner(owner);
        data.tier(tier.key());
        data.mainColor(main);
        data.accentColor(accent);
        data.contents(new ItemStack[tier.storageSlots()]);
        storage.save(data);
        return item;
    }
}
