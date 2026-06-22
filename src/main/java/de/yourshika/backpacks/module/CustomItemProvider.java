package de.yourshika.backpacks.module;

import de.yourshika.backpacks.tier.BackpackTier;
import org.bukkit.inventory.ItemStack;

/**
 * Liefert das visuelle Modell/Texturen für Backpack-Items.
 *
 * <p>Standardmäßig kommt der eingebaute Vanilla-Anbieter zum Einsatz (normale
 * Items mit {@code item_model}-Component bzw. {@code CustomModelData}). Externe
 * Module (Nexo, ItemsAdder, Oraxen) können diesen Anbieter optional und
 * experimentell überlagern – aktiviert über {@code /bp modules} bzw. die
 * config.yml.</p>
 */
public interface CustomItemProvider {

    /** Anbieter-Schlüssel (z.B. {@code vanilla}, {@code nexo}). */
    String id();

    /**
     * Wendet das Modell des Anbieters auf ein bereits aufgebautes Backpack-Item
     * an (PDC, Name, Lore und Grundfarbe sind zu diesem Zeitpunkt gesetzt).
     * Best-effort: bei Fehlern bleibt das Item unverändert.
     */
    void apply(ItemStack item, BackpackTier tier);
}
