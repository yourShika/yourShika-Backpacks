package de.yourshika.backpacks.achievement;

import org.bukkit.Material;

/**
 * Alle Backpack-Achievements (eigener „Reiter" über {@code /bp achievements}).
 * Jedes hat eine ID (= Trigger-Schlüssel), einen Titel, eine Beschreibung und ein
 * Icon. Einzelne Achievements lassen sich über {@code achievements.disabled} in der
 * config.yml abschalten.
 */
public enum Achievement {

    FIRST("first", "First Pack", "Obtain your first backpack.", Material.LEATHER_HORSE_ARMOR),
    TIER_COPPER("tier_copper", "Copper Carrier", "Open a Copper backpack.", Material.COPPER_INGOT),
    TIER_IRON("tier_iron", "Iron Hauler", "Open an Iron backpack.", Material.IRON_INGOT),
    TIER_GOLD("tier_gold", "Golden Touch", "Open a Gold backpack.", Material.GOLD_INGOT),
    TIER_DIAMOND("tier_diamond", "Shine Bright", "Open a Diamond backpack.", Material.DIAMOND),
    TIER_EMERALD("tier_emerald", "Trader's Pride", "Open an Emerald backpack.", Material.EMERALD),
    TIER_NETHERITE("tier_netherite", "Endgame", "Open a Netherite backpack.", Material.NETHERITE_INGOT),
    DYE("dye", "Splash of Color", "Dye a backpack.", Material.RED_DYE),
    RENAME("rename", "Make it Yours", "Rename a backpack.", Material.NAME_TAG),
    COLOR_NAME("color_name", "Fabulous", "Give a backpack a colored or gradient name.", Material.FIREWORK_ROCKET),
    PLACE("place", "Home Base", "Place a backpack in the world.", Material.SCAFFOLDING),
    RECALL("recall", "Come Back", "Recall a placed backpack.", Material.ENDER_PEARL),
    TRANSFER("transfer", "Generous", "Transfer a backpack to someone.", Material.PLAYER_HEAD),
    UPGRADE("upgrade", "Tinkerer", "Install your first function upgrade.", Material.PAPER),
    ADVANCED("advanced", "Next Level", "Install an advanced upgrade.", Material.DIAMOND_BLOCK),
    STATIONS("stations", "Swiss Army Pack", "Install 5 station upgrades in one backpack.", Material.CRAFTING_TABLE),
    FULL_UPGRADES("full_upgrades", "Fully Loaded", "Fill every upgrade slot of a backpack.", Material.ANVIL),
    PICKUP("pickup", "Vacuum", "Auto-pick an item straight into a backpack.", Material.HOPPER),
    MAGNET("magnet", "Magnetic", "Pull items in with a Magnet upgrade.", Material.IRON_NUGGET),
    FURNACE("furnace", "Portable Forge", "Use a portable furnace, smoker or blast furnace.", Material.FURNACE),
    COMPACT("compact", "Compactor", "Compact items into blocks.", Material.PISTON),
    XP("xp", "XP Bank", "Store experience in a backpack.", Material.EXPERIENCE_BOTTLE),
    ENDER("ender", "Pocket Dimension", "Open your ender chest from a backpack.", Material.ENDER_CHEST),
    CRAFTING("crafting", "On the Go", "Open a crafting grid from a backpack.", Material.CRAFTING_TABLE),
    TRASH("trash", "Spring Cleaning", "Delete items in the trash.", Material.LAVA_BUCKET),
    EVERLASTING("everlasting", "Indestructible", "Install an Everlasting upgrade.", Material.NETHERITE_INGOT),
    SMITHING_UP("smithing_up", "Upgraded", "Upgrade a backpack to a higher tier.", Material.SMITHING_TABLE),
    COLLECTOR("collector", "Collector", "Own 5 or more backpacks.", Material.CHEST),
    FILL("fill", "Hoarder", "Completely fill a backpack's storage.", Material.BUNDLE);

    private final String id;
    private final String title;
    private final String description;
    private final Material icon;

    Achievement(String id, String title, String description, Material icon) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
    }

    public String id() { return id; }
    public String title() { return title; }
    public String description() { return description; }
    public Material icon() { return icon; }

    public static Achievement byId(String id) {
        if (id == null) return null;
        for (Achievement a : values()) {
            if (a.id.equalsIgnoreCase(id)) return a;
        }
        return null;
    }
}
