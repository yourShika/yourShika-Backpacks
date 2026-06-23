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

        // Stations-Buttons je nach verbauten Funktions-Upgrades.
        java.util.Set<String> functions = functionUpgradesOf(holder.backpackId());
        if (functions.contains("trash")) {
            inv.setItem(BackpackMenuHolder.STATION_TRASH,
                    stationButton(Material.LAVA_BUCKET, "<gray>Trash"));
        }
        if (functions.contains("ender_link")) {
            inv.setItem(BackpackMenuHolder.STATION_ENDER,
                    stationButton(Material.ENDER_CHEST, "<dark_purple>Ender Chest"));
        }
        if (functions.contains("crafting")) {
            inv.setItem(BackpackMenuHolder.STATION_CRAFTING,
                    stationButton(Material.CRAFTING_TABLE, "<aqua>Crafting"));
        }
        if (functions.contains("stonecutter")) {
            inv.setItem(BackpackMenuHolder.STATION_STONECUTTER,
                    stationButton(Material.STONECUTTER, "<aqua>Stonecutter"));
        }
        if (functions.contains("smithing")) {
            inv.setItem(BackpackMenuHolder.STATION_SMITHING,
                    stationButton(Material.SMITHING_TABLE, "<aqua>Smithing Table"));
        }

        // Info-Item.
        inv.setItem(BackpackMenuHolder.INFO_SLOT, infoItem(tier, holder));

        // Blätter-Buttons.
        if (holder.hasPaging()) {
            if (holder.currentPage() > 0) {
                inv.setItem(BackpackMenuHolder.PREV_SLOT,
                        navButton(Material.ARROW, "<green>◀ Vorherige Seite", holder));
            }
            if (holder.currentPage() < holder.pageCount() - 1) {
                inv.setItem(BackpackMenuHolder.NEXT_SLOT,
                        navButton(Material.ARROW, "<green>Nächste Seite ▶", holder));
            }
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
                ? " <dark_gray>(Seite " + (holder.currentPage() + 1) + "/" + holder.pageCount() + ")" : "";
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
        if (tier != null) {
            lore.add(line("<gray>Tier: <white>" + tier.key()));
            lore.add(line("<gray>Storage: <white>" + tier.storageSlots() + " slots"));
            lore.add(line("<gray>Upgrade slots: <white>" + tier.upgradeSlots()));
        }
        lore.add(line("<gray>ID: <white>" + holder.backpackId().toString().substring(0, 8)));
        lore.add(line("<gray>Main: " + ColorUtil.pretty(holder.mainColor())
                + " <dark_gray>/</dark_gray> <gray>Accent: " + ColorUtil.pretty(holder.accentColor())));
        if (holder.hasPaging()) {
            lore.add(line("<gray>Page: <white>" + (holder.currentPage() + 1) + "/" + holder.pageCount()));
        }

        // Smelting-Status, wenn ein Schmelz-Upgrade verbaut ist.
        java.util.Set<String> fns = functionUpgradesOf(holder.backpackId());
        int[] st = computeSmeltStatus(holder.buffer(), fns);
        if (st != null) {
            String kind = fns.contains("blasting") ? "Blasting"
                    : fns.contains("smoking") ? "Smoking" : "Smelting";
            lore.add(Component.empty());
            lore.add(line("<gold>" + kind + " status"));
            lore.add(line("<gray>To smelt: <white>" + st[0] + " <gray>items"));
            lore.add(line("<gray>Fuel can smelt: <white>" + st[1] + " <gray>items"));
            String time = st[2] <= 0 ? "<dark_gray>idle"
                    : "<white>~" + st[2] + "s <dark_gray>(paused while open)";
            lore.add(line("<gray>Est. time: " + time));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack stationButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name + " <gray>Upgrade"));
        meta.lore(List.of(line("<yellow>Click to open")));
        item.setItemMeta(meta);
        return item;
    }

    /** Öffnet die Vanilla-Station (Crafting/Stonecutter/Smithing) für ein Stations-Upgrade. */
    public void openStation(Player player, String station) {
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
                case "trash" -> {
                    de.yourshika.backpacks.gui.TrashMenuHolder holder =
                            new de.yourshika.backpacks.gui.TrashMenuHolder();
                    Inventory trash = Bukkit.createInventory(holder, 27,
                            mini.deserialize("<dark_gray><bold>Trash</bold> <gray>(items here are deleted)")
                                    .decoration(TextDecoration.ITALIC, false));
                    holder.setInventory(trash);
                    player.openInventory(trash);
                }
                default -> { }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Station '" + station + "' konnte nicht geöffnet werden: " + t.getMessage());
        }
    }

    private ItemStack upgradeButton(BackpackTier tier) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gold><bold>Upgrades</bold></gold>"));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<gray>Upgrade-Slots: <white>" + tier.upgradeSlots()));
        lore.add(line("<yellow>Klicken zum Öffnen"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navButton(Material material, String name, BackpackMenuHolder holder) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name));
        meta.lore(List.of(line("<gray>Seite " + (holder.currentPage() + 1) + "/" + holder.pageCount())));
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
            // Compacting-Upgrade: 9er-Stacks zu Blöcken verdichten.
            if (functionUpgradesOf(holder.backpackId()).contains("compacting")) {
                compact(holder.buffer());
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

    /** Verdichtet 9er-Mengen verdichtbarer Items im Buffer zu Blöcken. */
    private void compact(ItemStack[] buffer) {
        java.util.Map<Material, Integer> totals = new java.util.HashMap<>();
        for (ItemStack it : buffer) {
            if (it == null) continue;
            Material block = COMPACT.get(it.getType());
            if (block == null || block == it.getType()) continue;          // nicht verdichtbar
            if (!it.isSimilar(new ItemStack(it.getType()))) continue;      // nur "plain" Items
            totals.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        for (var e : totals.entrySet()) {
            Material mat = e.getKey();
            int total = e.getValue();
            int blocks = total / 9;
            if (blocks == 0) continue;
            int remainder = total % 9;
            // Alle "plain" Items dieses Typs aus dem Buffer entfernen.
            for (int i = 0; i < buffer.length; i++) {
                ItemStack it = buffer[i];
                if (it != null && it.getType() == mat && it.isSimilar(new ItemStack(mat))) {
                    buffer[i] = null;
                }
            }
            addToBuffer(buffer, new ItemStack(COMPACT.get(mat), blocks));
            if (remainder > 0) addToBuffer(buffer, new ItemStack(mat, remainder));
        }
    }

    /** Legt einen Stack in freie/passende Buffer-Slots (für Compacting). */
    private void addToBuffer(ItemStack[] buffer, ItemStack stack) {
        int max = stack.getMaxStackSize();
        for (int i = 0; i < buffer.length && stack.getAmount() > 0; i++) {
            ItemStack slot = buffer[i];
            if (slot != null && slot.isSimilar(stack)) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                if (space > 0) {
                    int add = Math.min(space, stack.getAmount());
                    slot.setAmount(slot.getAmount() + add);
                    stack.setAmount(stack.getAmount() - add);
                }
            }
        }
        for (int i = 0; i < buffer.length && stack.getAmount() > 0; i++) {
            if (buffer[i] == null || buffer[i].getType().isAir()) {
                int add = Math.min(max, stack.getAmount());
                ItemStack copy = stack.clone();
                copy.setAmount(add);
                buffer[i] = copy;
                stack.setAmount(stack.getAmount() - add);
            }
        }
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

    /**
     * Schmelz-Status eines Backpacks aus seinem aktuellen Inhalt:
     * {@code [toSmelt, fuelCapacity, estSeconds]} oder null ohne Schmelz-Upgrade.
     * (Während das Backpack geöffnet ist, pausiert das Schmelzen.)
     */
    public int[] computeSmeltStatus(ItemStack[] contents, java.util.Set<String> fns) {
        if (contents == null) return null;
        boolean any = fns.contains("smelting") || fns.contains("blasting") || fns.contains("smoking");
        if (!any) return null;
        ensureSmeltMaps();
        java.util.Set<Material> inputs = new java.util.HashSet<>();
        if (fns.contains("smelting")) inputs.addAll(furnaceMap.keySet());
        if (fns.contains("blasting")) inputs.addAll(blastMap.keySet());
        if (fns.contains("smoking")) inputs.addAll(smokerMap.keySet());

        int toSmelt = 0, fuelCap = 0;
        for (ItemStack it : contents) {
            if (it == null || it.getType().isAir()) continue;
            int fv = fuelValue(it.getType());
            if (fv > 0) fuelCap += fv * it.getAmount();
            if (inputs.contains(it.getType()) && it.isSimilar(new ItemStack(it.getType()))) {
                toSmelt += it.getAmount();
            }
        }
        int processable = Math.min(toSmelt, fuelCap);
        int estSeconds = (int) Math.ceil(processable / 8.0) * 2;
        return new int[]{toSmelt, fuelCap, estSeconds};
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

    /**
     * Führt eine Schmelz-Operation an einem (nicht geöffneten) Backpack aus:
     * verbraucht eine Brennstoff-Einheit und schmilzt bis zu deren Wert an Items
     * eines passenden Typs. Gibt true zurück, wenn etwas geschmolzen wurde.
     */
    public boolean smelt(UUID backpackId, Map<Material, ItemStack> recipes) {
        if (isOpen(backpackId)) return false;
        BackpackData data = storage.load(backpackId);
        if (data == null) return false;
        ItemStack[] contents = data.contents();
        if (contents == null) return false;

        int fuelSlot = -1, inputSlot = -1;
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir()) continue;
            if (fuelSlot < 0 && fuelValue(it.getType()) > 0) fuelSlot = i;
            if (inputSlot < 0 && recipes.containsKey(it.getType())
                    && it.isSimilar(new ItemStack(it.getType()))) inputSlot = i;
        }
        if (fuelSlot < 0 || inputSlot < 0) return false;

        ItemStack input = contents[inputSlot];
        int batch = Math.min(input.getAmount(), Math.min(8, fuelValue(contents[fuelSlot].getType())));
        if (batch <= 0) return false;

        ItemStack result = recipes.get(input.getType()).clone();
        result.setAmount(result.getAmount() * batch);

        // Input verringern.
        input.setAmount(input.getAmount() - batch);
        if (input.getAmount() <= 0) contents[inputSlot] = null;
        // Eine Brennstoff-Einheit verbrauchen (Lava-Eimer -> leerer Eimer).
        ItemStack fuel = contents[fuelSlot];
        if (fuel.getType() == Material.LAVA_BUCKET) {
            contents[fuelSlot] = new ItemStack(Material.BUCKET);
        } else {
            fuel.setAmount(fuel.getAmount() - 1);
            if (fuel.getAmount() <= 0) contents[fuelSlot] = null;
        }
        addToBuffer(contents, result);
        data.contents(contents);
        storage.save(data);
        return true;
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
        meta.displayName(line("<yellow>◀ Zurück zum Backpack"));
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
