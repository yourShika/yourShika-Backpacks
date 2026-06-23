# yourShika Backpack's

> Ein eigenständiges, **vollständig serverseitiges** Backpack-System für **Paper/Spigot**.
> Spieler brauchen **keinen Client-Mod**.

[![Version](https://img.shields.io/badge/version-0.4.0-6E5BC8)](https://github.com/yourShika/yourShika-Backpacks/releases)
[![Plattform](https://img.shields.io/badge/Plattform-Paper%2026.1.2-5BE8D4)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![Lizenz](https://img.shields.io/badge/Lizenz-MIT-blue)](LICENSE)

---

## 📦 Beschreibung

**yourShika Backpack's** bringt hochwertige, **einfärbbare** Rucksäcke mit eigenen
Inventaren auf deinen Survival-Server – sicher gegen Dupe-Bugs, persistent
gespeichert und vorbereitet für eigene Custom-Texturen.

Das Plugin ist **von [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks)
([GitHub](https://github.com/P3pp3rF1y/SophisticatedBackpacks)) inspiriert**, aber eine
**komplett eigenständige Neuentwicklung**. Es wurde **kein Code, kein Asset und keine
Textur** aus der Mod übernommen. **Dies ist kein Forge-/Fabric-/NeoForge-Mod, sondern
ein Paper/Spigot-Plugin.**

---

## 🐞 v0.4.0 (Stabilität & Module)

- **Kein Freeze/Crash mehr beim Modul-Umschalten.** Ursache war das wiederholte
  Neu-Registrieren von Rezepten (`Bukkit.removeRecipe` löst je Aufruf einen
  vollen Rezept-/Advancement-Reload aus). Rezepte werden jetzt **idempotent**
  registriert und beim Umschalten **gar nicht** mehr angefasst.
- **Oraxen aus = zurück zu normaler Pferderüstung.** Beim Abschalten des
  Oraxen-Moduls wird das Custom-Modell sauber **entfernt**; beim Einschalten
  werden die Backpacks online befindlicher Spieler **automatisch aktualisiert**
  (danach ggf. `/oraxen reload`).
- **Module-/Info-GUIs:** Items lassen sich nicht mehr entnehmen/verschieben
  (war eine Folge des Freezes).
- **`/bp info`:** Smithing-Rezepte sauberer dargestellt, Slot-Beschriftungen auf
  Englisch (Template / Base / Addition) – passend zum Smithing Table.

---

## ✨ Neu in v0.3.5

- 🎨 **Echte zweite Akzentfarbe** über Oraxen-Varianten (z. B.
  `ysbp_leather_backpack_accent_red`). Die **Hauptfarbe** bleibt echtes
  `LEATHER_HORSE_ARMOR`-Tinting.
- 🧩 **Oraxen-YAMLs** mit **178 eindeutigen** Provider-/CustomModelData-Werten.
- 📦 **Versionierter Asset-Deployer:** aktualisiert alte Defaults und legt
  Backups unter `AssetBackups/` an.
- 🛟 **Oraxen-Item-YAMLs** werden **vor dem Überschreiben gesichert**.
- ⚒️ **Smithing-Fix:** Tier & Farbe werden erst beim `SmithItemEvent` geschrieben,
  nicht mehr in der Vorschau.
- 🧹 Alter `hooks.experimental`-Kommentar aus der `plugin.yml` entfernt.

---

## ✨ Neu in v0.3.0

- 🖼️ **Oraxen-Texturen integriert:** Ist das **Oraxen**-Modul aktiv, liefert das
  Plugin mitgelieferte Texturen & Item-Definitionen automatisch aus
  (`plugins/yourShika Backpack's/Textures/` zum Austauschen, Kopie nach Oraxen).
  Backpacks & Upgrade-Items erhalten so eigene Modelle – einfach `/oraxen reload`.
- 📖 **`/bp info` – Rezept-Browser:** Eine schöne GUI mit allen Rücksäcken und
  Upgrade-Items. Klick auf ein Item zeigt sein **Crafting-/Smithing-Rezept**
  (visuell, inkl. Zutaten & Ergebnis).
- 🔄 **Auto-Aktualisierung bestehender Rucksäcke:** Bei jedem Öffnen werden
  Name, Lore und Modell auf den aktuellen Stand gebracht – **ohne** ID, Farbe,
  Besitzer oder Inhalt zu verändern. Änderungen an Tier-Texten greifen so auch
  rückwirkend.
- 🔒 **Upgrade-Items missbrauchssicher:** Upgrade-Leder und Tier-Upgrades lassen
  sich **nur** in den Backpack-Rezepten verwenden – nicht mehr als Leder/Papier
  für Bücher, Karten o.ä.
- 🎨 **Doppelte Farb-Anzeige entfernt** (Vanilla-Dye-Tooltip ausgeblendet),
  Namen & Lore aufpoliert.
- 🐞 **Dye-Färben** wieder zuverlässig entnehmbar (robuste Ergebnis-Ausgabe).

---

## 🔧 v0.2.2

- **Farb-Bug behoben:** Eingefärbte Backpacks behalten ihre Farbe jetzt auch
  nach dem **Platzieren & Aufheben** (vorher Rücksprung auf Default).
- **Upgrade-Farben:** Beim Veredeln werden **Standard-Farben** auf das neue Tier
  angehoben, **individuell gefärbte** Backpacks behalten ihre Farbe.
- **Hooks automatisch an:** Externe Hooks aktivieren sich selbst, sobald das
  jeweilige Plugin installiert ist (kein „experimental"-Schalter mehr).
- **Besitzer:** Jedes Backpack merkt sich, **wer es gecraftet** hat (in der Lore).
  Optional `security.owner-only: true` → nur Besitzer (oder Admins) dürfen öffnen
  und ein platziertes Backpack aufheben. **Standard: aus** (jeder darf).
- **Lore aufgeräumt:** „(Roadmap)" entfernt, Besitzer-Zeile ergänzt.
- **Upgrade-Items vorbereitet für eigene Texturen:** CustomModelData (2000–2006)
  und optionales `item_model` je Upgrade in der Config einstellbar.
- **Netherite-Upgrade** wird (wie gewünscht) im **Smithing Table** erstellt.
- Hinweis bleibt: In **JEI/REI/EMI** zeigen die **Zutaten** der Smithing-
  Veredelung das generische Item (z. B. *Horse Leather Armor* / *Paper*) – das ist
  technisch bedingt (siehe unten), das **Ergebnis** ist korrekt benannt.

---

## 🔧 v0.2.1 (Bugfixes)

- **Config-Auto-Update:** Bei Struktur-Änderungen aktualisiert sich die
  `config.yml` automatisch (alte Datei wird als `config-backup-*.yml` gesichert) –
  so greifen neue Tiers/Rezepte/Upgrade-Regeln auch auf bestehenden Servern.
- **Nur Leder ist direkt craftbar** – alle höheren Tiers entstehen ausschließlich
  über die **Upgrade-Kette** (Smithing). Alte Direkt-Rezepte sind deaktiviert.
- **ID erscheint sofort** in der Lore beim Craften (vorher erst nach dem Öffnen).
- **Netherite-Upgrade** wird jetzt im **Smithing Table** erstellt
  (Upgrade-Leder + Netherite-Ingot + Faden — Minecraft-Smithing braucht 3 Slots).
- **Upgrade-Leder** wird im **Vorlage-Slot** (Leder) der Veredelung **abgelehnt**.
- **Dye-Färben:** Ergebnis lässt sich jetzt zuverlässig entnehmen (landet im Inventar).
- **PacketEvents statt ProtocolLib** als (Roadmap-)Hook.
- Hinweis: In **JEI/REI/EMI** zeigt der Basis-Slot der Smithing-Veredelung das
  generische *Horse Leather Armor* – technisch bedingt, weil das Rezept **jedes**
  Backpack dieses Tiers akzeptieren muss (das **Ergebnis** zeigt korrekt den Ziel-Rucksack).

---

## 🆕 Neu in v0.2.0

- 🔒 **Update-sichere Speicherung (MUSS):** Inhalte werden im versionierten
  Paper-Byte-Format gespeichert (mit Minecraft-Datenversion + DataFixer). Items
  – **inklusive beliebiger Custom-NBT/Components** – überstehen Server- und
  Plugin-Updates unbeschädigt. Alte Daten werden automatisch migriert.
- 🧵 **Upgrade-Kette:** **Leder → Copper → Eisen → Gold → Diamant → Smaragd →
  Netherite**. Aus **Upgrade-Leder** (Leder + Faden) werden mit Tier-Material
  **Tier-Upgrades**; die **Veredelung** läuft im **Smithing Table** und
  **erhält ID, Inhalt und Farbe** des Backpacks.
- 🧰 **Upgrade-GUI:** Eigenes Inventar je Backpack (Button in der Backpack-GUI),
  in das nur Upgrade-Items passen – server-seitig gespeichert.
- 🎨 **Dye-Färben im Crafting Table:** Backpack in die Mitte, Färbemittel in die
  **linke Spalte = Hauptfarbe**, **rechte Spalte = Akzentfarbe** (positions-
  unabhängig, mehrere Dyes werden zu **Hex** gemischt). Der Inhalt bleibt erhalten.
- 📦 **Platzierbare Backpacks:** **Shift-Rechtsklick** auf den Boden platziert
  (ItemDisplay + Interaction), **Rechtsklick öffnet**, **Sneak-Rechtsklick hebt
  auf** – persistent über Neustarts, geschützt vor Explosionen, nicht per Hopper
  auslesbar.
- 🐴 **Schutz:** Backpacks lassen sich **nicht** als Pferderüstung anlegen und
  **nicht** in Hopper/Automationen bewegen.
- 🔁 **`/bp update`:** Lädt die neueste Release-JAR von GitHub in den
  `plugins/update/`-Ordner (Übernahme beim Neustart; Daten bleiben erhalten).
- 🧪 **Module-GUI:** `/bp modules` schaltet Hooks **per Klick** live an/aus.
- 📖 **Recipe Book + JEI/REI/EMI:** Alle echten Rezepte (Backpacks, Upgrades,
  Smithing) sind freigeschaltet und sichtbar.
- ➖ **Custom-Item-Hooks:** nur noch **Oraxen** (Nexo & ItemsAdder entfernt).

---

## 🧱 Minecraft-/Paper-Version

- **Zielplattform:** Paper (läuft auch auf Spigot, dort ggf. eingeschränkte MiniMessage-Optik)
- **Gebaut gegen:** `io.papermc.paper:paper-api:26.1.2.build.72-stable`
- **Java:** **25** erforderlich (Minecraft 26.1.x setzt Java 25 voraus, Microsoft OpenJDK 25)
- **Versionsschema:** Seit dem neuen `YY.D.H`-Schema verwendet die Paper-API
  `<version>.build.<n>-<stage>` statt `-R0.1-SNAPSHOT`. Die Ziel-Version ist
  über die Eigenschaft `paper.version` in der [`pom.xml`](pom.xml) leicht änderbar.

---

## ✨ Features

- ✅ Backpacks als **normale Items** mit **eindeutiger, fälschungssicherer ID** (PDC)
- ✅ **Eigenes Inventar pro Backpack** – gleiche Items, unterschiedliche Inhalte
- ✅ **Persistente Speicherung** (SQLite oder YAML)
- ✅ **Sichere GUI** mit umfassendem Dupe-/Verlust-Schutz
- ✅ **Doppeltruhen-Layout (54 Slots)** – nur freigegebene Felder sind nutzbar
- ✅ **Größere Tiers = mehr Platz**, bei Überschreiten einer Seite automatisches **Paging**
- ✅ **6 Backpack-Tiers** (Leder, Kupfer, Eisen, Gold, Diamant, Netherite)
- ✅ **Echt einfärbbar** über färbbare Leder-Items (DyeColor **oder Hex `#RRGGBB`**) –
  Farbe sichtbar **auch ohne Resourcepack**
- ✅ **Crafting-Rezepte** (konfigurierbar, aktivier-/deaktivierbar)
- ✅ **Öffnen per Rechtsklick** (Haupt- & Nebenhand) **und Command**
- ✅ **Admin-Give-Command** und **Öffnen per ID**
- ✅ **CustomModelData + `item_model`-Component pro Tier** – vorbereitet für Resourcepacks
- ✅ **Modulares Hook-System** – alle externen Hooks **standardmäßig gesperrt & experimentell**,
  Live-Status über **`/bp modules`**
- ✅ **Deutsche, konfigurierbare Nachrichten** (MiniMessage + `&`-Codes)
- ✅ **Schutz gegen Backpacks-in-Backpacks**
- ❌ **Kein Vault** mehr (bewusst entfernt)

---

## 🛠️ Installation

1. Plugin-JAR aus den [Releases](https://github.com/yourShika/yourShika-Backpacks/releases)
   herunterladen (`yourShika-Backpacks-0.1.0.jar`).
2. In den `plugins/`-Ordner deines **Paper 26.1.2 (Java 25)**-Servers legen.
3. Server starten – der Datenordner **`plugins/yourShika Backpack's/`** wird automatisch
   mit `config.yml`, `messages_de.yml` und der Datenbank erstellt.
4. Optional `config.yml` anpassen und `/bp reload` ausführen.

---

## ⌨️ Commands

Hauptbefehl: `/backpack` · Aliase: `/bp`, `/ybackpack`, `/ysbackpack`

| Befehl | Beschreibung |
|---|---|
| `/bp help` | Hilfe anzeigen |
| `/bp open` | Backpack in der Hand öffnen |
| `/bp info` | Rücksäcke, Upgrades & **Rezepte** in einer GUI ansehen |
| `/bp list [Spieler]` | Backpacks auflisten |
| `/bp color <Farbe> [Akzent]` | Backpack einfärben (DyeColor-Name **oder** `#RRGGBB`) — **Admin** |
| `/bp give <Spieler> <Tier> [Anzahl] [Farbe] [Akzent]` | Backpack geben (Admin) |
| `/bp openid <ID>` | Backpack per ID öffnen (Admin) |
| `/bp modules` | Externe Module **per GUI an/aus** schalten (Admin) |
| `/bp update` | Neueste Version von GitHub laden (Admin, Neustart nötig) |
| `/bp reload` | Konfiguration neu laden (Admin) |
| `/bp version` | Plugin-Infos |

> **Färben für Spieler** läuft über den **Crafting Table** (siehe unten); der
> Befehl `/bp color` ist bewusst eine **Admin**-Funktion.

---

## 🔐 Permissions

| Permission | Beschreibung | Standard |
|---|---|---|
| `yourshika.backpack.use` | Backpacks benutzen | alle |
| `yourshika.backpack.open` | Eigenes Backpack öffnen | alle |
| `yourshika.backpack.list` | Eigene Backpacks auflisten | alle |
| `yourshika.backpack.place` | Backpacks platzieren/aufheben | alle |
| `yourshika.backpack.craft.<tier>` | Bestimmten Tier craften | alle |
| `yourshika.backpack.admin.color` | Backpack per Befehl einfärben | OP |
| `yourshika.backpack.admin.give` | Backpacks geben | OP |
| `yourshika.backpack.admin.openid` | Per ID öffnen | OP |
| `yourshika.backpack.admin.openother` | Fremde Backpacks öffnen | OP |
| `yourshika.backpack.admin.listother` | Fremde Backpacks listen | OP |
| `yourshika.backpack.admin.modules` | Modul-GUI öffnen/umschalten | OP |
| `yourshika.backpack.admin.update` | Self-Updater | OP |
| `yourshika.backpack.admin.reload` | Reload | OP |
| `yourshika.backpack.admin.debug` | Debug | OP |

---

## 🎒 Backpacks, Tiers, Größe & Paging

Jedes Backpack öffnet als **Doppeltruhe (54 Slots)**. Die unterste Reihe (9 Slots)
ist die **Steuerleiste** (Blättern, Info, Upgrade-Vorschau). Von den verbleibenden
**45 Slots** sind nur so viele **freigegeben**, wie der Tier erlaubt – der Rest ist
gesperrt. Tiers mit mehr als 45 Lager-Slots werden automatisch über mehrere
**Seiten** geblättert (◀ / ▶).

| Tier | Lager-Slots | Seiten* | Upgrade-Slots | CustomModelData |
|---|---|---|---|---|
| Leder | 9 | 1 | 1 | 1001 |
| Kupfer | 18 | 1 | 2 | 1002 |
| Eisen | 27 | 1 | 3 | 1003 |
| Gold | 45 | 1 | 4 | 1004 |
| Diamant | 54 | 2 | 5 | 1005 |
| **Smaragd** | 81 | 2 | 6 | 1006 |
| Netherite | 108 | 3 | 6 | 1007 |

\* Bei 45 nutzbaren Slots pro Seite (`gui.storage-slots-per-page`).
Die Upgrade-Slots sind über den **Upgrades-Button** in der Backpack-GUI als
eigenes Inventar nutzbar.

### Identität & Sicherheit

Jedes Backpack trägt seine **ID, Tier und Farben im PersistentDataContainer** – nicht in
Name oder Lore. Dadurch sind Backpacks **nicht durch Umbenennen fälschbar**.
**Der Inhalt wird niemals im Item gespeichert**, sondern serverseitig an die ID gebunden.
Das ist der Kern des Dupe-Schutzes: Ein kopiertes Item teilt sich dasselbe Inventar.
Beim Blättern wird stets nur die sichtbare Seite zwischen Inventar und Puffer
synchronisiert – die Operation bleibt atomar und dupe-sicher.

### Farben (echt einfärbbar)

Backpacks basieren standardmäßig auf einem **färbbaren Leder-Item**
(`LEATHER_HORSE_ARMOR`). Die **Hauptfarbe färbt das Item real ein** – sichtbar
**auch ohne Resourcepack** – und ist über `/bp color` änderbar. Als Farbe sind
sowohl **DyeColor-Namen** (`BROWN`, `CYAN`, …) als auch **Hex-Werte** (`#A0703C`)
erlaubt. Leder-Pferderüstung ist nicht vom Spieler anlegbar und eignet sich daher
ideal als Backpack-Basis.

---

## 🎨 CustomModelData, item_model & Resourcepacks

- Jeder Tier besitzt eine eigene **CustomModelData** (1001–1007) und kann optional
  eine moderne **`item_model`-Component** setzen (`item-model: "namespace:pfad"`).
- **Ohne Resourcepack** funktionieren Backpacks als normale (gefärbte) Leder-Items.
- **Mit Resourcepack** sehen sie wie eigene Items aus.
- **Externe Item-Systeme** (Nexo, ItemsAdder, Oraxen) sind **optional** und
  experimentell – siehe Hooks. Default ist immer der eingebaute Vanilla-Anbieter.

### Oraxen-Assets

Unter `src/main/resources/oraxen/` liegt eine fertige Oraxen-Beilage mit
16x16-Vanilla-Style-Texturen für alle Backpack-Tiers, Tier-Upgrade-Items und
52 vorbereitete Funktions-Upgrades (Pickup, Magnet, Filter, Void, Transfer,
Crafting/Processing, Stack, Utility, Tank/Energy/XP). Die Backpack-Texturen
nutzen drei Layer: dyebare Basis (`*_base.png`), feste Akzentvarianten
(`accents/<tier>_<dye>.png`) und Overlay für Outline, Tier-Metall und
Glanzdetails. Die Plugin-Config verweist per `provider-id` auf die Basis-IDs;
für abweichende Akzentfarben nutzt das Plugin automatisch Provider-IDs wie
`ysbp_leather_backpack_accent_red`. Erzeugt werden echte Backpacks weiterhin
über Plugin-Crafting, Smithing oder `/bp give`, damit ID, Inhalt, Besitzer und
Farbe erhalten bleiben.

Die Funktions-Upgrades sind als Modell-/Textur-Provider vorbereitet
(`ysbp_upgrade_<upgrade_key>`, CustomModelData `2100-2151`); ihre Gameplay-Logik
kann später daran angebunden werden.

---

## 🧰 Crafting, Upgrade-Kette & Färben

**Nur der Leder-Rucksack** wird direkt gecraftet (8× Leder um eine **Truhe**).
Alle höheren Tiers entstehen über die **Upgrade-Kette** im Smithing Table.
Frisch gecraftete Backpacks zeigen ihre **ID sofort** in der Lore.

**Upgrade-Kette** — Leder → Copper → Eisen → Gold → Diamant → Smaragd → Netherite:

1. **Upgrade-Leder** craften: 1× Leder (Mitte) + 4× Faden (Crafting Table).
2. **Tier-Upgrade** craften: Upgrade-Leder + 8× Tier-Material (z. B. Kupfer) →
   *Copper-Upgrade* (Crafting Table). **Netherite-Upgrade**: **Smithing Table** –
   Upgrade-Leder (Vorlage) + Netherite-Ingot (Basis) + Faden (Zugabe).
3. **Veredeln im Smithing Table:** **normales Leder** (Vorlage) + **vorheriges
   Backpack** (Basis) + **Tier-Upgrade** (Zugabe) → nächstes Backpack.
   **ID, Inhalt und Farbe bleiben erhalten.** (Upgrade-Leder wird im Vorlage-Slot
   nicht akzeptiert – dort gehört echtes Leder hin.)

**Färben (Crafting Table):** Backpack in die **mittlere Spalte**, Färbemittel in
die **linke Spalte** = Hauptfarbe, **rechte Spalte** = Akzentfarbe. Innerhalb
einer Spalte ist die Position egal; mehrere Dyes werden zu einem **Hex-Wert**
gemischt. Der Backpack-Inhalt bleibt dabei vollständig erhalten.

> Die echten Rezepte erscheinen im **Recipe Book** und in **JEI/REI/EMI**. Das
> positionsunabhängige Färben ist kein festes Rezept und daher dort nicht gelistet.

---

## 💾 Storage

- **SQLite** (Standard, empfohlen) → `backpacks.db`
- **YAML** (Alternative) → `backpacks.yml`

Gespeichert wird beim **Schließen**, beim **Seitenwechsel**, bei **Logout**,
**Plugin-Deaktivierung**, **Server-Stop** und per **Autosave**.

**Update-sicher (MUSS):** Inhalte werden im **versionierten Paper-Byte-Format**
(`serializeAsBytes`) abgelegt – inkl. Minecraft-Datenversion. Beim Laden migriert
der DataFixer automatisch auf die aktuelle Version, sodass Items mit beliebiger
Custom-NBT/Components nach Updates **nicht verloren gehen und nicht beschädigt
werden**. Ältere Daten im alten Format werden weiterhin gelesen und beim nächsten
Speichern automatisch migriert.

---

## 🔌 Externe Module / Hooks (automatisch)

Das Plugin läuft **vollständig eigenständig**. Externe Hooks sind als **Module**
gekapselt und aktivieren sich **automatisch**, sobald das jeweilige Plugin
installiert und das Modul in der Config aktiviert ist (**Standard: aktiviert**).
Fehlt das Plugin, bleibt das Modul still inaktiv.

Live-Status & Umschalten **per GUI**: **`/bp modules`** (Klick auf ein Modul
schaltet es um und lädt es neu).

| Modul | Zweck | Standard |
|---|---|---|
| **PlaceholderAPI** | `%ysbp_count%`, `%ysbp_highest_tier%`, `%ysbp_open%` | auto |
| **PacketEvents** | Packet-Darstellung platzierter Backpacks (Roadmap) | auto |
| **Oraxen** | Custom-Modelle/Texturen | auto |

> **Custom-Items:** Als Custom-Item-Hook wird ausschließlich **Oraxen** unterstützt
> (frei nutzbar, reife API). **Nexo** (kostenpflichtig) und **ItemsAdder** (Premium)
> wurden bewusst entfernt. Pro Tier kann eine `provider-id` gesetzt werden, die nur
> greift, wenn das Oraxen-Modul aktiv ist. **Vault wird bewusst nicht unterstützt.**

Einzelnes Modul deaktivieren (Beispiel):

```yaml
hooks:
  modules:
    oraxen: false
```

---

## ⚠️ Bekannte Einschränkungen

- **Funktionale Upgrade-Effekte** (Pickup, Magnet, Void, Filter …) folgen noch –
  v0.2.0 bringt die **Upgrade-Slots als Inventar** sowie die **Tier-Veredelung**.
- Ein vollständiges Server-Resourcepack wird nicht automatisch installiert; eine
  Oraxen-Beilage mit Texturen/YAML liegt unter `src/main/resources/oraxen/`.
- Externe Module sind **experimentell**; ihre Modell-Übernahme erfolgt best-effort.
- Das **positionsunabhängige Dye-Färben** ist kein festes Rezept und erscheint
  daher nicht in JEI/REI/EMI (alle echten Rezepte hingegen schon).
- **JEI/REI/EMI-Zutaten:** Bei der Smithing-Veredelung zeigen die Eingabe-Slots
  das generische Item (*Horse Leather Armor* für das Backpack, *Paper* für das
  Tier-Upgrade). Grund: Recipe-Zutaten werden nur als **Item-Typ** an den Client
  übertragen – der individuelle Name/NBT von Custom-Items geht dabei verloren.
  Das **Ergebnis** wird korrekt mit vollem Namen angezeigt. Server-seitig ist das
  nicht behebbar (es bräuchte ein client-seitiges JEI-Addon).

---

## 🗺️ Roadmap

- **Funktionale Upgrade-Effekte** (Pickup, Magnet, Void, Filter, Compacting,
  Smelting, Stack, Inception u.v.m.) – die Upgrade-Slots & -Items existieren bereits.
- **Eigenes Resourcepack** + tiefere Oraxen-Integration.
- **Backpacks-in-Backpacks** nur über das geplante **Inception Upgrade** (mit Limits & Dupe-Schutz).

---

## 🧪 Build-Hinweise

Voraussetzungen: **JDK 25** und **Maven**.

```bash
mvn clean package
```

Das fertige Plugin liegt anschließend unter:

```
target/yourShika-Backpacks-0.4.0.jar
```

Die Ziel-Paper-Version lässt sich über die Eigenschaft `paper.version` in der
[`pom.xml`](pom.xml) anpassen.

---

## 📄 Lizenz

Veröffentlicht unter der [MIT-Lizenz](LICENSE).

> Inspiriert von Sophisticated Backpacks – jedoch eine eigenständige Implementierung
> ohne übernommenen Code, ohne Assets und ohne Texturen.
