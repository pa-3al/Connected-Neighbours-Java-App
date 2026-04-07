package com.app.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResultTest {

    @Test
    void authResultShouldReturnAuthenticatedState() {
        AuthResult result = AuthResult.authenticated("token123");
        assertEquals("token123", result.accessToken());
        assertFalse(result.twoFactorRequired());
        assertTrue(result.isAuthenticated());
    }

    @Test
    void authResultShouldReturnTwoFactorRequiredState() {
        AuthResult result = AuthResult.requireTwoFactor();
        assertEquals("", result.accessToken());
        assertTrue(result.twoFactorRequired());
        assertFalse(result.isAuthenticated());
    }
}