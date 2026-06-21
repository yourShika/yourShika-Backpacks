package de.yourshika.backpacks;

import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.storage.BackpackStorage;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import de.yourshika.backpacks.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
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
 * Verwaltet außerdem, welches Backpack gerade von wem geöffnet ist, um
 * gleichzeitiges Öffnen (und damit Dupe-Risiken) zu verhindern.
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
     * Öffnet das Backpack-Item für einen Spieler. Gibt eine Fehlermeldung-Key
     * zurück, falls etwas schiefgeht, sonst null bei Erfolg.
     */
    public String openFromItem(Player player, ItemStack item) {
        String tierKey = items.getTierKey(item);
        BackpackTier tier = tiers.get(tierKey);
        if (tier == null) {
            return "error.invalid-backpack";
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

        DyeColor main = items.getMainColor(item, tier.defaultMainColor());
        DyeColor accent = items.getAccentColor(item, tier.defaultAccentColor());

        BackpackData data = storage.load(id);
        if (data == null) {
            data = new BackpackData(id);
            data.owner(player.getUniqueId());
            data.tier(tier.key());
            data.mainColor(main.name());
            data.accentColor(accent.name());
            data.contents(new ItemStack[tier.storageSlots()]);
            storage.save(data);
        } else if (fresh) {
            // Sollte selten passieren (ID-Kollision), Daten bleiben erhalten.
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
        if (openBackpacks.containsKey(id)) {
            return "error.already-open";
        }
        DyeColor main = ColorUtil.parseDye(data.mainColor(), tier.defaultMainColor());
        DyeColor accent = ColorUtil.parseDye(data.accentColor(), tier.defaultAccentColor());
        openInventory(player, tier, data, main, accent);
        return null;
    }

    private void openInventory(Player player, BackpackTier tier, BackpackData data,
                               DyeColor main, DyeColor accent) {
        int storageSlots = tier.storageSlots();
        int totalRows = tier.storageRows() + 1; // + Kontroll-/Upgrade-Reihe
        int size = totalRows * 9;
        int infoSlot = storageSlots + 8;

        BackpackMenuHolder holder = new BackpackMenuHolder(data.id(), tier.key(), storageSlots, infoSlot);

        Component title = mini.deserialize(tier.displayName(), TagResolver.resolver(
                Placeholder.component("main_color", Component.text(ColorUtil.pretty(main)).color(ColorUtil.toTextColor(main))),
                Placeholder.component("accent_color", Component.text(ColorUtil.pretty(accent)).color(ColorUtil.toTextColor(accent))),
                Placeholder.unparsed("storage", String.valueOf(storageSlots)),
                Placeholder.unparsed("upgrades", String.valueOf(tier.upgradeSlots())),
                Placeholder.unparsed("id", data.id().toString().substring(0, 8))
        )).decoration(TextDecoration.ITALIC, false);

        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        // Lager-Inhalt einsetzen.
        ItemStack[] contents = data.contents();
        if (contents != null) {
            for (int i = 0; i < storageSlots && i < contents.length; i++) {
                inv.setItem(i, contents[i]);
            }
        }

        // Kontroll-/Upgrade-Reihe füllen.
        fillControlRow(inv, tier, data, main, accent, storageSlots, infoSlot);

        openBackpacks.put(data.id(), player.getUniqueId());
        player.openInventory(inv);
    }

    private void fillControlRow(Inventory inv, BackpackTier tier, BackpackData data,
                                DyeColor main, DyeColor accent, int storageSlots, int infoSlot) {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));

        for (int i = storageSlots; i < storageSlots + 9; i++) {
            inv.setItem(i, filler);
        }

        // Vorbereitete (gesperrte) Upgrade-Slots – links beginnend, max. 7.
        int upgradeCount = Math.min(tier.upgradeSlots(), 7);
        for (int u = 0; u < upgradeCount; u++) {
            inv.setItem(storageSlots + u, upgradePlaceholder(u + 1));
        }

        // Info-Item rechts.
        inv.setItem(infoSlot, infoItem(tier, data, main, accent));
    }

    private ItemStack infoItem(BackpackTier tier, BackpackData data, DyeColor main, DyeColor accent) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize("<gold><bold>Backpack-Info</bold></gold>")
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<gray>Tier: <white>" + tier.key()));
        lore.add(line("<gray>ID: <white>" + data.id().toString().substring(0, 8)));
        lore.add(line("<gray>Lager: <white>" + tier.storageSlots() + " Slots"));
        lore.add(line("<gray>Hauptfarbe: <white>" + ColorUtil.pretty(main)));
        lore.add(line("<gray>Akzentfarbe: <white>" + ColorUtil.pretty(accent)));
        lore.add(line("<gray>Upgrade-Slots: <white>" + tier.upgradeSlots() + " <dark_gray>(Roadmap)"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack upgradePlaceholder(int index) {
        ItemStack item = new ItemStack(Material.IRON_BARS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize("<dark_gray>Upgrade-Slot " + index + "</dark_gray>")
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<dark_gray>Gesperrt"));
        lore.add(line("<gray>Upgrades folgen in einer späteren Version."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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
     * Speichert den Inhalt eines geöffneten Backpacks aus seinem Inventar und
     * gibt den "geöffnet"-Status frei.
     */
    public void saveAndRelease(BackpackMenuHolder holder, Inventory inv) {
        try {
            BackpackData data = storage.load(holder.backpackId());
            if (data == null) {
                data = new BackpackData(holder.backpackId());
                data.tier(holder.tierKey());
            }
            ItemStack[] contents = new ItemStack[holder.storageSlots()];
            for (int i = 0; i < holder.storageSlots(); i++) {
                contents[i] = inv.getItem(i);
            }
            data.contents(contents);
            storage.save(data);
        } catch (Exception ex) {
            plugin.getLogger().severe("Backpack " + holder.backpackId() + " konnte beim Schließen nicht gespeichert werden: " + ex.getMessage());
        } finally {
            openBackpacks.remove(holder.backpackId());
        }
    }

    /** Speichert alle aktuell offenen Backpacks (Autosave / Shutdown). */
    public void saveAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() instanceof BackpackMenuHolder holder) {
                // Inhalt speichern, aber Status NICHT freigeben (bleibt geöffnet).
                BackpackData data = storage.load(holder.backpackId());
                if (data == null) {
                    data = new BackpackData(holder.backpackId());
                    data.tier(holder.tierKey());
                }
                ItemStack[] contents = new ItemStack[holder.storageSlots()];
                for (int i = 0; i < holder.storageSlots(); i++) {
                    contents[i] = top.getItem(i);
                }
                data.contents(contents);
                storage.save(data);
            }
        }
    }

    /** Erzeugt ein neues, sofort registriertes Backpack-Item mit eigener ID. */
    public ItemStack createNew(BackpackTier tier, UUID owner, DyeColor main, DyeColor accent) {
        UUID id = UUID.randomUUID();
        ItemStack item = items.create(tier, id, main, accent);
        BackpackData data = new BackpackData(id);
        data.owner(owner);
        data.tier(tier.key());
        data.mainColor(main.name());
        data.accentColor(accent.name());
        data.contents(new ItemStack[tier.storageSlots()]);
        storage.save(data);
        return item;
    }
}
