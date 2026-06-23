package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} der Trash-GUI. Alles, was beim Schließen darin liegt,
 * wird gelöscht (die Inventar-Instanz wird einfach verworfen).
 */
public final class TrashMenuHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
