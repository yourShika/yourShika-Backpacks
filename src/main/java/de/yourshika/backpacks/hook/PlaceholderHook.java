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
import org.bukkit.inventory.ItemStack;

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
 *   %ysbp_placed%        – Anzahl platzierter Backpacks
 *   %ysbp_used_slots%    – belegte Lager-Slots (Summe über alle Backpacks)
 *   %ysbp_free_slots%    – freie Lager-Slots (Summe über alle Backpacks)
 *   %ysbp_tiers%         – Anzahl unterschiedlicher Tiers im Besitz
 *   %ysbp_count_<tier>%  – Anzahl Backpacks eines bestimmten Tiers
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
            case "placed" -> {
                int placed = 0;
                for (UUID id : ids) {
                    BackpackData d = manager.storage().load(id);
                    if (d != null && d.placed()) placed++;
                }
                return String.valueOf(placed);
            }
            case "used_slots" -> {
                return String.valueOf(slotStats(ids)[0]);
            }
            case "free_slots" -> {
                int[] s = slotStats(ids);
                return String.valueOf(s[1] - s[0]);
            }
            case "tiers" -> {
                java.util.Set<String> distinct = new java.util.HashSet<>();
                for (UUID id : ids) {
                    BackpackData d = manager.storage().load(id);
                    if (d != null && d.tier() != null) distinct.add(d.tier().toLowerCase());
                }
                return String.valueOf(distinct.size());
            }
            default -> {
                if (params.toLowerCase().startsWith("count_")) {
                    String tier = params.substring("count_".length()).toLowerCase();
                    int n = 0;
                    for (UUID id : ids) {
                        BackpackData d = manager.storage().load(id);
                        if (d != null && tier.equalsIgnoreCase(d.tier())) n++;
                    }
                    return String.valueOf(n);
                }
                return null;
            }
        }
    }

    /** Liefert {belegteSlots, kapazität} summiert über alle Backpacks des Spielers. */
    private int[] slotStats(List<UUID> ids) {
        int used = 0;
        int capacity = 0;
        for (UUID id : ids) {
            BackpackData d = manager.storage().load(id);
            if (d == null) continue;
            var tier = tiers.get(d.tier());
            if (tier != null) capacity += tier.storageSlots();
            ItemStack[] c = d.contents();
            if (c != null) {
                for (ItemStack it : c) {
                    if (it != null && !it.getType().isAir()) used++;
                }
            }
        }
        return new int[]{used, capacity};
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
