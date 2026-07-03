package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * {@link InventoryHolder} der Compacting-Filter-GUI eines Backpacks. Der Spieler
 * legt hier Beispiel-Items ab, die <b>verdichtet</b> werden dürfen (Whitelist).
 * Nur der Item-Typ zählt; Mengen sind egal. Ein leerer Filter bedeutet
 * „alles verdichten". Persistent pro Backpack gespeichert.
 */
public final class FilterMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int FILTER_SLOTS = 18; // Slots 0..17 sind die Whitelist
    public static final int INFO_SLOT = 22;
    public static final int BACK_SLOT = 26;

    /** Preset-Buttons (Erze/Farm/Redstone/Sonstiges) und Leeren-Button. */
    public static final int[] PRESET_SLOTS = {18, 19, 20, 21};
    public static final String[] PRESET_IDS = {"ores", "farm", "redstone", "misc"};
    public static final int CLEAR_SLOT = 23;

    /** Preset-ID an einem Slot oder null. */
    public String presetAt(int rawSlot) {
        for (int i = 0; i < PRESET_SLOTS.length; i++) {
            if (PRESET_SLOTS[i] == rawSlot) return PRESET_IDS[i];
        }
        return null;
    }

    public boolean isClear(int rawSlot) {
        return rawSlot == CLEAR_SLOT;
    }

    private final UUID backpackId;
    private final String tierKey;
    /** "compacting" oder "pickup" – bestimmt, in welches Feld gespeichert wird. */
    private final String kind;
    private Inventory inventory;

    public FilterMenuHolder(UUID backpackId, String tierKey) {
        this(backpackId, tierKey, "compacting");
    }

    public FilterMenuHolder(UUID backpackId, String tierKey, String kind) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
        this.kind = kind == null ? "compacting" : kind;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }
    public String kind() { return kind; }
    public boolean isPickup() { return "pickup".equals(kind); }

    /** Ist der Slot ein beschreibbarer Whitelist-Slot? */
    public boolean isFilterSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < FILTER_SLOTS;
    }

    public boolean isLocked(int rawSlot) {
        return !isFilterSlot(rawSlot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
