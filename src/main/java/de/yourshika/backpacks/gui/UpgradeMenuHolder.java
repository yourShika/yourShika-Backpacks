package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * {@link InventoryHolder} der separaten Upgrade-GUI eines Backpacks. Hier legt
 * der Spieler die Upgrade-Items des jeweiligen Backpacks ab; sie werden über die
 * Backpack-ID server-seitig gespeichert (dupe-sicher, wie der Hauptinhalt).
 */
public final class UpgradeMenuHolder implements InventoryHolder {

    private final UUID backpackId;
    private final String tierKey;
    private final int upgradeSlots;
    private final int size;
    private final ItemStack[] buffer; // Länge >= upgradeSlots
    private Inventory inventory;

    public UpgradeMenuHolder(UUID backpackId, String tierKey, int upgradeSlots, ItemStack[] buffer) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
        this.upgradeSlots = Math.max(0, upgradeSlots);
        this.size = upgradeSlots <= 7 ? 9 : (upgradeSlots <= 16 ? 18 : 27);
        this.buffer = buffer;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }
    public int upgradeSlots() { return upgradeSlots; }
    public int size() { return size; }
    public ItemStack[] buffer() { return buffer; }
    public int backButtonSlot() { return size - 1; }

    /** Ist der Slot ein offener Upgrade-Slot (beschreibbar)? */
    public boolean isUpgradeSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < upgradeSlots;
    }

    /** Ist der Slot gesperrt (Filler / Zurück-Button)? */
    public boolean isLocked(int rawSlot) {
        return !isUpgradeSlot(rawSlot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
