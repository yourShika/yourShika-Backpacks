package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.inventory.ItemStack;

/**
 * Oraxen-Anbindung. Liefert optional Custom-Modelle/Texturen über die
 * {@code provider-id} eines Tiers. Einziger unterstützter Custom-Item-Hook
 * (frei nutzbar, reife API).
 */
public final class OraxenModule extends ExternalItemModule {

    public OraxenModule(YourShikaBackpacks plugin) {
        super(plugin, "oraxen", "Oraxen",
                "Custom-Modelle/Texturen für Backpacks", "Oraxen");
    }

    @Override
    protected void onEnable() throws Throwable {
        verifyApi();
        // Mitgelieferte Texturen/Item-Definitionen bereitstellen (best-effort).
        new OraxenAssetDeployer(plugin).deploy();
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
