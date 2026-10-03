package de.yourshika.backpacks.hook;

import org.bukkit.Bukkit;

/**
 * Soft-Hook auf <b>PacketEvents</b>. Dient zunächst nur der <em>Erkennung</em>:
 * die Over-Stacking-Upgrades (Stack-Tiers) werden ausschließlich angeboten, wenn
 * PacketEvents vorhanden ist – so lässt sich das Plugin auch ohne spielen (die
 * Stack-Upgrades sind dann einfach nicht craftbar/gelistet).
 *
 * <p>Die eigentliche Paket-Anzeige der großen Stack-Zahlen wird später über
 * PacketEvents umgesetzt; dieser Hook kapselt die reine Verfügbarkeitsprüfung per
 * Reflection, damit das Plugin ohne harte Abhängigkeit kompiliert und läuft.</p>
 */
public final class PacketEventsHook {

    private static Boolean cached;

    private PacketEventsHook() {}

    /** Ist PacketEvents installiert & aktiv? (gecacht; via {@link #reset()} neu prüfen) */
    public static boolean isAvailable() {
        Boolean c = cached;
        if (c != null) return c;
        boolean ok = false;
        try {
            // PacketEvents wird üblicherweise als Plugin "packetevents" geladen.
            if (Bukkit.getPluginManager().isPluginEnabled("packetevents")
                    || Bukkit.getPluginManager().isPluginEnabled("PacketEvents")) {
                // API-Klasse gegenprüfen (vorhanden => nutzbar).
                Class.forName("com.github.retrooper.packetevents.PacketEvents");
                ok = true;
            }
        } catch (Throwable ignored) {
            ok = false;
        }
        cached = ok;
        return ok;
    }

    /** Erneut prüfen (falls PacketEvents nach uns geladen wird). */
    public static void reset() {
        cached = null;
    }
}
