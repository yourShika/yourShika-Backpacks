package de.yourshika.backpacks.item;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
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
 *
 * <p>Standard-Material ist ein färbbares Leder-Item: die Hauptfarbe wird real
 * über {@link LeatherArmorMeta} eingefärbt, sodass die Farbe auch ohne
 * Resourcepack sichtbar ist. Farben sind Tokens (DyeColor-Name oder Hex).</p>
 */
public final class BackpackItemFactory {

    /** Aktuelle Datenformat-Version eines Backpack-Items. */
    public static final int DATA_VERSION = 2;

    private static final Color DEFAULT_LEATHER = Color.fromRGB(0xA0, 0x70, 0x3C);

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
     * (Lazy-ID, z.B. für gecraftete Backpacks). {@code main}/{@code accent} sind
     * Farb-Tokens (DyeColor-Name oder Hex).
     */
    public ItemStack create(BackpackTier tier, UUID id, String main, String accent) {
        ItemStack item = new ItemStack(tier.material());
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.marker, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keys.tier, PersistentDataType.STRING, tier.key());
        pdc.set(keys.dataVersion, PersistentDataType.INTEGER, DATA_VERSION);
        if (id != null) {
            pdc.set(keys.id, PersistentDataType.STRING, id.toString());
        }
        pdc.set(keys.mainColor, PersistentDataType.STRING, main);
        pdc.set(keys.accentColor, PersistentDataType.STRING, accent);

        applyModel(meta, tier);

        // Backpacks niemals stapeln – garantiert eindeutige Items.
        meta.setMaxStackSize(1);

        item.setItemMeta(meta);
        applyDisplay(item, tier, id, main, accent);

