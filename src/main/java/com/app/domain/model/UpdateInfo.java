package com.app.domain.model;
import java.time.LocalDate;
public record UpdateInfo(
    String version,
    String downloadUrl,
    String description,
    String changelog,
    LocalDate releaseDate,
    boolean mandatory,
    String minVersion,
    String patchUrl
) {
    public UpdateInfo(String version, String url, String description) {
        this(version, url, description, "", LocalDate.now(), false, "1.0.0", null);
    }
}
