# yourShika Backpack's

> A standalone, **fully server-side** backpack system for **Paper/Spigot**.
> Players need **no client mod**.

[![Version](https://img.shields.io/badge/version-1.1.0-6E5BC8)](https://github.com/yourShika/yourShika-Backpacks/releases)
[![Platform](https://img.shields.io/badge/Platform-Paper%2026.1.2-5BE8D4)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 📦 Description

**yourShika Backpack's** brings high-quality, **dyeable** backpacks with their own
inventories to your survival server – safe against dupe bugs, persistently stored
and prepared for custom textures.

The plugin is **inspired by [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks)
([GitHub](https://github.com/P3pp3rF1y/SophisticatedBackpacks))**, but is a
**completely standalone re-implementation**. **No code, no asset and no texture**
was taken from the mod. **This is not a Forge/Fabric/NeoForge mod, but a
Paper/Spigot plugin.**

---

## ✨ v1.1.0

- 🎒 **Soulbound Upgrade (new)** – a new functional upgrade with its own Oraxen
  texture. A backpack with this upgrade **stays with you when you die** instead of
  dropping, and you keep it after respawn. It is **dupe-safe next to grave/deadbody
  plugins**: the death handler runs at `LOWEST` priority and removes soulbound
  backpacks from the death drops *before* grave plugins read them, and the backpack
  content lives server-side by ID (so even a stray copy shares the same storage).
  Craft it with a **Totem of Undying** + **Soul Soil** around Upgrade Leather.
- 🎨 **Carried backpack now uses your colors** – the carried/held Oraxen backpack
  baked the tier color into its **overlay layer** (e.g. an Emerald backpack stayed
  green and dyeing the accent didn't fully show). The overlay is now **neutral**
  (tier-independent), so the body takes your **main color** (dyeable base) and the
  accent variant colors the straps/pockets – the same fix v1.0.9 shipped for placed
  backpacks. *(Resource-pack change – run `/oraxen reload` after updating; existing
  backpacks refresh automatically on open/join.)*

---

## 🐞 v1.0.9

- 🎨 **Placed backpack now uses the main color** – the body of a placed Oraxen
  backpack had the tier's default main color baked in (e.g. an Emerald backpack
  stayed green) regardless of the chosen color. The body texture is now dyeable
  (`tintindex 0` + neutral base), so the placed backpack body takes your main
  color while the accent variant still colors the frame/side parts. *(Resource-
  pack change – run `/oraxen reload` after updating.)*

---

## 🐞 v1.0.8

- 🎨 **Accent color now updates when dyeing** – dyeing a backpack (or `/bp color`)
  only re-tinted the main color and lore but kept the old Oraxen accent model, so
  the accent stayed at its previous color. The item is now fully refreshed so the
  accent texture switches to the new color too.
- 🔁 **Existing backpacks get updated** – opening a backpack now also refreshes all
  other backpacks carried by the player, so previously dyed backpacks pull the
  corrected accent model (in addition to the existing refresh on join).

---

## 🐞 v1.0.7

- 📣 **Achievement chat broadcast fixed** – unlocks are now announced in chat
  reliably. Programmatically awarding an advancement only fired the toast (not the
  chat line) on this version, so the broadcast is now sent by the plugin itself
  (gated by `achievements.broadcast`, default on).

---

## 🐞 v1.0.6

- 🖼️ **Tab background fixed for MC 26.x** – newer versions resolve the background
  via the texture-id convention (the client adds `textures/` + `.png`). Even the
  classic full path renders as a missing texture there. The default is now the
  guaranteed `minecraft:block/dirt`. Changeable via `achievements.background`
  (`block/stone`, `block/netherrack`, … or `none` for no background).

---

## 🐞 v1.0.5

- 🖼️ **Tab background configurable + new texture format** – it can be changed in
  the config without a new release if it shows up as missing texture.
- 📣 **Chat broadcast on by default** – unlocked achievements are now announced in
  chat by default (`achievements.broadcast: true`, respects the
  `announceAdvancements` gamerule).
- 🌳 **Nicer menu** – achievements are now chained into themed branches (tiers,
  customization, placing, upgrades, stations) instead of one long column; tier
  achievements use the "goal" frame, hard ones the "challenge" frame.

---

## 🐞 v1.0.4

- 🖼️ Switched the tab background to a dedicated advancement background texture.
- 👀 **Tab & all achievements visible** – the (invisible) root is granted to every
  player on join so the achievement tab appears and **all** achievements (greyed
  out until earned) are shown.

---

## 🐞 v1.0.3

- 🛠️ **Advancement datapack fix** – the `background` field of the achievement tab
  must be a string; otherwise the advancements failed to load (`Not a string:
  {...}`). The tab and all achievements load cleanly now.

---

## ✨ v1.0.2

- 🏆 **Achievements are now real Minecraft advancements** – instead of a custom
  command/menu they appear as their **own tab** ("yourShika Backpack's") in the
  normal advancements screen (toast + progress handled by vanilla). The plugin
  generates a small datapack automatically and loads it on first start. Still
  configurable via `achievements.enabled` / `broadcast` / `disabled`.
- 🧹 `/bp achievements` was removed (replaced by the native integration).

---

## ✨ v1.0.1

- 🏅 **Achievements** – 28 achievements (title, description, icon). Unlocking gives
  a message + sound, optional server-wide announcement. Fully **configurable**.
- 🔊 **Pickup sound** – the Pickup upgrade plays a pickup sound when vacuuming items.
- 🧲 **Magnet** now pulls items **over blocks** instead of dragging them into the
  ground (a slight lift, nothing disappears anymore).
- 🛠️ **Compacting recipe reworked** – it collided with the Iron upgrade before; now
  made from **pistons + iron blocks** (distinct and a bit harder).
- 💀 **Everlasting more expensive** – now needs a **Nether Star** (Wither) + netherite + obsidian.
- 🔒 **Anvil protection** – upgrade items (Upgrade Leather, tier & function upgrades)
  can **no longer** be renamed in an anvil.

---

## 🎉 v1.0.0 – First stable release

The first official milestone. Contains all features of the 0.x line – tiers &
upgrade chain, dyeable backpacks, placeable backpacks with holograms, functional
upgrades (Pickup, Magnet, Crafting/Smithing/Stonecutter, portable
Furnace/Smoker/Blast Furnace with background operation, Compacting with filter,
Ender-Link, Trash, Recall, Everlasting, **XP Storage**), dupe-safe storage, Oraxen
assets incl. 3D models for placed backpacks, plus multi-language support (EN/DE/PL).

- 🧪 The **XP Storage upgrade** now has its own, style-matching item asset
  (experience bottle) for Oraxen.
- 🏷️ **Tier upgrade keeps the name** – a custom backpack name (incl. colors) is kept
  when upgrading at the smithing table.
- ✅ Final review: assets, code and recipes checked; CI model check green.

---

## ✨ v0.9.1

- 🪪 **Hologram name rendered correctly** – placed backpacks with gradient/hex/
  color-code names now show the name properly (instead of e.g. "<#aabbcc>…").
- 🧪 **New XP Storage upgrade** – store your **experience** in the backpack and
  withdraw it any time. Own button in the control bar with a GUI
  (deposit/withdraw, 1 level or all) and a live display of the stored XP.

---

## ✨ v0.9.0

- 🏷️ **Rename in the anvil** – backpacks can be renamed directly in an **anvil**,
  with full color support: **hex** (`&#RRGGBB` / `<#RRGGBB>`),
  **gradient/rainbow** (`<gradient:#a:#b>…`) and **Minecraft color codes** (`&a`, `&l` …).
- 🧷 **Name persists when placed** – a placed and then picked-up backpack no longer
  loses its custom name (the name is stored server-side).
- 🔥 **Portable furnace runs in the background** – smelting stations keep working
  even when the menu is closed. The **furnace icon in the backpack** shows a
  **live-updating lore**: what is being smelted, fuel type/amount and how many
  items will be smelted in total.
- 🧱 **Compacting chat message** – on close the backpack reports what was compacted
  (e.g. "3x Diamond Block").
- 👤 **`/bp recall` menu** now shows the **owner** of each placed backpack.
- 🗑️ **Trash protects backpacks** – backpacks (also inside shulkers/bundles) can no
  longer be put into the trash.
- 🎛️ **Station limit** – at most **5 station upgrades** fit per backpack so all icons
  in the control bar stay visible and don't fight for space.

---

## ✨ v0.8.0

**Commands & management**
- 🏷️ **`/bp rename <name|reset>`** – rename backpacks (config `rename.allow-players`, admins always)
- 🖱️ **Clickable `/bp list`** – `[Open]` `[Copy]` `[TP]`
- 📍 **`/bp locate [player|ID]`** + **`/bp goto <ID>`** – locate placed backpacks & teleport to them
- 🔁 **`/bp transfer <ID> <player>`** – change owner
- 🎯 **`/bp recall`** opens a **selection menu** when you have several backpacks (otherwise direct)
- 🧰 **`/bp assets <status|redeploy>`** & **`/bp doctor`** (diagnostics)
- 📝 **Admin audit log** (`audit.log`): give/openid/transfer/recall/pickup
- 🔢 **More PlaceholderAPI**: `placed`, `used_slots`, `free_slots`, `tiers`, `count_<tier>`

**GUI**
- 📊 Info item shows **used/free slots**
- ⏮️ **Shift-click** the paging arrows jumps to the **first/last page**

**Placed backpacks**
- 🧭 Rotate to **face the player**; optional **hologram (name + owner)**; **particles + sound** when placing/picking up

**Upgrades**
- 🗑️ **Trash with delete confirmation** (`trash.confirm`) – closing without a click returns the items
- 🧱 **Compacting preview** (chat) + **presets** (ores/farm/redstone/misc)
- 🧲 **Magnet**: throttle against lag + respect for item owner & pickup delay

**Robustness & CI**
- 🛡️ **Deep-nesting protection**: backpacks in shulkers/bundles are detected & blocked
- ♻️ **Auto-recovery**: save on page change + crash detection
- ✅ **Recipe validation** with clear config error messages
- 🤖 **CI asset check**: prevents one-sided faces in Oraxen models

---

## ✨ v0.7.2 (Placed Backpack Geometry)

- 🎒 **Placed Oraxen backpacks now use real cuboid geometry** for flap, pocket,
  side pouches, straps and buckle instead of one-sided faces, so they no longer
  disappear from certain viewing angles.
- 📦 **Oraxen asset bundle bumped to version 6** so managed model files update
  automatically while custom server assets stay protected by the deployer.

---

## ✨ v0.7.1 (Portable Stations, Filter, configurable recipes)

- 🔁 **Items auto-update on join/update:** existing backpacks and upgrade items are
  brought to the latest state when a player joins (and on reload), so nothing
  breaks or looks stale after a plugin update.
- 🔥 **Portable Furnace / Blast Furnace / Smoker:** the Smelting/Blasting/Smoking
  upgrade now adds a **GUI station** (icon in the backpack) that smelts like a real
  furnace. **State is saved in the backpack** – logout, crash or server stop never
  lose items, and progress persists. **Only one furnace-type upgrade per backpack.**
- 🧱 **Compacting Filter:** the Compacting upgrade gets a **filter GUI** (icon in the
  backpack) where you pick which items get compacted on close (empty = everything).
- 🚫 **Upgrade slots accept only function upgrades** – Upgrade Leather and tier
  upgrades (Iron-, Netherite-Upgrade …) can no longer be placed there.
- 🎒 **Upgrades button moved to the bottom-left corner** of the backpack menu.
- 🛠️ **Configurable recipes:** function-upgrade recipes (incl. the *advanced ➜ needs
  its base upgrade* rule), the tier-upgrade crafting shape and the **Smithing**
  recipes (template material, Netherite base/addition) are all editable in
  `config.yml` (config version 7, auto-migrates).
- 📖 **`/bp info`:** recipe arrows are now just `→` / `↓`; advanced upgrades show the
  required **base upgrade** as the `X` ingredient.

---

## 🐞 v0.6.1

- **Horse-armor block hardened:** a backpack can no longer be put on **any**
  mount – horses, **zombie/skeleton horses**, donkeys, mules, llamas, camels –
  via interaction, inventory, or **dispenser**.
- **Smelting status** is shown in the backpack's Info item: how much is left to
  smelt, how many items the fuel can smelt, and an estimated time.
- Backpack Info item is now English.

---

## ✨ v0.6.0 (more upgrades, languages, /bp give all)

- 🌍 **Languages:** bundled **English, German, Polish** (`messages_en/de/pl.yml`).
  Message files **auto-update** on plugin updates (new keys are merged in,
  your edits are kept).
- 🎒 **`/bp give` now gives everything** – backpacks, Upgrade Leather, tier
  upgrades (`upgrade_copper` …) and every function upgrade (`pickup`, `magnet` …).
- 🧩 **New functional upgrades:** Ender-Link, Compacting, Smelting/Blasting/Smoking,
  Recall, Trash.
- 🔗 **Advanced upgrades require their base** to craft (e.g. Advanced Pickup needs
  a Pickup Upgrade).
- 🧹 **PacketEvents removed** – placement uses vanilla display entities and never
  needed it.
- 🖼️ **Oraxen textures** now deploy for **all items** (decoupled from backups).

---

## ✨ v0.5.0 (English + Functional Upgrades)

- 🌐 **Plugin is now English by default** (`language: en`, `messages_en.yml`).
- 🧩 **Functional upgrades** you craft and install into a backpack: Pickup/Advanced
  Pickup, Magnet/Advanced Magnet, Crafting/Stonecutter/Smithing, Everlasting. Each
  has a fair recipe (`'U'` = Upgrade Leather) and appears in `/bp info`.
- 📖 **`/bp info`** fully English; smithing view reworked.
- 🗂️ **Config version 5** (auto-migrates; old file backed up).

---

## 🐞 v0.4.0 (Stability & Modules)

- **Vanilla-like Oraxen defaults:** backpack textures use the bundled reference as
  their style base. All pack PNGs are 64x64, upscaled from real 16x16 pixel art.
- **No more freeze/crash when toggling modules.** Recipes are now registered
  **idempotently** and not touched when toggling.
- **Oraxen off = back to normal horse armor.** Disabling the Oraxen module removes
  the custom model cleanly; enabling auto-updates online players' backpacks.
- **Module/info GUIs:** items can no longer be taken/moved.
- **`/bp info`:** smithing recipes shown more clearly with English slot labels.

---

## ✨ v0.3.5

- 🎨 **Real second accent color** via Oraxen variants (e.g.
  `ysbp_leather_backpack_accent_red`). The **main color** stays real
  `LEATHER_HORSE_ARMOR` tinting.
- 🧩 **Oraxen YAMLs** with **178 unique** provider/CustomModelData values.
- 📦 **Versioned asset deployer:** updates old defaults and creates backups under
  `AssetBackups/`.
- 🛟 **Oraxen item YAMLs** are **backed up before overwriting**.
- ⚒️ **Smithing fix:** tier & color are written on `SmithItemEvent`, not in the preview.

---

## ✨ v0.3.0

- 🖼️ **Oraxen textures integrated:** when the Oraxen module is active, the plugin
  ships bundled textures & item definitions automatically. Just `/oraxen reload`.
- 📖 **`/bp info` – recipe browser:** a GUI with all backpacks and upgrade items;
  clicking an item shows its crafting/smithing recipe.
- 🔄 **Auto-update of existing backpacks:** name, lore and model are brought up to
  date on each open – **without** changing ID, color, owner or contents.
- 🔒 **Upgrade items abuse-proof:** Upgrade Leather and tier upgrades can **only**
  be used in the backpack recipes.
- 🎨 Removed the duplicate color display; polished names & lore.
- 🐞 Dyeing is reliably retrievable again.

---

## 🔧 v0.2.2

- **Color bug fixed:** dyed backpacks keep their color even after **placing &
  picking up** (previously reverted to default).
- **Upgrade colors:** default colors are raised to the new tier on upgrade,
  individually dyed backpacks keep their color.
- **Hooks auto-on:** external hooks activate themselves once the respective plugin
  is installed.
- **Owner:** every backpack remembers **who crafted it**. Optional
  `security.owner-only: true` → only owner (or admins) may open/pick up. Default: off.
- **Upgrade items prepared for custom textures:** CustomModelData (2000–2006) and
  optional `item_model` per upgrade, configurable.
- **Netherite upgrade** is created in the **smithing table**.

---

## 🔧 v0.2.1 (Bugfixes)

- **Config auto-update:** on structure changes `config.yml` updates itself (old
  file backed up as `config-backup-*.yml`).
- **Only leather is directly craftable** – all higher tiers come from the upgrade
  chain (smithing).
- **ID appears immediately** in the lore on crafting.
- **Netherite upgrade** is now created in the **smithing table**.
- **Upgrade Leather** is **rejected** in the template slot of the upgrade.
- **Dyeing:** the result can now reliably be taken out (lands in the inventory).

---

## 🆕 v0.2.0

- 🔒 **Update-safe storage (MUST):** contents are stored in the versioned Paper byte
  format (with Minecraft data version + DataFixer). Items – **including arbitrary
  custom NBT/components** – survive server and plugin updates intact.
- 🧵 **Upgrade chain:** **Leather → Copper → Iron → Gold → Diamond → Emerald →
  Netherite**. The smithing-table upgrade **keeps ID, contents and color**.
- 🧰 **Upgrade GUI:** a dedicated inventory per backpack (button in the backpack GUI).
- 🎨 **Dyeing in the crafting table:** backpack in the center, dyes in the **left
  column = main color**, **right column = accent color** (mixed to **hex**).
- 📦 **Placeable backpacks:** **shift-right-click** the ground to place
  (ItemDisplay + Interaction), **right-click opens**, **sneak-right-click picks up**.
- 🐴 **Protection:** backpacks cannot be used as horse armor and cannot be moved by
  hoppers/automation.
- 🔁 **`/bp update`:** downloads the latest release JAR from GitHub.
- 🧪 **Module GUI:** `/bp modules` toggles hooks live by clicking.
- 📖 **Recipe Book + JEI/REI/EMI:** all real recipes are unlocked and visible.
- ➖ **Custom-item hooks:** Oraxen only (Nexo & ItemsAdder removed).

---

## 🧱 Minecraft/Paper version

- **Target platform:** Paper (also runs on Spigot, with possibly limited MiniMessage visuals)
- **Built against:** `io.papermc.paper:paper-api:26.1.2.build.72-stable`
- **Java:** **25** required (Minecraft 26.1.x requires Java 25, Microsoft OpenJDK 25)
- **Version scheme:** since the new `YY.D.H` scheme, the Paper API uses
  `<version>.build.<n>-<stage>` instead of `-R0.1-SNAPSHOT`. The target version is
  easily changed via the `paper.version` property in [`pom.xml`](pom.xml).

---

## ✨ Features

- ✅ Backpacks as **normal items** with a **unique, forgery-proof ID** (PDC)
- ✅ **Own inventory per backpack** – same items, different contents
- ✅ **Persistent storage** (SQLite or YAML)
- ✅ **Safe GUI** with comprehensive dupe/loss protection
- ✅ **Double-chest layout (54 slots)** – only enabled fields are usable
- ✅ **Bigger tiers = more space**, with automatic **paging** beyond one page
- ✅ **6 backpack tiers** (Leather, Copper, Iron, Gold, Diamond, Netherite) + Emerald
- ✅ **Truly dyeable** via dyeable leather items (DyeColor **or hex `#RRGGBB`**) –
  color visible **even without a resource pack**
- ✅ **Crafting recipes** (configurable, can be enabled/disabled)
- ✅ **Open by right-click** (main & off hand) **and command**
- ✅ **Admin give command** and **open by ID**
- ✅ **CustomModelData + `item_model` component per tier** – prepared for resource packs
- ✅ **Modular hook system** – live status via **`/bp modules`**
- ✅ **Configurable messages** (MiniMessage + `&` codes), bundled EN/DE/PL
- ✅ **Protection against backpacks-in-backpacks**
- ❌ **No Vault** (deliberately removed)

---

## 🛠️ Installation

1. Download the plugin JAR from the [Releases](https://github.com/yourShika/yourShika-Backpacks/releases)
   (`yourShika-Backpacks-1.1.0.jar`).
2. Put it into the `plugins/` folder of your **Paper 26.1.2 (Java 25)** server.
3. Start the server – the data folder **`plugins/yourShika Backpack's/`** is created
   automatically with `config.yml`, the message files and the database.
4. Optionally adjust `config.yml` and run `/bp reload`.

---

## ⌨️ Commands

Main command: `/backpack` · aliases: `/bp`, `/ybackpack`, `/ysbackpack`

| Command | Description |
|---|---|
| `/bp help` | Show help |
| `/bp open` | Open the backpack in your hand |
| `/bp info` | View backpacks, upgrades & **recipes** in a GUI |
| `/bp list [player]` | List backpacks (clickable: Open/Copy/TP) |
| `/bp rename <name\|reset>` | Rename the backpack in your hand |
| `/bp recall` | Recall placed backpacks (needs Recall upgrade; selection menu for several) |
| `/bp locate [player\|ID]` | Show coordinates of placed backpacks |
| `/bp goto <ID>` | Teleport to a placed backpack |
| `/bp transfer <ID> <player>` | Change a backpack's owner (admin) |
| `/bp color <color> [accent]` | Dye a backpack (DyeColor name **or** `#RRGGBB`) — **admin** |
| `/bp give <player> <tier> [amount] [color] [accent]` | Give a backpack (admin) |
| `/bp openid <ID>` | Open a backpack by ID (admin) |
| `/bp modules` | Toggle external modules **via GUI** (admin) |
| `/bp assets <status\|redeploy>` | Check / redeploy Oraxen assets (admin) |
| `/bp doctor` | Diagnostics (Oraxen, config, DB, assets, versions) (admin) |
| `/bp update` | Download the latest version from GitHub (admin, restart needed) |
| `/bp reload` | Reload the configuration (admin) |
| `/bp version` | Plugin info |

> **Dyeing for players** works via the **crafting table** (see below); the
> `/bp color` command is deliberately an **admin** function.

> **Achievements** are real Minecraft advancements and appear as their own tab in
> the advancements screen — there is no command for them.

---

## 🔐 Permissions

| Permission | Description | Default |
|---|---|---|
| `yourshika.backpack.use` | Use backpacks | all |
| `yourshika.backpack.open` | Open your own backpack | all |
| `yourshika.backpack.list` | List your own backpacks | all |
| `yourshika.backpack.place` | Place/pick up backpacks | all |
| `yourshika.backpack.rename` | Rename backpacks | all |
| `yourshika.backpack.craft.<tier>` | Craft a specific tier | all |
| `yourshika.backpack.admin.color` | Dye a backpack via command | OP |
| `yourshika.backpack.admin.give` | Give backpacks | OP |
| `yourshika.backpack.admin.openid` | Open by ID | OP |
| `yourshika.backpack.admin.openother` | Open others' backpacks | OP |
| `yourshika.backpack.admin.listother` | List others' backpacks | OP |
| `yourshika.backpack.admin.modules` | Open/toggle module GUI | OP |
| `yourshika.backpack.admin.update` | Self-updater | OP |
| `yourshika.backpack.admin.reload` | Reload | OP |
| `yourshika.backpack.admin.debug` | Debug | OP |

---

## 🎒 Backpacks, tiers, size & paging

Each backpack opens as a **double chest (54 slots)**. The bottom row (9 slots) is
the **control bar** (paging, info, upgrades). Of the remaining **45 slots** only as
many are **enabled** as the tier allows – the rest is locked. Tiers with more than
45 storage slots are paged automatically (◀ / ▶).

| Tier | Storage slots | Pages* | Upgrade slots | CustomModelData |
|---|---|---|---|---|
| Leather | 9 | 1 | 1 | 1001 |
| Copper | 18 | 1 | 2 | 1002 |
| Iron | 27 | 1 | 3 | 1003 |
| Gold | 45 | 1 | 4 | 1004 |
| Diamond | 54 | 2 | 5 | 1005 |
| **Emerald** | 81 | 2 | 6 | 1006 |
| Netherite | 108 | 3 | 6 | 1007 |

\* At 45 usable slots per page (`gui.storage-slots-per-page`).
The upgrade slots are usable as their own inventory via the **Upgrades button** in
the backpack GUI.

### Identity & security

Every backpack carries its **ID, tier and colors in the PersistentDataContainer** –
not in name or lore. This makes backpacks **not forgeable by renaming**.
**Contents are never stored in the item**, but server-side bound to the ID. That is
the core of the dupe protection: a copied item shares the same inventory. When
paging, only the visible page is synced between inventory and buffer – the
operation stays atomic and dupe-safe.

### Colors (truly dyeable)

Backpacks are based on a **dyeable leather item** (`LEATHER_HORSE_ARMOR`) by
default. The **main color tints the item for real** – visible **even without a
resource pack** – and is changeable via `/bp color`. Both **DyeColor names**
(`BROWN`, `CYAN`, …) and **hex values** (`#A0703C`) are allowed.

---

## 🎨 CustomModelData, item_model & resource packs

- Each tier has its own **CustomModelData** (1001–1007) and can optionally set a
  modern **`item_model` component** (`item-model: "namespace:path"`).
- **Without a resource pack** backpacks work as normal (dyed) leather items.
- **With a resource pack** they look like custom items.
- **External item systems** (Oraxen) are **optional**. The default is always the
  built-in vanilla provider.

### Oraxen assets

Under `src/main/resources/oraxen/` there is a ready-made Oraxen bundle with 64x64
PNGs in vanilla style for all backpack tiers, tier-upgrade items and many prepared
function upgrades. Real backpacks are still created via plugin crafting, smithing or
`/bp give` so that ID, contents, owner and color are preserved.

---

## 🧰 Crafting, upgrade chain & dyeing

**Only the leather backpack** is crafted directly (8× leather around a **chest**).
All higher tiers come from the **upgrade chain** in the smithing table. Freshly
crafted backpacks show their **ID immediately** in the lore.

**Upgrade chain** — Leather → Copper → Iron → Gold → Diamond → Emerald → Netherite:

1. Craft **Upgrade Leather**: 1× leather (center) + 4× string (crafting table).
2. Craft a **tier upgrade**: Upgrade Leather + 8× tier material (e.g. copper) →
   *Copper Upgrade* (crafting table). **Netherite upgrade**: smithing table –
   Upgrade Leather (template) + Netherite Ingot (base) + string (addition).
3. **Upgrade in the smithing table:** **plain leather** (template) + **previous
   backpack** (base) + **tier upgrade** (addition) → next backpack.
   **ID, contents and color are kept.**

**Dyeing (crafting table):** backpack in the **center column**, dyes in the **left
column** = main color, **right column** = accent color. Position within a column
does not matter; several dyes are mixed into a **hex value**. The backpack contents
are fully preserved.

> The real recipes appear in the **Recipe Book** and in **JEI/REI/EMI**. The
> position-independent dyeing is not a fixed recipe and is therefore not listed there.

---

## 💾 Storage

- **SQLite** (default, recommended) → `backpacks.db`
- **YAML** (alternative) → `backpacks.yml`

Saved on **close**, **page change**, **logout**, **plugin disable**, **server stop**
and via **autosave**.

**Update-safe (MUST):** contents are stored in the **versioned Paper byte format**
(`serializeAsBytes`) – incl. Minecraft data version. On load the DataFixer migrates
automatically to the current version, so items with arbitrary custom NBT/components
are **not lost or corrupted** after updates.

---

## 🔌 External modules / hooks (automatic)

The plugin runs **fully standalone**. External hooks are encapsulated as **modules**
and activate **automatically** as soon as the respective plugin is installed and the
module is enabled in the config (**default: enabled**). If the plugin is missing,
the module stays silently inactive.

Live status & toggling **via GUI**: **`/bp modules`**.

| Module | Purpose | Default |
|---|---|---|
| **PlaceholderAPI** | `%ysbp_count%`, `%ysbp_highest_tier%`, `%ysbp_open%` … | auto |
| **Oraxen** | Custom models/textures | auto |

> **Custom items:** only **Oraxen** is supported as a custom-item hook (free to use,
> mature API). **Nexo** and **ItemsAdder** were deliberately removed.
> **Vault is deliberately not supported.**

Disable a single module (example):

```yaml
hooks:
  modules:
    oraxen: false
```

---

## 🗺️ Roadmap

- More functional upgrade effects (Void, Filter, Stack, Inception, …) – the
  upgrade slots & items already exist.
- **Own resource pack** + deeper Oraxen integration.
- **Backpacks-in-backpacks** only via the planned **Inception upgrade** (with limits
  & dupe protection).

---

## 🧪 Build notes

Requirements: **JDK 25** and **Maven**.

```bash
mvn clean package
```

The finished plugin is then located at:

```
target/yourShika-Backpacks-1.1.0.jar
```

The target Paper version can be adjusted via the `paper.version` property in
[`pom.xml`](pom.xml).

---

## 📄 License

Released under the [MIT License](LICENSE).

> Inspired by Sophisticated Backpacks – but a standalone implementation without any
> taken code, assets or textures.
