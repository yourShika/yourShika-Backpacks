# Oraxen-Assets fuer yourShika Backpack's

Diese Dateien sind eine vorkonfigurierte Oraxen-Beilage fuer die Backpack-,
Tier-Upgrade- und Funktions-Upgrade-Texturen. Alle Texturen sind 64x64-PNGs im
Vanilla-nahen Pixelart-Stil.

## Installation

1. `items/yourshika_backpacks.yml` und `items/yourshika_function_upgrades.yml`
   nach `plugins/Oraxen/items/` kopieren.
2. Den Ordner `pack/textures/ysbp/` nach `plugins/Oraxen/pack/textures/ysbp/`
   kopieren.
3. Oraxen neu laden und den Pack erneut senden:

```text
/o reload all
/o pack send @a
```

## Provider-IDs: Plugin-Config

Die Plugin-Config verwendet diese Oraxen-IDs als `provider-id`:

```text
ysbp_leather_backpack
ysbp_copper_backpack
ysbp_iron_backpack
ysbp_gold_backpack
ysbp_diamond_backpack
ysbp_emerald_backpack
ysbp_netherite_backpack
ysbp_upgrade_base
ysbp_upgrade_copper
ysbp_upgrade_iron
ysbp_upgrade_gold
ysbp_upgrade_diamond
ysbp_upgrade_emerald
ysbp_upgrade_netherite
```

Backpacks werden weiterhin ueber `/bp give`, Crafting oder Smithing erzeugt.
Die Oraxen-Items dienen dem Plugin nur als Modell-/Textur-Provider, damit ID,
Besitzer, Inhalt, Lore und Faerbung beim Backpack-Item bleiben.

Die Backpack-Texturen bestehen aus zwei Layern:

- `*_base.png`: dyebare Hauptflaeche fuer die Lederfaerbung.
- `*_overlay.png`: feste Gurte, Tier-Metall und Akzentdetails.

Die Oraxen-Items setzen `color: 255, 255, 255`, damit die Basis hell bleibt.
Das Plugin schreibt beim echten Backpack-Item danach die dynamische
Lederfaerbung aus `main-color`.

## Vorbereitete Funktions-Upgrades

`items/yourshika_function_upgrades.yml` enthaelt 52 vorbereitete Oraxen-Items
mit CustomModelData `2100-2151`. Die IDs folgen diesem Schema:

```text
ysbp_upgrade_<upgrade_key>
```

Beispiele:

```text
ysbp_upgrade_pickup
ysbp_upgrade_advanced_pickup
ysbp_upgrade_magnet
ysbp_upgrade_advanced_filter
ysbp_upgrade_auto_smelting
ysbp_upgrade_stack_omega
ysbp_upgrade_infinity_admin
ysbp_upgrade_battery
```

Die zugehoerigen Texturen liegen unter:

```text
pack/textures/ysbp/upgrades/functions/
```

Diese Funktions-Upgrades sind bewusst nur als Textur-/Modell-Provider
vorbereitet. Die eigentliche Gameplay-Logik kann spaeter per Plugin-Code oder
Config an diese IDs angebunden werden.

Wenn du statt `item_model` zusaetzlich klassische CustomModelData-Pfade brauchst,
aktiviere in Oraxen `Pack.generation.appearance.model_data_float` und bei Bedarf
`generate_predicates`. Die YAMLs setzen passende Werte `1001-1007`, `2000-2006`
und `2100-2151`.
