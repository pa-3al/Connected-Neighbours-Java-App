package com.app.infrastructure.adapter.theme;
import com.app.infrastructure.util.DailyLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
public class ThemePreferenceManager {
    private static final Path PREFERENCES_FILE = Paths.get("config", "theme-preference.json");
    private final ObjectMapper objectMapper;
    public ThemePreferenceManager() {
        this.objectMapper = new ObjectMapper();
    }
    public void savePreference(String themeId) {
        try {
            ensureConfigDirExists();
            Map<String, Object> prefs = loadPreferencesMap();
            prefs.put("themeId", themeId);
            objectMapper.writeValue(PREFERENCES_FILE.toFile(), prefs);
        } catch (IOException e) {
            DailyLogger.logWarn("ThemePrefs", "Failed to save theme preference: " + e.getMessage());
        }
    }
    public String loadPreference() {
        if (Files.exists(PREFERENCES_FILE)) {
            Map<String, Object> prefs = loadPreferencesMap();
            Object val = prefs.get("themeId");
            return val != null ? val.toString() : "default-dark";
        }
        return "default-dark";
    }
    public void saveRadius(double radius) {
        try {
            ensureConfigDirExists();
            Map<String, Object> prefs = loadPreferencesMap();
            prefs.put("radius", radius);
            objectMapper.writeValue(PREFERENCES_FILE.toFile(), prefs);
        } catch (IOException e) {
            DailyLogger.logWarn("ThemePrefs", "Failed to save radius preference: " + e.getMessage());
        }
    }
    public double loadRadius() {
        Map<String, Object> prefs = loadPreferencesMap();
        Object r = prefs.get("radius");
        if (r instanceof Number) {
            return ((Number) r).doubleValue();
        }
        return 10.0; 
    }
    public void saveSidebarColor(String color) {
        try {
            ensureConfigDirExists();
            Map<String, Object> prefs = loadPreferencesMap();
            prefs.put("sidebarColor", color);
            objectMapper.writeValue(PREFERENCES_FILE.toFile(), prefs);
        } catch (IOException e) {
            DailyLogger.logWarn("ThemePrefs", "Failed to save sidebar color: " + e.getMessage());
        }
    }
    public String loadSidebarColor() {
        Map<String, Object> prefs = loadPreferencesMap();
        Object val = prefs.get("sidebarColor");
        return val != null ? val.toString() : "#2b2b2b"; 
    }
    public void saveSidebarWidth(double width) {
        try {
            ensureConfigDirExists();
            Map<String, Object> prefs = loadPreferencesMap();
            prefs.put("sidebarWidth", width);
            objectMapper.writeValue(PREFERENCES_FILE.toFile(), prefs);
        } catch (IOException e) {
            DailyLogger.logWarn("ThemePrefs", "Failed to save sidebar width: " + e.getMessage());
        }
    }
    public double loadSidebarWidth() {
        Map<String, Object> prefs = loadPreferencesMap();
        Object val = prefs.get("sidebarWidth");
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 200.0; 
    }
    private void ensureConfigDirExists() throws IOException {
        Path configDir = PREFERENCES_FILE.getParent();
        if (configDir != null && !Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> loadPreferencesMap() {
        try {
            if (Files.exists(PREFERENCES_FILE)) {
                return objectMapper.readValue(PREFERENCES_FILE.toFile(), Map.class);
            }
        } catch (IOException e) {
            DailyLogger.logWarn("ThemePrefs", "Failed to load preferences: " + e.getMessage());
        }
        return new HashMap<>();
    }
}
