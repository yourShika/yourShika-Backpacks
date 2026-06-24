package de.yourshika.backpacks.upgrade;

import de.yourshika.backpacks.YourShikaBackpacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Erstellt und erkennt die Upgrade-Items.
 *
 * <p>Es gibt das Basis-Item <b>Upgrade-Leder</b> (aus Leder + Faden) und je
 * Ziel-Tier ein <b>Tier-Upgrade</b> (Copper-, Iron-, Gold-, Diamond-, Emerald-,
 * Netherite-Upgrade). Mit einem Tier-Upgrade veredelt man im Smithing Table das
 * jeweils vorherige Backpack-Tier zum nächsten – unter Erhalt von ID und
 * Inhalt.</p>
 *
 * <p>Die Identität liegt – wie bei Backpacks – ausschließlich im
 * PersistentDataContainer und ist nicht fälschbar.</p>
 */
public final class UpgradeItemFactory {

    public static final int BASE_CMD = 2000;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NamespacedKey baseMarker;   // markiert das Upgrade-Leder
    private final NamespacedKey typeKey;      // Ziel-Tier eines Tier-Upgrades
    private final NamespacedKey functionKey;  // Funktions-Upgrade (pickup, magnet, ...)
    private final YourShikaBackpacks plugin;

    public UpgradeItemFactory(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.baseMarker = new NamespacedKey(plugin, "upgrade_base");
        this.typeKey = new NamespacedKey(plugin, "upgrade_type");
        this.functionKey = new NamespacedKey(plugin, "upgrade_function");
    }

    /** Ein Funktions-Upgrade (z.B. "pickup", "magnet", "crafting", ...). */
    public ItemStack functionUpgrade(String functionId, String displayName, List<String> loreMini,
                                     int cmd, String itemModel, String providerId) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(functionKey, PersistentDataType.STRING, functionId);
        meta.displayName(MINI.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new java.util.ArrayList<>();
        lore.add(line("<dark_gray><st>                    </st>"));
        for (String l : loreMini) lore.add(line(l));
        lore.add(Component.empty());
        lore.add(line("<#6E5BC8>❖ <gray>Install via a backpack's <white>Upgrades</white> menu."));
        lore.add(line("<dark_gray><st>                    </st>"));
        meta.lore(lore);
        applyCustomModel(meta, cmd, itemModel);
        item.setItemMeta(meta);
        applyExternalModel(item, providerId);
        return item;
    }

    public boolean isFunctionUpgrade(ItemStack item) {
        return getFunctionType(item) != null;
    }

    /** Funktions-Typ eines Funktions-Upgrades oder null. */
    public String getFunctionType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(functionKey, PersistentDataType.STRING);
    }

    /** Das Basis-Item "Upgrade Leather" mit konfigurierbarem Modell. */
    public ItemStack base(int cmd, String itemModel, String providerId) {
        ItemStack item = new ItemStack(Material.LEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(baseMarker, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(line("<#A0703C><bold>Upgrade Leather</bold></#A0703C>"));
        meta.lore(List.of(
                line("<dark_gray><st>                    </st>"),
                line("<gray>The base material for backpack upgrades."),
                Component.empty(),
                line("<#A0703C>❖ <gray>Combine with <white>8× tier material</white>"),
                line("<gray>   in a Crafting Table to make a tier upgrade."),
                line("<dark_gray><st>                    </st>")
        ));
        applyCustomModel(meta, cmd, itemModel);
        item.setItemMeta(meta);
        applyExternalModel(item, providerId);
        return item;
    }

    /**
     * Ein Tier-Upgrade für das Ziel-Tier {@code targetTier} (z.B. "copper") mit
     * konfigurierbarem CustomModelData/item_model (für spätere eigene Texturen).
     */
    public ItemStack tierUpgrade(String targetTier, String displayName, int cmd, String itemModel, String providerId) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, targetTier);
        meta.displayName(MINI.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        String pretty = targetTier.isEmpty() ? targetTier
                : Character.toUpperCase(targetTier.charAt(0)) + targetTier.substring(1);
        meta.lore(List.of(
                line("<dark_gray><st>                    </st>"),
                line("<gray>Upgrades a backpack to <white>" + pretty + "</white>."),
                Component.empty(),
                line("<#8AB4F8>❖ <gray>Smithing Table: <white>Leather</white> + <white>Backpack</white> + this."),
                line("<gray>   ID, contents and color are kept."),
                line("<dark_gray><st>                    </st>")
        ));
        applyCustomModel(meta, cmd, itemModel);
        item.setItemMeta(meta);
        applyExternalModel(item, providerId);
        return item;
    }

    private Component line(String mm) {
        return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }

    /** Ist aktuell ein externer Custom-Item-Anbieter (z.B. Oraxen) aktiv? */
    private boolean externalActive() {
        return plugin.moduleManager() != null
                && plugin.moduleManager().activeItemProvider() != null;
    }

    /**
     * Setzt CustomModelData und item_model NUR, wenn ein externer Custom-Item-Hook
     * (z.B. Oraxen) aktiv ist. Ist das Modul aus, bleiben die Upgrade-Items
     * normale Vanilla-Items (Papier/Leder) – analog dazu, wie Backpacks zur
     * normalen Pferderüstung zurückkehren. Andernfalls werden die Komponenten
     * sauber geleert, damit ein abgeschalteter Hook keine Custom-Textur hinterlässt.
     */
    private void applyCustomModel(ItemMeta meta, int cmd, String itemModel) {
        CustomModelDataComponent c = meta.getCustomModelDataComponent();
        if (externalActive() && cmd > 0) {
            c.setFloats(List.of((float) cmd));
        } else {
            c.setFloats(List.of());
        }
        meta.setCustomModelDataComponent(c);

        if (externalActive() && itemModel != null && !itemModel.isBlank()) {
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(itemModel);
            if (key != null) meta.setItemModel(key);
        } else {
            meta.setItemModel(null); // zurücksetzen -> Vanilla-Optik
        }
    }

    private void applyExternalModel(ItemStack item, String providerId) {
        if (plugin.moduleManager() != null) {
            plugin.moduleManager().applyExternalModel(item, providerId);
        }
    }

    public boolean isUpgradeBase(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(baseMarker, PersistentDataType.BYTE);
    }

    public boolean isTierUpgrade(ItemStack item) {
        return getUpgradeTarget(item) != null;
    }

    /** Ziel-Tier eines Tier-Upgrades oder null. */
    public String getUpgradeTarget(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    /** Ist das Item irgendein Upgrade-Item (Basis, Tier- oder Funktions-Upgrade)? */
    public boolean isAnyUpgrade(ItemStack item) {
        return isUpgradeBase(item) || isTierUpgrade(item) || isFunctionUpgrade(item);
    }
}
