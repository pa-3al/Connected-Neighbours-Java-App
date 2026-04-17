package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    void builtInShouldCreateSystemTheme() {
        Theme theme = Theme.builtIn("light", "Light", "themes/light.css");

        assertEquals("light", theme.id());
        assertEquals("Light", theme.name());
        assertEquals("System", theme.author());
        assertEquals("Built-in theme", theme.description());
        assertEquals("themes/light.css", theme.cssPath());
        assertTrue(theme.isBuiltIn());
    }

    @Test
    void customShouldCreateNonBuiltInTheme() {
        Theme theme = Theme.custom("ocean", "Ocean", "Ada", "Blue palette", "themes/ocean.css");

        assertEquals("ocean", theme.id());
        assertEquals("Ocean", theme.name());
        assertEquals("Ada", theme.author());
        assertEquals("Blue palette", theme.description());
        assertEquals("themes/ocean.css", theme.cssPath());
        assertFalse(theme.isBuiltIn());
    }
}