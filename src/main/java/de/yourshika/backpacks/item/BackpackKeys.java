package de.yourshika.backpacks.item;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.NamespacedKey;

/**
 * Zentrale Sammlung aller {@link NamespacedKey}, die im PersistentDataContainer
 * von Backpack-Items verwendet werden. So bleibt die Erkennung eindeutig und ist
 * nicht durch Umbenennen oder Lore fälschbar.
 */
public final class BackpackKeys {

    /** Eindeutige Backpack-ID (UUID als String). */
    public final NamespacedKey id;
    /** Tier-Schlüssel (z.B. "leather"). */
    public final NamespacedKey tier;
    /** Hauptfarbe (DyeColor-Name). */
    public final NamespacedKey mainColor;
    /** Akzentfarbe (DyeColor-Name). */
    public final NamespacedKey accentColor;
    /** Marker-Byte, dass dieses Item ein gültiges Backpack ist. */
    public final NamespacedKey marker;
    /** Datenformat-Version (für spätere Migrationen). */
    public final NamespacedKey dataVersion;
    /** Besitzer-UUID (wer das Backpack erstellt/gecraftet hat). */
    public final NamespacedKey owner;
    /** Besitzer-Name (für die Lore-Anzeige). */
    public final NamespacedKey ownerName;

    public BackpackKeys(YourShikaBackpacks plugin) {
        this.id = new NamespacedKey(plugin, "backpack_id");
        this.tier = new NamespacedKey(plugin, "backpack_tier");
        this.mainColor = new NamespacedKey(plugin, "backpack_main_color");
        this.accentColor = new NamespacedKey(plugin, "backpack_accent_color");
        this.marker = new NamespacedKey(plugin, "backpack_marker");
        this.dataVersion = new NamespacedKey(plugin, "backpack_data_version");
        this.owner = new NamespacedKey(plugin, "backpack_owner");
        this.ownerName = new NamespacedKey(plugin, "backpack_owner_name");
    }
}
