package de.yourshika.backpacks.storage;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Server-seitige Daten eines Backpacks. Der Inhalt lebt ausschließlich hier
 * (an die Backpack-ID gebunden) und niemals direkt im Item – das ist der Kern
 * des Dupe-Schutzes: Ein kopiertes Item teilt sich dasselbe Inventar und kann
 * keine Items vervielfachen.
 */
public final class BackpackData {

    private final UUID id;
    private UUID owner;            // kann null sein (z.B. Admin-Spawn)
    private String tier;
    private String mainColor;
    private String accentColor;
    private ItemStack[] contents; // Lager-Inhalt
    private ItemStack[] upgrades; // installierte Upgrade-Items
    private ItemStack[] furnace;  // portable Schmelz-Station: [0]=Eingabe, [1]=Brennstoff, [2]=Ergebnis
    private int furnaceCook;      // aktuelle Koch-Schritte (Fortschritt)
    private int furnaceBurn;      // verbleibende Items, die der gezündete Brennstoff noch schmilzt
    private ItemStack[] compactFilter; // Compacting-Whitelist (leer = alles verdichten)
    private boolean placed;
    private String world;
    private double x, y, z;
    private long created;
    private long modified;

    public BackpackData(UUID id) {
        this.id = id;
        long now = System.currentTimeMillis();
        this.created = now;
        this.modified = now;
    }

    public UUID id() { return id; }

    public UUID owner() { return owner; }
    public void owner(UUID owner) { this.owner = owner; }

    public String tier() { return tier; }
    public void tier(String tier) { this.tier = tier; }

    public String mainColor() { return mainColor; }
    public void mainColor(String mainColor) { this.mainColor = mainColor; }

    public String accentColor() { return accentColor; }
    public void accentColor(String accentColor) { this.accentColor = accentColor; }

    public ItemStack[] contents() { return contents; }
    public void contents(ItemStack[] contents) { this.contents = contents; }

    public ItemStack[] upgrades() { return upgrades; }
    public void upgrades(ItemStack[] upgrades) { this.upgrades = upgrades; }

    public ItemStack[] furnace() { return furnace; }
    public void furnace(ItemStack[] furnace) { this.furnace = furnace; }

    public int furnaceCook() { return furnaceCook; }
    public void furnaceCook(int furnaceCook) { this.furnaceCook = furnaceCook; }

    public int furnaceBurn() { return furnaceBurn; }
    public void furnaceBurn(int furnaceBurn) { this.furnaceBurn = furnaceBurn; }

    public ItemStack[] compactFilter() { return compactFilter; }
    public void compactFilter(ItemStack[] compactFilter) { this.compactFilter = compactFilter; }

    public boolean placed() { return placed; }
    public void placed(boolean placed) { this.placed = placed; }

    public String world() { return world; }
    public void world(String world) { this.world = world; }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public void position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    public long created() { return created; }
    public void created(long created) { this.created = created; }

    public long modified() { return modified; }
    public void modified(long modified) { this.modified = modified; }

    public void touch() { this.modified = System.currentTimeMillis(); }
}
