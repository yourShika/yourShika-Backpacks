package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.inventory.ItemStack;

/**
 * Nexo-Anbindung (empfohlener, offener Oraxen-Nachfolger). Liefert optional
 * Custom-Modelle/Texturen über {@code provider-id} eines Tiers.
 */
public final class NexoModule extends ExternalItemModule {

    public NexoModule(YourShikaBackpacks plugin) {
        super(plugin, "nexo", "Nexo",
                "Custom-Modelle/Texturen (empfohlener Oraxen-Nachfolger)", "Nexo");
    }

    @Override
    protected void verifyApi() throws Throwable {
        Class.forName("com.nexomc.nexo.api.NexoItems");
    }

    @Override
    protected ItemStack fetchById(String id) throws Throwable {
        Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
        Object builder = nexoItems.getMethod("itemFromId", String.class).invoke(null, id);
        if (builder == null) return null;
        Object stack = builder.getClass().getMethod("build").invoke(builder);
        return (ItemStack) stack;
    }
}
