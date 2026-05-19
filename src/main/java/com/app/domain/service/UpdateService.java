package com.app.domain.service;
import com.app.domain.model.DownloadProgress;
import com.app.domain.model.UpdateInfo;
import com.app.domain.port.in.CheckUpdateUseCase;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.UpdateRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
public class UpdateService implements CheckUpdateUseCase {
    private static final String CURRENT_VERSION = resolveCurrentVersion();
    private static final Path UPDATES_DIR = Paths.get("updates");
    private final UpdateRepository updateRepository;
    private final LoggerPort logger;
    public UpdateService(UpdateRepository updateRepository, LoggerPort logger) {
        this.updateRepository = updateRepository;
        this.logger = logger;
        ensureUpdatesDirExists();
    }
    private void ensureUpdatesDirExists() {
        try {
            if (!Files.exists(UPDATES_DIR)) {
                Files.createDirectories(UPDATES_DIR);
            }
        } catch (IOException e) {
            logger.warn("UpdateService", "Updates dir creation failed");
        }
    }
    @Override
    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return updateRepository.fetchLatestUpdateInfo(CURRENT_VERSION)
            .thenApply(info -> {
                if (info != null && isNewer(info.version(), CURRENT_VERSION)) {
                    logger.info("UpdateService", "New update found: " + info.version());
                    return info;
                }
                logger.debug("UpdateService", "No updates found. Current: " + CURRENT_VERSION);
                return null;
            });
    }
    @Override
    public CompletableFuture<Path> downloadUpdate(UpdateInfo updateInfo, Consumer<DownloadProgress> progressCallback) {
        String fileName = "app-" + updateInfo.version() + ".jar";
        Path targetPath = UPDATES_DIR.resolve(fileName);
        logger.info("UpdateService", "Starting update download: " + updateInfo.version() + " to " + targetPath);
        if (shouldUsePatch(updateInfo)) {
            return downloadPatchUpdate(updateInfo, targetPath, progressCallback)
                .exceptionallyCompose(ex -> {
                    logger.warn("UpdateService", "Patch update failed, downloading full jar");
                    return downloadFullUpdate(updateInfo, targetPath, progressCallback);
                });
        }
        return downloadFullUpdate(updateInfo, targetPath, progressCallback);
    }
    private CompletableFuture<Path> downloadFullUpdate(UpdateInfo updateInfo, Path targetPath, Consumer<DownloadProgress> progressCallback) {
        if (updateInfo.downloadUrl() == null || updateInfo.downloadUrl().isBlank()) {
            CompletableFuture<Path> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Missing update download URL"));
            return failed;
        }
        return updateRepository.downloadUpdate(updateInfo.downloadUrl(), targetPath, progressCallback)
            .whenComplete((path, ex) -> {
                if (ex != null) {
                    logger.error("UpdateService", "Update download failed", ex);
                } else {
                    logger.info("UpdateService", "Update download completed: " + path);
                }
            });
    }
    private CompletableFuture<Path> downloadPatchUpdate(UpdateInfo updateInfo, Path targetPath, Consumer<DownloadProgress> progressCallback) {
        Path runningJar = getRunningJarPath();
        if (runningJar == null) {
            return downloadFullUpdate(updateInfo, targetPath, progressCallback);
        }
        Path patchPath = UPDATES_DIR.resolve("app-" + CURRENT_VERSION + "-to-" + updateInfo.version() + ".patch.jar");
        return updateRepository.downloadUpdate(updateInfo.patchUrl(), patchPath, progressCallback)
            .thenApply(patchFile -> {
                try {
                    JarPatcher.apply(runningJar, patchFile, targetPath);
                    logger.info("UpdateService", "Patch applied from " + CURRENT_VERSION + " to " + updateInfo.version());
                    return targetPath;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to apply jar patch", e);
                }
            });
    }
    public CompletableFuture<Boolean> installLatestUpdateIfAvailable() {
        return checkForUpdates()
            .thenCompose(info -> {
                if (info == null) {
                    return CompletableFuture.completedFuture(false);
                }
                return downloadUpdate(info, null)
                    .thenApply(path -> {
                        applyUpdate(path);
                        return true;
                    });
            });
    }
    @Override
    public void applyUpdate(Path updateFile) {
        try {
            logger.warn("UpdateService", "Applying update and restarting application with: " + updateFile);
            ProcessBuilder pb;
            Path runningJar = getRunningJarPath();
            if (runningJar != null) {
                pb = new ProcessBuilder(
                    getJavaBin(),
                    "-cp",
                    updateFile.toAbsolutePath().toString(),
                    "com.app.infrastructure.update.UpdateInstaller",
                    runningJar.toAbsolutePath().toString(),
                    updateFile.toAbsolutePath().toString(),
                    Long.toString(ProcessHandle.current().pid())
                );
            } else {
                pb = new ProcessBuilder(
                    getJavaBin(),
                    "-jar",
                    updateFile.toAbsolutePath().toString()
                );
            }
            pb.inheritIO();
            pb.start();
            logger.info("UpdateService", "Application shutting down for update");
            javafx.application.Platform.exit();
        } catch (IOException e) {
            logger.error("UpdateService", "Failed to apply update", e);
            throw new RuntimeException("Failed to apply update", e);
        }
    }
    @Override
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    private boolean shouldUsePatch(UpdateInfo updateInfo) {
        return updateInfo.patchUrl() != null && !updateInfo.patchUrl().isBlank() && getRunningJarPath() != null;
    }
    private static String resolveCurrentVersion() {
        String packageVersion = UpdateService.class.getPackage().getImplementationVersion();
        if (isResolvedVersion(packageVersion)) {
            return packageVersion;
        }
        try (InputStream input = UpdateService.class.getResourceAsStream("/version.properties")) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                String version = properties.getProperty("app.version");
                if (isResolvedVersion(version)) {
                    return version;
                }
            }
        } catch (IOException ignored) {
        }
        return "1.0.0";
    }
    private static boolean isResolvedVersion(String version) {
        return version != null && !version.isBlank() && !version.contains("${");
    }
    private static String getJavaBin() {
        String javaHome = System.getProperty("java.home");
        return Paths.get(javaHome, "bin", "java").toString();
    }
    private Path getRunningJarPath() {
        try {
            URI location = UpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(location);
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                return path;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    private boolean isNewer(String remote, String current) {
        int[] remoteParts = parseVersion(remote);
        int[] currentParts = parseVersion(current);
        for (int i = 0; i < Math.max(remoteParts.length, currentParts.length); i++) {
            int r = i < remoteParts.length ? remoteParts[i] : 0;
            int c = i < currentParts.length ? currentParts[i] : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }
    private int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
