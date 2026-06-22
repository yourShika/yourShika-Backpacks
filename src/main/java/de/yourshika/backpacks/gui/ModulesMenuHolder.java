package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} der reinen Anzeige-GUI {@code /bp modules}. Dient nur
 * zur sicheren Erkennung des Menüs; jede Interaktion wird unterbunden.
 */
public final class ModulesMenuHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
