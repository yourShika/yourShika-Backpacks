package de.yourshika.backpacks.place;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import de.yourshika.backpacks.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Platzierbare Backpacks ohne echten Block: per Shift-Rechtsklick wird ein
 * {@link ItemDisplay} (Optik) plus eine {@link Interaction}-Entity (Klick-Hitbox)
 * gesetzt. Beide sind persistent und mit der Backpack-ID getaggt, sodass sie
 * Server-Neustarts überstehen und weiterhin geöffnet werden können.
 */
public final class PlaceableManager {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final BackpackItemFactory items;
    private final TierRegistry tiers;

    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final net.kyori.adventure.text.minimessage.MiniMessage mini =
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    public PlaceableManager(YourShikaBackpacks plugin, BackpackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.items = manager.items();
        this.tiers = plugin.tiers();
        this.markerKey = new NamespacedKey(plugin, "placed_marker");
        this.idKey = new NamespacedKey(plugin, "placed_id");
    }

    public boolean isPlacedEntity(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public UUID backpackIdOf(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Platziert ein Backpack. Gibt einen Fehler-Key zurück oder null bei Erfolg. */
    public String place(Player player, Block clicked, BlockFace face, EquipmentSlot hand) {
        ItemStack inHand = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!items.isBackpack(inHand)) return "error.hold-backpack";

        BackpackTier tier = tiers.get(items.getTierKey(inHand));
        if (tier == null) return "error.invalid-backpack";

        Block target = clicked.getRelative(face);
        if (!target.getType().isAir() && !target.isReplaceable()) {
            return "error.no-place";
        }
        Location loc = target.getLocation().add(0.5, 0.0, 0.5);

        // Bereits belegt?
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.45, 0.6, 0.45)) {
            if (isPlacedEntity(e)) return "error.place-occupied";
        }

        UUID id = items.getId(inHand);
        if (id == null) {
            id = UUID.randomUUID();
            items.writeId(inHand, id);
        }
        String main = items.getMainColor(inHand, tier.defaultMainColor());
        String accent = items.getAccentColor(inHand, tier.defaultAccentColor());

        BackpackData data = manager.storage().load(id);
        if (data == null) {
            data = new BackpackData(id);
            data.owner(player.getUniqueId());
            data.tier(tier.key());
            data.contents(new ItemStack[tier.storageSlots()]);
        }
        // Farben IMMER aus dem Item übernehmen, damit ein zuvor (z.B. per Crafting)
        // umgefärbtes Backpack beim Aufheben nicht auf die Default-Farbe zurückfällt.
        data.mainColor(main);
        data.accentColor(accent);
        data.placed(true);
        data.world(loc.getWorld().getName());
        data.position(loc.getX(), loc.getY(), loc.getZ());
        manager.storage().save(data);

        ItemStack visual = items.create(tier, id, main, accent);
        applyPlacedVisualModel(visual, tier, accent);
        float yaw = player.getLocation().getYaw();
        spawnEntities(loc, id, visual, yaw);
        String hologramName = items.getCustomName(inHand);
        if (hologramName == null || hologramName.isBlank()) {
            hologramName = capitalize(tier.key()) + " Backpack";
        }
        spawnHologram(loc, id, hologramName, player.getName());
        effects(loc, true);

