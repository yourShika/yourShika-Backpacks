package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;

/**
 * ProtocolLib-Modul (vorbereitet/Roadmap). Vorgesehen für Fake-Blöcke bei
 * platzierbaren Backpacks. Aktuell wird lediglich die Verfügbarkeit erkannt;
 * es werden noch keine Pakete manipuliert.
 */
public final class ProtocolLibModule extends AbstractModule {

    public ProtocolLibModule(YourShikaBackpacks plugin) {
        super(plugin, "protocollib", "ProtocolLib",
                "Fake-Blöcke für platzierbare Backpacks (Roadmap)", "ProtocolLib");
    }

    @Override
    protected void onEnable() {
        plugin.getLogger().info("Modul 'ProtocolLib' erkannt – Funktionen folgen (Roadmap).");
    }
}
