package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.inventory.ItemStack;

/**
 * ItemsAdder-Anbindung. Liefert optional Custom-Modelle/Texturen über
 * {@code provider-id} eines Tiers.
 */
public final class ItemsAdderModule extends ExternalItemModule {

    public ItemsAdderModule(YourShikaBackpacks plugin) {
        super(plugin, "itemsadder", "ItemsAdder",
                "Custom-Modelle/Texturen", "ItemsAdder");
    }

    @Override
    protected void verifyApi() throws Throwable {
        Class.forName("dev.lone.itemsadder.api.CustomStack");
    }

    @Override
    protected ItemStack fetchById(String id) throws Throwable {
        Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
        Object instance = customStack.getMethod("getInstance", String.class).invoke(null, id);
        if (instance == null) return null;
        return (ItemStack) customStack.getMethod("getItemStack").invoke(instance);
    }
}
