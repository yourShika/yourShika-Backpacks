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

    /**
     * Pro Tod (innerhalb einer Event-Auslieferung) aus den Drops entnommene
     * Soulbound-Rucksäcke – zwischen dem {@code LOWEST}- und dem {@code MONITOR}-
     * Handler gehalten, damit ein spät gesetztes {@code keepInventory} noch
     * berücksichtigt werden kann (Dupe-Schutz, B1).
     */
    private final Map<UUID, List<ItemStack>> pendingDeath = new ConcurrentHashMap<>();

    /** Verhindert doppelte Auslieferung (Respawn + Join im selben Tick) (B10). */
    private final java.util.Set<UUID> delivering = ConcurrentHashMap.newKeySet();

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

        // Noch NICHT in den Respawn-Stash übernehmen: erst nach allen Plugins
        // (MONITOR) entscheiden, ob keepInventory nachträglich gesetzt wurde.
        pendingDeath.merge(player.getUniqueId(), kept, (a, b) -> { a.addAll(b); return a; });
    }

    /**
     * Läuft NACH allen anderen Plugins. Hat ein Plugin (Combat/Region/Essentials)
     * {@code keepInventory} erst nach unserem {@code LOWEST}-Handler auf {@code true}
     * gesetzt, bleibt der Rucksack im Inventar des Spielers – dann darf er NICHT
     * zusätzlich zwischengelagert werden, sonst entsteht beim Respawn ein Duplikat.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathFinalize(PlayerDeathEvent event) {
        List<ItemStack> pending = pendingDeath.remove(event.getEntity().getUniqueId());
        if (pending == null || pending.isEmpty()) return;
        if (event.getKeepInventory()) {
            // Inventar wird behalten -> Rucksack ist noch im Inventar. Drops werden
            // bei keepInventory ohnehin ignoriert. Nichts zwischenlagern (Dupe-Schutz).
            return;
        }
        stash.merge(event.getEntity().getUniqueId(), pending, (a, b) -> { a.addAll(b); return a; });
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
        UUID uuid = player.getUniqueId();
        if (!stash.containsKey(uuid)) return;
        // Doppelte Auslieferung (Respawn + Join) verhindern.
        if (!delivering.add(uuid)) return;
        // Einen Tick warten, damit Inventar/Respawn vollständig sind.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                Player online = plugin.getServer().getPlayer(uuid);
                if (online == null || !online.isOnline()) {
                    // Spieler ist zwischenzeitlich offline -> im Stash lassen und beim
                    // nächsten Join erneut ausliefern (kein Item-Verlust, B10).
                    return;
                }
                // Erst JETZT aus dem Stash nehmen, da die Auslieferung garantiert läuft.
                List<ItemStack> kept = stash.remove(uuid);
                if (kept == null || kept.isEmpty()) return;
                for (ItemStack item : kept) {
                    for (ItemStack rest : online.getInventory().addItem(item).values()) {
                        online.getWorld().dropItemNaturally(online.getLocation(), rest);
                    }
                }
            } finally {
                delivering.remove(uuid);
            }
        });
    }

    private boolean isSoulbound(ItemStack stack) {
        if (!items.isBackpack(stack)) return false;
        UUID id = items.getId(stack);
        return id != null && manager.functionUpgradesOf(id).contains(SOULBOUND);
    }
}
