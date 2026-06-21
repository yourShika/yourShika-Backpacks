package de.yourshika.backpacks.item;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Erstellt Backpack-Items und liest/schreibt deren Daten im
 * PersistentDataContainer. Die Identität (ID + Tier) liegt ausschließlich im
 * PDC und ist damit nicht durch Umbenennen oder Lore fälschbar.
 *
 * <p>Der Inhalt eines Backpacks wird bewusst NICHT im Item gespeichert, sondern
 * server-seitig anhand der ID – das verhindert Item-Duplikation.</p>
 */
public final class BackpackItemFactory {

    /** Aktuelle Datenformat-Version eines Backpack-Items. */
    public static final int DATA_VERSION = 1;

    private final YourShikaBackpacks plugin;
    private final BackpackKeys keys;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public BackpackItemFactory(YourShikaBackpacks plugin, BackpackKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public BackpackKeys keys() {
        return keys;
    }

    /**
     * Erstellt ein vollständiges Backpack-Item. {@code id} darf null sein – dann
     * wird beim ersten Öffnen automatisch eine eindeutige ID vergeben
     * (Lazy-ID, z.B. für gecraftete Backpacks).
     */
    public ItemStack create(BackpackTier tier, UUID id, DyeColor main, DyeColor accent) {
        ItemStack item = new ItemStack(tier.material());
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.marker, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keys.tier, PersistentDataType.STRING, tier.key());
        pdc.set(keys.dataVersion, PersistentDataType.INTEGER, DATA_VERSION);
        if (id != null) {
            pdc.set(keys.id, PersistentDataType.STRING, id.toString());
        }
        pdc.set(keys.mainColor, PersistentDataType.STRING, main.name());
        pdc.set(keys.accentColor, PersistentDataType.STRING, accent.name());

        if (tier.customModelData() > 0) {
            meta.setCustomModelData(tier.customModelData());
        }
        // Backpacks niemals stapeln – garantiert eindeutige Items.
        try {
            meta.setMaxStackSize(1);
        } catch (Throwable ignored) {
            // Ältere API ohne setMaxStackSize – unkritisch.
        }

        item.setItemMeta(meta);
        applyDisplay(item, tier, id, main, accent);
        return item;
    }

    /** Erzeugt ein Template ohne ID (für Rezepte / Vorschau). */
    public ItemStack createTemplate(BackpackTier tier) {
        return create(tier, null, tier.defaultMainColor(), tier.defaultAccentColor());
    }

    /** Aktualisiert Name und Lore eines bestehenden Backpacks. */
    public void applyDisplay(ItemStack item, BackpackTier tier, UUID id, DyeColor main, DyeColor accent) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        TagResolver resolvers = TagResolver.resolver(
                Placeholder.component("main_color", Component.text(ColorUtil.pretty(main)).color(ColorUtil.toTextColor(main))),
                Placeholder.component("accent_color", Component.text(ColorUtil.pretty(accent)).color(ColorUtil.toTextColor(accent))),
                Placeholder.unparsed("storage", String.valueOf(tier.storageSlots())),
                Placeholder.unparsed("upgrades", String.valueOf(tier.upgradeSlots())),
                Placeholder.unparsed("id", id == null ? "—" : shortId(id))
        );

        meta.displayName(mini.deserialize(tier.displayName(), resolvers)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : tier.lore()) {
            lore.add(mini.deserialize(line, resolvers).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    public boolean isBackpack(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.marker, PersistentDataType.BYTE)
                && meta.getPersistentDataContainer().has(keys.tier, PersistentDataType.STRING);
    }

    public String getTierKey(ItemStack item) {
        if (!isBackpack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keys.tier, PersistentDataType.STRING);
    }

    public UUID getId(ItemStack item) {
        if (!isBackpack(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.id, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public DyeColor getMainColor(ItemStack item, DyeColor fallback) {
        if (!isBackpack(item)) return fallback;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.mainColor, PersistentDataType.STRING);
        return ColorUtil.parseDye(raw, fallback);
    }

    public DyeColor getAccentColor(ItemStack item, DyeColor fallback) {
        if (!isBackpack(item)) return fallback;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.accentColor, PersistentDataType.STRING);
        return ColorUtil.parseDye(raw, fallback);
    }

    /** Vergibt bei Bedarf eine neue, eindeutige ID und schreibt sie in das Item. */
    public UUID writeId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.id, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    public void writeColors(ItemStack item, DyeColor main, DyeColor accent) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.mainColor, PersistentDataType.STRING, main.name());
        meta.getPersistentDataContainer().set(keys.accentColor, PersistentDataType.STRING, accent.name());
        item.setItemMeta(meta);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
