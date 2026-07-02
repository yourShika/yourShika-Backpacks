package de.yourshika.backpacks.module;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Stellt die mitgelieferten Oraxen-Assets bereit, sobald das Oraxen-Modul aktiv
 * ist. Die Texturen im Plugin-Datenordner sind versioniert: alte oder noch nie
 * versionierte Defaults werden gesichert und aktualisiert, spaetere eigene
 * Server-Texturen bleiben erhalten.
 *
 * <p>Oraxen-Item-YAMLs muessen aktiv aktualisiert werden, damit neue Provider-IDs
 * verfuegbar sind. Vor jedem Ueberschreiben wird eine Backup-Kopie erstellt.</p>
 */
public final class OraxenAssetDeployer {

    private static final String BUNDLE_PREFIX = "oraxen/";
    private static final String TEX_PREFIX = "oraxen/pack/textures/";
    private static final String MODEL_PREFIX = "oraxen/pack/models/";
    private static final String ITEMS_PREFIX = "oraxen/items/";
    private static final String MANIFEST_ENTRY = "oraxen/asset-manifest.properties";
    private static final String STATE_FILE = ".oraxen-asset-state.properties";
    private static final String HASH_PREFIX = "sha256.";
    private static final String MANAGED_PREFIX = "managed.";

    private final YourShikaBackpacks plugin;
    private Path backupRoot;

