package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Stellt die mitgelieferten Oraxen-Assets bereit, sobald das Oraxen-Modul aktiv
 * ist:
 *
 * <ol>
 *   <li>Die Standard-Texturen werden – nur wenn Oraxen vorhanden &amp; aktiv ist –
 *       nach {@code plugins/yourShika Backpack's/Textures/} entpackt. Dort sind
 *       sie pro Dateiname austauschbar.</li>
 *   <li>Die Item-Definitionen werden nach {@code plugins/Oraxen/items/} kopiert.</li>
 *   <li>Die (ggf. angepassten) Texturen aus dem Plugin-Ordner werden in Oraxens
 *       Pack-Ordner kopiert, sodass sie im von Oraxen ausgelieferten
 *       Resourcepack landen.</li>
 * </ol>
 *
 * <p>Alles ist best-effort und in try/catch gekapselt – schlägt etwas fehl,
 * läuft das Plugin normal weiter (dann eben mit Vanilla-Optik).</p>
 */
public final class OraxenAssetDeployer {

    private static final String BUNDLE_PREFIX = "oraxen/";
    private static final String TEX_PREFIX = "oraxen/pack/textures/";
    private static final String ITEMS_PREFIX = "oraxen/items/";

    private final YourShikaBackpacks plugin;

    public OraxenAssetDeployer(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    public void deploy() {
        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxen == null) return;

        File texturesDir = new File(plugin.getDataFolder(), "Textures");
        File oraxenData = oraxen.getDataFolder();
        File oraxenItems = new File(oraxenData, "items");
        File oraxenTextures = new File(oraxenData, "pack/textures");

        int extracted = 0, items = 0, copied = 0;
        try (ZipFile zip = new ZipFile(plugin.pluginJarFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(BUNDLE_PREFIX)) continue;

                if (name.startsWith(ITEMS_PREFIX)) {
                    // Item-Definition -> Oraxen/items (immer aktualisieren).
                    File target = new File(oraxenItems, name.substring(ITEMS_PREFIX.length()));
                    if (writeIfPossible(zip, entry, target, true)) items++;
                } else if (name.startsWith(TEX_PREFIX)) {
                    String rel = name.substring(TEX_PREFIX.length());
                    // 1) In den Plugin-Texturordner entpacken (nur falls noch nicht da).
                    File pluginTex = new File(texturesDir, rel);
                    if (!pluginTex.exists() && writeIfPossible(zip, entry, pluginTex, false)) extracted++;
                    // 2) Aus dem Plugin-Texturordner (ggf. angepasst) zu Oraxen kopieren.
                    File oraxenTex = new File(oraxenTextures, rel);
                    File source = pluginTex.exists() ? pluginTex : null;
                    if (source != null) {
                        if (copyFile(source.toPath(), oraxenTex)) copied++;
                    } else if (writeIfPossible(zip, entry, oraxenTex, true)) {
                        copied++;
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Oraxen-Assets konnten nicht bereitgestellt werden: " + ex.getMessage());
            return;
        }

        plugin.getLogger().info("Oraxen-Assets bereitgestellt: " + items + " Item-Dateien, "
                + extracted + " Texturen entpackt, " + copied + " Texturen kopiert.");
        plugin.getLogger().info("Bitte einmalig '/oraxen reload' ausführen, damit das Resourcepack neu gebaut wird.");
    }

    private boolean writeIfPossible(ZipFile zip, ZipEntry entry, File target, boolean overwrite) {
        try {
            if (target.exists() && !overwrite) return false;
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean copyFile(Path source, File target) {
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
