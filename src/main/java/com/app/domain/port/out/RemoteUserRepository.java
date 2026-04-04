package com.app.domain.port.out;

import com.app.domain.model.User;
import java.util.List;

public interface RemoteUserRepository {
    List<User> fetchAllUsers();
}