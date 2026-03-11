package com.app.infrastructure.config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class ConfigProvider {
    private final Properties properties = new Properties();
    public ConfigProvider() {
        loadProperties();
    }
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                com.app.infrastructure.adapter.logging.InfrastructureLogger.logWarn("ConfigProvider", "No application.properties found, utilizing defaults.");
            }
        } catch (IOException e) {
            com.app.infrastructure.adapter.logging.InfrastructureLogger.logWarn("Config", "Config load failed, using defaults");
        }
    }
    public String getUpdateCheckUrl() {
        return properties.getProperty("app.update.checkUrl", "http://localhost:8000/api/updates/latest");
    }
    public int getUpdateTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.update.timeout", "30"));
    }
    public String getPluginsPath() {
        return properties.getProperty("app.plugins.path", "plugins");
    }

    public String getThemesPath() {
        return properties.getProperty("app.themes.path", "themes");
    }

    public String getCustomThemesPath() {
        return properties.getProperty("app.themes.custom.path", "themes/custom");
    }

    public String getConfigPath() {
        return properties.getProperty("app.config.path", "config");
    }

    public String getPluginStatePath() {
        return properties.getProperty("app.plugins.state.path", "config/plugins-state.json");
    }

    public long getMaxPluginSizeBytes() {
        return Long.parseLong(properties.getProperty("app.plugins.max.size", "10000000"));
    }
}

