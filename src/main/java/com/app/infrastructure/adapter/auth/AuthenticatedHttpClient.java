package com.app.infrastructure.adapter.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.app.infrastructure.config.ConfigProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthenticatedHttpClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ConfigProvider configProvider = new ConfigProvider();
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpResponse<String> send(HttpRequest request) throws Exception {
        String[] tokens = TokenManager.loadTokens();
        String accessToken = tokens != null ? tokens[0] : "";

        HttpRequest authenticatedRequest = addToken(request, accessToken);
        HttpResponse<String> response = client.send(authenticatedRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            if (tokens != null && tokens[1] != null && !tokens[1].isEmpty()) {
                String newAccessToken = doRefresh(tokens[1]);
                if (newAccessToken != null) {
                    TokenManager.saveTokens(newAccessToken, tokens[1]);
                    HttpRequest retriedRequest = addToken(request, newAccessToken);
                    return client.send(retriedRequest, HttpResponse.BodyHandlers.ofString());
                } else {
                    TokenManager.clearTokens();
                }
            }
        }
        return response;
    }

    private HttpRequest addToken(HttpRequest request, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

        request.headers().map().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Authorization")) {
                for (String val : v) {
                    builder.header(k, val);
                }
            }
        });

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    public String doRefresh(String refreshToken) {
        try {
            String url = configProvider.getAuthBaseUrl() + "/admin/auth/refresh/desktop";
            ObjectNode json = mapper.createObjectNode();
            json.put("refreshToken", refreshToken);

            HttpRequest refreshReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(refreshReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return mapper.readTree(response.body()).path("accessToken").asText();
            }
        } catch (Exception ignored) {}
        return null;
    }
}