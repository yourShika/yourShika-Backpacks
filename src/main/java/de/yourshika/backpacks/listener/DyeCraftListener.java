package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Färbt Backpacks direkt im Crafting Table mit Färbemitteln (Dye) ein.
 *
 * <p>Layout (3×3): Backpack in der mittleren Spalte, Färbemittel in der
 * <b>linken</b> Spalte ergeben die <b>Hauptfarbe</b>, in der <b>rechten</b>
 * Spalte die <b>Akzentfarbe</b>. Mehrere Färbemittel je Spalte werden zu einem
 * Hex-Wert gemischt. Innerhalb einer Spalte ist die Position egal.</p>
 *
 * <p>Da das Ergebnis dynamisch ist (kein festes Rezept), wird es in
 * {@link PrepareItemCraftEvent} angezeigt und das eigentliche Craften manuell
 * abgesichert. Der bestehende Backpack-ID bleibt erhalten – damit bleiben auch
 * alle Inhalte unverändert erhalten.</p>
 */
public final class DyeCraftListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackItemFactory items;

    private static final int[] LEFT = {0, 3, 6};
    private static final int[] MIDDLE = {1, 4, 7};
    private static final int[] RIGHT = {2, 5, 8};

    public DyeCraftListener(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.items = plugin.itemFactory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        ItemStack[] matrix = inv.getMatrix();
        if (matrix.length < 9) return; // nur am Crafting Table (3x3)
        ItemStack result = compute(matrix);
        if (result != null) {
            inv.setResult(result);
        }
    }

    // Sicherheitsnetz, falls die Vanilla-Verarbeitung doch greift.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        ItemStack[] matrix = inv.getMatrix();
        if (matrix.length < 9) return;
        if (compute(matrix) != null) return; // gültiges Dye-Crafting wird im Klick-Handler verarbeitet
        if (containsBackpack(matrix) && event.getRecipe() != null) {
            // Verhindert, dass ein zufällig passendes Vanilla-Rezept greift.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof CraftingInventory inv)) return;
        if (event.getRawSlot() != 0) return; // nur der Ergebnis-Slot
        ItemStack[] matrix = inv.getMatrix();
        if (matrix.length < 9) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = compute(matrix);
        if (result == null) return; // kein Färbe-Vorgang -> Vanilla wie gehabt

        event.setCancelled(true);

        // 1) Zutaten verbrauchen (je ein Stück aus jedem beteiligten Slot).
        for (int i = 0; i < 9; i++) {
            ItemStack it = matrix[i];
            if (it == null || it.getType().isAir()) continue;
            int amt = it.getAmount() - 1;
            matrix[i] = amt <= 0 ? null : it.clone();
            if (matrix[i] != null) matrix[i].setAmount(amt);
        }
        inv.setMatrix(matrix);

        // 2) Ergebnis ausgeben: auf den Cursor, wenn frei – sonst ins Inventar.
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            event.getView().setCursor(result);
        } else {
            player.getInventory().addItem(result).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }

        // 3) Farben server-seitig (DB) nachziehen – darf den Vorgang nie stören.
        try {
            UUID id = items.getId(result);
            if (id != null) {
                de.yourshika.backpacks.storage.BackpackData data = plugin.manager().storage().load(id);
                if (data != null) {
                    data.mainColor(items.getMainColor(result, data.mainColor()));
                    data.accentColor(items.getAccentColor(result, data.accentColor()));
                    plugin.manager().storage().save(data);
                }
            }
        } catch (Exception ignored) {
        }

        // 4) Client im nächsten Tick synchronisieren (verhindert Slot-Desync).
        org.bukkit.Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    /**
     * Berechnet das gefärbte Backpack-Ergebnis aus der Matrix oder null, wenn es
     * sich nicht um einen gültigen Färbe-Vorgang handelt.
     */
    private ItemStack compute(ItemStack[] matrix) {
        // Genau ein Backpack, ausschließlich in der mittleren Spalte.
        ItemStack backpack = null;
        for (int idx : MIDDLE) {
            ItemStack it = matrix[idx];
            if (it == null || it.getType().isAir()) continue;
            if (!items.isBackpack(it)) return null;
            if (backpack != null) return null;
            backpack = it;
        }
        if (backpack == null || backpack.getAmount() != 1) return null;

        List<String> mainTokens = collectDyes(matrix, LEFT);
        List<String> accentTokens = collectDyes(matrix, RIGHT);
        if (mainTokens == null || accentTokens == null) return null; // Fremd-Item in Spalte
        if (mainTokens.isEmpty() && accentTokens.isEmpty()) return null; // nichts zu färben

        String tierKey = items.getTierKey(backpack);
        BackpackTier tier = plugin.tiers().get(tierKey);
        if (tier == null) return null;

        String main = items.getMainColor(backpack, tier.defaultMainColor());
        String accent = items.getAccentColor(backpack, tier.defaultAccentColor());
        if (!mainTokens.isEmpty()) {
            String blended = ColorUtil.blendToHex(mainTokens);
            if (blended != null) main = blended;
        }
        if (!accentTokens.isEmpty()) {
            String blended = ColorUtil.blendToHex(accentTokens);
            if (blended != null) accent = blended;
        }

        ItemStack result = backpack.clone();
        UUID id = items.getId(result);
        items.writeColors(result, main, accent);
        items.applyDisplay(result, tier, id, main, accent);
        return result;
    }

    /**
     * Sammelt die Dye-Farb-Tokens einer Spalte. Gibt null zurück, wenn die
     * Spalte ein Nicht-Dye-/Nicht-Leer-Item enthält.
     */
    private List<String> collectDyes(ItemStack[] matrix, int[] column) {
        List<String> tokens = new ArrayList<>();
        for (int idx : column) {
            ItemStack it = matrix[idx];
            if (it == null || it.getType().isAir()) continue;
            String token = dyeToken(it);
            if (token == null) return null;
            tokens.add(token);
        }
        return tokens;
    }

    /** Liefert den Farb-Token eines Dye-Items (z.B. RED) oder null. */
    private String dyeToken(ItemStack item) {
        String name = item.getType().name();
        if (!name.endsWith("_DYE")) return null;
        return name.substring(0, name.length() - "_DYE".length()).toUpperCase(Locale.ROOT);
    }

    private boolean containsBackpack(ItemStack[] matrix) {
        for (ItemStack it : matrix) {
            if (items.isBackpack(it)) return true;
        }
        return false;
    }
}
