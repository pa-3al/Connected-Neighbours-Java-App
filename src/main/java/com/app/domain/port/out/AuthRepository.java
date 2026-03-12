package com.app.domain.port.out;

import com.app.domain.model.AuthResult;

public interface AuthRepository {
    AuthResult login(String email, String password);

    AuthResult loginWith2FA(String email, String password, String code);

    String loginWithSso();
}
