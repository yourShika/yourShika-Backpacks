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
    private ItemStack[] upgrades; // vorbereitet, in 0.0.1 ungenutzt
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
