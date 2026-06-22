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

    public UpgradeItemFactory(YourShikaBackpacks plugin) {
        this.baseMarker = new NamespacedKey(plugin, "upgrade_base");
        this.typeKey = new NamespacedKey(plugin, "upgrade_type");
    }

    /** Das Basis-Item "Upgrade-Leder" mit konfigurierbarem Modell. */
    public ItemStack base(int cmd, String itemModel) {
        ItemStack item = new ItemStack(Material.LEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(baseMarker, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(line("<#A0703C><bold>Upgrade-Leder</bold></#A0703C>"));
        meta.lore(List.of(
                line("<gray>Basis für Backpack-Upgrades."),
                line("<dark_gray>Im Crafting Table mit 8× Tier-Material"),
                line("<dark_gray>zu einem Tier-Upgrade kombinieren.")
        ));
        applyCmd(meta, cmd);
        applyModel(meta, itemModel);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Ein Tier-Upgrade für das Ziel-Tier {@code targetTier} (z.B. "copper") mit
     * konfigurierbarem CustomModelData/item_model (für spätere eigene Texturen).
     */
    public ItemStack tierUpgrade(String targetTier, String displayName, int cmd, String itemModel) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, targetTier);
        meta.displayName(MINI.deserialize(displayName).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                line("<gray>Veredelt im Smithing Table das"),
                line("<gray>vorherige Backpack-Tier zu <white>" + targetTier + "</white>."),
                line("<dark_gray>Slots: Leder + Backpack + dieses Upgrade.")
        ));
        applyCmd(meta, cmd);
        applyModel(meta, itemModel);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String mm) {
        return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }

    private void applyCmd(ItemMeta meta, int cmd) {
        if (cmd <= 0) return;
        CustomModelDataComponent c = meta.getCustomModelDataComponent();
        c.setFloats(List.of((float) cmd));
        meta.setCustomModelDataComponent(c);
    }

    private void applyModel(ItemMeta meta, String itemModel) {
        if (itemModel == null || itemModel.isBlank()) return;
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(itemModel);
        if (key != null) meta.setItemModel(key);
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

    /** Ist das Item irgendein Upgrade-Item (Basis oder Tier-Upgrade)? */
    public boolean isAnyUpgrade(ItemStack item) {
        return isUpgradeBase(item) || isTierUpgrade(item);
    }
}
