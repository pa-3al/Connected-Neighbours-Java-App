package com.app.infrastructure.i18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class I18nService {

    private static final I18nService INSTANCE = new I18nService();
    private final ObjectProperty<java.util.Locale> locale = new SimpleObjectProperty<>(java.util.Locale.FRENCH);
    private final ObjectProperty<java.util.ResourceBundle> resources = new SimpleObjectProperty<>(); 

    private java.nio.file.Path resourcesRoot;

    private I18nService() {
        java.nio.file.Path current = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
        this.resourcesRoot = current.resolve("src/main/resources/com/app/i18n");

        locale.addListener((obs, oldLocale, newLocale) -> loadBundle(newLocale));
        loadBundle(locale.get());
    }

    public static I18nService getInstance() {
        return INSTANCE;
    }

    void setResourcesRoot(java.nio.file.Path root) {
        this.resourcesRoot = root;
    }

    private java.nio.file.Path resolvePath(java.util.Locale locale) {
        String filename = "messages_" + locale.getLanguage() + ".properties";
        return resourcesRoot.resolve(filename);
    }

    private void loadBundle(java.util.Locale locale) {
        java.util.ResourceBundle bundle;
        com.app.infrastructure.util.DailyLogger.logDebug("I18n", "Loading bundle for locale: " + locale);
        try {
            bundle = java.util.ResourceBundle.getBundle("com.app.i18n.messages", locale, new UTF8Control());
            com.app.infrastructure.util.DailyLogger.logDebug("I18n", "Loaded bundle: " + bundle.getBaseBundleName() + " (Locale: " + bundle.getLocale() + ")");
        } catch (Exception e) {
            com.app.infrastructure.util.DailyLogger.logError("I18n", "Error loading resource bundle", e);
            bundle = java.util.ResourceBundle.getBundle("com.app.i18n.messages", locale);
        }
        resources.set(bundle);
        com.app.infrastructure.util.DailyLogger.logDebug("I18n", "Resources property updated.");
    }
    
    private static class UTF8Control extends java.util.ResourceBundle.Control {
        @Override
        public java.util.ResourceBundle newBundle(String baseName, java.util.Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, java.io.IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            java.io.InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream != null) {
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    return new java.util.PropertyResourceBundle(reader);
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    }

    public String get(String key, Object... args) {
        try {
            String pattern;
            try {
                pattern = resources.get().getString(key);
            } catch (java.util.MissingResourceException missing) {
                pattern = java.util.ResourceBundle.getBundle("com.app.i18n.messages", java.util.Locale.FRENCH, new UTF8Control()).getString(key);
            }
            return java.text.MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public StringBinding createStringBinding(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), resources);
    }

    public StringBinding createStringBinding(java.util.concurrent.Callable<String> func) {
        return Bindings.createStringBinding(func, resources);
    }

    public java.util.ResourceBundle getBundle() {
        return resources.get();
    }

    public ObjectProperty<java.util.Locale> localeProperty() {
        return locale;
    }

    public void setLocale(java.util.Locale newLocale) {
        locale.set(newLocale);
    }

    public void updateTranslation(String key, String value) {
    }

    public void saveKeys(java.util.Locale locale, java.util.Map<String, String> keys) throws java.io.IOException {
        java.util.Properties props = new java.util.Properties();
        props.putAll(keys);

        java.nio.file.Path path = resolvePath(locale);
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(path)) {
            props.store(out, "Updated by Translation Plugin");
        }
        
        if (locale.equals(this.locale.get())) {
            loadBundle(locale);
        }
    }

    public void createNewLocale(String languageCode) throws java.io.IOException {
        java.util.Locale newLocale = java.util.Locale.forLanguageTag(languageCode.replace('_', '-'));
        java.nio.file.Path path = resolvePath(newLocale);
        if (!java.nio.file.Files.exists(path)) {
            java.nio.file.Files.createFile(path);
            java.util.Properties props = new java.util.Properties();
            props.setProperty("app.name", "New Language");
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(path)) {
                props.store(out, "Initial creation");
            }
        }
    }

    public java.util.List<java.util.Locale> getAvailableLocales() {
        java.util.List<java.util.Locale> locales = new java.util.ArrayList<>();
        locales.add(java.util.Locale.FRENCH);
        locales.add(java.util.Locale.ENGLISH);

        try {
            java.nio.file.Path current = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
            java.nio.file.Path resourcesPath = current.resolve("src/main/resources/com/app/i18n");
            
            if (java.nio.file.Files.exists(resourcesPath)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(resourcesPath)) {
                    stream.filter(p -> p.getFileName().toString().startsWith("messages_") && p.getFileName().toString().endsWith(".properties"))
                          .forEach(p -> {
                              String filename = p.getFileName().toString();
                              String code = filename.replace("messages_", "").replace(".properties", "");
                              java.util.Locale loc = java.util.Locale.forLanguageTag(code.replace('_', '-'));
                              if (!locales.contains(loc)) {
                                  locales.add(loc);
                              }
                          });
                }
            }
        } catch (Exception e) {
            com.app.infrastructure.util.DailyLogger.logWarn("I18n", "Error listing locales: " + e.getMessage());
        }
        return locales;
    }

    public void deleteLocale(java.util.Locale locale) throws java.io.IOException {
        if (locale.equals(java.util.Locale.FRENCH) || locale.equals(java.util.Locale.ENGLISH)) {
            throw new IllegalArgumentException("Cannot delete default languages (fr/en).");
        }
        
        java.nio.file.Path path = resolvePath(locale);
        if (java.nio.file.Files.exists(path)) {
            java.nio.file.Files.delete(path);
        }
        
        if (this.locale.get().equals(locale)) {
            setLocale(java.util.Locale.FRENCH);
        }
    }

    public void resetLocale(java.util.Locale locale) throws java.io.IOException {
        if (!locale.equals(java.util.Locale.FRENCH) && !locale.equals(java.util.Locale.ENGLISH)) {
            throw new IllegalArgumentException("Reset only available for system locales.");
        }
        
        java.util.Properties props = new java.util.Properties();
        
        if (locale.equals(java.util.Locale.FRENCH)) {
            restoreFrenchDefaults(props);
        } else {
            props.put("home.title", "Toucan");
        }
        
        java.nio.file.Path path = resolvePath(locale);
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(path)) {
            props.store(out, "Restored Factory Defaults");
        }
        
        if (locale.equals(this.locale.get())) {
            loadBundle(locale);
        }
    }

    private void restoreFrenchDefaults(java.util.Properties props) {
            props.put("home.title", "Toucan");
            props.put("home.subtitle", "Application Modulaire");
            props.put("nav.home", "\uD83C\uDFE0 Accueil");
            props.put("nav.plugins", "\uD83E\uDDE9 Plugins");
            props.put("nav.themes", "\uD83C\uDFA8 Thèmes");
            props.put("nav.query", "\uD83D\uDD0D Requêtes");
            props.put("nav.online", "En ligne");
            props.put("nav.header", "NAVIGATION");
            props.put("action.import", "Importer");
            props.put("action.export", "Exporter");
            props.put("action.clear", "Tout Effacer");
            props.put("action.file.import.title", "Importer une image");
            props.put("action.file.export.title", "Exporter le dessin");
            props.put("action.file.import.error", "Erreur Import");
            props.put("action.file.export.error", "Erreur Export");
            props.put("action.file.load.error.msg", "Impossible de charger l'image : {0}");
            props.put("action.file.save.error.msg", "Impossible de sauvegarder le fichier : {0}");
            props.put("settings.title", "Paramètres \u2699\uFE0F");
            props.put("settings.language", "Langue / Language \uD83C\uDF0D");
            props.put("settings.general", "Général");
            props.put("settings.about", "À propos");
            props.put("settings.online.active", "\uD83D\uDFE2 En ligne");
            props.put("settings.online.inactive", "\uD83D\uDD34 Hors ligne");
            props.put("settings.updates.title", "\uD83D\uDD04 Mises à jour");
            props.put("settings.updates.version.prefix", "Version actuelle : ");
            props.put("plugin.title", "\uD83E\uDDE9 Plugins");
            props.put("plugin.empty", "Aucun plugin installé.\nPlacez des fichiers .jar dans le dossier 'plugins/'.");
            props.put("plugin.add", "\u2795 Ajouter un plugin");
            props.put("plugin.refresh", "\uD83D\uDD04 Actualiser");
            props.put("plugin.status.enabled", "\uD83D\uDFE2 Actif");
            props.put("plugin.status.disabled", "\uD83D\uDD34 Désactivé");
            props.put("plugin.action.enable", "Activer");
            props.put("plugin.action.disable", "Désactiver");
            props.put("plugin.action.uninstall", "\uD83D\uDDD1 Désinstaller");
            props.put("plugin.translation.name", "Éditeur de Traductions");
            props.put("plugin.translation.tab", "Traductions");
            props.put("theme.title", "\uD83C\uDFA8 Thèmes");
            props.put("error.title", "Erreur");
    }
}
