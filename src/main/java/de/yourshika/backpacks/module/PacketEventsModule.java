package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;

/**
 * PacketEvents-Modul (vorbereitet/Roadmap). Moderner, performanter Ersatz für
 * ProtocolLib – vorgesehen für Fake-Blöcke / packet-basierte Darstellung
 * platzierbarer Backpacks. Aktuell wird lediglich die Verfügbarkeit erkannt.
 */
public final class PacketEventsModule extends AbstractModule {

    public PacketEventsModule(YourShikaBackpacks plugin) {
        super(plugin, "packetevents", "PacketEvents",
                "Packet-basierte Darstellung platzierbarer Backpacks (Roadmap)", "packetevents");
    }

    @Override
    protected void onEnable() throws Throwable {
        // API-Klasse anstoßen, damit eine fehlende Bibliothek sofort auffällt.
        Class.forName("com.github.retrooper.packetevents.PacketEvents");
        plugin.getLogger().info("Modul 'PacketEvents' erkannt – Funktionen folgen (Roadmap).");
    }
}
