package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link InventoryHolder} der {@code /bp modules}-GUI. Hält die Zuordnung
 * Slot → Modul-ID sowie den Slot des Master-Schalters, sodass Klicks die
 * jeweiligen Hooks live an- und ausschalten können.
 */
public final class ModulesMenuHolder implements InventoryHolder {

    /** Slot → Modul-ID. */
    private final Map<Integer, String> moduleSlots = new HashMap<>();
    private Inventory inventory;

    public void mapSlot(int slot, String moduleId) {
        moduleSlots.put(slot, moduleId);
    }

    public String moduleAt(int slot) {
        return moduleSlots.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
