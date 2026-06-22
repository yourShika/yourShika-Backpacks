package de.yourshika.backpacks.tier;

import org.bukkit.Material;

import java.util.List;

/**
 * Unveränderliche Beschreibung eines Backpack-Tiers. Alle Werte werden aus der
 * config.yml geladen, sodass Server-Admins Tiers frei anpassen können.
 *
 * <p>Speicher- und Upgrade-Slots werden hier nur definiert; die eigentliche
 * Persistenz erfolgt server-seitig über die Backpack-ID.</p>
 *
 * <p>Farben werden als <em>Token</em> gespeichert: ein DyeColor-Name oder ein
 * Hex-Wert ({@code #RRGGBB}). Siehe {@code ColorUtil}.</p>
 */
public final class BackpackTier {

    private final String key;
    private final String displayName;     // MiniMessage
    private final Material material;
    private final int customModelData;    // 0 = keiner
    private final String itemModel;       // moderne item_model-Component ("" = keine)
    private final String providerId;      // Custom-Item-ID für externes Modul ("" = keine)
    private final int storageSlots;       // nutzbare Lager-Slots insgesamt (kann > 45 sein)
    private final int upgradeSlots;       // vorbereitete Upgrade-Slots
    private final String defaultMainColor;   // Token (Dye oder Hex)
    private final String defaultAccentColor; // Token (Dye oder Hex)
    private final String permission;      // null = keine
    private final List<String> lore;      // MiniMessage-Zeilen
    private final RecipeDefinition recipe;

    public BackpackTier(String key, String displayName, Material material, int customModelData,
                        String itemModel, String providerId, int storageSlots, int upgradeSlots,
                        String defaultMainColor, String defaultAccentColor, String permission,
                        List<String> lore, RecipeDefinition recipe) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.itemModel = itemModel == null ? "" : itemModel;
        this.providerId = providerId == null ? "" : providerId;
        this.storageSlots = Math.max(1, storageSlots);
        this.upgradeSlots = Math.max(0, upgradeSlots);
        this.defaultMainColor = defaultMainColor;
        this.defaultAccentColor = defaultAccentColor;
        this.permission = permission;
        this.lore = lore;
        this.recipe = recipe;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public int customModelData() { return customModelData; }
    public String itemModel() { return itemModel; }
    public String providerId() { return providerId; }
    /** Nutzbare Lager-Slots insgesamt (über alle Seiten). */
    public int storageSlots() { return storageSlots; }
    public int upgradeSlots() { return upgradeSlots; }
    public String defaultMainColor() { return defaultMainColor; }
    public String defaultAccentColor() { return defaultAccentColor; }
    public String permission() { return permission; }
    public List<String> lore() { return lore; }
    public RecipeDefinition recipe() { return recipe; }

    /** Anzahl benötigter GUI-Seiten bei gegebener Seitengröße. */
    public int pageCount(int slotsPerPage) {
        int per = Math.max(1, slotsPerPage);
        return Math.max(1, (storageSlots + per - 1) / per);
    }

    /** Konfigurierbares Crafting-Rezept eines Tiers. */
    public record RecipeDefinition(boolean enabled, List<String> shape,
                                   java.util.Map<Character, Material> ingredients) {
    }
}
