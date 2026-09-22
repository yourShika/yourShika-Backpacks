package de.yourshika.backpacks.upgrade;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import de.yourshika.backpacks.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registriert alle Upgrade-bezogenen Rezepte und führt die Tier-Veredelung im
 * Smithing Table aus.
 *
 * <p>Kette: Leder → Copper → Eisen → Gold → Diamant → Emerald → Netherite.</p>
 *
 * <ul>
 *   <li><b>Upgrade-Leder</b>: Leder + Faden (Crafting Table).</li>
 *   <li><b>Tier-Upgrade</b> (copper…emerald): Upgrade-Leder + 8× Tier-Material
 *       (Crafting Table). Netherite-Upgrade: Smithing Table – Upgrade-Leder
 *       (Vorlage) + Netherite-Ingot (Basis) + Faden (Zugabe).</li>
 *   <li><b>Backpack-Veredelung</b> (Smithing Table): Leder + vorheriges Backpack
 *       + passendes Tier-Upgrade → nächstes Backpack, <b>unter Erhalt von ID,
 *       Inhalt und Farbe</b>.</li>
 * </ul>
 */
public final class UpgradeManager implements Listener {

    private final YourShikaBackpacks plugin;
    private final TierRegistry tiers;
    private final BackpackItemFactory backpacks;
    private final UpgradeItemFactory upgrades;
    private final BackpackManager manager;

    private final List<NamespacedKey> registered = new ArrayList<>();
    /** Ziel-Tier -> kanonisches Tier-Upgrade-Item (für ExactChoice & Smithing). */
    private final Map<String, ItemStack> upgradeItems = new HashMap<>();
    /** Kanonisches Upgrade-Leder (für ExactChoice & Rezepte). */
    private ItemStack baseItem;
    /** Kanonischer Dragon Core (Zwischenprodukt der Dragon-Upgrade-Kette). */
    private ItemStack dragonCoreItem;

    public UpgradeManager(YourShikaBackpacks plugin, TierRegistry tiers,
                          BackpackItemFactory backpacks, UpgradeItemFactory upgrades,
                          BackpackManager manager) {
        this.plugin = plugin;
        this.tiers = tiers;
        this.backpacks = backpacks;
        this.upgrades = upgrades;
        this.manager = manager;
    }

    public UpgradeItemFactory items() {
        return upgrades;
    }

    /**
     * Idempotente Registrierung: bereits vorhandene Rezepte werden NICHT entfernt
     * und neu angelegt. {@code Bukkit.removeRecipe} ist extrem teuer (voller
     * Rezept-/Advancement-Reload) und würde den Server zur Laufzeit – etwa beim
     * Umschalten eines Moduls – einfrieren lassen.
     */
    private boolean ensureAbsent(NamespacedKey key) {
        if (Bukkit.getRecipe(key) != null) {
            if (!registered.contains(key)) registered.add(key);
            return false;
        }
        return true;
    }

