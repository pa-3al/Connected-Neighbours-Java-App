package com.app.infrastructure.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;

class ThemeCssGeneratorTest {

    @Test
    void toHexShouldConvertColorToLowercaseHex() {
        ThemeCssGenerator generator = new ThemeCssGenerator();

        assertEquals("#0a141e", generator.toHex(Color.rgb(10, 20, 30)));
    }

    @Test
    void generateShouldIncludeMainThemeVariables() {
        ThemeCssGenerator generator = new ThemeCssGenerator();

        String css = generator.generate(themeColors(Color.rgb(1, 2, 3)));

        assertTrue(css.contains("-fx-global-radius: 8px;"));
        assertTrue(css.contains("-fx-sidebar-width: 240px;"));
        assertTrue(css.contains("-fx-sidebar-color: #010203;"));
        assertTrue(css.contains("-fx-color-primary: #646e78;"));
        assertTrue(css.contains("CHART_COLOR_8: #151617;"));
    }

    @Test
    void generateShouldFallbackToCardBackgroundForMissingSidebarColor() {
        ThemeCssGenerator generator = new ThemeCssGenerator();

        String css = generator.generate(themeColors(null));

        assertTrue(css.contains("-fx-sidebar-color: #00ff00;"));
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
