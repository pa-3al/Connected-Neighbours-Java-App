package com.app.domain.service;
import com.app.domain.model.Theme;
import com.app.domain.port.in.ThemeUseCase;
import com.app.domain.port.out.ThemeRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
public class ThemeService implements ThemeUseCase {
    private static final Path THEMES_DIR = Paths.get("themes", "custom");
    private final ThemeRepository themeRepository;
    private Theme currentTheme;
    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
        List<Theme> themes = themeRepository.getAllThemes();
        if (!themes.isEmpty()) {
            String savedPref = themeRepository.loadPreference();
            currentTheme = themes.stream()
                .filter(t -> t.id().equals(savedPref))
                .findFirst()
                .orElse(themes.get(0));
        }
    }
    @Override
    public List<Theme> getAvailableThemes() {
        return themeRepository.getAllThemes();
    }
    @Override
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    @Override
    public void switchTheme(String themeId) {
        Theme theme = findThemeById(themeId);
        themeRepository.applyTheme(themeId);
        currentTheme = theme;
        themeRepository.savePreference(themeId);
    }
    @Override
    public CompletableFuture<Theme> downloadTheme(String url) {
        return themeRepository.downloadTheme(url, THEMES_DIR)
            .thenApply(theme -> {
                reloadThemes();
                return theme;
            });
    }
    @Override
    public void deleteTheme(String themeId) {
        Theme theme = findThemeById(themeId);
        if (theme.isBuiltIn()) {
            throw new IllegalArgumentException("Cannot delete built-in theme: " + themeId);
        }
        themeRepository.deleteTheme(themeId);
        if (currentTheme != null && currentTheme.id().equals(themeId)) {
            List<Theme> remaining = themeRepository.getAllThemes();
            if (!remaining.isEmpty()) {
                switchTheme(remaining.get(0).id());
            } else {
                currentTheme = null;
            }
        }
    }
    @Override
    public void exportTheme(String themeId, Path targetPath) {
        themeRepository.exportTheme(themeId, targetPath);
    }
    @Override
    public void importTheme(Path zipFile) {
        themeRepository.importTheme(zipFile);
    }
    @Override
    public void savePreference(String themeId) {
        themeRepository.savePreference(themeId);
    }
    @Override
    public void saveRadius(double radius) {
        themeRepository.saveRadius(radius);
    }
    @Override
    public double loadRadius() {
        return themeRepository.loadRadius();
    }
    @Override
    public void saveSidebarColor(String color) {
        themeRepository.saveSidebarColor(color);
    }
    @Override
    public String loadSidebarColor() {
        return themeRepository.loadSidebarColor();
    }
    @Override
    public void saveSidebarWidth(double width) {
        themeRepository.saveSidebarWidth(width);
    }
    @Override
    public double loadSidebarWidth() {
        return themeRepository.loadSidebarWidth();
    }
    @Override
    public Map<String, String> getThemeProperties(String themeId) {
        return themeRepository.getThemeProperties(themeId);
    }
    public void reloadThemes() {
        themeRepository.reloadThemes();
    }
    private Theme findThemeById(String themeId) {
        return themeRepository.getAllThemes().stream()
            .filter(t -> t.id().equals(themeId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));
    }
}
