package com.app.infrastructure.ui.theme;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

public record ThemeColors(
        Color background,
        Color cardBackground,
        Color primaryText,
        Color secondaryText,
        Color accent,
        Color accentHover,
        Color buttonPrimary,
        Color buttonSuccess,
        Color buttonWarning,
        Color buttonDanger,
        Color inputBackground,
        Color inputBorder,
        Color borderColor,
        Color scrollbarColor,
        double borderRadius,
        double sidebarWidth,
        Color sidebarColor
) {
    public Map<String, String> toMap() {
        Map<String, String> values = new HashMap<>();
        values.put("background", toHex(background));
        values.put("cardBackground", toHex(cardBackground));
        values.put("primaryText", toHex(primaryText));
        values.put("secondaryText", toHex(secondaryText));
        values.put("accent", toHex(accent));
        values.put("accentHover", toHex(accentHover));
        values.put("buttonPrimary", toHex(buttonPrimary));
        values.put("buttonSuccess", toHex(buttonSuccess));
        values.put("buttonWarning", toHex(buttonWarning));
        values.put("buttonDanger", toHex(buttonDanger));
        values.put("inputBackground", toHex(inputBackground));
        values.put("inputBorder", toHex(inputBorder));
        values.put("borderColor", toHex(borderColor));
        values.put("scrollbarColor", toHex(scrollbarColor));
        values.put("borderRadius", String.valueOf(borderRadius));
        values.put("sidebarWidth", String.valueOf(sidebarWidth));
        if (sidebarColor != null) {
            values.put("sidebarColor", toHex(sidebarColor));
        }
        return values;
    }

    private String toHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }
}
