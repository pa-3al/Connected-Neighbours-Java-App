package com.app.domain.service;

import com.app.domain.model.AuthResult;
import com.app.domain.port.in.AuthUseCase;
import com.app.domain.port.out.AuthRepository;

public class AuthService implements AuthUseCase {
    private final AuthRepository authRepository;

    public AuthService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public AuthResult login(String email, String password) {
        return authRepository.login(email, password);
    }

    @Override
    public AuthResult loginWith2FA(String email, String password, String code) {
        return authRepository.loginWith2FA(email, password, code);
    }

    @Override
    public String loginWithSso() {
        return authRepository.loginWithSso();
    }
}
