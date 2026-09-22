package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pet-Booster-Upgrade (BetterPets): Sobald ein Spieler in den <b>Kampf</b> geht
 * (Schaden austeilt oder nimmt), wird – falls er das Upgrade trägt, das Feature
 * nicht abgeschaltet hat und gerade kein Booster läuft – automatisch ein im
 * Backpack liegender Pet-XP-Booster aktiviert. Ein kurzer Cooldown je Spieler
 * verhindert, dass bei jedem Treffer erneut geprüft wird.
 */
public final class PetBoosterListener implements Listener {

    private static final long COOLDOWN_MS = 3000L;

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public PetBoosterListener(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        // Spieler, der austeilt (Nahkampf oder als Schütze eines Projektils).
        if (event.getDamager() instanceof Player p) {
            attempt(p);
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            attempt(shooter);
        }
        // Spieler, der Schaden nimmt.
        if (event.getEntity() instanceof Player victim) {
            attempt(victim);
        }
    }

    private void attempt(Player player) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return;
        cooldown.put(player.getUniqueId(), now);
        manager.tryActivateStoredBooster(player);
        if (cooldown.size() > 256) {
            cooldown.keySet().removeIf(id -> plugin.getServer().getPlayer(id) == null);
        }
    }
}
