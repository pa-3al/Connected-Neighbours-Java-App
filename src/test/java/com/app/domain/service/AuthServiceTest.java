package com.app.domain.service;

import com.app.domain.model.AuthResult;
import com.app.domain.port.out.AuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthRepository authRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authRepository = mock(AuthRepository.class);
        authService = new AuthService(authRepository);
    }

    @Test
    void authServiceShouldReturnAuthResultOnLogin() {
        AuthResult expected = AuthResult.authenticated("token");
        when(authRepository.login("user@test.com", "pass")).thenReturn(expected);

        AuthResult actual = authService.login("user@test.com", "pass");

        assertEquals(expected, actual);
        verify(authRepository).login("user@test.com", "pass");
    }

    @Test
    void authServiceShouldReturnAuthResultOnLoginWith2FA() {
        AuthResult expected = AuthResult.authenticated("token2fa");
        when(authRepository.loginWith2FA("user@test.com", "pass", "123456")).thenReturn(expected);

        AuthResult actual = authService.loginWith2FA("user@test.com", "pass", "123456");

        assertEquals(expected, actual);
        verify(authRepository).loginWith2FA("user@test.com", "pass", "123456");
    }

    @Test
    void authServiceShouldReturnUrlOnLoginWithSso() {
        String expectedUrl = "https://sso.url";
        when(authRepository.loginWithSso()).thenReturn(expectedUrl);

        String actualUrl = authService.loginWithSso();

        assertEquals(expectedUrl, actualUrl);
        verify(authRepository).loginWithSso();
    }
}