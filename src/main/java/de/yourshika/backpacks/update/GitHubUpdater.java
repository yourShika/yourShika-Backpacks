package de.yourshika.backpacks.update;

import de.yourshika.backpacks.YourShikaBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.yourshika.backpacks.config.MessageManager.ph;

/**
 * Einfacher Self-Updater über die GitHub-Releases-API.
 *
 * <p>{@code /bp update} lädt die neueste Release-JAR und legt sie im
 * {@code plugins/update/}-Ordner ab. Bukkit/Paper übernimmt sie automatisch beim
 * nächsten Server-Neustart – die gespeicherten Backpack-Daten bleiben dabei
 * vollständig erhalten (separate Datenbank-/YAML-Dateien).</p>
 */
public final class GitHubUpdater {

    private static final String API_LATEST =
            "https://api.github.com/repos/yourShika/yourShika-Backpacks/releases/latest";

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_URL = Pattern.compile(
            "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

    private final YourShikaBackpacks plugin;

    public GitHubUpdater(YourShikaBackpacks plugin) {
        this.plugin = plugin;
    }

    /** Startet die Update-Prüfung asynchron und meldet das Ergebnis an {@code sender}. */
    public void checkAndUpdate(CommandSender sender) {
        plugin.messages().send(sender, "update.checking");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(sender));
    }

    private void run(CommandSender sender) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(API_LATEST))
                    .header("User-Agent", "yourShika-Backpacks-Updater")
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                fail(sender, "GitHub API HTTP " + response.statusCode());
                return;
            }
            String body = response.body();

            Matcher tagMatcher = TAG.matcher(body);
            Matcher urlMatcher = JAR_URL.matcher(body);
            if (!tagMatcher.find()) {
                fail(sender, "Keine Versionsangabe gefunden.");
                return;
            }
            String latest = tagMatcher.group(1).replaceFirst("^[vV]", "");
            String current = plugin.getPluginMeta().getVersion();

            if (latest.equalsIgnoreCase(current)) {
                main(() -> plugin.messages().send(sender, "update.up-to-date", ph("version", current)));
                return;
            }
            if (!urlMatcher.find()) {
                fail(sender, "Keine JAR im Release gefunden.");
                return;
            }
            String jarUrl = urlMatcher.group(1);
            main(() -> plugin.messages().send(sender, "update.downloading", ph("version", latest)));

            download(client, jarUrl);
            main(() -> plugin.messages().send(sender, "update.success", ph("version", latest)));
        } catch (Throwable t) {
            fail(sender, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void download(HttpClient client, String jarUrl) throws Exception {
        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            throw new IllegalStateException("Update-Ordner konnte nicht erstellt werden.");
        }
        // Dateiname MUSS dem aktuellen Plugin-JAR entsprechen, damit Bukkit/Paper
        // beim Neustart automatisch ersetzt.
        File target = new File(updateFolder, plugin.pluginJarFile().getName());

        HttpRequest request = HttpRequest.newBuilder(URI.create(jarUrl))
                .header("User-Agent", "yourShika-Backpacks-Updater")
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Download HTTP " + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, Path.of(target.toURI()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void fail(CommandSender sender, String error) {
        main(() -> plugin.messages().send(sender, "update.failed", ph("error", error)));
    }

    private void main(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
