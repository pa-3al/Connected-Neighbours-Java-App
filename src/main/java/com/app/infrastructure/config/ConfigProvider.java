package com.app.infrastructure.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProvider {
    private final Properties properties = new Properties();

    public ConfigProvider() {
        loadProperties();
    }

    private File getExternalConfigFile() {
        File configDir = new File(getConfigPath());
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return new File(configDir, "application.properties");
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

        File externalFile = getExternalConfigFile();
        if (externalFile.exists()) {
            try (InputStream input = new FileInputStream(externalFile)) {
                properties.load(input);
            } catch (IOException e) {
                com.app.infrastructure.util.DailyLogger.logWarn("Config", "Failed to load external config");
            }
        }
    }

    public void saveDatabaseConfig(String url, String user, String password) {
        properties.setProperty("app.db.url", url);
        properties.setProperty("app.db.user", user);
        properties.setProperty("app.db.password", password);

        File externalFile = getExternalConfigFile();
        try (FileOutputStream out = new FileOutputStream(externalFile)) {
            properties.store(out, "");
        } catch (IOException e) {
            com.app.infrastructure.util.DailyLogger.logError("Config", "Failed to save config", e);
        }
    }

    public void saveSyncDatabaseConfig(String url) {
        Properties props = new Properties();
        File configFile = new File("src/main/resources/application.properties");

        try {
            if (configFile.exists()) {
                try (FileInputStream in = new FileInputStream(configFile)) {
                    props.load(in);
                }
            }

            props.setProperty("app.sync.db.url", url);

            try (FileOutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "Mise à jour via l'interface des paramètres");
            }

            System.out.println("Configuration enregistrée dans : " + configFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUpdateCheckUrl() {
        return properties.getProperty("app.update.checkUrl", "http://localhost:8000/api/updates/latest");
    }

    public int getUpdateTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.update.timeout", "30"));
    }

    public String getSyncUrl() {
        return properties.getProperty("app.sync.url", "https://test-backend.remythibaut.fr/sync");
    }

    public String getSyncDatabaseUrl() {
        return properties.getProperty("app.sync.db.url", "");
    }

    public String getSyncDatabaseTable() {
        return properties.getProperty("app.sync.db.table", "").trim();
    }

    public int getSyncTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("app.sync.timeout", "20"));
    }

    public String getAuthBaseUrl() {
        return properties.getProperty("app.auth.baseUrl", "https://test-backend.remythibaut.fr");
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

    public String getDatabaseUrl() {
        return properties.getProperty("app.db.url", "jdbc:sqlite:./data/neighborhood.db");
    }

    public String getDatabaseUser() {
        return properties.getProperty("app.db.user", "");
    }

    public String getDatabasePassword() {
        return properties.getProperty("app.db.password", "");
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