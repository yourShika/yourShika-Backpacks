# yourShika Backpack's

> Ein eigenständiges, **vollständig serverseitiges** Backpack-System für **Paper/Spigot**.
> Spieler brauchen **keinen Client-Mod**.

[![Version](https://img.shields.io/badge/version-0.0.1-6E5BC8)](https://github.com/yourShika/yourShika-Backpacks/releases)
[![Plattform](https://img.shields.io/badge/Plattform-Paper%201.21.x-5BE8D4)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![Lizenz](https://img.shields.io/badge/Lizenz-MIT-blue)](LICENSE)

---

## 📦 Beschreibung

**yourShika Backpack's** bringt hochwertige, einfärbbare Rucksäcke mit eigenen Inventaren
auf deinen Survival-Server – sicher gegen Dupe-Bugs, persistent gespeichert und
vorbereitet für eigene Custom-Texturen.

Das Plugin ist **von [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks)
([GitHub](https://github.com/P3pp3rF1y/SophisticatedBackpacks)) inspiriert**, aber eine
**komplett eigenständige Neuentwicklung**. Es wurde **kein Code, kein Asset und keine
Textur** aus der Mod übernommen – die Mod diente ausschließlich als Feature-Ideengeber.
**Dies ist kein Forge-/Fabric-/NeoForge-Mod, sondern ein Paper/Spigot-Plugin.**

---

## 🧱 Minecraft-/Paper-Version

- **Zielplattform:** Paper
- **Gebaut gegen:** `paper-api 1.21.4-R0.1-SNAPSHOT` (Java 21)
- **Hinweis zur gewünschten Version `26.1.2`:** Diese Versionsnummer existiert nicht als
  offizielle Minecraft-/Paper-Version. Es wird daher die nächstpassende stabile
  Paper-Version (1.21.x) verwendet. Die Version ist **leicht änderbar** über die
  Eigenschaft `paper.version` in der [`pom.xml`](pom.xml) – einfach anpassen und neu bauen.
- **Spigot:** läuft, soweit realistisch möglich. Es wird die Adventure-API von Paper
  genutzt; auf reinem Spigot können MiniMessage-Funktionen eingeschränkt sein.

---

## ✨ Features (v0.0.1)

- ✅ Backpacks als **normale Items** mit **eindeutiger, fälschungssicherer ID** (PDC)
- ✅ **Eigenes Inventar pro Backpack** – gleiche Items, unterschiedliche Inhalte
- ✅ **Persistente Speicherung** (SQLite oder YAML)
- ✅ **Sichere GUI** mit umfassendem Dupe-/Verlust-Schutz
- ✅ **6 Backpack-Tiers** (Leder, Kupfer, Eisen, Gold, Diamant, Netherite)
- ✅ **Crafting-Rezepte** (konfigurierbar, aktivier-/deaktivierbar)
- ✅ **Öffnen per Rechtsklick** (Haupt- & Nebenhand) **und Command**
- ✅ **Admin-Give-Command** und **Öffnen per ID**
- ✅ **Einfärbbare Backpacks** (Haupt- + Akzentfarbe)
- ✅ **CustomModelData pro Tier** – vorbereitet für eigene Texturen/Resourcepacks
- ✅ **Deutsche, konfigurierbare Nachrichten** (MiniMessage + `&`-Codes)
- ✅ **Vorbereitete Upgrade-Slots** (in der GUI sichtbar, gesperrt)
- ✅ **Optionale Hooks** (PlaceholderAPI aktiv; weitere vorbereitet)
- ✅ **Schutz gegen Backpacks-in-Backpacks**

---

## 🛠️ Installation

1. Plugin-JAR aus den [Releases](https://github.com/yourShika/yourShika-Backpacks/releases)
   herunterladen (`yourShika-Backpacks-0.0.1.jar`).
2. In den `plugins/`-Ordner deines Paper-Servers legen.
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
| `/bp color <Hauptfarbe> [Akzent]` | Backpack einfärben |
| `/bp list [Spieler]` | Backpacks auflisten |
| `/bp give <Spieler> <Tier> [Anzahl] [Farbe] [Akzent]` | Backpack geben (Admin) |
| `/bp openid <ID>` | Backpack per ID öffnen (Admin) |
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
| `yourshika.backpack.admin.reload` | Reload | OP |
| `yourshika.backpack.admin.debug` | Debug | OP |
| `yourshika.backpack.place` | Platzieren *(Roadmap)* | OP |
| `yourshika.backpack.upgrade` | Upgrades *(Roadmap)* | OP |

`yourshika.backpack.*` und `yourshika.backpack.admin.*` bündeln die jeweiligen Rechte.

---

## ⚙️ Konfiguration

Alle Werte liegen in `plugins/yourShika Backpack's/config.yml`:

- **Sprache**, **Debug**, **Speicherart** (SQLite/YAML), **Autosave-Intervall**
- **Tiers** mit Slots, Upgrade-Slots, Material, **CustomModelData**, Farben, Lore, Rezept
- **Crafting** global an/aus + pro Tier
- **Öffnen** per Rechtsklick / Nebenhand
- **Sicherheit** (Nesting), **Welt-Whitelist/Blacklist**
- **Platzierbare Backpacks** (vorbereitet, Roadmap)
- **Upgrades** (vorbereiteter Abschnitt, Roadmap)
- **Hooks**

Nachrichten sind in `messages_de.yml` frei anpassbar (MiniMessage + `&`-Codes, Platzhalter).

---

## 🎒 Backpacks, Tiers & Farben

### Tiers

| Tier | Lager-Slots | Upgrade-Slots* | CustomModelData |
|---|---|---|---|
| Leder | 9 | 1 | 1001 |
| Kupfer | 18 | 2 | 1002 |
| Eisen | 27 | 3 | 1003 |
| Gold | 36 | 4 | 1004 |
| Diamant | 45 | 5 | 1005 |
| Netherite | 45 | 6 | 1006 |

\* Upgrade-Slots sind in v0.0.1 **vorbereitet und gesperrt** (siehe Roadmap).
Diamant und Netherite teilen sich in 0.0.1 die Lagergröße (45); Netherite hat mehr
Upgrade-Slots. Erweiterte Lagerung (Paging) ist als Roadmap-Feature geplant.

### Identität & Sicherheit

Jedes Backpack trägt seine **ID, Tier und Farben im PersistentDataContainer** – nicht in
Name oder Lore. Dadurch sind Backpacks **nicht durch Umbenennen fälschbar**.
**Der Inhalt wird niemals im Item gespeichert**, sondern serverseitig an die ID gebunden.
Das ist der Kern des Dupe-Schutzes: Ein kopiertes Item teilt sich dasselbe Inventar.

### Farben

Haupt- und Akzentfarbe werden als Minecraft-`DyeColor` gespeichert, im Item-Lore
angezeigt und sind über `/bp color` änderbar. Spätere visuelle Darstellung über
Resourcepack / CustomModelData / RGB ist vorbereitet.

---

## 🎨 CustomModelData, Custom-Texturen & Resourcepack

- Jeder Tier besitzt eine eigene **CustomModelData** (1001–1006).
- **Fertige Custom-Texturen sind für spätere Versionen geplant.**
- v0.0.1 **bereitet CustomModelData bereits vor** – ein eigenes **Resourcepack kann
  später ergänzt werden**, ohne das Backpack-System umzubauen.
- **Ohne Resourcepack** funktionieren Backpacks als normale Items mit Name und Lore.
- **Mit Resourcepack** sehen sie später wie eigene Items aus.
- **ItemsAdder** und **Oraxen** können später optional genutzt werden – beide sind
  **keine Pflicht**; fehlen sie, läuft das Plugin normal weiter.
- Spätere Farbvarianten können zusätzliche CustomModelData-Werte verwenden
  (z. B. Leder Rot/Blau/Grün, Diamant Schwarz/Gold, Netherite Lila/Schwarz).

---

## 🧰 Crafting

Standardmäßig craftet man jeden Tier aus 8× Tier-Material um eine **Truhe**:

```
III      I = Tier-Material (Leder/Kupfer/Eisen/Gold/Diamant/Netherite)
IHI      H = Truhe (CHEST)
III
```

Gecraftete Backpacks sind **Templates ohne ID**; die eindeutige ID wird **beim ersten
Öffnen** automatisch vergeben. Rezepte sind je Tier in der `config.yml` frei anpassbar.

---

## 💾 Storage

- **SQLite** (Standard, empfohlen) → `backpacks.db`
- **YAML** (Alternative) → `backpacks.yml`

Gespeichert wird beim **Schließen**, bei **Logout**, **Plugin-Deaktivierung**,
**Server-Stop** und per **Autosave**. Inhalte werden versioniert/Base64-serialisiert.

---

## 🔌 Externe Hooks (optional)

| Hook | Status | Zweck |
|---|---|---|
| **PlaceholderAPI** | ✅ aktiv | `%ysbp_count%`, `%ysbp_highest_tier%`, `%ysbp_open%` |
| **Vault** | 🔜 Roadmap | Economy (Kauf/Upgrades/Farben) |
| **ProtocolLib** | 🔜 Roadmap | Fake-Blocks für platzierte Backpacks |
| **ItemsAdder** | 🔜 Roadmap | Custom-Modelle/Texturen |
| **Oraxen** | 🔜 Roadmap | Custom-Modelle/Texturen |

Alle Hooks sind **optional** – fehlt eine Library, **bricht nichts**.

---

## ⚠️ Bekannte Einschränkungen (v0.0.1)

- **Upgrades sind noch nicht funktionsfähig** (Slots sind vorbereitet/gesperrt).
- **Platzierbare Backpacks** sind noch nicht enthalten (Roadmap, vorbereitet).
- **Custom-Texturen/Resourcepack** liegen noch nicht bei (nur vorbereitet).
- Diamant/Netherite teilen sich die Lagergröße (Paging ist Roadmap).
- Reines Spigot (ohne Paper) kann MiniMessage-Funktionen einschränken.

---

## 🗺️ Roadmap

### Platzierbare Backpacks
Geplant über serverseitige Alternativen (Barrel-/Chest-basiert, `BlockDisplay`/
`ItemDisplay`/`Interaction`-Entities oder ProtocolLib-Fake-Blocks), inkl. Schutz vor
Explosionen, optionalem Hopper-Verhalten und persistenter Speicherung.

### Upgrade-System
Die GUI besitzt bereits vorbereitete Upgrade-Slots, die Config einen `upgrades`-Bereich.
Geplante Upgrades (Auswahl):

> Pickup · Magnet · Void · Refill · Filter · Advanced Filter · Compacting ·
> Advanced Compacting · Feeding · Advanced Feeding · Smelting · Auto-Smelting ·
> Smoking · Auto-Smoking · Blasting · Auto-Blasting · Crafting · Stonecutter ·
> Stack · Stack Downgrade · Jukebox · Restock · Deposit · Inception · Everlasting ·
> Toolswapper · Tank · Pump · Battery · XP Pump · Anvil · Smithing · Alchemy · Infinity

Jedes Upgrade soll später eigene CustomModelData/Texturen, Permissions, Rezepte und
Konfiguration erhalten. **Backpacks-in-Backpacks** bleiben standardmäßig deaktiviert und
kommen erst mit dem **Inception Upgrade** (mit Limits & Dupe-Schutz).

### Weiteres
Custom-Texturen + eigenes Resourcepack · ItemsAdder-/Oraxen-Integration ·
Vault-Economy · Hopper-Automation für platzierte Backpacks.

---

## 📝 Release Notes – v0.0.1

Erste Version. Fokus auf das **stabile Grund-Backpack-System**:
Items mit eindeutigen IDs, eigene persistente Inventare, sichere GUI, 6 Tiers,
Crafting, Farben, Commands, Permissions, deutsche Nachrichten, CustomModelData-
Vorbereitung sowie vorbereitete Upgrade-Slots und -Konfiguration.

---

## 🧪 Build-Hinweise

Voraussetzungen: **JDK 21** und **Maven** (oder das mitgelieferte Build via GitHub Actions).

```bash
mvn clean package
```

Das fertige Plugin liegt anschließend unter:

```
target/yourShika-Backpacks-0.0.1.jar
```

Die Ziel-Minecraft-/Paper-Version lässt sich über die Eigenschaft `paper.version`
in der [`pom.xml`](pom.xml) anpassen.

---

## 📄 Lizenz

Veröffentlicht unter der [MIT-Lizenz](LICENSE).

> Inspiriert von Sophisticated Backpacks – jedoch eine eigenständige Implementierung
> ohne übernommenen Code, ohne Assets und ohne Texturen.
