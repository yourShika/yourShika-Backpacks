package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link InventoryHolder} der Info-/Rezept-GUI ({@code /bp info}). Hält die
 * Zuordnung Slot → Aktion, damit Klicks zur passenden Rezept-Ansicht wechseln.
 * Reine Anzeige – es lassen sich keine Items entnehmen.
 */
public final class InfoMenuHolder implements InventoryHolder {

    /** Slot → Aktion (z.B. {@code "bp:iron"}, {@code "up:copper"}, {@code "back"}). */
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public void mapAction(int slot, String action) {
        actions.put(slot, action);
    }

    public String actionAt(int slot) {
        return actions.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
