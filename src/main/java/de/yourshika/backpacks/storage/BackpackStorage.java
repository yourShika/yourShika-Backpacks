package de.yourshika.backpacks.storage;

import java.util.List;
import java.util.UUID;

/**
 * Persistenz-Abstraktion. Implementierungen müssen thread-safe genug sein,
 * dass sie aus dem Haupt-Thread (Speichern beim Schließen) und ggf. aus
 * Autosave-Tasks aufgerufen werden können.
 */
public interface BackpackStorage {

    void init() throws Exception;

    /** Lädt ein Backpack oder gibt null zurück, wenn es nicht existiert. */
    BackpackData load(UUID id);

    /** Erstellt oder aktualisiert ein Backpack. */
    void save(BackpackData data);

    void delete(UUID id);

    boolean exists(UUID id);

    List<UUID> listByOwner(UUID owner);

    int count();

    /**
     * Leichte Metadaten ALLER Backpacks (ohne die schweren Inhalts-Blobs) – für
     * Statistiken ({@code /bp stats}), Orphan-Bereinigung ({@code /bp purge}) und
     * das Aufwärmen des Owner-Index im Cache.
     */
    List<BackpackMeta> allMeta();

    void close();

    /** Kompakte Metadaten eines Backpacks (id, Besitzer, Tier, platziert). */
    record BackpackMeta(UUID id, UUID owner, String tier, boolean placed) {}
}
