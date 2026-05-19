package com.app.domain.port.out;
import com.app.domain.model.DownloadProgress;
import com.app.domain.model.UpdateInfo;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
public interface UpdateRepository {
    CompletableFuture<UpdateInfo> fetchLatestUpdateInfo(String currentVersion);
    CompletableFuture<Path> downloadUpdate(String downloadUrl, Path targetPath, Consumer<DownloadProgress> progressCallback);
}
