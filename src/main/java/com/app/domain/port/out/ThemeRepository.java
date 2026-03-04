package com.app.domain.port.out;
import com.app.domain.model.Theme;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
public interface ThemeRepository {
    List<Theme> getAllThemes();
    void applyTheme(String themeId);
    CompletableFuture<Theme> downloadTheme(String url, Path targetDir);
    void deleteTheme(String themeId);
    void savePreference(String themeId);
    String loadPreference();
    void saveRadius(double radius);
    double loadRadius();
    void exportTheme(String themeId, Path targetPath);
    void saveSidebarColor(String colorObj); 
    String loadSidebarColor();
    void saveSidebarWidth(double width);
    double loadSidebarWidth();
    void importTheme(Path zipFile);
    java.util.Map<String, String> getThemeProperties(String themeId);
    default void reloadThemes() {
    }
}
