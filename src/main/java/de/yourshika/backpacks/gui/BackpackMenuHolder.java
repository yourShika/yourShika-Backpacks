package de.yourshika.backpacks.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * {@link InventoryHolder} eines geöffneten Backpacks. Dient als sichere
 * Erkennung "dieses Inventar gehört zu einem Backpack" – unabhängig von Titel
 * oder Items – und hält den kompletten Paging-Zustand.
 *
 * <p>Das Fenster ist stets eine Doppeltruhe (54 Slots). Die obersten Reihen
 * sind Lager (max. {@code slotsPerPage} nutzbar), die unterste Reihe (Slots
 * 45–53) ist die gesperrte Steuerleiste (Blättern, Info, Upgrade-Vorschau).</p>
 *
 * <p>Der gesamte Inhalt lebt im {@code buffer} (Länge ≥ Kapazität). Beim
 * Blättern und Speichern wird stets nur die aktuell sichtbare Seite zwischen
 * Inventar und Buffer synchronisiert – das hält die Operation atomar und
 * dupe-sicher.</p>
 */
public final class BackpackMenuHolder implements InventoryHolder {

    public static final int INVENTORY_SIZE = 54;
    public static final int CONTROL_ROW_START = 45; // Slots 45..53 = Steuerleiste

    // Feste Plätze in der Steuerleiste.
    public static final int PREV_SLOT = 45;
    public static final int UPGRADE_BUTTON = 47; // öffnet die separate Upgrade-GUI
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;
    // Stations-Buttons (nur sichtbar, wenn das passende Funktions-Upgrade verbaut ist).
    public static final int STATION_TRASH = 46;
    public static final int STATION_ENDER = 48;
    public static final int STATION_CRAFTING = 50;
    public static final int STATION_STONECUTTER = 51;
    public static final int STATION_SMITHING = 52;

    /** Liefert die Stations-ID eines Slots oder null. */
    public static String stationAt(int rawSlot) {
        return switch (rawSlot) {
            case STATION_TRASH -> "trash";
            case STATION_ENDER -> "ender_link";
            case STATION_CRAFTING -> "crafting";
            case STATION_STONECUTTER -> "stonecutter";
            case STATION_SMITHING -> "smithing";
            default -> null;
        };
    }

    private final UUID backpackId;
    private final String tierKey;
    private final int capacity;       // nutzbare Lager-Slots insgesamt
    private final int slotsPerPage;   // nutzbare Lager-Slots pro Seite (1..45)
    private final int pageCount;
    private final ItemStack[] buffer; // kompletter Inhalt (Länge >= capacity)
    private final String mainColor;   // Token (Dye/Hex) – für Info-Item
    private final String accentColor; // Token (Dye/Hex) – für Info-Item

    private int currentPage;
    private Inventory inventory;

    public BackpackMenuHolder(UUID backpackId, String tierKey, int capacity,
                              int slotsPerPage, ItemStack[] buffer,
                              String mainColor, String accentColor) {
        this.backpackId = backpackId;
        this.tierKey = tierKey;
        this.capacity = Math.max(1, capacity);
        this.slotsPerPage = Math.max(1, Math.min(45, slotsPerPage));
        this.pageCount = Math.max(1, (this.capacity + this.slotsPerPage - 1) / this.slotsPerPage);
        this.buffer = buffer;
        this.mainColor = mainColor;
        this.accentColor = accentColor;
        this.currentPage = 0;
    }

    public UUID backpackId() { return backpackId; }
    public String tierKey() { return tierKey; }
    public String mainColor() { return mainColor; }
    public String accentColor() { return accentColor; }
    public int capacity() { return capacity; }
    public int slotsPerPage() { return slotsPerPage; }
    public int pageCount() { return pageCount; }
    public int currentPage() { return currentPage; }
    public void currentPage(int page) { this.currentPage = Math.max(0, Math.min(pageCount - 1, page)); }
    public ItemStack[] buffer() { return buffer; }
    public boolean hasPaging() { return pageCount > 1; }

    /** Globaler Buffer-Index, an dem die aktuelle Seite beginnt. */
    public int pageBase() {
        return currentPage * slotsPerPage;
    }

    /** Anzahl aktiver (nutzbarer) Lager-Slots auf der aktuellen Seite. */
    public int activeCount() {
        int remaining = capacity - pageBase();
        return Math.max(0, Math.min(slotsPerPage, remaining));
    }

    /**
     * Ein Slot ist gesperrt, wenn er in der Steuerleiste liegt oder ein
     * Lager-Slot ist, der auf dieser Seite nicht aktiv ist.
     */
    public boolean isLocked(int rawSlot) {
        if (rawSlot >= CONTROL_ROW_START) return true;
        return rawSlot >= activeCount();
    }

    public boolean isUpgradeButton(int rawSlot) {
        return rawSlot == UPGRADE_BUTTON;
    }

    public boolean isPrevButton(int rawSlot) {
        return hasPaging() && rawSlot == PREV_SLOT && currentPage > 0;
    }

    public boolean isNextButton(int rawSlot) {
        return hasPaging() && rawSlot == NEXT_SLOT && currentPage < pageCount - 1;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
