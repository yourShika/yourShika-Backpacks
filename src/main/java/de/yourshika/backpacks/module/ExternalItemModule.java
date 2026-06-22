package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

/**
 * Basis für externe Custom-Item-Module (Nexo, ItemsAdder, Oraxen). Sie liefern
 * optional Modell/Texturen für Backpacks und überlagern damit den eingebauten
 * Vanilla-Anbieter.
 *
 * <p>Die Anbindung erfolgt bewusst per Reflection, damit das Plugin ohne deren
 * Bibliotheken kompiliert und startet. Schlägt etwas fehl, bleibt das Item
 * unverändert (Vanilla-Optik) – nichts bricht.</p>
 */
public abstract class ExternalItemModule extends AbstractModule implements CustomItemProvider {

    protected ExternalItemModule(YourShikaBackpacks plugin, String id, String displayName,
                                 String description, String requiredPlugin) {
        super(plugin, id, displayName, description, requiredPlugin);
    }

    @Override
    protected void onEnable() throws Throwable {
        // Erkennungsklasse anstoßen, damit eine fehlende API sofort auffällt.
        verifyApi();
    }

    /** Wirft, wenn die erwartete API-Klasse fehlt. */
    protected abstract void verifyApi() throws Throwable;

    /** Holt ein Custom-Item per ID aus dem externen Plugin (oder null). */
    protected abstract ItemStack fetchById(String id) throws Throwable;

    @Override
    public void apply(ItemStack item, BackpackTier tier) {
        if (!isActive()) return;
        String pid = tier.providerId();
        if (pid == null || pid.isBlank()) return;
        try {
            ItemStack ext = fetchById(pid);
            if (ext == null) return;
            ItemMeta extMeta = ext.getItemMeta();
            ItemMeta ourMeta = item.getItemMeta();
            if (extMeta == null || ourMeta == null) return;

            // Nur die modell-bestimmenden Bestandteile übernehmen – Identität
            // (PDC), Name, Lore und Färbung bleiben unsere.
            if (extMeta.hasItemModel()) {
                ourMeta.setItemModel(extMeta.getItemModel());
            }
            CustomModelDataComponent extCmd = extMeta.getCustomModelDataComponent();
            if (!extCmd.getFloats().isEmpty() || !extCmd.getStrings().isEmpty()) {
                ourMeta.setCustomModelDataComponent(extCmd);
            }
            item.setItemMeta(ourMeta);
        } catch (Throwable t) {
            plugin.debug("Externes Modell ('" + id() + "', id=" + pid + ") nicht anwendbar: " + t.getMessage());
        }
    }
}
