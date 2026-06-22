package de.yourshika.backpacks.gui;

import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.module.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interaktive GUI für {@code /bp modules}: zeigt alle externen, experimentellen
 * Module mit ihrem Live-Status und erlaubt es Admins, den Master-Schalter und
 * jedes Modul per Klick an-/auszuschalten (mit anschließendem Live-Reload).
 */
public final class ModulesMenu {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private ModulesMenu() {}

    public static void open(YourShikaBackpacks plugin, Player player) {
        List<Module> modules = plugin.moduleManager().modules();
        boolean experimental = plugin.moduleManager().experimentalEnabled();

        ModulesMenuHolder holder = new ModulesMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                line("<gradient:#6E5BC8:#5BE8D4><bold>Externe Module</bold></gradient>"));
        holder.setInventory(inv);

        inv.setItem(ModulesMenuHolder.MASTER_SLOT, header(experimental));

        int slot = 9;
        for (Module module : modules) {
            if (slot >= 27) break;
            holder.mapSlot(slot, module.id());
            inv.setItem(slot++, moduleItem(module, experimental));
        }

        player.openInventory(inv);
    }

    /** Baut die GUI im bereits geöffneten Inventar neu auf (nach einem Toggle). */
    public static void refresh(YourShikaBackpacks plugin, ModulesMenuHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        boolean experimental = plugin.moduleManager().experimentalEnabled();
        inv.setItem(ModulesMenuHolder.MASTER_SLOT, header(experimental));
        int slot = 9;
        for (Module module : plugin.moduleManager().modules()) {
            if (slot >= 27) break;
            holder.mapSlot(slot, module.id());
            inv.setItem(slot++, moduleItem(module, experimental));
        }
    }

    private static ItemStack header(boolean experimental) {
        ItemStack item = new ItemStack(experimental ? Material.REDSTONE_TORCH : Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<gold><bold>Master-Schalter</bold></gold>"));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<gray>hooks.experimental: " + (experimental ? "<green>AN" : "<red>AUS")));
        if (experimental) {
            lore.add(line("<gray>Module dürfen laden, wenn einzeln"));
            lore.add(line("<gray>aktiviert und Plugin installiert."));
        } else {
            lore.add(line("<gray>Alle externen Module sind gesperrt."));
            lore.add(line("<gray>Das Plugin läuft eigenständig."));
        }
        lore.add(line("<yellow>Klicken zum Umschalten"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack moduleItem(Module module, boolean experimental) {
        Material material;
        if (module.isActive()) {
            material = Material.LIME_DYE;
        } else if (!module.isPluginPresent()) {
            material = Material.RED_DYE;
        } else if (!module.isEnabledInConfig() || !experimental) {
            material = Material.ORANGE_DYE;
        } else {
            material = Material.GRAY_DYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(line("<white><bold>" + module.displayName() + "</bold></white>"));
        List<Component> lore = new ArrayList<>();
        lore.add(line("<gray>" + module.description()));
        lore.add(Component.empty());
        lore.add(line("<gray>Benötigt: <white>" + module.requiredPlugin()));
        lore.add(line("<gray>Installiert: " + yesNo(module.isPluginPresent())));
        lore.add(line("<gray>In Config aktiviert: " + yesNo(module.isEnabledInConfig())));
        lore.add(line("<gray>Experimentell freigegeben: " + yesNo(experimental)));
        lore.add(Component.empty());
        lore.add(line("<gray>Status: " + (module.isActive() ? "<green><bold>AKTIV" : "<red><bold>INAKTIV")));
        lore.add(Component.empty());
        lore.add(line("<yellow>Klicken zum Umschalten"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String yesNo(boolean value) {
        return value ? "<green>ja" : "<red>nein";
    }

    private static Component line(String mm) {
        return MINI.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }
}
