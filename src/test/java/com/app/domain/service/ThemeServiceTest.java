package com.app.domain.service;
import com.app.domain.model.Theme;
import com.app.domain.port.out.ThemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
class ThemeServiceTest {
    private ThemeService themeService;
    private MockThemeRepository mockRepo;
    @BeforeEach
    void setUp() {
        mockRepo = new MockThemeRepository();
        themeService = new ThemeService(mockRepo);
    }
    @Test
    void testDeleteTheme_shouldThrow_whenBuiltIn() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> themeService.deleteTheme("default-dark")
        );
        assertTrue(ex.getMessage().contains("built-in"));
    }
    @Test
    void testDeleteTheme_shouldThrow_whenNotFound() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> themeService.deleteTheme("non-existent-theme")
        );
        assertTrue(ex.getMessage().contains("not found"));
    }
    @Test
    void testSwitchTheme_shouldUpdateCurrentTheme() {
        themeService.switchTheme("light");
        Theme current = themeService.getCurrentTheme();
        assertNotNull(current);
        assertEquals("light", current.id());
    }
    @Test
    void testSwitchTheme_shouldThrow_whenNotFound() {
        assertThrows(
            IllegalArgumentException.class,
            () -> themeService.switchTheme("non-existent-theme")
        );
    }
    @Test
    void testGetAvailableThemes_shouldReturnNonEmptyList() {
        List<Theme> themes = themeService.getAvailableThemes();
        assertNotNull(themes);
        assertFalse(themes.isEmpty());
    }
    @Test
    void testSavePreference_shouldCallRepository() {
        themeService.savePreference("nord");
        assertEquals("nord", mockRepo.savedThemeId);
    }
    private static class MockThemeRepository implements ThemeRepository {
        String savedThemeId;
        @Override
        public List<Theme> getAllThemes() {
            return List.of(
                Theme.builtIn("default-dark", "Sombre", "/themes/default.css"),
                Theme.builtIn("light", "Clair", "/themes/light.css"),
                Theme.builtIn("nord", "Nord", "/themes/nord.css")
            );
        }
        @Override
        public void applyTheme(String themeId) {
        }
        @Override
        public CompletableFuture<Theme> downloadTheme(String url, Path targetDir) {
            return CompletableFuture.completedFuture(null);
        }
        @Override
        public void deleteTheme(String themeId) {}
        @Override
        public void exportTheme(String themeId, Path targetPath) {}
        @Override
        public void savePreference(String themeId) {
            this.savedThemeId = themeId;
        }
        @Override
        public String loadPreference() {
            return "default-dark";
        }
        @Override
        public void saveRadius(double radius) {}
        @Override
        public double loadRadius() {
            return 10.0;
        }
        @Override
        public void saveSidebarColor(String color) {}
        @Override
        public String loadSidebarColor() {
            return "#2b2b2b";
        }
        @Override
        public void saveSidebarWidth(double width) {}
        @Override
        public double loadSidebarWidth() {
            return 200.0;
        }
        @Override
        public void reloadThemes() {}
        @Override
        public void importTheme(Path zipFile) {}
        @Override
        public Map<String, String> getThemeProperties(String themeId) {
            return null;
        }
    }
}
