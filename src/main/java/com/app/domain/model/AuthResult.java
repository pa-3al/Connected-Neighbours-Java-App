package com.app.domain.model;

public record AuthResult(String accessToken, boolean twoFactorRequired) {
    public static AuthResult authenticated(String accessToken) {
        return new AuthResult(accessToken, false);
    }

    public static AuthResult requireTwoFactor() {
        return new AuthResult("", true);
    }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank() && !twoFactorRequired;
    }
}
