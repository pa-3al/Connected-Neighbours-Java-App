package com.app.infrastructure.adapter.theme;
import com.app.domain.model.Theme;
import com.app.domain.port.out.ThemeRepository;
import com.app.infrastructure.util.DailyLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Scene;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
public class JavaFXThemeAdapter implements ThemeRepository {
    private static final String THEMES_RESOURCE_PATH = "/themes/";
    private static final Path CUSTOM_THEMES_DIR = Paths.get("themes", "custom");
    private Scene scene;
    private final Map<String, Theme> themesMap = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThemePreferenceManager preferenceManager;
    private final ThemeExporter themeExporter;
    public JavaFXThemeAdapter() {
        this.preferenceManager = new ThemePreferenceManager();
        this.themeExporter = new ThemeExporter();
        loadBuiltInThemes();
        loadCustomThemes();
    }
    private void loadBuiltInThemes() {
        themesMap.put("default-dark", Theme.builtIn(
            "default-dark", 
            "🌙 Sombre (Défaut)", 
            THEMES_RESOURCE_PATH + "default.css"
        ));
        themesMap.put("light", Theme.builtIn(
            "light", 
            "☀️ Clair", 
            THEMES_RESOURCE_PATH + "light.css"
        ));
        themesMap.put("dracula", Theme.builtIn(
            "dracula",
            "🧛 Dracula",
            THEMES_RESOURCE_PATH + "dracula.css"
        ));
        themesMap.put("nord", Theme.builtIn(
            "nord",
            "❄️ Nord",
            THEMES_RESOURCE_PATH + "nord.css"
        ));
    }
    private void loadCustomThemes() {
        try {
            if (!Files.exists(CUSTOM_THEMES_DIR)) {
                Files.createDirectories(CUSTOM_THEMES_DIR);
                return;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(CUSTOM_THEMES_DIR)) {
                for (Path themeDir : stream) {
                    if (Files.isDirectory(themeDir)) {
                        Path manifestPath = themeDir.resolve("theme.json");
                        if (Files.exists(manifestPath)) {
                            try {
                                ThemeExporter.ThemeManifest manifest = objectMapper.readValue(
                                    manifestPath.toFile(), ThemeExporter.ThemeManifest.class);
                                Path cssPath = themeDir.resolve(manifest.cssFile);
                                if (Files.exists(cssPath)) {
                                    Theme theme = Theme.custom(
                                        manifest.id,
                                        manifest.name,
                                        manifest.author,
                                        manifest.description,
                                        cssPath.toUri().toString()
                                    );
                                    themesMap.put(manifest.id, theme);
                                }
                            } catch (IOException e) {
                                DailyLogger.logWarn("ThemeAdapter", "Failed to load theme manifest: " + manifestPath);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            DailyLogger.logError("ThemeAdapter", "Failed to scan custom themes", e);
        }
    }
    public void setScene(Scene scene) {
        this.scene = scene;
    }
    @Override
    public List<Theme> getAllThemes() {
        return new ArrayList<>(themesMap.values());
    }
    public void reloadCustomThemes() {
        themesMap.entrySet().removeIf(entry -> !entry.getValue().isBuiltIn());
        loadCustomThemes();
    }
    @Override
    public void reloadThemes() {
        reloadCustomThemes();
    }
    @Override
    public void applyTheme(String themeId) {
        if (scene == null) {
            DailyLogger.logWarn("ThemeAdapter", "Scene not set, cannot apply theme");
            return;
        }
        Theme theme = themesMap.get(themeId);
        if (theme == null) {
            DailyLogger.logWarn("ThemeAdapter", "Theme not found: " + themeId);
            return;
        }
        scene.getStylesheets().clear();
        String cssPath = theme.cssPath();
        if (theme.isBuiltIn()) {
            URL url = getClass().getResource(cssPath);
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            } else {
                DailyLogger.logWarn("ThemeAdapter", "Built-in theme CSS not found: " + cssPath);
            }
        } else {
            scene.getStylesheets().add(cssPath);
        }
        if (theme.isBuiltIn()) {
            applySidebarWidth(loadSidebarWidth());
            clearInlineStyles();
        } else {
            Map<String, String> props = getThemeProperties(themeId);
            if (props != null) {
                try {
                    if (props.containsKey("borderRadius")) {
                        applyRadius(Double.parseDouble(props.get("borderRadius")));
                    } else {
                        applyRadius(loadRadius());
                    }
                } catch (Exception e) { applyRadius(loadRadius()); }
                try {
                    if (props.containsKey("sidebarWidth")) {
                        applySidebarWidth(Double.parseDouble(props.get("sidebarWidth")));
                    } else {
                        applySidebarWidth(loadSidebarWidth());
                    }
                } catch (Exception e) { applySidebarWidth(loadSidebarWidth()); }
                try {
                    if (props.containsKey("sidebarColor")) {
                        applySidebarColor(props.get("sidebarColor"));
                    } else {
                        applySidebarColor(loadSidebarColor());
                    }
                } catch (Exception e) { applySidebarColor(loadSidebarColor()); }
            } else {
                applyRadius(loadRadius());
                applySidebarColor(loadSidebarColor());
                applySidebarWidth(loadSidebarWidth());
            }
        }
    }
    @Override
    public CompletableFuture<Theme> downloadTheme(String url, Path targetDir) {
        return themeExporter.downloadTheme(url, targetDir, themesMap);
    }
    @Override
    public void deleteTheme(String themeId) {
        Theme theme = themesMap.get(themeId);
        if (theme == null || theme.isBuiltIn()) {
            return;
        }
        try {
            Path themeDir = CUSTOM_THEMES_DIR.resolve(themeId);
            if (Files.exists(themeDir)) {
                Files.walk(themeDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            DailyLogger.logWarn("ThemeAdapter", "Failed to delete: " + path);
                        }
                    });
            }
            themesMap.remove(themeId);
        } catch (IOException e) {
            DailyLogger.logError("ThemeAdapter", "Failed to delete theme: " + themeId, e);
        }
    }
    @Override
    public void exportTheme(String themeId, Path targetPath) {
        themeExporter.exportTheme(themeId, targetPath, themesMap);
    }
    @Override
    public void savePreference(String themeId) {
        preferenceManager.savePreference(themeId);
    }
    @Override
    public String loadPreference() {
        return preferenceManager.loadPreference();
    }
    @Override
    public void saveRadius(double radius) {
        preferenceManager.saveRadius(radius);
        if (scene != null) {
            applyRadius(radius);
        }
    }
    @Override
    public void saveSidebarColor(String color) {
        preferenceManager.saveSidebarColor(color);
        if (scene != null) {
            applySidebarColor(color);
        }
    }
    @Override
    public String loadSidebarColor() {
        return preferenceManager.loadSidebarColor();
    }
    @Override
    public void saveSidebarWidth(double width) {
        preferenceManager.saveSidebarWidth(width);
        if (scene != null) {
            applySidebarWidth(width);
        }
    }
    @Override
    public double loadSidebarWidth() {
        return preferenceManager.loadSidebarWidth();
    }
    @Override
    public double loadRadius() {
        return preferenceManager.loadRadius();
    }
    private void clearInlineStyles() {
        if (scene == null || scene.getRoot() == null) return;
        String rootStyle = scene.getRoot().getStyle();
        if (rootStyle != null) {
            scene.getRoot().setStyle(rootStyle.replaceAll("-fx-global-radius: [^;]+;?", ""));
        }
        javafx.scene.Node sidebar = scene.lookup("#sidebar");
        if (sidebar != null) {
            String sidebarStyle = sidebar.getStyle();
            if (sidebarStyle != null) {
                sidebar.setStyle(sidebarStyle.replaceAll("-fx-background-color: [^;]+;?", ""));
            }
        }
    }
    private void applySidebarColor(String color) {
        if (scene == null || scene.getRoot() == null) return;
        javafx.scene.Node sidebar = scene.lookup("#sidebar");
        if (sidebar != null) {
            String style = sidebar.getStyle();
            style = style == null ? "" : style.replaceAll("-fx-background-color: [^;]+;?", "");
            sidebar.setStyle(style + "-fx-background-color: " + color + ";");
        }
    }
    private void applySidebarWidth(double width) {
        if (scene == null || scene.getRoot() == null) return;
        javafx.scene.Node sidebar = scene.lookup("#sidebar");
        if (sidebar instanceof javafx.scene.layout.Region) {
            ((javafx.scene.layout.Region) sidebar).setMinWidth(width);
            ((javafx.scene.layout.Region) sidebar).setPrefWidth(width);
            ((javafx.scene.layout.Region) sidebar).setMaxWidth(width);
        }
    }
    private void applyRadius(double radius) {
        if (scene != null && scene.getRoot() != null) {
            String style = scene.getRoot().getStyle();
            style = style == null ? "" : style.replaceAll("-fx-global-radius: [^;]+;?", "");
            scene.getRoot().setStyle(style + String.format("-fx-global-radius: %.0fpx;", radius));
        }
    }
    @Override
    public void importTheme(Path zipFile) {
        themeExporter.importTheme(zipFile, themesMap, this::reloadCustomThemes);
    }
    @Override
    public Map<String, String> getThemeProperties(String themeId) {
        try {
            Path manifestPath = CUSTOM_THEMES_DIR.resolve(themeId).resolve("theme.json");
            if (Files.exists(manifestPath)) {
                ThemeExporter.ThemeManifest manifest = objectMapper.readValue(
                    manifestPath.toFile(), ThemeExporter.ThemeManifest.class);
                return manifest.colors;
            }
        } catch (IOException e) {
            DailyLogger.logWarn("ThemeAdapter", "Failed to read theme properties: " + e.getMessage());
        }
        return null;
    }
}
