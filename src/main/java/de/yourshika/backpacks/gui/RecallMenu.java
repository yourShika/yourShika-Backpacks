package de.yourshika.backpacks.gui;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Auswahl-GUI für {@code /bp recall}, wenn ein Spieler mehr als ein platziertes
 * Backpack mit Recall-Upgrade besitzt. Klick auf ein Backpack holt genau dieses
 * zurück; ein Button rechts unten holt alle zurück.
 */
public final class RecallMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private RecallMenu() {}

    public static void open(YourShikaBackpacks plugin, Player player, List<UUID> ids) {
        RecallMenuHolder holder = new RecallMenuHolder();
        int rows = Math.min(6, Math.max(2, (ids.size() + 8) / 9 + 1));
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(holder, size,
                line("<gradient:#6E5BC8:#5BE8D4><bold>Recall a backpack</bold></gradient>"));
        holder.setInventory(inv);

        BackpackItemFactory items = plugin.itemFactory();
        TierRegistry tiers = plugin.tiers();
        int slot = 0;
        for (UUID id : ids) {
            if (slot >= size - 1) break;
            BackpackData d = plugin.manager().storage().load(id);
            ItemStack icon;
            if (d != null) {
                BackpackTier tier = tiers.get(d.tier());
                if (tier == null) tier = tiers.all().iterator().next();
                String main = d.mainColor() != null ? d.mainColor() : tier.defaultMainColor();
                String accent = d.accentColor() != null ? d.accentColor() : tier.defaultAccentColor();
                icon = items.create(tier, id, main, accent);
                ItemMeta meta = icon.getItemMeta();
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                String ownerName = d.owner() == null ? null
                        : Bukkit.getOfflinePlayer(d.owner()).getName();
                lore.add(line("<gray>Owner: <white>" + (ownerName == null ? "—" : ownerName)));
                lore.add(line("<dark_gray>" + (d.world() == null ? "?" : d.world()) + " "
                        + (int) Math.floor(d.x()) + ", " + (int) Math.floor(d.y()) + ", " + (int) Math.floor(d.z())));
                lore.add(line("<yellow>Click to recall this backpack"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            } else {
                icon = new ItemStack(Material.CHEST);
            }
            inv.setItem(slot, icon);
            holder.map(slot, id);
            slot++;
        }

        int allSlot = size - 1;
        ItemStack all = new ItemStack(Material.ENDER_PEARL);
        ItemMeta am = all.getItemMeta();
        am.displayName(line("<green><bold>Recall all</bold></green>"));
        am.lore(List.of(line("<gray>Bring back all <white>" + ids.size() + "</white> backpacks")));
        all.setItemMeta(am);
        inv.setItem(allSlot, all);
        holder.allSlot(allSlot);

        player.openInventory(inv);
    }

    private static Component line(String mm) {
        return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }
}
