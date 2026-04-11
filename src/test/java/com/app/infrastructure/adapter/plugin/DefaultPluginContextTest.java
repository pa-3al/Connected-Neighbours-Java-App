package com.app.infrastructure.adapter.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.app.plugin.Plugin;

class DefaultPluginContextTest {

    private final List<Path> createdPaths = new ArrayList<>();

    @AfterEach
    void tearDown() throws IOException {
        for (Path path : createdPaths) {
            if (Files.exists(path)) {
                try (var walk = Files.walk(path)) {
                    walk.sorted((left, right) -> right.compareTo(left))
                            .forEach(current -> {
                                try {
                                    Files.deleteIfExists(current);
                                } catch (IOException ignored) {
                                }
                            });
                }
            }
        }
        createdPaths.clear();
    }

    @Test
    void publishShouldNotifySubscribersAndSupportUnsubscribe() {
        DefaultPluginContext context = new DefaultPluginContext();
        AtomicReference<Object> received = new AtomicReference<>();
        Consumer<Object> handler = received::set;

        context.subscribe("event", handler);
        context.publish("event", "payload");

        assertEquals("payload", received.get());

        context.unsubscribe("event", handler);
        received.set(null);
        context.publish("event", "second");

        assertNull(received.get());
    }

    @Test
    void exportFormatRegistryShouldAddAndRemoveFormats() {
        DefaultPluginContext context = new DefaultPluginContext();
        Plugin.ExportFormat csvFormat = new Plugin.ExportFormat("csv", "CSV", ".csv", (data, destination) -> {
        });

        context.registerExportFormat(csvFormat);
        assertEquals(1, context.getRegisteredExportFormats().size());
        assertEquals("csv", context.getRegisteredExportFormats().get(0).id());

        context.unregisterExportFormat("csv");
        assertTrue(context.getRegisteredExportFormats().isEmpty());
    }

    @Test
    void getPluginDataDirectoryShouldCreateDirectory() {
        DefaultPluginContext context = new DefaultPluginContext();

        Path directory = context.getPluginDataDirectory("test-plugin-context");
        createdPaths.add(directory);

        assertTrue(Files.exists(directory));
        assertTrue(directory.toString().replace('\\', '/').endsWith("data/plugins/test-plugin-context"));
        assertFalse(context.isHeadless());
    }
}