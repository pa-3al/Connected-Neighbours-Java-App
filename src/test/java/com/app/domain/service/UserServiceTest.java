package com.app.domain.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.domain.model.User;
import com.app.domain.port.out.RemoteUserRepository;
import com.app.domain.port.out.UserRepository;

class UserServiceTest {

    private UserRepository userRepository;
    private RemoteUserRepository remoteUserRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        remoteUserRepository = mock(RemoteUserRepository.class);
        userService = new UserService(userRepository, remoteUserRepository);
    }

    @Test
    void getUserFullNameShouldReturnEmptyStringForBlankIdentifier() {
        assertEquals("", userService.getUserFullName("   "));
    }

    @Test
    void getUserFullNameShouldReturnUserFullNameWhenFound() {
        User user = new User("42", "Ada", "Lovelace", "ada@test.com");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));

        assertEquals("Ada Lovelace", userService.getUserFullName("42"));
        verify(userRepository).findById("42");
    }

    @Test
    void getUserFullNameShouldFallbackToIdentifierWhenUserIsMissing() {
        when(userRepository.findById("42")).thenReturn(Optional.empty());

        assertEquals("42", userService.getUserFullName("42"));
        verify(userRepository).findById("42");
    }

    @Test
    void syncUsersShouldPersistRemoteUsersWhenAvailable() {
        List<User> remoteUsers = List.of(
                new User("1", "Ada", "Lovelace", "ada@test.com"),
                new User("2", "Grace", "Hopper", "grace@test.com")
        );
        when(remoteUserRepository.fetchAllUsers()).thenReturn(remoteUsers);

        userService.syncUsers();

        verify(remoteUserRepository).fetchAllUsers();
        verify(userRepository).saveAll(remoteUsers);
    }

    @Test
    void syncUsersShouldSkipSaveWhenRemoteUsersAreEmpty() {
        when(remoteUserRepository.fetchAllUsers()).thenReturn(List.of());

        userService.syncUsers();

        verify(remoteUserRepository).fetchAllUsers();
        verify(userRepository, never()).saveAll(anyList());
    }
}