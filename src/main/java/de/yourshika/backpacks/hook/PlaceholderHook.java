package de.yourshika.backpacks.hook;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.BackpackMenuHolder;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.tier.TierRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

/**
 * Optionale PlaceholderAPI-Integration. Wird nur instanziiert, wenn PAPI
 * installiert ist – fehlt die Library, wird diese Klasse nie geladen.
 *
 * Platzhalter:
 *   %ysbp_count%         – Anzahl Backpacks des Spielers
 *   %ysbp_highest_tier%  – höchster Tier des Spielers
 *   %ysbp_open%          – aktuell geöffneter Backpack (Kurz-ID oder "-")
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final TierRegistry tiers;

    public PlaceholderHook(YourShikaBackpacks plugin, BackpackManager manager, TierRegistry tiers) {
        this.plugin = plugin;
        this.manager = manager;
        this.tiers = tiers;
    }

    @Override
    public String getIdentifier() {
        return "ysbp";
    }

    @Override
    public String getAuthor() {
        return "yourShika";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        List<UUID> ids = manager.storage().listByOwner(player.getUniqueId());
        switch (params.toLowerCase()) {
            case "count" -> {
                return String.valueOf(ids.size());
            }
            case "highest_tier" -> {
                return highestTier(ids);
            }
            case "open" -> {
                if (!(player instanceof Player online)) return "-";
                Inventory top = online.getOpenInventory().getTopInventory();
                if (top.getHolder() instanceof BackpackMenuHolder holder) {
                    return holder.backpackId().toString().substring(0, 8);
                }
                return "-";
            }
            default -> {
                return null;
            }
        }
    }

    private String highestTier(List<UUID> ids) {
        List<String> order = tiers.keys();
        int best = -1;
        for (UUID id : ids) {
            BackpackData data = manager.storage().load(id);
            if (data == null) continue;
            int idx = order.indexOf(data.tier());
            if (idx > best) best = idx;
        }
        return best < 0 ? "-" : order.get(best);
    }
}
