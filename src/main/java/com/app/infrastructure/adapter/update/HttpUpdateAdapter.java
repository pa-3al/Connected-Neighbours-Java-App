package com.app.infrastructure.adapter.update;
import com.app.domain.model.DownloadProgress;
import com.app.domain.model.UpdateInfo;
import com.app.domain.port.out.UpdateRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
public class HttpUpdateAdapter implements UpdateRepository {
    private static final int BUFFER_SIZE = 8192;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final com.app.infrastructure.config.ConfigProvider configProvider;
    private final Supplier<Boolean> onlineSupplier;
    public HttpUpdateAdapter(com.app.infrastructure.config.ConfigProvider configProvider, Supplier<Boolean> onlineSupplier) {
        this.configProvider = configProvider;
        this.onlineSupplier = onlineSupplier;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(configProvider.getUpdateTimeoutSeconds()))
            .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.findAndRegisterModules();
    }
    @Override
    public CompletableFuture<UpdateInfo> fetchLatestUpdateInfo(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            if (!onlineSupplier.get()) {
                throw new RuntimeException("Mode hors ligne : Impossible de vérifier les mises à jour.");
            }
            try {
                String version = currentVersion != null ? currentVersion : "";
                String url = configProvider.getUpdateCheckUrl()
                    .replace("{version}", version)
                    .replace("{currentVersion}", version);
                validateHttpsUrl(url);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return normalizeUpdateInfo(objectMapper.readValue(response.body(), UpdateInfo.class), currentVersion);
                } else if (response.statusCode() == 204) {
                    return null;
                } else {
                    throw new RuntimeException("Server returned status: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                com.app.infrastructure.util.DailyLogger.logError("UpdateAdapter", "Failed to check for updates");
                throw new RuntimeException("Failed to check for updates: " + e.getMessage(), e);
            }
        });
    }
    @Override
    public CompletableFuture<Path> downloadUpdate(String downloadUrl, Path targetPath, Consumer<DownloadProgress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (!onlineSupplier.get()) {
                throw new RuntimeException("Mode hors ligne : Impossible de télécharger.");
            }
            try {
                validateHttpsUrl(downloadUrl);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofMinutes(10))
                    .GET()
                    .build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Download failed with status: " + response.statusCode());
                }
                long totalBytes = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(-1);
                Path parent = targetPath.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(targetPath, 
                         StandardOpenOption.CREATE, 
                         StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    long bytesDownloaded = 0;
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        bytesDownloaded += bytesRead;
                        if (progressCallback != null) {
                            progressCallback.accept(DownloadProgress.of(bytesDownloaded, totalBytes));
                        }
                    }
                }
                return targetPath;
            } catch (IOException | InterruptedException e) {
                com.app.infrastructure.util.DailyLogger.logError("UpdateAdapter", "Failed to download update");
                throw new RuntimeException("Failed to download update: " + e.getMessage(), e);
            }
        });
    }
    private UpdateInfo normalizeUpdateInfo(UpdateInfo info, String currentVersion) {
        if (info == null || info.version() == null || info.version().isBlank()) {
            return info;
        }
        String downloadUrl = info.downloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            downloadUrl = configProvider.getUpdateJarUrl(info.version());
        }
        String patchUrl = info.patchUrl();
        if ((patchUrl == null || patchUrl.isBlank())
                && currentVersion != null
                && !currentVersion.isBlank()
                && !currentVersion.equals(info.version())
                && configProvider.isDifferentialUpdateEnabled()) {
            patchUrl = configProvider.getUpdatePatchUrl(currentVersion, info.version());
        }
        return new UpdateInfo(
            info.version(),
            downloadUrl,
            info.description(),
            info.changelog(),
            info.releaseDate(),
            info.mandatory(),
            info.minVersion(),
            patchUrl
        );
    }
    private static void validateHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are allowed, got: " + scheme);
        }
    }
}
