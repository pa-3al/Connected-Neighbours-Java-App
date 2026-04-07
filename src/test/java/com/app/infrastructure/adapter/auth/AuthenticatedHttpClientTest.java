package com.app.infrastructure.adapter.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedHttpClientTest {

    private HttpServer server;
    private String baseUrl;
    private AuthenticatedHttpClient client;
    private String capturedAuthHeader;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
        client = new AuthenticatedHttpClient();
        capturedAuthHeader = null;
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendShouldAddBearerTokenAndReturnResponse() throws Exception {
        server.createContext("/api/data", exchange -> {
            capturedAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String response = "success";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        try (MockedStatic<TokenManager> tokenManagerMock = Mockito.mockStatic(TokenManager.class)) {
            tokenManagerMock.when(TokenManager::loadTokens).thenReturn(new String[]{"access123", "refresh123"});

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/data"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request);

            assertEquals(200, response.statusCode());
            assertEquals("success", response.body());
            assertEquals("Bearer access123", capturedAuthHeader);
        }
    }

    @Test
    void sendShouldNotAddTokenWhenTokensAreNull() throws Exception {
        server.createContext("/api/data", exchange -> {
            capturedAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String response = "success";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        try (MockedStatic<TokenManager> tokenManagerMock = Mockito.mockStatic(TokenManager.class)) {
            tokenManagerMock.when(TokenManager::loadTokens).thenReturn(null);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/data"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request);

            assertEquals(200, response.statusCode());
            assertNull(capturedAuthHeader);
        }
    }
}