package com.app.infrastructure.ui.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.paint.Color;

class ThemeCssGeneratorTest {

    @Test
    void toHexShouldConvertColorToLowercaseHex() {
        assertEquals("#0a141e", ThemeCssGenerator.toHex(Color.rgb(10, 20, 30)));
    }

    @Test
    void generateCssShouldIncludeMainColors() {
        String css = ThemeCssGenerator.generateCss(themeColors());

        assertTrue(css.contains("-fx-base: #ff0000;"));
        assertTrue(css.contains("-fx-background-color: #ff0000;"));
        assertTrue(css.contains("-fx-text-fill: #0000ff;"));
        assertTrue(css.contains(".button { -fx-background-color: #646e78;"));
        assertTrue(css.contains(".scroll-bar .thumb { -fx-background-color: #111213;"));
    }

    private ThemeCssGenerator.ThemeColors themeColors() {
        return new ThemeCssGenerator.ThemeColors(
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
                Color.rgb(1, 2, 3)
        );
    }
}
