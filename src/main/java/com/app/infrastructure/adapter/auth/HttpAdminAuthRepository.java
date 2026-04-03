package com.app.infrastructure.adapter.auth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.app.domain.model.AuthResult;
import com.app.domain.port.out.AuthRepository;
import com.app.infrastructure.config.ConfigProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class HttpAdminAuthRepository implements AuthRepository {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConfigProvider configProvider;

    public HttpAdminAuthRepository(ConfigProvider configProvider) {
        this.configProvider = configProvider;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(configProvider.getAuthTimeoutSeconds()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AuthResult login(String email, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return authenticate(configProvider.getAdminLoginPath(), payload, "Invalid credentials");
    }

    @Override
    public AuthResult loginWith2FA(String email, String password, String code) {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("code", code);
        return authenticate(configProvider.getAdminLogin2FAPath(), payload, "Invalid 2FA code or credentials");
    }

    @Override
    public String loginWithSso() {
        HttpServer callbackServer;
        int callbackPort = 8080;
        while (true) {
            try {
                callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", callbackPort), 0);
                break;
            } catch (IOException e) {
                callbackPort++;
                if (callbackPort > 65535) {
                    throw new RuntimeException("", e);
                }
            }
        }

        CompletableFuture<String> tokenFuture = new CompletableFuture<>();

        callbackServer.createContext("/callback", exchange -> handleSsoCallback(exchange, tokenFuture));
        callbackServer.createContext("/auth/callback", exchange -> handleSsoCallback(exchange, tokenFuture));
        callbackServer.start();

        String authorizeBaseUrl = buildUrl(configProvider.getAdminSsoAuthorizePath());
        String separator = authorizeBaseUrl.contains("?") ? "&" : "?";
        String authorizeUrl = authorizeBaseUrl + separator + "localPort=" + callbackPort;

        try {
            Platform.runLater(() -> {
                Stage ssoStage = new Stage();
                ssoStage.initModality(Modality.APPLICATION_MODAL);
                ssoStage.setTitle("SSO Login");

                WebView webView = new WebView();
                webView.getEngine().load(authorizeUrl);

                StackPane root = new StackPane(webView);
                Scene scene = new Scene(root, 600, 700);
                ssoStage.setScene(scene);

                ssoStage.setOnCloseRequest(event -> {
                    if (!tokenFuture.isDone()) {
                        tokenFuture.completeExceptionally(new IllegalStateException("SSO login cancelled by user."));
                    }
                });

                tokenFuture.whenComplete((res, ex) -> {
                    Platform.runLater(ssoStage::close);
                });

                ssoStage.show();
            });

            return tokenFuture.get(configProvider.getSsoTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("SSO timeout: no callback received", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SSO login interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("SSO login failed", cause == null ? e : cause);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            callbackServer.stop(0);
        }
    }

    private AuthResult authenticate(String path, Map<String, String> payload, String unauthorizedMessage) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path)))
                    .timeout(Duration.ofSeconds(configProvider.getAuthTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 401) {
                throw new IllegalArgumentException(unauthorizedMessage);
            }

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Authentication failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (root.path("twoFactorRequired").asBoolean(false)) {
                return AuthResult.requireTwoFactor();
            }

            String accessToken = root.path("accessToken").asText("");
            if (accessToken.isBlank()) {
                throw new IllegalStateException("Authentication response does not contain an access token");
            }

            return AuthResult.authenticated(accessToken);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Authentication request interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Authentication request failed", e);
        }
    }

    private void handleSsoCallback(HttpExchange exchange, CompletableFuture<String> tokenFuture) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String accessToken = params.get("accessToken");
        String refreshToken = params.get("refreshToken");
        String error = params.get("error");

        int statusCode = 200;
        String message = "SSO login successful. You can close this tab.";

        if (error != null && !error.isBlank()) {
            statusCode = 401;
            message = "SSO failed: " + error;
            if (!tokenFuture.isDone()) {
                tokenFuture.completeExceptionally(new IllegalStateException("SSO failed: " + error));
            }
        } else if (accessToken == null || accessToken.isBlank()) {
            statusCode = 400;
            message = "SSO failed: missing access token.";
            if (!tokenFuture.isDone()) {
                tokenFuture.completeExceptionally(new IllegalStateException("SSO failed: missing access token"));
            }
        } else if (!tokenFuture.isDone()) {
            String combinedTokens = accessToken + ":" + (refreshToken != null ? refreshToken : "");
            tokenFuture.complete(combinedTokens);
        }

        byte[] body = htmlResponse(message).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, body.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new HashMap<>();

        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }

        return result;
    }

    private String htmlResponse(String message) {
        return "<html><body style='font-family:sans-serif;padding:24px'>"
                + "<h2>Connected-Neighbours-Java-App</h2>"
                + "<p>" + message + "</p>"
                + "</body></html>";
    }

    private String buildUrl(String path) {
        String base = configProvider.getAuthBaseUrl();
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
