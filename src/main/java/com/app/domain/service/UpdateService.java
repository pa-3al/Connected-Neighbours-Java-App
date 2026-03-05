package com.app.domain.service;
import com.app.domain.model.DownloadProgress;
import com.app.domain.model.UpdateInfo;
import com.app.domain.port.in.CheckUpdateUseCase;
import com.app.domain.port.out.LoggerPort;
import com.app.domain.port.out.UpdateRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
public class UpdateService implements CheckUpdateUseCase {
    private static final String CURRENT_VERSION = "1.0.0";
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
        return updateRepository.fetchLatestUpdateInfo()
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
        return updateRepository.downloadUpdate(updateInfo.downloadUrl(), targetPath, progressCallback)
            .whenComplete((path, ex) -> {
                if (ex != null) {
                    logger.error("UpdateService", "Update download failed", ex);
                } else {
                    logger.info("UpdateService", "Update download completed: " + path);
                }
            });
    }
    @Override
    public void applyUpdate(Path updateFile) {
        try {
            logger.warn("UpdateService", "Applying update and restarting application with: " + updateFile);
            String javaHome = System.getProperty("java.home");
            String javaBin = Paths.get(javaHome, "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-jar",
                updateFile.toAbsolutePath().toString()
            );
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
