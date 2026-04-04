package com.app.domain.service;

import com.app.domain.model.User;
import com.app.domain.port.in.UserUseCase;
import com.app.domain.port.out.UserRepository;
import com.app.domain.port.out.RemoteUserRepository;

import java.util.List;

public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RemoteUserRepository remoteUserRepository;

    public UserService(UserRepository userRepository, RemoteUserRepository remoteUserRepository) {
        this.userRepository = userRepository;
        this.remoteUserRepository = remoteUserRepository;
    }

    @Override
    public String getUserFullName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "";
        }
        return userRepository.findById(userId)
                .map(User::fullName)
                .orElse(userId);
    }

    @Override
    public void syncUsers() {
        List<User> remoteUsers = remoteUserRepository.fetchAllUsers();
        if (!remoteUsers.isEmpty()) {
            userRepository.saveAll(remoteUsers);
        }
    }
}