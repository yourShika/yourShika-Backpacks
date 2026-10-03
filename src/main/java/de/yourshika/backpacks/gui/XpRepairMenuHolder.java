package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link InventoryHolder} des Advanced-XP-Reparatur-Menüs. Zeigt die beschädigten
 * Mending-Items des Spielers als anklickbare Buttons; ein Klick repariert genau
 * dieses Item mit dem im Backpack gespeicherten XP.
 *
 * <p><b>Dupe-sicher:</b> Die Buttons sind nur Anzeige-Kopien – die echten Items
 * bleiben im Spieler-Inventar, es ändert sich ausschließlich die Haltbarkeit. Das
 * Mapping {@code GUI-Slot -> Spieler-Inventar-Index} wird beim Klick neu validiert.</p>
 */
public final class XpRepairMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int ITEM_SLOTS = 18;      // Slots 0..17 = Item-Buttons
    public static final int INFO_SLOT = 22;
    public static final int REPAIR_ALL_SLOT = 20;
    public static final int BACK_SLOT = 26;

    private final UUID backpackId;
    private final String tierKey;
    /** GUI-Slot -> Spieler-Inventar-Index der angezeigten Items (beim Render gesetzt). */
    private final Map<Integer, Integer> slotToInv = new HashMap<>();
    private Inventory inventory;

    public XpRepairMenuHolder(UUID backpackId, String tierKey) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }

    public void clearMapping() { slotToInv.clear(); }
    public void map(int guiSlot, int invIndex) { slotToInv.put(guiSlot, invIndex); }
    /** Spieler-Inventar-Index zu einem GUI-Slot (oder -1). */
    public int invIndexAt(int guiSlot) { return slotToInv.getOrDefault(guiSlot, -1); }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
