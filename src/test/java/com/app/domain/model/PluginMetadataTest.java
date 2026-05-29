package com.app.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.app.domain.model.PluginOrigin;

class PluginMetadataTest {

    @Test
    void builtinConstructorShouldSetDefaultMetadata() {
        PluginMetadata metadata = new PluginMetadata("Test Plugin");

        assertEquals("builtin-test-plugin", metadata.id());
        assertEquals("Test Plugin", metadata.name());
        assertEquals("1.0.0", metadata.version());
        assertEquals("System", metadata.author());
        assertEquals("Built-in plugin", metadata.description());
        assertTrue(metadata.enabled());
        assertTrue(metadata.isLoaded());
        assertNull(metadata.jarPath());
        assertNull(metadata.downloadUrl());
        assertEquals(PluginOrigin.BUILTIN, metadata.source());
    }

    @Test
    void withEnabledShouldPreserveOtherFields() {
        PluginMetadata metadata = new PluginMetadata("plug-1", "Plugin", "2.0.0", "Author", "Desc", false, true, "/tmp/plugin.jar", "https://example.com/plugin.jar", PluginOrigin.LOCAL);

        PluginMetadata updated = metadata.withEnabled(true);

        assertEquals(metadata.id(), updated.id());
        assertEquals(metadata.name(), updated.name());
        assertEquals(metadata.version(), updated.version());
        assertEquals(metadata.author(), updated.author());
        assertEquals(metadata.description(), updated.description());
        assertTrue(updated.enabled());
        assertEquals(metadata.isLoaded(), updated.isLoaded());
        assertEquals(metadata.jarPath(), updated.jarPath());
        assertEquals(metadata.downloadUrl(), updated.downloadUrl());
        assertEquals(metadata.source(), updated.source());
    }

    @Test
    void withLoadedShouldPreserveOtherFields() {
        PluginMetadata metadata = new PluginMetadata("plug-1", "Plugin", "2.0.0", "Author", "Desc", true, false, "/tmp/plugin.jar", "https://example.com/plugin.jar", PluginOrigin.LOCAL);

        PluginMetadata updated = metadata.withLoaded(true);

        assertEquals(metadata.id(), updated.id());
        assertEquals(metadata.name(), updated.name());
        assertEquals(metadata.version(), updated.version());
        assertEquals(metadata.author(), updated.author());
        assertEquals(metadata.description(), updated.description());
        assertEquals(metadata.enabled(), updated.enabled());
        assertTrue(updated.isLoaded());
        assertEquals(metadata.jarPath(), updated.jarPath());
        assertEquals(metadata.downloadUrl(), updated.downloadUrl());
        assertEquals(metadata.source(), updated.source());
    }
}