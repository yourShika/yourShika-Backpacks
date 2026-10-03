package de.yourshika.backpacks.upgrade;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Definition aller funktionalen Upgrades, die in einen Rucksack eingebaut werden
 * können. Jedes hat eine ID, ein Custom-Model (Oraxen/Resourcepacks), englische
 * Anzeige + Lore und ein faires Crafting-Rezept.
 *
 * <p>In den Rezepten gilt: {@code 'U'} = Upgrade-Leder (ExactChoice), {@code 'X'}
 * = das vorausgesetzte Basis-Upgrade ({@link #requires()}) als ExactChoice. So
 * sind Funktions-Upgrades fest an das System gebunden, und Advanced-Varianten
 * benötigen ihre Grundversion.</p>
 */
public enum FunctionUpgrade {

    PICKUP("pickup", "<#5BE85B><bold>Pickup Upgrade</bold></#5BE85B>", 2100,
            List.of("<gray>Picks up items <white>directly into the backpack</white>."),
            List.of(" H ", "RUR", " C "),
            Map.of('H', Material.HOPPER, 'R', Material.REDSTONE, 'C', Material.CHEST),
            0, null),

    ADVANCED_PICKUP("advanced_pickup", "<#9BFF9B><bold>Advanced Pickup Upgrade</bold></#9BFF9B>", 2101,
            List.of("<gray>Picks up items into the backpack and",
                    "<gray>vacuums a <white>small radius</white>.",
                    "<dark_gray>Requires a Pickup Upgrade."),
            List.of(" E ", "EXE", " E "),
            Map.of('E', Material.ENDER_PEARL),
            3, "pickup"),

    MAGNET("magnet", "<#FF6B6B><bold>Magnet Upgrade</bold></#FF6B6B>", 2102,
            List.of("<gray>Pulls nearby dropped items <white>towards you</white>."),
            List.of("IRI", "RUR", "IRI"),
            Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE),
            5, null),

    ADVANCED_MAGNET("advanced_magnet", "<#FF9B9B><bold>Advanced Magnet Upgrade</bold></#FF9B9B>", 2103,
            List.of("<gray>Pulls nearby items over a <white>larger radius</white>.",
                    "<dark_gray>Requires a Magnet Upgrade."),
            List.of(" D ", "DXD", " D "),
            Map.of('D', Material.DIAMOND),
            9, "magnet"),

    CRAFTING("crafting", "<#7FD7FF><bold>Crafting Upgrade</bold></#7FD7FF>", 2114,
            List.of("<gray>Opens a <white>crafting grid</white> from the backpack."),
            List.of("PPP", "PUP", "PPP"),
            Map.of('P', Material.OAK_PLANKS),
            0, null),

    STONECUTTER("stonecutter", "<#C0C0C0><bold>Stonecutter Upgrade</bold></#C0C0C0>", 2123,
            List.of("<gray>Opens a <white>stonecutter</white> from the backpack."),
            List.of(" T ", "SUS", " S "),
            Map.of('T', Material.STONECUTTER, 'S', Material.STONE),
            0, null),

    SMITHING("smithing", "<#8AB4F8><bold>Smithing Upgrade</bold></#8AB4F8>", 2125,
            List.of("<gray>Opens a <white>smithing table</white> from the backpack."),
            List.of(" T ", "IUI", " I "),
            Map.of('T', Material.SMITHING_TABLE, 'I', Material.IRON_INGOT),
            0, null),

    ENDER_LINK("ender_link", "<#A66BFF><bold>Ender-Link Upgrade</bold></#A66BFF>", 2150,
            List.of("<gray>Opens your <white>ender chest</white> from the backpack."),
            List.of(" Y ", "OUO", " O "),
            Map.of('Y', Material.ENDER_EYE, 'O', Material.OBSIDIAN),
            0, null),

    COMPACTING("compacting", "<#D2B48C><bold>Compacting Upgrade</bold></#D2B48C>", 2115,
            List.of("<gray>Compacts 9x stacks into blocks",
                    "<gray>(e.g. Iron Ingot → Iron Block)."),
            List.of("PIP", "IUI", "PIP"),
            Map.of('P', Material.PISTON, 'I', Material.IRON_BLOCK),
            0, null),

    SMELTING("smelting", "<#FF8C42><bold>Smelting Upgrade</bold></#FF8C42>", 2117,
            List.of("<gray>Adds a <white>portable furnace</white> to the backpack.",
                    "<gray>Open it from a button in the backpack menu",
                    "<gray>and smelt items with fuel.",
                    "<dark_gray>Only one furnace-type upgrade per backpack."),
            List.of(" B ", "FUF", " B "),
            Map.of('B', Material.BLAZE_POWDER, 'F', Material.FURNACE),
            0, null),

    BLASTING("blasting", "<#FFB36B><bold>Blasting Upgrade</bold></#FFB36B>", 2121,
            List.of("<gray>Adds a <white>portable blast furnace</white> to the backpack.",
                    "<gray>Smelts ores and metals twice as fast.",
                    "<dark_gray>Only one furnace-type upgrade per backpack."),
            List.of(" B ", "FUF", " B "),
            Map.of('B', Material.BLAZE_POWDER, 'F', Material.BLAST_FURNACE),
            0, null),

    SMOKING("smoking", "<#FFD27F><bold>Smoking Upgrade</bold></#FFD27F>", 2119,
            List.of("<gray>Adds a <white>portable smoker</white> to the backpack.",
                    "<gray>Cooks food twice as fast.",
                    "<dark_gray>Only one furnace-type upgrade per backpack."),
            List.of(" C ", "SUS", " C "),
            Map.of('C', Material.CHARCOAL, 'S', Material.SMOKER),
            0, null),

    RECALL("recall", "<#5BC8FF><bold>Recall Upgrade</bold></#5BC8FF>", 2151,
            List.of("<gray>A placed backpack returns to you with <white>/bp recall</white>.",
                    "<dark_gray>Expensive."),
            List.of("EDE", "DUD", "EDE"),
            Map.of('E', Material.ENDER_PEARL, 'D', Material.DIAMOND),
            0, null),

    TRASH("trash", "<#8B8B8B><bold>Trash Upgrade</bold></#8B8B8B>", 2152,
            List.of("<gray>Opens a <white>trash</white> menu – placed items are deleted."),
            List.of(" C ", "CUC", " L "),
            Map.of('C', Material.CACTUS, 'L', Material.LAVA_BUCKET),
            0, null),

    XP_STORAGE("xp", "<#7CFF6B><bold>XP Storage Upgrade</bold></#7CFF6B>", 2155,
            List.of("<gray>Store your <white>experience</white> in the backpack",
                    "<gray>and withdraw it again any time.",
                    "<dark_gray>Open it from a button in the backpack menu."),
            List.of(" E ", "BUB", " E "),
            Map.of('E', Material.EMERALD, 'B', Material.EXPERIENCE_BOTTLE),
            0, null),

    // Erweitert das XP-Storage: repariert Mending-Items mit dem im Backpack
    // gespeicherten XP (Auswahl aus den eigenen beschädigten Mending-Items).
    ADVANCED_XP("advanced_xp", "<#AEFF9B><bold>Advanced XP Upgrade</bold></#AEFF9B>", 2158,
            List.of("<gray>Repair <white>Mending</white> items using the experience",
                    "<gray>stored in the backpack – <white>pick what to fix</white>.",
                    "<dark_gray>Opens from the XP Storage menu.",
                    "<dark_gray>Requires an XP Storage Upgrade."),
            List.of("GAG", "EXE", "GUG"),
            Map.of('G', Material.GOLD_BLOCK, 'A', Material.ANVIL, 'E', Material.EMERALD),
            0, "xp"),

    EVERLASTING("everlasting", "<#B388FF><bold>Everlasting Upgrade</bold></#B388FF>", 2144,
            List.of("<gray>The dropped backpack is immune to fire, lava,",
                    "<gray>explosions and <white>never despawns</white>.",
                    "<dark_gray>Needs a Nether Star (Wither)."),
            List.of("OSO", "NUN", "OOO"),
            Map.of('O', Material.OBSIDIAN, 'S', Material.NETHER_STAR, 'N', Material.NETHERITE_INGOT),
            0, null),

    SOULBOUND("soulbound", "<#7CE0FF><bold>Soulbound Upgrade</bold></#7CE0FF>", 2156,
            List.of("<gray>The backpack <white>stays with you when you die</white>",
                    "<gray>instead of dropping – you keep it after respawn.",
                    "<dark_gray>Needs a Totem of Undying."),
            List.of(" T ", "SUS", " S "),
            Map.of('T', Material.TOTEM_OF_UNDYING, 'S', Material.SOUL_SOIL),
            0, null),

    // Bindet an das vorbereitete Oraxen-Asset "ysbp_upgrade_restock" (restock.png, cmd 2110).
    RESTOCK("restock", "<#53BD74><bold>Restock Upgrade</bold></#53BD74>", 2110,
            List.of("<gray>When a hotbar/inventory stack runs out, it is",
                    "<gray><white>refilled from the backpack</white> with the same item."),
            List.of(" R ", "CUC", " R "),
            Map.of('R', Material.COMPARATOR, 'C', Material.CHEST),
            0, null),

    // Bindet an das vorbereitete Oraxen-Asset "ysbp_upgrade_feeding" (feeding.png, cmd 2137).
    FEEDING("feeding", "<#E0A843><bold>Feeding Upgrade</bold></#E0A843>", 2137,
            List.of("<gray>Automatically <white>eats food</white> from the backpack",
                    "<gray>when you get hungry."),
            List.of(" H ", "BUB", " H "),
            Map.of('H', Material.HAY_BLOCK, 'B', Material.BREAD),
            0, null),

    // Braucht das BetterPets-Plugin: aktiviert automatisch einen im Backpack
    // liegenden Pet-XP-Booster, sobald der Spieler in den Kampf geht.
    PET_BOOSTER("pet_booster", "<#E040FB><bold>Pet Booster Upgrade</bold></#E040FB>", 2157,
            List.of("<gray>Auto-activates a <white>Pet XP Booster</white> from the",
                    "<gray>backpack when you enter <white>combat</white>.",
                    "<dark_gray>Requires the BetterPets plugin.",
                    "<dark_gray>Toggle with /bp booster."),
            List.of(" E ", "BUB", " E "),
            Map.of('E', Material.EXPERIENCE_BOTTLE, 'B', Material.BONE),
            0, null),

    // --- Over-Stacking (nur verfügbar, wenn PacketEvents installiert ist) ----
    // Erhöhen die Stapelgröße eines Backpacks weit über 64 (Multiplikatoren in
    // der config überschreibbar). Jede Stufe setzt die vorige voraus. Omega ist
    // nur per Command erhältlich (kein Rezept) und hebt das Limit auf das Maximum.
    STACK_STARTER("stack_starter", "<#D9C99A><bold>Stack Upgrade: Starter</bold>", 2128,
            List.of("<gray>Items in the backpack stack up to <white>1.5×</white> (96).",
                    "<dark_gray>Requires PacketEvents."),
            List.of("CCC", "CUC", "CCC"),
            Map.of('C', Material.CHEST),
            0, null),
    STACK_TIER_1("stack_tier_1", "<#D9C99A><bold>Stack Upgrade: Tier 1</bold>", 2129,
            List.of("<gray>Items stack up to <white>2×</white> (128).",
                    "<dark_gray>Requires a Starter Stack Upgrade + PacketEvents."),
            List.of("III", "IXI", "III"),
            Map.of('I', Material.IRON_BLOCK),
            0, "stack_starter"),
    STACK_TIER_2("stack_tier_2", "<#D9C99A><bold>Stack Upgrade: Tier 2</bold>", 2130,
            List.of("<gray>Items stack up to <white>4×</white> (256).",
                    "<dark_gray>Requires a Tier 1 Stack Upgrade + PacketEvents."),
            List.of("GGG", "GXG", "GGG"),
            Map.of('G', Material.GOLD_BLOCK),
            0, "stack_tier_1"),
    STACK_TIER_3("stack_tier_3", "<#D9C99A><bold>Stack Upgrade: Tier 3</bold>", 2131,
            List.of("<gray>Items stack up to <white>8×</white> (512).",
                    "<dark_gray>Requires a Tier 2 Stack Upgrade + PacketEvents."),
            List.of("DDD", "DXD", "DDD"),
            Map.of('D', Material.DIAMOND_BLOCK),
            0, "stack_tier_2"),
    STACK_TIER_4("stack_tier_4", "<#D9C99A><bold>Stack Upgrade: Tier 4</bold>", 2132,
            List.of("<gray>Items stack up to <white>16×</white> (1024).",
                    "<dark_gray>Requires a Tier 3 Stack Upgrade + PacketEvents."),
            List.of("NNN", "NXN", "NNN"),
            Map.of('N', Material.NETHERITE_BLOCK),
            0, "stack_tier_3"),
    STACK_OMEGA("stack_omega", "<#D06CF5><bold>Stack Upgrade: Omega</bold>", 2133,
            List.of("<gray>Raises the stack limit to the <white>maximum possible</white>.",
                    "<dark_gray>Admin only – not craftable. Requires PacketEvents."),
            List.of("   ", "   ", "   "),
            Map.of(),
            0, null);

    private final String id;
    private final String displayName;
    private final int customModelData;
    private final List<String> lore;
    private final List<String> shape;
    private final Map<Character, Material> ingredients;
    private final int radius;
    private final String requires;

    FunctionUpgrade(String id, String displayName, int customModelData, List<String> lore,
                    List<String> shape, Map<Character, Material> ingredients, int radius, String requires) {
        this.id = id;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.lore = lore;
        this.shape = shape;
        this.ingredients = ingredients;
        this.radius = radius;
        this.requires = requires;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int customModelData() { return customModelData; }
    public List<String> lore() { return lore; }
    public List<String> shape() { return shape; }
    public Map<Character, Material> ingredients() { return ingredients; }
    public int radius() { return radius; }
    /** Vorausgesetztes Basis-Upgrade ('X' im Rezept) oder null. */
    public String requires() { return requires; }
    public String providerId() { return "ysbp_upgrade_" + id; }

    /** Stationen, die einen Button in der Backpack-GUI bekommen. */
    public boolean isStation() {
        return switch (id) {
            case "crafting", "stonecutter", "smithing", "ender_link", "trash",
                 "smelting", "blasting", "smoking", "compacting", "xp" -> true;
            default -> false;
        };
    }

    /** Braucht dieses Upgrade das externe BetterPets-Plugin? */
    public boolean requiresBetterPets() {
        return id.equals("pet_booster");
    }

    /** Over-Stacking-Upgrade (stack_*)? Nur verfügbar mit PacketEvents. */
    public boolean isStackUpgrade() {
        return id.startsWith("stack_");
    }

    /** Braucht dieses Upgrade PacketEvents? (aktuell die Stack-Upgrades) */
    public boolean requiresPacketEvents() {
        return isStackUpgrade();
    }

    /** Nur per Command erhältlich (kein Rezept)? */
    public boolean isCommandOnly() {
        return id.equals("stack_omega");
    }

    /** Standard-Stapelmultiplikator (per config überschreibbar). Omega -> MAX. */
    public double stackMultiplier() {
        return switch (id) {
            case "stack_starter" -> 1.5;
            case "stack_tier_1" -> 2.0;
            case "stack_tier_2" -> 4.0;
            case "stack_tier_3" -> 8.0;
            case "stack_tier_4" -> 16.0;
            case "stack_omega" -> Double.MAX_VALUE;
            default -> 1.0;
        };
    }

    /** Schmelz-Upgrade (portable Furnace/Smoker/Blast Furnace)? Nur eines pro Rucksack. */
    public boolean isFurnace() {
        return switch (id) {
            case "smelting", "blasting", "smoking" -> true;
            default -> false;
        };
    }

    /** Ob {@code id} ein Schmelz-Upgrade ist (smelting/blasting/smoking). */
    public static boolean isFurnaceId(String id) {
        FunctionUpgrade u = byId(id);
        return u != null && u.isFurnace();
    }

    public static FunctionUpgrade byId(String id) {
        if (id == null) return null;
        for (FunctionUpgrade u : values()) {
            if (u.id.equalsIgnoreCase(id)) return u;
        }
        return null;
    }
}
