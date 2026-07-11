package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hintergrund-Task für die passiven Funktions-Upgrades:
 * <ul>
 *   <li><b>Auto-Restock</b> – füllt einen leergewordenen Hotbar-/Inventar-Slot
 *       automatisch mit demselben Item aus dem Rucksack nach.</li>
 *   <li><b>Feeding</b> – isst bei Hunger automatisch Nahrung aus dem Rucksack
 *       (mit Ess-Sound).</li>
 * </ul>
 * Läuft im Sekundentakt und ist bewusst günstig: nur Spieler mit dem jeweiligen
 * Upgrade lösen Arbeit aus, geöffnete Rucksäcke werden übersprungen (der offene
 * Puffer ist dann maßgeblich).
 */
public final class UpgradeUtilityTask extends BukkitRunnable {

    private static final int FEED_THRESHOLD = 16;      // ab diesem Sättigungslevel füttern
    private static final long FEED_COOLDOWN_MS = 1500L; // Ess-Sound/Frequenz drosseln

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;

    /** Letzter bekannter Zustand der 36 Haupt-Inventar-Slots je Spieler (Auto-Restock). */
    private final Map<UUID, ItemStack[]> lastInv = new HashMap<>();
    private final Map<UUID, Long> lastFeed = new HashMap<>();

    public UpgradeUtilityTask(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            autoRestock(player);
            feeding(player);
        }
        // Speicher für ausgeloggte Spieler freigeben.
        lastInv.keySet().removeIf(id -> plugin.getServer().getPlayer(id) == null);
        lastFeed.keySet().removeIf(id -> plugin.getServer().getPlayer(id) == null);
    }

    private void autoRestock(Player player) {
        UUID id = manager.firstCarriedBackpackWith(player, "auto_restock");
        if (id == null) {
            lastInv.remove(player.getUniqueId());
            return;
        }
        ItemStack[] current = player.getInventory().getStorageContents();
        ItemStack[] prev = lastInv.get(player.getUniqueId());
        boolean changed = false;
        if (prev != null) {
            int n = Math.min(prev.length, current.length);
            for (int i = 0; i < n; i++) {
                ItemStack before = prev[i];
                if (before == null || before.getType().isAir()) continue;
                // Nur nachfüllen, wenn der Slot komplett leer wurde (nicht getauscht).
                if (current[i] != null && !current[i].getType().isAir()) continue;
                if (manager.items().isBackpack(before)) continue; // keine Backpacks nachziehen
                ItemStack refill = manager.withdrawMatching(id, before, before.getMaxStackSize());
                if (refill != null) {
                    current[i] = refill;
                    changed = true;
                }
            }
        }
        if (changed) player.getInventory().setStorageContents(current);
        // Zustand NACH dem Nachfüllen merken, damit derselbe Slot nicht erneut auslöst.
        lastInv.put(player.getUniqueId(), cloneSnapshot(player.getInventory().getStorageContents()));
    }

    private void feeding(Player player) {
        if (player.getFoodLevel() > FEED_THRESHOLD) return;
        long now = System.currentTimeMillis();
        Long last = lastFeed.get(player.getUniqueId());
        if (last != null && now - last < FEED_COOLDOWN_MS) return;

        UUID id = manager.firstCarriedBackpackWith(player, "feeding");
        if (id == null) return;
        ItemStack food = manager.withdrawFood(id);
        if (food == null) return;

        int nutrition = 4;
        float saturation = 2.4f;
        ItemMeta meta = food.getItemMeta();
        if (meta != null && meta.hasFood()) {
            nutrition = meta.getFood().getNutrition();
            saturation = meta.getFood().getSaturation();
        }
        int newFood = Math.min(20, player.getFoodLevel() + nutrition);
        player.setFoodLevel(newFood);
        player.setSaturation(Math.min(newFood, player.getSaturation() + saturation));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.7f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.5f, 1.1f);
        lastFeed.put(player.getUniqueId(), now);
    }

    private static ItemStack[] cloneSnapshot(ItemStack[] src) {
        ItemStack[] copy = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i] == null ? null : src[i].clone();
        }
        return copy;
    }
}
