package com.app.infrastructure.adapter.plugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.app.domain.model.PluginMetadata;
import com.app.domain.port.out.PluginRepository;
import com.app.infrastructure.util.DailyLogger;
import com.app.plugin.Plugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
public class FileSystemPluginAdapter implements PluginRepository {
    private static final String PLUGIN_MANIFEST = "plugin.json";
    private final Path pluginsDir;
    private final Path stateFilePath;
    private final long maxJarSizeBytes;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, URLClassLoader> classLoaders = new HashMap<>();
    public FileSystemPluginAdapter(String pluginsPath, String statePath, long maxJarSizeBytes) {
        this.pluginsDir = Path.of(pluginsPath);
        this.stateFilePath = Path.of(statePath);
        this.maxJarSizeBytes = maxJarSizeBytes;
        ensureDirectoryExists();
    }
    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
            }
        } catch (IOException e) {
            DailyLogger.logWarn("PluginAdapter", "Plugin dir creation failed: " + e.getMessage());
        }
    }
    @Override
    public List<PluginMetadata> discoverAndLoadPlugins() {
        List<PluginMetadata> plugins = new ArrayList<>();
        File[] jarFiles = pluginsDir.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return plugins;
        }
        for (File jarFile : jarFiles) {
            PluginMetadata meta = scanJar(jarFile);
            if (meta != null) {
                plugins.add(meta);
            }
        }
        return plugins;
    }
    @Override
    public List<PluginMetadata> getInstalledPlugins() {
        return discoverAndLoadPlugins();
    }

    @Override
    public void installPlugin(File jarFile) {
        if (jarFile == null) {
            throw new IllegalArgumentException("Plugin JAR is required");
        }

        String fileName = jarFile.getName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IllegalArgumentException("Only .jar files are supported");
        }

        if (!validateJar(jarFile, false)) {
            throw new IllegalArgumentException("Invalid plugin JAR: " + fileName);
        }

        Path source = jarFile.toPath();
        Path target = pluginsDir.resolve(fileName);

        try {
            if (!source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            DailyLogger.logInfo("PluginAdapter", "Plugin copied to plugins dir: " + fileName);
        } catch (IOException e) {
            DailyLogger.logError("PluginAdapter", "Failed to install plugin: " + fileName, e);
            throw new IllegalStateException("Failed to install plugin: " + fileName, e);
        }
    }

    private PluginMetadata scanJar(File jarFile) {
        if (!validateJar(jarFile, false)) {
            return null;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry manifestEntry = jar.getJarEntry(PLUGIN_MANIFEST);
            if (manifestEntry != null) {
                try (InputStream is = jar.getInputStream(manifestEntry)) {
                    JsonNode node = objectMapper.readTree(is);
                    return new PluginMetadata(
                        node.get("id").asText(),
                        node.get("name").asText(),
                        node.has("version") ? node.get("version").asText() : "1.0.0",
                        node.has("author") ? node.get("author").asText() : "Unknown",
                        node.has("description") ? node.get("description").asText() : "",
                        true,
                        false,
                        jarFile.getAbsolutePath()
                    );
                }
            }
            return scanForPluginClass(jar, jarFile);
        } catch (IOException e) {
            DailyLogger.logWarn("PluginAdapter", "Failed to scan JAR: " + jarFile.getName() + " - " + e.getMessage());
            return null;
        }
    }
    private PluginMetadata scanForPluginClass(JarFile jar, File jarFile) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class") && !name.contains("$")) {
                String className = name.replace('/', '.').replace(".class", "");
                try {
                    URLClassLoader loader = new URLClassLoader(
                        new URL[]{jarFile.toURI().toURL()},
                        getClass().getClassLoader()
                    );
                    Class<?> clazz = loader.loadClass(className);
                    if (Plugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        Plugin instance = (Plugin) clazz.getDeclaredConstructor().newInstance();
                        loader.close();
                        return new PluginMetadata(
                            instance.getId(),
                            instance.getName(),
                            instance.getVersion(),
                            instance.getAuthor(),
                            instance.getDescription(),
                            true,
                            false,
                            jarFile.getAbsolutePath()
                        );
                    }
                    loader.close();
                } catch (Exception e) {
                    DailyLogger.logDebug("PluginAdapter", "Class " + className + " is not a plugin: " + e.getMessage());
                }
            }
        }
        return null;
    }
    private boolean validateJar(File jarFile, boolean requireManifest) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            return false;
        }
        long size = jarFile.length();
        if (size > maxJarSizeBytes) {
            DailyLogger.logWarn("PluginLoader", "Plugin JAR exceeds max size (" + size + " > " + maxJarSizeBytes + "): " + jarFile.getName());
            return false;
        }
        if (requireManifest) {
            try (JarFile jar = new JarFile(jarFile)) {
                return jar.getJarEntry(PLUGIN_MANIFEST) != null;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
    @Override
    public Plugin loadPlugin(String pluginId) {
        File[] jarFiles = pluginsDir.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return null;
        }
        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry manifestEntry = jar.getJarEntry(PLUGIN_MANIFEST);
                String targetId = null;
                if (manifestEntry != null) {
                    try (InputStream is = jar.getInputStream(manifestEntry)) {
                        JsonNode node = objectMapper.readTree(is);
                        targetId = node.get("id").asText();
                    }
                }
                if (pluginId.equals(targetId)) {
                    return loadPluginFromJar(jarFile, pluginId);
                }
                Plugin scannedPlugin = tryLoadPluginByClass(jarFile, pluginId);
                if (scannedPlugin != null) {
                    return scannedPlugin;
                }
            } catch (IOException e) {
                DailyLogger.logWarn("PluginLoader", "Error reading JAR: " + jarFile.getName());
            }
        }
        return null;
    }
    private Plugin loadPluginFromJar(File jarFile, String pluginId) {
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                getClass().getClassLoader()
            );
            classLoaders.put(pluginId, loader);
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        try {
                            Class<?> clazz = loader.loadClass(className);
                            if (Plugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                return (Plugin) clazz.getDeclaredConstructor().newInstance();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            DailyLogger.logError("PluginLoader", "Failed to load plugin JAR: " + jarFile.getName(), e);
        }
        return null;
    }
    private Plugin tryLoadPluginByClass(File jarFile, String pluginId) {
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                getClass().getClassLoader()
            );
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        try {
                            Class<?> clazz = loader.loadClass(className);
                            if (Plugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                Plugin instance = (Plugin) clazz.getDeclaredConstructor().newInstance();
                                if (instance.getId().equals(pluginId)) {
                                    classLoaders.put(pluginId, loader);
                                    return instance;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            loader.close();
        } catch (IOException e) {
            DailyLogger.logDebug("PluginLoader", "Could not search JAR: " + jarFile.getName());
        }
        return null;
    }
    @Override
    public void unloadPlugin(String pluginId) {
        URLClassLoader loader = classLoaders.remove(pluginId);
        if (loader != null) {
            try {
                loader.close();
            } catch (IOException e) {
                DailyLogger.logWarn("PluginLoader", "Error closing classloader for: " + pluginId);
            }
        }
    }
    @Override
    public void deletePlugin(String pluginId) {
        unloadPlugin(pluginId);
        File[] jarFiles = pluginsDir.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) {
            return;
        }
        
        File fileToDelete = null;

        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry manifestEntry = jar.getJarEntry(PLUGIN_MANIFEST);
                if (manifestEntry != null) {
                    try (InputStream is = jar.getInputStream(manifestEntry)) {
                        JsonNode node = objectMapper.readTree(is);
                        if (pluginId.equals(node.get("id").asText())) {
                            fileToDelete = jarFile;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                DailyLogger.logWarn("PluginLoader", "Error checking JAR for deletion: " + jarFile.getName());
            }
        }

        if (fileToDelete != null) {
            
            System.gc();
            try {
                Thread.sleep(100); 
            } catch (InterruptedException ignored) {}

            try {
                Files.delete(fileToDelete.toPath());
                DailyLogger.logInfo("PluginLoader", "Deleted plugin JAR: " + fileToDelete.getName());
            } catch (IOException e) {
                DailyLogger.logError("PluginLoader", "Failed to delete plugin JAR: " + fileToDelete.getName(), e);
                
                fileToDelete.deleteOnExit();
            }
        }
    }
    @Override
    public void savePluginStates(Map<String, Boolean> states) {
        try {
            if (stateFilePath.getParent() != null) {
                Files.createDirectories(stateFilePath.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFilePath.toFile(), states);
        } catch (IOException e) {
            DailyLogger.logError("PluginAdapter", "Failed to save plugin states", e);
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> loadPluginStates() {
        if (!Files.exists(stateFilePath)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(stateFilePath.toFile(), Map.class);
        } catch (IOException e) {
            DailyLogger.logWarn("PluginAdapter", "Failed to load plugin states: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
