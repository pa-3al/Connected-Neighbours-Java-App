package com.app.infrastructure.ui.theme;

import java.util.Map;

import javafx.scene.paint.Color;

public class ThemeColorFactory {

    public static final double DEFAULT_BORDER_RADIUS = 10.0;
    public static final double EDIT_FALLBACK_BORDER_RADIUS = 0.0;
    public static final double DEFAULT_SIDEBAR_WIDTH = 250.0;
    private static final String DEFAULT_BACKGROUND = "#1a1a2e";
    private static final String DEFAULT_CARD_BACKGROUND = "#16213e";
    private static final String DEFAULT_PRIMARY_TEXT = "#e0e0e0";
    private static final String DEFAULT_SECONDARY_TEXT = "#888888";
    private static final String DEFAULT_ACCENT = "#e94560";
    private static final String DEFAULT_ACCENT_HOVER = "#ff6b7a";
    private static final String DEFAULT_BUTTON_PRIMARY = "#0f3460";
    private static final String DEFAULT_BUTTON_SUCCESS = "#16c79a";
    private static final String DEFAULT_BUTTON_WARNING = "#f39c12";
    private static final String DEFAULT_BUTTON_DANGER = "#e74c3c";
    private static final String DEFAULT_INPUT_BACKGROUND = "#0f3460";
    private static final String DEFAULT_INPUT_BORDER = "#3d4f6f";
    private static final String DEFAULT_BORDER_COLOR = "#3d4f6f";
    private static final String DEFAULT_SCROLLBAR_COLOR = "#3d4f6f";
    public static final String DEFAULT_SIDEBAR_COLOR = "#2b2b2b";

    public ThemeColors defaultForCreate() {
        return defaultThemeColors(DEFAULT_BORDER_RADIUS, DEFAULT_SIDEBAR_WIDTH, Color.web(DEFAULT_SIDEBAR_COLOR));
    }

    public ThemeColors defaultForEdit() {
        return defaultThemeColors(EDIT_FALLBACK_BORDER_RADIUS, DEFAULT_SIDEBAR_WIDTH, null);
    }

    public ThemeColors readThemeColors(Map<String, String> props, ThemeColors fallback) {
        return new ThemeColors(
                parseColor(props, "background", fallback.background()),
                parseColor(props, "cardBackground", fallback.cardBackground()),
                parseColor(props, "primaryText", fallback.primaryText()),
                parseColor(props, "secondaryText", fallback.secondaryText()),
                parseColor(props, "accent", fallback.accent()),
                parseColor(props, "accentHover", fallback.accentHover()),
                parseColor(props, "buttonPrimary", fallback.buttonPrimary()),
                parseColor(props, "buttonSuccess", fallback.buttonSuccess()),
                parseColor(props, "buttonWarning", fallback.buttonWarning()),
                parseColor(props, "buttonDanger", fallback.buttonDanger()),
                parseColor(props, "inputBackground", fallback.inputBackground()),
                parseColor(props, "inputBorder", fallback.inputBorder()),
                parseColor(props, "borderColor", fallback.borderColor()),
                parseColor(props, "scrollbarColor", fallback.scrollbarColor()),
                parseDouble(props, "borderRadius", fallback.borderRadius()),
                parseDouble(props, "sidebarWidth", fallback.sidebarWidth()),
                parseColor(props, "sidebarColor", fallback.sidebarColor())
        );
    }

    private ThemeColors defaultThemeColors(double borderRadius, double sidebarWidth, Color sidebarColor) {
        return new ThemeColors(
                Color.web(DEFAULT_BACKGROUND),
                Color.web(DEFAULT_CARD_BACKGROUND),
                Color.web(DEFAULT_PRIMARY_TEXT),
                Color.web(DEFAULT_SECONDARY_TEXT),
                Color.web(DEFAULT_ACCENT),
                Color.web(DEFAULT_ACCENT_HOVER),
                Color.web(DEFAULT_BUTTON_PRIMARY),
                Color.web(DEFAULT_BUTTON_SUCCESS),
                Color.web(DEFAULT_BUTTON_WARNING),
                Color.web(DEFAULT_BUTTON_DANGER),
                Color.web(DEFAULT_INPUT_BACKGROUND),
                Color.web(DEFAULT_INPUT_BORDER),
                Color.web(DEFAULT_BORDER_COLOR),
                Color.web(DEFAULT_SCROLLBAR_COLOR),
                borderRadius,
                sidebarWidth,
                sidebarColor
        );
    }

    private double parseDouble(Map<String, String> props, String key, double fallbackValue) {
        String rawValue = props.get(key);
        if (rawValue == null || rawValue.isBlank()) {
            return fallbackValue;
        }

        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    private Color parseColor(Map<String, String> props, String key, Color fallbackColor) {
        String rawValue = props.get(key);
        if (rawValue == null || rawValue.isBlank()) {
            return fallbackColor;
        }

        try {
            return Color.web(rawValue);
        } catch (IllegalArgumentException e) {
            return fallbackColor;
        }
    }
}
