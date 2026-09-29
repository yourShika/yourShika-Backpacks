BACKPACK UI + ICONS

5 Layouts: upgrades, smelting, xp_storage, trash, compacting.
Smoker und Blasting verwenden dasselbe Portable-Furnace-Layout; separate Dateien liegen bei.

gui/: transparente RGBA-Texturen, immer 256×256. Inhalt am Ursprung (0,0), Hauptpanel 176px breit. Das Item-Raster wurde nicht skaliert: 18px Abstand, Item-Anker (8,18). Die Hintergründe enthalten keine eingebrannten Items, Item-Anzahlen oder Titel. Alle Spielerdaten zeichnet Minecraft/das Plugin.
icons_16x16/: 16 separate transparente Icons im nativen 16px-Format.
icons_32x32/: alternative höher aufgelöste Exporte derselben Entwürfe.
previews/: 4-fache Vorschau mit exemplarischen Bedien-Icons. Nicht als GUI-Textur verwenden!
layout/: genaue Positionen und aktive Slot-Indizes (nullbasiert, zeilenweise).

Die unbenutzten grauen Füllfelder der Vorlagen wurden nicht eingerahmt. Leere aktive Slots und das gesamte Spieler-Inventar bleiben sichtbar. Beim Compactor sind die Positionen 17 und 25 als inaktive Füllfelder behandelt.
Die rechten Reiter sind in den Screenshots abgeschnitten. Ihre sichtbaren Ansatzpunkte und Anzahl wurden übernommen, die äußere Form ergänzt. Reiterflächen sind leer für vorhandene Plugin-Symbole.

Zuordnung:
Blaze Powder -> smelting_progress.png
Arrow -> back.png
Barrier -> clear_filter.png
Green Dye im Compactor -> compactor_on.png / compactor_off.png
Green Dye XP -> xp_deposit_one.png
Green Block XP -> xp_deposit_all.png
XP Bottle -> xp_info.png
Yellow Dye XP -> xp_withdraw_one.png
Yellow Block XP -> xp_withdraw_all.png
Lava Bucket -> trash_delete_all.png

Das Paket enthält Texturen und Positionsdaten, keine plugin- oder versionsabhängigen Model-/Font-Konfigurationen. Ein Resourcepack-Hintergrund allein entfernt keine Füll-Items: die grauen Platzhalter muss das Plugin ausblenden/transparent darstellen. Exakte Ingame-Ausrichtung hängt zusätzlich von der Font-/GUI-Einbindung ab; bitte mit den Layoutkoordinaten abgleichen.
