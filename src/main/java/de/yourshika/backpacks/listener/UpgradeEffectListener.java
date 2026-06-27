package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Setzt die funktionalen Upgrades um, die auf Events reagieren:
 *
 * <ul>
 *   <li><b>Pickup / Advanced Pickup</b>: aufgesammelte Items wandern direkt in
 *       den Rucksack.</li>
 *   <li><b>Everlasting</b>: ein als Item liegender Rucksack ist immun gegen
 *       Feuer/Lava/Explosionen und despawnt nicht.</li>
 * </ul>
 *
 * <p>Magnet/Advanced Magnet laufen als Wiederhol-Task ({@link UpgradeMagnetTask}).</p>
 */
public final class UpgradeEffectListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final BackpackItemFactory items;

    public UpgradeEffectListener(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = manager.items();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item entity = event.getItem();
        ItemStack stack = entity.getItemStack();
        if (items.isBackpack(stack)) return; // Backpacks selbst nicht einsaugen.

        ItemStack work = stack.clone();
        if (!manager.depositItemWithFunction(player, work,
                java.util.Set.of("pickup", "advanced_pickup"))) return; // nichts ging rein -> normal aufsammeln

        event.setCancelled(true);
        // Feedback-Sound, da der Vanilla-Aufsammel-Sound durch das Canceln entfällt.
        de.yourshika.backpacks.util.Sounds.play(plugin, player, "pickup");
        if (work.getAmount() <= 0) {
            entity.remove();
        } else {
            entity.setItemStack(work); // Rest liegen lassen
        }
        plugin.achievements().trigger(player, "pickup");
    }

    // --- Everlasting -------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item entity = event.getEntity();
        if (!isEverlasting(entity.getItemStack())) return;
        entity.setInvulnerable(true);
        try {
            entity.setUnlimitedLifetime(true);
        } catch (Throwable ignored) {
            // ältere API ohne setUnlimitedLifetime – Despawn dann nicht verhindert.
        }
        entity.setWillAge(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && isEverlasting(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item && isEverlasting(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    private boolean isEverlasting(ItemStack stack) {
        if (!items.isBackpack(stack)) return false;
        java.util.UUID id = items.getId(stack);
        return id != null && manager.functionUpgradesOf(id).contains("everlasting");
    }
}
