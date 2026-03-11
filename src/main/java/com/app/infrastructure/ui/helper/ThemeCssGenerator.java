package com.app.infrastructure.ui.helper;

import javafx.scene.paint.Color;

public class ThemeCssGenerator {

    public static String generateCss(ThemeColors colors) {
        String bg = toHex(colors.background());
        String card = toHex(colors.cardBackground());
        String text = toHex(colors.primaryText());
        String muted = toHex(colors.secondaryText());
        String accent = toHex(colors.accent());
        String accentHover = toHex(colors.accentHover());
        String btnPrimary = toHex(colors.buttonPrimary());
        String btnSuccess = toHex(colors.buttonSuccess());
        String btnWarning = toHex(colors.buttonWarning());
        String btnDanger = toHex(colors.buttonDanger());
        String inputBg = toHex(colors.inputBackground());
        String inputBorder = toHex(colors.inputBorder());
        String border = toHex(colors.borderColor());
        String scrollbar = toHex(colors.scrollbarColor());

        return String.format("""
            .root {
                -fx-base: %s;
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-accent: %s;
                -fx-focus-color: %s;
            }
            
            .label { -fx-text-fill: %s; -fx-font-size: 14px; }
            .section-title { -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .text-muted { -fx-text-fill: %s; -fx-font-size: 12px; }

            .button { -fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; }
            .button:hover { -fx-background-color: %s; }
            .primary-button { -fx-background-color: %s; -fx-text-fill: white; }
            .success-button { -fx-background-color: %s; -fx-text-fill: white; }
            .warning-button { -fx-background-color: %s; -fx-text-fill: white; }
            .danger-button { -fx-background-color: %s; -fx-text-fill: white; }

            .card { -fx-background-color: %s; -fx-background-radius: 10; -fx-padding: 15; }
            .theme-card, .plugin-card { -fx-background-color: %s; -fx-background-radius: 10; -fx-border-color: %s; -fx-border-radius: 10; }
            .theme-card:hover, .plugin-card:hover { -fx-border-color: %s; }

            .text-field { -fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-background-radius: 6; -fx-border-radius: 6; }
            .text-field:focused { -fx-border-color: %s; }

            .scroll-bar .thumb { -fx-background-color: %s; -fx-background-radius: 4; }

            .progress-bar { -fx-accent: %s; }
            .progress-bar .bar { -fx-background-color: linear-gradient(to right, %s, %s); }
            """,
            bg, bg, text, accent, accent,
            text, text, muted,
            btnPrimary, accentHover, btnPrimary, btnSuccess, btnWarning, btnDanger,
            card, card, border, accent,
            inputBg, text, inputBorder, accent,
            scrollbar,
            accent, btnPrimary, accent
        );
    }

    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

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
    ) {}
}
