package com.app.domain.model;
public record DownloadProgress(
    long bytesDownloaded,
    long totalBytes,
    double percentage
) {
    public static DownloadProgress of(long downloaded, long total) {
        double pct = total > 0 ? (downloaded * 100.0 / total) : 0;
        return new DownloadProgress(downloaded, total, pct);
    }
    public boolean isComplete() {
        return totalBytes > 0 && bytesDownloaded >= totalBytes;
    }
}
