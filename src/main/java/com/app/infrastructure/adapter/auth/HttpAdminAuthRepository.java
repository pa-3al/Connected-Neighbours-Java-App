package com.app.infrastructure.adapter.auth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
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
        throw new UnsupportedOperationException("Standard login is disabled.");
    }

    @Override
    public AuthResult loginWith2FA(String email, String password, String code) {
        throw new UnsupportedOperationException("Standard login is disabled.");
    }

    @Override
    public String loginWithSso() {
        HttpServer callbackServer;
        try {
            callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize local SSO callback server", e);
        }

        CompletableFuture<String> tokenFuture = new CompletableFuture<>();
        int callbackPort = callbackServer.getAddress().getPort();

        callbackServer.createContext("/callback", exchange -> handleSsoCallback(exchange, tokenFuture));
        callbackServer.createContext("/auth/callback", exchange -> handleSsoCallback(exchange, tokenFuture));
        callbackServer.start();

        String authorizeBaseUrl = buildUrl(configProvider.getAdminSsoAuthorizePath());
        String separator = authorizeBaseUrl.contains("?") ? "&" : "?";
        String authorizeUrl = authorizeBaseUrl + separator + "localPort=" + callbackPort;

        // Capture login stage dimensions on the JavaFX thread before going async
        CompletableFuture<double[]> stageInfoFuture = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage loginStage = javafx.stage.Window.getWindows().stream()
                    .filter(w -> w instanceof Stage && w.isShowing())
                    .map(w -> (Stage) w)
                    .findFirst()
                    .orElse(null);
            if (loginStage != null) {
                stageInfoFuture.complete(new double[]{
                        loginStage.getWidth(),
                        loginStage.getHeight(),
                        loginStage.getX(),
                        loginStage.getY()
                });
            } else {
                stageInfoFuture.complete(new double[]{800, 600, 0, 0});
            }
        });

        double[] stageInfo;
        try {
            stageInfo = stageInfoFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            stageInfo = new double[]{800, 600, 0, 0};
        }
        final double[] finalStageInfo = stageInfo;

        Platform.runLater(() -> {
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            Stage stage = new Stage();
            stage.setTitle("SSO Login");
            stage.setScene(new Scene(new BorderPane(webView), finalStageInfo[0], finalStageInfo[1]));
            stage.setX(finalStageInfo[2]);
            stage.setY(finalStageInfo[3]);
            stage.show();

            webEngine.load(authorizeUrl);

            tokenFuture.whenComplete((token, ex) -> Platform.runLater(stage::close));
        });

        try {
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
        throw new UnsupportedOperationException("Direct authentication is disabled.");
    }

    private void handleSsoCallback(HttpExchange exchange, CompletableFuture<String> tokenFuture) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String accessToken = params.get("accessToken");
        String error = params.get("error");

        int statusCode = 200;
        String message = "Authentication complete. Returning to application...";

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
            tokenFuture.complete(accessToken);
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