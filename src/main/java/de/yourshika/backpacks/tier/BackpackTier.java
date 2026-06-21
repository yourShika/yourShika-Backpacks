package de.yourshika.backpacks.tier;

import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Unveränderliche Beschreibung eines Backpack-Tiers. Alle Werte werden aus der
 * config.yml geladen, sodass Server-Admins Tiers frei anpassen können.
 *
 * <p>Speicher und Upgrade-Slots werden hier nur definiert; die eigentliche
 * Persistenz erfolgt server-seitig über die Backpack-ID.</p>
 */
public final class BackpackTier {

    private final String key;
    private final String displayName;     // MiniMessage
    private final Material material;
    private final int customModelData;    // 0 = keiner
    private final int storageRows;        // Anzahl Lager-Reihen (1-5)
    private final int upgradeSlots;       // vorbereitete Upgrade-Slots
    private final DyeColor defaultMainColor;
    private final DyeColor defaultAccentColor;
    private final String permission;      // null = keine
    private final double price;           // Vault-Roadmap
    private final List<String> lore;      // MiniMessage-Zeilen
    private final RecipeDefinition recipe;

    public BackpackTier(String key, String displayName, Material material, int customModelData,
                        int storageRows, int upgradeSlots, DyeColor defaultMainColor,
                        DyeColor defaultAccentColor, String permission, double price,
                        List<String> lore, RecipeDefinition recipe) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.storageRows = Math.max(1, Math.min(5, storageRows));
        this.upgradeSlots = Math.max(0, upgradeSlots);
        this.defaultMainColor = defaultMainColor;
        this.defaultAccentColor = defaultAccentColor;
        this.permission = permission;
        this.price = price;
        this.lore = lore;
        this.recipe = recipe;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public int customModelData() { return customModelData; }
    public int storageRows() { return storageRows; }
    /** Nutzbare Lager-Slots (Reihen * 9). */
    public int storageSlots() { return storageRows * 9; }
    public int upgradeSlots() { return upgradeSlots; }
    public DyeColor defaultMainColor() { return defaultMainColor; }
    public DyeColor defaultAccentColor() { return defaultAccentColor; }
    public String permission() { return permission; }
    public double price() { return price; }
    public List<String> lore() { return lore; }
    public RecipeDefinition recipe() { return recipe; }

    /** Konfigurierbares Crafting-Rezept eines Tiers. */
    public record RecipeDefinition(boolean enabled, List<String> shape,
                                   java.util.Map<Character, Material> ingredients) {
    }
}
