package com.app.infrastructure.adapter.update;

import com.app.domain.model.UpdateInfo;
import com.app.infrastructure.config.ConfigProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpUpdateAdapterTest {

    private HttpServer server;
    private String baseUrl;
    private ConfigProvider configProvider;
    private Supplier<Boolean> onlineSupplier;
    private HttpUpdateAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();

        configProvider = mock(ConfigProvider.class);
        onlineSupplier = mock(Supplier.class);
        when(configProvider.getUpdateTimeoutSeconds()).thenReturn(10);
        adapter = new HttpUpdateAdapter(configProvider, onlineSupplier);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchLatestUpdateInfoShouldReturnUpdateInfoWhenStatus200() throws Exception {
        server.createContext("/update", exchange -> {
            String response = "{\"version\":\"2.0\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        when(onlineSupplier.get()).thenReturn(true);
        when(configProvider.getUpdateCheckUrl()).thenReturn(baseUrl + "/update");

        CompletableFuture<UpdateInfo> future = adapter.fetchLatestUpdateInfo();
        UpdateInfo result = future.get();
        assertNotNull(result);
    }

    @Test
    void fetchLatestUpdateInfoShouldReturnNullWhenStatus204() throws Exception {
        server.createContext("/update", exchange -> {
            exchange.sendResponseHeaders(204, -1);
        });

        when(onlineSupplier.get()).thenReturn(true);
        when(configProvider.getUpdateCheckUrl()).thenReturn(baseUrl + "/update");

        CompletableFuture<UpdateInfo> future = adapter.fetchLatestUpdateInfo();
        UpdateInfo result = future.get();
        assertNull(result);
    }

    @Test
    void fetchLatestUpdateInfoShouldThrowExceptionWhenOffline() {
        when(onlineSupplier.get()).thenReturn(false);
        CompletableFuture<UpdateInfo> future = adapter.fetchLatestUpdateInfo();
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void downloadUpdateShouldReturnTargetPathWhenSuccess(@TempDir Path tempDir) throws Exception {
        server.createContext("/download", exchange -> {
            String response = "bin-data";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        when(onlineSupplier.get()).thenReturn(true);
        Path targetFile = tempDir.resolve("update.bin");
        CompletableFuture<Path> future = adapter.downloadUpdate(
                baseUrl + "/download", targetFile, null);
        Path result = future.get();

        assertTrue(Files.exists(result));
        assertEquals("bin-data", Files.readString(result));
    }
}