    public void registerAll() {
        if (!plugin.getConfig().getBoolean("upgrades.enabled", true)) {
            plugin.getLogger().info("Upgrade-System deaktiviert (config: upgrades.enabled).");
            return;
        }
        if (!plugin.pluginConfig().recipesEnabled()) {
            return;
        }
        buildUpgradeItems();

        int count = 0;
        // a) Upgrade-Leder: Leder mittig, von Faden umgeben.
        try {
            if (ensureAbsent(key("upgrade_base"))) {
                ShapedRecipe base = new ShapedRecipe(key("upgrade_base"), baseItem);
                base.shape(" S ", "SLS", " S ");
                base.setIngredient('S', Material.STRING);
                base.setIngredient('L', Material.LEATHER);
                base.setGroup("yourshika_upgrades");
                Bukkit.addRecipe(base);
                registered.add(base.getKey());
                count++;
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Rezept 'upgrade_base' fehlerhaft: " + ex.getMessage());
        }

        // b) Tier-Upgrades.
        List<String> order = tiers.keys();
        for (int i = 1; i < order.size(); i++) {
            String target = order.get(i);
            // Dragon nutzt eine eigene, mehrstufige Kette (siehe unten) statt des
            // generischen "8x Material"-Rezepts.
            if (target.equalsIgnoreCase("dragon")) continue;
            ItemStack result = upgradeItems.get(target);
            if (result == null) continue;
            if (!ensureAbsent(key("upgrade_" + target))) continue;
            Material mat = tierMaterial(target);
            try {
                if (mat == Material.NETHERITE_INGOT || target.equalsIgnoreCase("netherite")) {
                    // Smithing Table: Upgrade-Leder (Vorlage) + Basis-Material
                    // + Zugabe-Material (beide per Config einstellbar).
                    SmithingTransformRecipe r = new SmithingTransformRecipe(
                            key("upgrade_" + target), result,
                            new RecipeChoice.ExactChoice(baseItem),                 // Vorlage
                            new RecipeChoice.MaterialChoice(netheriteBaseMaterial(mat)),   // Basis
                            new RecipeChoice.MaterialChoice(netheriteAdditionMaterial())); // Zugabe
                    Bukkit.addRecipe(r);
                    registered.add(r.getKey());
                } else {
                    // Tier-Material um das Upgrade-Leder (Muster per Config einstellbar).
                    ShapedRecipe r = new ShapedRecipe(key("upgrade_" + target), result);
                    r.shape(tierRecipeShape().toArray(new String[0]));
                    r.setIngredient('M', new RecipeChoice.MaterialChoice(mat));
                    r.setIngredient('U', new RecipeChoice.ExactChoice(baseItem));
                    r.setGroup("yourshika_upgrades");
                    Bukkit.addRecipe(r);
                    registered.add(r.getKey());
                }
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Tier-Upgrade-Rezept '" + target + "' fehlerhaft: " + ex.getMessage());
            }
        }

        // b2) Dragon-Endgame-Kette (mehrstufig), falls das Dragon-Tier existiert und
        //     aktiviert ist: Stufe 1 = Dragon Core, Stufe 2 = Dragon-Tier-Upgrade.
        if (tiers.exists("dragon") && plugin.getConfig().getBoolean("upgrades.dragon.enabled", true)) {
            count += registerDragonChain();
        }

        // c) Smithing-Veredelung je Ziel-Tier.
        for (int i = 1; i < order.size(); i++) {
            String target = order.get(i);
            BackpackTier targetTier = tiers.get(target);
            ItemStack upgrade = upgradeItems.get(target);
            if (targetTier == null || upgrade == null) continue;
            if (!ensureAbsent(key("smith_" + target))) continue;
            try {
                ItemStack placeholder = backpacks.createTemplate(targetTier);
                SmithingTransformRecipe r = new SmithingTransformRecipe(
                        key("smith_" + target),
                        placeholder,
                        new RecipeChoice.MaterialChoice(smithingTemplateMaterial()),      // Template
                        new RecipeChoice.MaterialChoice(Material.LEATHER_HORSE_ARMOR),    // Basis (Backpack)
                        new RecipeChoice.ExactChoice(upgrade));                           // Zugabe (Tier-Upgrade)
                Bukkit.addRecipe(r);
                registered.add(r.getKey());
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Smithing-Rezept '" + target + "' fehlerhaft: " + ex.getMessage());
            }
        }

        plugin.getLogger().info("Upgrade-Rezepte registriert: " + count);
    }

    /**
     * Baut die kanonischen Upgrade-Items neu (z.B. nach dem Live-Umschalten des
     * Oraxen-Moduls), damit ihre Modell-Komponenten den aktuellen Hook-Status
     * widerspiegeln. Rezepte werden bewusst NICHT angefasst.
     */
    public void rebuildItems() {
        buildUpgradeItems();
    }

    private void buildUpgradeItems() {
        upgradeItems.clear();
        // Upgrade-Leder mit konfigurierbarem Modell.
        int baseCmd = plugin.getConfig().getInt("upgrades.models.base.custom-model-data",
                UpgradeItemFactory.BASE_CMD);
        String baseModel = plugin.getConfig().getString("upgrades.models.base.item-model", "");
        String baseProviderId = plugin.getConfig().getString("upgrades.models.base.provider-id", "");
        this.baseItem = upgrades.base(baseCmd, baseModel, baseProviderId);

        List<String> order = tiers.keys();
        for (int i = 1; i < order.size(); i++) {
            String target = order.get(i);
            BackpackTier tier = tiers.get(target);
            String hex = tier == null ? "FFFFFF" : ColorUtil.hex6(tier.defaultMainColor(), "FFFFFF");
            String name = "<#" + hex + "><bold>" + capitalize(target) + " Upgrade</bold></#" + hex + ">";
            int cmd = plugin.getConfig().getInt("upgrades.models." + target + ".custom-model-data",
                    UpgradeItemFactory.BASE_CMD + i);
            String model = plugin.getConfig().getString("upgrades.models." + target + ".item-model", "");
            String providerId = plugin.getConfig().getString("upgrades.models." + target + ".provider-id", "");
            upgradeItems.put(target, upgrades.tierUpgrade(target, name, cmd, model, providerId));
        }

        // Dragon Core (Zwischenprodukt der Dragon-Kette).
        int coreCmd = plugin.getConfig().getInt("upgrades.dragon.core-model.custom-model-data", 2160);
        String coreModel = plugin.getConfig().getString("upgrades.dragon.core-model.item-model", "");
        String coreProvider = plugin.getConfig().getString("upgrades.dragon.core-model.provider-id", "ysbp_dragon_core");
        this.dragonCoreItem = upgrades.dragonCore(coreCmd, coreModel, coreProvider);
    }

    /** Kanonischer Dragon Core (für GUI / Give-Befehle). */
    public ItemStack dragonCoreItem() {
        return dragonCoreItem == null ? null : dragonCoreItem.clone();
    }

    /**
     * Registriert die mehrstufige Dragon-Kette:
     * Stufe 1 (Crafting): End-Materialien -> Dragon Core.
     * Stufe 2 (Crafting): Dragon Core ('C') + Upgrade-Leder ('U') + Zutaten -> Dragon-Tier-Upgrade.
     * Stufe 3 (Smithing) wird generisch registriert (Netherite-Backpack + Dragon-Upgrade).
     */
    private int registerDragonChain() {
        int count = 0;
        // Stufe 1: Dragon Core.
        if (dragonCoreItem != null && ensureAbsent(key("dragon_core"))) {
            try {
                ShapedRecipe r = new ShapedRecipe(key("dragon_core"), dragonCoreItem);
                List<String> shape = configShape("upgrades.dragon.core-recipe.shape",
                        List.of("ESE", "SNS", "EDE"));
                r.shape(shape.toArray(new String[0]));
                String shapeStr = String.join("", shape);
                Map<Character, Material> ing = configIngredients("upgrades.dragon.core-recipe.ingredients",
                        Map.of('E', Material.ECHO_SHARD, 'S', Material.SHULKER_SHELL,
                                'N', Material.NETHER_STAR, 'D', Material.DRAGON_HEAD));
                for (Map.Entry<Character, Material> e : ing.entrySet()) {
                    if (shapeStr.indexOf(e.getKey()) < 0) continue;
                    r.setIngredient(e.getKey(), new RecipeChoice.MaterialChoice(e.getValue()));
                }
                r.setGroup("yourshika_upgrades");
                Bukkit.addRecipe(r);
                registered.add(r.getKey());
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Rezept 'dragon_core' fehlerhaft: " + ex.getMessage());
            }
        }
        // Stufe 2: Dragon-Tier-Upgrade. 'U' = Upgrade-Leder, 'C' = Dragon Core (beide ExactChoice).
        ItemStack dragonUpgrade = upgradeItems.get("dragon");
        if (dragonUpgrade != null && dragonCoreItem != null && ensureAbsent(key("upgrade_dragon"))) {
            try {
                ShapedRecipe r = new ShapedRecipe(key("upgrade_dragon"), dragonUpgrade);
                List<String> shape = configShape("upgrades.dragon.upgrade-recipe.shape",
                        List.of(" B ", "CUC", " B "));
                r.shape(shape.toArray(new String[0]));
                String shapeStr = String.join("", shape);
                Map<Character, Material> ing = configIngredients("upgrades.dragon.upgrade-recipe.ingredients",
                        Map.of('B', Material.DRAGON_BREATH));
                for (Map.Entry<Character, Material> e : ing.entrySet()) {
                    if (e.getKey() == 'U' || e.getKey() == 'C') continue; // reserviert
                    if (shapeStr.indexOf(e.getKey()) < 0) continue;
                    r.setIngredient(e.getKey(), new RecipeChoice.MaterialChoice(e.getValue()));
                }
                if (shapeStr.indexOf('U') >= 0) r.setIngredient('U', new RecipeChoice.ExactChoice(baseItem));
                if (shapeStr.indexOf('C') >= 0) r.setIngredient('C', new RecipeChoice.ExactChoice(dragonCoreItem));
                r.setGroup("yourshika_upgrades");
                Bukkit.addRecipe(r);
                registered.add(r.getKey());
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Rezept 'upgrade_dragon' fehlerhaft: " + ex.getMessage());
            }
        }
        return count;
    }

    private List<String> configShape(String path, List<String> def) {
        List<String> shape = plugin.getConfig().getStringList(path);
        return (shape == null || shape.isEmpty()) ? def : shape;
    }

    private Map<Character, Material> configIngredients(String path, Map<Character, Material> def) {
        var sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return def;
        Map<Character, Material> map = new HashMap<>();
        for (String k : sec.getKeys(false)) {
            if (k.isEmpty()) continue;
            Material m = Material.matchMaterial(sec.getString(k, ""));
            if (m != null) map.put(k.charAt(0), m);
        }
        return map.isEmpty() ? def : map;
    }

    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Throwable ignored) {
            }
        }
        registered.clear();
    }

    /** Alle registrierten Rezept-Schlüssel (für die Recipe-Book-Freischaltung). */
    public List<NamespacedKey> keys() {
        return List.copyOf(registered);
    }

    /** Kanonisches Tier-Upgrade-Item (für GUI / Give-Befehle). */
    public ItemStack upgradeItem(String target) {
        ItemStack item = upgradeItems.get(target);
        return item == null ? null : item.clone();
    }

    /** Kanonisches Upgrade-Leder (für GUI). Baut es bei Bedarf einmalig auf. */
    public ItemStack baseUpgradeItem() {
        if (baseItem == null) {
            int baseCmd = plugin.getConfig().getInt("upgrades.models.base.custom-model-data",
                    UpgradeItemFactory.BASE_CMD);
            String baseModel = plugin.getConfig().getString("upgrades.models.base.item-model", "");
            String baseProvider = plugin.getConfig().getString("upgrades.models.base.provider-id", "");
            this.baseItem = upgrades.base(baseCmd, baseModel, baseProvider);
        }
        return baseItem.clone();
    }

    /** Benötigtes Material eines Tier-Upgrades (für die Rezept-Anzeige). */
    public Material materialFor(String target) {
        return tierMaterial(target);
    }

    /** Ist {@code target} das letzte Tier (Netherite-Upgrade per Smithing)? */
    public boolean isSmithingUpgrade(String target) {
        return tierMaterial(target) == Material.NETHERITE_INGOT || target.equalsIgnoreCase("netherite");
    }

    /**
     * Berechnet das Smithing-Ergebnis dynamisch und validiert die Tier-Kette.
     * So bleiben ID, Inhalt und Farbe des Backpacks erhalten.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingUpgrade upgrade = computeSmithingUpgrade(event.getInventory(), event.getViewers(), false);
        if (upgrade == null) {
            if (hasTierUpgradeAddition(event.getInventory())) event.setResult(null);
            return;
        }
        event.setResult(upgrade.result());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        SmithingUpgrade upgrade = computeSmithingUpgrade(event.getInventory(), event.getViewers(), true);
        if (upgrade == null) {
            if (hasTierUpgradeAddition(event.getInventory())) event.setCancelled(true);
            return;
        }
        event.setCurrentItem(upgrade.result());
        saveSmithingUpgrade(upgrade);
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player p && plugin.achievements() != null) {
            plugin.achievements().trigger(p, "smithing_up");
        }
    }

    private boolean hasTierUpgradeAddition(SmithingInventory inv) {
        return upgrades.getUpgradeTarget(inv.getItem(2)) != null;
    }

    private SmithingUpgrade computeSmithingUpgrade(SmithingInventory inv,
                                                   List<HumanEntity> viewers,
                                                   boolean assignMissingId) {
        ItemStack template = inv.getItem(0);
        ItemStack base = inv.getItem(1);
        ItemStack addition = inv.getItem(2);

        String target = upgrades.getUpgradeTarget(addition);
        if (target == null) return null;                          // nicht unser Rezept -> Vanilla unberührt
        if (!backpacks.isBackpack(base)) return null;              // unsere Zugabe, aber Basis ist kein Backpack
        if (template == null || template.getType() != smithingTemplateMaterial()
                || upgrades.isUpgradeBase(template)) return null;  // Vorlage muss das Template-Material sein

        BackpackTier targetTier = tiers.get(target);
        if (targetTier == null) return null;

        String baseTierKey = backpacks.getTierKey(base);
        String expectedFrom = previousTier(target);
        if (expectedFrom == null || !expectedFrom.equalsIgnoreCase(baseTierKey)) return null;

        String perm = "yourshika.backpack.craft." + target;
        for (HumanEntity viewer : viewers) {
            if (!viewer.hasPermission(perm)) return null;
        }

        UUID id = backpacks.getId(base);
        if (id == null && assignMissingId) {
            id = UUID.randomUUID();
            backpacks.writeId(base, id);
        }

        BackpackTier prevTier = tiers.get(expectedFrom);
        String prevMain = prevTier != null ? prevTier.defaultMainColor() : targetTier.defaultMainColor();
        String prevAccent = prevTier != null ? prevTier.defaultAccentColor() : targetTier.defaultAccentColor();

        String baseMain = backpacks.getMainColor(base, prevMain);
        String baseAccent = backpacks.getAccentColor(base, prevAccent);

        String main = isSameColor(baseMain, prevMain) ? targetTier.defaultMainColor() : baseMain;
        String accent = isSameColor(baseAccent, prevAccent) ? targetTier.defaultAccentColor() : baseAccent;

        ItemStack result = backpacks.create(targetTier, id, main, accent);
        // Custom-Namen und Besitzer des bisherigen Backpacks übernehmen, damit sie
        // bei der Tier-Veredelung nicht verloren gehen.
        backpacks.writeName(result, backpacks.getCustomName(base));
        UUID ownerUuid = backpacks.getOwner(base);
        if (ownerUuid != null) {
            backpacks.writeOwner(result, ownerUuid, backpacks.getOwnerName(base));
        }
        backpacks.applyDisplay(result, targetTier, id, main, accent);
        return new SmithingUpgrade(id, target, main, accent, result);
    }

    private void saveSmithingUpgrade(SmithingUpgrade upgrade) {
        UUID id = upgrade.id();
        if (id == null) return;
        BackpackData data = manager.storage().load(id);
        if (data == null) return;
        boolean changed = false;
        if (!upgrade.target().equalsIgnoreCase(data.tier())) { data.tier(upgrade.target()); changed = true; }
        if (!upgrade.main().equals(data.mainColor())) { data.mainColor(upgrade.main()); changed = true; }
        if (!upgrade.accent().equals(data.accentColor())) { data.accentColor(upgrade.accent()); changed = true; }
        if (changed) manager.storage().save(data);
    }

    /** Vergleicht zwei Farb-Tokens unabhängig von Schreibweise/Hex-Format. */
    private boolean isSameColor(String a, String b) {
        if (a == null || b == null) return false;
        return ColorUtil.normalize(a, a).equalsIgnoreCase(ColorUtil.normalize(b, b));
    }

    private String previousTier(String target) {
        List<String> order = tiers.keys();
        int idx = order.indexOf(target.toLowerCase());
        if (idx <= 0) return null;
        return order.get(idx - 1);
    }

    private Material tierMaterial(String target) {
        String def = switch (target.toLowerCase()) {
            case "copper" -> "COPPER_INGOT";
            case "iron" -> "IRON_INGOT";
            case "gold" -> "GOLD_INGOT";
            case "diamond" -> "DIAMOND";
            case "emerald" -> "EMERALD";
            case "netherite" -> "NETHERITE_INGOT";
            default -> "IRON_INGOT";
        };
        String configured = plugin.getConfig().getString("upgrades.materials." + target.toLowerCase(), def);
        Material m = Material.matchMaterial(configured);
        return m == null ? Material.matchMaterial(def) : m;
    }

    /** Template-Material der Backpack-Veredelung (Config, Standard: Leder). */
    private Material smithingTemplateMaterial() {
        Material m = Material.matchMaterial(
                plugin.getConfig().getString("upgrades.smithing.template-material", "LEATHER"));
        return m == null ? Material.LEATHER : m;
    }

    /** Basis-Material des Netherite-Tier-Upgrades (Config, Standard: Tier-Material). */
    private Material netheriteBaseMaterial(Material fallback) {
        Material m = Material.matchMaterial(
                plugin.getConfig().getString("upgrades.smithing.netherite.base-material", fallback.name()));
        return m == null ? fallback : m;
    }

    /** Zugabe-Material des Netherite-Tier-Upgrades (Config, Standard: Faden). */
    private Material netheriteAdditionMaterial() {
        Material m = Material.matchMaterial(
                plugin.getConfig().getString("upgrades.smithing.netherite.addition-material", "STRING"));
        return m == null ? Material.STRING : m;
    }

    /** Crafting-Muster der Tier-Upgrades (Config, Standard: 8x Material um das Leder). */
    private List<String> tierRecipeShape() {
        List<String> shape = plugin.getConfig().getStringList("upgrades.tier-recipe.shape");
        return (shape == null || shape.size() != 3) ? List.of("MMM", "MUM", "MMM") : shape;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record SmithingUpgrade(UUID id, String target, String main, String accent, ItemStack result) {
    }
}
