package com.app.infrastructure.ui.theme;

import javafx.scene.paint.Color;

public class ThemeCssGenerator {

    public String generate(ThemeColors c) {
        String background = toHex(c.background());
        String backgroundDark = toHex(c.background().darker());
        String cardBackground = toHex(c.cardBackground());
        String cardBackgroundHover = toHex(c.cardBackground().brighter());
        String primaryText = toHex(c.primaryText());
        String secondaryText = toHex(c.secondaryText());
        String accent = toHex(c.accent());
        String accentHover = toHex(c.accentHover());
        String buttonPrimary = toHex(c.buttonPrimary());
        String buttonPrimaryHover = toHex(c.buttonPrimary().brighter());
        String buttonSuccess = toHex(c.buttonSuccess());
        String buttonSuccessHover = toHex(c.buttonSuccess().brighter());
        String buttonWarning = toHex(c.buttonWarning());
        String buttonWarningHover = toHex(c.buttonWarning().brighter());
        String buttonDanger = toHex(c.buttonDanger());
        String buttonDangerHover = toHex(c.buttonDanger().brighter());
        String inputBackground = toHex(c.inputBackground());
        String inputBorder = toHex(c.inputBorder());
        String borderColor = toHex(c.borderColor());
        String scrollbarColor = toHex(c.scrollbarColor());
        String scrollbarHover = toHex(c.scrollbarColor().brighter());
        String sidebarColor = c.sidebarColor() != null ? toHex(c.sidebarColor()) : cardBackground;
        String chartColor1 = toHex(c.chartColor1());
        String chartColor2 = toHex(c.chartColor2());
        String chartColor3 = toHex(c.chartColor3());
        String chartColor4 = toHex(c.chartColor4());
        String chartColor5 = toHex(c.chartColor5());
        String chartColor6 = toHex(c.chartColor6());
        String chartColor7 = toHex(c.chartColor7());
        String chartColor8 = toHex(c.chartColor8());
        String radius = String.format("%.0f", c.borderRadius());
        String sidebarWidth = String.format("%.0f", c.sidebarWidth());

        String css = String.format("""
            .root {
                -fx-font-family: 'Poppins', 'Segoe UI', 'Segoe UI Emoji', 'Apple Color Emoji', system-ui, sans-serif;
                -fx-font-size: 14px;
                -fx-base: %s;
                -fx-background: %s;
                -fx-control-inner-background: %s;
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-accent: %s;
                -fx-focus-color: %s;
                -fx-faint-focus-color: transparent;
                -fx-global-radius: %spx;
                -fx-sidebar-width: %spx;
                -fx-sidebar-color: %s;
                -fx-color-primary: %s;
                -fx-color-primary-hover: %s;
                -fx-color-secondary: %s;
                -fx-color-surface: %s;
                -fx-color-surface-elevated: %s;
                -fx-color-border: %s;
                -fx-color-text: %s;
                -fx-color-text-secondary: %s;
                -fx-color-text-muted: %s;
                -fx-color-success: %s;
                -fx-color-warning: %s;
                -fx-color-error: %s;
                -fx-color-info: %s;
                CHART_COLOR_1: %s;
                CHART_COLOR_2: %s;
                CHART_COLOR_3: %s;
                CHART_COLOR_4: %s;
                CHART_COLOR_5: %s;
                CHART_COLOR_6: %s;
                CHART_COLOR_7: %s;
                CHART_COLOR_8: %s;
                -fx-chart-color-1: CHART_COLOR_1;
                -fx-chart-color-2: CHART_COLOR_2;
                -fx-chart-color-3: CHART_COLOR_3;
                -fx-chart-color-4: CHART_COLOR_4;
                -fx-chart-color-5: CHART_COLOR_5;
                -fx-chart-color-6: CHART_COLOR_6;
                -fx-chart-color-7: CHART_COLOR_7;
                -fx-chart-color-8: CHART_COLOR_8;
            }

            .label { -fx-text-fill: %s; -fx-font-size: 14px; }
            .section-title { -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .card-title { -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s; }
            .text-muted { -fx-text-fill: %s; -fx-font-size: 12px; }

            .button {
                -fx-background-color: %s;
                -fx-text-fill: white;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
                -fx-border-color: transparent;
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
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
                -fx-border-color: %s;
                -fx-border-width: 1;
                -fx-padding: 15;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);
            }
            .info-card {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
                -fx-border-width: 1;
            }
            .theme-card, .plugin-card {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
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

            .text-field, .password-field {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-prompt-text-fill: %s;
                -fx-background-radius: %spx;
                -fx-border-color: %s;
                -fx-border-radius: %spx;
                -fx-padding: 8;
            }
            .text-field:focused, .password-field:focused { -fx-border-color: %s; }
            .text-area {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-family: 'Consolas', 'Monaco', monospace;
                -fx-font-size: 13px;
                -fx-background-radius: %spx;
                -fx-border-color: %s;
                -fx-border-radius: %spx;
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
            .scroll-bar .track { -fx-background-color: transparent; }
            .scroll-bar .thumb { -fx-background-color: %s; -fx-background-radius: 4; }
            .scroll-bar .thumb:hover { -fx-background-color: %s; }
            .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }
            .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: ""; -fx-padding: 0; }

            .separator { -fx-background-color: %s; }
            .separator .line { -fx-border-color: %s; -fx-border-width: 1 0 0 0; }
            .list-view {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
            }
            .list-cell { -fx-background-color: transparent; -fx-text-fill: %s; -fx-padding: 8; }
            .list-cell:filled:selected { -fx-background-color: %s; -fx-text-fill: white; }
            .list-cell:filled:hover { -fx-background-color: %s; }

            .combo-box, .color-picker {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
            }
            .combo-box:focused, .color-picker:focused { -fx-border-color: %s; }
            .combo-box .list-cell, .color-picker .label { -fx-text-fill: %s; }
            .combo-box-popup .list-view {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
            }

            .tab-pane .tab-header-area, .tab-pane .tab-header-background { -fx-background-color: transparent; }
            .tab { -fx-background-color: transparent; -fx-padding: 10 16; }
            .tab .tab-label { -fx-text-fill: %s; }
            .tab:selected .tab-label { -fx-text-fill: %s; -fx-font-weight: bold; }
            .table-view {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-background-radius: %spx;
                -fx-border-radius: %spx;
            }
            .table-view .column-header-background { -fx-background-color: %s; }
            .table-view .column-header, .table-view .column-header .label { -fx-background-color: transparent; -fx-text-fill: %s; }
            .table-row-cell { -fx-background-color: transparent; -fx-border-color: %s; -fx-border-width: 0 0 1 0; }
            .table-row-cell:hover { -fx-background-color: %s; }
            .table-row-cell:selected { -fx-background-color: %s; }
            .table-cell { -fx-text-fill: %s; }

            .status-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .current-theme-label { -fx-text-fill: %s; -fx-font-weight: bold; }
            .count-label { -fx-text-fill: %s; -fx-font-size: 13px; }
            .empty-message { -fx-text-fill: %s; -fx-font-size: 14px; -fx-text-alignment: center; }
            .themes-container, .plugins-container { -fx-padding: 10; }

            .chart { -fx-padding: 10; }
            .chart-content { -fx-padding: 10; }
            .chart-title { -fx-text-fill: %s; }
            .axis { -fx-tick-label-fill: %s; -fx-tick-mark-stroke: %s; }
            .axis-label { -fx-text-fill: %s; }
            .chart-legend { -fx-background-color: transparent; -fx-text-fill: %s; }
            .chart-legend-item { -fx-text-fill: %s; }
            .chart-plot-background { -fx-background-color: transparent; }
            .chart-vertical-grid-lines { -fx-stroke: %s; }
            .chart-horizontal-grid-lines { -fx-stroke: %s; }
            .chart-pie-label { -fx-fill: %s; }
            .chart-pie-label-line { -fx-stroke: %s; }
            .default-color0.chart-bar { -fx-bar-fill: CHART_COLOR_1; }
            .default-color1.chart-bar { -fx-bar-fill: CHART_COLOR_2; }
            .default-color2.chart-bar { -fx-bar-fill: CHART_COLOR_3; }
            .default-color3.chart-bar { -fx-bar-fill: CHART_COLOR_4; }
            .default-color4.chart-bar { -fx-bar-fill: CHART_COLOR_5; }
            .default-color5.chart-bar { -fx-bar-fill: CHART_COLOR_6; }
            .default-color6.chart-bar { -fx-bar-fill: CHART_COLOR_7; }
            .default-color7.chart-bar { -fx-bar-fill: CHART_COLOR_8; }
            .default-color0.chart-series-line { -fx-stroke: CHART_COLOR_1; }
            .default-color1.chart-series-line { -fx-stroke: CHART_COLOR_2; }
            .default-color2.chart-series-line { -fx-stroke: CHART_COLOR_3; }
            .default-color3.chart-series-line { -fx-stroke: CHART_COLOR_4; }
            .default-color4.chart-series-line { -fx-stroke: CHART_COLOR_5; }
            .default-color5.chart-series-line { -fx-stroke: CHART_COLOR_6; }
            .default-color6.chart-series-line { -fx-stroke: CHART_COLOR_7; }
            .default-color7.chart-series-line { -fx-stroke: CHART_COLOR_8; }
            .default-color0.chart-line-symbol { -fx-background-color: CHART_COLOR_1, -fx-background; }
            .default-color1.chart-line-symbol { -fx-background-color: CHART_COLOR_2, -fx-background; }
            .default-color2.chart-line-symbol { -fx-background-color: CHART_COLOR_3, -fx-background; }
            .default-color3.chart-line-symbol { -fx-background-color: CHART_COLOR_4, -fx-background; }
            .default-color4.chart-line-symbol { -fx-background-color: CHART_COLOR_5, -fx-background; }
            .default-color5.chart-line-symbol { -fx-background-color: CHART_COLOR_6, -fx-background; }
            .default-color6.chart-line-symbol { -fx-background-color: CHART_COLOR_7, -fx-background; }
            .default-color7.chart-line-symbol { -fx-background-color: CHART_COLOR_8, -fx-background; }
            .default-color0.chart-series-area-line { -fx-stroke: CHART_COLOR_1; }
            .default-color1.chart-series-area-line { -fx-stroke: CHART_COLOR_2; }
            .default-color2.chart-series-area-line { -fx-stroke: CHART_COLOR_3; }
            .default-color3.chart-series-area-line { -fx-stroke: CHART_COLOR_4; }
            .default-color4.chart-series-area-line { -fx-stroke: CHART_COLOR_5; }
            .default-color5.chart-series-area-line { -fx-stroke: CHART_COLOR_6; }
            .default-color6.chart-series-area-line { -fx-stroke: CHART_COLOR_7; }
            .default-color7.chart-series-area-line { -fx-stroke: CHART_COLOR_8; }
            .default-color0.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_1, 80%%); }
            .default-color1.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_2, 80%%); }
            .default-color2.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_3, 80%%); }
            .default-color3.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_4, 80%%); }
            .default-color4.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_5, 80%%); }
            .default-color5.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_6, 80%%); }
            .default-color6.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_7, 80%%); }
            .default-color7.chart-series-area-fill { -fx-fill: derive(CHART_COLOR_8, 80%%); }
            .default-color0.chart-bubble { -fx-bubble-fill: CHART_COLOR_1; }
            .default-color1.chart-bubble { -fx-bubble-fill: CHART_COLOR_2; }
            .default-color2.chart-bubble { -fx-bubble-fill: CHART_COLOR_3; }
            .default-color3.chart-bubble { -fx-bubble-fill: CHART_COLOR_4; }
            .default-color4.chart-bubble { -fx-bubble-fill: CHART_COLOR_5; }
            .default-color5.chart-bubble { -fx-bubble-fill: CHART_COLOR_6; }
            .default-color6.chart-bubble { -fx-bubble-fill: CHART_COLOR_7; }
            .default-color7.chart-bubble { -fx-bubble-fill: CHART_COLOR_8; }
            .default-color0.chart-pie { -fx-pie-color: CHART_COLOR_1; }
            .default-color1.chart-pie { -fx-pie-color: CHART_COLOR_2; }
            .default-color2.chart-pie { -fx-pie-color: CHART_COLOR_3; }
            .default-color3.chart-pie { -fx-pie-color: CHART_COLOR_4; }
            .default-color4.chart-pie { -fx-pie-color: CHART_COLOR_5; }
            .default-color5.chart-pie { -fx-pie-color: CHART_COLOR_6; }
            .default-color6.chart-pie { -fx-pie-color: CHART_COLOR_7; }
            .default-color7.chart-pie { -fx-pie-color: CHART_COLOR_8; }
            .bar-legend-symbol.default-color0, .pie-legend-symbol.default-color0 { -fx-background-color: CHART_COLOR_1; }
            .bar-legend-symbol.default-color1, .pie-legend-symbol.default-color1 { -fx-background-color: CHART_COLOR_2; }
            .bar-legend-symbol.default-color2, .pie-legend-symbol.default-color2 { -fx-background-color: CHART_COLOR_3; }
            .bar-legend-symbol.default-color3, .pie-legend-symbol.default-color3 { -fx-background-color: CHART_COLOR_4; }
            .bar-legend-symbol.default-color4, .pie-legend-symbol.default-color4 { -fx-background-color: CHART_COLOR_5; }
            .bar-legend-symbol.default-color5, .pie-legend-symbol.default-color5 { -fx-background-color: CHART_COLOR_6; }
            .bar-legend-symbol.default-color6, .pie-legend-symbol.default-color6 { -fx-background-color: CHART_COLOR_7; }
            .bar-legend-symbol.default-color7, .pie-legend-symbol.default-color7 { -fx-background-color: CHART_COLOR_8; }
            """,
                background, background, cardBackground, background,
                primaryText, buttonPrimary, accent, radius, sidebarWidth, sidebarColor,
                buttonPrimary, accentHover, secondaryText, cardBackground, cardBackgroundHover,
                borderColor, primaryText, secondaryText, secondaryText, buttonSuccess,
                buttonWarning, buttonDanger, accent,
                chartColor1, chartColor2, chartColor3, chartColor4,
                chartColor5, chartColor6, chartColor7, chartColor8,
                primaryText, primaryText, primaryText, secondaryText,
                buttonPrimary, radius, radius, buttonPrimaryHover,
                accent, accentHover,
                buttonSuccess, buttonSuccessHover,
                buttonWarning, buttonWarningHover,
                buttonDanger, buttonDangerHover,
                borderColor,
                cardBackground, radius, radius, borderColor,
                buttonPrimary, accent, radius, radius,
                cardBackground, borderColor, radius, radius, accent, buttonSuccess,
                primaryText, secondaryText, secondaryText,
                borderColor, secondaryText, buttonPrimary, accent, secondaryText,
                inputBackground, primaryText, secondaryText, radius, inputBorder, radius, accent,
                inputBackground, primaryText, radius, inputBorder, radius, inputBackground,
                backgroundDark, backgroundDark,
                accent, buttonPrimary, accent, accentHover, buttonPrimary,
                cardBackground, buttonSuccess, buttonSuccess,
                scrollbarColor, scrollbarHover,
                borderColor, borderColor,
                cardBackground, borderColor, radius, radius, primaryText, buttonPrimary, cardBackgroundHover,
                cardBackground, borderColor, radius, radius, accent, primaryText,
                cardBackground, borderColor, radius, radius,
                secondaryText, primaryText,
                cardBackground, borderColor, radius, radius, background, secondaryText, borderColor,
                cardBackgroundHover, buttonPrimary, primaryText,
                secondaryText, buttonSuccess, secondaryText, secondaryText,
                primaryText, secondaryText, borderColor, primaryText,
                primaryText, primaryText, borderColor, borderColor,
                primaryText, borderColor
        );
        return css + String.format("""

            .dialog-pane {
                -fx-background-color: %s;
                -fx-background-radius: %spx;
                -fx-border-color: %s;
                -fx-border-radius: %spx;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 30, 0, 0, 8);
            }
            .dialog-pane .header-panel {
                -fx-background-color: %s;
                -fx-background-radius: %spx %spx 0 0;
            }
            .dialog-pane .button-bar {
                -fx-background-color: %s;
                -fx-background-radius: 0 0 %spx %spx;
            }
            """,
                cardBackground, radius, borderColor, radius,
                background, radius, radius,
                backgroundDark, radius, radius
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
