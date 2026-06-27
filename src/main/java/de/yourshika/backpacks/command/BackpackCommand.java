package de.yourshika.backpacks.command;

import de.yourshika.backpacks.BackpackManager;
import de.yourshika.backpacks.YourShikaBackpacks;
import de.yourshika.backpacks.config.MessageManager;
import de.yourshika.backpacks.item.BackpackItemFactory;
import de.yourshika.backpacks.storage.BackpackData;
import de.yourshika.backpacks.tier.BackpackTier;
import de.yourshika.backpacks.tier.TierRegistry;
import de.yourshika.backpacks.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.yourshika.backpacks.config.MessageManager.ph;

/**
 * Verarbeitet /backpack (Aliase: /bp, /ybackpack, /ysbackpack) inkl. Tab-Completion.
 */
public final class BackpackCommand implements CommandExecutor, TabCompleter {

    private final YourShikaBackpacks plugin;
    private final BackpackManager manager;
    private final TierRegistry tiers;
    private final BackpackItemFactory items;
    private final MessageManager msg;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public BackpackCommand(YourShikaBackpacks plugin, BackpackManager manager, TierRegistry tiers) {
        this.plugin = plugin;
        this.manager = manager;
        this.tiers = tiers;
        this.items = manager.items();
        this.msg = plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help" -> help(sender);
            case "open" -> open(sender);
            case "color", "farbe" -> color(sender, args);
            case "rename", "name" -> rename(sender, args);
            case "list" -> list(sender, args);
            case "give" -> give(sender, args);
            case "openid" -> openId(sender, args);
            case "transfer" -> transfer(sender, args);
            case "locate" -> locate(sender, args);
            case "goto", "tp" -> gotoBackpack(sender, args);
            case "info", "recipes", "rezepte" -> info(sender);
            case "recall" -> recall(sender);
            case "modules", "module" -> modules(sender);
            case "assets" -> assets(sender, args);
            case "doctor" -> doctor(sender);
            case "update" -> update(sender);
            case "reload" -> reload(sender);
            case "version", "ver" -> version(sender);
            default -> msg.send(sender, "error.unknown-subcommand", ph("input", args[0]));
        }
        return true;
    }

    private void help(CommandSender sender) {
        msg.sendRaw(sender, "help.header");
        msg.sendRaw(sender, "help.open");
        msg.sendRaw(sender, "help.info");
        msg.sendRaw(sender, "help.rename");
        msg.sendRaw(sender, "help.list");
        msg.sendRaw(sender, "help.locate");
        msg.sendRaw(sender, "help.transfer");
        msg.sendRaw(sender, "help.recall");
        if (sender.hasPermission("yourshika.backpack.admin.color")) msg.sendRaw(sender, "help.color");
        if (sender.hasPermission("yourshika.backpack.admin.give")) msg.sendRaw(sender, "help.give");
        if (sender.hasPermission("yourshika.backpack.admin.openid")) msg.sendRaw(sender, "help.openid");
        if (sender.hasPermission("yourshika.backpack.admin.modules")) msg.sendRaw(sender, "help.modules");
        if (sender.hasPermission("yourshika.backpack.admin.assets")) msg.sendRaw(sender, "help.assets");
        if (sender.hasPermission("yourshika.backpack.admin.doctor")) msg.sendRaw(sender, "help.doctor");
        if (sender.hasPermission("yourshika.backpack.admin.update")) msg.sendRaw(sender, "help.update");
        if (sender.hasPermission("yourshika.backpack.admin.reload")) msg.sendRaw(sender, "help.reload");
        msg.sendRaw(sender, "help.footer");
    }

    private void info(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        de.yourshika.backpacks.gui.InfoMenu.openOverview(plugin, player);
    }

    private void modules(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (!sender.hasPermission("yourshika.backpack.admin.modules")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        de.yourshika.backpacks.gui.ModulesMenu.open(plugin, player);
    }

    private void open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (!player.hasPermission("yourshika.backpack.open")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isBackpack(item)) {
            msg.send(sender, "error.hold-backpack");
            return;
        }
        String error = manager.openFromItem(player, item);
        if (error != null) {
            msg.send(sender, error);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }

    private void color(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (!player.hasPermission("yourshika.backpack.admin.color")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "error.color-usage");
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isBackpack(item)) {
            msg.send(sender, "error.hold-backpack");
            return;
        }
        if (!ColorUtil.isValid(args[1])) {
            msg.send(sender, "error.invalid-color", ph("input", args[1]));
            return;
        }
        String main = ColorUtil.normalize(args[1], args[1]);
        String accent = main;
        if (args.length >= 3) {
            if (!ColorUtil.isValid(args[2])) {
                msg.send(sender, "error.invalid-color", ph("input", args[2]));
                return;
            }
            accent = ColorUtil.normalize(args[2], args[2]);
        }

        String tierKey = items.getTierKey(item);
        BackpackTier tier = tiers.get(tierKey);
        if (tier == null) {
            msg.send(sender, "error.invalid-backpack");
            return;
        }

        // ID sicherstellen, Farben im Item und in der Persistenz aktualisieren.
        UUID id = items.getId(item);
        if (id == null) {
            id = UUID.randomUUID();
            items.writeId(item, id);
        }
        items.writeColors(item, main, accent);
        // refresh() statt nur applyDisplay, damit auch das externe Akzent-Modell wechselt.
        items.refresh(item, tier);
        player.getInventory().setItemInMainHand(item);

        BackpackData data = manager.storage().load(id);
        if (data == null) {
            data = new BackpackData(id);
            data.owner(player.getUniqueId());
            data.tier(tier.key());
            data.contents(new ItemStack[tier.storageSlots()]);
        }
        data.mainColor(main);
        data.accentColor(accent);
        manager.storage().save(data);

        msg.send(sender, "color.success",
                ph("main", ColorUtil.pretty(main)), ph("accent", ColorUtil.pretty(accent)));
    }

    private void rename(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        boolean admin = player.hasPermission("yourshika.backpack.admin.rename");
        if (!admin) {
            if (!plugin.pluginConfig().renameAllowed()) {
                msg.send(sender, "rename.disabled");
                return;
            }
            if (!player.hasPermission("yourshika.backpack.rename")) {
                msg.send(sender, "error.no-permission");
                return;
            }
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!items.isBackpack(item)) {
            msg.send(sender, "error.hold-backpack");
            return;
        }
        BackpackTier tier = tiers.get(items.getTierKey(item));
        if (tier == null) {
            msg.send(sender, "error.invalid-backpack");
            return;
        }
        UUID id = items.getId(item);
        if (id == null) {
            id = UUID.randomUUID();
            items.writeId(item, id);
        }
        String main = items.getMainColor(item, tier.defaultMainColor());
        String accent = items.getAccentColor(item, tier.defaultAccentColor());

        boolean reset = args.length < 2 || args[1].equalsIgnoreCase("reset") || args[1].equalsIgnoreCase("clear");
        if (reset) {
            items.writeName(item, null);
            items.applyDisplay(item, tier, id, main, accent);
            player.getInventory().setItemInMainHand(item);
            msg.send(sender, "rename.cleared");
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                .replace('§', ' ').trim();
        if (name.length() > 48) name = name.substring(0, 48);
        items.writeName(item, name);
        items.applyDisplay(item, tier, id, main, accent);
        player.getInventory().setItemInMainHand(item);
        msg.send(sender, "rename.success", ph("name", name));
        if (plugin.achievements() != null) {
            plugin.achievements().trigger(player, "rename");
            if (name.indexOf('&') >= 0 || name.indexOf('<') >= 0) {
                plugin.achievements().trigger(player, "color_name");
            }
        }
    }

    private void list(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            if (!sender.hasPermission("yourshika.backpack.admin.listother")) {
                msg.send(sender, "error.no-permission");
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        } else {
            if (!(sender instanceof Player p)) {
                msg.send(sender, "error.players-only");
                return;
            }
            if (!sender.hasPermission("yourshika.backpack.list")) {
                msg.send(sender, "error.no-permission");
                return;
            }
            target = p;
        }

        List<UUID> ids = manager.storage().listByOwner(target.getUniqueId());
        msg.send(sender, "list.header",
                ph("player", target.getName() == null ? args.length >= 2 ? args[1] : "?" : target.getName()),
                ph("count", String.valueOf(ids.size())));
        for (UUID id : ids) {
            BackpackData data = manager.storage().load(id);
            String tierKey = data == null ? "?" : data.tier();
            boolean placed = data != null && data.placed();
            sender.sendMessage(listEntry(id, tierKey, placed));
        }
    }

    /** Baut eine anklickbare Listen-Zeile (Öffnen / ID kopieren / TP bei platziert). */
    private Component listEntry(UUID id, String tier, boolean placed) {
        String full = id.toString();
        String shortId = full.substring(0, 8);
        StringBuilder sb = new StringBuilder();
        sb.append("<dark_gray>• <gray>ID <white>").append(shortId)
          .append(" <dark_gray>–</dark_gray> <gray>Tier <white>").append(tier == null ? "?" : tier);
        sb.append(" <click:run_command:'/bp openid ").append(full)
          .append("'><hover:show_text:'Open this backpack'><green>[Open]</green></hover></click>");
        sb.append(" <click:copy_to_clipboard:'").append(full)
          .append("'><hover:show_text:'Copy full ID'><yellow>[Copy]</yellow></hover></click>");
        if (placed) {
            sb.append(" <click:run_command:'/bp goto ").append(full)
              .append("'><hover:show_text:'Teleport to placed backpack'><aqua>[TP]</aqua></hover></click>");
        }
        return mini.deserialize(sb.toString());
    }

    private void transfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        boolean admin = player.hasPermission("yourshika.backpack.admin.transfer");
        if (!admin && !player.hasPermission("yourshika.backpack.transfer")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        if (args.length < 3) {
            msg.send(sender, "error.transfer-usage");
            return;
        }
        UUID id = tryUuid(args[1]);
        if (id == null) {
            msg.send(sender, "error.invalid-id", ph("input", args[1]));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            msg.send(sender, "error.player-not-found", ph("input", args[2]));
            return;
        }
        BackpackData data = manager.storage().load(id);
        if (data == null) {
            msg.send(sender, "error.id-not-found");
            return;
        }
        if (!admin && (data.owner() == null || !data.owner().equals(player.getUniqueId()))) {
            msg.send(sender, "error.not-owner");
            return;
        }
        data.owner(target.getUniqueId());
        manager.storage().save(data);
        updateHeldOwner(player, id, target);
        plugin.audit(player.getName(), "TRANSFER", id + " -> " + target.getName());
        msg.send(sender, "transfer.success",
                ph("id", id.toString().substring(0, 8)), ph("player", target.getName()));
        msg.send(target, "transfer.received",
                ph("id", id.toString().substring(0, 8)), ph("player", player.getName()));
        if (plugin.achievements() != null) plugin.achievements().trigger(player, "transfer");
    }

    /** Schreibt den neuen Besitzer in ein Backpack-Item, falls der Spieler es trägt. */
    private void updateHeldOwner(Player holderPlayer, UUID id, Player target) {
        for (ItemStack it : holderPlayer.getInventory().getContents()) {
            if (it == null) continue;
            if (items.isBackpack(it) && id.equals(items.getId(it))) {
                items.writeOwner(it, target.getUniqueId(), target.getName());
            }
        }
    }

    private void locate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (args.length >= 2) {
            UUID maybe = tryUuid(args[1]);
            if (maybe != null) {
                locateOne(sender, player, maybe);
                return;
            }
            if (!sender.hasPermission("yourshika.backpack.admin.locateother")) {
                msg.send(sender, "error.no-permission");
                return;
            }
            OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
            locatePlaced(sender, t.getUniqueId(), args[1]);
            return;
        }
        if (!player.hasPermission("yourshika.backpack.locate")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        locatePlaced(sender, player.getUniqueId(), player.getName());
    }

    private void locateOne(CommandSender sender, Player player, UUID id) {
        BackpackData data = manager.storage().load(id);
        if (data == null) {
            msg.send(sender, "error.id-not-found");
            return;
        }
        boolean admin = player.hasPermission("yourshika.backpack.admin.locateother");
        if (!admin && (data.owner() == null || !data.owner().equals(player.getUniqueId()))) {
            msg.send(sender, "error.not-owner");
            return;
        }
        if (!data.placed()) {
            msg.send(sender, "locate.not-placed");
            return;
        }
        sender.sendMessage(locateLine(data));
    }

    private void locatePlaced(CommandSender sender, UUID owner, String name) {
        List<UUID> ids = manager.storage().listByOwner(owner);
        List<BackpackData> placed = new ArrayList<>();
        for (UUID id : ids) {
            BackpackData d = manager.storage().load(id);
            if (d != null && d.placed()) placed.add(d);
        }
        msg.send(sender, "locate.header",
                ph("player", name == null ? "?" : name), ph("count", String.valueOf(placed.size())));
        for (BackpackData d : placed) {
            sender.sendMessage(locateLine(d));
        }
    }

    private Component locateLine(BackpackData d) {
        String full = d.id().toString();
        String shortId = full.substring(0, 8);
        String w = d.world() == null ? "?" : d.world();
        String coords = (int) Math.floor(d.x()) + ", " + (int) Math.floor(d.y()) + ", " + (int) Math.floor(d.z());
        String s = "<dark_gray>• <gray>ID <white>" + shortId + " <dark_gray>–</dark_gray> <gray>"
                + w + " <white>" + coords
                + " <click:run_command:'/bp goto " + full + "'><hover:show_text:'Teleport'><aqua>[TP]</aqua></hover></click>";
        return mini.deserialize(s);
    }

    private void gotoBackpack(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "error.locate-usage");
            return;
        }
        UUID id = tryUuid(args[1]);
        if (id == null) {
            msg.send(sender, "error.invalid-id", ph("input", args[1]));
            return;
        }
        BackpackData data = manager.storage().load(id);
        if (data == null || !data.placed()) {
            msg.send(sender, "locate.not-placed");
            return;
        }
        boolean admin = player.hasPermission("yourshika.backpack.admin.locateother");
        if (!admin && (data.owner() == null || !data.owner().equals(player.getUniqueId()))) {
            msg.send(sender, "error.not-owner");
            return;
        }
        World w = Bukkit.getWorld(data.world());
        if (w == null) {
            msg.send(sender, "locate.world-missing");
            return;
        }
        player.teleport(new Location(w, data.x() + 0.5, data.y() + 1, data.z() + 0.5));
        msg.send(sender, "locate.teleported", ph("id", id.toString().substring(0, 8)));
    }

    private UUID tryUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yourshika.backpack.admin.give")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        if (args.length < 3) {
            msg.send(sender, "error.give-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg.send(sender, "error.player-not-found", ph("input", args[1]));
            return;
        }
        String key = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                msg.send(sender, "error.invalid-number", ph("input", args[3]));
                return;
            }
        }

        // 1) Backpack-Tier (mit optionalen Farben).
        BackpackTier tier = tiers.get(key);
        if (tier != null) {
            String main = args.length >= 5 ? colorTokenOrDefault(args[4], tier.defaultMainColor()) : tier.defaultMainColor();
            String accent = args.length >= 6 ? colorTokenOrDefault(args[5], tier.defaultAccentColor()) : tier.defaultAccentColor();
            for (int i = 0; i < amount; i++) {
                ItemStack item = manager.createNew(tier, target.getUniqueId(), main, accent);
                target.getInventory().addItem(item).values()
                        .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
            }
            announceGive(sender, target, amount, tier.key());
            return;
        }

        // 2) Upgrade-/Funktions-Item.
        ItemStack proto = resolveGiveItem(key);
        if (proto == null) {
            msg.send(sender, "error.invalid-tier", ph("input", args[2]));
            return;
        }
        ItemStack stack = proto.clone();
        stack.setAmount(amount);
        target.getInventory().addItem(stack).values()
                .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        announceGive(sender, target, amount, key);
    }

    private void recall(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (!player.hasPermission("yourshika.backpack.place")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        var ids = plugin.placeableManager().recallableIds(player);
        if (ids.isEmpty()) {
            msg.send(player, "place.recall-none");
            return;
        }
        if (ids.size() == 1) {
            if (plugin.placeableManager().recallOne(player, ids.get(0))) {
                plugin.audit(player.getName(), "RECALL", "1 backpack");
                msg.send(player, "place.recalled", ph("count", "1"));
            } else {
                msg.send(player, "place.recall-none");
            }
            return;
        }
        // Mehrere platzierte Backpacks -> Auswahlmenü (#38).
        de.yourshika.backpacks.gui.RecallMenu.open(plugin, player, ids);
    }

    private void announceGive(CommandSender sender, Player target, int amount, String what) {
        plugin.audit(sender.getName(), "GIVE", amount + "x " + what + " -> " + target.getName());
        msg.send(sender, "give.success",
                ph("amount", String.valueOf(amount)), ph("tier", what), ph("player", target.getName()));
        msg.send(target, "give.received",
                ph("amount", String.valueOf(amount)), ph("tier", what));
    }

    /** Löst einen Give-Schlüssel zu einem Upgrade-/Funktions-Item auf (oder null). */
    private ItemStack resolveGiveItem(String key) {
        if (key.equals("base") || key.equals("upgrade_base") || key.equals("upgrade_leather")) {
            return plugin.upgradeManager().baseUpgradeItem();
        }
        if (key.startsWith("upgrade_")) {
            ItemStack t = plugin.upgradeManager().upgradeItem(key.substring("upgrade_".length()));
            if (t != null) return t;
        }
        if (key.endsWith("_upgrade")) {
            ItemStack t = plugin.upgradeManager().upgradeItem(key.substring(0, key.length() - "_upgrade".length()));
            if (t != null) return t;
        }
        if (de.yourshika.backpacks.upgrade.FunctionUpgrade.byId(key) != null) {
            return plugin.functionUpgrades().item(key);
        }
        return null;
    }

    private List<String> giveKeys() {
        List<String> keys = new ArrayList<>(tiers.keys());
        keys.add("upgrade_base");
        for (int i = 1; i < tiers.keys().size(); i++) {
            keys.add("upgrade_" + tiers.keys().get(i));
        }
        for (var fu : de.yourshika.backpacks.upgrade.FunctionUpgrade.values()) {
            keys.add(fu.id());
        }
        return keys;
    }

    private void openId(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "error.players-only");
            return;
        }
        if (!sender.hasPermission("yourshika.backpack.admin.openid")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "error.openid-usage");
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(args[1]);
        } catch (IllegalArgumentException ex) {
            msg.send(sender, "error.invalid-id", ph("input", args[1]));
            return;
        }
        String error = manager.openById(player, id);
        if (error != null) {
            msg.send(sender, error);
        } else {
            plugin.audit(player.getName(), "OPENID", id.toString());
        }
    }

    private void update(CommandSender sender) {
        if (!sender.hasPermission("yourshika.backpack.admin.update")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        new de.yourshika.backpacks.update.GitHubUpdater(plugin).checkAndUpdate(sender);
    }

    private void assets(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yourshika.backpack.admin.assets")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        if (plugin.moduleManager() == null || !plugin.moduleManager().isActive("oraxen")) {
            msg.send(sender, "assets.no-oraxen");
            return;
        }
        var deployer = new de.yourshika.backpacks.module.OraxenAssetDeployer(plugin);
        String sub = args.length >= 2 ? args[1].toLowerCase() : "status";
        if (sub.equals("redeploy")) {
            deployer.deploy();
            plugin.audit(sender.getName(), "ASSETS", "redeploy");
            msg.send(sender, "assets.redeployed");
            return;
        }
        var st = deployer.status();
        msg.send(sender, "assets.status-header");
        msg.sendRaw(sender, "assets.status-oraxen", ph("value", st.oraxenPresent() ? "yes" : "no"));
        msg.sendRaw(sender, "assets.status-version",
                ph("bundled", st.bundledVersion()), ph("deployed", st.deployedVersion()));
        msg.sendRaw(sender, "assets.status-files",
                ph("managed", String.valueOf(st.managed())),
                ph("total", String.valueOf(st.total())),
                ph("missing", String.valueOf(st.missing())));
    }

    private void doctor(CommandSender sender) {
        if (!sender.hasPermission("yourshika.backpack.admin.doctor")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        msg.send(sender, "doctor.header");
        doctorLine(sender, "Server", Bukkit.getVersion());
        doctorLine(sender, "Java", System.getProperty("java.version"));
        doctorLine(sender, "Plugin", plugin.getPluginMeta().getVersion());
        doctorLine(sender, "Storage", plugin.pluginConfig().storageType()
                + " (" + manager.storage().count() + " backpacks)");
        doctorLine(sender, "Config", "v" + plugin.getConfig().getInt("config-version", -1));
        doctorLine(sender, "Tiers", String.valueOf(tiers.all().size()));
        boolean oraxen = plugin.moduleManager() != null && plugin.moduleManager().isActive("oraxen");
        doctorLine(sender, "Oraxen module", oraxen ? "active" : "inactive");
        if (oraxen) {
            var st = new de.yourshika.backpacks.module.OraxenAssetDeployer(plugin).status();
            String assets = "v" + st.deployedVersion() + " (bundled v" + st.bundledVersion() + ")"
                    + (st.missing() > 0 ? ", " + st.missing() + " missing" : ", complete");
            doctorLine(sender, "Assets", assets);
        }
        boolean papi = plugin.moduleManager() != null && plugin.moduleManager().isActive("placeholderapi");
        doctorLine(sender, "PlaceholderAPI", papi ? "active" : "inactive");
    }

    private void doctorLine(CommandSender sender, String label, String value) {
        msg.sendRaw(sender, "doctor.line", ph("label", label), ph("value", value));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("yourshika.backpack.admin.reload")) {
            msg.send(sender, "error.no-permission");
            return;
        }
        plugin.reloadAll();
        msg.send(sender, "reload.success");
    }

    private void version(CommandSender sender) {
        msg.send(sender, "version.info",
                ph("version", plugin.getPluginMeta().getVersion()),
                ph("tiers", String.valueOf(tiers.all().size())),
                ph("count", String.valueOf(manager.storage().count())));
    }

    private String colorTokenOrDefault(String s, String def) {
        return ColorUtil.isValid(s) ? ColorUtil.normalize(s, def) : def;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "open", "info", "rename", "list", "locate", "transfer", "recall", "version"));
            if (sender.hasPermission("yourshika.backpack.admin.color")) subs.add("color");
            if (sender.hasPermission("yourshika.backpack.admin.give")) subs.add("give");
            if (sender.hasPermission("yourshika.backpack.admin.openid")) subs.add("openid");
            if (sender.hasPermission("yourshika.backpack.admin.modules")) subs.add("modules");
            if (sender.hasPermission("yourshika.backpack.admin.assets")) subs.add("assets");
            if (sender.hasPermission("yourshika.backpack.admin.doctor")) subs.add("doctor");
            if (sender.hasPermission("yourshika.backpack.admin.update")) subs.add("update");
            if (sender.hasPermission("yourshika.backpack.admin.reload")) subs.add("reload");
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("give")) {
            if (args.length == 2) return filter(onlinePlayers(), args[1]);
            if (args.length == 3) return filter(giveKeys(), args[2]);
            if (args.length == 4) return filter(List.of("1", "8", "16"), args[3]);
            if (args.length == 5 || args.length == 6) return filter(dyeNames(), args[args.length - 1]);
        }
        if ((sub.equals("color") || sub.equals("farbe")) && (args.length == 2 || args.length == 3)
                && sender.hasPermission("yourshika.backpack.admin.color")) {
            return filter(dyeNames(), args[args.length - 1]);
        }
        if (sub.equals("list") && args.length == 2 && sender.hasPermission("yourshika.backpack.admin.listother")) {
            return filter(onlinePlayers(), args[1]);
        }
        if (sub.equals("transfer") && args.length == 3) {
            return filter(onlinePlayers(), args[2]);
        }
        if (sub.equals("locate") && args.length == 2 && sender.hasPermission("yourshika.backpack.admin.locateother")) {
            return filter(onlinePlayers(), args[1]);
        }
        if (sub.equals("assets") && args.length == 2 && sender.hasPermission("yourshika.backpack.admin.assets")) {
            return filter(List.of("status", "redeploy"), args[1]);
        }
        return List.of();
    }

    private List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> dyeNames() {
        return Arrays.stream(DyeColor.values()).map(d -> d.name().toLowerCase()).collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(p)).collect(Collectors.toList());
    }
}
