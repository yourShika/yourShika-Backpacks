package de.yourshika.backpacks.storage;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory-Cache mit asynchronem Write-Behind vor einem beliebigen
 * {@link BackpackStorage}-Backend (SQLite/YAML).
 *
 * <h3>Warum</h3>
 * Zuvor lief jede Aktion (Öffnen, Seitenwechsel, Pickup, Furnace-Tick,
 * Placeholder-Auflösung) synchron gegen die Datenbank auf dem Haupt-Thread –
 * der größte TPS-Risikofaktor. Dieser Wrapper:
 * <ul>
 *   <li>bedient <b>Lesezugriffe aus dem Speicher</b> (Cache), sodass der
 *       Haupt-Thread nicht mehr pro Aktion auf die Platte wartet;</li>
 *   <li>schreibt Änderungen <b>gebündelt und asynchron</b> zurück
 *       (Write-Behind), nicht mehr bei jeder Mutation einzeln;</li>
 *   <li>hält einen <b>In-Memory-Owner-Index</b>, sodass
 *       {@link #listByOwner(UUID)}, {@link #exists(UUID)} und {@link #count()}
 *       ohne DB-Abfrage auskommen.</li>
 * </ul>
 *
 * <h3>Konsistenz</h3>
 * Jede Mutation markiert den Datensatz erneut als "dirty"; ein evtl. während der
 * Serialisierung veränderter Slot wird beim nächsten Flush automatisch korrigiert
 * (self-healing). Beim {@link #close()} (Server-Stopp) wird <b>synchron</b>
 * geflusht, sodass kein bestätigter Speicherstand verloren geht. Das verbleibende
 * Verlustfenster (harter Crash zwischen zwei Flushes) ist deutlich kleiner als
 * das bisherige Autosave-Intervall.
 */
public final class CachedBackpackStorage implements BackpackStorage {

    /** Sentinel im {@code ownerOf}-Index für Datensätze ohne Besitzer. */
    private static final UUID NULL_OWNER = new UUID(0L, 0L);

    private final YourShikaBackpacks plugin;
    private final BackpackStorage delegate;
    private final long flushIntervalTicks;

    private final Map<UUID, BackpackData> cache = new ConcurrentHashMap<>();
    /** Stabile Momentaufnahmen zum asynchronen Flushen (Deep-Copies vom save()-Zeitpunkt). */
    private final Map<UUID, BackpackData> pending = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerOf = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> byOwner = new ConcurrentHashMap<>();
    private final Set<UUID> known = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> deletions = ConcurrentHashMap.newKeySet();

    private final Object flushLock = new Object();
    private BukkitTask flushTask;

    public CachedBackpackStorage(YourShikaBackpacks plugin, BackpackStorage delegate, long flushIntervalTicks) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.flushIntervalTicks = Math.max(20L, flushIntervalTicks);
    }

    @Override
    public void init() throws Exception {
        delegate.init();
        // Owner-Index aus leichten Metadaten aufwärmen (keine schweren Blobs laden).
        for (BackpackMeta meta : delegate.allMeta()) {
            known.add(meta.id());
            UUID owner = meta.owner();
            ownerOf.put(meta.id(), owner == null ? NULL_OWNER : owner);
            if (owner != null) byOwner.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(meta.id());
        }
        // Periodischer, asynchroner Write-Behind-Flush.
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::flush, flushIntervalTicks, flushIntervalTicks);
        plugin.getLogger().info("Backpack-Cache aktiv (" + known.size()
                + " bekannt, Flush alle " + (flushIntervalTicks / 20L) + "s).");
    }

    @Override
    public BackpackData load(UUID id) {
        BackpackData cached = cache.get(id);
        if (cached != null) return cached;
        BackpackData data = delegate.load(id);
        if (data == null) return null;
        cache.put(id, data);
        known.add(id);
        indexOwnerIfAbsent(id, data.owner());
        return data;
    }

    @Override
    public void save(BackpackData data) {
        UUID id = data.id();
        cache.put(id, data);
        // Stabile Momentaufnahme (Deep-Copy) auf dem Haupt-Thread anlegen – NUR diese
        // wird später asynchron serialisiert. So kann der Flush kein halb-verändertes
        // Live-Objekt schreiben (Torn-Write / Item-Verlust bei z.B. schnellem Pickup).
        pending.put(id, data.copy());
        known.add(id);
        deletions.remove(id);
        reindexOwner(id, data.owner());
        dirty.add(id);
    }

    @Override
    public void delete(UUID id) {
        cache.remove(id);
        pending.remove(id);
        known.remove(id);
        dirty.remove(id);
        removeOwnerIndex(id);
        deletions.add(id);
    }

    @Override
    public boolean exists(UUID id) {
        return known.contains(id);
    }

    @Override
    public List<UUID> listByOwner(UUID owner) {
        Set<UUID> ids = byOwner.get(owner);
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

    @Override
    public int count() {
        return known.size();
    }

    @Override
    public List<BackpackMeta> allMeta() {
        // Erst alle ausstehenden Schreibvorgänge sichern, damit die Metadaten des
        // Backends den aktuellen Stand widerspiegeln (für /bp stats und /bp purge).
        flushNow();
        return delegate.allMeta();
    }

    @Override
    public void close() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        flushNow();
        delegate.close();
    }

    /** Erzwingt einen sofortigen (synchronen) Flush – z.B. beim Shutdown. */
    public void flushNow() {
        flush();
    }

    private void flush() {
        synchronized (flushLock) {
            if (!deletions.isEmpty()) {
                for (UUID id : new ArrayList<>(deletions)) {
                    try {
                        delegate.delete(id);
                        deletions.remove(id);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Backpack " + id + " konnte nicht gelöscht werden (Flush): " + ex.getMessage());
                    }
                }
            }
            if (!dirty.isEmpty()) {
                for (UUID id : new ArrayList<>(dirty)) {
                    // Dirty-Flag ZUERST entfernen: ein gleichzeitiger save() setzt es dann
                    // erneut (+ neue Momentaufnahme) und geht nicht verloren.
                    dirty.remove(id);
                    BackpackData snap = pending.get(id);
                    if (snap == null) continue;
                    try {
                        delegate.save(snap);
                        // Nur DIESE Momentaufnahme entfernen – eine zwischenzeitlich neuere
                        // (durch parallelen save()) bleibt erhalten und wird als "dirty"
                        // beim nächsten Flush geschrieben.
                        pending.remove(id, snap);
                    } catch (Exception ex) {
                        dirty.add(id); // erneut versuchen
                        plugin.getLogger().warning("Backpack " + id + " konnte nicht gespeichert werden (Flush): " + ex.getMessage());
                    }
                }
            }
        }
    }

    // --- Owner-Index-Pflege -------------------------------------------------

    private void indexOwnerIfAbsent(UUID id, UUID owner) {
        ownerOf.putIfAbsent(id, owner == null ? NULL_OWNER : owner);
        if (owner != null) byOwner.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    private void reindexOwner(UUID id, UUID newOwner) {
        UUID prev = ownerOf.get(id);
        UUID prevReal = (prev == null || prev.equals(NULL_OWNER)) ? null : prev;
        if (prevReal != null && !prevReal.equals(newOwner)) {
            Set<UUID> set = byOwner.get(prevReal);
            if (set != null) set.remove(id);
        }
        ownerOf.put(id, newOwner == null ? NULL_OWNER : newOwner);
        if (newOwner != null) byOwner.computeIfAbsent(newOwner, k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    private void removeOwnerIndex(UUID id) {
        UUID prev = ownerOf.remove(id);
        if (prev != null && !prev.equals(NULL_OWNER)) {
            Set<UUID> set = byOwner.get(prev);
            if (set != null) set.remove(id);
        }
    }
}
