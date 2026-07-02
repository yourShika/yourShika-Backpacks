package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.inventory.ItemStack;

/**
 * Oraxen-Anbindung. Liefert optional Custom-Modelle/Texturen über die
 * {@code provider-id} eines Tiers. Einziger unterstützter Custom-Item-Hook
 * (frei nutzbar, reife API).
 */
public final class OraxenModule extends ExternalItemModule {

    /** Dynamisch registrierter Listener für Oraxens Reload-Event (per Reflection). */
    private org.bukkit.event.Listener reloadListener;

    public OraxenModule(YourShikaBackpacks plugin) {
        super(plugin, "oraxen", "Oraxen",
                "Custom-Modelle/Texturen für Backpacks", "Oraxen");
    }

    @Override
    protected void onEnable() throws Throwable {
        verifyApi();
        // Mitgelieferte Texturen/Item-Definitionen bereitstellen (best-effort).
        new OraxenAssetDeployer(plugin).deploy();
        registerReloadListener();
    }

    @Override
    protected void onDisable() {
        if (reloadListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(reloadListener);
            reloadListener = null;
        }
    }

    /**
     * Registriert – nur wenn Oraxen vorhanden ist – einen Listener auf Oraxens
     * {@code OraxenItemsLoadedEvent}. Nach einem {@code /oraxen reload} (bzw.
     * {@code /oraxen rl all}) werden dadurch die vom Hook abhängigen Items/Modelle
     * automatisch neu synchronisiert, sodass frisch bereitgestellte Texturen sofort
     * greifen – ohne dass man das Modul erst aus- und wieder einschalten muss.
     *
     * <p>Bewusst per Reflection + {@link org.bukkit.plugin.EventExecutor}, damit das
     * Plugin ohne Oraxen auf dem Klassenpfad kompiliert und läuft.</p>
     */
    @SuppressWarnings("unchecked")
    private void registerReloadListener() {
        try {
            Class<? extends org.bukkit.event.Event> eventClass =
                    (Class<? extends org.bukkit.event.Event>) Class.forName(
                            "io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent");
            reloadListener = new org.bukkit.event.Listener() {};
            org.bukkit.plugin.EventExecutor executor = (listener, event) ->
                    // Einen Tick verzögern: erst wenn Oraxen den Reload abgeschlossen
                    // hat, sind alle Items via getItemById wieder verfügbar.
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, plugin::resyncExternalAssets);
            org.bukkit.Bukkit.getPluginManager().registerEvent(
                    eventClass, reloadListener, org.bukkit.event.EventPriority.MONITOR,
                    executor, plugin);
            plugin.debug("Oraxen-Reload-Listener aktiv (Auto-Resync nach /oraxen reload).");
        } catch (Throwable t) {
            // Ältere Oraxen-Version ohne dieses Event: kein Auto-Resync, kein Fehler.
            plugin.debug("Oraxen-Reload-Listener nicht verfügbar: " + t.getMessage());
        }
    }

    @Override
    protected String defaultProviderId(de.yourshika.backpacks.tier.BackpackTier tier) {
        // Standard-Oraxen-Item-ID der mitgelieferten Definitionen.
        return "ysbp_" + tier.key().toLowerCase() + "_backpack";
    }

    @Override
    protected void verifyApi() throws Throwable {
        Class.forName("io.th0rgal.oraxen.api.OraxenItems");
    }

    @Override
    protected ItemStack fetchById(String id) throws Throwable {
        Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
        Object builder = oraxenItems.getMethod("getItemById", String.class).invoke(null, id);
        if (builder == null) return null;
        Object stack = builder.getClass().getMethod("build").invoke(builder);
        return (ItemStack) stack;
    }
}
