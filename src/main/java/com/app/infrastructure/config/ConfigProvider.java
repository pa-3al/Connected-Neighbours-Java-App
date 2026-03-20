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
                com.app.infrastructure.util.DailyLogger.logWarn("ConfigProvider", "No application.properties found, utilizing defaults.");
            }
        } catch (IOException e) {
            com.app.infrastructure.util.DailyLogger.logWarn("Config", "Config load failed, using defaults");
        }
    }
    public String getUpdateCheckUrl() {
        return properties.getProperty("app.update.checkUrl", "http://localhost:8000/api/updates/latest");
    }
    public int getUpdateTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.update.timeout", "30"));
    }

    public String getAuthBaseUrl() {
        return properties.getProperty("app.auth.baseUrl", "http://localhost:3000");
    }

    public String getAdminLoginPath() {
        return properties.getProperty("app.auth.admin.loginPath", "/admin/auth/login");
    }

    public String getAdminLogin2FAPath() {
        return properties.getProperty("app.auth.admin.login2faPath", "/admin/auth/login-2fa");
    }

    public String getAdminSsoAuthorizePath() {
        return properties.getProperty("app.auth.admin.ssoAuthorizePath", "/admin/auth/desktop/sso");
    }

    public int getAuthTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.auth.timeout", "15"));
    }

    public int getSsoTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.auth.sso.timeout", "180"));
    }

    public boolean isAuthBypassEnabled() {
        return Boolean.parseBoolean(properties.getProperty("app.auth.bypass.enabled", "false"));
    }

    public String getAuthBypassToken() {
        return properties.getProperty("app.auth.bypass.token", "local-dev-token");
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
