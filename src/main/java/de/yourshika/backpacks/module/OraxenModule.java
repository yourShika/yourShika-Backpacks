package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.inventory.ItemStack;

/**
 * Oraxen-Anbindung (Legacy). Liefert optional Custom-Modelle/Texturen über
 * {@code provider-id} eines Tiers. Für neue Setups wird {@link NexoModule}
 * empfohlen.
 */
public final class OraxenModule extends ExternalItemModule {

    public OraxenModule(YourShikaBackpacks plugin) {
        super(plugin, "oraxen", "Oraxen",
                "Custom-Modelle/Texturen (Legacy – Nexo empfohlen)", "Oraxen");
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
