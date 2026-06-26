package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * {@link InventoryHolder} der XP-Storage-GUI eines Backpacks. Reine Button-GUI:
 * Erfahrung ein-/auszahlen. Die gespeicherte Menge lebt in den Backpack-Daten.
 */
public final class XpMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int INFO_SLOT = 13;
    public static final int DEPOSIT_LEVEL = 10;
    public static final int DEPOSIT_ALL = 11;
    public static final int WITHDRAW_LEVEL = 15;
    public static final int WITHDRAW_ALL = 16;
    public static final int BACK_SLOT = 22;

    private final UUID backpackId;
    private final String tierKey;
    private Inventory inventory;

    public XpMenuHolder(UUID backpackId, String tierKey) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
