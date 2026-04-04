package com.app.domain.port.out;

import com.app.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    void saveAll(List<User> users);
    List<User> findAll();
}