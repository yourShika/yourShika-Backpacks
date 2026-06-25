package de.yourshika.backpacks.craft;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registriert die konfigurierbaren Crafting-Rezepte der Backpack-Tiers und
 * erzwingt optionale Tier-Permissions beim Craften. Gecraftete Backpacks sind
 * Templates ohne ID – die eindeutige ID wird beim ersten Öffnen vergeben.
 */
public final class RecipeManager implements Listener {

    private final YourShikaBackpacks plugin;
    private final TierRegistry tiers;
    private final BackpackItemFactory items;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public RecipeManager(YourShikaBackpacks plugin, TierRegistry tiers, BackpackItemFactory items) {
        this.plugin = plugin;
        this.tiers = tiers;
        this.items = items;
    }

    public void registerAll() {
        if (!plugin.pluginConfig().recipesEnabled()) {
            plugin.getLogger().info("Crafting global deaktiviert (config: crafting.enabled).");
            return;
        }
        int count = 0;
        for (BackpackTier tier : tiers.all()) {
            BackpackTier.RecipeDefinition def = tier.recipe();
            if (def == null || !def.enabled()) continue;
            if (def.shape() == null || def.shape().isEmpty()) continue;

            // Config-Rezept validieren mit klaren Meldungen (#56).
            List<String> problems = validateRecipe(tier, def);
            if (!problems.isEmpty()) {
                plugin.getLogger().warning("Crafting-Rezept für Tier '" + tier.key() + "' übersprungen (Config-Fehler):");
                for (String p : problems) plugin.getLogger().warning("  - " + p);
                continue;
            }
            try {
                NamespacedKey key = new NamespacedKey(plugin, "backpack_" + tier.key());
                // Idempotent: bereits registrierte Rezepte NICHT erneut entfernen/anlegen.
                // Bukkit.removeRecipe ist sehr teuer (voller Rezept-/Advancement-Reload)
                // und darf nicht zur Laufzeit (z.B. beim Modul-Umschalten) laufen.
                if (Bukkit.getRecipe(key) != null) {
                    if (!registered.contains(key)) registered.add(key);
                    continue;
                }
                ItemStack result = items.createTemplate(tier);
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape(def.shape().toArray(new String[0]));
                for (Map.Entry<Character, org.bukkit.Material> e : def.ingredients().entrySet()) {
                    recipe.setIngredient(e.getKey(), e.getValue());
                }
                recipe.setGroup("yourshika_backpacks");
                Bukkit.addRecipe(recipe);
                registered.add(key);
                count++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Rezept für Tier '" + tier.key() + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Crafting-Rezepte registriert: " + count);
    }

    /** Prüft ein Config-Rezept und liefert verständliche Problembeschreibungen (#56). */
    private List<String> validateRecipe(BackpackTier tier, BackpackTier.RecipeDefinition def) {
        List<String> problems = new ArrayList<>();
        List<String> shape = def.shape();
        if (shape.size() > 3) {
            problems.add("shape hat " + shape.size() + " Reihen (erlaubt sind max. 3)");
        }
        int width = -1;
        for (String row : shape) {
            if (row == null) {
                problems.add("eine shape-Reihe ist leer/null");
                continue;
            }
            if (row.length() > 3) {
                problems.add("shape-Reihe \"" + row + "\" ist länger als 3 Zeichen");
            }
            if (width == -1) width = row.length();
            else if (row.length() != width) {
                problems.add("shape-Reihen haben unterschiedliche Längen (alle müssen gleich lang sein)");
            }
        }
        java.util.Set<Character> used = new java.util.HashSet<>();
        for (String row : shape) {
            if (row == null) continue;
            for (char c : row.toCharArray()) {
                if (c != ' ') used.add(c);
            }
        }
        for (char c : used) {
            if (!def.ingredients().containsKey(c)) {
                problems.add("Zutat '" + c + "' wird in der shape verwendet, ist aber nicht (gültig) definiert"
                        + " – prüfe den Material-Namen unter tiers." + tier.key() + ".recipe.ingredients");
            }
        }
        for (Character c : def.ingredients().keySet()) {
            if (!used.contains(c)) {
                problems.add("Zutat '" + c + "' ist definiert, kommt aber nicht in der shape vor");
            }
        }
        return problems;
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

    /**
     * Vergibt einem frisch gecrafteten Backpack sofort eine eindeutige ID und
     * aktualisiert die Lore – so steht die ID direkt nach dem Craften im Item
     * (nicht erst beim Öffnen). Bei Shift-Craft (mehrere auf einmal) bleibt es
     * beim Lazy-Verfahren: die ID wird beim ersten Öffnen vergeben, da sich ein
     * ganzer Stapel keine eindeutige ID teilen kann.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCraftBackpack(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (!items.isBackpack(result)) return;
        if (items.getId(result) != null) return;   // hat bereits eine ID
        if (event.isShiftClick()) return;           // Stapel -> Lazy-ID beim Öffnen

        String tierKey = items.getTierKey(result);
        BackpackTier tier = tiers.get(tierKey);
        if (tier == null) return;

        UUID id = UUID.randomUUID();
        items.writeId(result, id);
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player crafter) {
            items.writeOwner(result, crafter.getUniqueId(), crafter.getName());
        }
        String main = items.getMainColor(result, tier.defaultMainColor());
        String accent = items.getAccentColor(result, tier.defaultAccentColor());
        items.applyDisplay(result, tier, id, main, accent);
        event.setCurrentItem(result);
    }

    /**
     * Verhindert den Missbrauch von Upgrade-Items: Upgrade-Leder und Tier-Upgrades
     * dürfen ausschließlich in den Plugin-eigenen Upgrade-Rezepten verwendet werden
     * (z.B. NICHT als Leder für ein Buch oder als Papier für eine Karte).
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onPrepareUpgradeMisuse(PrepareItemCraftEvent event) {
        var upgrades = plugin.upgradeItems();
        if (upgrades == null) return;
        boolean hasUpgrade = false;
        for (ItemStack it : event.getInventory().getMatrix()) {
            if (upgrades.isAnyUpgrade(it)) { hasUpgrade = true; break; }
        }
        if (!hasUpgrade) return;
        // Erlaubt ist nur, wenn das Ergebnis selbst ein Upgrade-Item ist
        // (= unser Tier-Upgrade-Rezept). Alles andere wird unterbunden.
        ItemStack result = event.getInventory().getResult();
        if (result != null && !result.getType().isAir() && !upgrades.isAnyUpgrade(result)) {
            event.getInventory().setResult(null);
        }
    }

    /** Erzwingt Tier-Permissions: ohne Berechtigung verschwindet das Ergebnis. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        ItemStack result = recipe.getResult();
        if (!items.isBackpack(result)) return;

        String tierKey = items.getTierKey(result);
        if (tierKey == null) return;
        String perm = "yourshika.backpack.craft." + tierKey;

        for (HumanEntity viewer : event.getViewers()) {
            if (!viewer.hasPermission(perm)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
