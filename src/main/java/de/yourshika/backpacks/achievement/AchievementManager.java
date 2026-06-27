package de.yourshika.backpacks.achievement;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.gui.AchievementMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet die Backpack-Achievements: Fortschritt pro Spieler (persistent in
 * {@code achievements.yml}), Freischalt-Trigger, Benachrichtigung und die
 * Anzeige-GUI. Global und je Achievement über {@code achievements.*} in der
 * config.yml konfigurierbar.
 */
public final class AchievementManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final YourShikaBackpacks plugin;
    private final File file;
    private YamlConfiguration store;
    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();

    public AchievementManager(YourShikaBackpacks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "achievements.yml");
    }

    public void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (Exception ignored) {
            }
        }
        store = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        for (String key : store.getKeys(false)) {
            try {
                cache.put(UUID.fromString(key), new HashSet<>(store.getStringList(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private boolean globallyEnabled() {
        return plugin.getConfig().getBoolean("achievements.enabled", true);
    }

    private boolean disabled(String id) {
        return plugin.getConfig().getStringList("achievements.disabled").contains(id);
    }

    public Set<String> unlocked(UUID uuid) {
        return cache.getOrDefault(uuid, Set.of());
    }

    public boolean has(UUID uuid, String id) {
        return unlocked(uuid).contains(id);
    }

    /**
     * Schaltet ein Achievement frei, falls noch nicht erreicht (und aktiviert).
     * Idempotent – mehrfaches Auslösen ist harmlos.
     */
    public void trigger(Player player, String id) {
        if (player == null || id == null) return;
        if (!globallyEnabled() || disabled(id)) return;
        Achievement ach = Achievement.byId(id);
        if (ach == null) return;
        UUID uuid = player.getUniqueId();
        Set<String> set = cache.computeIfAbsent(uuid, u -> new HashSet<>());
        if (!set.add(id)) return; // bereits freigeschaltet
        persist(uuid, set);
        notifyUnlock(player, ach);
    }

    private void persist(UUID uuid, Set<String> set) {
        if (store == null) return;
        store.set(uuid.toString(), new ArrayList<>(set));
        try {
            store.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("Achievements konnten nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private void notifyUnlock(Player player, Achievement ach) {
        plugin.messages().send(player, "achievement.unlocked",
                de.yourshika.backpacks.config.MessageManager.ph("title", ach.title()),
                de.yourshika.backpacks.config.MessageManager.ph("desc", ach.description()));
        de.yourshika.backpacks.util.Sounds.play(plugin, player, "achievement");
        if (plugin.getConfig().getBoolean("achievements.broadcast", false)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(player)) continue;
                plugin.messages().send(p, "achievement.broadcast",
                        de.yourshika.backpacks.config.MessageManager.ph("player", player.getName()),
                        de.yourshika.backpacks.config.MessageManager.ph("title", ach.title()));
            }
        }
    }

    /** Öffnet die Achievements-Übersicht für einen Spieler. */
    public void openMenu(Player player) {
        AchievementMenuHolder holder = new AchievementMenuHolder();
        Achievement[] all = Achievement.values();
        int rows = Math.min(6, Math.max(2, (all.length + 8) / 9 + 1));
        Inventory inv = Bukkit.createInventory(holder, rows * 9,
                line("<gradient:#6E5BC8:#5BE8D4><bold>Backpack Achievements</bold></gradient>"));
        holder.setInventory(inv);

        Set<String> done = unlocked(player.getUniqueId());
        int slot = 0;
        for (Achievement ach : all) {
            if (slot >= rows * 9 - 1) break;
            boolean got = done.contains(ach.id());
            ItemStack item = new ItemStack(got ? ach.icon() : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(line((got ? "<green>✔ " : "<gray>✖ ") + "<bold>" + escape(ach.title()) + "</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(line("<gray>" + escape(ach.description())));
            lore.add(Component.empty());
            lore.add(got ? line("<green>Unlocked") : line("<dark_gray>Locked"));
            meta.lore(lore);
            if (got) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta im = info.getItemMeta();
        im.displayName(line("<gold><bold>Progress</bold>"));
        im.lore(List.of(line("<gray>Unlocked: <white>" + done.size() + "</white><gray>/<white>" + all.length)));
        info.setItemMeta(im);
        inv.setItem(rows * 9 - 1, info);

        player.openInventory(inv);
    }

    private static String escape(String s) {
        return s.replace("<", "\\<");
    }

    private static Component line(String mm) {
        return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }
}
