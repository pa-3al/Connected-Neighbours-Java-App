package com.app.domain.port.in;
import com.app.domain.model.DownloadProgress;
import com.app.domain.model.UpdateInfo;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
public interface CheckUpdateUseCase {
    CompletableFuture<UpdateInfo> checkForUpdates();
    CompletableFuture<Path> downloadUpdate(UpdateInfo updateInfo, Consumer<DownloadProgress> progressCallback);
    void applyUpdate(Path updateFile);
    String getCurrentVersion();
}
