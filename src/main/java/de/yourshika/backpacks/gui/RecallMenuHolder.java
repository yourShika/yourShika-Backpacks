package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link InventoryHolder} der Recall-Auswahl-GUI ({@code /bp recall} bei mehreren
 * platzierten Backpacks). Hält die Zuordnung Slot → Backpack-ID sowie den Slot
 * des „Alle zurückholen"-Buttons.
 */
public final class RecallMenuHolder implements InventoryHolder {

    private final Map<Integer, UUID> slots = new HashMap<>();
    private int allSlot = -1;
    private Inventory inventory;

    public void map(int slot, UUID id) { slots.put(slot, id); }
    public UUID at(int slot) { return slots.get(slot); }
    public void allSlot(int slot) { this.allSlot = slot; }
    public boolean isAll(int slot) { return slot >= 0 && slot == allSlot; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