    public OraxenAssetDeployer(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    /** Status-Übersicht für {@code /bp assets status}. */
    public record AssetStatus(boolean oraxenPresent, String bundledVersion, String deployedVersion,
                              int managed, int missing, int total) {
    }

    /** Berechnet (read-only) den aktuellen Asset-Status gegen die gebündelten Defaults. */
    public AssetStatus status() {
        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        boolean present = oraxen != null;
        Properties state = loadState(new File(plugin.getDataFolder(), STATE_FILE).toPath());
        String deployedVersion = state.getProperty("asset-version", "-");
        String bundledVersion = "-";
        int total = 0, missing = 0, managed = 0;
        try (ZipFile zip = new ZipFile(plugin.pluginJarFile())) {
            Properties bundled = loadBundledManifest(zip);
            bundledVersion = bundled.getProperty("asset-version", "-");
            File oraxenItems = present ? new File(oraxen.getDataFolder(), "items") : null;
            File oraxenTextures = present ? new File(oraxen.getDataFolder(), "pack/textures") : null;
            File oraxenModels = present ? new File(oraxen.getDataFolder(), "pack/models") : null;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(BUNDLE_PREFIX)) continue;
                if (name.equals(MANIFEST_ENTRY)) continue;
                total++;
                if (state.getProperty(MANAGED_PREFIX + name) != null) managed++;
                if (present) {
                    File target = null;
                    if (name.startsWith(ITEMS_PREFIX)) target = new File(oraxenItems, name.substring(ITEMS_PREFIX.length()));
                    else if (name.startsWith(TEX_PREFIX)) target = new File(oraxenTextures, name.substring(TEX_PREFIX.length()));
                    else if (name.startsWith(MODEL_PREFIX)) target = new File(oraxenModels, name.substring(MODEL_PREFIX.length()));
                    if (target != null && !target.exists()) missing++;
                }
            }
        } catch (Exception ex) {
            plugin.debug("Asset-Status fehlgeschlagen: " + ex.getMessage());
        }
        return new AssetStatus(present, bundledVersion, deployedVersion, managed, missing, total);
    }

    public void deploy() {
        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxen == null) return;

        File texturesDir = new File(plugin.getDataFolder(), "Textures");
        File modelsDir = new File(plugin.getDataFolder(), "Models");
        File oraxenData = oraxen.getDataFolder();
        File oraxenItems = new File(oraxenData, "items");
        File oraxenTextures = new File(oraxenData, "pack/textures");
        File oraxenModels = new File(oraxenData, "pack/models");
        Path statePath = new File(plugin.getDataFolder(), STATE_FILE).toPath();

        int extracted = 0, models = 0, items = 0, copied = 0, preserved = 0, backedUp = 0;
        Properties state = loadState(statePath);

        try (ZipFile zip = new ZipFile(plugin.pluginJarFile())) {
            Properties bundled = loadBundledManifest(zip);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(BUNDLE_PREFIX)) continue;
                if (name.equals(MANIFEST_ENTRY)) continue;

                String bundledHash = bundled.getProperty(HASH_PREFIX + name);
                byte[] bundledBytes = readAll(zip, entry);
                if (bundledHash == null) bundledHash = sha256(bundledBytes);

                if (name.startsWith(ITEMS_PREFIX)) {
                    File target = new File(oraxenItems, name.substring(ITEMS_PREFIX.length()));
                    if (deployItemYaml(target.toPath(), name, bundledBytes, bundledHash, state)) {
                        items++;
                    }
                } else if (name.startsWith(TEX_PREFIX)) {
                    String rel = name.substring(TEX_PREFIX.length());
                    File pluginTex = new File(texturesDir, rel);
                    TextureResult result = deployPluginTexture(pluginTex.toPath(), name,
                            bundledBytes, bundledHash, state);
                    if (result.extracted) extracted++;
                    if (result.preserved) preserved++;

                    File oraxenTex = new File(oraxenTextures, rel);
                    if (copyAssetToOraxen(pluginTex.toPath(), oraxenTex.toPath())) copied++;
                } else if (name.startsWith(MODEL_PREFIX)) {
                    String rel = name.substring(MODEL_PREFIX.length());
                    File pluginModel = new File(modelsDir, rel);
                    TextureResult result = deployPluginTexture(pluginModel.toPath(), name,
                            bundledBytes, bundledHash, state);
                    if (result.extracted) models++;
                    if (result.preserved) preserved++;

                    File oraxenModel = new File(oraxenModels, rel);
                    if (copyAssetToOraxen(pluginModel.toPath(), oraxenModel.toPath())) copied++;
                }
            }
            backedUp = backupRoot == null || !Files.exists(backupRoot) ? 0 : countFiles(backupRoot);
            saveState(statePath, state, bundled);
        } catch (Exception ex) {
            plugin.getLogger().warning("Oraxen-Assets konnten nicht bereitgestellt werden: " + ex.getMessage());
            return;
        }

        plugin.getLogger().info("Oraxen-Assets bereitgestellt: " + items + " Item-Dateien, "
                + extracted + " Texturen aktualisiert, " + models + " Modelle aktualisiert, "
                + preserved + " eigene Assets behalten, "
                + copied + " Pack-Dateien kopiert, " + backedUp + " Backups.");
        plugin.getLogger().info("Bitte einmalig '/oraxen reload' ausfuehren, damit das Resourcepack neu gebaut wird.");
    }

    private boolean deployItemYaml(Path target, String entryName, byte[] bundledBytes,
                                   String bundledHash, Properties state) throws Exception {
        if (Files.exists(target)) {
            String currentHash = sha256(Files.readAllBytes(target));
            if (currentHash.equals(bundledHash)) {
                markManaged(state, entryName, bundledHash);
                return false;
            }
            backup(target);
        }
        writeBytes(target, bundledBytes);
        markManaged(state, entryName, bundledHash);
        return true;
    }

    private TextureResult deployPluginTexture(Path target, String entryName, byte[] bundledBytes,
                                              String bundledHash, Properties state) throws Exception {
        if (!Files.exists(target)) {
            writeBytes(target, bundledBytes);
            markManaged(state, entryName, bundledHash);
            return new TextureResult(true, false);
        }

        String currentHash = sha256(Files.readAllBytes(target));
        if (currentHash.equals(bundledHash)) {
            markManaged(state, entryName, bundledHash);
            return new TextureResult(false, false);
        }

        boolean knownAsset = state.getProperty(MANAGED_PREFIX + entryName) != null;
        if (!knownAsset || wasManaged(state, entryName, currentHash) || isLegacyDefaultTexture(target)) {
            backup(target);
            writeBytes(target, bundledBytes);
            markManaged(state, entryName, bundledHash);
            return new TextureResult(true, false);
        }

        state.remove(MANAGED_PREFIX + entryName);
        return new TextureResult(false, true);
    }

    /**
     * Stellt sicher, dass die Textur im Oraxen-Pack mit unserer Quelle
     * übereinstimmt – für ALLE Items, nicht nur geänderte. Der Oraxen-Pack-Ordner
     * ist unsere verwaltete Ausgabe, daher wird hier KEIN Backup erstellt
     * (das koppelte das Kopieren bisher an Backups).
     */
    private boolean copyAssetToOraxen(Path source, Path target) throws Exception {
        if (!Files.exists(source)) return false;
        if (Files.exists(target)) {
            String sourceHash = sha256(Files.readAllBytes(source));
            String targetHash = sha256(Files.readAllBytes(target));
            if (sourceHash.equals(targetHash)) return false; // bereits aktuell
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private boolean isLegacyDefaultTexture(Path target) {
        String name = target.getFileName().toString().toLowerCase();
        if (!name.endsWith(".png")) return false;
        try {
            BufferedImage image = ImageIO.read(target.toFile());
            return image != null && image.getWidth() == 16 && image.getHeight() == 16;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void backup(Path target) throws Exception {
        if (!Files.exists(target)) return;
        if (backupRoot == null) {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            backupRoot = plugin.getDataFolder().toPath().resolve("AssetBackups").resolve(stamp);
        }
        Path absolute = target.toAbsolutePath().normalize();
        String safe = absolute.toString()
                .replace(':', '_')
                .replace('\\', '/')
                .replaceAll("[^A-Za-z0-9._/-]", "_");
        while (safe.startsWith("/")) safe = safe.substring(1);
        Path backup = backupRoot.resolve(safe);
        Files.createDirectories(backup.getParent());
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    private Properties loadBundledManifest(ZipFile zip) throws Exception {
        Properties properties = new Properties();
        ZipEntry manifest = zip.getEntry(MANIFEST_ENTRY);
        if (manifest != null) {
            try (InputStream in = zip.getInputStream(manifest)) {
                properties.load(in);
            }
        }
        return properties;
    }

    private Properties loadState(Path path) {
        Properties properties = new Properties();
        if (!Files.exists(path)) return properties;
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (Exception ignored) {
        }
        return properties;
    }

    private void saveState(Path path, Properties state, Properties bundled) {
        try {
            Files.createDirectories(path.getParent());
            state.setProperty("asset-version", bundled.getProperty("asset-version", "7"));
            try (var out = Files.newOutputStream(path)) {
                state.store(out, "yourShika Backpack's Oraxen asset state");
            }
        } catch (Exception ex) {
            plugin.debug("Oraxen-Asset-State konnte nicht geschrieben werden: " + ex.getMessage());
        }
    }

    private void writeBytes(Path target, byte[] bytes) throws Exception {
        Files.createDirectories(target.getParent());
        Files.copy(new ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private byte[] readAll(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    private void markManaged(Properties state, String entryName, String hash) {
        state.setProperty(MANAGED_PREFIX + entryName, hash);
    }

    private boolean wasManaged(Properties state, String entryName, String currentHash) {
        return currentHash.equals(state.getProperty(MANAGED_PREFIX + entryName));
    }

    private int countFiles(Path root) throws Exception {
        try (var stream = Files.walk(root)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder out = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    private record TextureResult(boolean extracted, boolean preserved) {
    }
}
