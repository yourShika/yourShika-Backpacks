package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Setzt das <b>Soulbound-Upgrade</b> um: ein Rucksack mit diesem Upgrade wird beim
 * Tod <b>nicht gedroppt</b>, sondern bleibt beim Spieler und ist nach dem Respawn
 * wieder im Inventar.
 *
 * <h3>Dupe-Schutz (wichtig wegen Grave-/Deadbody-Plugins)</h3>
 * <ul>
 *   <li>Der Death-Handler läuft auf {@link EventPriority#LOWEST} und entfernt die
 *       Soulbound-Rucksäcke aus {@link PlayerDeathEvent#getDrops()} <em>bevor</em>
 *       die meisten Grave-Plugins die Drop-Liste einlesen – so landet der Rucksack
 *       nicht zusätzlich in einem Grab.</li>
 *   <li>Der Inhalt eines Rucksacks liegt ohnehin server-seitig anhand seiner ID
 *       (nicht im Item) – selbst ein versehentliches zweites Item würde denselben
 *       Speicher öffnen, es gibt also keinen Inhalts-Dupe.</li>
 *   <li>Die zwischengespeicherten Items werden pro Spieler genau einmal gehalten
 *       und beim Respawn (oder Wieder-Beitritt, falls im Tod-Zustand neu verbunden)
 *       ausgegeben und sofort gelöscht (Instanz-Guard gegen Doppel-Restore).</li>
 * </ul>
 */
public final class SoulboundDeathListener implements Listener {

    private static final String SOULBOUND = "soulbound";

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final BackpackItemFactory items;

    /** Pro Spieler zwischengelagerte Soulbound-Rucksäcke bis zum Respawn. */
    private final Map<UUID, List<ItemStack>> stash = new ConcurrentHashMap<>();

    public SoulboundDeathListener(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = manager.items();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        // Bei keepInventory bleiben die Items ohnehin im Inventar -> nichts zu tun.
        if (event.getKeepInventory()) return;

        Player player = event.getEntity();
        List<ItemStack> kept = new ArrayList<>();
        for (var it = event.getDrops().iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (!isSoulbound(drop)) continue;
            kept.add(drop.clone());
            it.remove(); // aus den Todes-Drops nehmen, bevor Grave-Plugins zugreifen
        }
        if (kept.isEmpty()) return;

        // Bereits vorhandene Zwischenlagerung ergänzen (Mehrfachtod ohne Respawn).
        stash.merge(player.getUniqueId(), kept, (a, b) -> { a.addAll(b); return a; });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        giveBack(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Sicherheitsnetz: Relog im Tod-Zustand -> beim Beitritt ausgeben.
        if (stash.containsKey(event.getPlayer().getUniqueId())) {
            giveBack(event.getPlayer());
        }
    }

    private void giveBack(Player player) {
        List<ItemStack> kept = stash.remove(player.getUniqueId());
        if (kept == null || kept.isEmpty()) return;
        // Einen Tick warten, damit Inventar/Respawn vollständig sind.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (ItemStack item : kept) {
                for (ItemStack rest : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rest);
                }
            }
        });
    }

    private boolean isSoulbound(ItemStack stack) {
        if (!items.isBackpack(stack)) return false;
        UUID id = items.getId(stack);
        return id != null && manager.functionUpgradesOf(id).contains(SOULBOUND);
    }
}
