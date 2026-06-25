package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} der Trash-GUI.
 *
 * <p>Im Bestätigungs-Modus ({@code trash.confirm: true}) liegen Items in den
 * Slots 0–25; ein „Löschen bestätigen"-Button (Slot {@link #CONFIRM_SLOT}) löscht
 * sie. Schließt der Spieler ohne Klick, werden die Items zurückgegeben. Ohne
 * Bestätigungs-Modus werden alle Items beim Schließen sofort verworfen.</p>
 */
public final class TrashMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int CONFIRM_SLOT = 26;
    public static final int ITEM_SLOTS = 26; // Slots 0..25 = Lösch-Bereich (im Confirm-Modus)

    private final boolean confirm;
    private Inventory inventory;

    public TrashMenuHolder(boolean confirm) {
        this.confirm = confirm;
    }

    public boolean confirm() {
        return confirm;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
