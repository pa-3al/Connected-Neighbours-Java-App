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
        Color sidebarColor,
        Color chartColor1,
        Color chartColor2,
        Color chartColor3,
        Color chartColor4,
        Color chartColor5,
        Color chartColor6,
        Color chartColor7,
        Color chartColor8
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
        values.put("chartColor1", toHex(chartColor1));
        values.put("chartColor2", toHex(chartColor2));
        values.put("chartColor3", toHex(chartColor3));
        values.put("chartColor4", toHex(chartColor4));
        values.put("chartColor5", toHex(chartColor5));
        values.put("chartColor6", toHex(chartColor6));
        values.put("chartColor7", toHex(chartColor7));
        values.put("chartColor8", toHex(chartColor8));
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