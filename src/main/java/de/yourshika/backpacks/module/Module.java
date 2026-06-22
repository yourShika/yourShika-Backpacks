package de.yourshika.backpacks.module;

/**
 * Ein externes, optionales und <strong>experimentelles</strong> Modul (Hook).
 *
 * <p>Module laden nur, wenn der globale Master-Schalter
 * {@code hooks.experimental} aktiv ist, das Modul einzeln in der Config
 * aktiviert wurde und das benötigte Plugin installiert ist. Fehlt etwas davon,
 * bleibt das Modul inaktiv – das Plugin läuft vollständig eigenständig weiter.</p>
 *
 * <p>Der Live-Status aller Module ist im Spiel über {@code /bp modules}
 * einsehbar.</p>
 */
public interface Module {

    /** Stabiler Config-/intern-Schlüssel (z.B. {@code placeholderapi}). */
    String id();

    /** Anzeigename für die GUI (z.B. {@code PlaceholderAPI}). */
    String displayName();

    /** Kurze Beschreibung des Modulzwecks. */
    String description();

    /** Name des benötigten Bukkit-Plugins (für die Erkennung). */
    String requiredPlugin();

    /** Ist das benötigte Plugin auf dem Server installiert? */
    boolean isPluginPresent();

    /** Ist das Modul einzeln in der Config aktiviert? */
    boolean isEnabledInConfig();

    /** Läuft das Modul aktuell? */
    boolean isActive();

    /** Versucht, das Modul zu aktivieren. Wirft bei Fehlern. */
    void enable() throws Throwable;

    /** Fährt das Modul wieder herunter (idempotent). */
    void disable();
}
