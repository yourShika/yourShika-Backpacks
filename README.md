# yourShika Backpack's

> Ein eigenständiges, **vollständig serverseitiges** Backpack-System für **Paper/Spigot**.
> Spieler brauchen **keinen Client-Mod**.

[![Version](https://img.shields.io/badge/version-0.1.0-6E5BC8)](https://github.com/yourShika/yourShika-Backpacks/releases)
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
| `/bp color <Farbe> [Akzent]` | Backpack einfärben (DyeColor-Name **oder** `#RRGGBB`) |
| `/bp list [Spieler]` | Backpacks auflisten |
| `/bp give <Spieler> <Tier> [Anzahl] [Farbe] [Akzent]` | Backpack geben (Admin) |
| `/bp openid <ID>` | Backpack per ID öffnen (Admin) |
| `/bp modules` | Externe Module & ihren Status anzeigen (Admin) |
| `/bp reload` | Konfiguration neu laden (Admin) |
| `/bp version` | Plugin-Infos |

---

## 🔐 Permissions

| Permission | Beschreibung | Standard |
|---|---|---|
| `yourshika.backpack.use` | Backpacks benutzen | alle |
| `yourshika.backpack.open` | Eigenes Backpack öffnen | alle |
| `yourshika.backpack.color` | Backpack einfärben | alle |
| `yourshika.backpack.list` | Eigene Backpacks auflisten | alle |
| `yourshika.backpack.craft.<tier>` | Bestimmten Tier craften | alle |
| `yourshika.backpack.admin.give` | Backpacks geben | OP |
| `yourshika.backpack.admin.openid` | Per ID öffnen | OP |
| `yourshika.backpack.admin.openother` | Fremde Backpacks öffnen | OP |
| `yourshika.backpack.admin.listother` | Fremde Backpacks listen | OP |
| `yourshika.backpack.admin.modules` | Modul-Übersicht öffnen | OP |
| `yourshika.backpack.admin.reload` | Reload | OP |
| `yourshika.backpack.admin.debug` | Debug | OP |

---

## 🎒 Backpacks, Tiers, Größe & Paging

Jedes Backpack öffnet als **Doppeltruhe (54 Slots)**. Die unterste Reihe (9 Slots)
ist die **Steuerleiste** (Blättern, Info, Upgrade-Vorschau). Von den verbleibenden
**45 Slots** sind nur so viele **freigegeben**, wie der Tier erlaubt – der Rest ist
gesperrt. Tiers mit mehr als 45 Lager-Slots werden automatisch über mehrere
**Seiten** geblättert (◀ / ▶).

| Tier | Lager-Slots | Seiten* | Upgrade-Slots** | CustomModelData |
|---|---|---|---|---|
| Leder | 9 | 1 | 1 | 1001 |
| Kupfer | 18 | 1 | 2 | 1002 |
| Eisen | 27 | 1 | 3 | 1003 |
| Gold | 45 | 1 | 4 | 1004 |
| Diamant | 54 | 2 | 5 | 1005 |
| Netherite | 108 | 3 | 6 | 1006 |

\* Bei 45 nutzbaren Slots pro Seite (`gui.storage-slots-per-page`).
\*\* Upgrade-Slots sind **vorbereitet und gesperrt** (Roadmap).

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

- Jeder Tier besitzt eine eigene **CustomModelData** (1001–1006) und kann optional
  eine moderne **`item_model`-Component** setzen (`item-model: "namespace:pfad"`).
- **Ohne Resourcepack** funktionieren Backpacks als normale (gefärbte) Leder-Items.
- **Mit Resourcepack** sehen sie wie eigene Items aus.
- **Externe Item-Systeme** (Nexo, ItemsAdder, Oraxen) sind **optional** und
  experimentell – siehe Hooks. Default ist immer der eingebaute Vanilla-Anbieter.

---

## 🧰 Crafting

Standardmäßig craftet man jeden Tier aus 8× Tier-Material um eine **Truhe**.
Rezepte sind je Tier in der `config.yml` frei anpassbar. Gecraftete Backpacks sind
**Templates ohne ID**; die eindeutige ID wird **beim ersten Öffnen** vergeben.

---

## 💾 Storage

- **SQLite** (Standard, empfohlen) → `backpacks.db`
- **YAML** (Alternative) → `backpacks.yml`

Gespeichert wird beim **Schließen**, beim **Seitenwechsel**, bei **Logout**,
**Plugin-Deaktivierung**, **Server-Stop** und per **Autosave**.

---

## 🔌 Externe Module / Hooks (experimentell, standardmäßig gesperrt)

Das Plugin läuft **vollständig eigenständig**. Externe Hooks sind als **Module**
gekapselt und stehen unter einem **Master-Schalter** `hooks.experimental`
(Standard: `false`). Solange dieser `false` ist, lädt **kein** externes Modul –
unabhängig von den Einzel-Schaltern.

Den Live-Status zeigt **`/bp modules`** (installiert / in Config aktiviert /
experimentell freigegeben / **AKTIV**-**INAKTIV**).

| Modul | Zweck | Standard |
|---|---|---|
| **PlaceholderAPI** | `%ysbp_count%`, `%ysbp_highest_tier%`, `%ysbp_open%` | gesperrt |
| **ProtocolLib** | Fake-Blocks für platzierte Backpacks (Roadmap) | gesperrt |
| **Nexo** | Custom-Modelle/Texturen (**empfohlener** Oraxen-Nachfolger) | gesperrt |
| **ItemsAdder** | Custom-Modelle/Texturen | gesperrt |
| **Oraxen** | Custom-Modelle/Texturen (Legacy) | gesperrt |

> **Empfehlung Custom-Items:** Für neue Setups ist **Nexo** die zukunftssicherste
> Wahl (offener, aktiv gepflegter Oraxen-Nachfolger). ItemsAdder bleibt eine
> mächtige Alternative, Oraxen ist als Legacy weiter wählbar. Pro Tier kann eine
> `provider-id` gesetzt werden, die nur greift, wenn das jeweilige Modul aktiv ist.
> **Vault wird bewusst nicht mehr unterstützt.**

Aktivieren (Beispiel PlaceholderAPI):

```yaml
hooks:
  experimental: true
  modules:
    placeholderapi: true
```

---

## ⚠️ Bekannte Einschränkungen

- **Upgrades** sind vorbereitet/gesperrt (Roadmap).
- **Platzierbare Backpacks** sind vorbereitet (Roadmap).
- **Custom-Texturen/Resourcepack** liegen nicht bei (nur vorbereitet).
- Externe Module sind **experimentell**; ihre Modell-Übernahme erfolgt best-effort.

---

## 🗺️ Roadmap

- **Upgrade-System** (Pickup, Magnet, Void, Filter, Compacting, Smelting, Stack,
  Inception u.v.m.) – Slots & Config sind bereits vorbereitet.
- **Platzierbare Backpacks** (BlockDisplay/Barrel-basiert, optional ProtocolLib).
- **Eigenes Resourcepack** + tiefere Nexo-/ItemsAdder-/Oraxen-Integration.
- **Backpacks-in-Backpacks** nur über das geplante **Inception Upgrade** (mit Limits & Dupe-Schutz).

---

## 🧪 Build-Hinweise

Voraussetzungen: **JDK 25** und **Maven**.

```bash
mvn clean package
```

Das fertige Plugin liegt anschließend unter:

```
target/yourShika-Backpacks-0.1.0.jar
```

Die Ziel-Paper-Version lässt sich über die Eigenschaft `paper.version` in der
[`pom.xml`](pom.xml) anpassen.

---

## 📄 Lizenz

Veröffentlicht unter der [MIT-Lizenz](LICENSE).

> Inspiriert von Sophisticated Backpacks – jedoch eine eigenständige Implementierung
> ohne übernommenen Code, ohne Assets und ohne Texturen.
