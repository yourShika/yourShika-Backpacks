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

    // Crafting grid (3x3) + arrow + result.
    private static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int ARROW = 24;
    private static final int RESULT = 25;
    private static final int BACK = 49;

    // Übersicht: Backpacks (2 Reihen) und Upgrade-Items (2 Reihen) – wachsen mit,
    // damit auch neue Tiers (z.B. Dragon) + Dragon Core hineinpassen.
    private static final int[] BP_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int[] UP_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    // Smithing layout: inputs in a row, a centered arrow below, the result
    // directly above the Back button.
    private static final int S_TEMPLATE = 20;
    private static final int S_BASE = 22;
    private static final int S_ADDITION = 24;
    private static final int S_ARROW = 31;
    private static final int S_RESULT = 40;

    private InfoMenu() {}

    // --- Übersicht ---------------------------------------------------------

    public static void openOverview(YourShikaBackpacks plugin, Player player) {
        InfoMenuHolder holder = new InfoMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                line("<gradient:#6E5BC8:#5BE8D4><bold>Backpack Overview</bold></gradient>"));
        holder.setInventory(inv);
        fill(inv);

        inv.setItem(4, header("<gold><bold>Backpacks & Upgrades</bold></gold>",
                List.of("<gray>Click an item to view", "<gray>its recipe.")));

        // Rücksäcke (Reihen 2-3, wachsen mit der Tier-Anzahl – auch Dragon passt rein).
        int bi = 0;
        for (BackpackTier tier : plugin.tiers().all()) {
            if (bi >= BP_SLOTS.length) break;
            ItemStack icon = plugin.itemFactory().createTemplate(tier);
            int s = BP_SLOTS[bi++];
            holder.mapAction(s, "bp:" + tier.key());
            inv.setItem(s, withHint(icon));
        }

        // Upgrade-Items (Reihen 4-5): Upgrade-Leder, Tier-Upgrades, Dragon Core.
        UpgradeManager um = plugin.upgradeManager();
        inv.setItem(27, header("<#A0703C><bold>Upgrade Items</bold></#A0703C>",
                List.of("<gray>The building blocks of the upgrade chain.")));
        int ui = 0;
        ItemStack base = um.baseUpgradeItem();
        if (base != null) {
            holder.mapAction(UP_SLOTS[ui], "up:base");
            inv.setItem(UP_SLOTS[ui++], withHint(base));
        }
        List<String> order = plugin.tiers().keys();
        for (int i = 1; i < order.size() && ui < UP_SLOTS.length; i++) {
            String target = order.get(i);
            ItemStack up = um.upgradeItem(target);
            if (up == null) continue;
            holder.mapAction(UP_SLOTS[ui], "up:" + target);
            inv.setItem(UP_SLOTS[ui++], withHint(up));
        }
        // Dragon Core (Zwischenprodukt der Dragon-Kette), falls vorhanden.
        ItemStack core = um.dragonCoreItem();
        if (core != null && ui < UP_SLOTS.length) {
            holder.mapAction(UP_SLOTS[ui], "up:dragon_core");
            inv.setItem(UP_SLOTS[ui++], withHint(core));
        }

        // Function upgrades entry.
        holder.mapAction(8, "functions");
        inv.setItem(8, header("<aqua><bold>Function Upgrades</bold></aqua>",
                List.of("<gray>Pickup, Magnet, Crafting,", "<gray>Smithing, Everlasting …",
                        "", "<yellow>» Click to view")));

        player.openInventory(inv);
    }

    // --- Recipe views ------------------------------------------------------

    public static void openAction(YourShikaBackpacks plugin, Player player, String action) {
        if (action == null) return;
        if (action.equals("back")) {
            openOverview(plugin, player);
            return;
        }
        if (action.equals("functions")) {
            openFunctionList(plugin, player);
        } else if (action.startsWith("bp:")) {
            openBackpackRecipe(plugin, player, action.substring(3));
        } else if (action.startsWith("up:")) {
            openUpgradeRecipe(plugin, player, action.substring(3));
        } else if (action.startsWith("fn:")) {
            openFunctionRecipe(plugin, player, action.substring(3));
        }
    }

    private static void openFunctionList(YourShikaBackpacks plugin, Player player) {
        InfoMenuHolder holder = new InfoMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54,
                line("<aqua><bold>Function Upgrades</bold></aqua>"));
        holder.setInventory(inv);
        fill(inv);
        backButton(holder, inv);
        inv.setItem(4, header("<aqua><bold>Function Upgrades</bold></aqua>",
                List.of("<gray>Build these and place them in a", "<gray>backpack's Upgrades menu.")));

        int slot = 10;
        for (de.yourshika.backpacks.upgrade.FunctionUpgrade up
                : de.yourshika.backpacks.upgrade.FunctionUpgrade.values()) {
            if (slot > 43) break;
            if (slot % 9 == 8) slot += 2; // Rand überspringen
            ItemStack item = plugin.functionUpgrades().item(up.id());
            if (item == null) continue;
            holder.mapAction(slot, "fn:" + up.id());
            inv.setItem(slot++, withHint(item));
        }
        player.openInventory(inv);
    }

    private static void openFunctionRecipe(YourShikaBackpacks plugin, Player player, String id) {
        de.yourshika.backpacks.upgrade.FunctionUpgrade up =
                de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(id);
        ItemStack result = plugin.functionUpgrades().item(id);
        if (up == null || result == null) { openFunctionList(plugin, player); return; }

        InfoMenuHolder holder = new InfoMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title(name(result)));
        holder.setInventory(inv);
        fill(inv);
        backButtonTo(holder, inv, "functions");

        ItemStack required = up.requires() != null ? plugin.functionUpgrades().item(up.requires()) : null;
        renderFunctionGrid(inv, up.shape(), up.ingredients(),
                plugin.upgradeManager().baseUpgradeItem(), required);
        inv.setItem(RESULT, result);
        List<String> hint = new ArrayList<>();
        hint.add("<gray>'U' = Upgrade Leather (required).");
        if (required != null) {
            hint.add("<gray>'X' = the base upgrade you must craft first.");
        }
        inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>", hint));
        player.openInventory(inv);
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
                    List.of("<gray>8x Leather around a Chest.")));
        } else {
            // Smithing-Veredelung aus dem vorherigen Tier.
            int idx = order.indexOf(tierKey.toLowerCase());
            String prev = idx > 0 ? order.get(idx - 1) : null;
            ItemStack prevPack = prev != null
                    ? plugin.itemFactory().createTemplate(plugin.tiers().get(prev)) : icon(Material.LEATHER_HORSE_ARMOR, "<gray>Backpack");
            ItemStack upgrade = plugin.upgradeManager().upgradeItem(tierKey);
            renderSmithing(inv,
                    icon(Material.LEATHER, "<white>Leather <gray>(Template)"),
                    label(prevPack, "<gray>Previous backpack (Base)"),
                    upgrade != null ? label(upgrade, "<gray>Tier upgrade (Addition)")
                            : icon(Material.PAPER, "<gray>Tier upgrade"),
                    result);
            inv.setItem(4, header("<aqua><bold>Smithing Table</bold></aqua>",
                    List.of("<gray>Leather + previous backpack + tier upgrade.",
                            "<gray>Keeps ID, contents & color.")));
        }
        player.openInventory(inv);
    }

    private static void openUpgradeRecipe(YourShikaBackpacks plugin, Player player, String key) {
        UpgradeManager um = plugin.upgradeManager();
        InfoMenuHolder holder = new InfoMenuHolder();

        if (key.equals("base")) {
            Inventory inv = Bukkit.createInventory(holder, 54, line("<#A0703C><bold>Upgrade Leather</bold>"));
            holder.setInventory(inv);
            fill(inv);
            backButton(holder, inv);
            renderGrid(inv, List.of(" S ", "SLS", " S "),
                    java.util.Map.of('S', Material.STRING, 'L', Material.LEATHER));
            inv.setItem(RESULT, um.baseUpgradeItem());
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>1x Leather + 4x String.")));
            player.openInventory(inv);
            return;
        }

        if (key.equals("dragon_core")) {
            ItemStack core = um.dragonCoreItem();
            if (core == null) { openOverview(plugin, player); return; }
            Inventory inv = Bukkit.createInventory(holder, 54, title(name(core)));
            holder.setInventory(inv);
            fill(inv);
            backButton(holder, inv);
            renderGrid(inv, configShape(plugin, "upgrades.dragon.core-recipe.shape",
                            List.of("ESE", "SNS", "EDE")),
                    configIng(plugin, "upgrades.dragon.core-recipe.ingredients",
                            java.util.Map.of('E', Material.ECHO_SHARD, 'S', Material.SHULKER_SHELL,
                                    'N', Material.NETHER_STAR, 'D', Material.DRAGON_HEAD)));
            inv.setItem(RESULT, core);
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>Craft the Dragon Core from End materials.",
                            "<dark_gray>Step 1 of the Dragon upgrade chain.")));
            player.openInventory(inv);
            return;
        }

        ItemStack upgrade = um.upgradeItem(key);
        if (upgrade == null) { openOverview(plugin, player); return; }
        Inventory inv = Bukkit.createInventory(holder, 54, title(name(upgrade)));
        holder.setInventory(inv);
        fill(inv);
        backButton(holder, inv);

        if (key.equalsIgnoreCase("dragon")) {
            // Mehrstufig: 'C' = Dragon Core (Stufe 1), 'U' = Upgrade-Leder.
            renderDragonUpgradeGrid(inv,
                    configShape(plugin, "upgrades.dragon.upgrade-recipe.shape",
                            List.of(" B ", "CUC", " B ")),
                    configIng(plugin, "upgrades.dragon.upgrade-recipe.ingredients",
                            java.util.Map.of('B', Material.DRAGON_BREATH)),
                    um.baseUpgradeItem(), um.dragonCoreItem());
            inv.setItem(RESULT, upgrade);
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>'C' = Dragon Core (craft it first),",
                            "<gray>'U' = Upgrade Leather.",
                            "<dark_gray>Then smith it onto a Netherite Backpack.")));
            player.openInventory(inv);
            return;
        }

        if (um.isSmithingUpgrade(key)) {
            // Netherite-Upgrade per Smithing.
            renderSmithing(inv,
                    label(um.baseUpgradeItem(), "<gray>Upgrade Leather (Template)"),
                    icon(um.materialFor(key), "<gray>Netherite Ingot (Base)"),
                    icon(Material.STRING, "<gray>String (Addition)"),
                    upgrade);
            inv.setItem(4, header("<aqua><bold>Smithing Table</bold></aqua>",
                    List.of("<gray>Upgrade Leather + Netherite Ingot + String.")));
        } else {
            renderGrid(inv, List.of("MMM", "MUM", "MMM"),
                    java.util.Map.of('M', um.materialFor(key)));
            inv.setItem(GRID[4], label(um.baseUpgradeItem(), "<gray>Upgrade-Leder"));
            inv.setItem(RESULT, upgrade);
            inv.setItem(4, header("<yellow><bold>Crafting Table</bold></yellow>",
                    List.of("<gray>Upgrade Leather + 8x tier material.")));
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
        inv.setItem(ARROW, icon(Material.ARROW, "<gray>→"));
    }

    private static void renderSmithing(Inventory inv, ItemStack template, ItemStack base,
                                       ItemStack addition, ItemStack result) {
        inv.setItem(S_TEMPLATE, template);
        inv.setItem(S_BASE, base);
        inv.setItem(S_ADDITION, addition);
        inv.setItem(S_ARROW, icon(Material.SPECTRAL_ARROW, "<gray>↓"));
        inv.setItem(S_RESULT, label(result, "<yellow>Result"));
    }

    private static void backButton(InfoMenuHolder holder, Inventory inv) {
        backButtonTo(holder, inv, "back");
    }

    private static void backButtonTo(InfoMenuHolder holder, Inventory inv, String action) {
        holder.mapAction(BACK, action);
        inv.setItem(BACK, icon(Material.BARRIER, "<red>◀ Back"));
    }

    private static void renderFunctionGrid(Inventory inv, List<String> shape,
                                           java.util.Map<Character, Material> ingredients,
                                           ItemStack base, ItemStack required) {
        for (int r = 0; r < 3; r++) {
            String row = r < shape.size() ? shape.get(r) : "   ";
            for (int c = 0; c < 3; c++) {
                char ch = c < row.length() ? row.charAt(c) : ' ';
                if (ch == 'U') {
                    inv.setItem(GRID[r * 3 + c], base);
                } else if (ch == 'X' && required != null) {
                    inv.setItem(GRID[r * 3 + c], label(required, "<gray>Base upgrade (required)"));
                } else {
                    Material mat = ingredients.get(ch);
                    if (mat != null) inv.setItem(GRID[r * 3 + c], new ItemStack(mat));
                }
            }
        }
        inv.setItem(ARROW, icon(Material.ARROW, "<gray>→"));
    }

    /** Wie {@link #renderGrid}, aber 'U' = Upgrade-Leder und 'C' = Dragon Core. */
    private static void renderDragonUpgradeGrid(Inventory inv, List<String> shape,
                                                java.util.Map<Character, Material> ingredients,
                                                ItemStack base, ItemStack core) {
        for (int r = 0; r < 3; r++) {
            String row = r < shape.size() ? shape.get(r) : "   ";
            for (int col = 0; col < 3; col++) {
                char ch = col < row.length() ? row.charAt(col) : ' ';
                if (ch == 'U' && base != null) {
                    inv.setItem(GRID[r * 3 + col], label(base, "<gray>Upgrade Leather"));
                } else if (ch == 'C' && core != null) {
                    inv.setItem(GRID[r * 3 + col], label(core, "<gray>Dragon Core (craft first)"));
                } else {
                    Material mat = ingredients.get(ch);
                    if (mat != null) inv.setItem(GRID[r * 3 + col], new ItemStack(mat));
                }
            }
        }
        inv.setItem(ARROW, icon(Material.ARROW, "<gray>→"));
    }

    private static List<String> configShape(YourShikaBackpacks plugin, String path, List<String> def) {
        List<String> s = plugin.getConfig().getStringList(path);
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static java.util.Map<Character, Material> configIng(YourShikaBackpacks plugin, String path,
                                                               java.util.Map<Character, Material> def) {
        var sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return def;
        java.util.Map<Character, Material> map = new java.util.HashMap<>();
        for (String k : sec.getKeys(false)) {
            if (k.isEmpty()) continue;
            Material m = Material.matchMaterial(sec.getString(k, ""));
            if (m != null) map.put(k.charAt(0), m);
        }
        return map.isEmpty() ? def : map;
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
        lore.add(line("<yellow>» Click to view recipe"));
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
        return "<white>Recipe";
    }

    private static Component line(String mini) {
        return MINI.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }
}
