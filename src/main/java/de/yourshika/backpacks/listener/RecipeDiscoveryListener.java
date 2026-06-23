package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Schaltet alle Plugin-Rezepte (Backpacks, Upgrade-Items, Smithing-Veredelung)
 * für jeden Spieler frei. Dadurch erscheinen sie im <b>vanilla Recipe Book</b>.
 *
 * <p>Da alle Rezepte als echte (Shaped-/Shapeless-/Smithing-)Rezepte registriert
 * und an die Clients synchronisiert werden, sind sie zusätzlich in
 * Rezept-Anzeigern wie <b>JEI/REI/EMI</b> sichtbar. Das positionsunabhängige
 * Dye-Färben ist bewusst kein festes Rezept und erscheint daher nicht dort.</p>
 */
public final class RecipeDiscoveryListener implements Listener {

    private final YourShikaBackpacks plugin;

    public RecipeDiscoveryListener(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        discover(event.getPlayer());
    }

    public void discover(Player player) {
        List<NamespacedKey> keys = new ArrayList<>();
        if (plugin.recipeManager() != null) keys.addAll(plugin.recipeManager().keys());
        if (plugin.upgradeManager() != null) keys.addAll(plugin.upgradeManager().keys());
        if (plugin.functionUpgrades() != null) keys.addAll(plugin.functionUpgrades().keys());
        if (keys.isEmpty()) return;
        try {
            player.discoverRecipes(keys);
        } catch (Throwable ignored) {
            // Recipe-Book-Freischaltung ist optional – niemals fatal.
        }
    }
}
