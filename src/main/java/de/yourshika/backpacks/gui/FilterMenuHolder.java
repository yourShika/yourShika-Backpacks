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

    private final UUID backpackId;
    private final String tierKey;
    private Inventory inventory;

    public FilterMenuHolder(UUID backpackId, String tierKey) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }

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