        // Optionales externes Modell (Nexo/ItemsAdder/Oraxen) überlagern.
        if (plugin.moduleManager() != null) {
            applyExternalBackpackModel(item, tier, accent);
        }
        return item;
    }

    /** Erzeugt ein Template ohne ID (für Rezepte / Vorschau). */
    public ItemStack createTemplate(BackpackTier tier) {
        return create(tier, null, tier.defaultMainColor(), tier.defaultAccentColor());
    }

    /**
     * Setzt die CustomModelData- und item_model-Component auf den Tier-Standard
     * zurück. Wichtig: {@code item_model} wird hier auch <em>geleert</em>, wenn der
     * Tier keins definiert – so kehrt ein Backpack nach dem Abschalten eines
     * Custom-Item-Hooks (z.B. Oraxen) zur normalen Pferderüstung zurück. Ein
     * aktiver externer Anbieter überschreibt das Modell danach wieder.
     */
    private void applyModel(ItemMeta meta, BackpackTier tier) {
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(tier.customModelData() > 0
                ? List.of((float) tier.customModelData()) : List.of());
        meta.setCustomModelDataComponent(component);

        if (tier.itemModel() != null && !tier.itemModel().isBlank()) {
            meta.setItemModel(NamespacedKey.fromString(tier.itemModel()));
        } else {
            meta.setItemModel(null); // zurücksetzen -> Vanilla-Optik
        }
    }

    /** Aktualisiert Name, Lore und Leder-Färbung eines bestehenden Backpacks. */
    public void applyDisplay(ItemStack item, BackpackTier tier, UUID id, String main, String accent) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Echte Leder-Färbung anhand der Hauptfarbe.
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(ColorUtil.toBukkitColor(main, DEFAULT_LEATHER));
        }
        // Vanilla-Tooltips ausblenden (Farbcode/Attribute) – die Farbe steht
        // bereits sauber in unserer Lore, sonst doppelt.
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_DYE,
                org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        int perPage = plugin.pluginConfig().storageSlotsPerPage();
        TextColor mainColor = ColorUtil.toTextColor(main, TextColor.color(0xFFFFFF));
        TextColor accentColor = ColorUtil.toTextColor(accent, TextColor.color(0xFFFFFF));

        String ownerName = getOwnerName(item);
        TagResolver resolvers = TagResolver.resolver(
                Placeholder.component("main_color", Component.text(ColorUtil.pretty(main)).color(mainColor)),
                Placeholder.component("accent_color", Component.text(ColorUtil.pretty(accent)).color(accentColor)),
                Placeholder.unparsed("storage", String.valueOf(tier.storageSlots())),
                Placeholder.unparsed("pages", String.valueOf(tier.pageCount(perPage))),
                Placeholder.unparsed("upgrades", String.valueOf(tier.upgradeSlots())),
                Placeholder.unparsed("owner", ownerName == null ? "—" : ownerName),
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

    /**
     * Aktualisiert ein bestehendes Backpack-Item auf den aktuellen Stand
     * (Name, Lore, Modell, externes Modell, Datenversion) – ohne ID, Farbe,
     * Besitzer oder Inhalt zu verändern. So übernehmen bereits existierende
     * Backpacks automatisch spätere Änderungen an Tier-Name/-Lore/-Textur.
     */
    public void refresh(ItemStack item, BackpackTier tier) {
        if (!isBackpack(item)) return;
        UUID id = getId(item);
        String main = getMainColor(item, tier.defaultMainColor());
        String accent = getAccentColor(item, tier.defaultAccentColor());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyModel(meta, tier);
            meta.getPersistentDataContainer().set(keys.dataVersion, PersistentDataType.INTEGER, DATA_VERSION);
            item.setItemMeta(meta);
        }
        applyDisplay(item, tier, id, main, accent);
        if (plugin.moduleManager() != null) {
            applyExternalBackpackModel(item, tier, accent);
        }
    }

    private void applyExternalBackpackModel(ItemStack item, BackpackTier tier, String accent) {
        String providerId = accentProviderId(tier, accent);
        if (providerId == null) {
            plugin.moduleManager().applyExternalModel(item, tier);
        } else {
            plugin.moduleManager().applyExternalModel(item, providerId);
        }
    }

    private String accentProviderId(BackpackTier tier, String accent) {
        String base = tier.providerId();
        if (base == null || base.isBlank()) return null;
        String fallback = tier.defaultAccentColor();
        String variant = ColorUtil.nearestDyeKey(accent, fallback);
        String defaultVariant = ColorUtil.nearestDyeKey(fallback, fallback);
        if (variant.equals(defaultVariant)) return base;
        return base + "_accent_" + variant;
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

    /** Liefert den Haupt-Farb-Token oder den Fallback. */
    public String getMainColor(ItemStack item, String fallback) {
        if (!isBackpack(item)) return fallback;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.mainColor, PersistentDataType.STRING);
        return raw == null ? fallback : raw;
    }

    /** Liefert den Akzent-Farb-Token oder den Fallback. */
    public String getAccentColor(ItemStack item, String fallback) {
        if (!isBackpack(item)) return fallback;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.accentColor, PersistentDataType.STRING);
        return raw == null ? fallback : raw;
    }

    /** Vergibt bei Bedarf eine neue, eindeutige ID und schreibt sie in das Item. */
    public UUID writeId(ItemStack item, UUID id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.id, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    public void writeColors(ItemStack item, String main, String accent) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.mainColor, PersistentDataType.STRING, main);
        meta.getPersistentDataContainer().set(keys.accentColor, PersistentDataType.STRING, accent);
        item.setItemMeta(meta);
    }

    /** Schreibt den Besitzer (UUID + Name) in das Item. */
    public void writeOwner(ItemStack item, UUID owner, String name) {
        ItemMeta meta = item.getItemMeta();
        if (owner != null) {
            meta.getPersistentDataContainer().set(keys.owner, PersistentDataType.STRING, owner.toString());
        }
        if (name != null) {
            meta.getPersistentDataContainer().set(keys.ownerName, PersistentDataType.STRING, name);
        }
        item.setItemMeta(meta);
    }

    public UUID getOwner(ItemStack item) {
        if (!isBackpack(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(keys.owner, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getOwnerName(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(keys.ownerName, PersistentDataType.STRING);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
