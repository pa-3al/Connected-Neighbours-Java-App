package com.app.domain.port.in;
import com.app.domain.model.Theme;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
public interface ThemeUseCase {
    List<Theme> getAvailableThemes();
    Theme getCurrentTheme();
    void switchTheme(String themeId);
    CompletableFuture<Theme> downloadTheme(String url);
    void deleteTheme(String themeId);
    void exportTheme(String themeId, Path targetPath);
    void importTheme(Path zipFile);
    void savePreference(String themeId);
    void saveRadius(double radius);
    double loadRadius();
    void saveSidebarColor(String color);
    String loadSidebarColor();
    void saveSidebarWidth(double width);
    double loadSidebarWidth();
    Map<String, String> getThemeProperties(String themeId);
}
