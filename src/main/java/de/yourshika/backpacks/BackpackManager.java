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
        meta.displayName(line("<gold><bold>Backpack-Info</bold></gold>"));
        List<Component> lore = new ArrayList<>();
        if (tier != null) {
            lore.add(line("<gray>Tier: <white>" + tier.key()));
            lore.add(line("<gray>Lager gesamt: <white>" + tier.storageSlots() + " Slots"));
            lore.add(line("<gray>Upgrade-Slots: <white>" + tier.upgradeSlots()));
        }
        lore.add(line("<gray>ID: <white>" + holder.backpackId().toString().substring(0, 8)));
        lore.add(line("<gray>Haupt: " + ColorUtil.pretty(holder.mainColor())
                + " <dark_gray>/</dark_gray> <gray>Akzent: " + ColorUtil.pretty(holder.accentColor())));
        if (holder.hasPaging()) {
            lore.add(line("<gray>Seite: <white>" + (holder.currentPage() + 1) + "/" + holder.pageCount()));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
