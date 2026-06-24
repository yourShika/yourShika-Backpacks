package de.yourshika.backpacks.storage;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.util.ItemSerialization;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-Implementierung der Persistenz. Eine einzige Verbindung wird gehalten
 * und alle Zugriffe sind synchronisiert – für ein Backpack-Plugin ist das
 * ausreichend schnell und vermeidet Race-Conditions / korrupte Daten.
 */
public final class SqliteBackpackStorage implements BackpackStorage {

    private final YourShikaBackpacks plugin;
    private final File file;
    private Connection connection;
    private final Object lock = new Object();

    public SqliteBackpackStorage(YourShikaBackpacks plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    @Override
    public void init() throws Exception {
        // Treiber explizit registrieren (robust auch nach Relocation/Shading).
        DriverManager.registerDriver(new org.sqlite.JDBC());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Datenordner konnte nicht erstellt werden: " + parent);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS backpacks (" +
                        "id TEXT PRIMARY KEY," +
                        "owner TEXT," +
                        "tier TEXT NOT NULL," +
                        "main_color TEXT," +
                        "accent_color TEXT," +
                        "contents TEXT," +
                        "upgrades TEXT," +
                        "furnace TEXT," +
                        "furnace_cook INTEGER DEFAULT 0," +
                        "furnace_burn INTEGER DEFAULT 0," +
                        "compact_filter TEXT," +
                        "placed INTEGER DEFAULT 0," +
                        "world TEXT," +
                        "x REAL DEFAULT 0," +
                        "y REAL DEFAULT 0," +
                        "z REAL DEFAULT 0," +
                        "created INTEGER," +
                        "modified INTEGER)")) {
            ps.executeUpdate();
        }
        // Bestehende Datenbanken um neue Spalten erweitern (Fehler ignorieren, falls vorhanden).
        addColumnIfMissing("furnace", "TEXT");
        addColumnIfMissing("furnace_cook", "INTEGER DEFAULT 0");
        addColumnIfMissing("furnace_burn", "INTEGER DEFAULT 0");
        addColumnIfMissing("compact_filter", "TEXT");
        plugin.getLogger().info("SQLite-Speicher initialisiert (" + file.getName() + ").");
    }

    /** Fügt eine Spalte hinzu, falls sie noch nicht existiert (für DB-Migrationen). */
    private void addColumnIfMissing(String column, String definition) {
        try (PreparedStatement ps = connection.prepareStatement(
                "ALTER TABLE backpacks ADD COLUMN " + column + " " + definition)) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // Spalte existiert bereits.
        }
    }

    @Override
    public BackpackData load(UUID id) {
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM backpacks WHERE id = ?")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return read(rs);
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Backpack " + id + " konnte nicht geladen werden: " + ex.getMessage());
                return null;
            }
        }
    }

    private BackpackData read(ResultSet rs) throws SQLException {
        BackpackData data = new BackpackData(UUID.fromString(rs.getString("id")));
        String owner = rs.getString("owner");
        data.owner(owner == null ? null : UUID.fromString(owner));
        data.tier(rs.getString("tier"));
        data.mainColor(rs.getString("main_color"));
        data.accentColor(rs.getString("accent_color"));
        data.contents(ItemSerialization.fromBase64(rs.getString("contents")));
        data.upgrades(ItemSerialization.fromBase64(rs.getString("upgrades")));
        data.furnace(ItemSerialization.fromBase64(rs.getString("furnace")));
        data.furnaceCook(rs.getInt("furnace_cook"));
        data.furnaceBurn(rs.getInt("furnace_burn"));
        data.compactFilter(ItemSerialization.fromBase64(rs.getString("compact_filter")));
        data.placed(rs.getInt("placed") != 0);
        data.world(rs.getString("world"));
        data.position(rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
        data.created(rs.getLong("created"));
        data.modified(rs.getLong("modified"));
        return data;
    }

    @Override
    public void save(BackpackData data) {
        data.touch();
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO backpacks (id, owner, tier, main_color, accent_color, contents, upgrades, furnace, furnace_cook, furnace_burn, compact_filter, placed, world, x, y, z, created, modified) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "owner=excluded.owner, tier=excluded.tier, main_color=excluded.main_color, " +
                            "accent_color=excluded.accent_color, contents=excluded.contents, upgrades=excluded.upgrades, " +
                            "furnace=excluded.furnace, furnace_cook=excluded.furnace_cook, furnace_burn=excluded.furnace_burn, " +
                            "compact_filter=excluded.compact_filter, " +
                            "placed=excluded.placed, world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, " +
                            "modified=excluded.modified")) {
                ps.setString(1, data.id().toString());
                ps.setString(2, data.owner() == null ? null : data.owner().toString());
                ps.setString(3, data.tier());
                ps.setString(4, data.mainColor());
                ps.setString(5, data.accentColor());
                ps.setString(6, ItemSerialization.toBase64(data.contents()));
                ps.setString(7, ItemSerialization.toBase64(data.upgrades()));
                ps.setString(8, ItemSerialization.toBase64(data.furnace()));
                ps.setInt(9, data.furnaceCook());
                ps.setInt(10, data.furnaceBurn());
                ps.setString(11, ItemSerialization.toBase64(data.compactFilter()));
                ps.setInt(12, data.placed() ? 1 : 0);
                ps.setString(13, data.world());
                ps.setDouble(14, data.x());
                ps.setDouble(15, data.y());
                ps.setDouble(16, data.z());
                ps.setLong(17, data.created());
                ps.setLong(18, data.modified());
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Backpack " + data.id() + " konnte nicht gespeichert werden: " + ex.getMessage());
            }
        }
    }

    @Override
    public void delete(UUID id) {
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM backpacks WHERE id = ?")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Backpack " + id + " konnte nicht gelöscht werden: " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean exists(UUID id) {
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM backpacks WHERE id = ?")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException ex) {
                return false;
            }
        }
    }

    @Override
    public List<UUID> listByOwner(UUID owner) {
        List<UUID> result = new ArrayList<>();
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM backpacks WHERE owner = ?")) {
                ps.setString(1, owner.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(UUID.fromString(rs.getString("id")));
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Backpacks von " + owner + " konnten nicht gelistet werden: " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public int count() {
        synchronized (lock) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM backpacks");
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException ex) {
                return 0;
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
