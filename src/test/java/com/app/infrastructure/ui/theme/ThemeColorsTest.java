package com.app.infrastructure.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;

class ThemeColorsTest {

    @Test
    void toMapShouldConvertColorsAndNumericValues() {
        ThemeColors colors = themeColors(Color.rgb(1, 2, 3));

        Map<String, String> values = colors.toMap();

        assertEquals("#ff0000", values.get("background"));
        assertEquals("#00ff00", values.get("cardBackground"));
        assertEquals("8.0", values.get("borderRadius"));
        assertEquals("240.0", values.get("sidebarWidth"));
        assertEquals("#010203", values.get("sidebarColor"));
        assertEquals("#151617", values.get("chartColor8"));
    }

    @Test
    void toMapShouldOmitSidebarColorWhenMissing() {
        ThemeColors colors = themeColors(null);

        assertFalse(colors.toMap().containsKey("sidebarColor"));
    }

    private ThemeColors themeColors(Color sidebarColor) {
        return new ThemeColors(
                Color.rgb(255, 0, 0),
                Color.rgb(0, 255, 0),
                Color.rgb(0, 0, 255),
                Color.rgb(10, 20, 30),
                Color.rgb(40, 50, 60),
                Color.rgb(70, 80, 90),
                Color.rgb(100, 110, 120),
                Color.rgb(130, 140, 150),
                Color.rgb(160, 170, 180),
                Color.rgb(190, 200, 210),
                Color.rgb(220, 230, 240),
                Color.rgb(11, 12, 13),
                Color.rgb(14, 15, 16),
                Color.rgb(17, 18, 19),
                8,
                240,
                sidebarColor,
                Color.rgb(21, 22, 10),
                Color.rgb(21, 22, 11),
                Color.rgb(21, 22, 12),
                Color.rgb(21, 22, 13),
                Color.rgb(21, 22, 14),
                Color.rgb(21, 22, 15),
                Color.rgb(21, 22, 16),
                Color.rgb(21, 22, 23)
        );
    }
}
