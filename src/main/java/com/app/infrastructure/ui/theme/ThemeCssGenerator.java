package com.app.infrastructure.ui.theme;

import javafx.scene.paint.Color;

public class ThemeCssGenerator {

    public String generate(ThemeColors c) {
        return String.format("""
            .root {
                -fx-base: %s;
                -fx-control-inner-background: %s;
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-accent: %s;
                -fx-focus-color: %s;
            }
            .label { -fx-text-fill: %s; -fx-font-size: 14px; }
            .section-title { -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .card-title { -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .text-muted { -fx-text-fill: %s; -fx-font-size: 12px; }
            .button {
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-background-radius: 6;
                -fx-padding: 8 16;
                -fx-cursor: hand;
            }
            .button:hover { -fx-background-color: %s; }
            .primary-button { -fx-background-color: %s; -fx-text-fill: white; }
            .primary-button:hover { -fx-background-color: %s; }
            .success-button { -fx-background-color: %s; -fx-text-fill: white; }
            .success-button:hover { -fx-background-color: %s; }
            .warning-button { -fx-background-color: %s; -fx-text-fill: white; }
            .warning-button:hover { -fx-background-color: %s; }
            .danger-button { -fx-background-color: %s; -fx-text-fill: white; }
            .danger-button:hover { -fx-background-color: %s; }
            .secondary-button { -fx-background-color: %s; -fx-text-fill: white; }
            .card {
                -fx-background-color: %s;
                -fx-background-radius: 10;
                -fx-padding: 15;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);
            }
            .info-card {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-border-width: 1;
            }
            .theme-card, .plugin-card {
                -fx-background-color: %s;
                -fx-background-radius: 10;
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-border-width: 1;
            }
            .theme-card:hover, .plugin-card:hover { -fx-border-color: %s; }
            .theme-card-active, .plugin-card-active { -fx-border-color: %s; -fx-border-width: 2; }
            .plugin-card-disabled { -fx-opacity: 0.6; }
            .theme-card-title, .plugin-card-title { -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .theme-card-author, .plugin-card-author { -fx-font-size: 12px; -fx-text-fill: %s; }
            .theme-card-description, .plugin-card-description { -fx-font-size: 13px; -fx-text-fill: %s; }
            .badge-builtin { -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; }
            .version-badge { -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px; }
            .status-badge { -fx-font-size: 12px; -fx-text-fill: %s; }
            .text-field {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-prompt-text-fill: %s;
                -fx-background-radius: 6;
                -fx-border-color: %s;
                -fx-border-radius: 6;
                -fx-padding: 8;
            }
            .text-field:focused { -fx-border-color: %s; }
            .text-area {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-family: 'Consolas', 'Monaco', monospace;
                -fx-font-size: 13px;
            }
            .text-area .content { -fx-background-color: %s; }
            .log-area { -fx-background-color: %s; }
            .log-area .content { -fx-background-color: %s; }
            .progress-bar { -fx-accent: %s; -fx-control-inner-background: %s; }
            .progress-bar .bar { -fx-background-color: linear-gradient(to right, %s, %s); -fx-background-radius: 5; }
            .progress-bar .track { -fx-background-color: %s; -fx-background-radius: 5; }
            .online-toggle { -fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 15; -fx-padding: 5 12; }
            .online-toggle:selected { -fx-background-color: %s; -fx-text-fill: white; }
            .scroll-pane, .scroll-pane .viewport { -fx-background-color: transparent; }
            .scroll-bar { -fx-background-color: transparent; }
            .scroll-bar .thumb { -fx-background-color: %s; -fx-background-radius: 4; }
            .scroll-bar .thumb:hover { -fx-background-color: %s; }
            .separator { -fx-background-color: %s; }
            .separator .line { -fx-border-color: %s; -fx-border-width: 1 0 0 0; }
            .list-view { -fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 6; }
            .list-cell { -fx-background-color: transparent; -fx-text-fill: %s; -fx-padding: 8; }
            .list-cell:filled:selected { -fx-background-color: %s; -fx-text-fill: white; }
            .list-cell:filled:hover { -fx-background-color: %s; }
            .status-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .current-theme-label { -fx-text-fill: %s; -fx-font-weight: bold; }
            .count-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .empty-message { -fx-text-fill: %s; -fx-font-size: 14px; -fx-text-alignment: center; }
            .themes-container, .plugins-container { -fx-padding: 10; }
            """,
                toHex(c.background()), toHex(c.cardBackground()), toHex(c.background()),
                toHex(c.primaryText()), toHex(c.buttonPrimary()), toHex(c.accent()),
                toHex(c.primaryText()), toHex(c.primaryText()), toHex(c.primaryText()), toHex(c.secondaryText()),
                toHex(c.buttonPrimary()), toHex(c.buttonPrimary().brighter()),
                toHex(c.accent()), toHex(c.accentHover()),
                toHex(c.buttonSuccess()), toHex(c.buttonSuccess().brighter()),
                toHex(c.buttonWarning()), toHex(c.buttonWarning().brighter()),
                toHex(c.buttonDanger()), toHex(c.buttonDanger().brighter()),
                toHex(c.borderColor()),
                toHex(c.cardBackground()),
                toHex(c.buttonPrimary()), toHex(c.accent()),
                toHex(c.cardBackground()), toHex(c.borderColor()), toHex(c.accent()), toHex(c.buttonSuccess()),
                toHex(c.primaryText()), toHex(c.secondaryText()), toHex(c.secondaryText()),
                toHex(c.borderColor()), toHex(c.secondaryText()), toHex(c.buttonPrimary()), toHex(c.accent()), toHex(c.secondaryText()),
                toHex(c.inputBackground()), toHex(c.primaryText()), toHex(c.secondaryText()), toHex(c.inputBorder()), toHex(c.accent()),
                toHex(c.inputBackground()), toHex(c.primaryText()), toHex(c.inputBackground()),
                toHex(c.background().darker()), toHex(c.background().darker()),
                toHex(c.accent()), toHex(c.buttonPrimary()), toHex(c.accent()), toHex(c.accentHover()), toHex(c.buttonPrimary()),
                toHex(c.cardBackground()), toHex(c.buttonSuccess()), toHex(c.buttonSuccess()),
                toHex(c.scrollbarColor()), toHex(c.scrollbarColor().brighter()),
                toHex(c.borderColor()), toHex(c.borderColor()),
                toHex(c.cardBackground()), toHex(c.borderColor()), toHex(c.primaryText()), toHex(c.buttonPrimary()), toHex(c.cardBackground().brighter()),
                toHex(c.secondaryText()), toHex(c.buttonSuccess()), toHex(c.secondaryText()), toHex(c.secondaryText())
        );
    }

    public String toHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }
}
