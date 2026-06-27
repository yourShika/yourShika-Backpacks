package de.yourshika.backpacks.listener;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.tier.BackpackTier;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Erlaubt das Umbenennen eines Backpacks im <b>Amboss</b> – mit voller Farb-
 * Unterstützung (Hex, Gradient, Minecraft-Farbcodes über {@link de.yourshika.backpacks.util.NameUtil}).
 * Der Name wird in der Backpack-Identität (PDC) gespeichert, nicht als roher
 * Vanilla-Anzeigename, damit er beim Platzieren/Aufheben erhalten bleibt.
 */
public final class AnvilRenameListener implements Listener {

    private final YourShikaBackpacks plugin;
    private final BackpackItemFactory items;

    public AnvilRenameListener(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.items = plugin.itemFactory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getItem(0);
        // Upgrade-Items (Upgrade-Leder, Tier- & Funktions-Upgrades) dürfen NICHT
        // im Amboss umbenannt werden – sonst ließe sich die Identität verschleiern.
        if (base != null && plugin.upgradeItems() != null && plugin.upgradeItems().isAnyUpgrade(base)) {
            event.setResult(null);
            return;
        }
        if (!items.isBackpack(base)) return;

        // Zweiten Slot (Kombinieren) ignorieren – wir benennen nur um.
        ItemStack second = event.getInventory().getItem(1);
        if (second != null && !second.getType().isAir()) return;

        Player player = viewer(event);
        if (player != null && !mayRename(player)) {
            event.setResult(null);
            return;
        }

        BackpackTier tier = plugin.tiers().get(items.getTierKey(base));
        if (tier == null) return;

        String renameText = event.getInventory().getRenameText();
        UUID id = items.getId(base);
        String main = items.getMainColor(base, tier.defaultMainColor());
        String accent = items.getAccentColor(base, tier.defaultAccentColor());

        // Kein Klartext-Clobber: zeigt das Feld unverändert den aktuellen (gerenderten)
        // Namen, nichts tun – sonst ginge eine bestehende Farb-/Gradient-Formatierung
        // beim bloßen Einlegen verloren.
        var meta = base.getItemMeta();
        String currentPlain = meta == null || meta.displayName() == null ? ""
                : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
        if (renameText != null && renameText.equals(currentPlain)) {
            return; // keine Änderung
        }

        ItemStack result = base.clone();
        if (renameText == null || renameText.isBlank()) {
            items.writeName(result, null); // leeres Feld -> Standardname
        } else {
            String name = renameText.replace('§', ' ').trim();
            if (name.length() > 64) name = name.substring(0, 64);
            items.writeName(result, name);
        }
        items.applyDisplay(result, tier, id, main, accent);
        event.setResult(result);
    }

    private boolean mayRename(Player player) {
        if (player.hasPermission("yourshika.backpack.admin.rename")) return true;
        return plugin.pluginConfig().renameAllowed()
                && player.hasPermission("yourshika.backpack.rename");
    }

    private Player viewer(PrepareAnvilEvent event) {
        for (HumanEntity he : event.getViewers()) {
            if (he instanceof Player p) return p;
        }
        return null;
    }
}
