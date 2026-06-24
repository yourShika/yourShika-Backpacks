package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Bringt beim Beitritt alle Plugin-Items im Inventar eines Spielers auf den
 * aktuellen Stand (Backpacks + Upgrade-Items). So aktualisieren sich vorhandene
 * Items nach einem Plugin-Update automatisch und „funktionieren" wieder, ohne dass
 * alte Stände hängen bleiben.
 */
public final class ItemUpdateListener implements Listener {

    private final YourShikaBackpacks plugin;

    public ItemUpdateListener(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Einen Tick warten, damit das Inventar vollständig geladen ist.
        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.refreshPlayerItems(event.getPlayer()));
    }
}