        // Ein Backpack aus der Hand entfernen.
        inHand.setAmount(inHand.getAmount() - 1);
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(inHand);
        } else {
            player.getInventory().setItemInMainHand(inHand);
        }
        return null;
    }

    private void spawnEntities(Location loc, UUID id, ItemStack visual, float yaw) {
        loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(visual);
            display.setPersistent(true);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            try {
                // Nach Blickrichtung des Spielers drehen (#31): Y-Rotation aus dem Yaw.
                Quaternionf left = new Quaternionf().rotationY((float) Math.toRadians(-yaw));
                display.setTransformation(new Transformation(
                        new Vector3f(0f, 0.08f, 0f), left,
                        new Vector3f(0.82f, 0.82f, 0.82f), new Quaternionf()));
            } catch (Throwable ignored) {
            }
            tag(display, id);
        });
        loc.getWorld().spawn(loc.clone().add(0, 0.1, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(0.7f);
            interaction.setInteractionHeight(0.7f);
            interaction.setResponsive(true);
            interaction.setPersistent(true);
            tag(interaction, id);
        });
    }

    /** Optionales Hologramm (Name + Besitzer) über dem platzierten Backpack (#32). */
    private void spawnHologram(Location loc, UUID id, String name, String ownerName) {
        if (!plugin.getConfig().getBoolean("placeable.hologram.enabled", true)) return;
        Location at = loc.clone().add(0, 0.85, 0);
        loc.getWorld().spawn(at, org.bukkit.entity.TextDisplay.class, td -> {
            td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            td.setPersistent(true);
            td.setSeeThrough(false);
            td.setDefaultBackground(false);
            try {
                td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            } catch (Throwable ignored) {
            }
            td.text(mini.deserialize("<gold><name></gold><newline><gray>by <white><owner>",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", name),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed(
                            "owner", ownerName == null ? "?" : ownerName)));
            tag(td, id);
        });
    }

    /** Partikel + Sound beim Platzieren/Aufheben (#36), konfigurierbar. */
    private void effects(Location loc, boolean placing) {
        if (!plugin.getConfig().getBoolean("placeable.effects", true)) return;
        var world = loc.getWorld();
        if (world == null) return;
        Location at = loc.clone().add(0, 0.3, 0);
        world.spawnParticle(placing ? org.bukkit.Particle.HAPPY_VILLAGER : org.bukkit.Particle.POOF,
                at, 12, 0.2, 0.2, 0.2, 0.02);
        String key = placing ? "minecraft:block.wool.place" : "minecraft:block.wool.break";
        world.playSound(net.kyori.adventure.sound.Sound.sound(
                        net.kyori.adventure.key.Key.key(key),
                        net.kyori.adventure.sound.Sound.Source.BLOCK, 0.7f, 1.0f),
                at.getX(), at.getY(), at.getZ());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void applyPlacedVisualModel(ItemStack visual, BackpackTier tier, String accent) {
        if (plugin.moduleManager() == null || !plugin.moduleManager().isActive("oraxen")) return;
        String base = tier.providerId();
        if (base == null || base.isBlank()) {
            base = "ysbp_" + tier.key().toLowerCase() + "_backpack";
        }
        String fallback = tier.defaultAccentColor();
        String variant = ColorUtil.nearestDyeKey(accent, fallback);
        String defaultVariant = ColorUtil.nearestDyeKey(fallback, fallback);
        String provider = variant.equals(defaultVariant)
                ? base + "_placed"
                : base + "_placed_accent_" + variant;
        plugin.moduleManager().applyExternalModel(visual, provider);
    }

    /**
     * Ruft alle platzierten Backpacks des Spielers mit Recall-Upgrade zurück:
     * Entities entfernen, Persistenz aktualisieren, Item ins Inventar geben.
     * Gibt die Anzahl zurückgeholter Backpacks zurück.
     */
    /** Platzierte Backpacks des Spielers mit Recall-Upgrade, die gerade nicht offen sind. */
    public java.util.List<UUID> recallableIds(Player player) {
        java.util.List<UUID> out = new java.util.ArrayList<>();
        for (UUID id : manager.storage().listByOwner(player.getUniqueId())) {
            BackpackData data = manager.storage().load(id);
            if (data == null || !data.placed()) continue;
            if (manager.isOpen(id)) continue;
            if (!manager.functionUpgradesOf(id).contains("recall")) continue;
            out.add(id);
        }
        return out;
    }

    /** Holt genau ein platziertes Backpack zurück. Gibt true bei Erfolg zurück. */
    public boolean recallOne(Player player, UUID id) {
        BackpackData data = manager.storage().load(id);
        if (data == null || !data.placed()) return false;
        if (manager.isOpen(id)) return false;
        if (!manager.functionUpgradesOf(id).contains("recall")) return false;

        org.bukkit.World world = data.world() == null ? null : org.bukkit.Bukkit.getWorld(data.world());
        if (world == null) return false;
        org.bukkit.Location loc = new org.bukkit.Location(world, data.x(), data.y(), data.z());
        world.getChunkAt(loc).load();
        for (Entity e : world.getNearbyEntities(loc, 0.8, 1.4, 0.8)) {
            if (isPlacedEntity(e) && id.equals(backpackIdOf(e))) e.remove();
        }
        effects(loc, false);

        BackpackTier tier = tiers.get(data.tier());
        if (tier == null) tier = tiers.all().iterator().next();
        String main = data.mainColor() != null ? data.mainColor() : tier.defaultMainColor();
        String accent = data.accentColor() != null ? data.accentColor() : tier.defaultAccentColor();
        ItemStack item = items.create(tier, id, main, accent);
        if (data.owner() != null) {
            items.writeOwner(item, data.owner(), org.bukkit.Bukkit.getOfflinePlayer(data.owner()).getName());
            items.applyDisplay(item, tier, id, main, accent);
        }
        data.placed(false);
        manager.storage().save(data);
        player.getInventory().addItem(item).values()
                .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        return true;
    }

    public int recall(Player player) {
        int recalled = 0;
        for (UUID id : recallableIds(player)) {
            if (recallOne(player, id)) recalled++;
        }
        return recalled;
    }

    private void tag(Entity entity, UUID id) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(idKey, PersistentDataType.STRING, id.toString());
    }

    /**
     * Hebt ein platziertes Backpack wieder auf: Entities entfernen, Backpack-Item
     * droppen, Persistenz aktualisieren. Der Inhalt bleibt über die ID erhalten.
     */
    public boolean pickup(Entity clickedEntity) {
        UUID id = backpackIdOf(clickedEntity);
        if (id == null) {
            clickedEntity.remove();
            return true;
        }
        if (manager.isOpen(id)) return false;
        Location loc = clickedEntity.getLocation();

        BackpackData data = manager.storage().load(id);
        ItemStack drop;
        if (data != null) {
            BackpackTier tier = tiers.get(data.tier());
            if (tier == null) tier = tiers.all().iterator().next();
            String main = data.mainColor() != null ? data.mainColor() : tier.defaultMainColor();
            String accent = data.accentColor() != null ? data.accentColor() : tier.defaultAccentColor();
            drop = items.create(tier, id, main, accent);
            if (data.owner() != null) {
                items.writeOwner(drop, data.owner(),
                        org.bukkit.Bukkit.getOfflinePlayer(data.owner()).getName());
                items.applyDisplay(drop, tier, id, main, accent);
            }
            data.placed(false);
            manager.storage().save(data);
        } else {
            return false;
        }

        // Alle getaggten Entities mit gleicher ID entfernen (inkl. Hologramm).
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.8, 1.4, 0.8)) {
            if (isPlacedEntity(e) && id.equals(backpackIdOf(e))) {
                e.remove();
            }
        }
        effects(loc, false);
        loc.getWorld().dropItemNaturally(loc, drop);
        return true;
    }
}
