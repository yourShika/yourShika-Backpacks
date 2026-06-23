package de.yourshika.backpacks.upgrade;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Definition aller funktionalen Upgrades, die in einen Rucksack eingebaut werden
 * können. Jedes Upgrade hat eine eindeutige ID, ein Custom-Model (für Oraxen/
 * Resourcepacks), englische Anzeige + Lore und ein faires Crafting-Rezept.
 *
 * <p>In jedem Rezept steht {@code 'U'} für das <b>Upgrade-Leder</b> (per
 * ExactChoice) – so sind Funktions-Upgrades fest an das Backpack-System gebunden
 * und nicht aus reinen Vanilla-Items herstellbar.</p>
 */
public enum FunctionUpgrade {

    PICKUP("pickup", "<#5BE85B><bold>Pickup Upgrade</bold></#5BE85B>", 2100,
            List.of("<gray>Picks up items <white>directly into the backpack</white>."),
            List.of(" H ", "RUR", " C "),
            Map.of('H', Material.HOPPER, 'R', Material.REDSTONE, 'C', Material.CHEST),
            0),

    ADVANCED_PICKUP("advanced_pickup", "<#9BFF9B><bold>Advanced Pickup Upgrade</bold></#9BFF9B>", 2101,
            List.of("<gray>Picks up items into the backpack",
                    "<gray>and vacuums a <white>small radius</white>."),
            List.of(" H ", "DUD", " C "),
            Map.of('H', Material.HOPPER, 'D', Material.DIAMOND, 'C', Material.CHEST),
            3),

    MAGNET("magnet", "<#FF6B6B><bold>Magnet Upgrade</bold></#FF6B6B>", 2102,
            List.of("<gray>Pulls nearby dropped items <white>towards you</white>."),
            List.of("IRI", "RUR", "IRI"),
            Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE),
            5),

    ADVANCED_MAGNET("advanced_magnet", "<#FF9B9B><bold>Advanced Magnet Upgrade</bold></#FF9B9B>", 2103,
            List.of("<gray>Pulls nearby dropped items towards you",
                    "<gray>over a <white>larger radius</white>."),
            List.of("IRI", "DUD", "IRI"),
            Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'D', Material.DIAMOND),
            9),

    CRAFTING("crafting", "<#7FD7FF><bold>Crafting Upgrade</bold></#7FD7FF>", 2114,
            List.of("<gray>Opens a <white>crafting grid</white> from the backpack."),
            List.of("PPP", "PUP", "PPP"),
            Map.of('P', Material.OAK_PLANKS),
            0),

    STONECUTTER("stonecutter", "<#C0C0C0><bold>Stonecutter Upgrade</bold></#C0C0C0>", 2123,
            List.of("<gray>Opens a <white>stonecutter</white> from the backpack."),
            List.of(" T ", "SUS", " S "),
            Map.of('T', Material.STONECUTTER, 'S', Material.STONE),
            0),

    SMITHING("smithing", "<#8AB4F8><bold>Smithing Upgrade</bold></#8AB4F8>", 2125,
            List.of("<gray>Opens a <white>smithing table</white> from the backpack."),
            List.of(" T ", "IUI", " I "),
            Map.of('T', Material.SMITHING_TABLE, 'I', Material.IRON_INGOT),
            0),

    EVERLASTING("everlasting", "<#B388FF><bold>Everlasting Upgrade</bold></#B388FF>", 2144,
            List.of("<gray>Protects the dropped backpack from <white>fire,",
                    "<white>lava, explosions and despawning</white>."),
            List.of(" N ", "OUO", " O "),
            Map.of('N', Material.NETHERITE_INGOT, 'O', Material.OBSIDIAN),
            0);

    private final String id;
    private final String displayName;
    private final int customModelData;
    private final List<String> lore;
    private final List<String> shape;
    private final Map<Character, Material> ingredients;
    private final int radius;

    FunctionUpgrade(String id, String displayName, int customModelData, List<String> lore,
                    List<String> shape, Map<Character, Material> ingredients, int radius) {
        this.id = id;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.lore = lore;
        this.shape = shape;
        this.ingredients = ingredients;
        this.radius = radius;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int customModelData() { return customModelData; }
    public List<String> lore() { return lore; }
    public List<String> shape() { return shape; }
    public Map<Character, Material> ingredients() { return ingredients; }
    /** Wirk-Radius (für Pickup/Magnet); 0 = nicht radius-basiert. */
    public int radius() { return radius; }
    /** Standard-Oraxen-Provider-ID. */
    public String providerId() { return "ysbp_upgrade_" + id; }

    public static FunctionUpgrade byId(String id) {
        if (id == null) return null;
        for (FunctionUpgrade u : values()) {
            if (u.id.equalsIgnoreCase(id)) return u;
        }
        return null;
    }
}
