package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * {@link InventoryHolder} einer portablen Schmelz-Station (Furnace / Blast
 * Furnace / Smoker), die ein Backpack über das Smelting-/Blasting-/Smoking-
 * Upgrade bereitstellt. Funktioniert wie ein tragbarer Ofen: Eingabe + Brennstoff
 * rein, Ergebnis raus. Der gesamte Zustand (Slots + Fortschritt) gehört dem
 * Backpack und wird persistent gespeichert – bei Logout, Crash oder Server-Stop
 * bleibt alles erhalten (nichts verschwindet).
 */
public final class FurnaceMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;

    // Layout (3 Reihen): Eingabe oben, Brennstoff darunter, Ergebnis rechts.
    public static final int INPUT_SLOT = 11;
    public static final int FUEL_SLOT = 20;
    public static final int OUTPUT_SLOT = 15;
    public static final int ARROW_SLOT = 13;  // Fortschritts-Pfeil
    public static final int INFO_SLOT = 4;     // Info-Item
    public static final int BACK_SLOT = 22;    // Zurück-Button

    private final UUID backpackId;
    private final String type;   // "furnace" | "blast" | "smoker"
    private final String tierKey;
    private Inventory inventory;
    private BukkitTask task;

    // Live-Schmelz-Zustand.
    private int cook;            // aktuelle Koch-Schritte am laufenden Item
    private int burn;            // verbleibende Items, die der gezündete Brennstoff noch schmilzt
    private int ticks;           // Tick-Zähler für periodisches Speichern

    public int ticks() { return ticks; }
    public int incTicks() { return ++ticks; }

    public FurnaceMenuHolder(UUID backpackId, String type, String tierKey) {
        this.backpackId = backpackId;
        this.type = type;
        this.tierKey = tierKey;
    }

    public UUID backpackId() { return backpackId; }
    public String type() { return type; }
    public String tierKey() { return tierKey; }

    public int cook() { return cook; }
    public void cook(int cook) { this.cook = cook; }
    public int burn() { return burn; }
    public void burn(int burn) { this.burn = burn; }

    public BukkitTask task() { return task; }
    public void task(BukkitTask task) { this.task = task; }

    /** Slot, in dem der Spieler Items ablegen/entnehmen darf. */
    public boolean isInteractable(int rawSlot) {
        return rawSlot == INPUT_SLOT || rawSlot == FUEL_SLOT || rawSlot == OUTPUT_SLOT;
    }

    /** Nur entnehmbarer Slot (Ergebnis). */
    public boolean isOutput(int rawSlot) {
        return rawSlot == OUTPUT_SLOT;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
