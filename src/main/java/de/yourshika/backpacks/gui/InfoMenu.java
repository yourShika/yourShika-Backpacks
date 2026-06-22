package de.yourshika.backpacks.gui;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.upgrade.UpgradeManager;
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

/**
 * Schöne Übersichts- und Rezept-GUI ({@code /bp info}): zeigt alle Backpacks und
 * Upgrade-Items; ein Klick öffnet das jeweilige Crafting-/Smithing-Rezept.
 * Reine Anzeige – nichts lässt sich entnehmen.
 */
public final class InfoMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    // Crafting-Grid-Slots (3x3) + Pfeil + Ergebnis.
    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int ARROW = 24;
    private static final int RESULT = 25;
    private static final int BACK = 49;

    // Smithing-Slots.
    private static final int S_TEMPLATE = 11;
    private static final int S_BASE = 13;
    private static final int S_ADDITION = 15;

    private InfoMenu() {}

    // --- Übersicht ---------------------------------------------------------

    public static void openOverview(YourShikaBackpacks plugin, Player player) {
        InfoMenuHolder holder = new InfoMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                line("<gradient:#6E5BC8:#5BE8D4><bold>Backpack-Übersicht</bold></gradient>"));
        holder.setInventory(inv);
        fill(inv);

        inv.setItem(4, header("<gold><bold>Rücksäcke & Upgrades</bold></gold>",
                List.of("<gray>Klicke ein Item, um sein", "<gray>Rezept anzuzeigen.")));

        // Rücksäcke (Reihe 2).
        int slot = 10;
        for (BackpackTier tier : plugin.tiers().all()) {
            if (slot > 16) break;
            ItemStack icon = plugin.itemFactory().createTemplate(tier);
            holder.mapAction(slot, "bp:" + tier.key());
            inv.setItem(slot++, withHint(icon));
        }

        // Upgrade-Items (Reihe 4).
        UpgradeManager um = plugin.upgradeManager();
        inv.setItem(27, header("<#A0703C><bold>Upgrade-Items</bold></#A0703C>",
                List.of("<gray>Die Bausteine der Upgrade-Kette.")));
        int us = 28;
        ItemStack base = um.baseUpgradeItem();
        if (base != null) {
            holder.mapAction(us, "up:base");
            inv.setItem(us++, withHint(base));
        }
        List<String> order = plugin.tiers().keys();
        for (int i = 1; i < order.size() && us <= 34; i++) {
            String target = order.get(i);
            ItemStack up = um.upgradeItem(target);
            if (up == null) continue;
            holder.mapAction(us, "up:" + target);
            inv.setItem(us++, withHint(up));
        }

        player.openInventory(inv);
    }

    // --- Rezept-Ansichten --------------------------------------------------

    public static void openAction(YourShikaBackpacks plugin, Player player, String action) {
        if (action == null) return;
        if (action.equals("back")) {
            openOverview(plugin, player);
            return;
        }
        if (action.startsWith("bp:")) {
            openBackpackRecipe(plugin, player, action.substring(3));
        } else if (action.startsWith("up:")) {
            openUpgradeRecipe(plugin, player, action.substring(3));
        }
    }

    private static void openBackpackRecipe(YourShikaBackpacks plugin, Player player, String tierKey) {
        BackpackTier tier = plugin.tiers().get(tierKey);
        if (tier == null) { openOverview(plugin, player); return; }

        InfoMenuHolder holder = new InfoMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                title(tier.displayName()));
        holder.setInventory(inv);
        fill(inv);
        backButton(holder, inv);

        ItemStack result = plugin.itemFactory().createTemplate(tier);
        List<String> order = plugin.tiers().keys();
        boolean isFirst = !order.isEmpty() && order.get(0).equalsIgnoreCase(tierKey);

        if (isFirst && tier.recipe() != null && tier.recipe().enabled()
                && tier.recipe().shape() != null && !tier.recipe().shape().isEmpty()) {
            // Direktes Crafting (Leder-Rucksack).
            renderGrid(inv, tier.recipe().shape(), tier.recipe().ingredients());
            inv.setItem(RESULT, result);
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>8× Leder um eine Truhe.")));
        } else {
            // Smithing-Veredelung aus dem vorherigen Tier.
            int idx = order.indexOf(tierKey.toLowerCase());
            String prev = idx > 0 ? order.get(idx - 1) : null;
            ItemStack prevPack = prev != null
                    ? plugin.itemFactory().createTemplate(plugin.tiers().get(prev)) : icon(Material.LEATHER_HORSE_ARMOR, "<gray>Backpack");
            ItemStack upgrade = plugin.upgradeManager().upgradeItem(tierKey);
            renderSmithing(inv,
                    icon(Material.LEATHER, "<white>Leder <gray>(Vorlage)"),
                    label(prevPack, "<gray>vorheriges Backpack (Basis)"),
                    upgrade != null ? label(upgrade, "<gray>Tier-Upgrade (Zugabe)")
                            : icon(Material.PAPER, "<gray>Tier-Upgrade"),
                    result);
            inv.setItem(4, header("<aqua><bold>Smithing Table</bold></aqua>",
                    List.of("<gray>Leder + vorheriges Backpack + Upgrade.",
                            "<gray>ID, Inhalt & Farbe bleiben erhalten.")));
        }
        player.openInventory(inv);
    }

    private static void openUpgradeRecipe(YourShikaBackpacks plugin, Player player, String key) {
        UpgradeManager um = plugin.upgradeManager();
        InfoMenuHolder holder = new InfoMenuHolder();

        if (key.equals("base")) {
            Inventory inv = Bukkit.createInventory(holder, 54, line("<#A0703C><bold>Upgrade-Leder</bold>"));
            holder.setInventory(inv);
            fill(inv);
            backButton(holder, inv);
            renderGrid(inv, List.of(" S ", "SLS", " S "),
                    java.util.Map.of('S', Material.STRING, 'L', Material.LEATHER));
            inv.setItem(RESULT, um.baseUpgradeItem());
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>1× Leder + 4× Faden.")));
            player.openInventory(inv);
            return;
        }

        ItemStack upgrade = um.upgradeItem(key);
        if (upgrade == null) { openOverview(plugin, player); return; }
        Inventory inv = Bukkit.createInventory(holder, 54, title(name(upgrade)));
        holder.setInventory(inv);
        fill(inv);
        backButton(holder, inv);

        if (um.isSmithingUpgrade(key)) {
            // Netherite-Upgrade per Smithing.
            renderSmithing(inv,
                    label(um.baseUpgradeItem(), "<gray>Upgrade-Leder (Vorlage)"),
                    icon(um.materialFor(key), "<gray>Netherite-Ingot (Basis)"),
                    icon(Material.STRING, "<gray>Faden (Zugabe)"),
                    upgrade);
            inv.setItem(4, header("<aqua><bold>Smithing Table</bold></aqua>",
                    List.of("<gray>Upgrade-Leder + Netherite-Ingot + Faden.")));
        } else {
            renderGrid(inv, List.of("MMM", "MUM", "MMM"),
                    java.util.Map.of('M', um.materialFor(key)));
            inv.setItem(GRID[4], label(um.baseUpgradeItem(), "<gray>Upgrade-Leder"));
            inv.setItem(RESULT, upgrade);
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>Upgrade-Leder + 8× Tier-Material.")));
        }
        player.openInventory(inv);
    }

    // --- Render-Helfer -----------------------------------------------------

    private static void renderGrid(Inventory inv, List<String> shape, java.util.Map<Character, Material> ingredients) {
        for (int r = 0; r < 3; r++) {
            String row = r < shape.size() ? shape.get(r) : "   ";
            for (int c = 0; c < 3; c++) {
                char ch = c < row.length() ? row.charAt(c) : ' ';
                Material mat = ingredients.get(ch);
                if (mat != null) {
                    inv.setItem(GRID[r * 3 + c], new ItemStack(mat));
                }
            }
        }
        inv.setItem(ARROW, icon(Material.ARROW, "<gray>ergibt"));
    }

    private static void renderSmithing(Inventory inv, ItemStack template, ItemStack base,
                                       ItemStack addition, ItemStack result) {
        inv.setItem(S_TEMPLATE, template);
        inv.setItem(S_BASE, base);
        inv.setItem(S_ADDITION, addition);
        inv.setItem(ARROW, icon(Material.ARROW, "<gray>ergibt"));
        inv.setItem(RESULT, result);
    }

    private static void backButton(InfoMenuHolder holder, Inventory inv) {
        holder.mapAction(BACK, "back");
        inv.setItem(BACK, icon(Material.BARRIER, "<red>◀ Zurück"));
    }

    private static void fill(Inventory inv) {
        ItemStack pane = icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        // Innenraum freiräumen.
        for (int i = 0; i < inv.getSize(); i++) {
            int col = i % 9, row = i / 9;
            if (col > 0 && col < 8 && row > 0 && row < 5) inv.setItem(i, null);
        }
    }

    private static ItemStack withHint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(line("<yellow>» Klicken für das Rezept"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack label(ItemStack item, String loreLine) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(line(loreLine));
            meta.lore(lore);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    private static ItemStack icon(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(line(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack header(String name, List<String> loreLines) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line(name));
        List<Component> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(line(l));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Component title(String mini) {
        return MINI.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }

    private static String name(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            return MINI.serialize(meta.displayName());
        }
        return "<white>Rezept";
    }

    private static Component line(String mini) {
        return MINI.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }
}
