package com.app.infrastructure.adapter.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TokenManager {

    private static final Path TOKEN_FILE = Paths.get("data", "tokens.txt");

    public static void saveTokens(String accessToken, String refreshToken) {
        try {
            Files.createDirectories(TOKEN_FILE.getParent());
            Files.write(TOKEN_FILE, List.of(
                    accessToken != null ? accessToken : "",
                    refreshToken != null ? refreshToken : ""
            ));
        } catch (IOException ignored) {}
    }

    public static String[] loadTokens() {
        try {
            if (Files.exists(TOKEN_FILE)) {
                List<String> lines = Files.readAllLines(TOKEN_FILE);
                if (lines.size() >= 2) {
                    return new String[]{lines.get(0), lines.get(1)};
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    public static void clearTokens() {
        try {
            Files.deleteIfExists(TOKEN_FILE);
        } catch (IOException ignored) {}
    }
}