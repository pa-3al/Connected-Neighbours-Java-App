package com.app.infrastructure.adapter.theme;
import com.app.domain.model.Theme;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class ThemeExporter {
    private static final Path CUSTOM_THEMES_DIR = Paths.get("themes", "custom");
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    public ThemeExporter() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    public void exportTheme(String themeId, Path targetPath, Map<String, Theme> themesMap) {
        Theme theme = themesMap.get(themeId);
        if (theme == null) {
            throw new IllegalArgumentException("Theme not found: " + themeId);
        }
        if (theme.isBuiltIn()) {
            throw new IllegalArgumentException("Cannot export built-in theme (system theme): " + themeId);
        }
        Path sourceDir = CUSTOM_THEMES_DIR.resolve(themeId);
        if (!Files.exists(sourceDir)) {
            throw new IllegalStateException("Theme directory missing: " + sourceDir);
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetPath))) {
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        com.app.infrastructure.util.DailyLogger.logWarn("ThemeExport", "Error zipping file: " + path);
                    }
                });
            com.app.infrastructure.util.DailyLogger.logInfo("ThemeExport", "Exported theme " + themeId + " to " + targetPath);
        } catch (IOException e) {
            com.app.infrastructure.util.DailyLogger.logError("ThemeExport", "Failed to export theme " + themeId, e);
            throw new RuntimeException("Failed to export theme", e);
        }
    }
    public void importTheme(Path zipFile, Map<String, Theme> themesMap, Runnable reloadCallback) {
        com.app.infrastructure.util.DailyLogger.logInfo("ThemeImport", "Importing theme from: " + zipFile);
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
            ThemeManifest manifest = null;
            Map<String, byte[]> extractedFiles = new HashMap<>();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    extractedFiles.put(zipEntry.getName(), zis.readAllBytes());
                }
                zipEntry = zis.getNextEntry();
            }
            byte[] manifestBytes = extractedFiles.get("theme.json");
            if (manifestBytes == null) {
                throw new IllegalArgumentException("Invalid theme package: missing theme.json");
            }
            manifest = objectMapper.readValue(manifestBytes, ThemeManifest.class);
            if (manifest.id == null || manifest.id.isBlank()) {
                throw new IllegalArgumentException("Invalid manifest: missing 'id'");
            }
            if (themesMap.containsKey(manifest.id) && themesMap.get(manifest.id).isBuiltIn()) {
                throw new IllegalArgumentException("Cannot overwrite system theme: " + manifest.id);
            }
            Path installDir = CUSTOM_THEMES_DIR.resolve(manifest.id);
            if (Files.exists(installDir)) {
                Files.walk(installDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
            }
            Files.createDirectories(installDir);
            for (Map.Entry<String, byte[]> entry : extractedFiles.entrySet()) {
                Path filePath = installDir.resolve(entry.getKey());
                if (!filePath.normalize().startsWith(installDir.normalize())) {
                    com.app.infrastructure.util.DailyLogger.logWarn("ThemeImport", "Security: Zip Path Traversal allowed blocked: " + entry.getKey());
                    throw new SecurityException("Zip Path Traversal detected: " + entry.getKey());
                }
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, entry.getValue());
            }
            if (reloadCallback != null) {
                reloadCallback.run();
            }
            com.app.infrastructure.util.DailyLogger.logInfo("ThemeImport", "Theme imported successfully: " + manifest.id);
        } catch (IOException e) {
            com.app.infrastructure.util.DailyLogger.logError("ThemeImport", "Failed to import theme", e);
            throw new RuntimeException("Failed to import theme", e);
        }
    }
    public CompletableFuture<Theme> downloadTheme(String url, Path targetDir, Map<String, Theme> themesMap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                com.app.infrastructure.util.DailyLogger.logInfo("ThemeDownloader", "Downloading theme from: " + url);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
                HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Download failed: " + response.statusCode());
                }
                String themeId = "custom-" + System.currentTimeMillis();
                Path themeDir = targetDir.resolve(themeId);
                Files.createDirectories(themeDir);
                Path cssFile = themeDir.resolve("theme.css");
                Files.copy(response.body(), cssFile, StandardCopyOption.REPLACE_EXISTING);
                ThemeManifest manifest = new ThemeManifest();
                manifest.id = themeId;
                manifest.name = "Downloaded Theme";
                manifest.author = "Unknown";
                manifest.description = "Downloaded from " + url;
                manifest.cssFile = "theme.css";
                Path manifestPath = themeDir.resolve("theme.json");
                objectMapper.writeValue(manifestPath.toFile(), manifest);
                Theme theme = Theme.custom(
                    manifest.id, manifest.name, manifest.author, 
                    manifest.description, cssFile.toUri().toString()
                );
                themesMap.put(themeId, theme);
                com.app.infrastructure.util.DailyLogger.logInfo("ThemeDownloader", "Theme downloaded and installed: " + themeId);
                return theme;
            } catch (IOException | InterruptedException e) {
                com.app.infrastructure.util.DailyLogger.logError("ThemeDownloader", "Failed to download theme", e);
                throw new RuntimeException("Failed to download theme", e);
            }
        });
    }
    public static class ThemeManifest {
        public String id;
        public String name;
        public String author;
        public String description;
        public String cssFile;
        public Map<String, String> colors;
    }
}
