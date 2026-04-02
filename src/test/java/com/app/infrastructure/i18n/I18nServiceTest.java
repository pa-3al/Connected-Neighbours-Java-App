package com.app.infrastructure.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class I18nServiceTest {

    private I18nService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = I18nService.getInstance();
        service.setResourcesRoot(tempDir);
    }

    @Test
    void testCreateNewLocale_shouldCreateFile() throws IOException {
        String lang = "es";
        service.createNewLocale(lang);
        
        Path expectedFile = tempDir.resolve("messages_es.properties");
        assertTrue(Files.exists(expectedFile), "Properties file should exist");
        
        String content = Files.readString(expectedFile);
        assertTrue(content.contains("app.name"), "File should contain default properties");
    }

    @Test
    void testDeleteLocale_shouldDeleteFile() throws IOException {
        String lang = "it";
        service.createNewLocale(lang);
        Path expectedFile = tempDir.resolve("messages_it.properties");
        assertTrue(Files.exists(expectedFile));
        
        service.deleteLocale(Locale.forLanguageTag(lang));
        assertFalse(Files.exists(expectedFile), "Properties file should be deleted");
    }

    @Test
    void testDeleteLocale_shouldThrowForSystemLocales() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteLocale(Locale.FRENCH));
        assertThrows(IllegalArgumentException.class, () -> service.deleteLocale(Locale.ENGLISH));
    }
}
