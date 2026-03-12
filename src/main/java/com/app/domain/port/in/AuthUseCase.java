package com.app.domain.port.in;

import com.app.domain.model.AuthResult;

public interface AuthUseCase {
    AuthResult login(String email, String password);

    AuthResult loginWith2FA(String email, String password, String code);

    String loginWithSso();
}
