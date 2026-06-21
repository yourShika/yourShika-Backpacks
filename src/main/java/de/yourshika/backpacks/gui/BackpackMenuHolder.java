package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * {@link InventoryHolder} eines geöffneten Backpacks. Dient als sichere
 * Erkennung "dieses Inventar gehört zu einem Backpack" – unabhängig von Titel
 * oder Items. Hält außerdem das Layout (welche Slots Lager bzw. gesperrt sind).
 */
public final class BackpackMenuHolder implements InventoryHolder {

    private final UUID backpackId;
    private final String tierKey;
    private final int storageSlots;   // 0 .. storageSlots-1 = Lager (frei)
    private final int infoSlot;
    private Inventory inventory;

    public BackpackMenuHolder(UUID backpackId, String tierKey, int storageSlots, int infoSlot) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
        this.storageSlots = storageSlots;
        this.infoSlot = infoSlot;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }
    public int storageSlots() { return storageSlots; }
    public int infoSlot() { return infoSlot; }

    /** Ein Slot ist gesperrt, sobald er außerhalb des Lagerbereichs liegt (Kontroll-/Upgrade-Reihe). */
    public boolean isLocked(int rawSlot) {
        return rawSlot >= storageSlots;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
