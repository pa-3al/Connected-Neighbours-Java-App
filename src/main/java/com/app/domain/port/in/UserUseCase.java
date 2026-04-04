package com.app.domain.port.in;

public interface UserUseCase {
    String getUserFullName(String userId);
    void syncUsers();
}
